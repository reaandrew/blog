---
title: "Creating a Simple Stress Test Tool in Go Part 1"
date: 2019-06-09T14:43:46Z
draft: false
---

## Must be continually built and published to github releases supporting Windows, Linux and Mac

The first part is to create a new repository and setup the continuous integration environment which I will use github and circleci for respectively.  I will name the repository and the project `surge`.  [https://github.com/reaandrew/surge.git](https://github.com/reaandrew/surge.git)

To begin with I will create a simple hello world application to flush the pipeline with circleci.

```go
package main

import "fmt"

func main() {
	fmt.Println("vim-go")

}
```

I also want a test which will fail at first but it will show the test part of the CI working.

```go
package main_test

import "testing"

func TestHelloWorld(t *testing.T) {
	t.Fatal("not implemented")
}
```

I have used the `.gitignore` file from the github collection ([https://github.com/github/gitignore/blob/master/Go.gitignore](https://github.com/github/gitignore/blob/master/Go.gitignore)) and added the name of executable too.

Next I will create the circle ci config which will test, build and deploy the application to github.  You must setup CircleCI and give it permission to your github for this step, their walkthrough is very good and actually gives you a starter build script straight to your clipboard.

```yaml
# Golang CircleCI 2.0 configuration file
version: 2
jobs:
  build:
    docker:
      - image: circleci/golang:1.12
    working_directory: /go/src/github.com/reaandrew/surge
    steps:
      - checkout
      - run: go get -v -t -d ./...
      - run: go test -v ./...
      - run: go build 
```

This fails as expected with `not implemented`, changing the test to something which will now pass is all I need for this part, I have not put any functionality in so a `1==1` assertion will do just fine.  I am using the testify package for the assertions ([https://github.com/stretchr/testify](https://github.com/stretchr/testify)).

```go
package main_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestHelloWorld(t *testing.T) {
	assert.Equal(t, 1, 1)
}
```

The last step is to upload the executable to github releases and each part of this series will increment the minor version until the last post when the version will be `v1.0.0`.  There are a couple of tools which I will use for this, the first is a cross compile tool [https://github.com/Songmu/goxz](https://github.com/Songmu/goxz) which will simplify the task of cross compiling the application for Windows, Linux and Mac; and the second is a tool to simplify the process of tagging the repository and uploading the artefacts as a release to github [https://github.com/tcnksm/ghr](https://github.com/tcnksm/ghr).  I need to update the build step in the CI config to use `goxz` now instead of the vanilla `go build`, then I will add a step to invoke `ghr` as per the example on the github page.  

```yml
# Golang CircleCI 2.0 configuration file
version: 2
jobs:
  build:
    docker:
      - image: circleci/golang:1.12
    working_directory: /go/src/github.com/reaandrew/surge
    steps:
      - checkout
      - run: go get -u github.com/tcnksm/ghr
      - run: go get github.com/Songmu/goxz/cmd/goxz
      - run: go get -v -t -d ./...
      - run: go test -v ./...
      - run: goxz -d out
      - run: ghr v0.1.0 out
```

If you have not set the environment variable `GITHUB_TOKEN` in the Circle CI console `ghr` will error so this must be set.  Once this has built the releases will be uploaded and the repository will be tagged.

