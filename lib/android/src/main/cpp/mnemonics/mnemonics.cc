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
  std::vector<char> entropy = JavaToNativeByteArray(env, j_entropy);
  Eraser entropy_eraser(entropy);

  std::string language = JavaToNativeString(env, j_language);

  epee::wipeable_string words;
  bool success =
      crypto::ElectrumWords::bytes_to_words(entropy.data(), entropy.size(),
                                            words, language);
  if (!success) {
    return nullptr;
  }

  jobject j_mnemonic_code = CallStaticObjectMethod(
      env, MoneroMnemonicClass.obj(),
      MoneroMnemonic_buildMnemonicFromJNI,
      j_entropy,
      NativeToJavaByteArray(env, words.data(), words.size()),
      j_language);

  return j_mnemonic_code;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_im_molly_monero_mnemonics_MoneroMnemonicKt_nativeElectrumWordsRecoverEntropy(
    JNIEnv* env,
    jclass clazz,
    jbyteArray j_source
) {
  std::vector<char> words = JavaToNativeByteArray(env, j_source);
  Eraser words_eraser(words);

  epee::wipeable_string entropy, w_words(words.data(), words.size());
  std::string language;
  bool success =
      crypto::ElectrumWords::words_to_bytes(w_words,
                                            entropy,
                                            0,    /* len */
                                            true, /* duplicate */
                                            language);
  if (!success) {
    return nullptr;
  }

  jobject j_mnemonic_code = CallStaticObjectMethod(
      env, MoneroMnemonicClass.obj(),
      MoneroMnemonic_buildMnemonicFromJNI,
      NativeToJavaByteArray(env, entropy.data(), entropy.size()),
      j_source,
      NativeToJavaString(env, language));

  return j_mnemonic_code;
}

}  // namespace monero
