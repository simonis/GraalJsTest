# libgraal options internals

The available libgraal options are collected at build time (i.e. when running the `native-image` tool) and triggered by the `LibGraalFeature` [Feature](https://docs.oracle.com/en/graalvm/enterprise/21/sdk/org/graalvm/nativeimage/hosted/Feature.html) class:

```java
public final class LibGraalFeature implements Feature {
    static class Options {
        @Option(help = "The value of the java.home system property reported by the Java " +
                        "installation that includes the Graal classes in its runtime image " +
                        "from which libgraal will be built. If not provided, the java.home " +
                        "of the Java installation running native-image will be used.") //
        public static final HostedOptionKey<Path> LibGraalJavaHome = new HostedOptionKey<>(Path.of(System.getProperty("java.home")));
    }
    /**
     * Loader used for loading classes from the guest GraalVM.
     */
    LibGraalClassLoader loader;
    /**
     * Handle to {@link BuildTime} in the guest.
     */
    Class<?> buildTimeClass;

    public void afterRegistration(AfterRegistrationAccess access) {
        loader = new LibGraalClassLoader(Options.LibGraalJavaHome.getValue().resolve(Path.of("lib", "modules")));
        buildTimeClass = loader.loadClassOrFail("jdk.graal.compiler.hotspot.libgraal.BuildTime");
    }

    public void duringSetup(DuringSetupAccess access) {
        optionCollector = new OptionCollector(LibGraalEntryPoints.vmOptionDescriptors);
        accessImpl.registerObjectReachableCallback(OptionKey.class, optionCollector::doCallback);
        accessImpl.registerObjectReachableCallback(loader.loadClassOrFail(OptionKey.class.getName()), optionCollector::doCallback);
    }
    /**
     * Collects all options that are reachable at run time. Reachable options are the
     * {@link OptionKey} instances reached by the static analysis. The VM options are instances of
     * {@link OptionKey} loaded by the {@link com.oracle.svm.hosted.NativeImageClassLoader} and
     * compiler options are instances of {@link OptionKey} loaded by the
     * {@link LibGraalClassLoader}.
     */
    private class OptionCollector implements ObjectReachableCallback<Object> {
        private final Set<Object> options = Collections.newSetFromMap(new ConcurrentHashMap<>());
        /**
         * Libgraal VM options.
         */
        private final EconomicMap<String, OptionDescriptor> vmOptionDescriptors;

        /**
         * Libgraal compiler options info.
         */
        private final Object compilerOptionsInfo;

        private boolean sealed;

        OptionCollector(EconomicMap<String, OptionDescriptor> vmOptionDescriptors) {
            this.vmOptionDescriptors = vmOptionDescriptors;
                MethodHandle mh = mhl.findStatic(buildTimeClass, "initLibgraalOptions", mt);
                compilerOptionsInfo = mh.invoke();
        }
        public void doCallback(DuringAnalysisAccess access, Object option, ObjectScanner.ScanReason reason) {
                options.add(option);
        }

    }
}
```

The `LibGraalClassLoader` is the class loader which loads the Graal compiler class of the Graal version which is about to be compiled into a native image (i.e. "libgraal" or `libjvmcicompiler.so` in our case). It is created after the registration of the `LibGraalFeature` class. During setup of the `LibGraalFeature` class, callbacks for reachable objects of type [`OptionKey`](https://docs.oracle.com/en/graalvm/enterprise/21/sdk/org/graalvm/options/OptionKey.html) (or derived types) are registered (N.B. - all Graal options are derived from `OptionKey`). Notice that Graal compiler options will be loaded by the `LibGraalClassLoader` whereas SubstrateVM options will be loaded by the `NativeImageClassLoader` (according to the API-doc, but in reality the SubstrateVM options get loaded by the `jdk.internal.loader.ClassLoaders$AppClassLoader` - this might be because the `NativeImageClassLoader` delegates to it?).

After the reachability analysis has finished, `LibGraalFeature::afterAnalysis()` calls `OptionCollector::afterAnalysis()` which puts all SubstrateVM options into `vmOptionDescriptors` and calls `BuildTime::finalizeLibgraalOptions()` with all the Graal compiler options:

```java
void afterAnalysis(AfterAnalysisAccess access) {
    sealed = true;
    List<Object> compilerOptions = new ArrayList<>(options.size());
    for (Object option : options) {
        if (option instanceof OptionKey<?> optionKey) {
            OptionDescriptor descriptor = optionKey.getDescriptor();
            if (descriptor.isServiceLoaded()) {
                vmOptionDescriptors.put(optionKey.getName(), descriptor);
            }
        } else {
            compilerOptions.add(option);
        }
    }

        MethodHandle mh = mhl.findStatic(buildTimeClass, "finalizeLibgraalOptions", mt);
        Map<String, String> modules = loader.getModules();
        Iterable<Object> values = (Iterable<Object>) mh.invoke(compilerOptions, compilerOptionsInfo, modules);
}
```

```java
/**
 * Options related to {@link GraalCompiler}.
 */
public class GraalCompilerOptions {
    ...
    @Option(help = "Print an informational line to the console for each completed compilation.", type = OptionType.Debug, stability = OptionStability.STABLE)
    public static final OptionKey<Boolean> PrintCompilation = new OptionKey<>(false);
    ...
}
```

```java
public class GraalCompilerOptions_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        case "PrintCompilation": {
            return OptionDescriptor.create(
                /*name*/ "PrintCompilation",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Print an informational line to the console for each completed compilation.",
                /*declaringClass*/ GraalCompilerOptions.class,
                /*fieldName*/ "PrintCompilation",
                /*option*/ GraalCompilerOptions.PrintCompilation,
                /*stability*/ OptionStability.STABLE,
                /*deprecated*/ false,
                /*deprecationMessage*/ "");
        }
        ...
        }
    }

```


```java
@OptionGroup(prefix = "compiler.", registerAsService = false)
public class TruffleCompilerOptions {
    ...
    @Option(help = "Maximum depth for recursive inlining (default: 2, usage: [0, inf)).", type = OptionType.Expert) //
    public static final OptionKey<Integer> InliningRecursionDepth = new OptionKey<>(2);
    ...
}
```