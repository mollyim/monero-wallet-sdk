#include "common/jvm.h"

namespace monero {

extern "C"
JNIEXPORT jint
JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env = initializeJvm(vm, JNI_VERSION_1_6);
  if (env == nullptr) {
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}

}  // namespace monero
