package io.simonis.graaljs.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public class JsStackOverflow {
  public static void main(String[] args) throws Exception {
    final long depth = args.length > 0 ? Long.parseLong(args[0]) : 100;
    Engine engine = Engine.newBuilder("js")
      // Needed for -Dpolyglot.engine.TraceCompilation
      .allowExperimentalOptions(true)
      .build();
    System.out.println(engine.getClass().getModule());
    try (Context context = Context.newBuilder("js")
         .engine(engine)
         // Needed for 'js.stack-trace-limit'
         //.allowExperimentalOptions(true)
         //.option("js.stack-trace-limit", "7")
         .build()) {
      context.eval("js", """
ex = new Error("Hello");
function recurse(num) {
  if (num === 0) {
    throw ex;
  } else {
    print(num);
    return recurse(num - 1);
  }
}""");
      Value jsBindings = context.getBindings("js");
      Value recurse = jsBindings.getMember("recurse");
      assert recurse.canExecute();
      try {
        int x = recurse.execute(depth).asInt();
        assert x == 42;
      } catch (PolyglotException pe) {
        pe.printStackTrace(System.out);
      }
      try {
        int x = recurse.execute(depth).asInt();
        assert x == 42;
      } catch (PolyglotException pe) {
        pe.printStackTrace(System.out);
      }
      try {
        int x = recurse.execute(depth).asInt();
        assert x == 42;
      } catch (PolyglotException pe) {
        pe.printStackTrace(System.out);
      }
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      in.readLine();
    }
  }
}
