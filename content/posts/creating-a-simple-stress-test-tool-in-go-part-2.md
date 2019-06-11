---
title: "Creating a Simple Stress Test Tool in Go Part 2"
date: 2019-06-10T20:22:20Z
draft: false
---

## Must be a CLI

I chose the Cobra [https://github.com/spf13/cobra](https://github.com/spf13/cobra) package for the CLI since it is used by a lot of popular applications and it made things a lot simpler than working with the raw command line arguments, including testing etc...  In my opinion this is one of those decisions better made early so more focus can be given to the actual requirements.  To get started I literally installed the generator and package with two separate `go gets` as per the documentation which created a default root command which I could build on.  The generated code also went into its own package called `cmd` which was useful.

In order to test this with a package name for test i.e. `cmd_test` rather than `cmd`, I changed the visibility of the RootCmd which was generated.  I also reviewed how best to test a cobra app and copied a couple of methods into this app as they were not exported by the Cobra package.  I added a simple test to comfirm that the program works by asserting "Hello, World!" was written to standard out.

```go
package cmd_test

import (
  "bytes"
  "testing"

  "github.com/reaandrew/surge/cmd"
  "github.com/spf13/cobra"
  "github.com/stretchr/testify/assert"

    )

func executeCommand(root *cobra.Command, args ...string) (output string, err error) {
  _, output, err = executeCommandC(root, args...)
  return output, err

}

func executeCommandC(root *cobra.Command, args ...string) (c *cobra.Command, output string, err error) {
  buf := new(bytes.Buffer)
  root.SetOut(buf)
  root.SetErr(buf)
  root.SetArgs(args)

  c, err = root.ExecuteC()

  return c, buf.String(), err


}

func TestHelloWorld(t *testing.T) {
  output, err := executeCommand(cmd.RootCmd)

  assert.Nil(t, err, "Unexpected error: %v", err)
  assert.Equal(t, "Hello, World!\n", output)

}
```

I updated the version of the application into the circle ci config to v0.2.0 and lso had to add one extra line to get the Cobra app to cross compile for windows (`GOOS=windows go get -u github.com/spf13/cobra` [https://github.com/spf13/cobra/issues/250](https://github.com/spf13/cobra/issues/250))

