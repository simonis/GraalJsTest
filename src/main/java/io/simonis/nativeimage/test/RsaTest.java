package io.simonis.nativeimage.test;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.oracle.svm.util.ReflectionUtil;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.FieldValueTransformer;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import javax.crypto.Cipher;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

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
         *
         * This is because `Security.getProviders()` as used below, only returns the providers which were added to the
         * native image, whereas `ServiceLoader.load(Provider.class)` also returns providers which were removed from the
         * native image (see the list of removed providers at the end of the `/security_services_*.txt` file produced by `-H:+TraceSecurityServices`).
         * Also, `java.security.Provider` is in the list of `SKIPPED_SERVICES` in `com.oracle.svm.hosted.ServiceLoaderFeature` and so
         * security providers don't get added to the native image automatically.
         *

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

        String HOST = "raw.githubusercontent.com";
        String PATH = "/simonis/GraalJsTest/refs/heads/main/src/main/java/io/simonis/nativeimage/test/RsaTest.java";
        String host = System.getProperty("host", HOST);
        Integer port = Integer.getInteger("port", 443);
        String path = System.getProperty("path", PATH);
        SocketFactory sslSocketFactory = SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket)sslSocketFactory.createSocket(host, port);
        InputStream sin = sslSocket.getInputStream();
        OutputStream out = sslSocket.getOutputStream();
        String request = String.format("GET %s HTTP/1.1\r\nHost: %s\r\n\r\n", path, host);
        System.out.println(request);
        out.write(request.getBytes());
        BufferedReader br = new BufferedReader(new InputStreamReader(sin));
        int length = 0;
        String line;
        do {
            line = br.readLine();
            if (line != null && line.startsWith("Content-Length: ")) {
                length = Integer.parseUnsignedInt(line, "Content-Length: ".length(), line.length(), 10);
                System.out.println("Content-Length: " + length);
            }
        } while (!"".equals(line));
        char[] buf = new char[length];
        StringBuilder sb = new StringBuilder(length);
        for (int read; (read = br.read(buf, 0, length)) > 0; length -= read) {
            sb.append(buf, 0, read);
        }
        if (HOST.equals(host) && PATH.equals(path)) {
            if (sb.toString().contains(PATH)) {
                System.out.println("Successfully read " + PATH);
                System.out.println("From https://" + HOST);
            } else {
                System.out.println("This class is out of sync with the GitHub repo.");
                System.out.println("Do you need to commit?");
            }
        }
    }
}

/** A predicate to tell whether this platform includes the argument class.
 *  Copied from substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/jdk/PlatformHasClass.java.
 * */
final class PlatformHasClass implements Predicate<String> {
    @Override
    public boolean test(String className) {
        return ReflectionUtil.lookupClass(true, className) != null;
    }
}

