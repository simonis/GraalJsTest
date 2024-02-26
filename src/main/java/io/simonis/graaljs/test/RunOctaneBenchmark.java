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

public class RunOctaneBenchmark {

  private static final String OCTANE = "resources/octane";
  private static final String RUN_JS = OCTANE + "/run.js";

  public static void main(String[] args) throws Exception {

    Engine engine = Engine.newBuilder("js").build();

    URL octaneUrl = RunOctaneBenchmark.class.getClassLoader().getResource(OCTANE);
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
    URL runJsUrl = RunOctaneBenchmark.class.getClassLoader().getResource(RUN_JS);
    Source runJsSource = Source.newBuilder("js", runJsUrl).build();

    int iterations = Integer.getInteger("iterations", 1);

    for (int i = 0; i < iterations; i++) {
      Context context = Context.newBuilder("js")
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
        .build();

      context.eval(runJsSource);

      System.out.println("\n");

      /*
      System.out.println("\nSecond run..\n");

      // The following code works, but `mandreel.js` run into a
      // `RangeError: Maximum call stack size exceeded`
      // That's probably because the benchmark is not reentrant.

      Value jsBindings = context.getBindings("js");
      // Now call the benchmark one more time manually. I.e run the following code from 'run.js':
      //
      // BenchmarkSuite.RunSuites({ NotifyResult: PrintResult,
      //                            NotifyError: PrintError,
      //                            NotifyScore: PrintScore });
      //
      Value BenchmarkSuite = jsBindings.getMember("BenchmarkSuite");
      Value RunSuites = BenchmarkSuite.getMember("RunSuites");
      assert RunSuites.canExecute();
      Value PrintResult = jsBindings.getMember("PrintResult");
      Value PrintError = jsBindings.getMember("PrintError");
      Value PrintScore = jsBindings.getMember("PrintScore");
      assert PrintResult.canExecute() && PrintError.canExecute() && PrintScore.canExecute();
      RunSuites.execute(ProxyObject.fromMap(Map.of("NotifyResult", PrintResult,
                                                   "NotifyError", PrintError,
                                                   "NotifyScore", PrintScore)));
      */

      context.close();
    }
  }
}
