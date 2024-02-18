#include "jni_cache.h"

#include "common/jvm.h"

namespace monero {

extern "C"
JNIEXPORT jint
JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env = InitializeJvm(vm, JNI_VERSION_1_6);
  if (env == nullptr) {
    return JNI_ERR;
  }

  InitializeJniCache(env);

  return JNI_VERSION_1_6;
}

}  // namespace monero
