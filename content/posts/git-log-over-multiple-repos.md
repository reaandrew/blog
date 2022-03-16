---
author: "Andy Rea"
title: "Git log over multiple local repos"
date: 2022-03-16
draft: false
---

I find my self at work executing a for loop in bash in order to get some information out of multiple git repos in one hit.  This time I thought it would be useful to create a small script (the thing I keep executing) and to invoke it from a git alias.

First step is to create the alias.  I have called it `dlog` as in `distributed log command`.

```shell
git config --global alias.dlog '!bash ~/scripts/git/gitdlog.sh'
```

Next make sure the directory exists.

```shell
mkdir -p ~/scripts/git/
```

Finally add the following to the file `~/scripts/git/gitdlog.sh`

```shell
#!/usr/bin/env bash
find "$GIT_PREFIX./" -name ".git" | 
  while read -r line;
  do
    (cd "${line/.git/}" && git --no-pager log "$@")
  done 
```

Now `git dlog` can be used in place of `git log` but still using the same command line flags and arguments as this script applied those flags and arguments you supply once to each invocation of `git log`.  The script uses `find` command to get all the `.git` directories and return the parent directory.  A sub shell is then used to run `git log` and output as normal.
