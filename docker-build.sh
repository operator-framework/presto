#!/bin/bash -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

docker build \
    -t quay.io/coreos/presto:latest \
    -f "$DIR/Dockerfile" \
    "$DIR"
