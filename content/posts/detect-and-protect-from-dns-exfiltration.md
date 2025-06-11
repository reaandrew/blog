---
author: "Andy Rea"
title: "Complementing GuardDuty: DNS Exfiltration Detection and Protection in under 60 seconds with AWS DNS Firewall, Athena, Glue, S3, and Lambda"
date: 2025-06-03
draft: false
description: "In under 2 minutes from receiving DNS traffic, I built an AWS integration that automatically detects high-frequency DNS queries (potential data exfiltration attempts) and immediately adds blocking rules to Route 53 DNS Firewall to prevent further data transmission. This post demonstrates real-time DNS threat detection and automated protection using AWS services."
image: "dns_data_exfiltration.png"
tags: ["DNS exfiltration", "AWS security", "threat detection", "Route 53","Amazon Athena", "DNS monitoring", "automated protection", "real-time analysis", "DNS Firewall", "cybersecurity"]
---


## TL;DR

In under 60 seconds from receiving DNS traffic, this AWS solution automatically detected high-frequency DNS queries (potential data-exfiltration attempts) and immediately added blocking rules to Route 53 DNS Firewall to prevent further data transmission. This post demonstrates real-time DNS threat detection and automated protection using AWS services.

While **GuardDuty** can detect DNS-based data exfiltration, my experience showed that it often responds too slowly to catch fast tunneling attacks. I was curious whether I could build something faster using serverless components—this project was the result.

### Actual Timeline (55 s end-to-end)
- **11 : 58 : 14** – DNS queries were logged to S3  
- **11 : 58 : 15** – Partition repair was triggered (< 1 s)  
- **11 : 59 : 09** – Threat was detected & firewall rule was created
- **Total:** 55 seconds from DNS traffic to automated blocking

> ⚠️ **Important Note:** This is an experimental project designed to explore DNS exfiltration patterns and AWS service orchestration. It is **not** production-grade and should only be used after thorough review, testing, and hardening.

