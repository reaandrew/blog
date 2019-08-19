---
author: "Andy Rea"
title: "Enumerating Github Repositories in Bash"
date: 2019-08-19
draft: false
---

I needed to get a list of all the repositories for a specific Github Organisation.  Github limits the page size which you have use which ruled out a single call with a large value.  I was also writing this routine in bash and less is more as they say.

My approach was very simplistic in that it simply tried an incrementing value for next page and if the response was empty then the end of the list had been reached.  I used [https://stedolan.github.io/jq/](https://stedolan.github.io/jq/) to parse the response and used the `-e` and `-r` options which set the exit status code and also prevent each line being wrapped in quotes.

```shell
page=1
pageSize=100

:>repos.txt

while true
do
  curl -H "Authorization: Bearer $GITHUB_TOKEN" "https://api.github.com/orgs/$ORG/repos?per_page=$pageSize&page=$page" | jq -er '.[] | .clone_url' >> repos.txt || break
  ((page++))
done
```
