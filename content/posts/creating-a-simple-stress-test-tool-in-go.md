---
author: "Andy Rea"
title: "Creating a Simple Stress Test Tool in Go - Requirements"
date: 2019-06-09T13:43:46Z
draft: false
---

One of the tools that has really stuck in my mind over the years is the siege stress test tool [https://github.com/JoeDog/siege](https://github.com/JoeDog/siege).  It was really simple to use, give it a list of urls, add some command line arguments including concurrency, time etc... and it would begin testing those urls with really clear output.  At the end of the test run it would print out statistics for the entire test like requests per second, average response time etc...  One thing it also did was make a log of these statistics in tabular form in a file in the home directory which was really useful to compare performance against historical runs.  

To create a simple clone of some of siege functionality I want to create an application with the following requirements, each being a separate blog post in this mini series:

The application:

- [Part 1 (v0.1.0): must be continually built and published to github releases supporting Windows, Linux and Mac.](/posts/creating-a-simple-stress-test-tool-in-go-part-1/)
- [Part 2 (v0.2.0): must be a CLI.](/posts/creating-a-simple-stress-test-tool-in-go-part-2/)
- [Part 3 (v0.3.0): must accept a file arguement of urls to test.](/posts/creating-a-simple-stress-test-tool-in-go-part-3/)
- [Part 4 (v0.4.0): must support http verbs GET,POST,PUT,DELETE.](/posts/creating-a-simple-stress-test-tool-in-go-part-4/)
- [Part 5 (v0.5.0): must accept an argument to read the urls sequentially or at random.](/posts/creating-a-simple-stress-test-tool-in-go-part-5/)
- [Part 6 (v0.6.0): must accept an argument to configure the number of simulated users.](/posts/creating-a-simple-stress-test-tool-in-go-part-6/)
- [Part 7 (v0.7.0): must accept an argument to specify the number of iterations.](/posts/creating-a-simple-stress-test-tool-in-go-part-7/)
- [Part 8 (v0.8.0): must output the statistics for Transactions (The total number of requests made) after the test run.](/posts/creating-a-simple-stress-test-tool-in-go-part-8/)
- [Part 9 (v0.9.0): must output the statistics for Availability (1 - (Number of errors / Transactions)) after the test run.](/posts/creating-a-simple-stress-test-tool-in-go-part-9/)
- [Part 10 (v0.10.0): must output the statistics for Elapsed Time (The total time the test took to run) after the test run.](/posts/creating-a-simple-stress-test-tool-in-go-part-10/)
- [Part 11 (v0.11.0): must output the statistics for Data Transferred (The total number of bytes received from the server) after the test run.](/posts/creating-a-simple-stress-test-tool-in-go-part-11/)
- must output the statistics for Response Time (The average response time in ms) after the test run.
- must output the statistics for Transaction rate (The number of requests per second) after the test run.
- must output the statistics for Concurrency (The number of simulataneous connections) after the test run.
- must output the statistics for Throughput (The average number of bytes of bytes received from the server per second) after the test run.
- must output the statistics for Successful transactions after the test run.
- must output the statistics for Failed transactions after the test run.
- must output the statistics for Longest transaction after the test run.
- must output the statistics for Shortest transaction after the test run.

The full source code for this series can be found [https://github.com/reaandrew/surge.git](https://github.com/reaandrew/surge.git) with a tag for each of the above versions for you to checkout the source code for each of the steps.

