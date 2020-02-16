---
title: "Creating a Simple Stress Test Tool in Go Part 6"
date: 2019-10-24T06:00:00Z
draft: true
---

## Must accept an argument to configure the number of simulated users

To simulate multiple different concurrent virtual users this feature makes use of goroutines.  Once the urls have been read into an array, a new goroutine is created for every virtual user according to the number specified in the configuration.  To make sure the execution does not end immediately due to the use of goroutines I added in a Semaphore (in go sync.WaitGroup) which I wait on at the end; each goroutine adds one to the wait group and also signals when it is done.  Once the number of signals equals the number added to the wait group, the latch is realeased and the program ends.

```go
type Surge struct {
  UrlFilePath string
  Random      bool
  WorkerCount int
}

func (surge Surge) execute(lines []string) {
  var wg sync.WaitGroup
  for i := 0; i < surge.WorkerCount; i++ {
          wg.Add(1)
          go func(linesValue []string) {
                  for _, line := range linesValue {
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

I added another argument in the CLI configuration, this time an int var.

```go
RootCmd.PersistentFlags().IntVarP(&workerCount, "worker-count", "c", 1, "The number of concurrent virtual users")
```

To test this, a single url was used and I asserted that 5 urls where visisted corresponding to the number of workers I specified.

```go
func TestSupportForConcurrentWorkers(t *testing.T) {
  file := CreateTestFile([]string{
    "http://localhost:8080/1",
  })  
  defer os.Remove(file.Name())

  var concurrentWorkerCount = 5 
  var count int 
  srv := startHTTPServer(func(r http.Request) {
    count++
  })  
  defer srv.Shutdown(context.TODO())

  output, err := executeCommand(cmd.RootCmd, "-u", file.Name(), "-c", strconv.Itoa(concurrentWorkerCount))
  assert.Nil(t, err, output)
  assert.Equal(t, count, concurrentWorkerCount)
}
```
