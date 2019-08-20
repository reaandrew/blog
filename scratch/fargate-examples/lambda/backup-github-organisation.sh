handler () {
  EVENT_DATA=$1
  echo "$EVENT_DATA" 1>&2;
  env 1>&2;

  aws ecs run-task \
      --task-definition backup-github-organisation \
      --cluster backup \
      --launch-type FARGATE \
      --network-configuration 'awsvpcConfiguration={subnets=[subnet-03b7b0b05f6caaba8],securityGroups=[sg-0fa17060]}' 1>&2
}
