---
title: "Creating a Simple Stress Test Tool in Go Part 11"
date: 2019-11-03T07:00:00Z
draft: true
---

## Must output the statistics for Data Transferred (The total number of bytes received from the server) after the test run.

For this requirement I made use of two utilities inside the `httputils` package being:

- `DumpRequestOut`
- `DumpResponse`

This provides the entire set of bytes for a given request or response respectively.  I have simply added each of the worker values to a single value which I output at the end of the run.

```go
func (surge *surge) worker(linesValue []string) {
	for i := 0; i < len(linesValue) || (surge.iterations > 0 && i < surge.iterations); i++ {
		line := linesValue[i%len(linesValue)]
		var command = HttpCommand{
			client: surge.httpClient,
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
		surge.lock.Unlock()
		if i > 0 && i == surge.iterations-1 {
			break
		}
	}
	surge.waitGroup.Done()
}
```

The values for the request and response bytes are retrieved inside the HttpCommand which uses the underlying HttpClient interface which I have defined for testing purposes.  It is getting a little large this method so this will be targeted for refactoring in the next requirement.  One thing to point out in this code below is that I only operate on the response body if it is not `nil` which is the case during some of the tests.

```go
func (httpCommand HttpCommand) Execute(args []string) HttpResult {
	var verb string
	var result HttpResult

	command := &cobra.Command{
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			request, err := http.NewRequest(verb, args[0], nil)
			if err != nil {
				return err
			}
			requestBytes, err := httputil.DumpRequestOut(request, true)
			if err != nil {
				return err
			}
			result.TotalBytesSent = len(requestBytes)
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
			return nil
		},
	}
	command.PersistentFlags().StringVarP(&verb, "verb", "X", "GET", "")
	command.SetArgs(args)
	command.Execute()

	return result
}
```

For the tests I did a quick run to understand the size of one of my pre-baked requests and responses and used this value as the target assertion.  I did not want to abstract away anymore than I have done so this seemed a reasonable solution.

```go
func Test_SurgeClientReturnsTotalBytesSent(t *testing.T) {
	file := utils.CreateRandomHttpTestFile(1)
	httpClient := client.NewFakeHTTPClient()
	surgeClient := client.NewSurgeClientBuilder().
		SetURLFilePath(file.Name()).
		SetHTTPClient(httpClient).
		Build()
	result, err := surgeClient.Run()

	assert.Nil(t, err)
	//This is the size of one request dumped
	assert.Equal(t, result.TotalBytesSent, 96)
}

func Test_SurgeClientReturnsTotalBytesReceived(t *testing.T) {
	file := utils.CreateRandomHttpTestFile(1)
	httpClient := client.NewFakeHTTPClient()
	surgeClient := client.NewSurgeClientBuilder().
		SetURLFilePath(file.Name()).
		SetHTTPClient(httpClient).
		Build()
	result, err := surgeClient.Run()

	assert.Nil(t, err)
	//This is the size of one request dumped
	assert.Equal(t, result.TotalBytesReceived, 38)
}
```

Finally the test to ensure the value is output when ran is below.  The output is formatted using a package called *humanize*.

```go

func TestOutputsTotalBytesSent(t *testing.T) {
	file := utils.CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	client := client.NewFakeHTTPClient()
	timer := utils.NewFakeTimer(1 * time.Minute)

	output, err := executeCommand(cmd.RootCmd, client, timer, "-u", file.Name(), "-n", "1", "-c", "1")

	assert.Nil(t, err)
	assert.Contains(t, output, "Total Bytes Sent: 96 B\n")
}

func TestOutputsTotalBytesReceived(t *testing.T) {
	file := utils.CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	client := client.NewFakeHTTPClient()
	timer := utils.NewFakeTimer(1 * time.Minute)

	output, err := executeCommand(cmd.RootCmd, client, timer, "-u", file.Name(), "-n", "1", "-c", "1")

	assert.Nil(t, err)
	assert.Contains(t, output, "Total Bytes Received: 38 B\n")
}
```
