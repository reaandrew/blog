package main

import (
	"errors"
	"fmt"
	"time"
)

type RetryStrategy interface{
	Retry() (bool, time.Duration)
}

type IncrementalBackOffStrategy struct{
	next int
	max int
}

func (strategy *IncrementalBackOffStrategy) Retry() (bool, time.Duration){
	if strategy.next < strategy.max{
		strategy.next++
		return true, time.Duration(strategy.next)*time.Second
	}
	return false, time.Duration(0)
}

func Retry(action func() error, retryStrategy RetryStrategy) error{
	for err := action(); err != nil;{
		fmt.Println(err)
		retry, duration := retryStrategy.Retry()
		if retry{
			fmt.Println("Sleeping")
			time.Sleep(duration)
		}else{
			return errors.New("Retry failed too many times")
		}
	}
	return nil
}
