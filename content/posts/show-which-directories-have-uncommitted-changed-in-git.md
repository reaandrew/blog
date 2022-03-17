---
author: "Andy Rea"
title: "Show which directories have uncommitted changes in git"
date: 2022-03-17
draft: false
---

Similar to my last post, when working with many different git repositories in a single directory, I want to quickly see which of the directories have changes which I have not yet comitted.  Another small script I made does this and I find it very useful.

First step is to create the alias.  I have called it `dchanges` as in `distributed changes command`.

```shell
git config --global alias.dchanges '!bash ~/scripts/git/gitdchanges.sh'
```

Next make sure the directory exists.

```shell
mkdir -p ~/scripts/git/
```

Finally add the following to the file `~/scripts/git/gitdchanges.sh`

```shell
#!/usr/bin/env bash

find "$GIT_PREFIX./" -mindepth 1 -maxdepth 1 -type d -not -path . |
  while read -r line;
  do
  printf "checking %s ..." "$line"
    (cd "$line" && \
      git status | grep -q 'nothing to commit' && printf " no changed detected" || printf " CHANGES DETECTED"
    )
    echo
  done

```

When ran in a directory you get similar output to the below:

```txt
~/Development ❯ git dchanges
checking ./preact-sandbox ... CHANGES DETECTED
checking ./ami-customer-website ... no changed detected
checking ./ami-browser ... no changed detected
checking ./blog ... no changed detected
checking ./schmokin ... no changed detected
checking ./rust ... CHANGES DETECTED
checking ./ami-bastion ... no changed detected
checking ./Dapper ... CHANGES DETECTED
checking ./node ... CHANGES DETECTED
checking ./.idea ... CHANGES DETECTED
checking ./aspnetcore ... CHANGES DETECTED
```

