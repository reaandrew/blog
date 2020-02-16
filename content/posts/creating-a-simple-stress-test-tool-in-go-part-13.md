---
title: "Creating a Simple Stress Test Tool in Go Part 13"
date: 2019-11-05T07:10:00Z
draft: true
---

## Must output the statistics for Transaction rate (The number of requests per second) after the test run

For this requirement I have used the `Meter` type from the go-metrics package.  The `RateMean()` function returns the value of the per second interval which is perfect for this requirement.  I have updated the value by 1 each time a test completes.

```go
	surge.transactionRate.Mark(1)
```

There was not much benefit in trying to mock out anything for this one so I just relied on a single integration test which tested that the outputted value was something greater than 0.  I could and should have written a test for this in the SurgeClient testt suite but I didn't bother.

```go
func TestOutputsAverageTransactionRate(t *testing.T) {
	file := utils.CreateTestFile([]string{
		"http://localhost:8080/1",
	})
	defer os.Remove(file.Name())

	client := client.NewFakeHTTPClient()
	timer := utils.NewFakeTimer(1 * time.Minute)

	output, err := executeCommand(cmd.RootCmd, client, timer, "-u", file.Name(), "-n", "1", "-c", "1")

	assert.Nil(t, err)
	matched, err := regexp.Match(`Average Transaction Rate: [^0][\d]+ transactions/sec`, []byte(output))
	assert.Nil(t, err)
	assert.True(t, matched)
}
```

The actual output of the program at this point looks like the following (I did not have any server running hence the 0% availability)

```shell
Transactions: 10
Availability: 0%          
Elapsed Time: 176.813µs                                         
Total Bytes Sent: 950 B                                                 
Total Bytes Received: 0 B                                
Average Response Time: 0.17251229999999998ms
Average Transaction Rate: 3105 transactions/sec
```
