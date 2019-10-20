---
title: "Creating a Simple Stress Test Tool in Go Part 5"
date: 2019-10-20T12:00:00Z
draft: false
---

## Must accept an argument to read the urls sequentially or at random

In siege one of the features was read the urls randomly using the `-i` flag which meant **internet mode**.  I thought it would be a bit more intuitive if I used `-r` and `--random` for the long hand flags.

I did some refactoring in this iteration and one helper function I created was to simply how I created the test files with a list of url lines.

```go
func CreateTestFile(lines []string) *os.File {
	fileContents := strings.Join(lines, "\n")
	file, err := ioutil.TempFile(os.TempDir(), "prefix")
	if err != nil {
		panic(err)
	}
	ioutil.WriteFile(file.Name(), []byte(fileContents), os.ModePerm)
	return file
}
```

I replaced all the use in the existing tests with this above.  I don't expect this to error and if it does I want everything to stop as it would be exceptional and I wanted to handle the error immediately as opposed to returning it.

It is not that simple to test for randomness in a way which is going to work 100% of the time.  I could have added more granular tests which assert that the Reader passed in is the reader which is executed, therefor when I pass a Random Reader in, it will be used.  This would then need unit tests on the Random Reader and then you are back to the question of "How do you truly test for random".  For my initial implementation I iterate 10 times and compare the resulting order with that of the sequential order and assert they are not equal.  Not perfect but this will do for now.

```go
func TestSupportForRandomOrder(t *testing.T) {
	urls := func() []string {
		returnUrls := []string{}
		for i := 0; i < 10; i++ {
			returnUrls = append(returnUrls, "http://localhost:8080/"+strconv.Itoa(i))
		}
		return returnUrls
	}()
	file := CreateTestFile(urls)
	defer os.Remove(file.Name())

	urlsVisited := []string{}
	srv := startHTTPServer(func(r http.Request) {
		urlsVisited = append(urlsVisited, r.RequestURI)
	})
	defer srv.Shutdown(context.TODO())

	output, err := executeCommand(cmd.RootCmd, "-u", file.Name(), "-r")
	assert.Nil(t, err, output)

	urlPaths := MapStrings(urls, func(value string) string {
		items := strings.Split(value, "/")
		return "/" + items[len(items)-1]
	})
	assert.NotEqual(t, urlsVisited, urlPaths)
}
```

The `MapStrings` function is a another custom one I made as I needed to extract the paths from the original urls to compare against as the path is the only part captured inside the web server.

```go
func MapStrings(array []string, delegate func(value string) string) (values []string) {
	for _, value := range array {
		values = append(values, delegate(value))
	}
	return
}
```
