ORG:=fu
REPO:=bar
AWS_ACCESS_KEY_ID:=`aws configure get aws_access_key_id`
AWS_SECRET_ACCESS_KEY:=`aws configure get aws_secret_access_key`
AWS_DEFAULT_REGION:=`aws configure get region`
AWS_DEFAULT_OUTPUT:=`aws configure get output`
AWS_ACCOUNT_NUMBER:=`aws sts get-caller-identity | jq -r '.Account'`

.PHONY: build
build:
	@docker build -t "${ORG}/${REPO}" .

.PHONY: run
run:
	@export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
	@export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
	@export AWS_DEFAULT_REGION=$(aws configure get region)
	@export AWS_DEFAULT_OUTPUT=$(aws configure get output)
	@docker run \
	   -e AWS_ACCESS_KEY_ID \
		 -e AWS_SECRET_ACCESS_KEY \
		 -e AWS_DEFAULT_REGION \
		 -e AWS_DEFAULT_OUTPUT \
		 -t "${ORG}/${REPO}"

.PHONY: lint
lint:
	@ [ -d ./lynis ] || curl -sL https://downloads.cisofy.com/lynis/lynis-2.7.5.tar.gz | tar xz
	@./lynis/lynis --log-file lynis-audit.log audit dockerfile Dockerfile
	@cat lynis-audit.log | grep -ivq warning || exit 1

.PHONY: publish
publish:
	docker tag "${ORG}/${REPO}:latest" "${AWS_ACCOUNT_NUMBER}.dkr.ecr.eu-west-2.amazonaws.com/${ORG}/${REPO}:latest"
	docker push "${AWS_ACCOUNT_NUMBER}.dkr.ecr.eu-west-2.amazonaws.com/${ORG}/${REPO}:latest"
