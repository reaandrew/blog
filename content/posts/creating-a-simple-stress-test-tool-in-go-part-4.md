---
title: "Creating a Simple Stress Test Tool in Go Part 4"
date: 2019-10-20T06:00:00Z
draft: true
---

## Must support http verbs GET,POST,PUT,DELETE

I have added another use of the Cobra CLI for this feature so that I could support command line flags in the url list which must be provided to surge.  Another reason was so that I was not parsing the commmand line arguments and flags by myself as that wheel is well and truly invented!

```go
package client

import (
	"net/http"

	"github.com/spf13/cobra"
)

type HttpCommand struct {
}

func (httpCommand HttpCommand) Execute(args []string) error {

	client := http.Client{}
	var verb string

	command := &cobra.Command{
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			request, err := http.NewRequest(verb, args[0], nil)
			if err != nil {
				return err
			}
			client.Do(request)
			return nil
		},
	}
	command.PersistentFlags().StringVarP(&verb, "verb", "X", "GET", "")
	command.SetArgs(args)
	return command.Execute()
}
```

There are a couple of things to say about this, first, I have extracted out the code to create a http request into its own file; second, although I have achieved the requirements of supporting GET,POST,PUT,DELETE it actually supports any verb.

The code which loops through the file now creates and executes a HttpCommand.

```go
line := scanner.Text()
args := strings.Fields(line)
HttpCommand{}.Execute(args)
```

To test this I had to update my test code so that each test could cleanly setup a new http server and to do this I used the gorilla mux package.

```go
func startHTTPServer(callback func(r http.Request)) *http.Server {
	r := mux.NewRouter()
	r.Handle("/{id}", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		callback(*r)
		io.WriteString(w, "hello world\n")
	}))
	srv := &http.Server{
		Addr:    ":8080",
		Handler: r,
	}

	go func() {
		if err := srv.ListenAndServe(); err != http.ErrServerClosed {
			log.Fatalf("ListenAndServe(): %s", err)
		}
	}()

	return srv
}
```

Another interesting thing/side effect is that ddue to using the cobra library the order of the arguments and flags does not seem to matter.  I am just counting the verbs which I received and they are specified in different a different order.

```go
func TestSupportForVerbPut(t *testing.T) {
	fileContents := `-X PUT http://localhost:8080/1
http://localhost:8080/2 -X PUT`

	//Write the urls to a file
	//Pass the urls file path in as a -u flag
	file, err := ioutil.TempFile(os.TempDir(), "prefix")
	if err != nil {
		log.Fatal(err)
	}
	defer os.Remove(file.Name())
	ioutil.WriteFile(file.Name(), []byte(fileContents), os.ModePerm)

	methods := []string{}
	srv := startHTTPServer(func(r http.Request) {
		methods = append(methods, r.Method)
	})

	executeCommand(cmd.RootCmd, "-u", file.Name())

	assert.Equal(t, methods, []string{"PUT", "PUT"})

	if err := srv.Shutdown(context.TODO()); err != nil {
		panic(err) // failure/timeout shutting down the server gracefully
	}
}
```
