package io.simonis.nativeimage.test;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;
import java.util.ServiceLoader;

public class RsaTest {
    public static void main(String[] args) throws Exception {
        Provider accp = null;
        /*
         * This code, although semantically equal to the below code, won't
         * work if compiled with native-image and result in an exception at
         * run time:
         *
         * Exception in thread "main" java.util.ServiceConfigurationError: java.security.Provider: Provider sun.security.pkcs11.SunPKCS11 not found
         *    at java.base@21.0.6/java.util.ServiceLoader.fail(ServiceLoader.java:593)
         *    at java.base@21.0.6/java.util.ServiceLoader.loadProvider(ServiceLoader.java:875)
         * at java.base@21.0.6/java.util.ServiceLoader$ModuleServicesLookupIterator.hasNext(ServiceLoader.java:1084)
         * at java.base@21.0.6/java.util.ServiceLoader$2.hasNext(ServiceLoader.java:1309)
         * at java.base@21.0.6/java.util.ServiceLoader$3.hasNext(ServiceLoader.java:1393)
         * at io.simonis.nativeimage.test.RsaTest.main(RsaTest.java:32)
         * at java.base@21.0.6/java.lang.invoke.LambdaForm$DMH/sa346b79c.invokeStaticInit(LambdaForm$DMH)

        ServiceLoader<Provider> pl = ServiceLoader.load(Provider.class);
        for (Provider p : pl) {
            System.out.println(p.getName());
            if ("AmazonCorrettoCryptoProvider".equals(p.getName())) {
                accp = p;
            }
        }
         */
        for (Provider p : Security.getProviders()) {
            System.out.println(p.getName());
            if ("AmazonCorrettoCryptoProvider".equals(p.getName())) {
                accp = p;
            }
        }

        String message = args.length == 0 ? "RSA encryption/decryption test" : args[0];
        if (message.toLowerCase().contains("accp") && accp != null) {
            Security.removeProvider("AmazonCorrettoCryptoProvider");
            Security.insertProviderAt(accp, 1);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("<RETURN>");
        in.readLine();
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        System.out.println("RSA KeyPairGenerator provider: " + rsaGen.getProvider().getName());
        rsaGen.initialize(2048);
        KeyPair rsa = rsaGen.generateKeyPair();
        PrivateKey rsaPrivate = rsa.getPrivate();
        PublicKey rsaPublic = rsa.getPublic();
        Cipher rsaEncryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        System.out.println("RSA Cipher provider: " + rsaEncryptCipher.getProvider().getName());
        rsaEncryptCipher.init(Cipher.ENCRYPT_MODE, rsaPublic);
        byte[] encrypted = rsaEncryptCipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        System.out.println("Encrypted message: " + Base64.getEncoder().encodeToString(encrypted));
        Cipher rsaDecryptCipher = Cipher.getInstance("RSA");
        rsaDecryptCipher.init(Cipher.DECRYPT_MODE, rsaPrivate);
        byte[] decrypted = rsaDecryptCipher.doFinal(encrypted);
        String decryptedMessage = new String(decrypted, StandardCharsets.UTF_8);
        assert message.equals(decryptedMessage);
        System.out.println("Decrypted message: " + decryptedMessage);
        System.out.print("<RETURN>");
        in.readLine();
    }
}
/*
@TargetClass(className = "com.amazon.corretto.crypto.provider.Janitor")
final class Target_com_amazon_corretto_crypto_provider_Janitor {
}
@TargetClass(className = "com.amazon.corretto.crypto.provider.Loader")
final class Target_com_amazon_corretto_crypto_provider_Loader {
    @Alias
    @RecomputeFieldValue(kind = Kind.Reset)
    static Target_com_amazon_corretto_crypto_provider_Janitor RESOURCE_JANITOR;
}
 */

class AccpFeature implements Feature {

    @Override
    public String getDescription() {
        return "ACCP Support";
    }

    @Override
    public boolean isInConfiguration(IsInConfigurationAccess a) {
        if (a.findClassByName("com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider") != null) {
            return true;
        } else {
            return false;
        }
    }

    /*
    @Override
    public void duringSetup(DuringSetupAccess a) {
        System.out.println("--> AccpFeature::duringSetup()");
        //RuntimeClassInitializationSupport rci = ImageSingletons.lookup(RuntimeClassInitializationSupport.class);
        // rerunInitialization() is just an alias for initializeAtRunTime() in Graal 23.1 but still used in internal Featuers.
        // TODO: replace by initializeAtRunTime()
        //rci.rerunInitialization("com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider", "for ACCP");
        try {
            RuntimeClassInitialization.initializeAtRunTime(a.findClassByName("com.amazon.corretto.crypto.provider.Loader"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
     */
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess a) {
        System.out.println("--> AccpFeature::beforeAnalysis()");
        Class spf = a.findClassByName("com.amazon.corretto.crypto.provider.ServiceProviderFactory");
        //a.registerAsUsed(spf);
        RuntimeReflection.register(spf);
        try {
            RuntimeReflection.register(spf.getDeclaredMethod("provider"));
            RuntimeReflection.registerAllDeclaredMethods(spf);
        } catch (NoSuchMethodException e) {
            System.out.println("Can't get com.amazon.corretto.crypto.provider.ServiceProviderFactory::provider() method.");
            e.printStackTrace(System.out);
        }
        String accpLibName = System.mapLibraryName("amazonCorrettoCryptoProvider");
        RuntimeResourceAccess.addResource(spf.getModule(), "com/amazon/corretto/crypto/provider/" + accpLibName);
        RuntimeResourceAccess.addResource(spf.getModule(), "com/amazon/corretto/crypto/provider/version.properties");
    }
}

/*
 * Build with a GraalJDK with ACCP as default crypto provider:
 *

$ GraalVM21/build/jdk-21/bin/native-image -g -O0 -H:+SourceLevelDebug -H:Log=registerResource --no-fallback \
--strict-image-heap --features=io.simonis.nativeimage.test.AccpFeature --initialize-at-build-time='\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider$ACCPService,\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider,\
com.amazon.corretto.crypto.provider.ExtraCheck,\
com.amazon.corretto.crypto.provider.SelfTestSuite,\
com.amazon.corretto.crypto.provider.SelfTestSuite$SelfTest,\
com.amazon.corretto.crypto.provider.SelfTestResult,\
com.amazon.corretto.crypto.provider.SelfTestStatus,\
com.amazon.corretto.crypto.provider.Utils$NativeContextReleaseStrategy' \
--trace-class-initialization=com.amazon.corretto.crypto.provider.Loader \
--trace-class-initialization=com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider \
-H:+TraceSecurityServices -H:DebugInfoSourceSearchPath=/priv/simonisv/Git/amazon-corretto-crypto-provider/src \
-cp target/graal-js-test-1.0-SNAPSHOT.jar io.simonis.nativeimage.test.RsaTest /tmp/RsaTest

 */