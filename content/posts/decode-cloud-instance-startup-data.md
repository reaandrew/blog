---
author: "Andy Rea"
title: "Decode cloud instance start-up data"
date: 2023-10-23
draft: false
description: "A small tool to decode instance user data after it has been deployed so that it can be audited etc..."
image: "img.png"
tags: ["cloud", "security", "userdata", "aws", "devops", "devsecops", "cloud security"]
---

## TL;DR;

I created a small tool which can be used to gather the user data of all instances and decode them including support for cloud-init format.  [https://github.com/reaandrew/cloud-startup-data-decoder](https://github.com/reaandrew/cloud-startup-data-decoder)

## Longer version

If you're deploying cloud resources, chances are you've encountered the need to pass in some form of user data or startup scripts. Whether it's AWS, Azure, or GCP, they all offer a way to inject data into virtual machines upon startup. 

One of the gaps I have found is where sensitive data is pulled at time of CI, injected into the user data and subsequently stored as Launch Templates (in AWS).  The problem this tool is trying to help with is discovering whether there is anything sensitive being injected into the user data; in AWS this is stored in clear text albeit encoded.  Based on this the advice is not to store sensitive data inside of user data and also why many services in AWS do not record the value e.g. in Cloud Trail etc...

This is where this tool comes in, [https://github.com/reaandrew/cloud-startup-data-decoder](https://github.com/reaandrew/cloud-startup-data-decoder).

### Scenarios

#### Scenario 1:  Use of cloud-init

When using cloud-init you create a `cloud-config.yaml` file specifying different types of contents and associated commands.

Example (with just write files, more examples can be found here [https://cloudinit.readthedocs.io/en/latest/reference/examples.html](https://cloudinit.readthedocs.io/en/latest/reference/examples.html)):

```yaml
# cloud-config.yaml
write_files:
  # Write the configuration file to /etc/sample_app/
  - path: "/etc/sample_app/some-yaml.yaml"
    encoding: gz+b64
    content: H4sICCVEFGUAA3NvbWUteWFtbC55YW1sAE2PMQ/CIBCF9/6KC7sDtGrt6Gp0MnE013IqKUJDabX/XopiGoZ3fO+9C9zcoHxfZQArwK7TFKcaTThxbB7k3JShUU/Uv2CDPqq096ikqXug8VltbRszXnlNvALGd2XBEhAB7B2OBCd6wcU6Lf9eHryzhYPSGhCOtmmVudfKhYSh3pO8SvQ4725p4rMC9EMdL8BG1ANxtqAiUcG+HbHs5MnNl50i0fDkxlonlUFPv0+/K+AihqcK8iKx9SaxbZl9AKvS2JxOAQAA

  # Write the template file to /usr/share/sample_app/
  - path: "/usr/share/sample_app/some-text.txt"
    encoding: gz+b64
    content: H4sICAuIGWUAA3NvbWUtdGV4dC50eHQAK87PTVUoSa0oUcjMU8hILUoFACe2fIYRAAAA

runcmd:
  - ["/usr/local/bin/start.sh"]
```

Cloud-init has a utility to package this data up which turns this into a Multi-Part MIME message, as the following example shows:

```text
Content-Type: multipart/mixed; boundary="===============7114695260841183278=="
MIME-Version: 1.0

--===============7114695260841183278==
Content-Type: text/cloud-config; charset="utf-8"
MIME-Version: 1.0
Content-Transfer-Encoding: base64
Content-Disposition: attachment; filename="cloud-config.yaml"

IyBjbG91ZC1jb25maWcueWFtbAp3cml0ZV9maWxlczoKICAjIFdyaXRlIHRoZSBjb25maWd1cmF0
aW9uIGZpbGUgdG8gL2V0Yy9zYW1wbGVfYXBwLwogIC0gcGF0aDogIi9ldGMvc2FtcGxlX2FwcC9z
b21lLXlhbWwueWFtbCIKICAgIGVuY29kaW5nOiBneitiNjQKICAgIGNvbnRlbnQ6IEg0c0lDQ1ZF
RkdVQUEzTnZiV1V0ZVdGdGJDNTVZVzFzQUUyUE1RL0NJQkNGOS82S0M3c0R0R3J0NkdwME1uRTAx
M0lxS1VKRGFiWC9Yb3BpR29aM2ZPKzlDOXpjb0h4ZlpRQXJ3SzdURktjYVRUaHhiQjdrM0pTaFVV
L1V2MkNEUHFxMDk2aWtxWHVnOFZsdGJSc3pYbmxOdkFMR2QyWEJFaEFCN0IyT0JDZDZ3Y1U2TGY5
ZUhyeXpoWVBTR2hDT3RtbVZ1ZGZLaFlTaDNwTzhTdlE0NzI1cDRyTUM5RU1kTDhCRzFBTnh0cUFp
VWNHK0hiSHM1TW5ObDUwaTBmRGt4bG9ubFVGUHYwKy9LK0FpaHFjSzhpS3g5U2F4YlpsOUFLdlMy
SnhPQVFBQQoKICAjIFdyaXRlIHRoZSB0ZW1wbGF0ZSBmaWxlIHRvIC91c3Ivc2hhcmUvc2FtcGxl
X2FwcC8KICAtIHBhdGg6ICIvdXNyL3NoYXJlL3NhbXBsZV9hcHAvc29tZS10ZXh0LnR4dCIKICAg
IGVuY29kaW5nOiBneitiNjQKICAgIGNvbnRlbnQ6IEg0c0lDQXVJR1dVQUEzTnZiV1V0ZEdWNGRD
NTBlSFFBSzg3UFRWVW9TYTBvVWNqTVU4aElMVW9GQUNlMmZJWVJBQUFBCgpydW5jbWQ6CiAgLSBb
Ii91c3IvbG9jYWwvYmluL3N0YXJ0LnNoIl0=

--===============7114695260841183278==
Content-Type: text/x-shellscript; charset="utf-8"
MIME-Version: 1.0
Content-Transfer-Encoding: base64
Content-Disposition: attachment; filename="start.sh"

ZWNobyAiU3RhcnRlZCI=

--===============7114695260841183278==--
```

Finally when loading this cloud-init supports gzipped user data which is then base64 encoded, more details about this can be found here [https://cloudinit.readthedocs.io/en/latest/explanation/format.html](https://cloudinit.readthedocs.io/en/latest/explanation/format.html).  The resulting output is then:

```text
H4sICKnBLGUAA3VzZXJkYXRhAL2USY+jOBTH73yKqO6ptiFJhyrlEBYTEiCNAbPczFJAYpYJZIFP33R1qTQ96kOPRhpfbD37Lf+nn5/c1H1W93N3aLOXWXVlfdnSS/+lKh9Z+jqLm2ud0suwedr8ur5CuFiJS34F1gsI1wL/db3ZPHGmbqpzkl26sqlfZvAZcNx8/ieunPxLIX326L8krLmm86Sp38r8dZYU9NJl/ebp2r/N179L9RniQuvuLbvM1Tpp0rLOX2Yx7bLV4vOFUnZt05X9uyvte5oU1WR/nb2VLKtplW2e/p78eaAVe+I4fZBOsSbCSIanmF9W1E+umY/6eNsKScVARMTJ9mDJ2Bx0eXvSUTrQADN9h5vIkT58UphUCHDUF6+6FrWx5uWpts4NnoBwEMfQh/dYI29hIN2Ne5PrMsgTDQGqTOdSZKlm3hIe9Yn2YAGP7oksjlzMQ2YErIj9+8+KZP1HBbmukWvIi2fqL+tjKdXZpNk62R931i2uMYtre6WrOUgAU2wYIQ6fU2J76ujWUUkgmWSlWqrtFcslERnRaHve4KkQG8Da22dLOzpr3gGmkAAMsLAH1jm9myq8Ynf74EzAHg4kB6yh0pfFMBakFvMiNfno22FkyjFoTzEoFhFrsR3sBWdMPXzuTyHBHt0VpX1KLyZoXYoI4QxIePNsqd4OPUzlzFO/f/g7Uh9R1E0VOonQhnH1OKZnZGLeHnx1j6iKZAvogwv2SqREQgg93tXCJRd5xZAFbeMTycV8obgC7mMSwUiLDIqYSxXr7o6FmzIVWKMOEwUPrmcusQfPrlLIeESSWxcg8VDLEd/aHUBROjsTuv7yGCvenbpShbV+MSFzjRHRvF14PwyicQCopTt0csaidYR86fFoEbK2O3rISJk5cE5dfLMJkmz7txyB6B0RBKbzO2+T/abLIkwEfUKjKJLK+0SE+8nI+kegXt9JRarlK13Wb2lgDYZgNWGwZ9NexIHUTQAXyW47OYt95EAQBQUwarxIP3Di/jVPAdlj+A+c1NS3NKxwlisxByHJGXPBQ9gnvuiGrnSbevmXS7wFVZk52TTbs5hZRXuf7CXbQ5Kct0PqL0+xb6/kcpsbjhRz09d41z81+xT691tYseskC0zyJglWozOw+S/D6DHvioyxLrmUbf+/T6Oun4byc1dMQyjyrSYetqUn4CKZ2h3J+h8Lm8857js3myh88gUAAA==
```

**NOTE** Compression helps with the 16KB limit AWS has for User Data

> User data is limited to 16 KB, in raw form, before it is base64-encoded. The size of a string of length n after base64-encoding is ceil(n/3)*4.
> 
> https://docs.aws.amazon.com/AWSEC2/latest/WindowsGuide/instancedata-add-user-data.html

Given that one or more instances will then be created with this user data, you can then run this tool to gather and examine the data as per the realtive file structure they will haveinside the filesystem of the instances they are deployed on.

### Running the tool

```shell
cloud-init-decoder-linux-amd64 --provider aws
```

This will result in a directory being created called output (which is the default if you do not specify it) and the nested directory structure contained within a directory to relfect the instance which it was pulled from:

```text
├── output
│   └── i-0abcd1234a56789ef
│       ├── etc
│       │   └── sample_app
│       │       └── some-yaml.yaml
│       └── usr
│           └── share
│               └── sample_app
│                   └── some-text.txt
```

#### Scenario 2:  Plain old user data

If you user data is literally, 'Hello, World!' then the output from this tool will be a file called userdata and the content matching that of the userdata


## Why this could be of use

User data can include scripts, configuration files, and other important details. It's often in different formats, including cloud-init, making it difficult to interpret.

Sure you can do this via the command line using the AWS CLI, gunzip, base64 etc... but it gets more complicated when you need to deal with MIME and cloud-init data.

For debugging, auditing, or even just understanding what's going on, you need a way to decode and look into this data.

## Current Features

- AWS User Data Support: Decode and analyze user data in Amazon EC2 instances.
- Cloud-Init Support: Decode cloud-init formatted user data, widely used for cloud instance initialization.
- Plain User Data: Decode plain-text user data that doesn't follow the cloud-init format.

## Upcoming Features (TODO)

- Azure Support: Extend decoding capabilities to Azure's Custom Script Extension and user data.
- GCP Support: Extend decoding capabilities to include Google Cloud Platform's startup scripts and metadata.

## Usage Scenarios

- Cloud Migration: Easily understand startup configurations when migrating across cloud platforms.
- Security Auditing: Utilize this tool to audit startup scripts for any security vulnerabilities.
- Debugging: Decode and analyze startup data to debug instance initialization issues.

