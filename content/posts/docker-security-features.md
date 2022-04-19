---
author: "Andy Rea"
title: "Distroless NGINX with a readonly filesystem"
date: 2022-03-22
draft: true
description: "A distroless NGINX container running with a readonly filesystem"
image: "img-readonly.png"
tags: ["docker", "nginx", "linux", "shell", "security"]
---

1. Distroless containers
   1. Multi Stage Builds
   2. Copy not add
2. Readonly containers
3. Disable inter-container communication
   1. Use specific build context
4. Limit Resources
5. Drop all capabilities
   - No new privileges
6. Use Linux Security Module
7. Static Code Analysis
8. Sign and verify images
9. Use meta data labels