@TargetClass(className = "com.amazon.corretto.crypto.provider.EvpKey", onlyWith = PlatformHasClass.class)
final class Target_com_amazon_corretto_crypto_provider_EvpKey {
    @Alias
    @RecomputeFieldValue(kind = Kind.Custom, declClass = InternalGetEncoded.class, isFinal = true)
    protected volatile byte[] encoded;
    static class InternalGetEncoded implements FieldValueTransformer {
        @Override
        public Object transform(Object receiver, Object original) {
            try {
                Class evpKey = ReflectionUtil.lookupClass(true, "com.amazon.corretto.crypto.provider.EvpKey");
                if (evpKey != null) {
                    Method internalGetEncoded = ReflectionUtil.lookupMethod(true, evpKey, "internalGetEncoded");
                    if (internalGetEncoded != null) {
                        Object ret = ReflectionUtil.invokeMethod(internalGetEncoded, receiver);
                        return ret;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace(System.out);
            }
            return null;
        }
    }
}

@TargetClass(className = "com.amazon.corretto.crypto.provider.EvpRsaKey", onlyWith = PlatformHasClass.class)
final class Target_com_amazon_corretto_crypto_provider_EvpRsaKey {
    @Alias
    @RecomputeFieldValue(kind = Kind.Custom, declClass = GetModulus.class)
    protected volatile BigInteger modulus;
    static class GetModulus implements FieldValueTransformer {
        @Override
        public Object transform(Object receiver, Object original) {
            try {
                Class evpRsaKey = ReflectionUtil.lookupClass(true, "com.amazon.corretto.crypto.provider.EvpRsaKey");
                if (evpRsaKey != null) {
                    Method getModulus = ReflectionUtil.lookupMethod(true, evpRsaKey, "getModulus");
                    if (getModulus != null) {
                        Object ret = ReflectionUtil.invokeMethod(getModulus, receiver);
                        return ret;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace(System.out);
            }
            return null;
        }
    }
}

@TargetClass(className = "com.amazon.corretto.crypto.provider.EvpHmac", innerClass = "SHA256", onlyWith = PlatformHasClass.class)
final class Target_com_amazon_corretto_crypto_provider_EvpHmac_SHA256 {
    @Alias
    @RecomputeFieldValue(kind = Kind.Custom, declClass = Utils_getEvpMdFromName.class)
    private static long evpMd;
    @Alias
    @RecomputeFieldValue(kind = Kind.Custom, declClass = Utils_getDigestLength.class)
    private static int digestLength;
    static class Utils_getEvpMdFromName implements FieldValueTransformer {
        @Override
        public Object transform(Object receiver, Object original) {
            return Target_com_amazon_corretto_crypto_provider_Utils.getEvpMdFromName("sha1");
        }
    }
    static class Utils_getDigestLength implements FieldValueTransformer {
        @Override
        public Object transform(Object receiver, Object original) {
            return Target_com_amazon_corretto_crypto_provider_Utils.getDigestLength(
                    Target_com_amazon_corretto_crypto_provider_Utils.getEvpMdFromName("sha1"));
        }
    }
}

@TargetClass(className = "com.amazon.corretto.crypto.provider.Utils", onlyWith = PlatformHasClass.class)
final class Target_com_amazon_corretto_crypto_provider_Utils {
    @Alias
    @RecomputeFieldValue(kind = Kind.NewInstance, declClass = ConcurrentHashMap.class, isFinal = false, disableCaching = true)
    private static Map<String, Long> digestPtrByName = new ConcurrentHashMap<>();
    @Alias
    @RecomputeFieldValue(kind = Kind.NewInstance, declClass = ConcurrentHashMap.class, isFinal = false, disableCaching = true)
    private static Map<Long, Integer> digestLengthByPtr = new ConcurrentHashMap<>();
    @Alias
    static native long getEvpMdFromName(String digestName);
    @Alias
    static native int getDigestLength(long evpMd);

}

@TargetClass(className = "java.util.concurrent.locks.AbstractOwnableSynchronizer")
final class Target_java_util_concurrent_locks_AbstractOwnableSynchronizer {
    @Alias
    @RecomputeFieldValue(kind = Kind.Reset)
    private transient Thread exclusiveOwnerThread;
}

@TargetClass(className = "java.util.concurrent.locks.AbstractQueuedSynchronizer", innerClass = "Node")
final class Target_java_util_concurrent_locks_AbstractQueuedSynchronizer_Node {
    @Alias
    @RecomputeFieldValue(kind = Kind.Reset)
    Thread waiter;
}

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

