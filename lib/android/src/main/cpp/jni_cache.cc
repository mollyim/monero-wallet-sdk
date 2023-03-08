#include "jni_cache.h"

namespace monero {

// im.molly.monero
ScopedJvmGlobalRef<jclass> OwnedTxOut;
jmethodID HttpResponse_getBody;
jmethodID HttpResponse_getCode;
jmethodID HttpResponse_getContentType;
jmethodID Logger_logFromNative;
jmethodID OwnedTxOut_ctor;
jmethodID WalletNative_callRemoteNode;
jmethodID WalletNative_onRefresh;
jmethodID WalletNative_onSuspendRefresh;

// android.os
jmethodID ParcelFileDescriptor_detachFd;

void initializeJniCache(JNIEnv* env) {
  // im.molly.monero
  auto httpResponse = findClass(env, "im/molly/monero/HttpResponse");
  auto logger = findClass(env, "im/molly/monero/Logger");
  auto ownedTxOut = findClass(env, "im/molly/monero/OwnedTxOut");
  auto walletNative = findClass(env, "im/molly/monero/WalletNative");

  HttpResponse_getBody = httpResponse
      .getMethodId(env, "getBody", "()Landroid/os/ParcelFileDescriptor;");
  HttpResponse_getCode = httpResponse
      .getMethodId(env, "getCode", "()I");
  HttpResponse_getContentType = httpResponse
      .getMethodId(env, "getContentType", "()Ljava/lang/String;");
  Logger_logFromNative = logger
      .getMethodId(env, "logFromNative", "(ILjava/lang/String;Ljava/lang/String;)V");
  OwnedTxOut_ctor = ownedTxOut
      .getMethodId(env, "<init>", "([BJJJ)V");
  WalletNative_callRemoteNode = walletNative
      .getMethodId(env,
                   "callRemoteNode",
                   "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B)Lim/molly/monero/HttpResponse;");
  WalletNative_onRefresh = walletNative
      .getMethodId(env, "onRefresh", "(JZ)V");
  WalletNative_onSuspendRefresh = walletNative
      .getMethodId(env, "onSuspendRefresh", "(Z)V");

  OwnedTxOut = ownedTxOut;

  // android.os
  auto parcelFileDescriptor = findClass(env, "android/os/ParcelFileDescriptor");

  ParcelFileDescriptor_detachFd = parcelFileDescriptor
      .getMethodId(env, "detachFd", "()I");
}

}  // namespace monero
