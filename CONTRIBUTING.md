# Contributing to bloop-config

Thanks for being willing to contribute!

`bloop-config` is a small library cross published for 2.11-2.13 and for the JVM
and JS platforms. It's build with
[Mill](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html).

## Building the project

To fully build the project:

```sh
./mill __.compile
```

## Testing the project

To test everything in the project:

```sh
./mill __.test
```

## Formatting and Fixing

This project uses both [Scalafmt](https://scalameta.org/scalafmt/) and
[Scalafix](https://scalacenter.github.io/scalafix/).

### Scalafmt

To format everything:

```sh
./mill __.reformat
```

To check formatting:

```sh
./mill __.checkFormat
```

### Scalaffix

To fun Scalafix:

```sh
./mill __.fix
```

To Scalafix check:

```sh
./mill __.fix --check
```

## MiMia Checks

This projects uses [MiMa](https://github.com/lightbend/mima) to ensure binary
compatibility is kept.

Run MiMa checks:

```sh
./mill __.mimaReportBinaryIssues
```

## Targeting a single module

Any of the tasks above can be ran on a single target. For example if you wanted
to test _only_ the JVM 2.13 modules (assuming you're on 2.13.10) you could do
the following:

```sh
./mill config.jvm[2.13.10].test.test
```

Maybe you want to run MiMa _only_ on your ScalaJS targets:

```sh
./mill config.js[__].mimaReportBinaryIssues
```

## Releasing

This projects uses
[`mill-ci-release`](https://github.com/ckipp01/mill-ci-release) for easy
releasing and versioning. Everything is based on git tags, so to trigger a new
release, just add a new git tag and then push that tag:

```sh
git push upstream --tags
```

There is also a unique snapshot produced on every merge to `main`.
