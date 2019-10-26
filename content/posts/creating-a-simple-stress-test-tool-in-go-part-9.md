---
title: "Creating a Simple Stress Test Tool in Go Part 9"
date: 2019-10-26T07:00:00Z
draft: false
---

## Must output the statistics for Availability (1 - (Number of errors / Transactions)) after the test run

I had to change the info which is returned by the HttpCommand for this and I created another struct called Result.

```go
package client

type Result struct {
	Transactions int
	Availability float64
}
```

I have defined an error in terms of Availability as any response which has a 4XX or 5XX response code.  One problem which I hit was the program would exit when I returned an error inside the CLI parser func (which is the correct behaviour).  I had to created an error variable in the outer scope, always return `nil` from the CLI function and then return the outer variable for the Surge app.

```go
package client

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/spf13/cobra"
)

type HttpCommand struct {
	client HttpClient
}

func (httpCommand HttpCommand) Execute(args []string) error {
	var verb string
	var returnError error

	command := &cobra.Command{
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			request, err := http.NewRequest(verb, args[0], nil)
			if err != nil {
				return err
			}
			response, err := httpCommand.client.Execute(request)
			if err != nil {
				return err
			}
			if response.StatusCode >= 400 {
				returnError = errors.New("Error " + strconv.Itoa(response.StatusCode))
			}
			return nil
		},
	}
	command.PersistentFlags().StringVarP(&verb, "verb", "X", "GET", "")
	command.SetArgs(args)
	command.Execute()

	return returnError
}
```

The main method in Surge is growing large now and will need to be refactored on the next pass.  If the execution of the HTTPCommand returns an error I incremented the errors value, inside the lock context, and then return a result at the end.

```go
func (surge Surge) execute(lines []string) Result {
	var lock = sync.Mutex{}
	transactions := 0
	errors := 0
	var wg sync.WaitGroup
	for i := 0; i < surge.WorkerCount; i++ {
		wg.Add(1)
		go func(linesValue []string) {
			for i := 0; i < len(linesValue) || (surge.Iterations > 0 && i < surge.Iterations); i++ {
				line := linesValue[i%len(linesValue)]
				var command = HttpCommand{
					client: surge.HttpClient,
				}
				var args = strings.Fields(line)
				err := command.Execute(args)
				lock.Lock()
				if err != nil {
					errors++
				}
				transactions++
				lock.Unlock()
				if i > 0 && i == surge.Iterations-1 {
					break
				}
			}
			wg.Done()
		}(lines)
	}
	wg.Wait()
	result := Result{
		Transactions: transactions,
	}
	if errors == 0 {
		result.Availability = 1
	} else {
		result.Availability = float64(1 - float64(errors)/float64(transactions))
	}
	return result
}
```

In order to test this with the FakeHTTPClient I created an Interceptor method which allows the test methods to change attributes of the response.

```go
package client

import (
	"net/http"
	"sync"
)

var m = sync.Mutex{}

type FakeHTTPClient struct {
	Requests    []*http.Request
	Interceptor InterceptorFunc
}

func NewFakeHTTPClient() *FakeHTTPClient {
	return &FakeHTTPClient{
		Interceptor: func(response *http.Response) {},
	}
}

type InterceptorFunc func(response *http.Response)

func (fakeClient *FakeHTTPClient) Execute(request *http.Request) (*http.Response, error) {
	m.Lock()
	fakeClient.Requests = append(fakeClient.Requests, request)
	m.Unlock()
	response := &http.Response{
		StatusCode: http.StatusOK,
	}
	fakeClient.Interceptor(response)
	return response, nil
}
```

I created another specific struct for the test cases for Availability including the expected availability.  

```go
type SurgeClientAvailabilityTestCase struct {
	StatusCodes          []int
	ExpectedAvailability float64
}

func Test_SurgeClientReturnsAvailability(t *testing.T) {
	cases := []SurgeClientAvailabilityTestCase{
		SurgeClientAvailabilityTestCase{StatusCodes: []int{200, 200, 500, 500}, ExpectedAvailability: float64(0.5)},
		SurgeClientAvailabilityTestCase{StatusCodes: []int{200, 200}, ExpectedAvailability: float64(1)},
		SurgeClientAvailabilityTestCase{StatusCodes: []int{200, 201, 202}, ExpectedAvailability: float64(1)},
		SurgeClientAvailabilityTestCase{StatusCodes: []int{200, 200, 404, 500}, ExpectedAvailability: float64(0.5)},
		SurgeClientAvailabilityTestCase{StatusCodes: []int{500, 500, 500, 500}, ExpectedAvailability: float64(0)},
	}

	for _, testCase := range cases {
		t.Run(fmt.Sprintf("Test_SurgeClientReturnAvailabilityOf%v%%", testCase.ExpectedAvailability*100), func(t *testing.T) {
			file := utils.CreateRandomHttpTestFile(len(testCase.StatusCodes))
			httpClient := client.NewFakeHTTPClient()
			client := client.Surge{
				UrlFilePath: file.Name(),
				WorkerCount: 1,
				HttpClient:  httpClient,
				Iterations:  1,
			}
			count := 0
			httpClient.Interceptor = func(response *http.Response) {
				response.StatusCode = testCase.StatusCodes[count]
				count++
			}
			result, err := client.Run()

			assert.Nil(t, err)
			assert.Equal(t, testCase.ExpectedAvailability, result.Availability)
		})
	}
}
```

The final test was to assert that the Availability statistic was written out to standard out.

```go
func TestOutputsNumberOfAvailability(t *testing.T) {
	file := utils.CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	client := client.NewFakeHTTPClient()

	output, err := executeCommand(cmd.RootCmd, client, "-u", file.Name(), "-n", "1", "-c", "1")

	assert.Nil(t, err)
	assert.Contains(t, output, "Availability: 100%\n")
}
```
