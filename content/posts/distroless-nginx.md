---
author: "Andy Rea"
title: "Distroless NGINX"
date: 2022-03-21
draft: false
---

## TL;DR;

I created a basic version of a distroless NGINX container [https://github.com/reaandrew/nginx-security](https://github.com/reaandrew/nginx-security)  It has less then 10 running processes and is less than 30MB in size.  It is based on `nginxinc/nginx-unprivileged` and `gcr.io/distroless/base-debian10:nonroot`.

## Longer version

### Distroless Container Images
I wanted to understand how I could reduce a few things with docker containers including image size, running 
processes, libraries, tools etc... with the goal of having only what is required to run a given application.  (The size of the actual binary is not in-scope for this as it is the underlying OS I am concentrating on.)  

This is where `distroless container images` come in.

>"Distroless" images contain only your application and its runtime dependencies. They do not contain package managers, shells or any other programs you would expect to find in a standard Linux distribution.

https://github.com/GoogleContainerTools/distroless

Why should you use them?

>Restricting what's in your runtime container to precisely what's necessary for your app is a best practice employed by Google and other tech giants that have used containers in production for many years. It improves the signal to noise of scanners (e.g. CVE) and reduces the burden of establishing provenance to just what you need.

https://github.com/GoogleContainerTools/distroless

### Finding something useful to build

I then needed a subject to try this out and a subject that was more that just `hello world` as I wanted some real value out of it at the end which actually supports some work I am doing.  For this I chose `NGINX`.

One of the difficulties with making distroless container images is understanding which files are required to run the app.  Some apps may only have a single file, some may have many (as is the case with NGINX).  

Another difficulty is that when building the distroless image, you literally have no OS tool to use i.e. can't create users, can't create directories - the tools simply aren't there.  You have to rely on the Dockerfile declarations and shift all the creation stuff into an earlier stages.

```dockerfile
FROM nginxinc/nginx-unprivileged as build

FROM gcr.io/distroless/base-debian10:nonroot

COPY --from=build /lib/x86_64-linux-gnu/libdl.so.2 /lib/x86_64-linux-gnu/libdl.so.2
COPY --from=build /lib/x86_64-linux-gnu/libc.so.6 /lib/x86_64-linux-gnu/libc.so.6
COPY --from=build /lib/x86_64-linux-gnu/libz.so.1 /lib/x86_64-linux-gnu/libz.so.1
COPY --from=build /lib/x86_64-linux-gnu/libcrypt.so.1 /lib/x86_64-linux-gnu/libcrypt.so.1
COPY --from=build /lib/x86_64-linux-gnu/libpthread.so.0 /lib/x86_64-linux-gnu/libpthread.so.0

COPY --from=build /lib64/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2

COPY --from=build /usr/lib/x86_64-linux-gnu/libssl.so.1.1 /usr/lib/x86_64-linux-gnu/libssl.so.1.1
COPY --from=build /usr/lib/x86_64-linux-gnu/libpcre2-8.so.0 /usr/lib/x86_64-linux-gnu/libpcre2-8.so.0
COPY --from=build /usr/lib/x86_64-linux-gnu/libcrypto.so.1.1 /usr/lib/x86_64-linux-gnu/libcrypto.so.1.1

COPY --from=build /usr/sbin/nginx /usr/sbin/nginx
COPY --from=build /var/log/nginx /var/log/nginx
COPY --from=build /etc/nginx /etc/nginx
COPY --from=build /usr/share/nginx/html /usr/share/nginx/html

COPY --from=build /etc/passwd /etc/passwd
COPY --from=build /etc/group /etc/group


USER nginx
ENTRYPOINT ["/usr/sbin/nginx", "-g", "daemon off;"] 
```

Some points about the above Dockerfile.

1. I have two stages with the first being an unprivileged build by the NGINX team.  Depending on where your trust boundaries are you may even want to build this part yourself or take a copy of the Dockerfile to build but for my purposes this worked out great.
2. There seems to be a lot of specific file references which I am picking out and copying over from one stage to the second.  You could do this by trial and error but a more efficient way is to use the `ldd` tool.

```shell
root@c5eec8fbc239:/# ldd $(which nginx) 
        linux-vdso.so.1 (0x00007fffb4dd2000)
        libdl.so.2 => /lib/x86_64-linux-gnu/libdl.so.2 (0x00007fed8f35d000)
        libpthread.so.0 => /lib/x86_64-linux-gnu/libpthread.so.0 (0x00007fed8f33b000)
        libcrypt.so.1 => /lib/x86_64-linux-gnu/libcrypt.so.1 (0x00007fed8f300000)
        libpcre.so.3 => /lib/x86_64-linux-gnu/libpcre.so.3 (0x00007fed8f28d000)
        libssl.so.1.1 => /usr/lib/x86_64-linux-gnu/libssl.so.1.1 (0x00007fed8f1fa000)
        libcrypto.so.1.1 => /usr/lib/x86_64-linux-gnu/libcrypto.so.1.1 (0x00007fed8ef06000)
        libz.so.1 => /lib/x86_64-linux-gnu/libz.so.1 (0x00007fed8eee7000)
        libc.so.6 => /lib/x86_64-linux-gnu/libc.so.6 (0x00007fed8ed22000)
        /lib64/ld-linux-x86-64.so.2 (0x00007fed8f4ab000)
```

3. I have copied some directories from the NGINX installation since I cant create them in the distroless container.
4. I copy the user and group permissions over.
5. I set the Dockerfile user to be `nginx`.
6. I start NGINX in a foreground process without the daemon and rely on Docker to manager the service lifecycle.

### Results

It has just as many processes as the `nginxinc/nginx-unprivileged` image does.

```shell
~/Development/nginx-security main !1 ❯ docker top nginx-secure
UID                 PID                 PPID                C                   STIME               TTY                 TIME                CMD
systemd+            109226              109205              0                   13:06               ?                   00:00:00            nginx: master process /usr/sbin/nginx -g daemon off;
systemd+            109259              109226              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109260              109226              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109261              109226              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109262              109226              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109263              109226              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109264              109226              0                   13:06               ?                   00:00:00            nginx: worker process

```

The `nginxinc/nginx-unprivileged` version.

```shell
~/Development/nginx-security main !1 ❯ docker top happy_austin
UID                 PID                 PPID                C                   STIME               TTY                 TIME                CMD
systemd+            109461              109441              0                   13:06               ?                   00:00:00            nginx: master process nginx -g daemon off;
systemd+            109523              109461              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109524              109461              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109525              109461              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109526              109461              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109527              109461              0                   13:06               ?                   00:00:00            nginx: worker process
systemd+            109528              109461              0                   13:06               ?                   00:00:00            nginx: worker process
```

It is over 100MB smaller than the `nginxinc/nginx-unprivileged` version.

```shell
~/Development/nginx-security main ❯ docker images      
REPOSITORY                                 TAG       IMAGE ID       CREATED             SIZE
reaandrew/nginx-secure                     latest    c3230b0acdf8   About an hour ago   27.4MB
nginxinc/nginx-unprivileged                latest    b85bccd0d388   3 days ago          142MB
```

### Running it

```shell
docker run --name nginx-secure -d -p 8080:8080 -t reaandrew/nginx-secure
```


The result seen from an elinks browser:

```shell
elinks http://localhost:8080
```

<img src="/images/img.png" class="image" />