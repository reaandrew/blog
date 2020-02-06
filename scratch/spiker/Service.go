package main

type Service struct{

}

func NewService() *Service {
	return &Service{}
}

func (s Service) Run(spec *TestSpecification) (TestResult, error) {
	return TestResult{}, nil
}

