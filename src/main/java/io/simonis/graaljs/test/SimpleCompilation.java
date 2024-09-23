package io.simonis.graaljs.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class SimpleCompilation {
  public static void main(String[] args) throws Exception {

    Engine engine = Engine.newBuilder("js")
      // Needed for -Dpolyglot.engine.TraceCompilation
      .allowExperimentalOptions(true)
      .option("cpusampler", System.getProperty("cpusampler", "false"))
      .option("cpusampler.Output", System.getProperty("cpusampler.Output", "histogram"))
      .option("cpusampler.OutputFile", System.getProperty("cpusampler.OutputFile", "/tmp/SimpleCompilation_cpusampler.txt"))
      .option("cpusampler.Period", System.getProperty("cpusampler.Period", "10")) // in ms
      .option("cpusampler.ShowTiers", System.getProperty("cpusampler.ShowTiers", "true"))
      .option("cpusampler.SampleContextInitialization", "true")
      .option("cpusampler.SampleInternal", "true")
      .build();
    System.out.println(engine.getClass().getModule());
    try (Context context = Context.newBuilder()
         .engine(engine)
         .build()) {

      String js = """
                  function test(x) {
                    let a = test_pow(x);
                    let b = test_iterate(x, Math.floor(a));
                    let c = test_loop(x, b);
                    return a + b + c;
                  }
                  function test_loop(x, y) {
                    let sum = 0;
                    for (let i = x; i > 0; i--) {
                      sum += y;
                    }
                    return sum;
                  }
                  function test_iterate(x, y) {
                    let sum = 0, i = x;
                    if (i-- > 0) sum += y;
                    if (i-- > 0) sum += y;
                    if (i-- > 0) sum += y;
                    if (i-- > 0) sum += y;
                    if (i-- > 0) sum += y;
                    return sum;
                  }
                  function test_pow(x) {
                    // The following will be optimized to `x * x * Math.sqrt(x)`
                    // in com.oracle.truffle.js.builtins.math.PowNode::pow()
                    return Math.pow(x, 2.5);
                  }
                  function main(arg) {
                    let res = 0;
                    for (let i = 0; i < 100000; i++) {
                      res += test(arg);
                    }
                    return res;
                  }
                  """;
      Source source = Source.newBuilder("js", js, "test.js").build();

      context.eval(source);
      Value jsBindings = context.getBindings("js");
      Value main = jsBindings.getMember("main");
      assert main.canExecute();
      int iterations = Integer.getInteger("iterations", 1);
      for (int j = 0; j < iterations; j++) {
        main.execute(5);
      }
      if (args.length > 0) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        in.readLine();
        for (int j = 0; j < iterations; j++) {
          main.execute(5d);
        }
      }
    }
  }
}
