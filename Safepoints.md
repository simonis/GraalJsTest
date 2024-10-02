## Safepoints

The Truffle framework implements its own, cooperative safepoint mechanism for Truffle guest languages which is independent from HotSpots JVM Safepoints.

### HotSpot Safepoints

HotSpot's Safepoints are mainly used to coordinate and synchronize Java application threads with VM-internal operations like GC, class redefinition, code cache management, etc. A VM safpoint rolls all (or a subset - see [JEP 312: Thread-Local Handshakes](https://openjdk.org/jeps/312)) of the Java application threads forward to a known state and stops them there for the time required to execute a specific VM operation. Notice that you can not execute Java code at a VM safepoint - all the VM operations executed at a VM safeoint are implemented natively in the HotSpot JVM.

Safepointing is a cooperative mechanism. This means that Java threads can not be interrupted at any specific position if they execute optimized, Just-In-Time (JIT) compiled code. Instead, the Hotspot JVM adds safepoint polls at back-edges of uncounted loops and at the return from function calls. This ensures that safepoints can usually be reached in a timely manner to guarantee low-latency for operations like GC which have to be executed at the safepoint. Still, the time to reach a safpoint (i.e. "Time To Safepoint" or TTS) can be a problem in some cases (see e.g. [Math Intrinsics & Time To Safepoint (TTS)](https://simonis.github.io/HotspotIntrinsics/intrinsics.xhtml#/8/2) or the `-XX:+UseCountedLoopSafepoints` command line options which was added by [JDK-6869327: Add new C2 flag to keep safepoints in counted loops](https://bugs.openjdk.org/browse/JDK-6869327)).

Another issue with HotSpot Safepoints is the so called "Safepoint Bias". Because safepoints are only added by the JIT-compilers at specific positions like loops and function calls, tools which rely on safepointing (e.g. some Profilers) only get a *biased* view of the applications they analyze. E.g. the well-known tools like [`jstack`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jstack.html) rely on safepoints which means that the thread dumps they create might miss methods inlined by the JIT compilers because they don't contain any safepoints any more.

Various command line options like `AbortVMOnSafepointTimeout`, `GuaranteedSafepointInterval`, `SafepointTimeout`, `SafepointTimeoutDelay`, `UseCountedLoopSafepoints` and `-Xlog:safefpoint*` can be used to configure and monitor the safepoint behavior. Also see the [Bibliography](#bibliography) for additional, in-depth information on safepoints.

### Truffle Safepoints

The following description of Truffle Safepoints is taken right from the [Truffle Language Safepoint Tutorial](https://github.com/oracle/graal/blob/master/truffle/docs/Safepoints.md) in the Truffle repository:

> *As of 21.1 Truffle has support for guest language safepoints. Truffle safepoints allow to interrupt the guest language execution to perform thread local actions submitted by a language or tool. A safepoint is a location during the guest language execution where the state is consistent and other operations can read its state.*

Truffle safepoints (i.e. [`com.oracle.truffle.api.TruffleSafepoint`](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/TruffleSafepoint.html)) are implemented in Java and they work in Java, so in contrast to HotSpot safepoints it is possible to execute Java code at Truffle safepoints. They work by registering [ThreadLocalActions](https://github.com/oracle/graal/blob/master/truffle/docs/Safepoints.md#thread-local-actions) which will then be executed locally by every thread once it reaches a Truffle safepoint.

Although their implementation requires native support in the VM (see [Implementation details](#implementation-details)), Truffle safepoints are "application-level" safepoints. Everyone can trigger a safepoint on all or on a selected subset of active polyglot threads by calling [`submitThreadLocal(..)`](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/TruffleLanguage.Env.html#submitThreadLocal(java.lang.Thread%5B%5D,com.oracle.truffle.api.ThreadLocalAction)) on either a [language](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/TruffleLanguage.Env.html) or an [instrument](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/instrumentation/TruffleInstrument.Env.html) environment:
```java
Env env; // language or instrument environment

env.submitThreadLocal(null /*all threads*/,
                      new ThreadLocalAction(true /*side-effecting*/, true /*synchronous*/, false /*recurring*/) {
    @Override
    protected void perform(Access access) {
       // Safepoint action
    }
});
```
This call registers the corresponding action with the selected threads and arms the safepoint mechanism. Once armed, the selected threads which execute in a polyglot context will stop on the next execution of [`TruffleSafepoint::poll()`](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/TruffleSafepoint.html#poll(com.oracle.truffle.api.nodes.Node)) and execute the registered safepoint action(s) before they resume normal execution. It is the responsibility of guest language writers to ensure that guest language implementations call `poll()` in regular, reasonably small time intervals!

Various options like `-Dpolyglot.engine.SafepointALot=true`, `-Dpolyglot.engine.TraceMissingSafepointPollInterval=<ms>` or `-Dpolyglot.engine.TraceThreadLocalActions=true` exist for debugging Truffle safepoints (see [Safepoints.md](https://github.com/oracle/graal/blob/master/truffle/docs/Safepoints.md) for more details).

#### Implementation details

Truffle safepoints are implemented with the help of a special field in HotSpot's `JavaThread` class which is created for and mirrors every native Java thread (i.e. `java.lang.Thread`):
```cpp
class JavaThread: public Thread {
  ...
#if INCLUDE_JVMCI
  // Fast thread locals for use by JVMCI
  jlong      _jvmci_reserved0;
```

##### Arming Safepoints
Arming a safepoint is done by calling `submitThreadLocal()` which not only registers a `ThreadLocalAction` for execution, but also sets `_jvmci_reserved0` to `1`:

<details>
  <summary>Arming safepoints from the <a href="https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Profiling/">Truffle Profiler</a> (i.e. <a href="https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/CPUSampler.html"><code>CPUSampler</code></a>)</summary>

```java
(gdb) call ps()

"Executing ps"
 for thread: "Sampling thread" #14 [312315] daemon prio=5 os_prio=0 cpu=598,75ms elapsed=140,11s tid=0x00007ffff2c64220 nid=312315 runnable  [0x00007fffe2bf7000]
   java.lang.Thread.State: RUNNABLE
Thread: 0x00007ffff2c64220  [0x4c3fb] State: _running _at_poll_safepoint 0
   JavaThread state: _thread_in_vm

     at jdk.internal.misc.Unsafe.putIntVolatile(java.base@21.0.5-internal/Native Method)
     at sun.misc.Unsafe.putIntVolatile(jdk.unsupported@21.0.5-internal/Unsafe.java:968)
     at com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.setVolatile(org.graalvm.truffle.runtime/HotSpotThreadLocalHandshake.java:120)
     at com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.setFastPending(org.graalvm.truffle.runtime/HotSpotThreadLocalHandshake.java:93)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake$TruffleSafepointImpl.setFastPendingAndInterrupt(org.graalvm.truffle/ThreadLocalHandshake.java:482)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake$TruffleSafepointImpl.addHandshakeImpl(org.graalvm.truffle/ThreadLocalHandshake.java:474)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake$TruffleSafepointImpl.addHandshake(org.graalvm.truffle/ThreadLocalHandshake.java:465)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake.addHandshakes(org.graalvm.truffle/ThreadLocalHandshake.java:136)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake.runThreadLocal(org.graalvm.truffle/ThreadLocalHandshake.java:125)
     at com.oracle.truffle.polyglot.PolyglotThreadLocalActions.submit(org.graalvm.truffle/PolyglotThreadLocalActions.java:309)
     - locked <0x0000000673b1ef78> (a com.oracle.truffle.polyglot.PolyglotContextImpl)
     at com.oracle.truffle.polyglot.PolyglotThreadLocalActions.submit(org.graalvm.truffle/PolyglotThreadLocalActions.java:236)
     at com.oracle.truffle.polyglot.PolyglotThreadLocalActions.submit(org.graalvm.truffle/PolyglotThreadLocalActions.java:232)
     at com.oracle.truffle.polyglot.EngineAccessor$EngineImpl.submitThreadLocal(org.graalvm.truffle/EngineAccessor.java:1769)
     at com.oracle.truffle.api.instrumentation.TruffleInstrument$Env.submitThreadLocal(org.graalvm.truffle/TruffleInstrument.java:1317)
     at com.oracle.truffle.tools.profiler.SafepointStackSampler.sample(com.oracle.truffle.tools.profiler/SafepointStackSampler.java:101)
     at com.oracle.truffle.tools.profiler.CPUSampler$SamplingTask.run(com.oracle.truffle.tools.profiler/CPUSampler.java:753)
     at java.util.concurrent.Executors$RunnableAdapter.call(java.base@21.0.5-internal/Executors.java:572)
     at java.util.concurrent.FutureTask.runAndReset(java.base@21.0.5-internal/FutureTask.java:358)
     at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(java.base@21.0.5-internal/ScheduledThreadPoolExecutor.java:305)
     at java.util.concurrent.ThreadPoolExecutor.runWorker(java.base@21.0.5-internal/ThreadPoolExecutor.java:1144)
     at java.util.concurrent.ThreadPoolExecutor$Worker.run(java.base@21.0.5-internal/ThreadPoolExecutor.java:642)
     at java.lang.Thread.runWith(java.base@21.0.5-internal/Thread.java:1596)
     at java.lang.Thread.run(java.base@21.0.5-internal/Thread.java:1583)
     at com.oracle.truffle.polyglot.SystemThread.run(org.graalvm.truffle/SystemThread.java:68)
```
</details>


Notice that although `_jvmci_reserved0` is not declared volatile, it is written with *volatile* semantics through `sun.misc.Unsafe::putIntVolatile()` (which gets efficiently inlined and intrinisfied by the JIT compiler into `HotSpotThreadLocalHandshake.setVolatile()`).

##### Polling Safepoints

Polling is implemented in [`TruffleSafepoint::poll()`](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/TruffleSafepoint.html#poll(com.oracle.truffle.api.nodes.Node)) by checking if `_jvmci_reserved0` is different from `0`. There are two different paths for doing this. If guest languages run in the Truffle interpreter, `sun.misc.Unsafe::getInt()` is used to read `_jvmci_reserved0` out of the `JavaThread` object (see [`HotSpotThreadLocalHandshake.java`](https://github.com/oracle/graal/blob/dda64b4eff4ce61e6a2544c5faf66dc4f1c1207c/truffle/src/com.oracle.truffle.runtime/src/com/oracle/truffle/runtime/hotspot/HotSpotThreadLocalHandshake.java#L73-L82) for the details):

```java
final class HotSpotThreadLocalHandshake extends ThreadLocalHandshake {
    ...
    private static final long THREAD_EETOP_OFFSET;
    static {
        try {
            THREAD_EETOP_OFFSET = HotSpotTruffleRuntime.getObjectFieldOffset(Thread.class.getDeclaredField("eetop"));
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }
    ...
    @Override
    public void poll(Node enclosingNode) {
        if (handshakeSupported()) {
            long eetop = UNSAFE.getLong(Thread.currentThread(), THREAD_EETOP_OFFSET);
            if (CompilerDirectives.injectBranchProbability(
                    CompilerDirectives.SLOWPATH_PROBABILITY,
                    UNSAFE.getInt(null, eetop + PENDING_OFFSET) != 0)) {
                processHandshake(enclosingNode);
            }
        }
    }
```

> [!NOTE]
> `eetop` is a private field of `java.lang.Thread` which holds a reference to the corresponding C++ `JavaThread` object:
> ```java
> /*
>  * Reserved for exclusive use by the JVM. Cannot be moved to the FieldHolder
>  * as it needs to be set by the VM for JNI attaching threads, before executing
>  * the constructor that will create the FieldHolder. The historically named
>  * `eetop` holds the address of the underlying VM JavaThread, and is set to
>  * non-zero when the thread is started, and reset to zero when the thread terminates.
>  * A non-zero value indicates this thread isAlive().
>  */
> private volatile long eetop;
> ```

Hitting a safepoint for the [Truffle Profiler](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Profiling/) (i.e. [`CPUSampler`](https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/CPUSampler.html)) in the Truffle interpreter looks as follows:

```java
(gdb) call ps()

"Executing ps"
 for thread: "main" #1 [236585] prio=5 os_prio=0 cpu=39511,65ms elapsed=695,86s tid=0x00007ffff1e94bf0 nid=236585 runnable  [0x00007ffff530c000]
   java.lang.Thread.State: RUNNABLE
Thread: 0x00007ffff1e94bf0  [0x39c29] State: _running _at_poll_safepoint 0
   JavaThread state: _thread_in_vm

	at jdk.internal.misc.Unsafe.getInt(java.base@21.0.5-internal/Native Method)
	at sun.misc.Unsafe.getInt(jdk.unsupported@21.0.5-internal/Unsafe.java:164)
	at com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.poll(org.graalvm.truffle.runtime/HotSpotThreadLocalHandshake.java:78)
	at com.oracle.truffle.api.TruffleSafepoint.poll(org.graalvm.truffle/TruffleSafepoint.java:155)
	at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:747)
	at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
  ...
```
  <details>
     <summary>Full stack trace</summary>

```java
	at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
	at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
	at com.oracle.truffle.runtime.OptimizedCallTarget.callDirect(org.graalvm.truffle.runtime/OptimizedCallTarget.java:535)
	at com.oracle.truffle.runtime.OptimizedDirectCallNode.call(org.graalvm.truffle.runtime/OptimizedDirectCallNode.java:94)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$DirectJSFunctionCacheNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:1361)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeAndSpecialize(org.graalvm.js/JSFunctionCallNode.java:313)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:258)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$CallNode.execute(org.graalvm.js/JSFunctionCallNode.java:545)
	at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute_generic4(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:158)
	at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:76)
	at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.executeVoid(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:364)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
	at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:92)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:85)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:55)
	at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:94)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
	at com.oracle.truffle.js.nodes.function.FunctionBodyNode.execute(org.graalvm.js/FunctionBodyNode.java:73)
	at com.oracle.truffle.js.nodes.function.FunctionRootNode.executeInRealm(org.graalvm.js/FunctionRootNode.java:156)
	at com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode.execute(org.graalvm.js/JavaScriptRealmBoundaryRootNode.java:96)
	at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:746)
	at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
	at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
	at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
	at com.oracle.truffle.runtime.OptimizedCallTarget.callDirect(org.graalvm.truffle.runtime/OptimizedCallTarget.java:535)
	at com.oracle.truffle.runtime.OptimizedDirectCallNode.call(org.graalvm.truffle.runtime/OptimizedDirectCallNode.java:94)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$DirectJSFunctionCacheNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:1361)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeAndSpecialize(org.graalvm.js/JSFunctionCallNode.java:313)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:258)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$CallNode.execute(org.graalvm.js/JSFunctionCallNode.java:545)
	at com.oracle.truffle.js.nodes.binary.JSAddNodeGen.execute_generic4(org.graalvm.js/JSAddNodeGen.java:561)
	at com.oracle.truffle.js.nodes.binary.JSAddNodeGen.execute(org.graalvm.js/JSAddNodeGen.java:379)
	at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute_generic4(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:158)
	at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:76)
	at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.executeVoid(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:364)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
	at com.oracle.truffle.runtime.OptimizedBlockNode.executeVoid(org.graalvm.truffle.runtime/OptimizedBlockNode.java:137)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:70)
	at com.oracle.truffle.js.nodes.control.AbstractRepeatingNode.executeBody(org.graalvm.js/AbstractRepeatingNode.java:67)
	at com.oracle.truffle.js.nodes.control.WhileNode$WhileDoRepeatingNode.executeRepeating(org.graalvm.js/WhileNode.java:238)
	at com.oracle.truffle.api.nodes.RepeatingNode.executeRepeatingWithValue(org.graalvm.truffle/RepeatingNode.java:112)
	at com.oracle.truffle.runtime.OptimizedOSRLoopNode.profilingLoop(org.graalvm.truffle.runtime/OptimizedOSRLoopNode.java:169)
	at com.oracle.truffle.runtime.OptimizedOSRLoopNode.execute(org.graalvm.truffle.runtime/OptimizedOSRLoopNode.java:120)
	at com.oracle.truffle.js.nodes.control.WhileNode.executeVoid(org.graalvm.js/WhileNode.java:181)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
	at com.oracle.truffle.runtime.OptimizedBlockNode.executeVoid(org.graalvm.truffle.runtime/OptimizedBlockNode.java:137)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:70)
	at com.oracle.truffle.js.nodes.function.BlockScopeNode.executeVoid(org.graalvm.js/BlockScopeNode.java:96)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
	at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:92)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:85)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:55)
	at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:94)
	at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
	at com.oracle.truffle.js.nodes.function.FunctionBodyNode.execute(org.graalvm.js/FunctionBodyNode.java:73)
	at com.oracle.truffle.js.nodes.function.FunctionRootNode.executeInRealm(org.graalvm.js/FunctionRootNode.java:156)
	at com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode.execute(org.graalvm.js/JavaScriptRealmBoundaryRootNode.java:96)
	at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:746)
	at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
	at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
	at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
	at com.oracle.truffle.runtime.OptimizedCallTarget.callDirect(org.graalvm.truffle.runtime/OptimizedCallTarget.java:535)
	at com.oracle.truffle.runtime.OptimizedDirectCallNode.call(org.graalvm.truffle.runtime/OptimizedDirectCallNode.java:94)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$DirectJSFunctionCacheNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:1361)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeAndSpecialize(org.graalvm.js/JSFunctionCallNode.java:313)
	at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:258)
	at com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode.doDefault(org.graalvm.js/JSInteropExecuteNode.java:68)
	at com.oracle.truffle.js.nodes.interop.JSInteropExecuteNodeGen.executeAndSpecialize(org.graalvm.js/JSInteropExecuteNodeGen.java:101)
	at com.oracle.truffle.js.nodes.interop.JSInteropExecuteNodeGen.execute(org.graalvm.js/JSInteropExecuteNodeGen.java:82)
	at com.oracle.truffle.js.runtime.interop.InteropBoundFunction.execute(org.graalvm.js/InteropBoundFunction.java:111)
	at com.oracle.truffle.js.runtime.interop.InteropBoundFunctionGen$InteropLibraryExports$Cached.executeNode_AndSpecialize(org.graalvm.js/InteropBoundFunctionGen.java:242)
	at com.oracle.truffle.js.runtime.interop.InteropBoundFunctionGen$InteropLibraryExports$Cached.execute(org.graalvm.js/InteropBoundFunctionGen.java:224)
	at com.oracle.truffle.api.interop.InteropLibraryGen$Delegate.execute(org.graalvm.truffle/InteropLibraryGen.java:3943)
	at com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue$SharedExecuteNode.doDefault(org.graalvm.truffle/PolyglotValueDispatch.java:4539)
	at com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$SharedExecuteNodeGen$Inlined.executeAndSpecialize(org.graalvm.truffle/PolyglotValueDispatchFactory.java:9262)
	at com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$SharedExecuteNodeGen$Inlined.executeShared(org.graalvm.truffle/PolyglotValueDispatchFactory.java:9214)
	at com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue$ExecuteNode.doDefault(org.graalvm.truffle/PolyglotValueDispatch.java:4621)
	at com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$ExecuteNodeGen.executeImpl(org.graalvm.truffle/PolyglotValueDispatchFactory.java:9620)
	at com.oracle.truffle.polyglot.HostToGuestRootNode.execute(org.graalvm.truffle/HostToGuestRootNode.java:124)
	at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:746)
	at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
	at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
	at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
	at com.oracle.truffle.runtime.OptimizedRuntimeSupport.callProfiled(org.graalvm.truffle.runtime/OptimizedRuntimeSupport.java:266)
	at com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue.execute(org.graalvm.truffle/PolyglotValueDispatch.java:2609)
	at org.graalvm.polyglot.Value.execute(org.graalvm.polyglot/Value.java:881)
	at io.simonis.graaljs.test.SimpleCompilation.main(SimpleCompilation.java:75)
```

  </details>

When the safepoint is armed, the thread will instantly invoke the corresponding thread local action. I.e. for the Truffle profiler in our example this means that we will call `SafepointStackSampler$StackVisitor.iterateFrames()`:

```java
(gdb) call ps()

"Executing ps"
 for thread: "main" #1 [236585] prio=5 os_prio=0 cpu=39514,39ms elapsed=731,21s tid=0x00007ffff1e94bf0 nid=236585 runnable  [0x00007ffff530c000]
   java.lang.Thread.State: RUNNABLE
Thread: 0x00007ffff1e94bf0  [0x39c29] State: _running _at_poll_safepoint 0
   JavaThread state: _thread_in_native

	at jdk.vm.ci.hotspot.CompilerToVM.iterateFrames(jdk.internal.vm.ci@21.0.5-internal/Native Method)
	at jdk.vm.ci.hotspot.HotSpotStackIntrospection.iterateFrames(jdk.internal.vm.ci@21.0.5-internal/HotSpotStackIntrospection.java:40)
	at com.oracle.truffle.runtime.OptimizedTruffleRuntime.iterateImpl(org.graalvm.truffle.runtime/OptimizedTruffleRuntime.java:819)
	at com.oracle.truffle.runtime.OptimizedTruffleRuntime.iterateFrames(org.graalvm.truffle.runtime/OptimizedTruffleRuntime.java:703)
	at com.oracle.truffle.api.TruffleRuntime.iterateFrames(org.graalvm.truffle/TruffleRuntime.java:172)
	at com.oracle.truffle.tools.profiler.SafepointStackSampler$StackVisitor.iterateFrames(com.oracle.truffle.tools.profiler/SafepointStackSampler.java:194)
	at com.oracle.truffle.tools.profiler.SafepointStackSampler$SampleAction.perform(com.oracle.truffle.tools.profiler/SafepointStackSampler.java:270)
	at com.oracle.truffle.api.LanguageAccessor$LanguageImpl.performTLAction(org.graalvm.truffle/LanguageAccessor.java:565)
	at com.oracle.truffle.polyglot.PolyglotThreadLocalActions$AsyncEvent.acceptImpl(org.graalvm.truffle/PolyglotThreadLocalActions.java:649)
	at com.oracle.truffle.polyglot.PolyglotThreadLocalActions$AbstractTLHandshake.accept(org.graalvm.truffle/PolyglotThreadLocalActions.java:601)
	at com.oracle.truffle.polyglot.PolyglotThreadLocalActions$AbstractTLHandshake.accept(org.graalvm.truffle/PolyglotThreadLocalActions.java:546)
	at com.oracle.truffle.api.impl.ThreadLocalHandshake$Handshake.perform(org.graalvm.truffle/ThreadLocalHandshake.java:219)
	at com.oracle.truffle.api.impl.ThreadLocalHandshake$TruffleSafepointImpl.processHandshakes(org.graalvm.truffle/ThreadLocalHandshake.java:368)
	at com.oracle.truffle.api.impl.ThreadLocalHandshake.processHandshake(org.graalvm.truffle/ThreadLocalHandshake.java:159)
	at com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.poll(org.graalvm.truffle.runtime/HotSpotThreadLocalHandshake.java:79)
	at com.oracle.truffle.api.TruffleSafepoint.poll(org.graalvm.truffle/TruffleSafepoint.java:155)
  ...
```

Once this code gets partially evaluated and compiled, `HotSpotThreadLocalHandshake::poll()` will be replaced (i.e. *lowered*) by [`HotSpotTruffleSafepointLoweringSnippet::pollSnippet()`]() which will in turn get intrinsified by the Graal compiler (see [`HotSpotTruffleSafepointLoweringSnippet.java`](https://github.com/oracle/graal/blob/master/compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/truffle/hotspot/HotSpotTruffleSafepointLoweringSnippet.java) for the details):
```java
public final class HotSpotTruffleSafepointLoweringSnippet implements Snippets {
    ....
    /**
     * Snippet that does the same as
     * {@code com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.poll()}.
     *
     * This condition cannot be hoisted out of loops as it is introduced in a phase late enough. See
     * {@link TruffleSafepointInsertionPhase}.
     */
    @Snippet
    private static void pollSnippet(Object node, @ConstantParameter int pendingHandshakeOffset) {
        Word thread = CurrentJavaThreadNode.get();
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY,
                        thread.readInt(pendingHandshakeOffset, PENDING_HANDSHAKE_LOCATION) != 0)) {
            foreignPoll(THREAD_LOCAL_HANDSHAKE, node);
        }
    }

    @NodeIntrinsic(value = ForeignCallNode.class)
    private static native void foreignPoll(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object node);
```

In compiled code the safepoint then folds down to a single memory read and a call to the `Stub<HotSpotThreadLocalHandshake.doHandshake(Object)void>` stub in the case `_jvmci_reserved0` is not `0`:
```cpp
     ...
     0x00007fffe496eb20:   cmpl   $0x0,0x4a8(%r15)     // <- Truffle Safepoint poll
                                                       // r15 = JavaThread, 0x4a8 = offsetof (JavaThread, _jvmci_reserved0)
  ╭  0x00007fffe496eb28:   jne    0x00007fffe496eb80
 ↱│  0x00007fffe496eb2e:   mov    0x20(%rsp),%rbp
 ││  0x00007fffe496eb33:   add    $0x28,%rsp
 ││  0x00007fffe496eb37:   nopw   0x0(%rax,%rax,1)
 ││  0x00007fffe496eb40:   cmp    0x448(%r15),%rsp     //   {poll_return} <- HotSpot Safepoint poll
 ││                                                    // r15 = JavaThread, 0x4a8 = offsetof(JavaThread, _poll_data_)
╭││  0x00007fffe496eb47:   ja     0x00007fffe496ec1f
│││  0x00007fffe496eb4d:   vzeroupper
│││  0x00007fffe496eb50:   ret
│││  ...
││⤷  0x00007fffe496eb80:   movabs $0x674005c00,%rsi    //   {oop(a 'io/simonis/graaljs/test/TestLang$TestRootNode'{0x0000000674005c00})}
││   0x00007fffe496eb8a:   call   0x00007fffe496e600   // ImmutableOopMap {rax=Oop [0]=Oop [8]=Oop }
││                                                     //*iload_2 {reexecute=1 rethrow=0 return_oop=0}
││                                                     // - (reexecute) io.simonis.graaljs.test.TestLang$AdditionNode::executeInt@112 (line 146)
││                                                     // - io.simonis.graaljs.test.TestLang$AdditionNode::executeGeneric@12 (line 162)
││                                                     // - io.simonis.graaljs.test.TestLang$TestRootNode::execute@5 (line 287)
││                                                     // - com.oracle.truffle.runtime.OptimizedCallTarget::executeRootNode@5 (line 746)
││                                                     // - com.oracle.truffle.runtime.OptimizedCallTarget::profiledPERoot@46 (line 669)
││                                                     //   {runtime_call Stub<HotSpotThreadLocalHandshake.doHandshake(Object)void>}
││   0x00007fffe496eb8f:   nopl   0x0(%rax,%rax,1)
│╰   0x00007fffe496eb97:   jmp    0x00007fffe496eb2e
│    ...
⤷    0x00007fffe496ec1f:   lea    -0xe6(%rip),%rcx     // #0x00007fffe496eb40 <- address of the safepoint poll
     0x00007fffe496ec26:   mov    %rcx,0x460(%r15)
     0x00007fffe496ec2d:   jmp    0x00007fffe4854400   //   {runtime_call SafepointBlob}
     0x00007fffe496ec32:   hlt
     ...
```

In the case the safepoint is armed, calling the thread local action from the compiled method through the `Stub<HotSpotThreadLocalHandshake.doHandshake(Object)void>` stub looks as follows:

```java
(gdb) call pns($sp, $rbp, $pc)

"Executing pns"
Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
V  [libjvm.so+0xeb4b97]  c2v_iterateFrames(JNIEnv_*, _jobject*, _jobjectArray*, _jobjectArray*, int, _jobject*)+0x56
J 3166  jdk.vm.ci.hotspot.CompilerToVM.iterateFrames([Ljdk/vm/ci/meta/ResolvedJavaMethod;[Ljdk/vm/ci/meta/ResolvedJavaMethod;ILjdk/vm/ci/code/stack/InspectedFrameVisitor;)Ljava/lang/Object; jdk.internal.vm.ci@21.0.5-internal (0 bytes) @ 0x00007fffd9d4195d [0x00007fffd9d41840+0x000000000000011d]
J 3341 c1 jdk.vm.ci.hotspot.HotSpotStackIntrospection.iterateFrames([Ljdk/vm/ci/meta/ResolvedJavaMethod;[Ljdk/vm/ci/meta/ResolvedJavaMethod;ILjdk/vm/ci/code/stack/InspectedFrameVisitor;)Ljava/lang/Object; jdk.internal.vm.ci@21.0.5-internal (20 bytes) @ 0x00007fffd280c10c [0x00007fffd280c020+0x00000000000000ec]
J 3464 c1 com.oracle.truffle.runtime.OptimizedTruffleRuntime.iterateImpl(Lcom/oracle/truffle/api/frame/FrameInstanceVisitor;I)Ljava/lang/Object; org.graalvm.truffle.runtime (38 bytes) @ 0x00007fffd2849f0c [0x00007fffd2849aa0+0x000000000000046c]
J 3337 c1 com.oracle.truffle.tools.profiler.SafepointStackSampler$StackVisitor.iterateFrames()V com.oracle.truffle.tools.profiler (78 bytes) @ 0x00007fffd280a394 [0x00007fffd280a0a0+0x00000000000002f4]
J 3467 c1 com.oracle.truffle.tools.profiler.SafepointStackSampler$SampleAction.perform(Lcom/oracle/truffle/api/ThreadLocalAction$Access;)V com.oracle.truffle.tools.profiler (65 bytes) @ 0x00007fffd284ddec [0x00007fffd284dac0+0x000000000000032c]
J 3465 c1 com.oracle.truffle.polyglot.PolyglotThreadLocalActions$AbstractTLHandshake.accept(Lcom/oracle/truffle/api/nodes/Node;)V org.graalvm.truffle (182 bytes) @ 0x00007fffd284b164 [0x00007fffd284ac20+0x0000000000000544]
J 3342 c1 com.oracle.truffle.polyglot.PolyglotThreadLocalActions$AbstractTLHandshake.accept(Ljava/lang/Object;)V org.graalvm.truffle (9 bytes) @ 0x00007fffd280c6bc [0x00007fffd280c560+0x000000000000015c]
J 2737 c1 com.oracle.truffle.api.impl.ThreadLocalHandshake$Handshake.perform(Lcom/oracle/truffle/api/nodes/Node;)V org.graalvm.truffle (203 bytes) @ 0x00007fffd268b6ec [0x00007fffd268b4e0+0x000000000000020c]
J 3417 c1 com.oracle.truffle.api.impl.ThreadLocalHandshake$TruffleSafepointImpl.processHandshakes(Lcom/oracle/truffle/api/nodes/Node;Ljava/util/List;)V org.graalvm.truffle (91 bytes) @ 0x00007fffd2831904 [0x00007fffd2830ee0+0x0000000000000a24]
J 3418 c1 com.oracle.truffle.api.impl.ThreadLocalHandshake.processHandshake(Lcom/oracle/truffle/api/nodes/Node;)V org.graalvm.truffle (22 bytes) @ 0x00007fffd28336ac [0x00007fffd28333a0+0x000000000000030c]
j  com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.doHandshake(Ljava/lang/Object;)V+29 org.graalvm.truffle.runtime
v  ~StubRoutines::call_stub 0x00007fffd9631d21
V  [libjvm.so+0xd45abf]  JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)+0x607
V  [libjvm.so+0x125cd66]  os::os_exception_wrapper(void (*)(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*), JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)+0x3a
V  [libjvm.so+0xd454b4]  JavaCalls::call(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)+0x3e
V  [libjvm.so+0xf672bf]  JVMCIRuntime::invoke_static_method_one_arg(JavaThread*, Method*, long)+0x26f
v  ~RuntimeStub::Stub<HotSpotThreadLocalHandshake.doHandshake(Object)void> 0x00007fffd9823775
J 3368 jvmci com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.truffle.runtime (57 bytes) @ 0x00007fffd9d852ed [0x00007fffd9d84ce0+0x000000000000060d] (test#2)
J 3423 jvmci com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.truffle.runtime (57 bytes) @ 0x00007fffd9d90c68 [0x00007fffd9d909a0+0x00000000000002c8] (main#1)
J 3001 c1 com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.truffle.runtime (6 bytes) @ 0x00007fffd273ee44 [0x00007fffd273edc0+0x0000000000000084]
...
```

<details>
  <summary>Full stack trace</summary>

```java
J 1827 jvmci com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.truffle.runtime (19 bytes) @ 0x00007fffd9cc9954 [0x00007fffd9cc98c0+0x0000000000000094]
J 3332 c2 com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeCall([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.js (45 bytes) @ 0x00007fffd9d6e2d4 [0x00007fffd9d6dfe0+0x00000000000002f4]
j  com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode.doDefault(Lcom/oracle/truffle/js/runtime/objects/JSDynamicObject;Ljava/lang/Object;[Ljava/lang/Object;Lcom/oracle/truffle/js/nodes/unary/IsCallableNode;Lcom/oracle/truffle/js/nodes/function/JSFunctionCallNode;Lcom/oracle/truffle/js/nodes/interop/ImportValueNode;)Ljava/lang/Object;+30 org.graalvm.js
j  com.oracle.truffle.js.nodes.interop.JSInteropExecuteNodeGen.execute(Lcom/oracle/truffle/js/runtime/objects/JSDynamicObject;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;+54 org.graalvm.js
j  com.oracle.truffle.js.runtime.interop.InteropBoundFunction.execute([Ljava/lang/Object;Lcom/oracle/truffle/api/interop/InteropLibrary;Lcom/oracle/truffle/js/nodes/interop/JSInteropExecuteNode;Lcom/oracle/truffle/js/nodes/interop/ExportValueNode;)Ljava/lang/Object;+29 org.graalvm.js
j  com.oracle.truffle.js.runtime.interop.InteropBoundFunctionGen$InteropLibraryExports$Cached.execute(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;+101 org.graalvm.js
j  com.oracle.truffle.api.interop.InteropLibraryGen$Delegate.execute(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;+43 org.graalvm.truffle
j  com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue$SharedExecuteNode.doDefault(Lcom/oracle/truffle/api/nodes/Node;Lcom/oracle/truffle/polyglot/PolyglotLanguageContext;Ljava/lang/Object;[Ljava/lang/Object;Lcom/oracle/truffle/api/interop/InteropLibrary;Lcom/oracle/truffle/polyglot/PolyglotLanguageContext$ToGuestValuesNode;Lcom/oracle/truffle/polyglot/PolyglotLanguageContext$ToHostValueNode;Lcom/oracle/truffle/api/profiles/InlinedBranchProfile;Lcom/oracle/truffle/api/profiles/InlinedBranchProfile;Lcom/oracle/truffle/api/profiles/InlinedBranchProfile;)Ljava/lang/Object;+15 org.graalvm.truffle
j  com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$SharedExecuteNodeGen$Inlined.executeShared(Lcom/oracle/truffle/api/nodes/Node;Lcom/oracle/truffle/polyglot/PolyglotLanguageContext;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;+85 org.graalvm.truffle
j  com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue$ExecuteNode.doDefault(Lcom/oracle/truffle/polyglot/PolyglotLanguageContext;Ljava/lang/Object;[Ljava/lang/Object;Lcom/oracle/truffle/polyglot/PolyglotLanguageContext$ToHostValueNode;Lcom/oracle/truffle/polyglot/PolyglotValueDispatch$InteropValue$SharedExecuteNode;)Ljava/lang/Object;+15 org.graalvm.truffle
j  com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$ExecuteNodeGen.executeImpl(Lcom/oracle/truffle/polyglot/PolyglotLanguageContext;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;+10 org.graalvm.truffle
j  com.oracle.truffle.polyglot.HostToGuestRootNode.execute(Lcom/oracle/truffle/api/frame/VirtualFrame;)Ljava/lang/Object;+239 org.graalvm.truffle
J 3040 c1 com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(Lcom/oracle/truffle/api/frame/VirtualFrame;Lcom/oracle/truffle/runtime/CompilationState;)Ljava/lang/Object; org.graalvm.truffle.runtime (95 bytes) @ 0x00007fffd2758ccc [0x00007fffd2758b80+0x000000000000014c]
J 3005 c1 com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.truffle.runtime (57 bytes) @ 0x00007fffd27414ec [0x00007fffd2741260+0x000000000000028c]
J 1827 jvmci com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.truffle.runtime (19 bytes) @ 0x00007fffd9cc9980 [0x00007fffd9cc98c0+0x00000000000000c0]
J 3001 c1 com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke([Ljava/lang/Object;)Ljava/lang/Object; org.graalvm.truffle.runtime (6 bytes) @ 0x00007fffd273ee44 [0x00007fffd273edc0+0x0000000000000084]
j  com.oracle.truffle.runtime.OptimizedRuntimeSupport.callProfiled(Lcom/oracle/truffle/api/CallTarget;[Ljava/lang/Object;)Ljava/lang/Object;+31 org.graalvm.truffle.runtime
j  com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue.execute(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;+23 org.graalvm.truffle
j  org.graalvm.polyglot.Value.execute([Ljava/lang/Object;)Lorg/graalvm/polyglot/Value;+37 org.graalvm.polyglot
j  io.simonis.graaljs.test.SimpleCompilation.main([Ljava/lang/String;)V+218
v  ~StubRoutines::call_stub 0x00007fffd9631d21
V  [libjvm.so+0xd45abf]  JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)+0x607
V  [libjvm.so+0x125cd66]  os::os_exception_wrapper(void (*)(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*), JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)+0x3a
V  [libjvm.so+0xd454b4]  JavaCalls::call(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)+0x3e
V  [libjvm.so+0xe219ac]  jni_invoke_static(JNIEnv_*, JavaValue*, _jobject*, JNICallType, _jmethodID*, JNI_ArgumentPusher*, JavaThread*)+0x1a6
V  [libjvm.so+0xe301bd]  jni_CallStaticVoidMethod+0x1e6
C  [libjli.so+0x6411]  JavaMain+0xff4
C  [libjli.so+0xcd38]  ThreadJavaMain+0x2b
```

</details>

Notice that the method with the compilation id `3368` is a "*hosted*" JVMCI compilation. i.e. it is the partially evaluated and natively compiled version of the Java script function `test()` (see the very end of the stack frame line for the compilation id `3368` where "`(test#2)`" denotes the guest languange function name and the Truffle/Graal compilation level).

##### Disarming Safepoints

Safepoints are disarmed right after the `ThreadLocalAction` was executed:

```java
(gdb) call ps()

"Executing ps"
 for thread: "main" #1 [310790] prio=5 os_prio=0 cpu=36441,29ms elapsed=1328,84s tid=0x00007ffff1e94e70 nid=310790 runnable  [0x00007ffff52ac000]
   java.lang.Thread.State: RUNNABLE
Thread: 0x00007ffff1e94e70  [0x4be06] State: _running _at_poll_safepoint 0
   JavaThread state: _thread_in_vm

     at jdk.internal.misc.Unsafe.putIntVolatile(java.base@21.0.5-internal/Native Method)
     at sun.misc.Unsafe.putIntVolatile(jdk.unsupported@21.0.5-internal/Unsafe.java:968)
     at com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.setVolatile(org.graalvm.truffle.runtime/HotSpotThreadLocalHandshake.java:120)
     at com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.clearFastPending(org.graalvm.truffle.runtime/HotSpotThreadLocalHandshake.java:107)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake$TruffleSafepointImpl.resetPending(org.graalvm.truffle/ThreadLocalHandshake.java:514)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake$TruffleSafepointImpl.processHandshakes(org.graalvm.truffle/ThreadLocalHandshake.java:375)
     at com.oracle.truffle.api.impl.ThreadLocalHandshake.processHandshake(org.graalvm.truffle/ThreadLocalHandshake.java:159)
     at com.oracle.truffle.runtime.hotspot.HotSpotThreadLocalHandshake.poll(org.graalvm.truffle.runtime/HotSpotThreadLocalHandshake.java:79)
     at com.oracle.truffle.api.TruffleSafepoint.poll(org.graalvm.truffle/TruffleSafepoint.java:155)
     ...
```
<details>
  <summary>Full stack trace</summary>

```java
     at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:747)
     at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
     at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
     at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
     at com.oracle.truffle.runtime.OptimizedCallTarget.callDirect(org.graalvm.truffle.runtime/OptimizedCallTarget.java:535)
     at com.oracle.truffle.runtime.OptimizedDirectCallNode.call(org.graalvm.truffle.runtime/OptimizedDirectCallNode.java:94)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$DirectJSFunctionCacheNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:1361)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeAndSpecialize(org.graalvm.js/JSFunctionCallNode.java:313)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:258)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$InvokeNode.execute(org.graalvm.js/JSFunctionCallNode.java:746)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$CallNNode.executeFillObjectArray(org.graalvm.js/JSFunctionCallNode.java:664)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$CallNNode.createArguments(org.graalvm.js/JSFunctionCallNode.java:657)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$CallNode.execute(org.graalvm.js/JSFunctionCallNode.java:545)
     at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute_generic4(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:158)
     at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:76)
     at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.executeVoid(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:364)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
     at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:92)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:85)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:55)
     at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:94)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
     at com.oracle.truffle.js.nodes.function.FunctionBodyNode.execute(org.graalvm.js/FunctionBodyNode.java:73)
     at com.oracle.truffle.js.nodes.function.FunctionRootNode.executeInRealm(org.graalvm.js/FunctionRootNode.java:156)
     at com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode.execute(org.graalvm.js/JavaScriptRealmBoundaryRootNode.java:96)
     at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:746)
     at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
     at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
     at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
     at com.oracle.truffle.runtime.OptimizedCallTarget.callDirect(org.graalvm.truffle.runtime/OptimizedCallTarget.java:535)
     at com.oracle.truffle.runtime.OptimizedDirectCallNode.call(org.graalvm.truffle.runtime/OptimizedDirectCallNode.java:94)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$DirectJSFunctionCacheNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:1361)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeAndSpecialize(org.graalvm.js/JSFunctionCallNode.java:313)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:258)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$CallNode.execute(org.graalvm.js/JSFunctionCallNode.java:545)
     at com.oracle.truffle.js.nodes.binary.JSAddNodeGen.execute_generic4(org.graalvm.js/JSAddNodeGen.java:561)
     at com.oracle.truffle.js.nodes.binary.JSAddNodeGen.execute(org.graalvm.js/JSAddNodeGen.java:379)
     at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute_generic4(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:158)
     at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.execute(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:76)
     at com.oracle.truffle.js.nodes.access.JSWriteCurrentFrameSlotNodeGen.executeVoid(org.graalvm.js/JSWriteCurrentFrameSlotNodeGen.java:364)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
     at com.oracle.truffle.runtime.OptimizedBlockNode.executeVoid(org.graalvm.truffle.runtime/OptimizedBlockNode.java:137)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:70)
     at com.oracle.truffle.js.nodes.control.AbstractRepeatingNode.executeBody(org.graalvm.js/AbstractRepeatingNode.java:67)
     at com.oracle.truffle.js.nodes.control.WhileNode$WhileDoRepeatingNode.executeRepeating(org.graalvm.js/WhileNode.java:238)
     at com.oracle.truffle.api.nodes.RepeatingNode.executeRepeatingWithValue(org.graalvm.truffle/RepeatingNode.java:112)
     at com.oracle.truffle.runtime.OptimizedOSRLoopNode.profilingLoop(org.graalvm.truffle.runtime/OptimizedOSRLoopNode.java:169)
     at com.oracle.truffle.runtime.OptimizedOSRLoopNode.execute(org.graalvm.truffle.runtime/OptimizedOSRLoopNode.java:120)
     at com.oracle.truffle.js.nodes.control.WhileNode.executeVoid(org.graalvm.js/WhileNode.java:181)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
     at com.oracle.truffle.runtime.OptimizedBlockNode.executeVoid(org.graalvm.truffle.runtime/OptimizedBlockNode.java:137)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:70)
     at com.oracle.truffle.js.nodes.function.BlockScopeNode.executeVoid(org.graalvm.js/BlockScopeNode.java:96)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:80)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeVoid(org.graalvm.js/AbstractBlockNode.java:55)
     at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:92)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:85)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.executeGeneric(org.graalvm.js/AbstractBlockNode.java:55)
     at com.oracle.truffle.runtime.OptimizedBlockNode.executeGeneric(org.graalvm.truffle.runtime/OptimizedBlockNode.java:94)
     at com.oracle.truffle.js.nodes.control.AbstractBlockNode.execute(org.graalvm.js/AbstractBlockNode.java:75)
     at com.oracle.truffle.js.nodes.function.FunctionBodyNode.execute(org.graalvm.js/FunctionBodyNode.java:73)
     at com.oracle.truffle.js.nodes.function.FunctionRootNode.executeInRealm(org.graalvm.js/FunctionRootNode.java:156)
     at com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode.execute(org.graalvm.js/JavaScriptRealmBoundaryRootNode.java:96)
     at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:746)
     at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
     at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
     at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
     at com.oracle.truffle.runtime.OptimizedCallTarget.callDirect(org.graalvm.truffle.runtime/OptimizedCallTarget.java:535)
     at com.oracle.truffle.runtime.OptimizedDirectCallNode.call(org.graalvm.truffle.runtime/OptimizedDirectCallNode.java:94)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode$DirectJSFunctionCacheNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:1361)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeAndSpecialize(org.graalvm.js/JSFunctionCallNode.java:313)
     at com.oracle.truffle.js.nodes.function.JSFunctionCallNode.executeCall(org.graalvm.js/JSFunctionCallNode.java:258)
     at com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode.doDefault(org.graalvm.js/JSInteropExecuteNode.java:68)
     at com.oracle.truffle.js.nodes.interop.JSInteropExecuteNodeGen.executeAndSpecialize(org.graalvm.js/JSInteropExecuteNodeGen.java:101)
     at com.oracle.truffle.js.nodes.interop.JSInteropExecuteNodeGen.execute(org.graalvm.js/JSInteropExecuteNodeGen.java:82)
     at com.oracle.truffle.js.runtime.interop.InteropBoundFunction.execute(org.graalvm.js/InteropBoundFunction.java:111)
     at com.oracle.truffle.js.runtime.interop.InteropBoundFunctionGen$InteropLibraryExports$Cached.executeNode_AndSpecialize(org.graalvm.js/InteropBoundFunctionGen.java:242)
     at com.oracle.truffle.js.runtime.interop.InteropBoundFunctionGen$InteropLibraryExports$Cached.execute(org.graalvm.js/InteropBoundFunctionGen.java:224)
     at com.oracle.truffle.api.interop.InteropLibraryGen$Delegate.execute(org.graalvm.truffle/InteropLibraryGen.java:3943)
     at com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue$SharedExecuteNode.doDefault(org.graalvm.truffle/PolyglotValueDispatch.java:4539)
     at com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$SharedExecuteNodeGen$Inlined.executeAndSpecialize(org.graalvm.truffle/PolyglotValueDispatchFactory.java:9262)
     at com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$SharedExecuteNodeGen$Inlined.executeShared(org.graalvm.truffle/PolyglotValueDispatchFactory.java:9214)
     at com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue$ExecuteNode.doDefault(org.graalvm.truffle/PolyglotValueDispatch.java:4621)
     at com.oracle.truffle.polyglot.PolyglotValueDispatchFactory$InteropValueFactory$ExecuteNodeGen.executeImpl(org.graalvm.truffle/PolyglotValueDispatchFactory.java:9620)
     at com.oracle.truffle.polyglot.HostToGuestRootNode.execute(org.graalvm.truffle/HostToGuestRootNode.java:124)
     at com.oracle.truffle.runtime.OptimizedCallTarget.executeRootNode(org.graalvm.truffle.runtime/OptimizedCallTarget.java:746)
     at com.oracle.truffle.runtime.OptimizedCallTarget.profiledPERoot(org.graalvm.truffle.runtime/OptimizedCallTarget.java:669)
     at com.oracle.truffle.runtime.OptimizedCallTarget.callBoundary(org.graalvm.truffle.runtime/OptimizedCallTarget.java:602)
     at com.oracle.truffle.runtime.OptimizedCallTarget.doInvoke(org.graalvm.truffle.runtime/OptimizedCallTarget.java:586)
     at com.oracle.truffle.runtime.OptimizedRuntimeSupport.callProfiled(org.graalvm.truffle.runtime/OptimizedRuntimeSupport.java:266)
     at com.oracle.truffle.polyglot.PolyglotValueDispatch$InteropValue.execute(org.graalvm.truffle/PolyglotValueDispatch.java:2609)
     at org.graalvm.polyglot.Value.execute(org.graalvm.polyglot/Value.java:881)
     at io.simonis.graaljs.test.SimpleCompilation.main(SimpleCompilation.java:75)
```
</details>


#### Bibliography
- [HotSpot JVM Deep Dive - Safepoint](https://www.youtube.com/watch?v=JkbWPPNc4SI)
- [HotSpot Internals: Signals, Safepoints and NullPointers](https://player.vimeo.com/video/221265923) ([slides](https://simonis.github.io/hotspot_internals/hotspot_internals.xhtml))
- [JVM Anatomy Quark #22: Safepoint Polls](https://shipilev.net/jvm/anatomy-quarks/22-safepoint-polls/)
- [Time to Safepoint Demo](https://player.vimeo.com/video/274560648#t=18m38s)