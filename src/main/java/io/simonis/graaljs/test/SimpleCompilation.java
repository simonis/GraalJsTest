package io.simonis.graaljs.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public class SimpleCompilation {
  public static void main(String[] args) throws Exception {
    Engine engine = Engine.newBuilder("js")
      // Needed for -Dpolyglot.engine.TraceCompilation
      .allowExperimentalOptions(true)
      .build();
    System.out.println(engine.getClass().getModule());
    try (Context context = Context.newBuilder("js")
         .engine(engine)
         .build()) {
      context.eval("js", """
function add(x, y) {
  return x + y;
}
function sub(x, y) {
  return x - y;
}
function mul(x, y) {
  return x * y;
}
function div(x, y) {
  return x / y;
}
""");
      Value jsBindings = context.getBindings("js");
      Value add = jsBindings.getMember("add");
      assert add.canExecute();
      Value sub = jsBindings.getMember("sub");
      assert sub.canExecute();
      Value mul = jsBindings.getMember("mul");
      assert mul.canExecute();
      Value div = jsBindings.getMember("div");
      assert div.canExecute();
      for (int i = 1 ; i <= 10_000; i++) {
        int r = add.execute(i, i).asInt();
        assert r == i + i;
        r = sub.execute(i, i).asInt();
        assert r == 0;
        r = mul.execute(i, i).asInt();
        assert r == i * i;
        r = div.execute(i, i).asInt();
        assert r == 1;
      }
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      in.readLine();
      for (int i = 1 ; i <= 10_000; i++) {
        double r = add.execute((double)i, i).asDouble();
        assert r == (double)i + i;
      }
    }
  }
}
