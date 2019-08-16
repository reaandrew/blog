aws ecr create-repository --repository-name dft-bluebadge/backup_github_org

docker build -t dft-bluebadge/backup_github_org .
docker tag dft-bluebadge/backup_github_org:latest 007763200772.dkr.ecr.eu-west-2.amazonaws.com/dft-bluebadge/backup_github_org:latest
docker push 007763200772.dkr.ecr.eu-west-2.amazonaws.com/dft-bluebadge/backup_github_org:latest

export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
export AWS_DEFAULT_REGION=$(aws configure get region)
export AWS_DEFAULT_OUTPUT=$(aws configure get output)

docker run -v ~/.aws:/home/worker/.aws \
  -e AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY \
  -e AWS_DEFAULT_REGION \
  -e AWS_DEFAULT_OUTPUT \
  -t dft-bluebadge/backup_github_org:latest
