package io.simonis.graalpython.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

public class PythonEmbedding {
  public static void main(String[] args) throws Exception {
    // From truffle/src/com.oracle.truffle.polyglot/src/com/oracle/truffle/polyglot/InternalResourceRoots.java:
    // Internal resources are utilized before the Engine is created; hence, we cannot leverage
    // engine options and engine logger.
    // This option is available since graal-24.0.0 (see [GR-50814] at https://github.com/oracle/graal/commit/6647934c)
    System.setProperty("polyglotimpl.TraceInternalResources", "true");

    Engine engine = Engine.newBuilder("python").build();
    String engineResourcesDir = System.getProperty("engineResourcesDir", "/tmp/PolyglotEngineResources");
    engine.copyResources(Path.of(engineResourcesDir));
    System.out.println("Copied Engine resources to " + engineResourcesDir);

    System.out.println("Engine module: " + engine.getClass().getModule());
    System.out.println("==================== Engine Options ====================");
    engine.getOptions().forEach(od -> System.out.println(od.getName() + " : \n\t" + od.getHelp() + "\n\t" + od.getKey().getDefaultValue()));
    System.out.println("==================== Python Options ====================");
    engine.getLanguages().get("python").getOptions().forEach(od -> System.out.println(od.getName() + " : \n\t" + od.getHelp() + "\n\t" + od.getKey().getDefaultValue()));
    // Only supported since Graal 25.0
    //System.out.println("==================== Python Source Options ====================");
    //engine.getLanguages().get("python").getSourceOptions().forEach(od -> System.out.println(od.getName() + " : \n\t" + od.getHelp() + "\n\t" + od.getKey().getDefaultValue()));

    try (Context context = Context.newBuilder("python")
         .engine(engine)
         .option("python.ForceImportSite", "true")
         .option("log.python.com.oracle.graal.python.builtins.Python3Core.level", "FINE")
         .allowAllAccess(true)
         .build()) {
      System.out.println("Context: " + context);
      Value function = context.eval("python", "print('Hello from GraalPython');\nlambda x : x+1");
      assert function.canExecute();
      int x = function.execute(41).asInt();
      assert x == 42;
      while (true) {
        System.out.print(">>> ");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line = in.readLine();
        if (line == null) {
          System.out.println();
          return;
        }
        if (line.trim().length() == 0) continue;
        try {
          System.out.println(context.eval("python", line));
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }
}
