#!/usr/bin/env bash
##  This script runs the snapshot given the different environment variables
##    dry_run
##
##  It relies on the .buildkite/hooks/pre-command so the Vault and other tooling
##  are prepared automatically by buildkite.
##

set -eo pipefail

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
echo "--- Deploy the snapshot :package:"
if [[ "$dry_run" == "true" ]] ; then
  echo './mvnw -s .ci/settings.xml -Pgpg clean deploy --batch-mode'
else
  ./mvnw -s .ci/settings.xml -Pgpg clean deploy --batch-mode | tee snapshot.txt
fi
