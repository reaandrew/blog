---
title: "Creating a Simple Stress Test Tool in Go Part 8"
date: 2019-10-25T07:00:00Z
draft: false
---

## Must output the statistics for Transactions (The total number of requests made) after the test run

During this feature I found a bug in the loop code in the SurgeClient where the program would continue to run if the number of urls was greater than the number of iterations and the number of iterations was greater than 1.  Whilst I was debugging this I got thinking that having to write a test which consumes a HTTP server for every single test and case was getting cumbersome so I decided to make the HttpClient configurable and in that way inject a Fake one which did not actually make a HTTP call.  The main purpose of these tests is to assert on the internal logic of the loops not that the calls are HTTP.

I have left out some integration tests for the `DefaultHTTPClient` which would be convenient and allow me to carry on using the Fake HTTP Client.

To calculate the number of transactions across different goroutines I used a simple mutex and also changed the signature to return an `int` which holds the number of transactions.  This will need to be changed to satisfy the future requirements but for now this sorted it.

```go
func (surge Surge) execute(lines []string) int {
	var lock = sync.Mutex{}
	transactions := 0
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
				command.Execute(args)
				lock.Lock()
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
	return transactions
}
```

I then created several test cases to check the different permutations of Urls, Workers and Iterations (which is how I discovered the bug above).  I have created a specific struct for the test cases including the expected value and then used golangs method of writing sub tests.  

```go
package client_test

import (
	"fmt"
	"testing"

	"github.com/reaandrew/surge/client"
	"github.com/reaandrew/surge/utils"
	"github.com/stretchr/testify/assert"
)

type SurgeClientTransactionTestCase struct {
	Urls                 int
	Workers              int
	Iterations           int
	ExpectedTransactions int
}

func Test_SurgeClientReturnNumberOfTransactions(t *testing.T) {
	cases := []SurgeClientTransactionTestCase{
		SurgeClientTransactionTestCase{Urls: 1, Workers: 1, Iterations: 1, ExpectedTransactions: 1},
		SurgeClientTransactionTestCase{Urls: 2, Workers: 1, Iterations: 1, ExpectedTransactions: 2},
		SurgeClientTransactionTestCase{Urls: 1, Workers: 2, Iterations: 1, ExpectedTransactions: 2},
		SurgeClientTransactionTestCase{Urls: 2, Workers: 2, Iterations: 1, ExpectedTransactions: 4},
		SurgeClientTransactionTestCase{Urls: 1, Workers: 2, Iterations: 2, ExpectedTransactions: 4},
		SurgeClientTransactionTestCase{Urls: 3, Workers: 2, Iterations: 2, ExpectedTransactions: 4},
		SurgeClientTransactionTestCase{Urls: 2, Workers: 2, Iterations: 3, ExpectedTransactions: 6},
		SurgeClientTransactionTestCase{Urls: 5, Workers: 100, Iterations: 5, ExpectedTransactions: 500},
	}

	for _, testCase := range cases {
		t.Run(fmt.Sprintf("Test_SurgeClientReturnNumberOfTransactions_Urls_%v_Workers_%v_Iterations_%v_Returns_%v_Transactions",
			testCase.Urls,
			testCase.Workers,
			testCase.Iterations,
			testCase.ExpectedTransactions), func(t *testing.T) {
			file := utils.CreateRandomHttpTestFile(testCase.Urls)
			client := client.Surge{
				UrlFilePath: file.Name(),
				WorkerCount: testCase.Workers,
				HttpClient:  &client.FakeHTTPClient{},
				Iterations:  testCase.Iterations,
			}
			transactions, err := client.Run()

			assert.Nil(t, err)
			assert.Equal(t, testCase.ExpectedTransactions, transactions)
		})
	}
}
```

I switched out the HTTP Client in the HTTP Command and then created a FakeHTTPClient which simply stores the requests which are made so the tests can assert on them.

```go
package client

import (
	"net/http"
	"sync"
)

var m = sync.Mutex{}

type FakeHTTPClient struct {
	Requests []*http.Request
}

func (fakeClient *FakeHTTPClient) Execute(request *http.Request) {
	m.Lock()
	fakeClient.Requests = append(fakeClient.Requests, request)
	m.Unlock()
}
```

All the test cases then got updated to remove the HTTP requirement and make use of the FakeHTTPClient.  As example here is the latest test which asserts that the number of transactions is written to the standard out.

```go
func TestOutputsNumberOfTransactions(t *testing.T) {
	file := utils.CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	client := &client.FakeHTTPClient{}

	output, err := executeCommand(cmd.RootCmd, client, "-u", file.Name(), "-n", "1", "-c", "1")

	assert.Nil(t, err)
	assert.Contains(t, output, "Transactions: 1\n")
}
```


