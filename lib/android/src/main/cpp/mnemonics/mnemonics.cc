#include "common/eraser.h"

#include "jni_cache.h"

#include "electrum-words.h"

namespace monero {

extern "C"
JNIEXPORT jobject JNICALL
Java_im_molly_monero_mnemonics_MoneroMnemonicKt_nativeElectrumWordsGenerateMnemonic(
    JNIEnv* env,
    jclass clazz,
    jbyteArray j_entropy,
    jstring j_language) {
  std::vector<char> entropy = jvmToNativeByteArray(env, JvmParamRef<jbyteArray>(j_entropy));
  Eraser entropy_eraser(entropy);

  std::string language = jvmToStdString(env, j_language);
  epee::wipeable_string words;

  bool success =
      crypto::ElectrumWords::bytes_to_words(entropy.data(),
                                            entropy.size(),
                                            words,
                                            language);
  if (!success) {
    return nullptr;
  }

  ScopedJvmLocalRef<jobject> j_mnemonic_code(
      env, MoneroMnemonicClass.callStaticObjectMethod(
          env, MoneroMnemonic_buildMnemonicFromNative,
          j_entropy,
          nativeToJvmByteArray(env, words.data(), words.size()).obj(),
          j_language
      )
  );

  return j_mnemonic_code.Release();
}

extern "C"
JNIEXPORT jobject JNICALL
Java_im_molly_monero_mnemonics_MoneroMnemonicKt_nativeElectrumWordsRecoverEntropy(
    JNIEnv* env,
    jclass clazz,
    jbyteArray j_source
) {
  std::vector<char> words = jvmToNativeByteArray(env, JvmParamRef<jbyteArray>(j_source));
  Eraser words_eraser(words);

  std::string language;
  epee::wipeable_string entropy, w_words(words.data(), words.size());

  bool success =
      crypto::ElectrumWords::words_to_bytes(w_words,
                                            entropy,
                                            0 /* len */,
                                            true /* duplicate */,
                                            language);
  if (!success) {
    return nullptr;
  }

  ScopedJvmLocalRef<jobject> j_mnemonic_code(
      env, MoneroMnemonicClass.callStaticObjectMethod(
          env, MoneroMnemonic_buildMnemonicFromNative,
          nativeToJvmByteArray(env, entropy.data(), entropy.size()).obj(),
          j_source,
          nativeToJvmString(env, language).obj()
      )
  );

  return j_mnemonic_code.Release();
}

}  // namespace monero
