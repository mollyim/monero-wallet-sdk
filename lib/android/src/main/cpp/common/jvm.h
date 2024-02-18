#ifndef COMMON_JVM_H_
#define COMMON_JVM_H_

#include <jni.h>

#include <stdexcept>

namespace monero {

// Get a JNIEnv* usable on this thread, regardless of whether it's a native
// thread or a Java thread.  Attach the thread to the JVM if necessary.
JNIEnv* GetJniEnv();

// This method should be called from JNI_OnLoad.
JNIEnv* InitializeJvm(JavaVM* jvm, int version);

// Checks for any Java exception and clears currently thrown exception.
static bool inline CheckException(JNIEnv* env, bool log_exception = true) {
  if (env->ExceptionCheck()) {
    if (log_exception) env->ExceptionDescribe();
    env->ExceptionClear();
    return true;
  }
  return false;
}

static void inline ThrowRuntimeErrorOnException(JNIEnv* env) {
  if (CheckException(env, false)) {
    throw std::runtime_error("Uncaught exception returned from Java call");
  }
}

}  // namespace monero

#endif  // COMMON_JVM_H_
