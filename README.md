# Java Tree-sitter

[![CI][ci-shield]](https://github.com/tree-sitter/java-tree-sitter/actions/workflows/ci.yml)
[![central][central-shield]](https://central.sonatype.com/artifact/io.github.tree-sitter/jtreesitter)
[![docs][docs-shield]][docs]

Java bindings to the [tree-sitter] parsing library.

## Usage

See the [documentation][docs].

## Building

- Install JDK 22 and set `JAVA_HOME` to it
- Download [jextract] and add it to your `PATH`
- Install the `tree-sitter` & `tree-sitter-java` libraries

```bash
git clone https://github.com/tree-sitter/java-tree-sitter
cd java-tree-sitter
git submodule update --init
mvn test
```

## Alternatives

These alternatives support older JDK versions or Android:

- [tree-sitter/kotlin-tree-sitter](https://github.com/tree-sitter/kotlin-tree-sitter) (JDK 17+, Android SDK 23+, Kotlin 1.9)
- [bonede/tree-sitter-ng](https://github.com/bonede/tree-sitter-ng) (JDK 8+)
- [seart-group/java-tree-sitter](https://github.com/seart-group/java-tree-sitter) (JDK 11+)
- [AndroidIDEOfficial/android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter) (Android SDK 21+)

[tree-sitter]: https://tree-sitter.github.io/tree-sitter/
[ci-shield]: https://img.shields.io/github/actions/workflow/status/tree-sitter/java-tree-sitter/ci.yml?logo=github&label=CI
[central-shield]: https://img.shields.io/maven-central/v/io.github.tree-sitter/jtreesitter?logo=sonatype&label=Maven%20Central
[docs-shield]: https://img.shields.io/github/deployments/tree-sitter/java-tree-sitter/github-pages?logo=githubpages&label=API%20Docs
[docs]: https://tree-sitter.github.io/java-tree-sitter/
[FFM]: https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html
[jextract]: https://jdk.java.net/jextract/
