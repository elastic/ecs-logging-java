#!/usr/bin/env bash
##  This script runs the release given the different environment variables
##  branch_specifier
##  dry_run
##
##  NOTE: *_SECRET env variables are masked, hence if you'd like to avoid any
##        surprises please use the suffix _SECRET for those values that contain
##        any sensitive data. Buildkite can mask those values automatically

set -e

echo "--- Prepare vault context"
set +x
VAULT_ROLE_ID_SECRET=$(vault read -field=role-id secret/ci/elastic-ecs-logging-java/internal-ci-approle)
export VAULT_ROLE_ID_SECRET
VAULT_SECRET_ID_SECRET=$(vault read -field=secret-id secret/ci/elastic-ecs-logging-java/internal-ci-approle)
export VAULT_SECRET_ID_SECRET
VAULT_ADDR=$(vault read -field=vault-url secret/ci/elastic-ecs-logging-java/internal-ci-approle)
export VAULT_ADDR

# Delete the vault specific accessing the ci vault
export VAULT_TOKEN_PREVIOUS=$VAULT_TOKEN
unset VAULT_TOKEN

echo "--- Prepare release context"
# Avoid detached HEAD since the release plugin requires to be on a branch
git checkout -f "${branch_specifier}"
# Prepare a secure temp folder not shared between other jobs to store the key ring
export TMP_WORKSPACE=/tmp/secured
export KEY_FILE=$TMP_WORKSPACE"/private.key"
# Secure home for our keyring
export GNUPGHOME=$TMP_WORKSPACE"/keyring"
mkdir -p $GNUPGHOME
chmod -R 700 $TMP_WORKSPACE
# Make sure we delete this folder before leaving even in case of failure
clean_up () {
  ARG=$?
  export VAULT_TOKEN=$VAULT_TOKEN_PREVIOUS
  echo "--- Deleting tmp workspace"
  rm -rf $TMP_WORKSPACE
  exit $ARG
}
trap clean_up EXIT

echo "--- Prepare keys context"
set +x
VAULT_TOKEN=$(vault write -field=token auth/approle/login role_id="$VAULT_ROLE_ID_SECRET" secret_id="$VAULT_SECRET_ID_SECRET")
export VAULT_TOKEN

# Signing keys
vault read -field=key secret/release/signing >$KEY_FILE
KEYPASS_SECRET=$(vault read -field=passphrase secret/release/signing)
export KEYPASS_SECRET
export KEY_ID_SECRET=D88E42B4
unset VAULT_TOKEN

# Import the key into the keyring
echo "$KEYPASS_SECRET" | gpg --batch --import "$KEY_FILE"

# Export secring
export SECRING_FILE=$GNUPGHOME"/secring.gpg"
gpg --pinentry-mode=loopback --passphrase "$KEYPASS_SECRET" --export-secret-key $KEY_ID_SECRET > "$SECRING_FILE"
set -x
# Configure the committer since the maven release requires to push changes to GitHub
# This will help with the SLSA requirements.
git config --global user.email "infra-root+apmmachine@elastic.co"
git config --global user.name "apmmachine"

echo "--- Install JDK11"
JAVA_URL=https://jvm-catalog.elastic.co/jdk
JAVA_HOME=$(pwd)/.openjdk11
JAVA_PKG="$JAVA_URL/latest_openjdk_11_linux.tar.gz"
curl -L --output /tmp/jdk.tar.gz "$JAVA_PKG"; \
  mkdir -p "$JAVA_HOME"; \
  tar --extract --file /tmp/jdk.tar.gz --directory "$JAVA_HOME" --strip-components 1
export JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH

set +x
echo "--- Release the binaries to Maven Central"
if [[ "$dry_run" == "true" ]] ; then
  echo './mvnw release:prepare release:perform --settings .ci/settings.xml --batch-mode'
else
  # providing settings in arguments to make sure they are propagated to the forked maven release process
  ./mvnw release:prepare release:perform --settings .ci/settings.xml -Darguments="--settings .ci/settings.xml" --batch-mode
fi
