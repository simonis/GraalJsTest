package io.simonis.graaljs.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class ScopeDescTest {
  public static void main(String[] args) throws Exception {

    Engine engine = Engine.newBuilder("js")
      // Needed for -Dpolyglot.engine.TraceCompilation
      .allowExperimentalOptions(true)
      .option("cpusampler", System.getProperty("cpusampler", "false"))
      .option("cpusampler.Output", System.getProperty("cpusampler.Output", "histogram"))
      .option("cpusampler.OutputFile", System.getProperty("cpusampler.OutputFile", "/tmp/ScopeDescTest_cpusampler.txt"))
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
                  function bar(arg) {
                    let sum = 0;
                    for (let i = arg; i > 0; i--) {
                      sum += arg;
                    }
                    return sum;
                  }
                  function foo(arg) {
                    return bar(arg);
                  }
                  function main(arg) {
                    return foo(arg);
                  }
                  """;
      Source source = Source.newBuilder("js", js, "test.js").build();

      context.eval(source);
      Value jsBindings = context.getBindings("js");
      Value main = jsBindings.getMember("main");
      assert main.canExecute();
      long iterations = Long.getLong("iterations", 10_000);
      for (long j = 0; j < iterations; j++) {
        main.execute(100);
      }
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      in.readLine();
    }
  }
}
