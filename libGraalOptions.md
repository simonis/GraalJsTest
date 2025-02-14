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

## Graal Compiler option handling

The Graal compiler has a sophisticated way of declaring and managing options. The corresponding classes are located in the `jdk.graal.compiler.options` package which is also used by SubstrateVM.

```java
package jdk.graal.compiler.options;

/**
 * A key for an option. The value for an option is obtained from an {@link OptionValues} object.
 */
public class OptionKey<T> {
    private final T defaultValue;
    ...
    public OptionKey(T defaultValue) {
        this.defaultValue = defaultValue;
    }}

/**
 * Describes the attributes of a static field {@linkplain Option option} and provides access to its
 * {@linkplain OptionKey value}.
 */
public final class OptionDescriptor {

    private final String name;
    private final OptionType optionType;
    private final Class<?> optionValueType;
    private final String help;
    private final List<String> extraHelp;
    private final OptionKey<?> optionKey;
    private final Class<?> declaringClass;
    private final String fieldName;
    private final OptionStability stability;
    private final boolean deprecated;
    private final String deprecationMessage;
    ...
}

public interface OptionDescriptors extends Iterable<OptionDescriptor> {
    /**
     * Gets the {@link OptionDescriptor} matching a given option name or {@code null} if this option
     * descriptor set doesn't contain a matching option name.
     */
    OptionDescriptor get(String value);
}
```
With the `Option`/`OptionGroup` annotations, it becomes very easy to define and group new options as static fields of arbitrary classes.

```java
/**
 * Describes the attributes of an option whose {@link OptionKey value} is in a static field
 * annotated by this annotation type.
 *
 * @see OptionDescriptor
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Option {
    ...
}
```

