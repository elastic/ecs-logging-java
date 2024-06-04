### Release process

To release a new version of ecs-logging-java, you must use the two GitHub Workflows.

- Trigger the `release-step-1` GH workflow
  - parameters: version to release
  - will open `release-step-2` PR
- Review and merge the `release-step-2` PR to `main` (version bump to release)
- Trigger the `release-step-3` GH workflow
  - parameters: version to release and the `main` branch (or merge commit/ref of `release-step-2` PR merge).
  - will generate and publish release artifact through buildkite.
  - will open `release-step-4` PR
- Review and merge the `release-step-4` PR to `main` (version bump from release to next snapshot version)

The tag release follows the naming convention: `v.<major>.<minor>.<patch>`, where `<major>`, `<minor>` and `<patch>`.


