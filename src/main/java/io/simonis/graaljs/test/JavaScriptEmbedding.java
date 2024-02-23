package io.simonis.graaljs.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

public class JavaScriptEmbedding {
  public static void main(String[] args) throws Exception {
    Engine engine = Engine.newBuilder("js").build();
    System.out.println(engine.getClass().getModule());
    try (Context context = Context.newBuilder("js")
         .engine(engine)
         .build()) {
      Value function = context.eval("js", "x => x+1");
      assert function.canExecute();
      int x = function.execute(41).asInt();
      assert x == 42;
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      in.readLine();
    }
  }
}
