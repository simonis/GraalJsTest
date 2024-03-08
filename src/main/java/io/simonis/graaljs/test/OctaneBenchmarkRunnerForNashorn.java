package io.simonis.graaljs.test;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class OctaneBenchmarkRunnerForNashorn {

  private static final String OCTANE = "resources/octane";

  public static void main(String[] args) throws Exception {

    URL octaneUrl = OctaneBenchmarkRunnerForNashorn.class.getClassLoader().getResource(OCTANE);
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

// Try to be as reproducible as possible
BenchmarkSuite.config.doWarmup = true;
BenchmarkSuite.config.doDeterministic = true;

var runner = { NotifyResult: PrintResult,
               NotifyError: PrintError,
               NotifyScore: PrintScore };

// Required for Nashorn which defines 'readFully()' in '-scripting' mode but not 'read()'
// This also means we have to enable '-scripting' mode by setting the 'nashorn.arg'
// system property to '-scripting' before we create the 'ScriptEngine'.
read = readFully;

function runOctane() {
  // We currently don't run 'mandreel.js' because it is not reentrant (i.e.
  // if executed repeatedly, it fails with 'RangeError: Maximum call stack size exceeded'.
  BenchmarkSuite.RunSuites(runner, ['Mandreel']);
}
"""
                      );
    int iterations = Integer.getInteger("iterations", 1);

    // Enable the '-scripting' mode for 'readFully()' (see:
    // https://wiki.openjdk.org/display/Nashorn/Nashorn+extensions).
    System.setProperty("nashorn.args", "-scripting");
    ScriptEngineManager manager = new ScriptEngineManager();
    ScriptEngine engine = manager.getEngineByName("nashorn");
    assert engine != null;

    engine.eval(benchmarks.toString());

    assert engine instanceof Invocable;
    Invocable runner = (Invocable)engine;

    for (int i = 0; i < iterations; i++) {
      runner.invokeFunction("runOctane");
      System.out.println("\n");
    }
  }
}
