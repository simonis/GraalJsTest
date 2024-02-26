package io.simonis.graaljs.test;

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

    Engine engine = Engine.newBuilder("js").build();

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
    StringBuffer benchmarks = new StringBuffer();
    benchmarks.append(Files.readString(octanePath.resolve("base.js")));
    benchmarks.append(Files.readString(octanePath.resolve("richards.js")));
    benchmarks.append(Files.readString(octanePath.resolve("deltablue.js")));
    benchmarks.append(Files.readString(octanePath.resolve("crypto.js")));
    benchmarks.append(Files.readString(octanePath.resolve("raytrace.js")));
    benchmarks.append(Files.readString(octanePath.resolve("earley-boyer.js")));
    benchmarks.append(Files.readString(octanePath.resolve("regexp.js")));
    benchmarks.append(Files.readString(octanePath.resolve("splay.js")));
    benchmarks.append(Files.readString(octanePath.resolve("navier-stokes.js")));
    benchmarks.append(Files.readString(octanePath.resolve("pdfjs.js")));
    benchmarks.append(Files.readString(octanePath.resolve("mandreel.js")));
    benchmarks.append(Files.readString(octanePath.resolve("gbemu-part1.js"))
                      // Make 'Gameboy' reentrant
                      .replace("""
                               function tearDownGameboy() {
                                 decoded_gameboy_rom = null;
                                 expectedGameboyStateStr = null;
                               }
                               """,
                               """
                               function tearDownGameboy() {
                                 decoded_gameboy_rom = null;
                               }
                               """));
    benchmarks.append(Files.readString(octanePath.resolve("gbemu-part2.js")));
    benchmarks.append(Files.readString(octanePath.resolve("code-load.js")));
    benchmarks.append(Files.readString(octanePath.resolve("box2d.js"))
                      // Make 'Box2D' reentrant
                      .replace("""
                               function tearDownBox2D() {
                                 world = null;
                                 Box2D = null;
                               }
                               """,
                               """
                               function tearDownBox2D() {
                                 world = null;
                               }
                               """));
    benchmarks.append(Files.readString(octanePath.resolve("zlib.js")));
    benchmarks.append(Files.readString(octanePath.resolve("zlib-data.js")));
    benchmarks.append(Files.readString(octanePath.resolve("typescript.js"))
                      // Make 'Typescript' reentrant
                      .replace("""
                               function tearDownTypescript() {
                                 compiler_input = null;
                               }
                               """,
                               """
                               function tearDownTypescript() {
                               }
                               """));
    benchmarks.append(Files.readString(octanePath.resolve("typescript-input.js")));
    benchmarks.append(Files.readString(octanePath.resolve("typescript-compiler.js")));
    benchmarks.append(// From "run.js":
"""
var success = true;

function PrintResult(name, result) {
  print(name + ': ' + result);
}


function PrintError(name, error) {
  PrintResult(name, error);
  success = false;
}


function PrintScore(score) {
  if (success) {
    print('----');
    print('Score (version ' + BenchmarkSuite.version + '): ' + score);
  }
}


BenchmarkSuite.config.doWarmup = undefined;
BenchmarkSuite.config.doDeterministic = undefined;

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
    Source runOctane = Source.create("js", benchmarks);

    int iterations = Integer.getInteger("iterations", 1);

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
