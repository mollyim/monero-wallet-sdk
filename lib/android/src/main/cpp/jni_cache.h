#ifndef JNI_CACHE_H_
#define JNI_CACHE_H_

#include "jvm.h"

namespace monero {

// Initialize various classes and method pointers cached for use in JNI.
void initializeJniCache(JNIEnv* env);

// im.molly.monero
extern ScopedJvmGlobalRef<jclass> OwnedTxOut;
extern jmethodID HttpResponse_getBody;
extern jmethodID HttpResponse_getCode;
extern jmethodID HttpResponse_getContentType;
extern jmethodID Logger_logFromNative;
extern jmethodID OwnedTxOut_ctor;
extern jmethodID WalletNative_callRemoteNode;
extern jmethodID WalletNative_onRefresh;
extern jmethodID WalletNative_onSuspendRefresh;

// android.os
extern jmethodID ParcelFileDescriptor_detachFd;

}  // namespace monero

#endif  // JNI_CACHE_H_
