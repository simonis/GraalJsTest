### Testing GraalJS

#### Building

```
$ $ git clone https://github.com/simonis/GraalJsTest.git
$ cd GraalJsTest
$ git submodule update --init --recursive
$ JAVA_HOME=<path-to-jdk17+> mvn clean package
```

Checking the dependencies:
```
$ mvn dependency:tree 
[INFO] io.simonis:graal-js-test:jar:1.0-SNAPSHOT
[INFO] +- org.graalvm.polyglot:polyglot:jar:23.1.2:compile
[INFO] |  +- org.graalvm.sdk:collections:jar:23.1.2:compile
[INFO] |  \- org.graalvm.sdk:nativeimage:jar:23.1.2:compile
[INFO] +- org.graalvm.polyglot:js-community:pom:23.1.2:compile
[INFO] |  +- org.graalvm.js:js-language:jar:23.1.2:runtime
[INFO] |  |  +- org.graalvm.regex:regex:jar:23.1.2:runtime
[INFO] |  |  +- org.graalvm.truffle:truffle-api:jar:23.1.2:runtime
[INFO] |  |  \- org.graalvm.shadowed:icu4j:jar:23.1.2:runtime
[INFO] |  \- org.graalvm.truffle:truffle-runtime:jar:23.1.2:runtime
[INFO] |     \- org.graalvm.sdk:jniutils:jar:23.1.2:runtime
[INFO] \- org.graalvm.compiler:compiler:jar:23.1.2:runtime
[INFO]    +- org.graalvm.sdk:word:jar:23.1.2:compile
[INFO]    \- org.graalvm.truffle:truffle-compiler:jar:23.1.2:runtime
```

#### Running

Without the GraalVM compiler:
```
$ java --module-path target/js-deps --add-modules org.graalvm.polyglot \
       -cp target/graal-js-test-1.0-SNAPSHOT.jar \
       -Diterations=5 io.simonis.graaljs.test.RunOctaneBenchmark
...
[engine] WARNING: The polyglot engine uses a fallback runtime that does not support runtime compilation to native code.
Execution without runtime compilation will negatively impact the guest application performance.
The following cause was found: JVMCI is not enabled for this JVM. Enable JVMCI using -XX:+EnableJVMCI.
...
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

With the GraalVM compiler:
```
$ java --module-path target/js-deps --add-modules org.graalvm.polyglot \
       -cp target/graal-js-test-1.0-SNAPSHOT.jar \
       -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
       --upgrade-module-path target/compiler-deps \
       -Diterations=5 io.simonis.graaljs.test.RunOctaneBenchmark
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
