package main

type Controller interface{
	Run(spec *TestSpecification) (TestResult, error)
	Start()
}
