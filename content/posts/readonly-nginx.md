---
author: "Andy Rea"
title: "Distroless NGINX with a readonly filesystem"
date: 2022-03-22
draft: false
description: "A distroless NGINX container running with a readonly filesystem"
image: "img.png"
tags: ["docker", "nginx", "linux", "shell", "security"]
---

## TL;DR;

Following on from my previous post where I created a distroless NGINX container, this post adds on to that ands make the file system readonly using the docker `--read-only` flag.  [https://github.com/reaandrew/nginx-security](https://github.com/reaandrew/nginx-security)

## Longer version

### Finding out which directories need to be writeable

Making the filesystem readonly in Docker still requires you to be explicit about which directories you are allowing to be writeable (depending on the application requirements) i.e. Make everything readonly **EXCEPT** for these directories.  It is similar in the context of proxies, in that it is always safer to create an ALLOWED list rather than a BLOCKED list; with the latter needing constant updating to keep up with threats.

I used trial and error this time to understand which directories needed to be writable.  With more specific configuration these could probably be reduced, as looking at the directories, they seem really specific to the type of upstream proxy they are intended to support; but for this example I simply kept running the container, mapping the volume it complained about and then retried it until I came up with the required list of volumes.

### Tweak the base image

The only other thing I needed to change was where the PID file got created. Currently, it is created directly in the /tmp directory which would mean I would have to map the entire /tmp directory to make it work which is too much in terms of scope; I want more control than that. So I followed a similar approach to the Dockerfile from `nginxinc/nginx-unprivileged` (which used sed to change the configuration) and simply put the path inside a child directory of nginx. This means I now can create a tempfs just for that directory and know exactly which directories will be writeable.

```Dockerfile
FROM nginxinc/nginx-unprivileged as build

RUN sed -i 's,/tmp/nginx.pid,/tmp/nginx/nginx.pid,' /etc/nginx/nginx.conf

...
```

### Makefile for convenience

I create a Makefile to save time keep destroying the container, rebuilding and running it.  It was also helpful as the command line to run this started to get a little long. 

```Makefile
.PHONY: build
build:
	docker build -t reaandrew/nginx-secure .

.PHONY: run
run: build
	docker run --name nginx-secure --read-only \
		--mount type=tmpfs,destination=/tmp/proxy_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/client_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/fastcgi_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/uwsgi_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/scgi_temp,tmpfs-size=2m \
		--mount type=tmpfs,destination=/tmp/nginx,tmpfs-size=1m \
		-d -p 8080:8080 -t reaandrew/nginx-secure

.PHONY: kill
kill:
	docker kill nginx-secure 2> /dev/null || :
	docker rm nginx-secure 2> /dev/null || :

.PHONY: logs
logs:
	docker logs nginx-secure
```

The above is mapping 2MB for the volumes, this would obviously have to be configured to your requirements (I reduced the PID volume to 1M as I dont know how small you can go).

### tempfs not mapped volumes

I am using tempfs here which (from my understanding) gets mapped to memory.  I could have used volumes but I have no need for the data which the container is writing.  The only data I am interested in is the logs and these are dealt with by the underlying container I used in stage 1 of the build [https://github.com/nginxinc/docker-nginx-unprivileged/blob/main/stable/debian/Dockerfile](https://github.com/nginxinc/docker-nginx-unprivileged/blob/main/stable/debian/Dockerfile).

In this they map the access and error log to stdout and stderr respectively which I really like.  Here is the snippet from the above Dockerfile link.

```Dockerfile
# forward request and error logs to docker log collector
    && ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log
```

### So far so good

So far this is a distroless container with a readonly filesystem.  This is giving me a much greater security posture than if I were simply running this on a vanilla OS or without a readonly filesystem and it didnt take that much effort.

The other thing I like about this and Docker, is that I can use the same methods and API to secure other applications in the same way.  OK they are going to differ on required volumes, ports, libraries etc... but ultimately they will all arrive at the same outcome; Distroless and readonly.


The result seen from an elinks browser:

```shell
elinks http://localhost:8080
```

![](/images/img-readonly.png)