E.g. many Graal Compiler options are defined in the [`GraalCompilerOptions`](https://github.com/oracle/graal/blob/4c10155e7b932e5cf636501dd90eba20630c284a/compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/core/GraalCompilerOptions.java) class as follows:

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

When the Graal Compiler is built, these `Option` annotations will be processed by the Graal Compiler [`OptionProcessor`](https://github.com/oracle/graal/blob/4c10155e7b932e5cf636501dd90eba20630c284a/compiler/src/jdk.graal.compiler.processor/src/jdk/graal/compiler/options/processor/OptionProcessor.java):

```java
package jdk.graal.compiler.options.processor;
/**
 * Processes static fields annotated with {@code Option}. An {@code OptionDescriptors}
 * implementation is generated for each top level class containing at least one such field. The name
 * of the generated class for top level class {@code com.foo.Bar} is
 * {@code com.foo.Bar_OptionDescriptors}.
 */
@SupportedAnnotationTypes({"jdk.graal.compiler.options.Option"})
public class OptionProcessor extends AbstractProcessor {
    ...
}
```
which is registered in the [META-INF/services/javax.annotation.processing.Processor](https://github.com/oracle/graal/blob/4c10155e7b932e5cf636501dd90eba20630c284a/compiler/src/jdk.graal.compiler.processor/src/META-INF/services/javax.annotation.processing.Processor#L6C1-L6C53) of `graal-processor.jar`. For the `GraalCompilerOptions` example above, it will create the following `GraalCompilerOptions_OptionDescriptors` class which implements `OptionDescriptors`:

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
                /*optionKey*/ GraalCompilerOptions.PrintCompilation,
                /*stability*/ OptionStability.STABLE,
                /*deprecated*/ false,
                /*deprecationMessage*/ "");
        }
        ...
        }
    }
    @Override
    public Iterator<OptionDescriptor> iterator() {
        return new Iterator<>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < 7;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    ...
                    case 5: return get("PrintCompilation");
                    case 6: return get("SystemicCompilationFailureRate");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
```

The build system (in [compiler/mx.compiler/mx_compiler.py](https://github.com/oracle/graal/blob/4c10155e7b932e5cf636501dd90eba20630c284a/compiler/mx.compiler/mx_compiler.py#L1296-L1312)):

```python
        elif arcname.endswith('_OptionDescriptors.class'):
            ...
                # Need to create service files for the providers of the
                # jdk.internal.vm.ci.options.Options service created by
                # jdk.internal.vm.ci.options.processor.OptionProcessor.
                provider = arcname[:-len('.class'):].replace('/', '.')
                service = 'jdk.graal.compiler.options.OptionDescriptors'
                add_serviceprovider(service, provider, version)
```

Will take care, to register all the `OptionDescriptors` in `META-INF/services/jdk.graal.compiler.options.OptionDescriptors`:

```java
jdk.graal.compiler.core.GraalCompilerOptions_OptionDescriptors
...
```

and the `module-info.class` file:

```java
    provides jdk.graal.compiler.options.OptionDescriptors with jdk.graal.compiler.core.GraalCompilerOptions_OptionDescriptors, ...
```


```java
    /**
     * Gets an iterable of available {@link OptionDescriptors}.
     */
    @ExcludeFromJacocoGeneratedReport("contains libgraal-only path")
    public static Iterable<OptionDescriptors> getOptionsLoader() {
        if (IS_IN_NATIVE_IMAGE) {
            System.out.println("===> getOptionsLoader()");
            System.out.println("     " + (libgraalOptions == null ? "null" : "libgraalOptions"));
            System.out.println("     " + OptionsParser.class.getClassLoader());
            new Throwable().printStackTrace(System.out);
            return List.of(new OptionDescriptorsMap(Objects.requireNonNull(libgraalOptions.descriptors, "missing options")));
        }
        boolean inLibGraal = libgraalOptions != null;
        if (inLibGraal && IS_BUILDING_NATIVE_IMAGE) {
            /*
             * Graal code is being run in the context of the LibGraalClassLoader while building
             * libgraal so use the LibGraalClassLoader to load the OptionDescriptors.
             */
            ClassLoader myCL = OptionsParser.class.getClassLoader();
            return ServiceLoader.load(OptionDescriptors.class, myCL);
        } else {
            /*
             * The Graal module (i.e., jdk.graal.compiler) is loaded by the platform class loader.
             * Modules that depend on and extend Graal are loaded by the app class loader so use it
             * (instead of the platform class loader) to load the OptionDescriptors.
             */
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            return ServiceLoader.load(OptionDescriptors.class, loader);
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

## GraalVM SDK / Polyglot / Truffle option handling

The GraalVM SDK provides a [public API for option handling](http://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/Option.html) (in the `org.graalvm.options` package) which is similar to the GraalVM options implementation:

```java
package org.graalvm.options;

/**
 * Represents the option key for an option specification.
 */
public final class OptionKey<T> {

    private final OptionType<T> type;
    private final T defaultValue;
    ...
}
/**
 * Represents metadata for a single option.
 */
public final class OptionDescriptor {

    private final OptionKey<?> key;
    private final String name;
    private final String help;
    private final OptionCategory category;
    private final OptionStability stability;
    private final boolean deprecated;
    private final String deprecationMessage;
    private final String usageSyntax;

    OptionDescriptor(OptionKey<?> key, String name, String help, OptionCategory category, OptionStability stability, boolean deprecated, String deprecationMessage, String usageSyntax) {
        ...
    }
    ...
}
/**
 * An interface to a set of {@link OptionDescriptor}s.
 */
public interface OptionDescriptors extends Iterable<OptionDescriptor> {
    ...
}
```

The Truffle framework extends the GraalVM SDK option package with [`Option`](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/Option.html)/[`Option.Group](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/Option.Group.html) annotations which are similar to their `jdk.graal.compiler.options.Option`/`jdk.graal.compiler.options.OptionGroup` counterparts in the Graal Compiler.

with these classes and annotations, the Truffle framework declares its own options e.g. as follows:

```java
package com.oracle.truffle.runtime;

/**
 * Truffle compilation options that can be configured per {@link Engine engine} instance. These
 * options are accessed by the Truffle runtime and not the Truffle compiler, unlike
 * jdk.graal.compiler.truffle.TruffleCompilerOptions
 */
@Option.Group("engine")
public final class OptimizedRuntimeOptions {
    ...
    @Option(help = "Enable asynchronous truffle compilation in background threads (default: true)", usageSyntax = "true|false", category = OptionCategory.EXPERT) //
    public static final OptionKey<Boolean> BackgroundCompilation = new OptionKey<>(true);
    ...
    public static OptionDescriptors getDescriptors() {
        return new OptimizedRuntimeOptionsOptionDescriptors();
    }
```

Notice that the class `OptimizedRuntimeOptionsOptionDescriptors` is generated at build time from the field names and `Option`/`Option.Group` annotations ([`TruffleOptionDescriptors`](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/TruffleOptionDescriptors.html) extends [`OptionDescriptors`](https://www.graalvm.org/truffle/javadoc/org/graalvm/options/OptionDescriptors.html)):

```java
// CheckStyle: start generated
package com.oracle.truffle.runtime;
...
@GeneratedBy(OptimizedRuntimeOptions.class)
final class OptimizedRuntimeOptionsOptionDescriptors implements TruffleOptionDescriptors {

    @Override
    public OptionDescriptor get(String optionName) {
        switch (optionName) {
            ...
            case "engine.BackgroundCompilation" :
                return OptionDescriptor.newBuilder(OptimizedRuntimeOptions.BackgroundCompilation, "engine.BackgroundCompilation").deprecated(false).help("Enable asynchronous truffle compilation in background threads (default: true)").usageSyntax("true|false").category(OptionCategory.EXPERT).stability(OptionStability.EXPERIMENTAL).build();
            ...
        }
    }

    @Override
    public Iterator<OptionDescriptor> iterator() {
        return List.of(
            ...
            OptionDescriptor.newBuilder(OptimizedRuntimeOptions.BackgroundCompilation, "engine.BackgroundCompilation").deprecated(false).help("Enable asynchronous truffle compilation in background threads (default: true)").usageSyntax("true|false").category(OptionCategory.EXPERT).stability(OptionStability.EXPERIMENTAL).build(),
            ...)
        .iterator();
    }
}
```


