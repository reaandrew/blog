---
author: "Andy Rea"
title: "Creating a Simple Stress Test Tool in Go - Requirements"
date: 2019-06-09T13:43:46Z
draft: false
---

## Introduction

One of the tools that has really stuck in mind over the years is the siege stess test tool (TODO: put a link to the tool).  It was really simple to use, give it a list of urls, add some command line arguments including concurrency, time etc... and it would begin testing those urls with really clear output.  At the end of the test run it would print out statistics for the entire test like requests per second, average response time etc...  One thing it also did was make a log of these statistics in tabular form in a file in the home directory which was really useful to compare performance against historical runs.  

To create a simple clone of some of siege functionality I want to create an application with the following requirements, each being a separate blog post in this mini series:

The application:

- must be continually built and published to github releases supporting Windows, Linux and Mac.
- must be a CLI.
- must accept a file arguement of urls to test.
- must only support the HTTP GET.
- must accept an argument to read the urls sequentially or at random.
- must accept an argument to configure the number of simulated users.
- must accept an argument to specify the number of iterations.
- must output the same statistics as siege into standard out after the test run:
  - Transactions (The total number of requests made)
  - Availability (1 - (Number of errors / Transactions))
  - Elapsed Time (The total time the test took to run)
  - Data Transferred (The total number of bytes received from the server)
  - Response Time (The average response time in ms)
  - Transaction rate (The number of requests per second)
  - Concurrency (The number of simulataneous connections)
  - Throughput (The average number of bytes of bytes received from the server per second)
  - Successful transactions
  - Failed transactions
  - Longest transaction
  - Shortest transaction

