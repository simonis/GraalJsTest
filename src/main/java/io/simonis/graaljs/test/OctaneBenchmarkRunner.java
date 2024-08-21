package io.simonis.graaljs.test;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class OctaneBenchmarkRunner {

  private static final String OCTANE = "resources/octane";

  public static void main(String[] args) throws Exception {

    // Want to see the actual runtime but don't want to make it a compile-time dependency.
    System.out.println(Class.forName("com.oracle.truffle.api.Truffle").getMethod("getRuntime").invoke(null));

    URL octaneUrl = OctaneBenchmarkRunner.class.getClassLoader().getResource(OCTANE);
    String[] octaneUrlParts = octaneUrl.toString().split("!");
    FileSystem fs;
    Path octanePath;
    if (octaneUrlParts.length == 2) {
      // If we're running from a jar file
      fs = FileSystems.newFileSystem(URI.create(octaneUrlParts[0]), new HashMap<String, String>());
      octanePath = fs.getPath(octaneUrlParts[1]).toAbsolutePath();
    } else {
      // If we're running from an exploded build
      assert octaneUrlParts.length == 1;
      fs = FileSystems.getDefault();
      octanePath = fs.getPath(octaneUrlParts[0]).toAbsolutePath();
    }
    if (args.length == 0) {
      args = new String[] { "richards.js",    "deltablue.js",   "crypto.js",           "raytrace.js", "earley-boyer.js",
                            "regexp.js",      "splay.js",       "navier-stokes.js",    "pdfjs.js",    "mandreel.js",
                            "gbemu-part1.js", "gbemu-part2.js", "code-load.js",        "box2d.js",    "zlib.js",
                            "zlib-data.js",   "typescript.js",  "typescript-input.js", "typescript-compiler.js"};
    }
    String octaneCustomJS = "octane_custom.js";
    System.out.println("All the Octane benchmark files get concatenated into the single");
    System.out.println("virtual file \"" + octaneCustomJS + "\" starting at line:");
    StringBuilder benchmarks = new StringBuilder();
    int line = 1;
    System.out.println(line + " : base.js");
    String base = Files.readString(octanePath.resolve("base.js"));
    line += base.lines().count();
    benchmarks.append(base);
    for (String benchmark : args) {
      System.out.println(line + " : " + benchmark);
      String src = Files.readString(octanePath.resolve(benchmark));
      switch(benchmark) {
        case "gbemu-part1.js" :
          // Make 'Gameboy' reentrant
          src = src.replace("""
                            function tearDownGameboy() {
                              decoded_gameboy_rom = null;
                              expectedGameboyStateStr = null;
                            }
                            """,
                            """
                            function tearDownGameboy() {
                              decoded_gameboy_rom = null;
                            }
                            """);
          break;
        case "box2d.js" :
          // Make 'Box2D' reentrant
          src = src.replace("""
                            function tearDownBox2D() {
                              world = null;
                              Box2D = null;
                            }
                            """,
                            """
                            function tearDownBox2D() {
                              world = null;
                            }
                            """);
          break;
        case "typescript.js" :
          // Make 'Typescript' reentrant
          src = src.replace("""
                            function tearDownTypescript() {
                              compiler_input = null;
                            }
                            """,
                            """
                            function tearDownTypescript() {
                            }
                            """);
          break;
      }
      line += src.lines().count();
      benchmarks.append(src);
    }
    System.out.println(line + " : run.js");
    benchmarks.append(// From "run.js":
"""
var success = true;

function PrintResult(name, result) {
  print(name + ': ' + result);
}


function PrintError(name, error) {
  PrintResult(name, error.stack);
  success = false;
}


function PrintScore(score) {
  if (success) {
    print('----');
    print('Score (version ' + BenchmarkSuite.version + '): ' + score);
  }
}

// Try to be as reproducible as possible
BenchmarkSuite.config.doWarmup = true;
BenchmarkSuite.config.doDeterministic = true;

var runner = { NotifyResult: PrintResult,
               NotifyError: PrintError,
               NotifyScore: PrintScore };

function runOctane() {
  // We currently don't run 'mandreel.js' because it is not reentrant (i.e.
  // if executed repeatedly, it fails with 'RangeError: Maximum call stack size exceeded'.
  BenchmarkSuite.RunSuites(runner, ['Mandreel']);
}
"""
                      );
    Source runOctane = Source.newBuilder("js", benchmarks, octaneCustomJS).build();

    int iterations = Integer.getInteger("iterations", 1);

    try (Engine engine = Engine.newBuilder("js")
         .allowExperimentalOptions(true)
         .logHandler(new java.io.FilterOutputStream(System.out) {
             public void close() throws IOException {
               // Do not close the underyling stream because HotSpot might want
               // to write some additional logs (e.g. -XX:+CITime) to stdout.
               flush();
             }
           })
         .build()) {
      try (Context context = Context.newBuilder("js")
           .engine(engine)
           .allowIO(true)
           // Needed for 'js.shell' option below
           .allowExperimentalOptions(true)
           // Required for 'octane/zlib' which requires the non-standard, global 'read()' function.
           // The "js.shell" option provides global built-ins like 'read()' for compatibility with d8.
           .option("js.shell", "true")
           // 'run.js' has to access the benchmarks and benchmark data which might be in the jar file
           // from which we are running so we have to make sure we're using the right file system.
           .fileSystem(org.graalvm.polyglot.io.FileSystem.newFileSystem(fs))
           .currentWorkingDirectory(octanePath)
           .build()) {

        context.eval(runOctane);

        Value jsBindings = context.getBindings("js");
        Value runner = jsBindings.getMember("runOctane");
        assert runner.canExecute();

        for (int i = 0; i < iterations; i++) {
          runner.execute();
          System.out.println("\n");
        }
      }

      int contexts = Integer.getInteger("contexts", 0);

      for (int j = 0; j < contexts; j++) {
        System.out.println("====================================================================");
        System.out.println("====================================================================");
        System.out.println("====================================================================");
        try (Context context = Context.newBuilder("js")
             .engine(engine)
             .allowIO(true)
             // Needed for 'js.shell' option below
             .allowExperimentalOptions(true)
             // Required for 'octane/zlib' which requires the non-standard, global 'read()' function.
             // The "js.shell" option provides global built-ins like 'read()' for compatibility with d8.
             .option("js.shell", "true")
             // 'run.js' has to access the benchmarks and benchmark data which might be in the jar file
             // from which we are running so we have to make sure we're using the right file system.
             .fileSystem(org.graalvm.polyglot.io.FileSystem.newFileSystem(fs))
             .currentWorkingDirectory(octanePath)
             .build()) {

          context.eval(runOctane);

          Value jsBindings = context.getBindings("js");
          Value runner = jsBindings.getMember("runOctane");
          assert runner.canExecute();

          for (int i = 0; i < iterations; i++) {
            runner.execute();
            System.out.println("\n");
          }
        }
      }
    }
  }
}
