#include "java_native.h"

#include "debug.h"

namespace monero {

extern ScopedJavaGlobalRef<jclass> StringClass;

jclass GetClass(JNIEnv* env, const char* name) {
  jclass clazz = env->FindClass(name);
  LOG_FATAL_IF(!clazz, "Java class not found: %s", name);
  return clazz;
}

jmethodID GetMethodId(JNIEnv* env,
                      jclass clazz,
                      const char* name,
                      const char* signature) {
  jmethodID ret = env->GetMethodID(clazz, name, signature);
  LOG_FATAL_IF(CheckException(env),
               "Error during GetMethodID: %s, %s", name, signature);
  return ret;
}

jmethodID GetStaticMethodId(JNIEnv* env,
                            jclass clazz,
                            const char* name,
                            const char* signature) {
  jmethodID ret = env->GetStaticMethodID(clazz, name, signature);
  LOG_FATAL_IF(CheckException(env),
               "Error during GetStaticMethodID: %s, %s", name, signature);
  return ret;
}

void CallVoidMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  env->CallVoidMethodV(obj, method_id, args);
  ThrowRuntimeErrorOnException(env);
}

jint CallIntMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  jint ret = env->CallIntMethodV(obj, method_id, args);
  ThrowRuntimeErrorOnException(env);
  return ret;
}

jboolean CallBooleanMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  jboolean ret = env->CallBooleanMethodV(obj, method_id, args);
  ThrowRuntimeErrorOnException(env);
  return ret;
}

jobject CallObjectMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  jobject ret = env->CallObjectMethodV(obj, method_id, args);
  ThrowRuntimeErrorOnException(env);
  return ret;
}

jstring CallStringMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  jstring ret = (jstring) env->CallObjectMethodV(obj, method_id, args);
  ThrowRuntimeErrorOnException(env);
  return ret;
}

void CallStaticVoidMethod(JNIEnv* env, jclass clazz, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  env->CallStaticVoidMethodV(clazz, method_id, args);
  ThrowRuntimeErrorOnException(env);
}

jobject CallStaticObjectMethod(JNIEnv* env, jclass clazz, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  jobject ret = env->CallStaticObjectMethodV(clazz, method_id, args);
  ThrowRuntimeErrorOnException(env);
  return ret;
}

jobject NewObject(JNIEnv* env, jclass clazz, jmethodID method_id, ...) {
  va_list args;
  va_start(args, method_id);
  jobject new_obj = env->NewObjectV(clazz, method_id, args);
  ThrowRuntimeErrorOnException(env);
  LOG_ASSERT(new_obj);
  return new_obj;
}

std::string JavaToNativeString(JNIEnv* env, jstring j_string) {
  const char* chars = env->GetStringUTFChars(j_string, /*isCopy=*/nullptr);
  LOG_FATAL_IF(CheckException(env));
  const jsize j_len = env->GetStringUTFLength(j_string);
  LOG_FATAL_IF(CheckException(env));
  std::string str(chars, j_len);
  env->ReleaseStringUTFChars(j_string, chars);
  LOG_FATAL_IF(CheckException(env));
  return str;
}

std::vector<char> JavaToNativeByteArray(JNIEnv* env, jbyteArray j_array) {
  const jsize len = env->GetArrayLength(j_array);
  LOG_FATAL_IF(CheckException(env));
  std::vector<char> v(len);
  env->GetByteArrayRegion(j_array, 0, len,
                          reinterpret_cast<jbyte*>(&v[0]));
  LOG_FATAL_IF(CheckException(env));
  return v;
}

std::vector<int32_t> JavaToNativeIntArray(JNIEnv* env, jintArray j_array) {
  const jsize len = env->GetArrayLength(j_array);
  LOG_FATAL_IF(CheckException(env));
  std::vector<int32_t> v(len);
  env->GetIntArrayRegion(j_array, 0, len,
                         reinterpret_cast<jint*>(&v[0]));
  LOG_FATAL_IF(CheckException(env));
  return v;
}

std::vector<int64_t> JavaToNativeLongArray(JNIEnv* env, jlongArray j_array) {
  const jsize len = env->GetArrayLength(j_array);
  LOG_FATAL_IF(CheckException(env));
  std::vector<int64_t> v(len);
  env->GetLongArrayRegion(j_array, 0, len,
                          reinterpret_cast<jlong*>(&v[0]));
  LOG_FATAL_IF(CheckException(env));
  return v;
}

jstring NativeToJavaString(JNIEnv* env, const char* str) {
  jstring j_str = env->NewStringUTF(str);
  LOG_FATAL_IF(CheckException(env));
  return j_str;
}

jstring NativeToJavaString(JNIEnv* env, const std::string& str) {
  return NativeToJavaString(env, str.c_str());
}

jbyteArray NativeToJavaByteArray(JNIEnv* env,
                                 const char* data,
                                 size_t size) {
  LOG_FATAL_IF(size > INT_MAX);
  const jsize j_len = (jsize) size;
  jbyteArray j_array = env->NewByteArray(j_len);
  LOG_FATAL_IF(CheckException(env));
  env->SetByteArrayRegion(j_array, 0, j_len,
                          reinterpret_cast<const jbyte*>(data));
  LOG_FATAL_IF(CheckException(env));
  return j_array;
}

jlongArray NativeToJavaLongArray(JNIEnv* env,
                                 const int64_t* data,
                                 size_t size) {
  LOG_FATAL_IF(size > INT_MAX);
  const jsize j_len = (jsize) size;
  jlongArray j_array = env->NewLongArray(j_len);
  LOG_FATAL_IF(CheckException(env));
  env->SetLongArrayRegion(j_array, 0, j_len, data);
  LOG_FATAL_IF(CheckException(env));
  return j_array;
}

jlongArray NativeToJavaLongArray(JNIEnv* env,
                                 const uint64_t* data,
                                 size_t size) {
  return NativeToJavaLongArray(env, (int64_t*) data, size);
}

jlong NativeToJavaPointer(void* ptr) {
  static_assert(sizeof(intptr_t) <= sizeof(jlong), "");
  return reinterpret_cast<intptr_t>(ptr);
}

jobjectArray NativeToJavaStringArray(JNIEnv* env, const std::vector<std::string>& container) {
  auto convert_function = [](JNIEnv* env, const std::string& str) {
    return ScopedJavaLocalRef<jstring>(env, NativeToJavaString(env, str));
  };
  return NativeToJavaObjectArray(env, container, StringClass.obj(), convert_function);
}

}  // namespace monero
