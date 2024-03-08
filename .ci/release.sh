#!/usr/bin/env bash
##  This script runs the release given the different environment variables
##    ref      : git reference (commit,branch, tag, ...)
##    dry_run  : dry-run when set to 'true'
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
git checkout -f "${ref}"

echo "--- Debug JDK installation :coffee:"
echo $JAVA_HOME
echo $PATH
java -version

set +x
# Default in dry-run mode
GOAL="package"
DRY_RUN_MSG="(dry-run)"
# Otherwise, a RELEASE
if [[ "$dry_run" == "false" ]] ; then
  GOAL="deploy"
  DRY_RUN_MSG=""
fi

echo "--- Release the binaries to Maven Central :maven: [./mvnw $GOAL)] $DRY_RUN_MSG"
./mvnw -V -s .ci/settings.xml -Pgpg clean $GOAL -DskipTests --batch-mode | tee release.txt
