#!/usr/bin/env bash
##  This script runs the snapshot
##
##  NOTE: *_SECRET env variables are masked, hence if you'd like to avoid any
##        surprises please use the suffix _SECRET for those values that contain
##        any sensitive data. Buildkite can mask those values automatically

set -e

# Make sure we delete this folder before leaving even in case of failure
clean_up () {
  ARG=$?
  export VAULT_TOKEN=$PREVIOUS_VAULT_TOKEN
  echo "--- Deleting tmp workspace"
  rm -rf $TMP_WORKSPACE
  exit $ARG
}
trap clean_up EXIT

set +x
echo "--- Deploy the snapshot"
./mvnw -s .ci/settings.xml -Pgpg clean deploy --batch-mode
