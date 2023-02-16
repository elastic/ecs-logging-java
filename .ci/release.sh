#!/usr/bin/env bash
##  This script runs the release given the different environment variables
##    branch_specifier
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

# Avoid detached HEAD since the release plugin requires to be on a branch
git checkout -f "${branch_specifier}"

set +x
echo "--- Release the binaries to Maven Central :maven:"
if [[ "$dry_run" == "true" ]] ; then
  echo './mvnw release:prepare release:perform --settings .ci/settings.xml --batch-mode'
else
  # providing settings in arguments to make sure they are propagated to the forked maven release process
  ./mvnw -V release:prepare release:perform --settings .ci/settings.xml -Darguments="--settings .ci/settings.xml" --batch-mode | tee release.txt
fi
