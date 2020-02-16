---
title: "Creating a Simple Stress Test Tool in Go Part 12"
date: 2019-11-03T07:10:00Z
draft: true
---

## Must output the statistics for Response Time (The average response time in ms) after the test run.

For this requirement I have started to use the metrics package from here [https://github.com/rcrowley/go-metrics](https://github.com/rcrowley/go-metrics).  It is a really good library and it without it I would just be adding more code to do simple maths which is fine but I also know that future requirements will need similar functionality so introducing this at this point seemed the best option. For the average response time I used a histogram and the `Mean()` function.  There are several more methods which would also be useful to add in the future.

```go
type surge struct {
	//TODO: Create a configuration struct for these
	urlFilePath string
	random      bool
	workerCount int
	iterations  int
	httpClient  HttpClient
	timer       utils.Timer
	lock        sync.Mutex
	waitGroup   sync.WaitGroup
	//TODO: Create a stats struct for these
	transactions       int
	errors             int
	totalBytesSent     int
	totalBytesReceived int
	responseTime       metrics.Histogram
}

func (surge *surge) worker(linesValue []string) {
	for i := 0; i < len(linesValue) || (surge.iterations > 0 && i < surge.iterations); i++ {
		line := linesValue[i%len(linesValue)]
		var command = HttpCommand{
			client: surge.httpClient,
			timer:  surge.timer,
		}
		var args = strings.Fields(line)
		result := command.Execute(args)
		surge.lock.Lock()
		if result.Error != nil {
			surge.errors++
		}
		surge.transactions++
		surge.totalBytesSent += result.TotalBytesSent
		surge.totalBytesReceived += result.TotalBytesReceived
		surge.responseTime.Update(int64(result.ResponseTime))
		surge.lock.Unlock()
		if i > 0 && i == surge.iterations-1 {
			break
		}
	}
	surge.waitGroup.Done()
}
```

The value of the `result.ResponseTime` is from the Timer implementation inside the `HttpCommand`.

```go
...
			httpCommand.timer.Start()
			response, err := httpCommand.client.Execute(request)
			if err != nil {
				result.Error = err
			} else {
				if response.Body != nil {
					defer response.Body.Close()
					io.Copy(ioutil.Discard, response.Body)
				}
				responseBytes, err := httputil.DumpResponse(response, true)
				result.TotalBytesReceived = len(responseBytes)
				if err != nil {
					result.Error = err
				}
				if response.StatusCode >= 400 {
					result.Error = errors.New("Error " + strconv.Itoa(response.StatusCode))
				}
			}
			//Stop the timer
			result.ResponseTime = httpCommand.timer.Stop()
...
```

The above location of the Stop method could be more exact and will be with the introduction of the trace utility from the go http package but for now this is definitely good enough.

The main test for the output (which I have not used humanize for as I simply want the time formatted in milliseconds every time).

```go
func TestOutputsAverageResponseTime(t *testing.T) {
	file := utils.CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	client := client.NewFakeHTTPClient()
	timer := utils.NewFakeTimer(1 * time.Second)

	output, err := executeCommand(cmd.RootCmd, client, timer, "-u", file.Name(), "-n", "1", "-c", "1")

	assert.Nil(t, err)
	assert.Contains(t, output, "Average Response Time: 1000ms\n")
}
```
