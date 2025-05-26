#include "jni_cache.h"

namespace monero {

// im.molly.monero.sdk
jmethodID HttpResponse_getBody;
jmethodID HttpResponse_getCode;
jmethodID HttpResponse_getContentType;
jmethodID ITransferCallback_onTransferCreated;
jmethodID ITransferCallback_onTransferCommitted;
jmethodID ITransferCallback_onUnexpectedError;
jmethodID Logger_logFromNative;
jmethodID TxInfo_ctor;
jmethodID NativeWallet_createPendingTransfer;
jmethodID NativeWallet_callRemoteNode;
jmethodID NativeWallet_onRefresh;
jmethodID NativeWallet_onSuspendRefresh;
ScopedJavaGlobalRef<jclass> TxInfoClass;

// android.os
jmethodID ParcelFd_detachFd;

// java.lang
ScopedJavaGlobalRef<jclass> StringClass;

void InitializeJniCache(JNIEnv* env) {
  jclass httpResponse = GetClass(env, "im/molly/monero/sdk/internal/HttpResponse");
  jclass iTransferCallback = GetClass(env, "im/molly/monero/sdk/internal/ITransferCallback");
  jclass logger = GetClass(env, "im/molly/monero/sdk/internal/Logger");
  jclass txInfo = GetClass(env, "im/molly/monero/sdk/internal/TxInfo");
  jclass nativeWallet = GetClass(env, "im/molly/monero/sdk/internal/NativeWallet");
  jclass parcelFd = GetClass(env, "android/os/ParcelFileDescriptor");

  HttpResponse_getBody = GetMethodId(
      env, httpResponse,
      "getBody", "()Landroid/os/ParcelFileDescriptor;");
  HttpResponse_getCode = GetMethodId(
      env, httpResponse,
      "getCode", "()I");
  HttpResponse_getContentType = GetMethodId(
      env, httpResponse,
      "getContentType", "()Ljava/lang/String;");
  ITransferCallback_onTransferCreated = GetMethodId(
      env, iTransferCallback,
      "onTransferCreated", "(Lim/molly/monero/sdk/internal/IPendingTransfer;)V");
  ITransferCallback_onTransferCommitted = GetMethodId(
      env, iTransferCallback,
      "onTransferCommitted", "()V");
  ITransferCallback_onUnexpectedError = GetMethodId(
      env, iTransferCallback,
      "onUnexpectedError", "(Ljava/lang/String;)V");
  Logger_logFromNative = GetMethodId(
      env, logger,
      "logFromNative", "(ILjava/lang/String;Ljava/lang/String;)V");
  TxInfo_ctor = GetMethodId(
      env, txInfo,
      "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;JIJJJJBZZ)V");
  NativeWallet_createPendingTransfer = GetMethodId(
      env, nativeWallet,
      "createPendingTransfer",
      "(JJJI)Lim/molly/monero/sdk/internal/IPendingTransfer;");
  NativeWallet_callRemoteNode = GetMethodId(
      env, nativeWallet,
      "callRemoteNode",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B)Lim/molly/monero/sdk/internal/HttpResponse;");
  NativeWallet_onRefresh = GetMethodId(
      env, nativeWallet,
      "onRefresh", "(IJZ)V");
  NativeWallet_onSuspendRefresh = GetMethodId(
      env, nativeWallet,
      "onSuspendRefresh", "(Z)V");

  TxInfoClass = ScopedJavaLocalRef<jclass>(env, txInfo);

  ParcelFd_detachFd = GetMethodId(env, parcelFd, "detachFd", "()I");

  StringClass = ScopedJavaLocalRef<jclass>(env, GetClass(env, "java/lang/String"));
}

}  // namespace monero
