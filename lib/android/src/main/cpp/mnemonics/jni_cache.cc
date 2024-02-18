#include "jni_cache.h"

namespace monero {

// im.molly.monero.mnemonics
jmethodID MoneroMnemonic_buildMnemonicFromJNI;
ScopedJavaGlobalRef<jclass> MoneroMnemonicClass;

void InitializeJniCache(JNIEnv* env) {
  jclass moneroMnemonic = GetClass(env, "im/molly/monero/mnemonics/MoneroMnemonic");

  MoneroMnemonic_buildMnemonicFromJNI = GetStaticMethodId(
      env, moneroMnemonic,
      "buildMnemonicFromJNI",
      "([B[BLjava/lang/String;)Lim/molly/monero/mnemonics/MnemonicCode;");

  MoneroMnemonicClass = ScopedJavaLocalRef<jclass>(env, moneroMnemonic);
}

}  // namespace monero
