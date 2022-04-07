---
author: "Andy Rea"
title: "Limiting Docker Resources"
date: 2022-04-07
draft: true
description: "Limiting CPU and Memory with Docker"
image: "loadtest.png"
tags: ["docker", "nginx", "linux", "shell", "security"]
---

## TL;DR;

I use locust to load test two different configurations of these containers to show the effect of customizing and constraining the resources used by the containers.  This is recommended good practise for security as this can help to prevent denial of service on the host machine if the containers are overrun.

## Longer version

> The best way to avoid DoS attacks is by limiting resources. You can limit memory, CPU, maximum number of restarts (--restart=on-failure:<number_of_restarts>), maximum number of file descriptors (--ulimit nofile=<number>) and maximum number of processes (--ulimit nproc=<number>).

[https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-7-limit-resources-memory-cpu-file-descriptors-processes-restarts](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-7-limit-resources-memory-cpu-file-descriptors-processes-restarts)

For this post I am going to constrain the containers CPU and Memory and then use Locust to generate some load and see some difference in performance.  I will configure values for CPU, Memory, File Descriptors, Processes and failure restarts.  

My intention is not to test docker here it is simply to show the effect of using these methods.

**NOTE:**  It is useful to test different values for these configurations against your performance targets and requirements in order to find a sweet spot as it might not be immediately obvious what values you need from the outset.

I will update the Makefile and use the following values for two runs of this test:

Run 1 the Makefile will use:

```Makefile
--restart=on-failure:3
--ulimit nofile=4096
--ulimit nproc=50
--memory="1g" 
--cpus="0.5" 
```

Run 2 the Makefile will use:

```Makefile
--restart=on-failure:3 
--ulimit nofile=4096 
--ulimit nproc=50 
--memory="1g" 
--cpus="2" 
```

The test will hit the list endpoint of the todos which will return `[]` every time as there are no todos.  A better load test would exercise different endpoints to test the different functionality under load.

For the load test I am using Locust which is a popular and flexible tool for load testing. [](https://locust.io/).  Here is the locustfile:

```python
from locust import HttpUser, task, constant

class TodosApiUser(HttpUser):
    wait_time = constant(0)

    @task
    def list_todos(self):
        self.client.get('/todos')
```

Here are the results with 0.5 CPU and 1GB Memory

![/images/constrained_cpu.png](/images/constrained_cpu.png)

Here are the results with 2 CPU and 1GB Memory

![/images/less_constrained_cpu.png](/images/less_constrained_cpu.png)

The updated Makefile is below.  To make it more convenient I have added another target (**loadtest_1m**) to run a load test with 60 users, ramping up 2 users every second and to run for 1 minute.  Each of the two tests I did:

```shell
make kill
make run
```

The Makefile

```Makefile
.PHONY: build
build:
	docker build -t reaandrew/nginx-secure ./proxy
	(cd todos && go build -o todos main.go)
	docker build -t reaandrew/todos-secure ./todos

.PHONY: run
run: build
	docker network create --driver bridge appz

	docker run --name todos-secure --read-only \
		--network appz \
		--restart=on-failure:3 \
		--ulimit nofile=4096 \
		--ulimit nproc=50 \
		--memory="1g" \
        --cpus="2" \
		-d -t reaandrew/todos-secure

	docker run --name nginx-secure --read-only \
		--mount type=tmpfs,destination=/tmp/proxy_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/client_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/fastcgi_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/uwsgi_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/scgi_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/nginx,tmpfs-size=1m \
		--network appz \
		--restart=on-failure:3 \
		--ulimit nofile=4096 \
		--ulimit nproc=50 \
		--memory="1g" \
		--cpus="2" \
		-d -p 8080:8080 -t reaandrew/nginx-secure


.PHONY: kill
kill:
	docker kill nginx-secure 2> /dev/null || :
	docker rm nginx-secure 2> /dev/null || :
	docker kill todos-secure 2> /dev/null || :
	docker rm todos-secure 2> /dev/null || :
	docker network rm appz 2> /dev/null || :


.PHONY: logs
logs:
	docker logs nginx-secure
	docker logs todos-secure

.PHONY: loadtest_1m
loadtest_1m:
	locust --headless \
		-u 60 \
		-r 2 \
		-t 1m \
		-f loadtest/locustfile.py \
		--host http://localhost:8080 \
		--html "loadtest_report_$(shell date +"%Y%m%d%H%M%S").html"
```