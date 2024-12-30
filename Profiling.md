## The [Truffle Profiler](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Profiling/)

The Truffle framework comes with a simple, sampling based profiler for Truffle languages built on top of the [Truffle safepoints](./Safepoints.md). It can either be used manually by leveraging the [`com.oracle.truffle.tools.profiler.CPUSampler`](https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/CPUSampler.html) class or configured directly on the Truffle engine as follows:
```java
Engine engine = Engine.newBuilder("js")
  .option("cpusampler", System.getProperty("cpusampler", "false"))
  .option("cpusampler.Output", System.getProperty("cpusampler.Output", "calltree"))
  .option("cpusampler.OutputFile", System.getProperty("cpusampler.OutputFile", "/tmp/SimpleCompilation_cpusampler.txt"))
  .option("cpusampler.Period", System.getProperty("cpusampler.Period", "10")) // in ms
  .option("cpusampler.ShowTiers", System.getProperty("cpusampler.ShowTiers", "true"))
  .option("cpusampler.SampleInternal", System.getProperty("cpusampler.SampleInternal", "false"))
  .option("cpusampler.SampleContextInitialization", System.getProperty("cpusampler.SampleContextInitialization", "false"))
  .build();
```
For the following analysis, we will use a simple JavaScript program which uses sevelar language features like function calls, loops and builtin library functions:
```JavaScript
function main(arg) {
  let res = 0;
  for (let i = 0; i < 100000; i++) {
    res += test(arg);
  }
  return res;
}
function test(x) {
  let a = test_pow(x);
  let b = test_iterate(x, Math.floor(a));
  let c = test_loop(x, b);
  return a + b + c;
}
function test_pow(x) {
  return Math.pow(x, 2.5) * Math.pow(x/2, 1.5) * Math.pow(x/3, 0.5);
}
function test_iterate(x, y) {
  let sum = 0, i = x;
  if (i-- > 0) sum += y;
  if (i-- > 0) sum += y;
  if (i-- > 0) sum += y;
  if (i-- > 0) sum += y;
  if (i-- > 0) sum += y;
  return sum;
}
function test_loop(x, y) {
  let sum = 0;
  for (let i = x; i > 0; i--) {
    sum += y;
  }
  return sum;
}
```
We save this program in a String, create a [`Source`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Source.html) object and evaluate it:
```Java
String js = """
            function main(arg) {
            ...
            """;
Source source = Source.newBuilder("js", js, "test.js").build();
context.eval(source);
```
Once we've done this, we can execute the JavaScript `main()` function from Java (the [full program is available in `SimpleCompilation.java`](./src/main/java/io/simonis/graaljs/test/SimpleCompilation.java)):
```Java
Value jsBindings = context.getBindings("js");
Value main = jsBindings.getMember("main");
for (int j = 0; j < iterations; j++) {
  main.execute(j % 5);
}
```

If we run this on a non-JVMCI capable JVM (or with `-XX:-EnableJVMCI`) for 100 `iterations` we will get the following profiler output in the file configured with `cpusampler.OutputFile`:
```
--------------------------------------------------------------------------------------------------------------------
Sampling Call Tree. Recorded 1724 samples with period 10ms. Missed 30 samples.
  Self Time: Time spent on the top of the stack.
  Total Time: Time spent somewhere on the stack.
  T0: Percent of time spent in interpreter.
--------------------------------------------------------------------------------------------------------------------
 Name           ||             Total Time    |   T0   ||              Self Time    |   T0   || Location
--------------------------------------------------------------------------------------------------------------------
 main           ||            17230ms  99.9% | 100.0% ||              340ms   2.0% | 100.0% || test.js~1-7:0-113
  test          ||            16890ms  98.0% | 100.0% ||             1900ms  11.0% | 100.0% || test.js~8-13:115-246
   test_pow     ||             6440ms  37.4% | 100.0% ||             6440ms  37.4% | 100.0% || test.js~14-18:248-467
   test_loop    ||             4790ms  27.8% | 100.0% ||             4790ms  27.8% | 100.0% || test.js~28-34:662-768
   test_iterate ||             3760ms  21.8% | 100.0% ||             3760ms  21.8% | 100.0% || test.js~19-27:469-660
 :program       ||               10ms   0.1% | 100.0% ||               10ms   0.1% | 100.0% || test.js~1-34:0-769
--------------------------------------------------------------------------------------------------------------------
```
The row labeled with `:program` denotes the time spent in JavaScript code when we evaluate the JavaScript source code (i.e. `context.eval(source)`). We actually don't execute any code there, so this time is minimal and only accounts for parsing our program. Besides that, we can see the self and total time of our JS functions. Notice how the `Location` column refers to the relative lines of the JS program that we've stored in the Java string `js` and to the virtual file `test.js` that we've specified when we created the `Source` object.

