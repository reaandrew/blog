---
title: "Creating a Simple Stress Test Tool in Go Part 7"
date: 2019-10-25T06:00:00Z
draft: false
---

## Must accept an argument to specify the number of iterations.

This feature provides the following functionality:

1.  If the number of iterations is not specified then the number of iterations will equal the number of lines in the url file.
2.  The number of iterations is per virtual user
3.  If the number of iterations is higher than the number of lines in the url file, the urls will be read from the beginning again until the number of iterations has been reached.

The main change for this was made in the SurgeClient's execute method where I have amended the for loop criteria.

```go
func (surge Surge) execute(lines []string) {
	var wg sync.WaitGroup
	for i := 0; i < surge.WorkerCount; i++ {
		wg.Add(1)
		go func(linesValue []string) {
			for i := 0; i < len(linesValue) || (surge.Iterations > 0 && i < surge.Iterations); i++ {
				line := linesValue[i%len(linesValue)]
				var command = HttpCommand{}
				var args = strings.Fields(line)
				command.Execute(args)
			}
			wg.Done()
		}(lines)
	}
	wg.Wait()
}
```

The part which makes the urls loop back around to the start is the modulus part `line := linesValue[i%len(linesValue)]`.

I then tested this with a single worker and then with multiple workers.

```go

func TestSupportForNumberOfIterations(t *testing.T) {
	file := CreateTestFile([]string{
		"http://localhost:8080/2",
	})
	defer os.Remove(file.Name())

	var concurrentWorkerCount = 1
	var iterationCount = 5
	var count int
	srv := startHTTPServer(func(r http.Request) {
		count++
	})
	defer srv.Shutdown(context.TODO())

	output, err := executeCommand(cmd.RootCmd, "-u", file.Name(),
		"-n", strconv.Itoa(iterationCount),
		"-c", strconv.Itoa(concurrentWorkerCount))
	assert.Nil(t, err, output)
	assert.Equal(t, count, iterationCount)
}

func TestSupportForNumberOfIterationsWithConcurrentWorkers(t *testing.T) {
	file := CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	var concurrentWorkerCount = 5
	var iterationCount = 5
	var count int
	srv := startHTTPServer(func(r http.Request) {
		count++
	})
	defer srv.Shutdown(context.TODO())

	output, err := executeCommand(cmd.RootCmd, "-u", file.Name(),
		"-n", strconv.Itoa(iterationCount),
		"-c", strconv.Itoa(concurrentWorkerCount))
	assert.Nil(t, err, output)
	assert.Equal(t, count, iterationCount*concurrentWorkerCount)
}
```

I added the Iterations property to the SurgeClient and also the new command line flag.

```go
RootCmd.PersistentFlags().IntVarP(&iterations, "number-iterations", "n", 1, "The number of iterations per virtual user")
```

Up to this point the help text of the application looks like the following.

```shell
A longer description that spans multiple lines and likely contains
examples and usage of using your application. For example:

Cobra is a CLI library for Go that empowers applications.
This application is a tool to generate the needed files
to quickly create a Cobra application.

Usage:
  surge [flags]

Flags:
      --config string           config file (default is $HOME/.surge.yaml)
  -h, --help                    help for surge
  -n, --number-iterations int   The number of iterations per virtual user (default 1)
  -r, --random                  Read the urls in random order
  -t, --toggle                  Help message for toggle
  -u, --urls string             The urls file to use
  -c, --worker-count int        The number of concurrent virtual users (default 1)
```
