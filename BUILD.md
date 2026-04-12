# Building Mezi

## Overview
Mezi is a JVM-targeting language compiler written in Java.

The current build works with a modern JDK and Maven.
The parser sources are generated from `src/main/java/mezic/parser/Parser.jjt`.
You do not need the Eclipse JavaCC plugin anymore.

## Requirements

1. Install a JDK
   - The current build has been verified with JDK 25
   - Java 9+ is supported through JDK `jmod` files
2. Install Maven
3. Install JavaCC tools so that these commands are available on `PATH`
   - `jjtree`
   - `javacc`

On Ubuntu, for example:

```bash
sudo apt-get update
sudo apt-get install -y maven javacc
```

## Build

From the project root:

```bash
mvn compile
```

This now performs the full parser generation and Java compilation flow automatically:

1. `jjtree Parser.jjt`
2. `javacc Parser.jj`
3. Java compilation through Maven

This assumes the `jjtree` and `javacc` command-line tools are already installed
and available on `PATH`. On Ubuntu, you can install them with:

```bash
sudo apt-get update
sudo apt-get install -y maven javacc
```

If Maven cannot write to the default local repository in your environment, you can override it:

```bash
mvn -Dmaven.repo.local=/tmp/m2 compile
```

## Clean Build

```bash
mvn clean compile
```

## Run Hello World Example

First build the project:

```bash
mvn compile
```

Then build the runtime classpath file:

```bash
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/mezi.cp
```

Run the bundled Hello World compiler driver:

```bash
java -cp target/classes:$(cat /tmp/mezi.cp) mezic.test.CompileHelloworld
```

Expected output:

```text
# Parsing(example.e000_helloworld) is completed

#
# Compilation is completed
Hello World!!
```

## Run Example Suites

Compile and execute the basic examples:

```bash
java -cp target/classes:$(cat /tmp/mezi.cp) mezic.test.CompileExamples
```

Compile and execute the grammar-oriented compiler tests:

```bash
java -cp target/classes:$(cat /tmp/mezi.cp) mezic.test.CompilerTest
```

Compile and execute the algorithm examples:

```bash
java -cp target/classes:$(cat /tmp/mezi.cp) mezic.test.CompileApsExamples
```

## Notes About Java Runtime Resolution

Older versions of this project assumed Java 8 and required `rt.jar`.

The current code now supports both runtime layouts:

- Java 8 style: `rt.jar`
- Java 9+ style: `jmods/*.jmod`

This is handled automatically by the test drivers and compiler runtime utilities.

## Notes About Generated Parser Files

Parser files are generated from `src/main/java/mezic/parser/Parser.jjt`
into `target/generated-sources/javacc/`.

Generated files include:

- `Parser.jj`
- `Parser.java`
- `ParserVisitor.java`
- `ParserTreeConstants.java`
- `AST*.java`

are generated from `Parser.jjt`.

If you delete generated parser sources, running `mvn compile` will regenerate them.
