---
title: "Creating a Simple Stress Test Tool in Go Part 2"
date: 2019-06-10T20:22:20Z
draft: true
---

## Must be a CLI

- Chose Cobra as the library since it is used by a lot of popular applications.
- Generated the initial root cmd.
- Changed the visibility of the RootCmd so it could be tested.
- Reviewed how best to test the cobra app and copied a couple of methods to achieve this.
- Added a simple test to comfirm that the program works with a "Hello, World!" output message.
- Updated the circle ci config to use v0.2.0 but there should be a better way of doing this.
- Added `GOOS=windows go get -u github.com/spf13/cobra` as an extra step which is required for the Windows build (found the solution here : https://github.com/spf13/cobra/issues/250)

![/images/v0.2.0-github-releases.png](/images/v0.2.0-github-releases.png)
