---
title: "Creating a Simple Stress Test Tool in Go Part 10"
date: 2019-11-03T05:00:00Z
draft: true
---

## Must output the statistics for Elapsed Time (The total time the test took to run) after the test run

Like all tests which involve time as a variable it is always useful to fake time to make testing quicker and simpler.  The first part of this was to assert that the time was output correctly in the results.

```go
func TestOutputsElapsedTimeInHumanReadableForm(t *testing.T) {
	file := utils.CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	client := client.NewFakeHTTPClient()
	timer := utils.NewFakeTimer(1 * time.Minute)

	output, err := executeCommand(cmd.RootCmd, client, timer, "-u", file.Name(), "-n", "1", "-c", "1")

	assert.Nil(t, err)
	assert.Contains(t, output, "Elapsed Time: 1m0s\n")
}
```

I added an extra argument to the `executeCommand` method to supply the timer the timer implementation.  In the test I am setting what the elapsed time will be when queried.

The interface of the timer.

```go
package utils

import "time"

type Timer interface {
	Start()
	Stop() time.Duration
}
```

The implementation of the FakeTimer.  There is an extra method to change the value of the Elapsed Time.

```go
package utils

import "time"

type FakeTimer struct {
	elapsed time.Duration
}

func (timer *FakeTimer) SetElapsed(duration time.Duration) {
	timer.elapsed = duration
}

func (timer *FakeTimer) Start() {

}

func (timer *FakeTimer) Stop() time.Duration {
	return timer.elapsed
}

func NewFakeTimer(duration time.Duration) *FakeTimer {
	timer := &FakeTimer{}
	timer.SetElapsed(duration)
	return timer
}
```

In the main command I have defaulted this to be the DefaultTimer which uses actual time.  The convention I have been following for interfaces, fakes and reals is *Interface*, *FakeInterfaceName*, *DefaultInterfaceName*.

```go
var (
	cfgFile     string
	urlFile     string
	random      bool
	workerCount int
	iterations  int
	Timer       utils.Timer       = &utils.DefaultTimer{}
	HttpClient  client.HttpClient = client.NewDefaultHttpClient()
)
```

One refactoring I made for this version is introducing the builder pattern to create the actual surge client which tidied up the code.

```go
surgeClient := client.NewSurgeClientBuilder().
  SetURLFilePath(urlFile).
  SetRandom(random).
  SetWorkers(workerCount).
  SetIterations(iterations).
  SetHTTPClient(HttpClient).
  SetTimer(Timer).
  Build()

result, err := surgeClient.Run()
```

The method to create the builder creates a default instantiation of a SurgeClient, as a pointer, and then uses the different methods to change the properties with the `Build` method returning a reference to the SurgeClient pointer.

```go
func NewSurgeClientBuilder() *SurgeClientBuilder {
	return &SurgeClientBuilder{
		client: &surge{
			workerCount: 1,
			iterations:  1,
			httpClient:  NewDefaultHttpClient(),
			timer:       &utils.DefaultTimer{},
			lock:        sync.Mutex{},
			waitGroup:   sync.WaitGroup{},
		},
	}
}
```

Another refactoring I made in this version was to separate out some concerns in the SurgeClient, with all worker related activies extracted into a method called worker which is executed for each goroutine.

```go
func (surge *surge) worker(linesValue []string) {
	for i := 0; i < len(linesValue) || (surge.iterations > 0 && i < surge.iterations); i++ {
		line := linesValue[i%len(linesValue)]
		var command = HttpCommand{
			client: surge.httpClient,
		}
		var args = strings.Fields(line)
		err := command.Execute(args)
		surge.lock.Lock()
		if err != nil {
			surge.errors++
		}
		surge.transactions++
		surge.lock.Unlock()
		if i > 0 && i == surge.iterations-1 {
			break
		}
	}
	surge.waitGroup.Done()
}

func (surge *surge) execute(lines []string) Result {
	for i := 0; i < surge.workerCount; i++ {
		surge.timer.Start()
		surge.waitGroup.Add(1)
		go surge.worker(lines)
	}
	surge.waitGroup.Wait()
	result := Result{
		Transactions: surge.transactions,
		ElapsedTime:  surge.timer.Stop(),
	}
	if surge.errors == 0 {
		result.Availability = 1
	} else {
		result.Availability = float64(1 - float64(surge.errors)/float64(surge.transactions))
	}
	return result
}
```

I did not have much need to use different test cases for this requirement; I should really have added an integration test for the *DefaultTimer* but I didn't yet.  The current test makes use of the *Builder* and the *FakeTimer*.

```go
func Test_SurgeClientReturnsElapsedTime(t *testing.T) {
	file := utils.CreateRandomHttpTestFile(1)
	httpClient := client.NewFakeHTTPClient()
	expectedElapsed := 100 * time.Second
	timer := &utils.FakeTimer{}
	timer.SetElapsed(expectedElapsed)
	surgeClient := client.NewSurgeClientBuilder().
		SetURLFilePath(file.Name()).
		SetHTTPClient(httpClient).
		SetTimer(timer).
		Build()
	result, err := surgeClient.Run()

	assert.Nil(t, err)
	assert.Equal(t, expectedElapsed, result.ElapsedTime)
}
```
