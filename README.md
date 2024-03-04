### Various GraalJS tests

This repository contains various tests, benchmarks and [notes](./Notes.md) for [GraalJS](https://github.com/oracle/graaljs).

#### Benchmarks
Running the [Octane](https://github.com/chromium/octane) benchmark with various configrations of [GraalJS](https://github.com/oracle/graaljs).

The default Octane runner [`run.js`](https://github.com/chromium/octane/blob/570ad1ccfe86e3eecba0636c8f932ac08edec517/run.js) is a small script which uses [`load(source)`](https://github.com/oracle/graaljs/blob/master/docs/user/JavaScriptCompatibility.md#loadsource) to load (i.e. parse and execute) all the single benchmarks. Because I'm not sure sure if these dynamically loaded benchmarks are still tied to and cached together with `run.js`'s [`Source`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html) object, I decided to manually load all the single benmarks into a [single `Source` object](https://github.com/simonis/GraalJsTest/blob/30f6fc28747bb3da8679e3e7332214f785260423/src/main/java/io/simonis/graaljs/test/OctaneBenchmarkRunner.java#L130) along with a slightly modified [runner](https://github.com/simonis/GraalJsTest/blob/30f6fc28747bb3da8679e3e7332214f785260423/src/main/java/io/simonis/graaljs/test/OctaneBenchmarkRunner.java#L94-L128) which executes the benchmarks.

Another issue I've encountered when running the benchmarks more than one time in the same [`Context`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html) is that some of them depend on some global state which leads to failures if the benchmarks is executed a second time. For [Gameboy](https://github.com/simonis/GraalJsTest/blob/30f6fc28747bb3da8679e3e7332214f785260423/src/main/java/io/simonis/graaljs/test/OctaneBenchmarkRunner.java#L51-L62), [Box2D](https://github.com/simonis/GraalJsTest/blob/30f6fc28747bb3da8679e3e7332214f785260423/src/main/java/io/simonis/graaljs/test/OctaneBenchmarkRunner.java#L66-L77) and [Typescript](https://github.com/simonis/GraalJsTest/blob/30f6fc28747bb3da8679e3e7332214f785260423/src/main/java/io/simonis/graaljs/test/OctaneBenchmarkRunner.java#L81-L90) I could easily fix the problem in their corresponding `tearDown()` functions. For [Mandreel](./src/main/resources/resources/octane/mandreel.js) this was not trivial and the benchmark failed with `RangeError: Maximum call stack size exceeded` if exectuted more than once so I [excluded it completely](https://github.com/simonis/GraalJsTest/blob/30f6fc28747bb3da8679e3e7332214f785260423/src/main/java/io/simonis/graaljs/test/OctaneBenchmarkRunner.java#L124-L126) from the benchmark.

##### Building

```bash
$ git clone https://github.com/simonis/GraalJsTest.git
$ cd GraalJsTest
$ git submodule update --init --recursive
$ JAVA_HOME=<path-to-jdk17+> mvn clean package
```

Checking the dependencies:
```bash
$ mvn dependency:tree -Dverbose
[INFO] io.simonis:graal-js-test:jar:1.0-SNAPSHOT
[INFO] +- org.graalvm.polyglot:polyglot:jar:23.1.2:compile
[INFO] |  +- org.graalvm.sdk:collections:jar:23.1.2:compile
[INFO] |  \- org.graalvm.sdk:nativeimage:jar:23.1.2:compile
[INFO] |     \- (org.graalvm.sdk:word:jar:23.1.2:compile - omitted for duplicate)
[INFO] +- org.graalvm.polyglot:js-community:pom:23.1.2:compile
[INFO] |  +- org.graalvm.js:js-language:jar:23.1.2:runtime
[INFO] |  |  +- org.graalvm.regex:regex:jar:23.1.2:runtime
[INFO] |  |  |  +- (org.graalvm.truffle:truffle-api:jar:23.1.2:runtime - omitted for duplicate)
[INFO] |  |  |  \- (org.graalvm.shadowed:icu4j:jar:23.1.2:runtime - omitted for duplicate)
[INFO] |  |  +- org.graalvm.truffle:truffle-api:jar:23.1.2:runtime
[INFO] |  |  |  \- (org.graalvm.polyglot:polyglot:jar:23.1.2:runtime - omitted for duplicate)
[INFO] |  |  +- (org.graalvm.polyglot:polyglot:jar:23.1.2:runtime - omitted for duplicate)
[INFO] |  |  \- org.graalvm.shadowed:icu4j:jar:23.1.2:runtime
[INFO] |  \- org.graalvm.truffle:truffle-runtime:jar:23.1.2:runtime
[INFO] |     +- org.graalvm.sdk:jniutils:jar:23.1.2:runtime
[INFO] |     |  +- (org.graalvm.sdk:collections:jar:23.1.2:runtime - omitted for duplicate)
[INFO] |     |  \- (org.graalvm.sdk:nativeimage:jar:23.1.2:runtime - omitted for duplicate)
[INFO] |     +- (org.graalvm.truffle:truffle-api:jar:23.1.2:runtime - omitted for duplicate)
[INFO] |     \- (org.graalvm.truffle:truffle-compiler:jar:23.1.2:runtime - omitted for duplicate)
[INFO] \- org.graalvm.compiler:compiler:jar:23.1.2:runtime
[INFO]    +- (org.graalvm.sdk:collections:jar:23.1.2:runtime - omitted for duplicate)
[INFO]    +- org.graalvm.sdk:word:jar:23.1.2:compile
[INFO]    \- org.graalvm.truffle:truffle-compiler:jar:23.1.2:runtime
```

There's an alternative profile for building the older `23.0.3` version of the libraries:

```bash
$ JAVA_HOME=<path-to-jdk17+> mvn clean package -Pgraal-23-0-3
```

The dependencies are a little different here (and described in more detail in the [Truffle Unchained — Portable Language Runtimes as Java Libraries](https://medium.com/graalvm/truffle-unchained-13887b77b62c) blog):
```bash
$ mvn dependency:tree -Pgraal-23-0-3 -Dverbose
[INFO] io.simonis:graal-js-test:jar:1.0-SNAPSHOT
[INFO] +- org.graalvm.compiler:compiler:jar:23.0.3:runtime
[INFO] |  +- (org.graalvm.sdk:graal-sdk:jar:23.0.3:runtime - omitted for duplicate)
[INFO] |  \- org.graalvm.truffle:truffle-api:jar:23.0.3:compile
[INFO] |     \- (org.graalvm.sdk:graal-sdk:jar:23.0.3:compile - omitted for duplicate)
[INFO] +- org.graalvm.sdk:graal-sdk:jar:23.0.3:compile
[INFO] \- org.graalvm.js:js:jar:23.0.3:compile
[INFO]    +- org.graalvm.regex:regex:jar:23.0.3:compile
[INFO]    |  +- (org.graalvm.truffle:truffle-api:jar:23.0.3:compile - omitted for duplicate)
[INFO]    |  \- (com.ibm.icu:icu4j:jar:72.1:compile - omitted for duplicate)
[INFO]    +- (org.graalvm.truffle:truffle-api:jar:23.0.3:compile - omitted for duplicate)
[INFO]    +- (org.graalvm.sdk:graal-sdk:jar:23.0.3:compile - omitted for duplicate)
[INFO]    \- com.ibm.icu:icu4j:jar:72.1:compile
```

##### Running

The following examples use the GraalJS/Truffle modules downloaded automatically by Maven during the build. See [Notes.md](./Notes.md#building-graaljs) for how to build all these dependencies from source.

1. Running an OpenJDK without the GraalVM JVMCI compiler (this should work on any JavaSE-compatible JDK 17 and later but gives a warning and is pretty slow):
    ```bash
    $ java --module-path target/js-deps --add-modules org.graalvm.polyglot \
           -cp target/graal-js-test-1.0-SNAPSHOT.jar \
           -Diterations=5 io.simonis.graaljs.test.OctaneBenchmarkRunner
    ...
    [engine] WARNING: The polyglot engine uses a fallback runtime that does not support runtime compilation to native code.
    Execution without runtime compilation will negatively impact the guest application performance.
    The following cause was found: JVMCI is not enabled for this JVM. Enable JVMCI using -XX:+EnableJVMCI.
    ...
    ```
    <details>
      <summary>Console output</summary>

    ```
    Richards: 179
    DeltaBlue: 204
    Crypto: 298
    RayTrace: 535
    EarleyBoyer: 482
    RegExp: 116
    Splay: 1293
    SplayLatency: 5411
    NavierStokes: 339
    PdfJS: 835
    Mandreel: 106
    MandreelLatency: 1025
    Gameboy: 1265
    CodeLoad: 3747
    Box2D: 651
    zlib: 303
    Typescript: 1724
    ----
    Score (version 9): 586
    ...
    ```
    </details>


2. Running with the jar-version (i.e. "[jargraal](https://www.graalvm.org/latest/reference-manual/java/compiler/#compiler-operating-modes)") of the GraalVM JVMCI compiler. This requires JVMCI support (i.e. `-XX:+EnableJVMCI`) but doesn't require the usage of the Graal compiler as second tier JIT compiler in the JVM (i.e. `-XX:+UseJVMCICompiler`). The GraalVM Compiler/Truffle/GraalJS jar files version 23.1.2 still work with OpenJDK 17 and the performance is an order of magnitude faster:
    ```bash
    $ java --module-path target/js-deps --add-modules org.graalvm.polyglot \
           -cp target/graal-js-test-1.0-SNAPSHOT.jar \
           -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
           --upgrade-module-path target/compiler-deps \
           -Diterations=5 io.simonis.graaljs.test.OctaneBenchmarkRunner
    ```
    <details>
      <summary>Console output</summary>

    ```
    Richards: 1587
    DeltaBlue: 540
    Crypto: 3769
    RayTrace: 2017
    EarleyBoyer: 3310
    RegExp: 191
    Splay: 982
    SplayLatency: 4361
    NavierStokes: 1762
    PdfJS: 567
    Mandreel: 500
    MandreelLatency: 1450
    Gameboy: 1152
    CodeLoad: 1162
    Box2D: 733
    zlib: 6129
    Typescript: 4137
    ----
    Score (version 9): 1403
    ...
    Score (version 9): 3118
    ...
    Score (version 9): 3285
    ...
    Score (version 9): 3536
    ...
    Score (version 9): 3449
    ```
    </details>

3. Running with the native GraalVM JVMCI compiler (see [Notes.md](./Notes.md#building-libgraal-with-mandrel-and-openjdk) for how to build a native version of the compiler). This does not only require that `libjvmcicompiler.so` was built with the same JDK we are now running on, it also requires that the version of libgraal is compatible with the version of the JDK (and JVMCI it supports) we are running on. E.g. we can run with libgraal 23.1.2 compiled with JDK 21 on JDK 21 or with libgraal 23.0.3 compiled with JDK 17 on JDK 17, but not with libgraal 23.1.2 compiled with JDK 17 on JDK 17.
    ```bash
    $ java --module-path target/js-deps --add-modules org.graalvm.polyglot \
           -cp target/graal-js-test-1.0-SNAPSHOT.jar \
           -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCINativeLibrary \
           -XX:JVMCILibPath=<path-to>/mandrel/sdk/mxbuild/linux-amd64/libjvmcicompiler.so.image \
           -Diterations=5 io.simonis.graaljs.test.OctaneBenchmarkRunner
    ```
#### Benchmark results

The following benchmark results were taken on a [`c5.metal` EC2 instance](https://aws.amazon.com/ec2/instance-types/c5/). They display the lowest, highest and average score out of 20 Octane benchmark runs in a single `Context` (except for the "interpreted" runs which didn't use "libgraal" at all and only ran for 3 times). They all used version `23.1.2` of the GraalVM libraries and ran with `-XX:ReservedCodeCacheSize=2g -XX:InitialCodeCacheSize=2g -XX:NonProfiledCodeHeapSize=1500m -XX:-UseCodeCacheFlushing` to make sure that insufficient CodeCache space won't affect the results. I turns out that the CodeCache usage is constantly growing with every new Octane run and never gets to a steady state (reaching about ~1gb of code in the non-profiled segment (measured by `jcmd OctaneBenchmarkRunner Compiler.codecache`) and more than 30.000 compiled methods as displayed by the output of `-Dpolyglot.engine.CompilationStatistics=true`). This behavior might be due to some dynamic code execution (i.e. using `eval()`) in the benchmark but requires more detailed analysis.

| JDK | min | max | avg. |
|--------------------|--------|---------|---------|
|  corretto17  |  653  |  692  |  669  |
|  corretto21  |  663  |  701  |  680  |
|  corretto17-jvmci  |  12561  |  14300  |  13716  |
|  corretto21-jvmci  |  9479  |  14100  |  12671  |
|  corretto17-jvmci-compiler  |  12240  |  14806  |  13882  |
|  corretto21-jvmci-compiler  |  9752  |  14155  |  12909  |
|  graalvm21-jvmci  |  9210  |  14630  |  12641  |
|  graalvm21-jvmci-native  |  10451  |  14216  |  12673  |
|  graalvm21-jvmci-compiler  |  10271  |  14642  |  13024  |
|  graalvm21-jvmci-compiler-native  |  9274  |  14339  |  12669  |
|  graalvm21-oracle-jvmci  |  9808  |  14957  |  12962  |
|  graalvm21-oracle-jvmci-native  |  10095  |  14164  |  12612  |
|  graalvm21-oracle-jvmci-compiler  |  10969  |  14663  |  13450  |
|  graalvm21-oracle-jvmci-compiler-native  |  10329  |  14397  |  13436  |

`*-jvmci` means `-XX:+EnableJVMCI`, `*-jvmci-compiler` means `-XX:+EnableJVMCI -XX:+UseJVMCICompiler`, `*-jvmci-native` means `-XX:+EnableJVMCI -XX:+UseJVMCINativeLibrary` and `*-jvmci-compiler-native` means `-XX:+EnableJVMCI -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary`.

One interesting result is that te results which use the native "libgraal" compiler (i.e. `-XX:+UseJVMCINativeLibrary`) aren't really better than the ones which only use the "jargraal" version. I would have expected that running with "jargraal" would result in a significantly lower low score and a slightly lower average score. But a single run of the Octane suite takes about 2 minutes on a `c5.metal` instance and the host has more than enough free CPUs so the startup-up costs of JIT-compiling "jargraal" at startup might not be significant enough for this use case.

Another interesting aspect is that JDK 17 seems to have higher low and average scores compared to JDK 21 and that the Oracle GraalVM version (previously known as GraalVM Enterprise Edition) doesn't seem to be significantly faster for this workload compared to the GraalVM community edition.