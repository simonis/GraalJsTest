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

The following benchmark results were taken on a [`c5.metal` EC2 instance](https://aws.amazon.com/ec2/instance-types/c5/). They display the lowest, highest and average score out of 20 Octane benchmark runs in a single `Context` (except for the "interpreted" runs which didn't use "libgraal" at all and only ran for 3 times). They all ran with `-XX:ReservedCodeCacheSize=2g -XX:InitialCodeCacheSize=2g -XX:NonProfiledCodeHeapSize=1500m -XX:-UseCodeCacheFlushing` to make sure that insufficient CodeCache space won't affect the results. I turns out that the CodeCache usage is constantly growing with every new Octane run and never gets to a steady state (reaching about ~1gb of code in the non-profiled segment (measured by `jcmd OctaneBenchmarkRunner Compiler.codecache`) and more than 30.000 compiled methods as displayed by the output of `-Dpolyglot.engine.CompilationStatistics=true`). This behavior might be due to some dynamic code execution (i.e. using [`eval()`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval)) in the benchmark but requires more detailed analysis.

##### Graal version 23.1.2

| JDK | min | max | avg. |
|--------------------|--------|---------|---------|
|  corretto17  |  653  |  692  |  669  |
|  corretto21  |  663  |  701  |  680  |
|  corretto17-nashorn  |  4434  |  4898  |  4765  |
|  corretto21-nashorn  |  4215  |  4870  |  4739  |
|  corretto17-jvmci  |  12561  |  14300  |  13716  |
|  corretto17-jvmci-compiler  |  12240  |  14806  |  13882  |
|  corretto21-jvmci  |  9479  |  14100  |  12671  |
|  corretto21-jvmci-native  |  9456  |  14458  |  12828  |
|  corretto21-jvmci-compiler  |  9752  |  14155  |  12909  |
|  corretto21-jvmci-compiler-native  |  9017  |  14540  |  12943  |
|  graalvm21-jvmci  |  9210  |  14630  |  12641  |
|  graalvm21-jvmci-native  |  10451  |  14216  |  12673  |
|  graalvm21-jvmci-compiler  |  10271  |  14642  |  13024  |
|  graalvm21-jvmci-compiler-native  |  9274  |  14339  |  12669  |
|  graalvm21-oracle-jvmci  |  9742  |  16763  |  13624  |
|  graalvm21-oracle-jvmci-native  |  9680  |  16744  |  14257  |
|  graalvm21-oracle-jvmci-compiler  |  9535  |  16281  |  14622  |
|  graalvm21-oracle-jvmci-compiler-native  |  10192  |  16553  |  14247  |

`*-jvmci` means `-XX:+EnableJVMCI`, `*-jvmci-compiler` means `-XX:+EnableJVMCI -XX:+UseJVMCICompiler`, `*-jvmci-native` means `-XX:+EnableJVMCI -XX:+UseJVMCINativeLibrary` and `*-jvmci-compiler-native` means `-XX:+EnableJVMCI -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary`. Notice that "libgraal" 23.1.2 (i.e. `libjvmcicompiler.so`, the native version of the Graal Compiler) only works with OpenJDK 21 but not with 17 (even if build with JDK 17 as described in [Notes.md](./Notes.md#building-libgraal-with-mandrel-and-openjdk)). "jargraal" 23.1.2, the pure-Java version of the compiler seems to work well if used for JS/Truffle and also works as `-XX:+UseJVMCICompiler` although with some warnings like:
```
Warning: Systemic Graal compilation failure detected: 5 of 206 (2%) of compilations failed during last 68 ms [max rate set by SystemicCompilationFailureRate is 1%]. To mitigate systemic compilation failure detection, set SystemicCompilationFailureRate to a higher value. To disable systemic compilation failure detection, set SystemicCompilationFailureRate to 0. To get more information on compilation failures, set CompilationFailureAction to Print or Diagnose.
```
I haven't checked if this impacts the performance of JIT-compiled Java code, but it doesn't seem to affect the JavaScript performance. Running with `-Dgraal.CompilationFailureAction=Print` indeed confirms that the failures are for JVMCICompiler compilations and not Truffle ones:
```
Thread[JVMCI CompilerThread2,9,system]: Compilation of java.util.zip.ZipFile$Source.readAt(byte[], int, int, long) @ -1 failed: java.lang.NoSuchMethodError: 'jdk.vm.ci.meta.AllocatableValue jdk.vm.ci.code.StackLockValue.getSlot()'
	at jdk.internal.vm.compiler/org.graalvm.compiler.lir.LIRFrameState.visitValues(LIRFrameState.java:167)
	at jdk.internal.vm.compiler/org.graalvm.compiler.lir.LIRFrameState.visitEachState(LIRFrameState.java:108)
...
	at jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.HotSpotGraalCompiler.compileMethod(HotSpotGraalCompiler.java:110)
	at jdk.internal.vm.ci/jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.compileMethod(HotSpotJVMCIRuntime.java:936)
```

One interesting outcome is that the results which use the native "libgraal" compiler (i.e. `-XX:+UseJVMCINativeLibrary`) aren't really better than the ones which only use the "jargraal" version. I would have expected that running with "jargraal" would result in a significantly lower low score and a slightly lower average score. But a single run of the Octane suite takes about 2 minutes on a `c5.metal` instance and the host has more than enough free CPUs so the startup-up costs of JIT-compiling "jargraal" at startup might not be significant enough for this use case.

Another interesting aspect is that JDK 17 seems to have considerable better low scores compared to JDK 21 (even compared to the Oracle GraalVM version) and these numbers get confirmed by the JDK 17 benchmarks with GraalVM version `23.0.3` in the next section. Oracle GraalVM [graalvm21-oracle-*](https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-x64_bin.tar.gz) (previously known as GraalVM Enterprise Edition) has ~10% better maximum and average scores for this workload compared to the GraalVM community edition [graalvm21-*](https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz). Finally, [Nashorn](https://github.com/openjdk/nashorn), although not actively developed since JDK 15, is still almost an order of magnitude faster than GraalJS without JVMCI support and reaches about one third of the GraalJS top performance.

##### Graal version 23.0.2/23.0.3

| JDK | min | max | avg. |
|--------------------|--------|---------|---------|
|  corretto17-23.0.3-jvmci  |  13113  |  14840  |  14514  |
|  corretto17-23.0.3-jvmci-native  |  3711  |  9012  |  7598  |
|  graalvm17-23.0.2-jvmci  |  12945  |  15139  |  14523  |
|  graalvm17-23.0.2-jvmci-native  |  12497  |  15028  |  14467  |

The Graal 23.0.x stream is still supported for JDK 17 and [graalvm-community-jdk-17.0.9](https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-17.0.9/graalvm-community-jdk-17.0.9_linux-x64_bin.tar.gz) is the latest community edition available for download from the GraalVM Github project. This release already contains some of the Graal modules like `org.graalvm.truffle` (in `lib/truffle/truffle-api.jar`) but the JavaScript support has to be additionally installed wit the help of the `gu` tool:
```bash
$ cd graalvm-community-openjdk-17.0.9+9.1
$ ./bin/gu install js
...
Installing new component: TRegex (org.graalvm.regex, version 23.0.2)
Installing new component: ICU4J (org.graalvm.icu4j, version 23.0.2)
Installing new component: Graal.js (org.graalvm.js, version 23.0.2)
```
I couldn't manage to run the benchmarks with graalvm-community-jdk-17.0.9 and the `23.0.3` version of the libraries I built (or downloaded from MavenCentral), so the `graalvm17-*` runs are with the builtin Graal libraries at version 23.0.2 and the `corretto17-*` runs with the libraries at version `23.0.3` as downloaded from Maven Central (see [pom.xml](./pom.xml)).

The numbers are similar to the JDK 17 numbers with `23.1.2` with a single exception. The native version `20.0.3` of the compiler ("libgraal") build from the Mandrel project (see [Notes.md](./Notes.md#building-libgraal-with-mandrel-and-openjdk)) with OpenJDK 17 performs very poor compared to the pure-Java ("jargraal") version of the compiler. The reason isn't currently clear to me, but I suspect that the graalvm-community-jdk-17.0.9 edition (which is based on the [GraalVM labs-openjdk-17](https://github.com/graalvm/labs-openjdk-17)) contains JVMCI changes which are not in OpenJDK 17 and which are required in order to reach peak performance.

Interestingly enough, "libgraal" `23.0.3` doesn't report any errors, but if running with `-Dpolyglot.engine.CompilationStatistics=true` we can see that it only compiles about ~7500 compilation units:
```
[engine] Truffle runtime statistics for engine 1
    Compilations                : 7481
      Success                   : 7396
      Temporary Bailouts        : 71
      Permanent Bailouts        : 1
...
```
while the "jargraal" variant (as well as the "libgraal" version `23.0.2` bundled with graalvm-community-jdk-17.0.9) compile about ~32.000 compilation units:
```
[engine] Truffle runtime statistics for engine 1
    Compilations                : 32023
      Success                   : 31375
      Temporary Bailouts        : 679
      Permanent Bailouts        : 1
...
```

##### Misc

In order to verify if Truffle uses the GraalVM compiler or if `-XX:+UseJVMCI` is indeed triggering the usage of the GraalVM compiler as HotSpot high-tier JIT compiler, the `-XX:+CITime` option comes in handy. At VM exit it displays compiler statistics which look as follows if running with `-XX:-UseJVMCI`:
```
    C1 Compile Time:        1.439 s
    C2 Compile Time:       10.910 s
    ...
```
There's no JVMCI section in the output because JVMCI is disabled.

If we enable JVMCI but do not use it as JIT compiler with `-XX:+EnableJVMCI -XX:-UseJVMCICompiler`:
```
    C1 Compile Time:        7.084 s
    C2 Compile Time:       52.016 s

    JVMCI CompileBroker Time:
       Compile:          0.000 s
       Install Code:     0.000 s (installs: 0, CodeBlob total size: 0, CodeBlob code size: 0)

    JVMCI Hosted Time:
       Install Code:    97.986 s (installs: 10155, CodeBlob total size: 201450216, CodeBlob code size: 95619200)
```
You can see that C1 and C2 are used as HotSpot JIT compilers and JVMCI is only used in "*Hosted*" mode (i.e. triggered directly by the Truffle interpreter).

With `-XX:+EnableJVMCI -XX:+UseJVMCICompiler` (independently of the usage of "libgraal" or "jargraal") the output looks as follows:
```
    C1 Compile Time:        8.003 s

    JVMCI CompileBroker Time:
       Compile:         55.869 s
       Install Code:     3.938 s (installs: 5050, CodeBlob total size: 14931256, CodeBlob code size: 7234888)

    JVMCI Hosted Time:
       Install Code:    99.125 s (installs: 10242, CodeBlob total size: 203402608, CodeBlob code size: 96381704)
```
You can see that there's no C2 usage at all and instead HotSpot uses the JVMCI compiler directly from the "*CompileBroker*" instead of C2 for the highest compilation tear.
