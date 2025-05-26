#include "jni_cache.h"

namespace monero {

// im.molly.monero.sdk.mnemonics
jmethodID MoneroMnemonic_buildMnemonicFromJNI;
ScopedJavaGlobalRef<jclass> MoneroMnemonicClass;

void InitializeJniCache(JNIEnv* env) {
  jclass moneroMnemonic = GetClass(env, "im/molly/monero/sdk/mnemonics/MoneroMnemonic");

  MoneroMnemonic_buildMnemonicFromJNI = GetStaticMethodId(
      env, moneroMnemonic,
      "buildMnemonicFromJNI",
      "([B[BLjava/lang/String;)Lim/molly/monero/sdk/mnemonics/MnemonicCode;");

  MoneroMnemonicClass = ScopedJavaLocalRef<jclass>(env, moneroMnemonic);
}

}  // namespace monero
