## GraalPython

### Building GraalPython

As for [GraalJS](Notes.md#building-graaljs), we first need to clone the [GraalPython](https://github.com/oracle/graalpython) repository in parallel to main [Graal](https://github.com/oracle/graal.git) and [`mx`](https://github.com/graalvm/mx.git) repositories. Graal and GraalPython should be synced to the same version (e.g. `release/graal-vm/25.0`) and `mx` should be checked out at the `mx_version` referenced in `graal/common.json` (e.g. `7.54.3`).

In order to build the GraalPython jars/modules along with their dependencies we can do the following:
```bash
$ cd graalpython
$ MX_ALT_OUTPUT_ROOT=/tmp/graalpy-25.0 \
  MX_PYTHON=/Python-3.13.3_bin/bin/python3 \
  mx build --build-logs oneline --targets GRAALPYTHON,GRAALPYTHON_RESOURCES,GRAALPYTHON_EMBEDDING
```
The resulting artifacts can be found under `$MX_ALT_OUTPUT_ROOT/graalpython/dists/`:

<details>
  <summary>GraalPython build artifacts</summary>

```bash
$ ls -1 $MX_ALT_OUTPUT_ROOT/graalpython/dists/*.jar
/tmp/graalpy-25.0/graalpython/dists/graalpython-embedding.jar
/tmp/graalpy-25.0/graalpython/dists/graalpython.jar
/tmp/graalpy-25.0/graalpython/dists/graalpython-launcher.jar
/tmp/graalpy-25.0/graalpython/dists/graalpython-processor.jar
/tmp/graalpy-25.0/graalpython/dists/graalpython-resources.jar
```
</details>

`graalpython.jar` is the Python language implementation (built by the target `GRAALPYTHON`), `graalpython-resources.jar` is the Python standard library (built by the target `GRAALPYTHON_RESOURCES`) and `graalpython-embedding.jar` (built by the target `GRAALPYTHON_EMBEDDING`) contains various helper classes like [`GraalPyResources`](https://github.com/oracle/graalpython/blob/release/graal-vm/25.0/graalpython/org.graalvm.python.embedding/src/org/graalvm/python/embedding/GraalPyResources.java) and [`VirtualFileSystem`](https://github.com/oracle/graalpython/blob/release/graal-vm/25.0/graalpython/org.graalvm.python.embedding/src/org/graalvm/python/embedding/VirtualFileSystem.java) which are required for [embedding GraalPy into Java applications](https://github.com/oracle/graalpython/blob/master/docs/user/Embedding-Build-Tools.md).

Notice that `graalpython.jar`/`graalpython-embedding.jar` have quite some transitive dependencies. If we set up a minimal GraalPython Maven project as follows:
```xml
  <dependencies>
    <dependency>
      <groupId>org.graalvm.polyglot</groupId>
      <artifactId>polyglot</artifactId>
      <version>${graalvm.version}</version>
    </dependency>
    <dependency>
      <groupId>org.graalvm.polyglot</groupId>
      <artifactId>python-community</artifactId>
      <version>${graalvm.version}</version>
      <type>pom</type>
    </dependency>
  </dependencies>
```
This will pull in all the following dependencies:
```bash
$ mvn dependency:tree
...
[INFO] --- dependency:3.6.1:tree (default-cli) @ graal-python-test ---
[INFO] io.simonis:graal-python-test:jar:1.0-SNAPSHOT
[INFO] +- org.graalvm.polyglot:polyglot:jar:24.2.1:compile
[INFO] |  +- org.graalvm.sdk:collections:jar:24.2.1:compile
[INFO] |  \- org.graalvm.sdk:nativeimage:jar:24.2.1:compile
[INFO] |     \- org.graalvm.sdk:word:jar:24.2.1:compile
[INFO] \- org.graalvm.polyglot:python-community:pom:24.2.1:compile
[INFO]    \- org.graalvm.python:python-community:pom:24.2.1:runtime
[INFO]       +- org.graalvm.python:python-language:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.truffle:truffle-api:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.tools:profiler-tool:jar:24.2.1:runtime
[INFO]       |  |  \- org.graalvm.shadowed:json:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.regex:regex:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.truffle:truffle-nfi:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.truffle:truffle-nfi-libffi:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.llvm:llvm-api:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.shadowed:icu4j:jar:24.2.1:runtime
[INFO]       |  +- org.graalvm.shadowed:xz:jar:24.2.1:runtime
[INFO]       |  +- org.bouncycastle:bcprov-jdk18on:jar:1.78.1:runtime
[INFO]       |  +- org.bouncycastle:bcpkix-jdk18on:jar:1.78.1:runtime
[INFO]       |  \- org.bouncycastle:bcutil-jdk18on:jar:1.78.1:runtime
[INFO]       +- org.graalvm.python:python-resources:jar:24.2.1:runtime
[INFO]       \- org.graalvm.truffle:truffle-runtime:jar:24.2.1:runtime
[INFO]          +- org.graalvm.sdk:jniutils:jar:24.2.1:runtime
[INFO]          \- org.graalvm.truffle:truffle-compiler:jar:24.2.1:runtime
```
In the next section we will explain how we can easily build all these dependencies from within the `graalpython/` repository.

#### Building the GraalPython Maven artifacts

Is is also possible to build all the GraalPython artifacts along with their dependencies into a local Maven repository. In order to do this, we first have to build the full GraalPython suite with all its dependencies:

```bash
$ GRADLE_JAVA_HOME=/corretto-21 \
  MX_ALT_OUTPUT_ROOT=/tmp/graalpy-25.0 \
  MX_PYTHON=/Python-3.13.3_bin/bin/python3 \
  mx build --build-logs oneline
```

Among the full set of modules/jars in the `$MX_ALT_OUTPUT_ROOT/*/dists/` subdirectories, this will also create a standalone GraalPY distribution under `$MX_ALT_OUTPUT_ROOT/graalpython/linux-amd64/GRAALPY_JVM_STANDALONE/` with the three binary launchers `bin/{graalpy,python,python3}` and a full blown GraalJDK with GraalPy included under `$MX_ALT_OUTPUT_ROOT/sdk/linux-amd64/GRAALVM_AEF5EAA70A_JAVA25/graalvm-aef5eaa70a-java25-25.0.0-dev` (the strange hash in the path name is described in the section [Building GraalVM Native Image](#building-graalvm-native-image)).

Once we have a full GraalPython build we can call
```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/graalpy-25.0 \
  MX_PYTHON=/Python-3.13.3_bin/bin/python3 \
  mx maven-deploy --all-suites \
  --licenses EPL-2.0,PSF-License,GPLv2-CPE,ICU,GPLv2,BSD-simplified,BSD-new,UPL,MIT \
  graalvm-snapshot-repo file:///tmp/graalpy-25.0-mvn
```
in order to create the Maven artifacts into the local Maven repository under `/tmp/graalpy-25.0-mvn`. `graalvm-snapshot-repo` is the mandatory `repository-id` argument of `mx maven-deploy` (run `mx maven-deploy --help` for more information) which I think isn't used for local deployments and `file:///tmp/graalpy-25.0-mvn` is the URL of the Maven repository (in this case a local directory).

Once we've deployed the GraalPy artifacts and their dependencies to a local repository, we can use it as follows in a POM file:
```xml
  <repositories>
    <!--
        Local repository with snapshot builds.
    -->
    <repository>
      <id>graalvm-snapshot-repo</id>
      <name>graalvm-snapshot-repo</name>
      <url>file://${MAVEN_REPOSITORY}</url>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>

  <profiles>
    <profile>
      <id>graal-25-0-0</id>
      <build>
        <directory>${basedir}/target-25-0-0-python</directory>
      </build>
      <activation>
        <property>
          <name>MAVEN_REPOSITORY</name>
        </property>
      </activation>
      <properties>
        <graalvm.version>25.0.0-SNAPSHOT</graalvm.version>
      </properties>
      ...
    </profile>
  </profiles>
```
With these settings, the profile `graal-25-0-0` will be activated if we pass `-DMAVEN_REPOSITORY=/tmp/graalpy-25.0-mvn` on the `mvn` command line and it will look for the Graal/GraalPy artifacts with the version `25.0.0-SNAPSHOT` in the local Maven repository at `/tmp/graalpy-25.0-mvn` that we've just created.

Once we run `mvn` with these parameters, the required artifacts will be copied from the local Maven repository into our local Maven cache (usuallyl located at `~/.m2`). We can remove all the cached dependencies of our current project/profile by using the [following command](https://www.baeldung.com/maven-clear-cache):

```bash
$ mvn -DMAVEN_REPOSITORY=/tmp/graalpy-25.0-mvn \
  dependency:purge-local-repository -DactTransitively=false -DreResolve=false
```

> [!NOTE]
> If we want to use the GraalPy artifact from the local repository we've just created, we have to use `org.graalvm.python` instead of `org.graalvm.polyglot` as `groupId` for the `python-community` artifact. This is because `mx maven-deploy` only builds the [org.graalvm.python/python-community](https://mvnrepository.com/artifact/org.graalvm.python/python-community) POM and not [org.graalvm.polyglot/python-community](https://mvnrepository.com/artifact/org.graalvm.polyglot/python-community). The latter is generated in the `graal/vm` suite by [`create_polyglot_meta_pom_distribution_from_base_distribution()`](https://github.com/oracle/graal/blob/c5df0c319473ceb21e7d9e9efa6896af496c0006/vm/mx.vm/mx_vm.py#L292) from the former and merely redirects to it.

> [!NOTE]
> It is also possible to download pre-built Maven bundles (e.g. [maven-resource-bundle-community-dev.tar.gz](https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/25.0.0-dev-20250607_2256/maven-resource-bundle-community-dev.tar.gz)) for the latest GraalVM Community development builds from https://github.com/graalvm/graalvm-ce-dev-builds as well as Oracle GraalVM early access builds (including Maven bundles) from https://github.com/graalvm/oracle-graalvm-ea-builds.

#### Building the GraalPython standalone distributions

A GraalPython standalone distribution is basically a stripped down JDK with only the three launchers `python`, `python3` and `graalpy` and all the required classes to run Python (up until 24.2.0, they also contained `graalpy-lt`, a "*launcher to use LLVM toolchain and Sulong execution of native extensions*", but that was deprecated and removed in GraalVM 25.0.0).

The standalone distributions come in three flavours: with a standard JDK and "jargraal" (see [Notes.md](./Notes.md#building-the-graalvm-compiler)), with a standard JDK and "libgraal" (i.e. the natively compiled version of the Graal compiler) and as a native version without bundled JDK where GraalPy and all its dependencies are compiled into a huge shared library (i.e. [isolate](./Notes.md#graalvm)) called `libpythonvm.so`. They can be build with `mx --env jvm-ce`, `mx --env jvm-ce-libgraal` and `mx --env native-ce` respectively which corresponds to the following build command lines if the predefined environment files `./mx.graalpython/{jvm-ce,jvm-ce-libgraal,native-ce}` are not used:

```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/graalpy-25.0 \
  mx \
  --dynamicimports /compiler \
  build --build-logs oneline \
  --targets GRAALPY_JVM_STANDALONE
...
$ $MX_ALT_OUTPUT_ROOT/graalpython/linux-amd64/GRAALPY_JVM_STANDALONE/bin/graalpy
Python 3.11.7 (Tue Jul 22 21:00:56 CEST 2025)
[Graal, Interpreted, Java 25.0.1-internal (amd64)] on linux
Type "help", "copyright", "credits" or "license" for more information.
>>>
```

```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/graalpy-25.0 \
  mx \
  --dynamicimports /compiler \
  --dynamicimports /substratevm \
  --native-images=lib:jvmcicompiler \
  --components=LibGraal \
  build --build-logs oneline \
  --targets GRAALPY_JVM_STANDALONE
...
$ $MX_ALT_OUTPUT_ROOT/graalpython/linux-amd64/GRAALPY_JVM_STANDALONE/bin/graalpy
Python 3.11.7 (Wed Jul 23 16:37:23 CEST 2025)
[Graal, GraalVM CE, Java 25.0.1-internal (amd64)] on linux
Type "help", "copyright", "credits" or "license" for more information.
>>> __graalpython__.is_native
False
```

```bash
$ MX_ALT_OUTPUT_ROOT=/tmp/graalpy-25.0 \
  mx \
  --dynamicimports /compiler \
  --dynamicimports /substratevm \
  --native-images=lib:pythonvm \
  --components=SubstrateVM,'Truffle SVM Macro' \
  build --build-logs oneline \
  --targets GRAALPY_NATIVE_STANDALONE
...
$ $MX_ALT_OUTPUT_ROOT/graalpython/linux-amd64/GRAALPY_NATIVE_STANDALONE/bin/graalpy
Python 3.11.7 (Thu Jul 24 17:24:44 CEST 2025)
[Graal, GraalVM CE, Java 25.0.1-internal (amd64)] on linux
Type "help", "copyright", "credits" or "license" for more information.
>>> __graalpython__.is_native
True
```

The distros can afterwards be found under `$MX_ALT_OUTPUT_ROOT/graalpython/linux-amd64/GRAALPY_JVM_STANDALONE/` and `$MX_ALT_OUTPUT_ROOT/graalpython/linux-amd64/GRAALPY_NATIVE_STANDALONE/` respectively.

### Building Python modules/wheels from source

Once we've build a GraalPy standalone distribution, we can
### Resources management in GraalVM Ployglot/Truffle

The Truffle framework has a sophisticated machinery for extracting, caching and handling resources required by Truffle itself but is also used by embedded languages and tools. Whenever a polyglot [`Engine`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Engine.html) is created, it will first setup all the required resources.


#### References

- [Pre-build binary wheels provided by the GraalPy team](https://www.graalvm.org/python/wheels/)
- [Introduction to the Python implementation for GraalVM](https://medium.com/graalvm/how-to-contribute-to-graalpython-7fd304fe8bb9): nice blog about some GraalPy implementation details, but unfortunately a little outdated in some parts because it is from Aug. 2019.
