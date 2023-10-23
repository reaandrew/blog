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

## Running the tool

```shell
cloud-init-decoder-linux-amd64 --provider aws
```

If you have plain old user data encoded or not, then a file called userdata is generated inside a directory matching the instance which it came from.  The content of the file will match the content of the user data.

```text
├── output
│   └── i-0abcd1234a56789ef
│       └── userdata
```

If you have used `cloud-init` then the output will contain the nested directory structure contained within another directory to reflect the instance which it was pulled from like above.

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

To read more about `clout-init` see [https://cloudinit.readthedocs.io/en/latest/](https://cloudinit.readthedocs.io/en/latest/)


## Why this could be of use

User data can include scripts, configuration files, and other important details. It's often in different formats, including cloud-init, making it difficult to interpret.

Sure you can do this via the command line using the AWS CLI, gunzip, base64 etc... but it gets more complicated when you need to deal with MIME and cloud-init data.

For debugging, auditing, or even just understanding what's going on, you need a way to decode and look into this data.  For example, when the data has been decoded you could use one of the many secret detection tools to run over the output folder to check for sensitive data inside the user data.

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

