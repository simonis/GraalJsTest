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
                    let a = square5_loop(x);
                    let b = square5_simple(x);
                    let c = pow2(x);
                    return a == b && b == c;
                  }
                  function square5_loop(x) {
                    let sum = 0;
                    for (let i = x; i > 0; i--) {
                      sum += x;
                    }
                    return sum;
                  }
                  function square5_simple(x) {
                    let sum = x + x + x + x + x;
                    return sum;
                  }
                  function pow2(x) {
                    return Math.pow(x, 2);
                  }
                  function main(arg) {
                    for (let i = 0; i < 100000; i++) {
                      test(arg);
                    }
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
