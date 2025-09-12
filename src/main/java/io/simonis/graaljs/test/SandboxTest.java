package io.simonis.graaljs.test;

import java.util.Iterator;
import java.util.ServiceLoader;

public class SandboxTest {

  public static void main(String[] args) throws Exception {
    // Want to see the actual runtime but don't want to make it a compile-time dependency.
    System.out.println(Class.forName("com.oracle.truffle.api.Truffle").getMethod("getRuntime").invoke(null));

    Class<?> abstractPolyglotImpl =
      Class.forName("org.graalvm.polyglot.impl.AbstractPolyglotImpl");
    ServiceLoader<?> polyglotImplLoader =
      ServiceLoader.load(/*abstractPolyglotImpl.getModule().getLayer(),*/ abstractPolyglotImpl);
    Iterator<?> polyglotImplIterator = polyglotImplLoader.iterator();
    while (polyglotImplIterator.hasNext()) {
      //Class<?> polyglotImpl = (Class<?>)polyglotImplIterator.next();
      //System.out.println(polyglotImpl.getName());
      System.out.println(polyglotImplIterator.next());
    }
  }
}
