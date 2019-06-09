---
author: "Andy Rea"
date: 2019-06-08
title: Getting back into blogging with Hugo!
draft: false
---

This is a first blog post whilst I get this site back up and running using Hugo.

The deployment option which I am using is the one where you use one github repository to store all the source and raw material and another github repository for the generated artefacts which are then published on github pages.  There other ways of doing this (e.g. using a branch), which I tried, but I settled with the two repos approach.

I am using Circle CI for the Hugo generation and git publishing workflow (the config is below) and github pages for the hosting of the blog.  The theme of the site is called `book` and I chose it because of its simplicity and focus it gives to the content.  For the editing I am using Vim with several plugins including `Goyo` which gives me a distraction free view whilst I write.  I got the original version of this circleci config.yml from [https://willschenk.com/articles/2018/automating_hugo_with_circleci/](https://willschenk.com/articles/2018/automating_hugo_with_circleci/) but I have altered it to use submodules and separated some of the comands to give them some context.

```yaml
version: 2
jobs:
  build:
    docker:
      - image: cibuilds/hugo:latest
    working_directory: ~/hugo
    environment:
      HUGO_BUILD_DIR: ~/hugo/public
      REPO: git@github.com:reaandrew/reaandrew.github.io.git
    steps:
      - run: apk update && apk add git
      - checkout
      - run: git submodule sync && git submodule update --init
      - run: 
          name: "Clean refresh of the sub module"
          command: |
            git rm -f $HUGO_BUILD_DIR > /dev/null 2>&1 || :
            rm -rf .git/modules/$HUGO_BUILD_DIR
      - run: git submodule add -b master $REPO $HUGO_BUILD_DIR
      - run: HUGO_ENV=production hugo -t book -v -d $HUGO_BUILD_DIR
      - run: 
          name: "Set the git identity"
          command: |
            git config --global user.email "email@andrewrea.co.uk"
            git config --global user.name "CircleCI"
      - run: 
          name: "Commit the changes"
          command: |
            cd $HUGO_BUILD_DIR
            git add --all
            git commit -m "rebuilding site `date`"
            git push
```

