### GraalVM

- [Isolates and Compressed References: More Flexible and Efficient Memory Management via GraalVM](https://medium.com/graalvm/isolates-and-compressed-references-more-flexible-and-efficient-memory-management-for-graalvm-a044cc50b67e) by Christian Wimmer, Jan 25, 2019
- [Polyglot Isolates](https://www.graalvm.org/latest/reference-manual/embed-languages/#polyglot-isolates). On Oracle GraalVM, a Polyglot engine can be configured to run in a dedicated Native Image isolate (since version 23.1). A polyglot engine in this mode executes within a VM-level fault domain with a dedicated garbage collector and JIT compiler. Polyglot isolates are useful for polyglot sandboxing. Running languages in an isolate works with HotSpot and Native Image host virtual machines. Languages used as polyglot isolates can be downloaded from Maven Central using the `-isolate` suffix.
  - What's the difference between [`org.graalvm.js`](https://mvnrepository.com/artifact/org.graalvm.js) and [`org.graalvm.polyglot`](https://mvnrepository.com/artifact/org.graalvm.polyglot) on Maven Central?
  - e.g. [`org.graalvm.polyglot » js-isolate`](https://mvnrepository.com/artifact/org.graalvm.polyglot/js-isolate) is just a POM with a dependency on [`org.graalvm.js » js-isolate`](https://mvnrepository.com/artifact/org.graalvm.js/js-isolate) and [`org.graalvm.truffle » truffle-enterprise`](https://mvnrepository.com/artifact/org.graalvm.truffle/truffle-enterprise).
  - `js-isolate-23.1.2-sources.jar` only contains the sources of `PolyglotIsolateResource.java` (and `PolyglotIsolateResourceProvider.java` which seems to be generated from the first one).
  - `js-isolate-23.1.2-sources.jar` contains a native version of the `org.graalvm.js.isolate` module for several platforms (e.g. `linux/amd64/libvm/libjsvm.so` (~200mb), `windows/amd64/libvm/jsvm.dll`, ..).
    - ToDo: does the native shared library already contain precompiled version of Truffle and GraalJS?
- [Polyglot Sandboxing](https://www.graalvm.org/latest/security-guide/polyglot-sandbox/). This feature (see [`org.graalvm.polyglot.SandboxPolicy`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/SandboxPolicy.html)) is available since version 23 and presumably only available in GraalVM Enterprise.
  - ToDo: what'S the difference between "Polyglot Isolates" and "Polyglot Sandboxing"? Are they orthogonal? Is it possible to combine Sandboxing with Isolates?
  - ToDo: find out at which level (i.e. GraalJS, Polyglot, Truffle, GraalVM Compiler, GraalVM SubstrateVM) this feature is implemented?
    - Looks like it is a [Truffle enterprise feature](https://www.graalvm.org/latest/reference-manual/embed-languages/#switching-to-the-fallback-engine): "*Since `truffle-enterprise` is excluded, the fallback engine does not support advanced extensions like sandbox limits or polyglot isolates*".
- [Mandrel: A community distribution of GraalVM for the Red Hat build of Quarkus](https://developers.redhat.com/blog/2020/06/05/mandrel-a-community-distribution-of-graalvm-for-the-red-hat-build-of-quarkus/) by Mark Little, June 5, 2020
- [Mandrel: A specialized distribution of GraalVM for Quarkus](https://developers.redhat.com/blog/2021/04/14/mandrel-a-specialized-distribution-of-graalvm-for-quarkus) by Severin Gehwolf, April 14, 2021
- [GraalVM, Galahad, and a New Release Schedule](https://medium.com/graalvm/graalvm-galahad-and-a-new-release-schedule-d081d1031bba)
  - Starting with JDK 20 in March 2023, GraalVM will follow the JDK’s six-month release cadence (before GraalVM Enterprise and Community Editions have followed a three-month cadence).
  - Starting with JDK 20, GraalVM releases will only support the latest JDK version (just as Oracle OpenJDK releases do).
  - GraalVM will adopt the JDK’s release numbering scheme based on the supported Java version. To avoid confusion with older releases, new releases will be named GraalVM for JDK &lt;Java version&gt;.
- [Introducing the GraalVM Free License](https://blogs.oracle.com/java/post/graalvm-free-license).
  - Oracle is making Oracle GraalVM for JDK 17 and Oracle GraalVM for JDK 20 (previously known as Oracle GraalVM Enterprise) and subsequent releases, including all quarterly security updates, free.
  - These releases will be available under the [GraalVM Free Terms and Conditions](https://www.oracle.com/downloads/licenses/graal-free-license.html) (GFTC) license. This license .. permits free use for all users, even for production use.
  - For designated Long Term Support releases (GraalVM for JDK 17), Oracle will provide these free GFTC releases until one full year after the subsequent LTS release.
- [GraalVM Community Edition Release Calendar](https://www.graalvm.org/release-calendar/) (also see [Oracle GraalVM Release Calendar](https://docs.oracle.com/en/graalvm/release-calendar.html))

### GraalVM JavaScript

- [Run GraalVM JavaScript on a Stock JDK (JDK17)](https://www.graalvm.org/jdk17/reference-manual/js/RunOnJDK/)
  - [GraalJS on Maven Central (org.graalvm.js/js)](https://mvnrepository.com/artifact/org.graalvm.js/js) - currently 23.0.3 (Jan 17, 2024)
  - Binary packages have to be installed with [GraalVM Updater](https://www.graalvm.org/jdk17/reference-manual/js/) ("*Since GraalVM 22.2, the JavaScript support is packaged in a separate GraalVM component. It can be installed with the GraalVM Updater*"). However, the latest version on GitHub is [GraalJS 23.0.2](https://github.com/oracle/graaljs/releases/tag/graal-23.0.2)
  - There are two versions available: `graaljs-23.0.2-linux-amd64.tar.gz` which basically only contains the `js` binary plus `libjsvm.so` and `js-installable-svm-java17-linux-amd64-23.0.2.jar` which also contains the former, but in addition `graaljs.jar` and `graaljs-launcher.jar`. Is it correct that `libjsvm.so` contains a natively compiled version of both, Truffle and GraalJS?
- [Run GraalVM JavaScript on a Stock JDK (JDK21)](https://www.graalvm.org/jdk21/reference-manual/js/RunOnJDK/)
  - [Js Community on Maven Central (org.graalvm.polyglot/js-community)](https://mvnrepository.com/artifact/org.graalvm.polyglot/js-community) - currently 23.1.2 (Jan 17, 2024)
  - https://github.com/graalvm/graaljs redirects to https://github.com/oracle/graaljs
  - Binary downloads of [GraalJS - GraalVM Community 23.1.2](https://github.com/oracle/graaljs/releases/tag/graal-23.1.2) (i.e. GraalJS 23.1.2 bundled with GraalVM Community Edition for JDK 21.0.2)
    - What's the difference between `graaljs-23.1.2-linux-amd64.tar.gz` and `graaljs-community-23.1.2-linux-amd64.tar.gz` (or `graaljs-jvm-23.1.2-linux-amd64.tar.gz` and `graaljs-community-jvm-23.1.2-linux-amd64.tar.gz`) respectively?
    *[As of GraalVM for JDK 21, the JavaScript (GraalJS) and Node.js runtimes are available as standalone distributions](https://www.graalvm.org/jdk21/reference-manual/js/). Two standalone language runtime options are available for both Oracle GraalVM and GraalVM Community Edition: a Native Image compiled native launcher or a JVM-based runtime (included). To distinguish between them, the GraalVM Community Edition version has the suffix -community in the name... A standalone that comes with a JVM has a -jvm suffix in a name*.
    - The `community-jvm` version contains a lot of additional modules compared to Oracle's `-jvm` version (e.g. `chromeinspector.jar`, `dap.jar`, `insight.jar`, `insight-heap.jar`, etc.). This can be seen when running `js --version:graalvm` which prints no *Installed Tools* at all for the non-community version.
    - On the ot her hand, the non `-jvm` version from ORacle contains all the *Installed Tools* from the corresponding `-coommunity-jvm` version plus the additional [`Sandbox`](https://www.graalvm.org/latest/security-guide/polyglot-sandbox/) tool.
    - The JVM in the `community-jvm` version contains the two additional (apparently empty, upgradeable?) modules `jdk.compiler.graal` and `jdk.compiler.graal.management` (see `graaljs-community-23.1.2-linux-amd64/jvm/release`).
  - What's the difference between the tags `vm-ee-23.1.2`, `vm-ce-23.1.2`, `vm-23.1.2` and `graal-23.1.2` in the https://github.com/oracle/graaljs repository and which tag/branch corresponds to the GitHub release `graaljs-community-23.1.2-linux-amd64.tar.gz` and the Maven central version `23.1.2`. At least for now, all the tags seem to be identical?
  - The `-jvm` versions contain the native version of the GraalVM Compiler (i.e. `jvm/lib/libjvmcicompiler.so`) and enables it with `-XX:+EnableJVMCIProduct -XX:+UseJVMCINativeLibrary` (`EnableJVMCIProduct` means *Allow JVMCI to be used in product mode. This alters a subset of JVMCI flags to be non-experimental, defaults `UseJVMCICompiler` and `EnableJVMCI` to true and defaults `UseJVMCINativeLibrary` to true if a JVMCI native library is available*).

#### Building GraalJS

Building GraalJS (version `23.1.2`) along with its dependencies from source:
```bash
$ mkdir Graal
$ cd Graal
$ git clone https://github.com/graalvm/mx.git
$ export PATH=$PWD/mx:$PATH
$ git clone https://github.com/oracle/graal.git
$ cd graal/compiler
$ git checkout vm-ce-23.1.2
$ export JAVA_HOME=/share/software/Java/corretto-17
$ mx build
$ cd ../truffle/
$ mx build
$ cd ../sdk/
$ mx build
$ cd ../regex/
$ mx build
$ cd ../../
$ git clone https://github.com/oracle/graaljs
$ cd graaljs/graal-js/
$ git checkout vm-ce-23.1.2
# dont't do 'mx sforceimports' as adviced in https://github.com/oracle/graaljs/blob/master/docs/Building.md because it will mess up your `../../graal` repo to a *strange* revision (defined in the 'imports.suites.regex' section of 'mx.graal-js/suite.py')!
$ mx build
```
Notice that although GraalVM `23.1` is targeted for JDK 21 the pure Java artifacts built from the tag `vm-ce-23.1.2` of these libraries can be build (and the results can be used) with OpenJDK 17 and later.

### GraalVM Truffle

- [Truffle Unchained — Portable Language Runtimes as Java Libraries](https://medium.com/graalvm/truffle-unchained-13887b77b62c). Also see [GR-43819: Split Graal-SDK into new modules: polyglot, word, collections and nativeimage](https://github.com/oracle/graal/pull/7171) and the [Changelog entry for Graal Version 23.1.0](https://github.com/oracle/graal/blob/master/sdk/CHANGELOG.md#version-2310).
  - ([GR-47917](https://github.com/oracle/graal/pull/7239)) Added class-path isolation if polyglot is used from the class-path. At class initialization time and if polyglot is used from the class-path then the polyglot implementation spawns a module class loader with the polyglot runtime and language implementations. This allows to use an optimized runtime even if languages and polyglot are used from the class-path. Note that for best performance, it is recommended to load polyglot and the languages from the module-path. Comment from [org.graalvm.polyglot.Engine$ClassPathIsolation](https://github.com/oracle/graal/blob/774141206b82771ab80e0ea38d26d660292eb8ab/sdk/src/org.graalvm.polyglot/src/org/graalvm/polyglot/Engine.java#L1768): "*If Truffle is on the class-path (or a language), we do not want to expose these classes to embedders (users of the polyglot API). Unless disabled, we load all Truffle jars on the class-path in a special module layer instead of loading it through the class-path in the unnamed module*". This feature is controlled by `-Dpolyglotimpl.DisableClassPathIsolation` which defaults to `true` and observed by `-Dpolyglotimpl.TraceClassPathIsolation=true`.
  - ToDo: do we want to GraalJS & Truffle on the class or on the module path? The Graal Compiler has to be placed on the `--upgrade-module-path`. Also, if we put GraalJS & Truffle on the module path but our application classes are running form the class path we have to explicitly do `--add-modules org.graalvm.polyglot`.
- [Optimizing Truffle Interpreters](https://docs.oracle.com/en/graalvm/jdk/17/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/#optimizing-truffle-interpreters)
  - E.g. `-Dpolyglot.engine.TraceCompilation=true` or `-Dpolyglot.engine.CompilationStatistics=true` (which requires ` Engine.newBuilder("js").allowExperimentalOptions(true)`).
  - `-Dpolyglot.engine.TraceCompilation=true` will give you output like:
    ```
    [engine] opt done   id=314   Math.floor                                         |Tier 1|Time   334( 225+109 )ms|AST    2|Inlined   0Y   0N|IR    103/   212|CodeSize    1024|Addr 0x7f5178b3b060|Timestamp 34053963283959|Src <builtin>:1
    ```
    Together with `-Dgraal.PrintCompilation=true` (see [below](#graalvm-compiler)) this prints:
    ```
    [engine] opt done   id=314   Math.floor                                         |Tier 1|Time   334( 225+109 )ms|AST    2|Inlined   0Y   0N|IR    103/   212|CodeSize    1024|Addr 0x7f5178b3b060|Timestamp 34053963283959|Src <builtin>:1
    TruffleHotSpotCompilation-6793 LTruffleIR/Tier1;                                                      Math_floor                                    ()LTruffleIR/Tier1;                                 | 325583us  2084B bytecodes  1024B codesize
    ```
- Mandrel [discussion/PR](https://github.com/graalvm/mandrel-packaging/pull/369) about supporting Truffle in native-image with Mandrel.
- [Truffle Enterprise](https://mvnrepository.com/artifact/org.graalvm.truffle/truffle-enterprise/23.1.2) is available for download from Maven Central (`org.graalvm.truffle/truffle-enterprise`) under the GFTC but the sources artifact `truffle-enterprise-23.1.2-sources.jar` only contains a `LICENSE` file with the GFTC.

### GraalVM Compiler

- Run with `-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --upgrade-module-path <path-to-compiler.jar-module>` to enable native compilation of GraalJS/Truffle code with the jar-based, pure Java GraalVM compiler.
- [graal/compiler/docs/Debugging.md](https://github.com/oracle/graal/blob/master/compiler/docs/Debugging.md) documents the option `-XX:+JVMCIPrintProperties` which can be used to print the graal compiler related command line properties like e.g. `-Dgraal.PrintCompilation=true`. Notice that starting with JDK 22, the [Graal compiler options have been moved to the `jdk.graal` prefix](https://github.com/oracle/graal/commit/6f34cc046f3b2) (e.g. )`-Djdk.graal.PrintCompilation=true`
  - Notice that `-XX:+JVMCIPrintProperties` only works on a GraalVM JDK standalone. On a standard JDK you additionally need `-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --upgrade-module-path compiler-23.1.2.jar --module-path word-23.1.2.jar:truffle-compiler-23.1.2.jar:collections-23.1.2.jar` in order make the Graal Compiler available (activating it with `-XX:+UseJVMCICompiler` is not required).

#### Building the GraalVM compiler

Building the native version (i.e. "[libgraal](https://www.graalvm.org/latest/reference-manual/java/compiler/#compiler-operating-modes)" as opposed to "jargraal" or `libjvmcicompiler.so`) of the GraalVM compiler is very sensitive to the JDK, both at build time as well as at run time. By default, building `libjvmcicompiler.so` is only supported with the so called [labs-openjdk-17](https://github.com/graalvm/labs-openjdk-17) or [labs-openjdk-21](https://github.com/graalvm/labs-openjdk-21). The labs-openjdk versions are OpenJDK forks which include support for "libgraal" and GraalVM CE. The labs-openjdk changes are significant. E.g  [the diff](https://github.com/graalvm/labs-openjdk-17/compare/jdk-17.0.9+9...jvmci-23.0-b22) between labs-openjdk at tag `jvmci-23-0-b22` and OpenJDK 17 at tag `jdk-17.0.9+9` (which it is based on) are 237 commits in 174 changed files summing up to ~13.000 changed lines of code. For labs-openjdk-21 [the diff](https://github.com/graalvm/labs-openjdk-21/compare/jdk-21.0.2+13...jvmci-23.1-b33) between the tags `jvmci-23.1-b33` and `jdk-21.0.2+13` still consists of 75 commits to 48 files which result in a total of ~2.200 lines of code. The changes are mostly to the `jdk.internal.vm.ci` JVMCI module along with the corresponding HotSpot changes but also general class library changes like [[GR-39566] An option to emit stable names for lambda classes](https://github.com/graalvm/labs-openjdk-17/commit/58906fad1bf33a1f071d931ea9a81568a76fd82e) which are required by SubstrateVM.

So in order to build and use `libjvmcicompiler.so` we either have to choose the *non-standard* labs-openjdk or use the [Mandrel](https://github.com/graalvm/mandrel) fork of GraalVM. Mandrel is a downstream distribution of the GraalVM community edition targeted to provide a native-image release specifically to support [Quarkus](https://github.com/quarkusio/quarkus). In contrast to patching OpenJDK, Mandrel patches the GraalVM project in order to make it compatible with unmodified upstream OpenJDK distributions. E.g. the [diff](https://github.com/graalvm/mandrel/compare/vm-23.1.0...mandrel-23.1.0.0-Final) between `mandrel-23.1.0.0-Final` and its upstream `vm-23.1.0` is 8 commits to 18 files resulting in ~400 lines of changes. It has to be noticed though, that the Mandrel project is only officially supporting the GraalVM's native image functionality and *not* libgraal/`libjvmcicompiler.so`. The "[Building Mandrel/libgraal at tag mandrel-23.1.2.0-Final with JDK 17 doesn't work](https://github.com/graalvm/mandrel/issues/688)" issue in the Mandrel project contains more details on the compatibility between various Mandrel, GraalVM and OpenJDK versions.

##### Building libgraal with Mandrel and OpenJDK

While Mandrel is compatible with upstream OpenJDK, every new OpenJDK update release can introduce changes which require fixes to Mandrel (e.g. the [downport](https://github.com/openjdk/jdk17u/commit/a06047acce82f60b5ca193a7b2aa329ed24b46f4) of "[JDK-8168469: Memory leak in JceSecurity](https://bugs.openjdk.org/browse/JDK-8168469)" to JDK 17.0.10 caused a build failure in Mandrel which had to be fixed with "[[23.0] Mandrel 23.0 fails to build with JDK 17.0.10-EA](https://github.com/graalvm/mandrel/issues/607)").

Mandrel as well as GraalVM requires a build JDK with static versions of the native libraries because they will be linked statically into the native image produced by the native image builder. since JDK 11, these static libraries can be created as follows:

```bash
$ git clone https://github.com/openjdk/jdk17u-dev
$ cd jdk17u-dev
$ git checkout jdk-17.0.10-ga
$ configure ...
$ make graal-builder-image
```
Afterwards we export the newly generated JDK with static libraries as build JDK for the Mandrel build and make the `mx` tool available on the `PATH`:
```bash
$ export JAVA_HOME=<path-to>/jdk17u-dev-opt/images/graal-builder-jdk
$ export PATH=<path-to>/Graal/mx:$PATH
```
Finally, building "libgraal" is a matter of:
```bash
$ mkdir Mandrel && cd Mandrel
$ git clone https://github.com/graalvm/mandrel
$ cd mandrel
$ git checkout mandrel-23.0.3.0-Final
$ cd vm
$ mx --env libgraal build
...
[libjvmcicompiler:180684] Finished generating 'libjvmcicompiler' in 34.6s.
```
and for `mandrel-23.1.2.0-Final` with OpenJDK 21:
```bash
$ export JAVA_HOME=<path-to>/jdk21u-dev-opt/images/graal-builder-jdk
$ git checkout mandrel-23.1.2.0-Final
$ mx --env libgraal build
...
```
Notice that building `mandrel-23.1.2.0-Final` with OpenJDK 17 is possible with a patch ([fix_mandrel-23.1.2.0-Final_on_jdk17.patch](./data/fix_mandrel-23.1.2.0-Final_on_jdk17.patch)):
```bash
$ cd ..
$ patch -p1 < fix_mandrel-23.1.2.0-Final_on_jdk17.patch
$ cd vm
$ export JAVA_HOME=<path-to>/jdk17u-dev-opt/images/graal-builder-jdk
$ mx --env libgraal build
...
```
But the resulting `libjvmcicompiler.so` will not work as native compiler with JDK 17:
```bash
$ <path-to>/jdk17u-dev-opt/images/jdk/bin/java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
                 -XX:+UseJVMCINativeLibrary -XX:JVMCILibPath=<path-to>/libjvmcicompiler.so.image \
                 ...

Your Java runtime '17.0.10+7-LTS' with compiler version '23.1.2' is incompatible with polyglot version '23.1.2'.
The Java runtime version must be greater or equal to JDK '21' and smaller than JDK '25'.
Update your Java runtime to resolve this.
```
