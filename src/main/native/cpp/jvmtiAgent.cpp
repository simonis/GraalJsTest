#include <jvmti.h>
#include <stdio.h>
#include <string.h>

jvmtiEnv *jvmti = NULL;

static int setupJVMTI(JNIEnv *env, JavaVM *jvm) {
  if (jvmti == NULL) {
    if (jvm == NULL) {
      jint result = env->GetJavaVM(&jvm);
      if (result != JNI_OK) {
        fprintf(stderr, "Can't get JavaVM!\n");
        return JNI_ERR;
      }
    }
    jint result = jvm->GetEnv((void**) &jvmti, JVMTI_VERSION_1_1);
    if (result != JNI_OK) {
      fprintf(stderr, "Can't access JVMTI!\n");
      return result;
    }
  }
  return JNI_OK;
}

static void printMethod(jvmtiEnv *jvmti_env,
                        jmethodID method,
                        const void* code_addr,
                        jint code_size,
                        const char* prefix) {
  char *name, *sig, *cl;
  jclass javaClass;
  jvmtiError error;
  error = jvmti->GetMethodDeclaringClass(method, &javaClass);
  if (error != JVMTI_ERROR_NONE) {
    printf("Error while calling GetMethodDeclaringClass(): %d\n", error);
    return;
  }
  error = jvmti->GetClassSignature(javaClass, &cl, NULL);
  if (error != JVMTI_ERROR_NONE) {
    printf("Error while calling GetClassSignature(): %d\n", error);
    return;
  }
  ++cl; // Ignore leading 'L'
  cl[strlen(cl) - 1] = '\0'; // Strip trailing ';'
  error = jvmti->GetMethodName(method, &name, &sig, NULL);
  if (error != JVMTI_ERROR_NONE) {
    printf("Error while calling GetMethodName(): %d\n", error);
    return;
  }
  fflush(stdout);
  fprintf(stdout, "==> %s: %s::%s%s at %p (%d bytes)\n", prefix, cl, name, sig, code_addr, code_size);
  fflush(stdout);
  jvmti->Deallocate((unsigned char*) name);
  jvmti->Deallocate((unsigned char*) sig);
  jvmti->Deallocate((unsigned char*) --cl);
}

void JNICALL CompiledMethodLoad(jvmtiEnv *jvmti_env,
                                jmethodID method,
                                jint code_size,
                                const void* code_addr,
                                jint map_length,
                                const jvmtiAddrLocationMap* map,
                                const void* compile_info) {
  printMethod(jvmti_env, method, code_addr, code_size, "CompiledMethodLoad");
}

void JNICALL CompiledMethodUnload(jvmtiEnv *jvmti_env,
                                  jmethodID method,
                                  const void* code_addr) {
  printMethod(jvmti_env, method, code_addr, 0, "CompiledMethodUnload");
}

void JNICALL DynamicCodeGenerated(jvmtiEnv *jvmti_env,
                                  const char* name,
                                  const void* address,
                                  jint length) {
  fprintf(stdout, "==> DynamicCodeGenerated: %s at %p (%d bytes)\n", name, address, length);
  fflush(stdout);
}

extern "C"
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
  fprintf(stdout, "Agent_OnLoad\n");
  int result = setupJVMTI(NULL, vm);
  if (result != JNI_OK) {
    return result;
  }

  jvmtiError error;

  jvmtiCapabilities capabilities = { 0 };
  capabilities.can_generate_compiled_method_load_events = 1;
  error = jvmti->AddCapabilities(&capabilities);
  if (error != JVMTI_ERROR_NONE) {
    printf("Can't add 'can_generate_compiled_method_load_events' capability: %d\n", error);
    return error;
  }

  jvmtiEventCallbacks callbacks = {0};
  callbacks.CompiledMethodLoad = CompiledMethodLoad;
  callbacks.CompiledMethodUnload = CompiledMethodUnload;
  callbacks.DynamicCodeGenerated = DynamicCodeGenerated;
  error = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
  if (error != JVMTI_ERROR_NONE) {
    printf("Can't set 'CompiledMethodLoad/CompiledMethodUnload/DynamicCodeGenerated' callbacks: %d\n", error);
    return error;
  }

  error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, NULL);
  if (error != JVMTI_ERROR_NONE) {
    printf("Can't set event notification for 'JVMTI_EVENT_DYNAMIC_CODE_GENERATED' : %d\n", error);
    return error;
  }
  jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
  if (error != JVMTI_ERROR_NONE) {
    printf("Can't set event notification for 'JVMTI_EVENT_COMPILED_METHOD_LOAD' : %d\n", error);
    return error;
  }
  jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, NULL);
  if (error != JVMTI_ERROR_NONE) {
    printf("Can't set event notification for 'JVMTI_EVENT_COMPILED_METHOD_UNLOAD' : %d\n", error);
    return error;
  }
  return JNI_OK;
}
