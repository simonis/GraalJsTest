### GraalVM

- [Isolates and Compressed References: More Flexible and Efficient Memory Management via GraalVM](https://medium.com/graalvm/isolates-and-compressed-references-more-flexible-and-efficient-memory-management-for-graalvm-a044cc50b67e) by Christian Wimmer, Jan 25, 2019
- [Polyglot Isolates](https://www.graalvm.org/latest/reference-manual/embed-languages/#polyglot-isolates). On Oracle GraalVM, a Polyglot engine can be configured to run in a dedicated Native Image isolate (since version 23.1). A polyglot engine in this mode executes within a VM-level fault domain with a dedicated garbage collector and JIT compiler. Polyglot isolates are useful for polyglot sandboxing. Running languages in an isolate works with HotSpot and Native Image host virtual machines. Languages used as polyglot isolates can be downloaded from Maven Central using the `-isolate` suffix.
  - What's the difference between [`org.graalvm.js`](https://mvnrepository.com/artifact/org.graalvm.js) and [`org.graalvm.polyglot`](https://mvnrepository.com/artifact/org.graalvm.polyglot) on Maven Central?
  - e.g. [`org.graalvm.polyglot » js-isolate`](https://mvnrepository.com/artifact/org.graalvm.polyglot/js-isolate) is just a POM with a dependency on [`org.graalvm.js » js-isolate`](https://mvnrepository.com/artifact/org.graalvm.js/js-isolate) and [`org.graalvm.truffle » truffle-enterprise`](https://mvnrepository.com/artifact/org.graalvm.truffle/truffle-enterprise).
  - `js-isolate-23.1.2-sources.jar` only contains the sources of `PolyglotIsolateResource.java` (and `PolyglotIsolateResourceProvider.java` which seems to be generated from the first one).
  - `js-isolate-23.1.2.jar` contains a native version of the `org.graalvm.js.isolate` module for several platforms (e.g. `linux/amd64/libvm/libjsvm.so` (~200mb), `windows/amd64/libvm/jsvm.dll`, ..).
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
- Oracle has created the [graalvm-for-jdk21-community-backports](https://github.com/graalvm/graalvm-for-jdk21-community-backports) and [graalvm-for-jdk17-community-backports](https://github.com/graalvm/graalvm-for-jdk17-community-backports) repositories which are basically mirrors of the corresponding [release/graal-vm/23.1](https://github.com/oracle/graal/tree/release/graal-vm/23.1) and [release/graal-vm/23.0](https://github.com/oracle/graal/tree/release/graal-vm/23.0) branches in the main [https://github.com/oracle/graal](https://github.com/oracle/graal) repository. The new backports repositories/branches have accumulated quite some changes since the last official [GraalVM for JDK 21 Community 21.0.2](https://github.com/graalvm/graalvm-ce-builds/releases/tag/jdk-21.0.2) and [ GraalVM for JDK 17 Community 17.0.9
](https://github.com/graalvm/graalvm-ce-builds/releases/tag/jdk-17.0.9) releases (at the specific tags [graal-23.1.2](https://github.com/oracle/graal/tree/graal-23.1.2) and [graal-23.0.3](https://github.com/oracle/graal/tree/graal-23.0.3)), which according to the [GraalVM Community Edition Release Calendar](https://www.graalvm.org/release-calendar/) should have been the last GraalVM Community releases for JDK 17/21 supported by Oracle. So why is Oracle then still maintaining these branches?

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

Building GraalJS (version `23.1.x`) from source:
```bash
$ mkdir Graal
$ cd Graal
$ git clone https://github.com/oracle/graal.git
$ cd graal/
$ git checkout release/graal-vm/23.1
$ cd ..
$ git clone https://github.com/oracle/graaljs
$ cd graaljs/
$ git checkout release/graal-vm/23.1
$ grep mx_version common.json
  "mx_version": "6.46.1",
# don't do 'mx sforceimports' as advised in https://github.com/oracle/graaljs/blob/master/docs/Building.md because it will mess up your `../../graal` repo to a *strange* revision (defined in the 'imports.suites.regex' section of 'mx.graal-js/suite.py')!
$ cd ..
$ git clone https://github.com/graalvm/mx.git
$ export PATH=$PWD/mx:$PATH
$ cd mx/
$ git checkout 6.46.1
$ cd ../graaljs/graal-js/
$ export JAVA_HOME=/Java/corretto-17
$ MX_ALT_OUTPUT_ROOT=/tmp/graaljs-23.1 mx build --targets \
  GRAALJS,GRAALJS_SCRIPTENGINE,TRUFFLE_JS_TESTS,GRAALJS_SCRIPTENGINE_TESTS
$ MX_ALT_OUTPUT_ROOT=/tmp/graaljs-23.1 mx gate -t UnitTests
```
This will build GraalJS along with its dependencies to `MX_ALT_OUTPUT_ROOT` without touching the source directory. The build artifacts can be found in the various `./*/dist/` subdirectories of `MX_ALT_OUTPUT_ROOT`:
<details>
  <summary>GraalJS build artifacts</summary>

```bash
$ ls -1 /tmp/graaljs-23.1/*/dists/*.jar
/tmp/graaljs-23.1/graal-js/dists/graaljs.jar
/tmp/graaljs-23.1/graal-js/dists/graaljs-scriptengine.jar
/tmp/graaljs-23.1/graal-js/dists/graaljs-scriptengine-tests.jar
/tmp/graaljs-23.1/graal-js/dists/truffle-js-factory-processor.jar
/tmp/graaljs-23.1/graal-js/dists/truffle-js-snapshot-tool.jar
/tmp/graaljs-23.1/graal-js/dists/truffle-js-tests.jar
/tmp/graaljs-23.1/mx/dists/junit-tool.jar
/tmp/graaljs-23.1/regex/dists/tregex.jar
/tmp/graaljs-23.1/sdk/dists/collections.jar
/tmp/graaljs-23.1/sdk/dists/nativebridge-processor.jar
/tmp/graaljs-23.1/sdk/dists/nativeimage.jar
/tmp/graaljs-23.1/sdk/dists/polyglot.jar
/tmp/graaljs-23.1/sdk/dists/polyglot-processor.jar
/tmp/graaljs-23.1/sdk/dists/word.jar
/tmp/graaljs-23.1/truffle/dists/truffle-api.jar
/tmp/graaljs-23.1/truffle/dists/truffle-dsl-processor.jar
/tmp/graaljs-23.1/truffle/dists/truffle-icu4j.jar
```
</details>

##### Building GraalJS together with the Graal Compiler

You can also build GraalJS together with the Graal compiler (i.e. `jargraal`) and the Graal tools (which contain the builtin Truffle profiler) by dynamically importing the compiler and tools suites:

```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/graaljs-master mx \
  --dynamicimports /compiler --dynamicimports /tools \
  build --targets GRAALJS,GRAALJS_SCRIPTENGINE,TRUFFLE_RUNTIME,TRUFFLE_COMPILER,POLYGLOT,GRAAL,TRUFFLE_PROFILER
```

In that case the output directory will look as follows:

<details>
  <summary>GraalJS and Graal Compiler build artifacts</summary>

```bash
$ ls -1 /tmp/graaljs-master/*/dists/*.jar
/tmp/graaljs-master/compiler/dists/graal.jar
/tmp/graaljs-master/compiler/dists/graal-processor.jar
/tmp/graaljs-master/graal-js/dists/graaljs.jar
/tmp/graaljs-master/graal-js/dists/graaljs-scriptengine.jar
/tmp/graaljs-master/graal-js/dists/truffle-js-factory-processor.jar
/tmp/graaljs-master/regex/dists/tregex.jar
/tmp/graaljs-master/sdk/dists/collections.jar
/tmp/graaljs-master/sdk/dists/jniutils.jar
/tmp/graaljs-master/sdk/dists/nativeimage.jar
/tmp/graaljs-master/sdk/dists/polyglot.jar
/tmp/graaljs-master/sdk/dists/word.jar
/tmp/graaljs-master/tools/dists/truffle-profiler.jar
/tmp/graaljs-master/truffle/dists/truffle-api.jar
/tmp/graaljs-master/truffle/dists/truffle-compiler.jar
/tmp/graaljs-master/truffle/dists/truffle-dsl-processor.jar
/tmp/graaljs-master/truffle/dists/truffle-icu4j.jar
/tmp/graaljs-master/truffle/dists/truffle-json.jar
/tmp/graaljs-master/truffle/dists/truffle-libgraal-processor.jar
/tmp/graaljs-master/truffle/dists/truffle-runtime.jar
/tmp/graaljs-master/truffle/dists/truffle-xz.jar
```
</details>

You can now execute an applications which uses GraalJS with Graal Compiler support with a default upstream version of OpenJDK by adding the following options to your command line:

```bash
-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
--module-path \
$MX_ALT_OUTPUT_ROOT/truffle/dists/truffle-api.jar:\
$MX_ALT_OUTPUT_ROOT/truffle/dists/truffle-runtime.jar:\
$MX_ALT_OUTPUT_ROOT/truffle/dists/truffle-compiler.jar:\
$MX_ALT_OUTPUT_ROOT/truffle/dists/truffle-dsl-processor.jar:\
$MX_ALT_OUTPUT_ROOT/truffle/dists/truffle-icu4j.jar:\
$MX_ALT_OUTPUT_ROOT/regex/dists/tregex.jar:\
$MX_ALT_OUTPUT_ROOT/graal-js/dists/graaljs.jar:\
$MX_ALT_OUTPUT_ROOT/sdk/dists/jniutils.jar:\
$MX_ALT_OUTPUT_ROOT/sdk/dists/word.jar:\
$MX_ALT_OUTPUT_ROOT/sdk/dists/polyglot.jar:\
$MX_ALT_OUTPUT_ROOT/sdk/dists/collections.jar:\
$MX_ALT_OUTPUT_ROOT/sdk/dists/nativeimage.jar:\
$MX_ALT_OUTPUT_ROOT/tools/dists/truffle-profiler.jar:\
$MX_ALT_OUTPUT_ROOT/truffle/dists/truffle-json.jar \
--add-modules org.graalvm.polyglot \
--upgrade-module-path \
$MX_ALT_OUTPUT_ROOT/compiler/dists
```

To build GraalJS you have to check out the Graal repository at the same directory level like the GraalJS repository (as showed in the build instructions above). If you don't do this, `mx build` will automatically clone the Graal repository at the commit specified in the `imports` section of `./graal-js/mx.graal-js/suite.py`.

The file `common.json` in the `graaljs` repository contains the version of `mx` that should be used for building GraalJS. It should be the same like `mx_version` in the `./graal` repository if both repositories are synced to the same tag or branch (e.g. `release/graal-vm/23.1` in this example).

Notice that although GraalVM `23.1` is targeted for JDK 21, the pure Java artifacts built from the tag `release/graal-vm/23.1` of these libraries can be build (and the results can be used) with OpenJDK 17 and later.

The [release/graal-vm/23.1](https://github.com/oracle/graal/tree/release/graal-vm/23.1) branch for GraalVM `23.1` for JDK 21 isn't supported by Oracle any more (at least not publicly). It has been moved to the master branch of the new [https://github.com/graalvm/graalvm-community-jdk21u](https://github.com/graalvm/graalvm-community-jdk21u) repository which is now maintained by the community. ~~The same is true for the [release/graal-vm/23.1](https://github.com/oracle/graaljs/tree/release/graal-vm/23.1) branch of the GraalJS repository. Until now there's no corresponding community repository for GraalJS 23.1 [but discussions to create one are underway](https://graalvm.slack.com/archives/CNBFR78F9/p1725034816736779)~~. At the [GraalVM Community Summit 2024](https://www.graalvm.org/community/meetup/) Oracle has agreed to continue to maintain GraalJS 23.1 in the [release/graal-vm/23.1](https://github.com/oracle/graaljs/tree/release/graal-vm/23.1) branch of the GraalJS repository, so currently a comunity repository for GraalJS is not required.

#### Building the Truffle Profiler

The [Truffle Profiler](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Profiling/) is part of the tools sub-project contained in the `./tools/ sub-directory. It can be build as follows:

```bash
$ cd ./tools
$ MX_ALT_OUTPUT_ROOT=/tmp/tools-unittests mx \
  build --targets TRUFFLE_PROFILER,TRUFFLE_RUNTIME,TRUFFLE_PROFILER_TEST
```
The `TRUFFLE_RUNTIME` and `TRUFFLE_PROFILER_TEST` targets are only required if you also plan to run some of the unit tests. The Truffle profiler can then be found under `$MX_ALT_OUTPUT_ROOT/tools/dists/truffle-profiler.jar`.

Afterwards, the profiler unit tests can be run as follows:
```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/tools-unittests mx \
  unittest --verbose ProfilerCLITest CPUSamplerTest
```
This will execute the [`ProfilerCLITest`](https://github.com/oracle/graal/blob/261c38a7a47dd87f13ec3cbe0fbb85cfdff17963/tools/src/com.oracle.truffle.tools.profiler.test/src/com/oracle/truffle/tools/profiler/test/ProfilerCLITest.java) and [`CPUSamplerTest`](https://github.com/oracle/graal/blob/261c38a7a47dd87f13ec3cbe0fbb85cfdff17963/tools/src/com.oracle.truffle.tools.profiler.test/src/com/oracle/truffle/tools/profiler/test/CPUSamplerTest.java) tests. Notice that the JDK for executing the tests will betaken from the `JAVA_HOME` environment variable and if that is a vanilla JDK, JVMCI will be disabled and the tests will be executed with the [`DefaultTruffleRuntime`](https://github.com/oracle/graal/blob/261c38a7a47dd87f13ec3cbe0fbb85cfdff17963/truffle/src/com.oracle.truffle.api/src/com/oracle/truffle/api/impl/DefaultTruffleRuntime.java), i.e. without Graal Compiler optimizations.

Some tests like for example [`CPUSamplerTest::testTiers()`](https://github.com/oracle/graal/blob/261c38a7a47dd87f13ec3cbe0fbb85cfdff17963/tools/src/com.oracle.truffle.tools.profiler.test/src/com/oracle/truffle/tools/profiler/test/CPUSamplerTest.java#L338C17-L338C26) specifically test for the optimizing [`HotSpotTruffleRuntime`](https://github.com/oracle/graal/blob/261c38a7a47dd87f13ec3cbe0fbb85cfdff17963/truffle/src/com.oracle.truffle.runtime/src/com/oracle/truffle/runtime/hotspot/HotSpotTruffleRuntime.java) and only run if it is available:

```java
public void testTiers() {
    Assume.assumeFalse(Truffle.getRuntime().getClass().toString().contains("Default"));
```
Others tests like for example [`ProfilerCLITest::testDefaultSampleHistogram()`](https://github.com/oracle/graal/blob/261c38a7a47dd87f13ec3cbe0fbb85cfdff17963/tools/src/com.oracle.truffle.tools.profiler.test/src/com/oracle/truffle/tools/profiler/test/ProfilerCLITest.java#L70) only run with the non-optimizing, `DefaultTruffleRuntime`.

In order to exercise the tests which require an optimizing runtime on a default JDK, the unit tests can be run as follows:
```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/tools-unittests mx \
  unittest --verbose \
    -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
    --upgrade-module-path $MX_COMPILER_OUTPUT_ROOT/compiler/dists \
  ProfilerCLITest CPUSamplerTest
```
where `$MX_COMPILER_OUTPUT_ROOT` denotes the output directory of a build containing the Graal Compiler as described for example in the section "[Building GraalJS together with the Graal Compiler
](#building-graaljs-together-with-the-graal-compiler)".

#### IDE support

`mx` can be used to [create IDE configurations](https://github.com/graalvm/mx/blob/master/docs/IDE.md) for IntelliJ (`mx intellijinit`), Eclipse (`mx eclipseinit`) and Netbeans (`mx netbeansinit`). The nice thing about these configurations is that they automatically include the project's dependencies (i.e. the "[suite](https://github.com/graalvm/mx/tree/master?tab=readme-ov-file#suites)" dependencies in `mx` terms). E.g. running `mx suites` in the `graaljs/graal-js/` directory will print:

```bash
$ mx suites | egrep '^[^ ].*'
graal-js
regex
truffle
sdk
```

Notice, how this includes the `regex`, `truffle` and `sdk` suites from the main Graal repository which are dependencies of `graal-js`. In order for this to work out of the box, the Graal repository has to be checked out in a sibling directory of the GraalJS repository like so:
```
├── Graal
│   ├── graal
│   │   ├── compiler
│   │   ├── regex
│   │   ├── truffle
│   │   ├── sdk
│   │   ...
│   ├── graaljs
│   │   ├── graal-js
│   │   ├── graal-nodejs
│   │   ...
```

The Graal compiler is not a static dependency of GraalJS, but it is possible to [dynamically import](https://github.com/graalvm/mx/blob/master/docs/dynamic-imports.md) the Compiler suite as well in order to create a project which includes the sources of GraalJS together with the ones of the Truffle framework and the Graal Compiler in a single IDE project. So to cut a long story short, an IntelliJ project with the joined GraalJS, Truffle, the Compiler and the Tools sources (including the Truffle built-in profiler) can be generated from the `graaljs/graal-js/` directory with:
```bash
$ mx --dynamicimports /compiler --dynamicimports /tools --dynamicimports /substratevm intellijinit
```

See Foivos Zakkak's [Getting started with GraalVM development](https://foivos.zakkak.net/tutorials/getting_started_with_graalvm_development/)

### GraalVM Truffle

- [Graal Truffle tutorial](https://www.endoflineblog.com/graal-truffle-tutorial-part-0-what-is-truffle) by Adam Ruka
- [Seminar: Dynamic Metacompilation with Truffle](https://www.youtube.com/watch?v=pksRrON5XfU) with Christian Humer
- [Truffle Language Implementation Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/) or on GitHub: https://github.com/oracle/graal/tree/master/truffle/docs
- [Truffle Unchained — Portable Language Runtimes as Java Libraries](https://medium.com/graalvm/truffle-unchained-13887b77b62c). Also see [GR-43819: Split Graal-SDK into new modules: polyglot, word, collections and nativeimage](https://github.com/oracle/graal/pull/7171) and the [Changelog entry for Graal Version 23.1.0](https://github.com/oracle/graal/blob/master/sdk/CHANGELOG.md#version-2310).
  - ([GR-47917](https://github.com/oracle/graal/pull/7239)) Added class-path isolation if polyglot is used from the class-path. At class initialization time and if polyglot is used from the class-path then the polyglot implementation spawns a module class loader with the polyglot runtime and language implementations. This allows to use an optimized runtime even if languages and polyglot are used from the class-path. Note that for best performance, it is recommended to load polyglot and the languages from the module-path. Comment from [org.graalvm.polyglot.Engine$ClassPathIsolation](https://github.com/oracle/graal/blob/774141206b82771ab80e0ea38d26d660292eb8ab/sdk/src/org.graalvm.polyglot/src/org/graalvm/polyglot/Engine.java#L1768): "*If Truffle is on the class-path (or a language), we do not want to expose these classes to embedders (users of the polyglot API). Unless disabled, we load all Truffle jars on the class-path in a special module layer instead of loading it through the class-path in the unnamed module*". This feature is controlled by `-Dpolyglotimpl.DisableClassPathIsolation` which defaults to `true` and observed by `-Dpolyglotimpl.TraceClassPathIsolation=true`.
  - ToDo: do we want to GraalJS & Truffle on the class or on the module path? The Graal Compiler has to be placed on the `--upgrade-module-path`. Also, if we put GraalJS & Truffle on the module path but our application classes are running form the class path we have to explicitly do `--add-modules org.graalvm.polyglot`.
- [ Truffle/Engine Options ](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/)
  To get them dynamically, you can do:
  ```java
  $ jshell -J--module-path -Jtarget/js-deps -J--add-modules -Jorg.graalvm.polyglot -J-XX:+UnlockExperimentalVMOptions -J-XX:+EnableJVMCI -J--upgrade-module-path -Jtarget/compiler-deps --execution local
  jshell> /env -module-path target/js-deps
  jshell> /env -add-modules org.graalvm.polyglot
  jshell> org.graalvm.polyglot.Engine.newBuilder().build().getOptions().forEach(ok -> System.out.println(ok.getName() + ": " + ok.getHelp()))
  ...
  engine.BackgroundCompilation: Enable asynchronous truffle compilation in background threads (default: true)
  engine.Compilation: Enable or disable Truffle compilation.
  ...
  ```
- [Optimizing Truffle Interpreters](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing)
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
  - Also see the latest version of [truffle/docs/Optimizing.md](https://github.com/oracle/graal/blob/master/truffle/docs/Optimizing.md) and [truffle/docs/Options.md](https://github.com/oracle/graal/blob/master/truffle/docs/Options.md) in the Graal GitHub repository.
- Mandrel [discussion/PR](https://github.com/graalvm/mandrel-packaging/pull/369) about supporting Truffle in native-image with Mandrel.
- [Truffle Enterprise](https://mvnrepository.com/artifact/org.graalvm.truffle/truffle-enterprise/23.1.2) is available for download from Maven Central (`org.graalvm.truffle/truffle-enterprise`) under the GFTC but the sources artifact `truffle-enterprise-23.1.2-sources.jar` only contains a `LICENSE` file with the GFTC.
- [TruffleRuby](https://chrisseaton.com/truffleruby/) by Chris Seaton
- [Graal Truffle tutorial in 13 parts](https://www.endoflineblog.com/graal-truffle-tutorial-part-0-what-is-truffle) by Adam Rubka
- [Embedding Truffle Languages](https://nirvdrum.com/2022/05/09/truffle-language-embedding.html) by Kevin Menard
- [Writing Truly Memory Safe JIT Compilers](https://medium.com/graalvm/writing-truly-memory-safe-jit-compilers-f79ad44558dd) by Mike Hearn

### GraalVM Compiler

- Run with `-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --upgrade-module-path <path-to-compiler.jar-module>` to enable native compilation of GraalJS/Truffle code with the jar-based, pure Java GraalVM compiler.
- [graal/compiler/docs/Debugging.md](https://github.com/oracle/graal/blob/master/compiler/docs/Debugging.md) documents the option `-XX:+JVMCIPrintProperties` which can be used to print the graal compiler related command line properties like e.g. `-Dgraal.PrintCompilation=true`. Notice that starting with JDK 22, the [Graal compiler options have been moved to the `jdk.graal` prefix](https://github.com/oracle/graal/commit/6f34cc046f3b2) (e.g. )`-Djdk.graal.PrintCompilation=true`
  - Notice that `-XX:+JVMCIPrintProperties` only works on a GraalVM JDK standalone. On a standard JDK you additionally need `-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --upgrade-module-path compiler-23.1.2.jar --module-path word-23.1.2.jar:truffle-compiler-23.1.2.jar:collections-23.1.2.jar` in order make the Graal Compiler available (activating it with `-XX:+UseJVMCICompiler` is not required).
- [Understanding How Graal Works - a Java JIT Compiler Written in Java](https://chrisseaton.com/truffleruby/jokerconf17/) by Chris Seaton

#### Building the GraalVM compiler

Building the native version (i.e. "[libgraal](https://www.graalvm.org/latest/reference-manual/java/compiler/#compiler-operating-modes)" as opposed to "jargraal" or `libjvmcicompiler.so`) of the GraalVM compiler is very sensitive to the JDK, both at build time as well as at run time. By default, building `libjvmcicompiler.so` is only supported with the so called [labs-openjdk-17](https://github.com/graalvm/labs-openjdk-17) or [labs-openjdk-21](https://github.com/graalvm/labs-openjdk-21). The labs-openjdk versions are OpenJDK forks which include support for "libgraal" and GraalVM CE. The labs-openjdk changes are significant. E.g  [the diff](https://github.com/graalvm/labs-openjdk-17/compare/jdk-17.0.9+9...jvmci-23.0-b22) between labs-openjdk at tag `jvmci-23-0-b22` and OpenJDK 17 at tag `jdk-17.0.9+9` (which it is based on) are 237 commits in 174 changed files summing up to ~13.000 changed lines of code. For labs-openjdk-21 [the diff](https://github.com/graalvm/labs-openjdk-21/compare/jdk-21.0.2+13...jvmci-23.1-b33) between the tags `jvmci-23.1-b33` and `jdk-21.0.2+13` still consists of 75 commits to 48 files which result in a total of ~2.200 lines of code. The changes are mostly to the `jdk.internal.vm.ci` JVMCI module along with the corresponding HotSpot changes but also general class library changes like [[GR-39566] An option to emit stable names for lambda classes](https://github.com/graalvm/labs-openjdk-17/commit/58906fad1bf33a1f071d931ea9a81568a76fd82e) which are required by SubstrateVM.

So in order to build and use `libjvmcicompiler.so` we either have to choose the *non-standard* labs-openjdk or use the [Mandrel](https://github.com/graalvm/mandrel) fork of GraalVM. Mandrel is a downstream distribution of the GraalVM community edition targeted to provide a native-image release specifically to support [Quarkus](https://github.com/quarkusio/quarkus). In contrast to patching OpenJDK, Mandrel patches the GraalVM project in order to make it compatible with unmodified upstream OpenJDK distributions. E.g. the [diff](https://github.com/graalvm/mandrel/compare/vm-23.1.0...mandrel-23.1.0.0-Final) between `mandrel-23.1.0.0-Final` and its upstream `vm-23.1.0` is 8 commits to 18 files resulting in ~400 lines of changes. It has to be noticed though, that the Mandrel project is only officially supporting the GraalVM's native image functionality and *not* libgraal (i.e. `libjvmcicompiler.so`). The "[Building Mandrel/libgraal at tag mandrel-23.1.2.0-Final with JDK 17 doesn't work](https://github.com/graalvm/mandrel/issues/688)" issue in the Mandrel project contains more details on the compatibility between various Mandrel, GraalVM and OpenJDK versions.

**Note**: Since the creation of the [graalvm-community-jdk21u](https://github.com/graalvm/graalvm-community-jdk21u) repository (which was forked from the now stale [release/graal-vm/23.1](https://github.com/oracle/graal/tree/release/graal-vm/23.1) branch of the upstream [Graal repository](https://github.com/oracle/graal)) for a community supported LTS version of Graal 23.1 for OpenJDK 21 it is possible to build the GraalVM compiler from [graalvm-community-jdk21u](https://github.com/graalvm/graalvm-community-jdk21u) repository repository with an unmodified OpenJDK 21u. The [graalvm-community-jdk21u](https://github.com/graalvm/graalvm-community-jdk21u)
repository already contains all the relevant GraalVM changes from the corresponding Mandrel repository and the community keeps it buildable with the latest updates of OpenJDK 21. In fact, [graalvm-community-jdk21u](https://github.com/graalvm/graalvm-community-jdk21u) has now become the new upstream for the [`mandrel/23.1`](https://github.com/graalvm/mandrel/tree/mandrel/23.1) branch of the [Mandrel](https://github.com/graalvm/mandrel) repository.

##### Building libgraal from graalvm-community-jdk21u

GraalVM requires a build JDK with static versions of the native libraries because they will be linked statically into the native image produced by the native image builder. Since JDK 11, these static libraries can be created as follows:

```bash
$ git clone https://github.com/openjdk/jdk21u-dev
$ cd jdk21u-dev
$ git checkout jdk-21.0.4-ga
$ configure ...
$ make graal-builder-image
$ export JAVA_HOME=<path-to>/jdk21u-dev/images/graal-builder-jdk
```
We also need the correct version of `mx` on the `PATH` (see [Building GraalJS](#building-graaljs)).
```
$ export PATH=<path-to>/Graal/mx:$PATH
```
Finally we can clone the community version of Graal 23.1 and build libgraal:
```bash
$ git clone https://github.com/graalvm/graalvm-community-jdk21u
$ cd graalvm-community-jdk21u/vm
$ MX_ALT_OUTPUT_ROOT=/tmp/libgraal-23.1 \
  mx --env libgraal \
     build --dependencies libjvmcicompiler.so.image
```

`libjvmcicompiler.so` can be found under `$MX_ALT_OUTPUT_ROOT/sdk/linux-amd64/libjvmcicompiler.so.image/`

##### Building libgraal from the upstream Graal repository

As of December 2024, it is possible to build the latest version of libgraal with the latest, unmodified version of OpenJDK (at the time of writing a pre-release of JDK 24) from the `graal/vm/` directory:
```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/libgraal-master \
  mx --dynamicimports /substratevm \
     --components=lg \
     --native-images=lib:jvmcicompiler \
     --disable-installables=true \
     build --targets libjvmcicompiler.so.image
```
The `--dynamicimports`, `--components`, `--native-images` and `--disable-installables` are an alternative for using `--env libgraal` as described above. The latter just takes the corresponding options from `graal/vm/mx.vm/libgraal`. I still don't know where I can find the target `libjvmcicompiler.so.image` because neither `mx suites` nor `mx graalvm-show` displays it? But without `--targets libjvmcicompiler.so.image` the build takes longer and produces conisderable more output in `MX_ALT_OUTPUT_ROOT`.

##### Building libgraal with Mandrel and OpenJDK

While Mandrel is compatible with upstream OpenJDK, every new OpenJDK update release can introduce changes which require fixes to Mandrel (e.g. the [downport](https://github.com/openjdk/jdk17u/commit/a06047acce82f60b5ca193a7b2aa329ed24b46f4) of "[JDK-8168469: Memory leak in JceSecurity](https://bugs.openjdk.org/browse/JDK-8168469)" to JDK 17.0.10 caused a build failure in Mandrel which had to be fixed with "[[23.0] Mandrel 23.0 fails to build with JDK 17.0.10-EA](https://github.com/graalvm/mandrel/issues/607)").

Mandrel as well as GraalVM requires a build JDK with static versions of the native libraries because they will be linked statically into the native image produced by the native image builder. Since JDK 11, these static libraries can be created as follows:

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

By default `mx` places the build artifacts into the `mxbuild/` subdirectory, but not only in the current directory but also in sibling directories /e.g. `../sdk/mxbuild/`, `../compiler/mxbuild` etc. which can be a little confusing. If you want to keep the source directory clean, you can use the `MX_ALT_OUTPUT_ROOT` environment variable to specify an alternative output directory for all build artifacts.

### GraalVM Native Image

#### Building GraalVM Native Image

As of December 2024, it is possible to build the latest version of the Graal Native Image tool with the latest, unmodified version of OpenJDK (at the time of writing a pre-release of JDK 24). Because SubstrateVM/Native is still tightly coupled to the JDK and its JVMCI interface, the most promising approach is to look for the latest `jdk-<major>+<build>` tags in the Graal repository (e.g. `jdk-25+9`) and build OpenJDK on that very tag. Usually, the latest upstream OpenJDK build is imported every two weeks by the Graal team.

After setting `JAVA_HOME` and putting `mx` in the path, we can then do the following from the top-level Graal directory:

```
$ MX_ALT_OUTPUT_ROOT=/tmp/native-image-master \
  mx --primary-suite=substratevm graalvm-show
```
This will print quite some information:
```
GraalVM distribution: GRAALVM_5A90E9CFDC_JAVA25
Version: 25.0.0-dev
Config name: None
Components:
 ...
 - Native Image ('ni', /svm, experimental)
 ...
Launchers:
 - native-image (bash, rebuildable)
 ...
Libraries:
 - libjvmcicompiler.so (skipped, rebuildable)
 - libnative-image-agent.so (skipped, rebuildable)
 ...
No standalone
```

To build a minial Native Image distribution, we only need the `Native Image` component along with its dependencies:
```
$ MX_ALT_OUTPUT_ROOT=/tmp/native-image-master \
  mx --primary-suite=substratevm \
  --components=ni graalvm-show
```
The output now contains much fewer components, so we can list them all:
```
GraalVM distribution: GRAALVM_50BA5489A0_JAVA25
Version: 25.0.0-dev
Config name: None
Components:
 - Graal SDK Compiler ('sdkc', /graalvm, experimental)
 - Graal SDK Native Image ('sdkni', /graalvm, experimental)
 - GraalVM compiler ('cmp', /graal, experimental)
 - Native Image ('ni', /svm, experimental)
 - Native Image licence files ('nil', /svm, experimental)
 - SubstrateVM ('svm', /svm, experimental)
 - SubstrateVM Static Libraries ('svmsl', /False, experimental)
 - Truffle Compiler ('tflc', /truffle, experimental)
 - Truffle Runtime SVM ('svmt', /truffle, experimental)
Launchers:
 - native-image (bash, rebuildable)
Libraries:
 - libnative-image-agent.so (skipped, rebuildable)
 - libnative-image-diagnostics-agent.so (skipped, rebuildable)
No standalone
```

Notice how the name of the "GraalVM distribution" in the first line of the output has changed from `GRAALVM_5A90E9CFDC_JAVA25` before to `GRAALVM_50BA5489A0_JAVA25`. The strange number in the middle of the distribution name is actually a [SHA-1 hash of the various components which get included in the build](https://github.com/oracle/graal/blob/b49de582df9c69bd45e60755f074b38eaf1002da/sdk/mx.sdk/mx_sdk_vm_impl.py#L1031-L1033). We will see this name again in the directory name of the resulting Graal JDK. Actually, every configuration change, will trigger the creation of a new output directory under `$MX_ALT_OUTPUT_ROOT/sdk/linux-amd64/` which contains a full Graal JDK distribution with the corresponding configuration.

Also, for some unknown reasons, this will still skip the creation of the native images agents:
```
Libraries:
 - libnative-image-agent.so (skipped, rebuildable)
 - libnative-image-diagnostics-agent.so (skipped, rebuildable)

```

In order to include them into our build, we additionally need to add `--native-images=lib:native-image-agent,lib:native-image-diagnostics-agent` to our build command:
```
$ MX_ALT_OUTPUT_ROOT=/tmp/native-image-master \
  mx --primary-suite=substratevm \
  --native-images=lib:native-image-agent,lib:native-image-diagnostics-agent \
  --components=ni graalvm-show
GraalVM distribution: GRAALVM_CCE6114D71_JAVA25
...
Libraries:
 - libnative-image-agent.so (native, rebuildable)
 - libnative-image-diagnostics-agent.so (native, rebuildable)
```
Notice how the distribution name changed again to `GRAALVM_CCE6114D71_JAVA25` now, because we've additionally included the agent libraries.

`graalvm-show` can also be used with optional command line parameters to get even more information on what will be built:
```
usage: mx graalvm-show [-h] [--stage1] [--print-env] [-v]

Print the GraalVM config

optional arguments:
  -h, --help     show this help message and exit
  --stage1       show the components for stage1
  --print-env    print the contents of an env file that reproduces the current GraalVM config
  -v, --verbose  print additional information about installables and standalones
```
Using just `graalvm` as command will trigger an error which reveals other interesting commands:
```
mx: command 'graalvm' is ambiguous
    graalvm-components graalvm-dist-name graalvm-version graalvm-home graalvm-type graalvm-enter graalvm-show graalvm-vm-name
```

Now that we know a bit more about the Native Image build process, we can build a Native Image JDK with:
```
$ MX_ALT_OUTPUT_ROOT=/tmp/native-image-master \
  mx --primary-suite=substratevm \
  --native-images=lib:native-image-agent,lib:native-image-diagnostics-agent \
  --components=ni build
```
The resulting Native Image enabled JDK can be found under `sdk/latest_graalvm_home` which is a symlink to `$MX_ALT_OUTPUT_ROOT/sdk/linux-amd64/GRAALVM_50BA5489A0_JAVA25/graalvm-50ba5489a0-java25-25.0.0-dev`. Notice the previously mentioned hash code in the directory names. This JDK has the `native-image` tool in its `bin/` director and the `libnative-image-agent.so` at `lib/libnative-image-agent.so`.

#### References
- [GraalVM Native Image Quick Reference v1](https://medium.com/graalvm/graalvm-native-image-quick-reference-4ceb84560fd8) and [GraalVM Native Image Quick Reference v2](https://medium.com/graalvm/native-image-quick-reference-v2-332cf453d1bc) by Olga Gupalo
- [Memory Management at Native Image Run Time](https://docs.oracle.com/en/graalvm/enterprise/20/docs/reference-manual/native-image/MemoryManagement)
- [Package `org.graalvm.nativeimage.c`](https://www.graalvm.org/sdk/javadoc/org/graalvm/nativeimage/c/package-summary.html): This package and its sub-packages provide a fast and lightweight interface between Java code and C code.
- [The many ways of polyglot programming with GraalVM](https://medium.com/graalvm/3-ways-to-polyglot-with-graalvm-fb28c1542b45) by Michael Simons. Example on how to build a shared library from a Java program with GraalVM and access it from C using the Native Image C API.
- [ABOUT THE TOOLING AVAILABLE TO CREATE NATIVE GRAALVM IMAGES.
](https://info.michael-simons.eu/2020/09/15/about-the-tooling-available-to-create-native-graalvm-images/) by Michael Simons. How to compile Neo4J into a native image (including substitution example).
- [GraalVM JNI Invocation API](https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/JNIInvocationAPI/) (on GitHub available as [https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/JNIInvocationAPI.md](JNIInvocationAPI.md))
- [Embedding Truffle Languages](https://nirvdrum.com/2022/05/09/truffle-language-embedding.html) by Kevin Menard. Compares the usage of the Native Image C API and the JNI Invocation API for calling Java methods from an Native Image shared library.
- [Understanding Class Initialization in GraalVM Native Image Generation](https://medium.com/graalvm/understanding-class-initialization-in-graalvm-native-image-generation-d765b7e4d6ed) by Christian Wimmer
- [Updates on Class Initialization in GraalVM Native Image Generation](https://medium.com/graalvm/updates-on-class-initialization-in-graalvm-native-image-generation-c61faca461f7) by Christian Wimmer
- [Native Image Substitutions](https://build-native-java-apps.cc/developer-guide/substitution/)
- [Native Image Features](https://build-native-java-apps.cc/developer-guide/feature/)
- [Quarkus - Including Native Libraries in the Native Image](https://github.com/quarkusio/quarkus/blob/main/adr/0006-native-compilation-with-binary-libraries.adoc)