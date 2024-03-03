#include "jni_cache.h"

namespace monero {

// im.molly.monero
jmethodID HttpResponse_getBody;
jmethodID HttpResponse_getCode;
jmethodID HttpResponse_getContentType;
jmethodID ITransferCallback_onTransferCreated;
jmethodID ITransferCallback_onTransferCommitted;
jmethodID ITransferCallback_onUnexpectedError;
jmethodID Logger_logFromNative;
jmethodID TxInfo_ctor;
jmethodID WalletNative_createPendingTransfer;
jmethodID WalletNative_callRemoteNode;
jmethodID WalletNative_onRefresh;
jmethodID WalletNative_onSuspendRefresh;
ScopedJavaGlobalRef<jclass> TxInfoClass;

// android.os
jmethodID ParcelFd_detachFd;

// java.lang
ScopedJavaGlobalRef<jclass> StringClass;

void InitializeJniCache(JNIEnv* env) {
  jclass httpResponse = GetClass(env, "im/molly/monero/HttpResponse");
  jclass iTransferCallback = GetClass(env, "im/molly/monero/ITransferCallback");
  jclass logger = GetClass(env, "im/molly/monero/Logger");
  jclass txInfo = GetClass(env, "im/molly/monero/internal/TxInfo");
  jclass walletNative = GetClass(env, "im/molly/monero/WalletNative");
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
      "onTransferCreated", "(Lim/molly/monero/IPendingTransfer;)V");
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
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;JIIJJJJZZ)V");
  WalletNative_createPendingTransfer = GetMethodId(
      env, walletNative,
      "createPendingTransfer",
      "(JJJI)Lim/molly/monero/IPendingTransfer;");
  WalletNative_callRemoteNode = GetMethodId(
      env, walletNative,
      "callRemoteNode",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B)Lim/molly/monero/HttpResponse;");
  WalletNative_onRefresh = GetMethodId(
      env, walletNative,
      "onRefresh", "(IJZ)V");
  WalletNative_onSuspendRefresh = GetMethodId(
      env, walletNative,
      "onSuspendRefresh", "(Z)V");

  TxInfoClass = ScopedJavaLocalRef<jclass>(env, txInfo);

  ParcelFd_detachFd = GetMethodId(env, parcelFd, "detachFd", "()I");

  StringClass = ScopedJavaLocalRef<jclass>(env, GetClass(env, "java/lang/String"));
}

}  // namespace monero
