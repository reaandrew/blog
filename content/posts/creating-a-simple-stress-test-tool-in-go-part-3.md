---
title: "Creating a Simple Stress Test Tool in Go Part 3"
date: 2019-10-14T06:00:00Z
draft: false
---

## Must accept a file arguement of urls to test

For this feature I have added a file argument to the CLI configuration, with the value being passed to a new Application object.  The CLI reposnsibility is to deal with the CLI context and not the logic of the application.

```go
RootCmd.PersistentFlags().StringVarP(&urlFile, "urls", "u", "", "The urls file to use")
```

I have called the new struct `Surge` which will now be the main entry point of the actual application logic.

```go
RunE: func(cmd *cobra.Command, args []string) error {                                                                                                                     
  return client.Surge{UrlFilePath: urlFile}.Run()
},  
```

The Surge struct is inside the `client` package where all the core application objects will be placed.

```go
package client

import (
        "bufio"
        "net/http"
        "os"
)

type Surge struct {
        UrlFilePath string
}

func (surge Surge) Run() error {
        if surge.UrlFilePath != "" {
                file, err := os.Open(surge.UrlFilePath)
                if err != nil {
                        return err
                }
                client := http.Client{}
                scanner := bufio.NewScanner(file)
                for scanner.Scan() {
                        request, err := http.NewRequest("GET", scanner.Text(), nil)
                        if err != nil {
                                return err
                        }
                        client.Do(request)
                }

                if err := scanner.Err(); err != nil {
                        return err
                }
        }
        return nil
}
```


