#ifndef MNEMONICS_JNI_CACHE_H__
#define MNEMONICS_JNI_CACHE_H__

#include "common/jvm.h"

namespace monero {

// Initialize various classes and method pointers cached for use in JNI.
void initializeJniCache(JNIEnv* env);

// im.molly.monero.mnemonics
extern ScopedJvmGlobalRef<jclass> MoneroMnemonicClass;
extern jmethodID MoneroMnemonic_buildMnemonicFromNative;

}  // namespace monero

#endif  // MNEMONICS_JNI_CACHE_H__
