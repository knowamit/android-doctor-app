fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Run unit tests

### android build

```sh
[bundle exec] fastlane android build
```

Build signed release AAB

### android build_apk

```sh
[bundle exec] fastlane android build_apk
```

Build signed release APK

### android deploy_internal

```sh
[bundle exec] fastlane android deploy_internal
```

Deploy to Internal Testing track

### android deploy_beta

```sh
[bundle exec] fastlane android deploy_beta
```

Deploy to Closed Beta track

### android deploy_production

```sh
[bundle exec] fastlane android deploy_production
```

Deploy to Production

### android deploy_rollout

```sh
[bundle exec] fastlane android deploy_rollout
```

Deploy to Production with gradual rollout (default 10%)

### android promote_to_beta

```sh
[bundle exec] fastlane android promote_to_beta
```

Promote internal → beta

### android promote_to_production

```sh
[bundle exec] fastlane android promote_to_production
```

Promote beta → production

### android update_metadata

```sh
[bundle exec] fastlane android update_metadata
```

Upload metadata and screenshots only (no binary)

### android bump

```sh
[bundle exec] fastlane android bump
```

Bump version code for next release

### android release_internal

```sh
[bundle exec] fastlane android release_internal
```

Full release: bump → build → deploy to internal

### android release_production

```sh
[bundle exec] fastlane android release_production
```

Full release: bump → build → deploy to production

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
