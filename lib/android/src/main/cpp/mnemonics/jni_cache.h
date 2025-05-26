#ifndef MNEMONICS_JNI_CACHE_H__
#define MNEMONICS_JNI_CACHE_H__

#include "common/java_native.h"

namespace monero {

// Initialize various classes and method pointers cached for use in JNI.
void InitializeJniCache(JNIEnv* env);

// im.molly.monero.sdk.mnemonics
extern jmethodID MoneroMnemonic_buildMnemonicFromJNI;
extern ScopedJavaGlobalRef<jclass> MoneroMnemonicClass;

}  // namespace monero

#endif  // MNEMONICS_JNI_CACHE_H__
