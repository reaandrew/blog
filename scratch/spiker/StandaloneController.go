package main

type StandloneController struct{
	worker *Service
}

func NewStandloneController(service *Service) *StandloneController {
	return &StandloneController{
		worker: service,
	}
}

func (s *StandloneController) Run(spec *TestSpecification) (TestResult, error) {
	return s.worker.Run(spec)
}

func (s *StandloneController) AddWorker(_ Worker) error {
	// No op in standalone mode
	return nil
}


