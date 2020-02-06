package main

import (
	"context"
	"fmt"
	"google.golang.org/grpc"
	"io"
	"log"
)

type Worker struct {
	address string
}

func NewWorker(address string) *Worker {
	return &Worker{address: address}
}

func (worker *Worker) Start() {
	//This will connect to the server and then run the necessary commands
	//Even if it is standalone, this will still use the distributed method
	// but with a single worker node.
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	conn, err := grpc.Dial(worker.address, grpc.WithInsecure())
	if err != nil {
		panic(err)
	}
	c := NewLoadrServiceClient(conn)

	log.Println("Worker connecting to server")
	var client LoadrService_ConnectClient
	err = Retry(func() error {
		client, err = c.Connect(ctx)
		if err != nil {
			return err
		}
		return nil
	}, &IncrementalBackOffStrategy{max: 10})
	if err != nil {
		panic(err)
	}
	for {
		controlRequest, err := client.Recv()
		if err == io.EOF || err != nil {
			break
		}
		service := NewService()
		result, err := service.Run(controlRequest.Spec)
		if err != nil {
			panic(err)
		}
		fmt.Println("Sending result")
		c.SendTestResult(ctx, &SendTestResultRequest{
			Result: &result,
		})
	}
}
