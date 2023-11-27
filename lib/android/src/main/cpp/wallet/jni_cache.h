#ifndef WALLET_JNI_CACHE_H__
#define WALLET_JNI_CACHE_H__

#include "common/jvm.h"

namespace monero {

// Initialize various classes and method pointers cached for use in JNI.
void initializeJniCache(JNIEnv* env);

// im.molly.monero
extern ScopedJvmGlobalRef<jclass> TxInfoClass;
extern jmethodID HttpResponse_getBody;
extern jmethodID HttpResponse_getCode;
extern jmethodID HttpResponse_getContentType;
extern jmethodID Logger_logFromNative;
extern jmethodID TxInfo_ctor;
extern jmethodID WalletNative_callRemoteNode;
extern jmethodID WalletNative_onRefresh;
extern jmethodID WalletNative_onSuspendRefresh;

// android.os
extern jmethodID ParcelFileDescriptor_detachFd;

}  // namespace monero

#endif  // WALLET_JNI_CACHE_H__
