#!/bin/bash

set -euo pipefail

DEFAULT_MEMORY=1024
DEFAULT_RUNTIME=bash-runtime
DEFAULT_TIMEOUT=30
DEFAULT_ROLE="ROLE_NAME_HERE"

LATEST_RUNTIME_VERSION=$(aws lambda list-layers --query "Layers[?LayerName=='$DEFAULT_RUNTIME'].LatestMatchingVersion.LayerVersionArn" --output text)

TASK_DEF_VER="$(aws ecs register-task-definition --cli-input-json file://task-def.json | jq -r .taskDefinition.taskDefinitionArn | cut -d: -f7)"

echo "Process lambda function for backup-github-files..."
rm -rf "backup-github-organisation.zip"
zip "backup-github-organisation.zip" "backup-github-organisation.sh"

if aws lambda get-function --function-name "backup-github-organisation"; then
  echo "Function backup-github-organisation exists.  Updating function."
  aws lambda update-function-code --publish --function-name backup-github-organisation --zip-file "fileb://backup-github-organisation.zip"
  aws lambda update-function-configuration --function-name backup-github-organisation \
    --layers "$LATEST_RUNTIME_VERSION"
else
  echo "Function backup-github-organisation does not exist.  Creating function."
  aws lambda create-function --function-name backup-github-organisation \
    --zip-file fileb://backup-github-organisation.zip \
    --handler backup-github-organisation.handler \
    --memory-size "$DEFAULT_MEMORY" \
    --timeout "$DEFAULT_TIMEOUT" \
    --runtime provided \
    --role "$DEFAULT_ROLE" \
    --publish

  aws lambda update-function-configuration --function-name backup-github-organisation \
    --layers "$LATEST_RUNTIME_VERSION"
fi
