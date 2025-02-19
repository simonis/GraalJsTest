package io.simonis.graaljs.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class EvalTest {
  public static void main(String[] args) throws Exception {

    Engine engine = Engine.newBuilder("js")
      // Needed for -Dpolyglot.engine.TraceCompilation
      .allowExperimentalOptions(true)
      .build();
    System.out.println(engine.getClass().getModule());
    try (Context context = Context.newBuilder()
         .engine(engine)
         .build()) {

      String js = """
                  var x = 9;
                  var y = 3;
                  function c(x, y) {
                    return x * y;
                  }
                  function b(x, y) {
                    return c(x, y);
                  }
                  function a(x, y) {
                    return b(x, y);
                  }
                  function bar(arg, x, y) {
                    return eval?.(arg);
                  }
                  function foo(arg, x, y) {
                    return bar(arg, x, y);
                  }
                  function main(arg) {
                    return bar(arg, 9, 3);
                  }
                  """;
      Source source = Source.newBuilder("js", js, "test.js").build();

      context.eval(source);
      Value jsBindings = context.getBindings("js");
      Value main = jsBindings.getMember("main");
      assert main.canExecute();
      long iterations = Long.getLong("iterations", 10_000);
      String arg = "x + y";
      for (long j = 0; j < iterations; j++) {
        main.execute(arg);
      }
      System.out.println(main.execute(arg));
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      in.readLine();
      arg = "x - y";
      for (long j = 0; j < iterations; j++) {
        main.execute(arg);
      }
      System.out.println(main.execute(arg));
      in.readLine();
      arg = "a(x, y)";
      for (long j = 0; j < iterations; j++) {
        main.execute(arg);
      }
      System.out.println(main.execute(arg));
      in.readLine();
      arg = "function f(x, y) { return g(x, y); } function g(x, y) { return h(x, y); } function h(x, y) { return x/y; } f(x, y)";
      for (long j = 0; j < iterations; j++) {
        main.execute(arg);
      }
      System.out.println(main.execute(arg));
      in.readLine();
      arg = "function f(a, b) { return g(a, b); } function g(a, b) { return h(a, b); } function h(a, b) { return b - a; } f(x, y)";
      for (long j = 0; j < iterations; j++) {
        main.execute(arg);
      }
      System.out.println(main.execute(arg));
      in.readLine();
      arg = "function l(a, b) { return m(a, b); } function m(a, b) { return n(a, b); } function n(a, b) { return a + a; } l(x, y)";
      for (long j = 0; j < iterations; j++) {
        main.execute(arg);
      }
      System.out.println(main.execute(arg));
      in.readLine();
      arg = "function l(a, b) { return m(a, b); } function m(a, b) { return q(a, b); } function q(a, b) { return b + b; } l(x, y)";
      for (long j = 0; j < iterations; j++) {
        main.execute(arg);
      }
      System.out.println(main.execute(arg));
      in.readLine();
    }
  }
}