**SOURCE CODE** – All code for this experiment is available at: [https://github.com/reaandrew/dnsexfil](https://github.com/reaandrew/dnsexfil)

## Background
A while back I spent time learning more about DNS data-exfiltration. One simple technique was to chunk and encode data into sub-domains: a DNS query to that sub-domain sent the data to the name-server, which could decode and rebuild it—simple yet powerful.

![dns_logical.png](/images/dns_logical.png)

That experiment got me thinking about how I could both detect and protect against this. Many products offered coverage, but I wanted to understand the patterns to look for and the safeguards I could put in place, so I built a small setup by using AWS managed services.

![simple_dns_infrastructure.png](/images/simple_dns_infrastructure.png)

## Pipeline Architecture
To wire everything together, I built a three-layer, event-driven pipeline that collected every DNS query, analysed it in near-real time and pushed blocks straight back into Route 53 DNS Firewall:

- **Data-collection layer** – Route 53 Resolver streamed every DNS lookup from the VPC straight into an S3 bucket defined in `dns_logs_athena.tf`, while a single `m5.large` instance generated the test traffic. Raw logs and Athena result sets lived in separate buckets so I could version-control retention independently.
- **Analysis layer** – The moment a new log dropped, a Partition-Repair Lambda (`lambdas/partition-repair`) fired and ran `MSCK REPAIR TABLE` so Athena could see the fresh partitions. Every five minutes an Athena workgroup ran a high-frequency-exfiltration query that flagged bursts of > 20 requests to the same apex domain within a five-minute window. Domain parsing leaned on the Public Suffix List, so `something.co.uk` didn’t raise a false alarm.
- **Response layer** – A second Lambda (`lambdas/threat-detection`) ran on a one-minute schedule, pulled the Athena results and, for anything rated HIGH or CRITICAL, wrote an immediate DNS-Firewall rule. If there were more than ten unique sub-domains it dropped a wildcard block (`*.domain.com`); otherwise it blocked the apex.

Because logs went straight to S3 and Athena scanned only the latest five-minute partition, the whole loop remained inexpensive and, more importantly, reacted inside a minute.

![step_by_step_flow.png](/images/step_by_step_flow.png)


## Athena Experience & Query Generation
I’ll be honest—I hadn’t used Amazon Athena much before this project, and it quickly became the star of the show. Athena brings a surprisingly complete set of SQL constructs you’d expect from a traditional RDBMS (window functions, CTEs, regex, `WITH` clauses, you name it) yet scales effortlessly to hundreds of gigabytes of logs without any cluster management on my side.

The detection queries are a bit gnarly, so I leaned on an LLM to create and refine the SQL and verify edge cases. Here’s a snippet of the high frequency exfiltration detector:

```sql
SELECT
  CASE 
    WHEN cardinality(labels) > suffix_len THEN
      concat(
        element_at(labels, cardinality(labels) - suffix_len),
        '.', suffix
      )
    ELSE suffix
  END AS apex_domain,
  count(*) AS query_count,
  count(DISTINCT srcaddr) AS source_count,
  count(DISTINCT fqdn) AS unique_subdomains,
  min(ts) AS first_seen,
  max(ts) AS last_seen,
  'HIGH_FREQUENCY' AS threat_type,
  CASE 
    WHEN count(*) > 100 THEN 'CRITICAL'
    WHEN count(*) > 50 THEN 'HIGH' 
    WHEN count(*) > 20 THEN 'MEDIUM'
    ELSE 'LOW'
  END AS severity
FROM best
WHERE cardinality(labels) > suffix_len + 1  -- Only domains with subdomains (not direct apex queries)
GROUP BY 
  CASE 
    WHEN cardinality(labels) > suffix_len THEN
      concat(element_at(labels, cardinality(labels) - suffix_len), '.', suffix)
    ELSE suffix
  END
HAVING count(*) > 20  -- Threshold for 5-minute window
ORDER BY query_count DESC
LIMIT 50;
```

## Traffic Generation Test
To generate a realistic burst, I launched a single instance in the target VPC and pointed it at a custom DNS server in a separate AWS account. A loop of 250 `dig` calls with no delay queried `(6-random-hex-chars).dnsdemo.andrewrea.co.uk`, giving the pipeline plenty of high-frequency noise to detect. The Response Lambda described above executed the saved Athena query every minute and pushed a DNS-Firewall rule whenever it saw a HIGH or CRITICAL result—keeping reaction time under a minute.

```shell
    for i in {1..250}; do
      dig "$(openssl rand -hex 3).dnsdemo.andrewrea.co.uk"
    done
```

Once I had seen the firewall blocking rule created, I executed the same test generation (new subdomains so not to be mistaken with DNS caching on the client) and no calls made it through to the server.  Success!

## Commercial Comparison – PowerDNS vs AWS
PowerDNS dnsdist added a dynamic-block feature (Issue #7380, fixed 11 Dec 2023) that fires when a client shows a high cache-miss ratio. My AWS pipeline counts unique sub-domains (> 20 in 5 minutes). Same intuition, different signals:

|                           | PowerDNS dnsdist | AWS pipeline |
|---------------------------|------------------|--------------|
| **Trigger metric**        | High cache-miss ratio | > 20 unique sub-domains / 5 min |
| **Reaction time**         | Milliseconds (inline) | ~ 55 s (out-of-band) |
| **Block scope**           | Per client, auto-expires | Per domain, persists until cleared |
| **Operational burden**    | Run dnsdist + Lua rules | Fully serverless (S3 → Athena → Lambda) |
{.table}

Reference: <https://github.com/PowerDNS/pdns/issues/7380>

## GuardDuty: Complementary Detection in Defense-in-Depth

This 55-second detection system works alongside AWS GuardDuty, not instead of it. Both provide valuable but different capabilities:

| System          | Timeline                 | Purpose                                        |
|-----------------|--------------------------|------------------------------------------------|
| Custom Pipeline | 55 seconds               | Immediate blocking of high-frequency bursts    |
| AWS GuardDuty   | 15 minutes to 1+ hours   | ML-based validation with lower false positives |
{.table}

**Is Faster Detection Worth It?**

It depends. The time, cost, and complexity of this experimental solution on top of GuardDuty is relatively low, so a pre-made extensible solution could add value rather than reducing it. The infrastructure deploys in ~66 seconds, costs ~$30-95/month for most use cases, and provides immediate protection against high-frequency attacks that could exfiltrate significant data during GuardDuty's variable detection window. 

**Defense-in-Depth Value**

This experimental custom pipeline stops high-frequency exfiltration under 60 seconds while GuardDuty provides threat validation for sophisticated patterns. GuardDuty's detection window of 15 minutes to 1+ hours (depending on traffic patterns) could allow significant data loss in burst attacks—demonstrating why sub-minute response times matter for comprehensive DNS protection. Any production implementation would need extensive security review, testing, and hardening to validate detection accuracy and prevent false positives that could disrupt legitimate traffic.

**Note:** According to AWS documentation, GuardDuty focuses on [detection rather than automatic blocking](https://aws.amazon.com/guardduty/faqs/#topic-2). AWS provides [guidance to automatically update DNS Firewall rules](https://aws.amazon.com/blogs/security/automatically-block-suspicious-dns-activity-with-amazon-guardduty-and-route-53-resolver-dns-firewall/) based on GuardDuty findings, similar to the approach demonstrated in this blog post.

**Architectural Consideration:** I chose to add firewall rules directly in this solution, whereas it would have been better to follow the GuardDuty approach and instead publish security events to EventBridge and Security Hub, then respond to these with Lambda. This would provide better integration with existing security workflows and allow for more sophisticated response orchestration.

Both systems together provide comprehensive coverage: fast protection plus accurate threat intelligence.

## Monthly Cost Estimates (Excluding EC2)

| Service / Traffic Level   | Low (10 K q/day) | Medium (500 K q/day) | High (10 M q/day) |
|---------------------------|------------------|----------------------|-------------------|
| Lambda executions         | \$15             | \$20                 | \$35              |
| Route 53 Resolver logging | \$0.15           | \$7.50               | \$150             |
| S3 storage                | \$5              | \$25                 | \$100             |
| Athena queries            | \$5              | \$25                 | \$150             |
| CloudWatch logs           | \$3              | \$15                 | \$75              |
| Route 53 DNS Firewall     | \$0.10           | \$0.50               | \$10              |
| **Estimated total / month** | **≈ \$30**       | **≈ \$95**           | **≈ \$520**       |
{.table}

## Future Detection Patterns to Explore

While this project focused on **high-frequency queries to a shared base domain** (e.g. `*.tunnel.example.com`), there are several other DNS exfiltration and misuse patterns I’m interested in exploring. For each pattern, I would first evaluate GuardDuty's response time and feature support—if the pattern could be accelerated or if GuardDuty lacks coverage for specific detection logic, then I would investigate it further in future experiments:

**High-entropy subdomains**  
Subdomains with long, random-looking labels—often base64 or hex—are a strong indicator of tunneling activity. These may look like:

```shell
b79f5a89d2b23a3d6e.example.com  
MDEyMzQ1Njc4OWFiY2Rl.example.com
```

Statistical analysis or entropy scoring on the label can help distinguish these from normal DNS patterns.

**Unusually long domain names**  
Total FQDN lengths exceeding 100+ characters are rare in standard usage but common in tunneling scenarios where encoded data is packed into the label space. A simple `length(fqdn) > 100` check could flag these for review.

**Uncommon query types**  
Exfiltration tools like `iodine` or `dnscat2` often use **TXT**, **NULL**, or **CNAME** query types instead of the usual `A` or `AAAA`. Filtering based on `qtype` can catch these:

```sql
SELECT *
FROM dns_logs
WHERE qtype IN ('TXT', 'NULL', 'CNAME')
```

**Subdomain Enumeration**  
Attackers performing reconnaissance may brute-force subdomains (e.g., `dev.example.com`, `test.example.com`). A sudden spike in unique subdomains (e.g., >10 within 10 minutes) can indicate enumeration attempts:

```sql
SELECT
  domain,
  count(DISTINCT subdomain) AS subdomain_count
FROM dns_logs
WHERE ts BETWEEN now() - interval '10' minute AND now()
GROUP BY domain
HAVING subdomain_count > 10
```

**LLM-assisted log analysis**  
A longer-term experiment I’m planning involves piping 24-hour logs into a Large Language Model (LLM) and asking it to summarise unusual domain names, query rates, or outliers. While not deterministic, this adds another layer of heuristic analysis that might catch subtle trends or new tooling before they show up as events.

Each of these ideas can be integrated into the existing Athena + Lambda pipeline with minor additions to the SQL logic or threat-detection Lambda handler. Over time, this can evolve into a layered detection system for multiple forms of DNS misuse.

## Infrastructure Provisioning and Destruction Summary

The full setup completed in ~1 minute using Terraform, including VPC, S3, Glue, Athena, Route 53, GuardDuty, IAM, and Lambda resources.

**Provisioning Highlights:**

- **VPC & Networking** – 11 s
- **S3 Buckets & Glue Tables** – 2–3 s
- **IAM Roles & Policies** – ~1 s each
- **Lambda Functions** – Partition Repair (25 s), Threat Detection (6 s)
- **Athena Workgroup & Queries** – Instant
- **Route 53 Firewall Setup** – Domain list + association (46 s total)
- **EC2 Test Instance** – 15 s
- **Public Suffix List Upload** – ~5 s

```shell
Total elapsed time: 1:06.55 (via `time`)
```

Tearing down the full environment took **~57 seconds**, cleaning up all AWS resources including networking, logging, Athena, Glue, Lambda, and DNS Firewall.

**Destruction Highlights:**

- **Athena & Glue Cleanup** – Instant (0 s each)
- **Lambda Functions & IAM Roles** – 0–1 s
- **CloudWatch Log Groups & Event Rules** – 0–1 s
- **Route 53 DNS Firewall Domain List** – 33 s
- **EC2 Instance Termination** – 41 s
- **VPC Networking (IGW/Subnet/SG)** – IGW 27 s, Subnet 3 s, SG instant
- **Longest Item** – DNS Firewall rule group association: 46 s

```shell
Total elapsed time: 0:56.70 (via `time`)
```


## Public Suffix List Loader Script
To identify the true apex domain of every query, I needed a fast, deterministic lookup table rather than splitting labels on the fly. The loader below downloads the canonical Public Suffix List, removes wildcard/exception rules, converts each entry into `suffix,label_count` CSV and uploads it to S3 for Athena.

```shell
#!/bin/bash

# Download and process Public Suffix List for Athena
echo "Downloading Public Suffix List..."
curl -s https://publicsuffix.org/list/public_suffix_list.dat > /tmp/psl.dat

# Convert to CSV format
echo "Converting to CSV..."
cat > /tmp/process_psl.py << 'EOF'
import sys

suffixes = []

with open('/tmp/psl.dat', 'r') as f:
    for line in f:
        line = line.strip()
        
        # Skip comments and empty lines
        if not line or line.startswith('//'):
            continue
            
        # Handle wildcard and exception rules
        if line.startswith('*.') or line.startswith('!'):
            continue
        else:
            # Regular suffix
            suffix = line.lower()
            label_count = len(suffix.split('.'))
            print(f"{suffix},{label_count}")

EOF

python3 /tmp/process_psl.py > /tmp/public_suffixes.csv

echo "Uploading to S3..."
aws s3 cp /tmp/public_suffixes.csv s3://dnsexfil-demo-athena-logs/psl/public_suffixes.csv

echo "Cleaning up..."
rm -f /tmp/psl.dat /tmp/process_psl.py /tmp/public_suffixes.csv

echo "Done! PSL data uploaded to S3."
```

## Conclusion

This experiment has taught me a lot more about DNS, types of exfiltration methods and tools, ways to detect and also protect. It has also shown me how other products have implemented these patterns and shown me the difference in timings and also some clue as to the implementation.

While this project focused on demonstrating detection and protection concepts using a simple test loop, there are more sophisticated DNS tunneling tools available that I researched but didn't use in this experiment (as it would be overkill for what I set out to prove). Notable tools include:

- **[iodine](https://github.com/yarrick/iodine)** - A DNS tunnel that allows IPv4 data through DNS servers
- **[dnscat2](https://github.com/iagox86/dnscat2)** - A tool for creating encrypted command-and-control channels over DNS

These tools demonstrate the real-world complexity of DNS exfiltration techniques and highlight why comprehensive detection systems—whether custom-built like the one in this project, or enterprise and commercial products—are essential for modern network security. Like tests, security controls should be tested to ensure they can be seen to both pass and equally important, fail.
