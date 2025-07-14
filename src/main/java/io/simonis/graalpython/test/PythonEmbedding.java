package io.simonis.graalpython.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

public class PythonEmbedding {
  public static void main(String[] args) throws Exception {
    Engine engine = Engine.newBuilder("python").build();
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
        if (line == null) return;
        try {
          System.out.println(context.eval("python", line));
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }
}
