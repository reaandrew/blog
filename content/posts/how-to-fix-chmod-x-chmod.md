---
author: "Andy Rea"
title: "How to fix chmod -x /usr/bin/chmod"
date: 2021-10-05
draft: false
tags: ["shell", "bash", "linux"]
---

I was asked this question years ago (and didnt know the answer then) and just recently I found a question and answer on Stackoverflow which basically solved this using python so I thought it would be fun to draw it out a little further.

Once you have executed `chmod -x /usr/bin/chmod` (or in other words you have removed executable permissions from a common tool that is used to change the permissions of files including execution) you will no longer be able to use it to make things executable including `chmod`.

The general answer to this question is that you need to flip the correct permission bits in order to make it executable again.  [chmod](https://github.com/coreutils/coreutils/blob/master/src/chmod.c) itself is a tool which uses underlying system calls to achieve its goal.  You can also do that with other languages very easily.

**NOTE: I am using the `stat` command to output the file access rights in both octal (%a) and human readable (%A) format.**

|| command | output| |
|---|---|---|---|
|1| ````stat -c "%a/%A" /usr/bin/chmod ```` | `755/-rwxr-xr-x` | |
|2| ````chmod -x /usr/bin/chmod```` || No output from this command |
|3| `chmod` | `bash: /usr/bin/chmod: Permission denied` | Not executable any more |
|4| ````stat -c "%a/%A" /usr/bin/chmod ```` | `644/-rw-r--r--` | So now the user, group or any others (nothing) can execute this|
|5| ```` python3 -c "import os,stat; os.chmod('/usr/bin/chmod', os.stat('/usr/bin/chmod').st_mode \| 0o111)" ```` | | The answer from Stackoverflow just put into one line for the purposes of demonstration |
|6| ```` stat -c "%a/%A" /usr/bin/chmod ````| `755/-rwxr-xr-x` | The permissions are now back to how they were before removing them |



### Taking this a little bit further

How can we then replicate `chmod -x` when the above now achieves the same functionality as `chmod +x`?  Using bitwise operations again, the first step is to ensure the file is executable and then `XOR` the executable bits which will flip them back to zero as both will be `1`.

> The name XOR stands for “exclusive or” since it performs exclusive disjunction on the bit pairs. In other words, every bit pair must contain opposing bit values to produce a one
> 
> <span>Bartosz Zaczyński - https://realpython.com/python-bitwise-operators/</span>

```python
#!/usr/bin/env python3                                                                                                 

import os
import stat
import sys 

exe_perm=0o111
command = sys.argv[1]
filename= sys.argv[2]
result=os.stat(filename)

if command == "+x":
  os.chmod(filename, result.st_mode | exe_perm)
elif command == "-x":
  os.chmod(filename, (result.st_mode | exe_perm) ^ exe_perm)
```

I saved the above into a file called `pychmod` and then could use this whether or not the `chmod` utility was executable.  Here is a quick run through changing executable permissions with this toy script in the absence of chmod.

```shell
❯ chmod -x /usr/bin/chmod
chmod: changing permissions of '/usr/bin/chmod': Operation not permitted
❯ sudo !!
❯ sudo chmod -x /usr/bin/chmod
[sudo] password for andy: 
❯ chmod +x test.sh
zsh: permission denied: chmod
❯ ./pychmod +x test.sh
❯ stat -c "%a/%A" test.sh
775/-rwxrwxr-x
❯ stat -c "%a/%A" /usr/bin/chmod
644/-rw-r--r--
❯ ./pychmod +x /usr/bin/chmod
Traceback (most recent call last):
  File "./pychmod", line 13, in <module>
    os.chmod(filename, result.st_mode | exe_perm)
PermissionError: [Errno 1] Operation not permitted: '/usr/bin/chmod'
❯ sudo !!
❯ sudo ./pychmod +x /usr/bin/chmod
❯ stat -c "%a/%A" /usr/bin/chmod
755/-rwxr-xr-x
```

References:

[https://stackoverflow.com/questions/12791997/how-do-you-do-a-simple-chmod-x-from-within-python](https://stackoverflow.com/questions/12791997/how-do-you-do-a-simple-chmod-x-from-within-python)

[https://linuxize.com/post/understanding-linux-file-permissions/](https://linuxize.com/post/understanding-linux-file-permissions/)

[https://www.howtogeek.com/451022/how-to-use-the-stat-command-on-linux/](https://www.howtogeek.com/451022/how-to-use-the-stat-command-on-linux/)

[https://realpython.com/python-bitwise-operators/](https://realpython.com/python-bitwise-operators/)