# Buildkite
This README overviews the Buildkite pipelines that automate the build and publishing processes.

## Release pipeline

The Buildkite pipeline for the ECS Logging Java is responsible for the releases.

### Pipeline Configuration

To view the pipeline and its configuration, click [here](https://buildkite.com/elastic/ecs-logging-java-release) or
go to the definition in the `elastic/ci` repository.

### Credentials

The release team provides the credentials required to publish the artifacts in Maven Central and sign them
with the GPG.

If further details are needed, please go to [pre-command](hooks/pre-command).

## Snapshot pipeline

The Buildkite pipeline for the APM Agent Java is responsible for the snapshots.

### Pipeline Configuration

To view the pipeline and its configuration, click [here](https://buildkite.com/elastic/ecs-logging-java-snapshot) or
go to the definition in the `elastic/ci` repository.
