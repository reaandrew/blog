#!/bin/sh

set -euo pipefail

aws sqs send-message --queue-url https://sqs.eu-west-2.amazonaws.com/007763200772/example --message-body "finished"
