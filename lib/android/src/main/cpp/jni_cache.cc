#include "jni_cache.h"

namespace monero {

// im.molly.monero
ScopedJvmGlobalRef<jclass> TxInfoClass;
jmethodID HttpResponse_getBody;
jmethodID HttpResponse_getCode;
jmethodID HttpResponse_getContentType;
jmethodID Logger_logFromNative;
jmethodID TxInfo_ctor;
jmethodID WalletNative_callRemoteNode;
jmethodID WalletNative_onRefresh;
jmethodID WalletNative_onSuspendRefresh;

// android.os
jmethodID ParcelFileDescriptor_detachFd;

void initializeJniCache(JNIEnv* env) {
  // im.molly.monero
  auto httpResponse = findClass(env, "im/molly/monero/HttpResponse");
  auto logger = findClass(env, "im/molly/monero/Logger");
  auto txInfoClass = findClass(env, "im/molly/monero/internal/TxInfo");
  auto walletNative = findClass(env, "im/molly/monero/WalletNative");

  TxInfoClass = txInfoClass;

  HttpResponse_getBody = httpResponse
      .getMethodId(env, "getBody", "()Landroid/os/ParcelFileDescriptor;");
  HttpResponse_getCode = httpResponse
      .getMethodId(env, "getCode", "()I");
  HttpResponse_getContentType = httpResponse
      .getMethodId(env, "getContentType", "()Ljava/lang/String;");
  Logger_logFromNative = logger
      .getMethodId(env, "logFromNative", "(ILjava/lang/String;Ljava/lang/String;)V");
  TxInfo_ctor = txInfoClass
      .getMethodId(env, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;JIIJJJJZZ)V");
  WalletNative_callRemoteNode = walletNative
      .getMethodId(env,
                   "callRemoteNode",
                   "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B)Lim/molly/monero/HttpResponse;");
  WalletNative_onRefresh = walletNative
      .getMethodId(env, "onRefresh", "(IZ)V");
  WalletNative_onSuspendRefresh = walletNative
      .getMethodId(env, "onSuspendRefresh", "(Z)V");

  // android.os
  auto parcelFileDescriptor = findClass(env, "android/os/ParcelFileDescriptor");

  ParcelFileDescriptor_detachFd = parcelFileDescriptor
      .getMethodId(env, "detachFd", "()I");
}

}  // namespace monero