    @Override
    public void duringSetup(DuringSetupAccess a) {
        System.out.println("--> AccpFeature::duringSetup()");
        try {
            RuntimeClassInitializationSupport rci = ImageSingletons.lookup(RuntimeClassInitializationSupport.class);
            System.out.println("--> " + rci.getClass().getName());
            // If running with `--strict-image-heap` (or the equivalent `-H:+StrictImageHeap`) then `rci` will be of type
            // `AllowAllHostedUsagesClassInitializationSupport` and `rerunInitialization()` will be just an alias for
            // `initializeAtRunTime()`. Starting with Graal 24.0 `--strict-image-heap` is the default (and only supported configuration).
            // See https://github.com/oracle/graal/pull/4684 and https://github.com/oracle/graal/pull/7474 for more information.
            //
            // However, without `--strict-image-heap`, `rci` will be a `ProvenSafeClassInitializationSupport` and `rerunInitialization()`
            // will indeed trigger the re-initialization of the corresponding classes at run time.
            // Notice that accesing `RuntimeClassInitializationSupport` requires
            // `--add-exports org.graalvm.nativeimage/org.graalvm.nativeimage.impl=ALL-UNNAMED` on the native image command line.
            rci.rerunInitialization("com.amazon.corretto.crypto.provider.Loader", "for ACCP");
            rci.rerunInitialization("com.amazon.corretto.crypto.provider.EvpHmac$SHA256", "for ACCP");
            rci.rerunInitialization("com.amazon.corretto.crypto.provider.Utils", "for ACCP");
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }

        /*
        try {
            RuntimeClassInitialization.initializeAtRunTime(a.findClassByName("com.amazon.corretto.crypto.provider.Loader"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
         */
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess a) {
        System.out.println("--> AccpFeature::beforeAnalysis()");
        Class spf = a.findClassByName("com.amazon.corretto.crypto.provider.ServiceProviderFactory");
        //a.registerAsUsed(spf);
        RuntimeReflection.register(spf);
        Class rex = a.findClassByName("com.amazon.corretto.crypto.provider.RuntimeCryptoException");
        RuntimeJNIAccess.register(rex);
        for (Constructor c : rex.getDeclaredConstructors()) {
            RuntimeJNIAccess.register(c);
        }
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
 * Build with a GraalJDK with ACCP as default crypto provider (RSA example):
 *

$ GraalVM21/build/jdk-21/bin/native-image -g -O0 -H:+SourceLevelDebug -H:+IncludeDebugHelperMethods -H:-DeleteLocalSymbols -H:Log=registerResource --no-fallback \
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

 *
 * Build with a GraalJDK with ACCP as default crypto provider (RSA & SSL example):
 * Requires Target_java_util_concurrent_locks_AbstractOwnableSynchronizer and
 * Target_java_util_concurrent_locks_AbstractQueuedSynchronizer_Node to reset all references to the
 * "Native reference cleanup thread" thread created by ACCP.
 *
 * This will still crash in `com.amazon.corretto.crypto.provider.EvpKey.encodePublicKey(Native Method)` because
 * `EvpKey` keeps an opaque reference to native image which is not available any more at run time.
 *
 * However we can fix it by disabeling ACCP in `Target_sun_security_ssl_TrustStoreManager` for the time we call
 * `TrustStoreManager::getTrustedCerts()` and `TrustStoreManager::getTrustedKeyStore()`
 *

$ graal/graalvm-community-jdk21u/sdk/linux-amd64/GRAALVM_72849DDB0E_JAVA21/graalvm-72849ddb0e-java21-23.1.7-dev/bin/native-image \
-g -O0 -H:+PrintImageObjectTree -H:+PrintUniverse -H:+DiagnosticsMode -H:+LogVerbose -H:+PrintFeatures \
-H:+SourceLevelDebug -H:Log=registerResource -H:LogFile=/tmp/native-image.log --no-fallback \
--strict-image-heap \
--add-exports org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED \
--add-exports org.graalvm.nativeimage/org.graalvm.nativeimage.impl=ALL-UNNAMED \
--features=io.simonis.nativeimage.test.AccpFeature --initialize-at-build-time='\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider$ACCPService,\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider,\
com.amazon.corretto.crypto.provider.ExtraCheck,\
com.amazon.corretto.crypto.provider.SelfTestSuite,\
com.amazon.corretto.crypto.provider.SelfTestSuite$SelfTest,\
com.amazon.corretto.crypto.provider.SelfTestResult,\
com.amazon.corretto.crypto.provider.SelfTestStatus,\
com.amazon.corretto.crypto.provider.Utils$NativeContextReleaseStrategy,\
com.amazon.corretto.crypto.provider.EvpRsaPublicKey,\
com.amazon.corretto.crypto.provider.EvpKeyType,\
com.amazon.corretto.crypto.provider.EvpKey$InternalKey,\
com.amazon.corretto.crypto.provider.NativeResource$Cell,\
com.amazon.corretto.crypto.provider.Janitor$HeldReference,\
com.amazon.corretto.crypto.provider.Janitor$Stripe,\
com.amazon.corretto.crypto.provider.EvpEcPublicKey' \
--trace-class-initialization=com.amazon.corretto.crypto.provider.Loader \
--trace-class-initialization=com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider \
--trace-object-instantiation=com.amazon.corretto.crypto.provider.EvpRsaPublicKey \
-H:+TraceSecurityServices -H:DebugInfoSourceSearchPath=/priv/simonisv/Git/amazon-corretto-crypto-provider/src \
-cp target/graal-js-test-1.0-SNAPSHOT.jar:/priv/simonisv/Git/amazon-corretto-crypto-provider/build/lib/AmazonCorrettoCryptoProvider.jar \
-Djava.security.properties=GraalVM21/build/jdk-21/conf/security/java.security io.simonis.nativeimage.test.RsaTest /tmp/RsaTest

 *
 * Build with a GraalJDK with ACCP as default crypto provider, without `--strict-image-heap` (RSA example)
 *

$ GraalVM21/build/jdk-21/bin/native-image -g -O0 -H:+SourceLevelDebug H:+IncludeDebugHelperMethods -H:Log=registerResource --no-fallback \
--add-exports org.graalvm.nativeimage/org.graalvm.nativeimage.impl=ALL-UNNAMED \
--features=io.simonis.nativeimage.test.AccpFeature --initialize-at-build-time='\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider$ACCPService,\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider,\
com.amazon.corretto.crypto.provider.ExtraCheck,\
com.amazon.corretto.crypto.provider.SelfTestSuite,\
com.amazon.corretto.crypto.provider.SelfTestSuite$SelfTest,\
com.amazon.corretto.crypto.provider.SelfTestResult,\
com.amazon.corretto.crypto.provider.SelfTestStatus,\
com.amazon.corretto.crypto.provider.Utils$NativeContextReleaseStrategy,\
com.amazon.corretto.crypto.provider.Janitor$Stripe,\
com.amazon.corretto.crypto.provider.EvpHmac,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA384,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA384Base,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA512,\
com.amazon.corretto.crypto.provider.EvpKeyType$1,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA256Base,\
com.amazon.corretto.crypto.provider.Utils,\
com.amazon.corretto.crypto.provider.Janitor$HeldReference,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA512Base,\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider$1,\
com.amazon.corretto.crypto.provider.EvpKeyType,\
com.amazon.corretto.crypto.provider.EvpHmac$MD5Base,\
com.amazon.corretto.crypto.provider.EvpHmac$MD5,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA1Base,\
com.amazon.corretto.crypto.provider.SHA1Spi,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA1,\
com.amazon.corretto.crypto.provider.DebugFlag,\
com.amazon.corretto.crypto.provider.LibCryptoRng$SPI,\
com.amazon.corretto.crypto.provider.SHA256Spi,\
com.amazon.corretto.crypto.provider.NativeResource$Cell,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA256' \
--trace-class-initialization=com.amazon.corretto.crypto.provider.Loader \
--trace-class-initialization=com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider \
-H:+TraceSecurityServices -H:DebugInfoSourceSearchPath=/priv/simonisv/Git/amazon-corretto-crypto-provider/src \
-cp target/graal-js-test-1.0-SNAPSHOT.jar io.simonis.nativeimage.test.RsaTest /tmp/RsaTest

 *
 * Build with a GraalJDK with ACCP as default crypto provider, without `--strict-image-heap` (RSA & SSL example)
 *
 * This crashes in `com.amazon.corretto.crypto.provider.EvpSignature.verify(Native Method)`
 *

$ graalvm-community-jdk21u/sdk/linux-amd64/GRAALVM_72849DDB0E_JAVA21/graalvm-72849ddb0e-java21-23.1.7-dev/bin/native-image \
-g -O0 -H:+PrintImageObjectTree -H:+PrintUniverse -H:+DiagnosticsMode -H:+LogVerbose -H:+PrintFeatures \
-H:+SourceLevelDebug -H:Log=registerResource -H:LogFile=/tmp/native-image.log --no-fallback \
--add-exports org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED \
--add-exports org.graalvm.nativeimage/org.graalvm.nativeimage.impl=ALL-UNNAMED \
--features=io.simonis.nativeimage.test.AccpFeature --initialize-at-build-time='\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider$ACCPService,\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider,\
com.amazon.corretto.crypto.provider.ExtraCheck,\
com.amazon.corretto.crypto.provider.SelfTestSuite,\
com.amazon.corretto.crypto.provider.SelfTestSuite$SelfTest,\
com.amazon.corretto.crypto.provider.SelfTestResult,\
com.amazon.corretto.crypto.provider.SelfTestStatus,\
com.amazon.corretto.crypto.provider.Utils$NativeContextReleaseStrategy,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA384,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA512,\
com.amazon.corretto.crypto.provider.EvpHmac$MD5Base,\
com.amazon.corretto.crypto.provider.SHA256Spi,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA256,\
com.amazon.corretto.crypto.provider.SHA1Spi,\
com.amazon.corretto.crypto.provider.LibCryptoRng$SPI,\
com.amazon.corretto.crypto.provider.AesCbcSpi,\
com.amazon.corretto.crypto.provider.Janitor$HeldReference,\
com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider$1,\
com.amazon.corretto.crypto.provider.EvpHmac$MD5,\
com.amazon.corretto.crypto.provider.EcGen,\
com.amazon.corretto.crypto.provider.DebugFlag,\
com.amazon.corretto.crypto.provider.EvpHmac,\
com.amazon.corretto.crypto.provider.EvpKeyType,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA1,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA256Base,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA1Base,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA384Base,\
com.amazon.corretto.crypto.provider.EvpHmac$SHA512Base,\
com.amazon.corretto.crypto.provider.Janitor$Stripe,\
com.amazon.corretto.crypto.provider.Utils' \
--trace-class-initialization=com.amazon.corretto.crypto.provider.Loader \
--trace-class-initialization=com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider \
--trace-class-initialization=com.amazon.corretto.crypto.provider.Utils \
-H:TraceObjectInstantiation=com.amazon.corretto.crypto.provider.Utils \
--trace-object-instantiation=com.amazon.corretto.crypto.provider.SelfTestResult \
-H:+TraceSecurityServices -H:DebugInfoSourceSearchPath=/priv/simonisv/Git/amazon-corretto-crypto-provider/src \
-cp target/graal-js-test-1.0-SNAPSHOT.jar:/priv/simonisv/Git/amazon-corretto-crypto-provider/build/lib/AmazonCorrettoCryptoProvider.jar \
-Djava.security.properties=GraalVM21/src/GraalVM21/build/jdk-21/conf/security/java.security io.simonis.nativeimage.test.RsaTest /tmp/RsaTest

 */
