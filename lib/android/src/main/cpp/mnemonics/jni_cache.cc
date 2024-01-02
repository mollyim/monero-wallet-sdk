#include "jni_cache.h"

namespace monero {

// im.molly.monero.mnemonics
ScopedJvmGlobalRef<jclass> MoneroMnemonicClass;
jmethodID MoneroMnemonic_buildMnemonicFromNative;

void initializeJniCache(JNIEnv* env) {
  // im.molly.monero.mnemonics
  auto moneroMnemonicClass = findClass(env, "im/molly/monero/mnemonics/MoneroMnemonic");

  MoneroMnemonic_buildMnemonicFromNative = moneroMnemonicClass
      .getStaticMethodId(env,
                         "buildMnemonicFromNative",
                         "([B[BLjava/lang/String;)Lim/molly/monero/mnemonics/MnemonicCode;");

  MoneroMnemonicClass = moneroMnemonicClass;
}

}  // namespace monero
