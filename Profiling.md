## Profiling various GraalJS configurations with [AsyncProfiler](https://github.com/async-profiler/async-profiler)

Running the [Octane](https://github.com/chromium/octane) `raytrace.js` benchmark without the GraalVM compiler:

```
$ time java -agentpath:libasyncProfiler.so=start,event=cpu,cstack=vm,ann,file=raytrace_nograal.html \
            -Xbatch -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:+PrintCodeCache \
			-XX:-UseCodeCacheFlushing -XX:-MethodFlushing -XX:-PrintCompilation \
			-XX:+UnlockDiagnosticVMOptions -XX:-PrintInlining -XX:+CITime \
			--module-path target/js-deps --add-modules org.graalvm.polyglot \
			-XX:+UnlockExperimentalVMOptions -XX:-EnableJVMCI \
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
