#include "transfer.h"

#include "common/debug.h"

#include "jni_cache.h"

namespace monero {

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_internal_NativeWallet_00024NativePendingTransfer_nativeDispose(
    JNIEnv* env,
    jobject thiz,
    jlong transfer_handle) {
  delete reinterpret_cast<PendingTransfer*>(transfer_handle);
}

}  // namespace monero
