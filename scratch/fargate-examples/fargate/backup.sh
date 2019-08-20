#!/bin/sh

set -euo pipefail

if [ -z "$GITHUB_TOKEN_KEY" ]; then
  echo "Must supply a github token"
  exit 1
fi

if [ -z "$GITHUB_ORG" ]; then
  echo "Must supply a github org"
  exit 1
fi

if [ -z "$S3_BUCKET_URL" ]; then
  echo "Must supply an S3 bucket"
  exit 1
fi

PAGE=1
PAGE_SIZE=100
TMP_FILE=$(mktemp)

:>"$TMP_FILE"


GITHUB_TOKEN=$(aws ssm get-parameter --name "$GITHUB_TOKEN_KEY" --with-decryption | jq -r '.Parameter.Value')

echo "echo $GITHUB_TOKEN" > "$HOME"/.git-askpass
chmod +x "$HOME/.git-askpass"
export GIT_ASKPASS="$HOME/.git-askpass"

git config --global url."https://user@github.com/".insteadOf "https://github.com/"

while true
do
  curl -sH "Authorization: Bearer $GITHUB_TOKEN" "https://api.github.com/orgs/$GITHUB_ORG/repos?per_page=$PAGE_SIZE&page=$PAGE" | jq -er '.[] | .clone_url' >> "$TMP_FILE" || break
  PAGE=$((PAGE+1))
done

DAY_OF_MONTH=$(date '+%d')

while read -r REPO
do
  FILENAME="$(basename "$REPO").tar.gz"
  git clone --mirror "$REPO"
  tar -czvf "$FILENAME" "$(basename "$REPO")"
  aws s3 cp "$FILENAME" "$S3_BUCKET_URL/Day_$DAY_OF_MONTH/$FILENAME"
done < "$TMP_FILE"