If we set the `cpusampler.SampleInternal` option to `true`, we also get the time spent in the builtin functions `Math.pow()` and `Math.floor()`:

```
--------------------------------------------------------------------------------------------------------------------
 Name           ||             Total Time    |   T0   ||              Self Time    |   T0   || Location
--------------------------------------------------------------------------------------------------------------------
 main           ||            16980ms  99.9% | 100.0% ||              380ms   2.2% | 100.0% || test.js~1-7:0-113
  test          ||            16600ms  97.7% | 100.0% ||              570ms   3.4% | 100.0% || test.js~8-13:115-246
   test_pow     ||             6600ms  38.8% | 100.0% ||               90ms   0.5% | 100.0% || test.js~14-18:248-467
    Math.pow    ||             6510ms  38.3% | 100.0% ||             6510ms  38.3% | 100.0% || <builtin>~1:0
   test_loop    ||             4460ms  26.3% | 100.0% ||             4460ms  26.3% | 100.0% || test.js~28-34:662-768
   test_iterate ||             3400ms  20.0% | 100.0% ||             3400ms  20.0% | 100.0% || test.js~19-27:469-660
   Math.floor   ||             1570ms   9.2% | 100.0% ||             1570ms   9.2% | 100.0% || <builtin>~1:0
 :program       ||               10ms   0.1% | 100.0% ||               10ms   0.1% | 100.0% || test.js~1-34:0-769
--------------------------------------------------------------------------------------------------------------------
```

## Profiling various GraalJS configurations with [AsyncProfiler](https://github.com/async-profiler/async-profiler)

