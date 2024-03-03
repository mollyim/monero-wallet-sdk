#ifndef WALLET_JNI_CACHE_H__
#define WALLET_JNI_CACHE_H__

#include "common/jvm.h"
#include "common/java_native.h"

namespace monero {

// Initialize various classes and method pointers cached for use in JNI.
void InitializeJniCache(JNIEnv* env);

// im.molly.monero
extern jmethodID HttpResponse_getBody;
extern jmethodID HttpResponse_getCode;
extern jmethodID HttpResponse_getContentType;
extern jmethodID ITransferCallback_onTransferCreated;
extern jmethodID ITransferCallback_onTransferCommitted;
extern jmethodID ITransferCallback_onUnexpectedError;
extern jmethodID Logger_logFromNative;
extern jmethodID TxInfo_ctor;
extern jmethodID WalletNative_callRemoteNode;
extern jmethodID WalletNative_createPendingTransfer;
extern jmethodID WalletNative_onRefresh;
extern jmethodID WalletNative_onSuspendRefresh;
extern ScopedJavaGlobalRef<jclass> TxInfoClass;

// android.os
extern jmethodID ParcelFd_detachFd;

// java.lang
extern ScopedJavaGlobalRef<jclass> StringClass;

}  // namespace monero

#endif  // WALLET_JNI_CACHE_H__
