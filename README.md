# The Mezi Programming Language

Mezi is an general purpose open source programming language. 

This programming language is an non-java JVM language. The source code written in the Mezi is compiled to JVM byte code.

Mezi is interested in the followings.

1. Expression oriented grammar
2. Type generalization, and type inference
3. Identifying definition and assignment
4. Thread serialization & live migration (Future Plan)

## Getting Started
[Building Mezi compiler & Helloworld example](BUILD.md)

## Build Notes
The project can be built with a modern JDK and Maven.

- `mvn compile` automatically runs JJTree and JavaCC for `src/main/java/mezic/parser/Parser.jjt`, assuming the `jjtree` and `javacc` tools are installed
- On Ubuntu, you can install those tools with `sudo apt-get update && sudo apt-get install -y maven javacc`
- Generated parser sources are written under `target/generated-sources/javacc`
- Example runners under `src/main/java/mezic/test` work on both Java 8-style `rt.jar` layouts and Java 9+ `jmod` layouts
- The current documented flow has been verified with JDK 25

## Example Mezi code

1. [Simple grammar examples](https://github.com/Samsung/MeziLang/tree/master/code_base/example)

2. [Simple algorithm implementation examples](https://github.com/Samsung/MeziLang/tree/master/code_base/aps) 


## Contributing
The project can only accept contributions which are licensed under the [Apache License 2.0](LICENSE) and are signed according to the Mezi languagge [Developer's Certificate of Origin](DCO.md). For further information please see our [Contribution Guidelines](CONTRIBUTING.md).

## License
Mezi languagge is open source software under the [Apache License 2.0](LICENSE). Complete license and copyright information can be found in the source code.