- Running the [Octane](https://github.com/chromium/octane) `raytrace.js` benchmark without the GraalVM compiler:

```
$ time java -agentpath:libasyncProfiler.so=start,event=cpu,cstack=vm,ann,file=raytrace_nograal.html \
            -Xbatch -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:+PrintCodeCache \
            -XX:-UseCodeCacheFlushing -XX:-MethodFlushing -XX:-PrintCompilation \
            -XX:+UnlockDiagnosticVMOptions -XX:-PrintInlining -XX:+CITime \
            -XX:+UnlockExperimentalVMOptions -XX:-EnableJVMCI \
            --module-path target/js-deps --add-modules org.graalvm.polyglot \
            --upgrade-module-path target/compiler-deps \
            -cp target/graal-js-test-1.0-SNAPSHOT.jar \
            io.simonis.graaljs.test.OctaneBenchmarkRunner raytrace.js

Score (version 9): 565

Individual compiler times (for compiled methods only)
------------------------------------------------

  C1 {speed: 430770,854 bytes/s; standard:  0,722 s, 310947 bytes, 3191 methods; osr:  0,002 s, 991 bytes, 1 methods; nmethods_size: 7664736 bytes; nmethods_code_size: 4614504 bytes}
  C2 {speed: 110109,462 bytes/s; standard:  2,701 s, 297741 bytes, 776 methods; osr:  0,014 s, 1239 bytes, 3 methods; nmethods_size: 2558520 bytes; nmethods_code_size: 971720 bytes}

Individual compilation Tier times (for compiled methods only)
------------------------------------------------

  Tier1 {speed: 103790,907 bytes/s; standard:  0,017 s, 1717 bytes, 376 methods; osr:  0,000 s, 0 bytes, 0 methods; nmethods_size: 102568 bytes; nmethods_code_size: 55928 bytes}
  Tier2 {speed:  0,000 bytes/s; standard:  0,000 s, 0 bytes, 0 methods; osr:  0,000 s, 0 bytes, 0 methods; nmethods_size: 0 bytes; nmethods_code_size: 0 bytes}
  Tier3 {speed: 438415,311 bytes/s; standard:  0,706 s, 309230 bytes, 2815 methods; osr:  0,002 s, 991 bytes, 1 methods; nmethods_size: 7562168 bytes; nmethods_code_size: 4558576 bytes}
  Tier4 {speed: 110109,462 bytes/s; standard:  2,701 s, 297741 bytes, 776 methods; osr:  0,014 s, 1239 bytes, 3 methods; nmethods_size: 2558520 bytes; nmethods_code_size: 971720 bytes}

Accumulated compiler times
----------------------------------------------------------
  Total compilation time   :   3,439 s
    Standard compilation   :   3,423 s, Average : 0,001 s
    Bailed out compilation :   0,000 s, Average : 0,000 s
    On stack replacement   :   0,016 s, Average : 0,004 s
    Invalidated            :   0,000 s, Average : 0,000 s

    C1 Compile Time:        0,722 s

    C2 Compile Time:        2,714 s

  Total compiled methods    :     3971 methods
    Standard compilation    :     3967 methods
    On stack replacement    :        4 methods
  Total compiled bytecodes  :   610918 bytes
    Standard compilation    :   608688 bytes
    On stack replacement    :     2230 bytes
  Average compilation speed :   177621 bytes/s

  nmethod code size         :  5586224 bytes
  nmethod total size        : 10223256 bytes
CodeHeap 'non-profiled nmethods': size=120036Kb used=3504Kb max_used=3504Kb free=116531Kb
 bounds [0x00007fffdf8c7000, 0x00007fffdfc37000, 0x00007fffe6e00000]
CodeHeap 'profiled nmethods': size=120032Kb used=9240Kb max_used=9240Kb free=110791Kb
 bounds [0x00007fffd7e00000, 0x00007fffd8710000, 0x00007fffdf338000]
CodeHeap 'non-nmethods': size=5692Kb used=1430Kb max_used=1460Kb free=4261Kb
 bounds [0x00007fffdf338000, 0x00007fffdf5a8000, 0x00007fffdf8c7000]
 total_blobs=4911 nmethods=4155 adapters=663
 compilation: enabled
              stopped_count=0, restarted_count=0
 full_count=0

real    2m40,144s
user    2m39,055s
sys     0m0,818s
```
[![Example](https://github.com/simonis/GraalJsTest/blob/main/data/flamegraphs/raytrace_nograal.png)](https://htmlpreview.github.io/?https://github.com/simonis/GraalJsTest/blob/main/data/flamegraphs/raytrace_nograal.html)

- Now running the same benchmark with "jargraal" (i.e. the pure Java version of the GraalVM compiler):

```
$ time java -agentpath:libasyncProfiler.so=start,event=cpu,cstack=vm,ann,file=raytrace_jargraal.html \
            -Xbatch -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:+PrintCodeCache \
            -XX:-UseCodeCacheFlushing -XX:-MethodFlushing -XX:-PrintCompilation \
            -XX:+UnlockDiagnosticVMOptions -XX:-PrintInlining -XX:+CITime \
            -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
            -XX:-UseJVMCINativeLibrary -XX:JVMCILibPath=libjvmcicompiler.so.image/ \
            -Dpolyglot.engine.CompilationStatistics=true \
            -Dpolyglot.engine.TraceCompilation=false \
            -Dpolyglot.compiler.TraceInlining=false \
            --module-path target/js-deps --add-modules org.graalvm.polyglot \
            --upgrade-module-path target/compiler-deps \
            -cp target/graal-js-test-1.0-SNAPSHOT.jar \
            io.simonis.graaljs.test.OctaneBenchmarkRunner raytrace.js

Score (version 9): 9524

[engine] Truffle runtime statistics for engine 1
    Compilations                : 123

Individual compiler times (for compiled methods only)
------------------------------------------------

  C1 {speed: 363302,599 bytes/s; standard:  3,490 s, 1267441 bytes, 10102 methods; osr:  0,049 s, 18256 bytes, 18 methods; nmethods_size: 32852960 bytes; nmethods_code_size: 19970104 bytes}
  C2 {speed: 74512,286 bytes/s; standard: 25,139 s, 1906563 bytes, 2989 methods; osr:  1,874 s, 106254 bytes, 57 methods; nmethods_size: 16733448 bytes; nmethods_code_size: 6187136 bytes}

Individual compilation Tier times (for compiled methods only)
------------------------------------------------

  Tier1 {speed: 96063,798 bytes/s; standard:  0,062 s, 5993 bytes, 1287 methods; osr:  0,000 s, 0 bytes, 0 methods; nmethods_size: 353680 bytes; nmethods_code_size: 192608 bytes}
  Tier2 {speed: 476436,809 bytes/s; standard:  0,053 s, 25441 bytes, 90 methods; osr:  0,000 s, 0 bytes, 0 methods; nmethods_size: 336200 bytes; nmethods_code_size: 133200 bytes}
  Tier3 {speed: 366408,138 bytes/s; standard:  3,374 s, 1236007 bytes, 8725 methods; osr:  0,049 s, 18256 bytes, 18 methods; nmethods_size: 32163080 bytes; nmethods_code_size: 19644296 bytes}
  Tier4 {speed: 74512,286 bytes/s; standard: 25,139 s, 1906563 bytes, 2989 methods; osr:  1,874 s, 106254 bytes, 57 methods; nmethods_size: 16733448 bytes; nmethods_code_size: 6187136 bytes}

Accumulated compiler times
----------------------------------------------------------
  Total compilation time   :  30,552 s
    Standard compilation   :  28,628 s, Average : 0,002 s
    Bailed out compilation :   0,000 s, Average : 0,000 s
    On stack replacement   :   1,924 s, Average : 0,026 s
    Invalidated            :   0,000 s, Average : 0,000 s

    C1 Compile Time:        3,529 s

    C2 Compile Time:       26,993 s

    JVMCI CompileBroker Time:
       Compile:          0,000 s
       Install Code:     0,000 s (installs: 0, CodeBlob total size: 0, CodeBlob code size: 0)

    JVMCI Hosted Time:
       Install Code:     0,985 s (installs: 131, CodeBlob total size: 1192016, CodeBlob code size: 449856)

  Total compiled methods    :    13166 methods
    Standard compilation    :    13091 methods
    On stack replacement    :       75 methods
  Total compiled bytecodes  :  3298514 bytes
    Standard compilation    :  3174004 bytes
    On stack replacement    :   124510 bytes
  Average compilation speed :   107963 bytes/s

  nmethod code size         : 26157240 bytes
  nmethod total size        : 49586408 bytes
CodeHeap 'non-profiled nmethods': size=120036Kb used=21344Kb max_used=21344Kb free=98692Kb
 bounds [0x00007fffdf8c7000, 0x00007fffe0da7000, 0x00007fffe6e00000]
CodeHeap 'profiled nmethods': size=120032Kb used=38075Kb max_used=38075Kb free=81956Kb
 bounds [0x00007fffd7e00000, 0x00007fffda330000, 0x00007fffdf338000]
CodeHeap 'non-nmethods': size=5692Kb used=1589Kb max_used=1653Kb free=4102Kb
 bounds [0x00007fffdf338000, 0x00007fffdf5a8000, 0x00007fffdf8c7000]
 total_blobs=14568 nmethods=13619 adapters=846
 compilation: enabled
              stopped_count=0, restarted_count=0
 full_count=0

real    0m30,025s
user    0m52,552s
sys     0m0,996s
```

[![Example](https://github.com/simonis/GraalJsTest/blob/main/data/flamegraphs/raytrace_jargraal.png)](https://htmlpreview.github.io/?https://github.com/simonis/GraalJsTest/blob/main/data/flamegraphs/raytrace_jargraal.html)

- And finally the same benchmark with "libgraal" (i.e. the native version of the GraalVM compiler):

```
$ time java -agentpath:libasyncProfiler.so=start,event=cpu,cstack=vm,ann,file=raytrace_libgraal.html \
            -Xbatch -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:+PrintCodeCache \
            -XX:-UseCodeCacheFlushing -XX:-MethodFlushing -XX:-PrintCompilation \
            -XX:+UnlockDiagnosticVMOptions -XX:-PrintInlining -XX:+CITime \
            -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
            -XX:+UseJVMCINativeLibrary -XX:JVMCILibPath=libjvmcicompiler.so.image/ \
            -Dpolyglot.engine.CompilationStatistics=true \
            -Dpolyglot.engine.TraceCompilation=false \
            -Dpolyglot.compiler.TraceInlining=false \
            --module-path target/js-deps --add-modules org.graalvm.polyglot \
            --upgrade-module-path target/compiler-deps \
            -cp target/graal-js-test-1.0-SNAPSHOT.jar \
            io.simonis.graaljs.test.OctaneBenchmarkRunner raytrace.js

Score (version 9): 22413

[engine] Truffle runtime statistics for engine 1
    Compilations                : 263

Individual compiler times (for compiled methods only)
------------------------------------------------

  C1 {speed: 350694,398 bytes/s; standard:  1,087 s, 380845 bytes, 3977 methods; osr:  0,004 s, 1640 bytes, 2 methods; nmethods_size: 9665232 bytes; nmethods_code_size: 5770768 bytes}
  C2 {speed: 86727,771 bytes/s; standard:  3,112 s, 271085 bytes, 810 methods; osr:  0,053 s, 3434 bytes, 5 methods; nmethods_size: 2290864 bytes; nmethods_code_size: 879864 bytes}

Individual compilation Tier times (for compiled methods only)
------------------------------------------------

  Tier1 {speed: 99274,957 bytes/s; standard:  0,022 s, 2219 bytes, 480 methods; osr:  0,000 s, 0 bytes, 0 methods; nmethods_size: 131144 bytes; nmethods_code_size: 71432 bytes}
  Tier2 {speed: 517335,473 bytes/s; standard:  0,001 s, 601 bytes, 3 methods; osr:  0,000 s, 0 bytes, 0 methods; nmethods_size: 8104 bytes; nmethods_code_size: 3248 bytes}
  Tier3 {speed: 355779,175 bytes/s; standard:  1,063 s, 378025 bytes, 3494 methods; osr:  0,004 s, 1640 bytes, 2 methods; nmethods_size: 9525984 bytes; nmethods_code_size: 5696088 bytes}
  Tier4 {speed: 86727,771 bytes/s; standard:  3,112 s, 271085 bytes, 810 methods; osr:  0,053 s, 3434 bytes, 5 methods; nmethods_size: 2290864 bytes; nmethods_code_size: 879864 bytes}

Accumulated compiler times
----------------------------------------------------------
  Total compilation time   :   4,256 s
    Standard compilation   :   4,199 s, Average : 0,001 s
    Bailed out compilation :   0,000 s, Average : 0,000 s
    On stack replacement   :   0,057 s, Average : 0,008 s
    Invalidated            :   0,000 s, Average : 0,000 s

    C1 Compile Time:        1,087 s

    C2 Compile Time:        3,162 s

    JVMCI CompileBroker Time:
       Compile:          0,000 s
       Install Code:     0,000 s (installs: 0, CodeBlob total size: 0, CodeBlob code size: 0)

    JVMCI Hosted Time:
       Install Code:     0,798 s (installs: 272, CodeBlob total size: 3259944, CodeBlob code size: 1272176)

  Total compiled methods    :     4794 methods
    Standard compilation    :     4787 methods
    On stack replacement    :        7 methods
  Total compiled bytecodes  :   657004 bytes
    Standard compilation    :   651930 bytes
    On stack replacement    :     5074 bytes
  Average compilation speed :   154373 bytes/s

  nmethod code size         :  6650632 bytes
  nmethod total size        : 11956096 bytes
CodeHeap 'non-profiled nmethods': size=120036Kb used=6572Kb max_used=6572Kb free=113463Kb
 bounds [0x00007fffdf8c7000, 0x00007fffdff37000, 0x00007fffe6e00000]
CodeHeap 'profiled nmethods': size=120032Kb used=11628Kb max_used=11628Kb free=108404Kb
 bounds [0x00007fffd7e00000, 0x00007fffd8960000, 0x00007fffdf338000]
CodeHeap 'non-nmethods': size=5692Kb used=1470Kb max_used=1585Kb free=4222Kb
 bounds [0x00007fffdf338000, 0x00007fffdf5a8000, 0x00007fffdf8c7000]
 total_blobs=6132 nmethods=5334 adapters=699
 compilation: enabled
              stopped_count=0, restarted_count=0
 full_count=0

real    0m10,991s
user    0m30,760s
sys     0m0,895s
```

[![Example](https://github.com/simonis/GraalJsTest/blob/main/data/flamegraphs/raytrace_libgraal.png)](https://htmlpreview.github.io/?https://github.com/simonis/GraalJsTest/blob/main/data/flamegraphs/raytrace_libgraal.html)
