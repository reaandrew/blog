package main

import (
	"fmt"
	"log"
	"sync"
)

func main(){

	waitFor := 25
	wg := sync.WaitGroup{}
	wg.Add(1)
	serverAddress := "0.0.0.0:41521"
	controller := NewRemoteController(waitFor)
	controller.OnReady(func(){
		fmt.Println("Running test")
		controller.Run(&TestSpecification{})
	})
	controller.OnComplete(func(){
		fmt.Println("Completed!!")
		wg.Done()
	})
	go controller.Start(serverAddress)

	for i := 0; i < waitFor; i++{
		log.Println("Creating new worker")
		worker := NewWorker(serverAddress)
		go worker.Start()
	}


	wg.Wait()
}
