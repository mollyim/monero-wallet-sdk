#ifndef COMMON_JVM_NATIVE_H_
#define COMMON_JVM_NATIVE_H_

#include <string>
#include <vector>

#include "common/jvm.h"
#include "common/scoped_java_ref.h"

namespace monero {

// Returns a class local reference from a fully-qualified name.
// This method must be called on a context capable of loading Java classes.
jclass GetClass(JNIEnv* env, const char* name);

// Functions to query method IDs.
jmethodID GetMethodId(JNIEnv* env,
                      jclass clazz,
                      const char* name,
                      const char* signature);
jmethodID GetStaticMethodId(JNIEnv* env,
                            jclass clazz,
                            const char* name,
                            const char* signature);

// Call non-static functions.
void CallVoidMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...);
jint CallIntMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...);
jboolean CallBooleanMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...);
jobject CallObjectMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...);
jstring CallStringMethod(JNIEnv* env, jobject obj, jmethodID method_id, ...);

// Call static functions.
void CallStaticVoidMethod(JNIEnv* env, jclass clazz, jmethodID method_id, ...);
jobject CallStaticObjectMethod(JNIEnv* env, jclass clazz, jmethodID method_id, ...);

// Create a new object using a constructor.
jobject NewObject(JNIEnv* env, jclass clazz, jmethodID method_id, ...);

// --------------------------------------------------------
// -- Methods for converting Java types to native types. --
// --------------------------------------------------------

// Converts a (UTF-16) jstring to a native string in modified UTF-8 encoding.
std::string JavaToNativeString(JNIEnv* env, jstring j_string);

std::vector<char> JavaToNativeByteArray(JNIEnv* env, jbyteArray j_array);
std::vector<int32_t> JavaToNativeIntArray(JNIEnv* env, jintArray j_array);
std::vector<int64_t> JavaToNativeLongArray(JNIEnv* env, jlongArray j_array);

template<typename T, typename Java_T = jobject, typename Convert>
std::vector<T> JavaToNativeVector(JNIEnv* env,
                                  jobjectArray j_container,
                                  Convert convert) {
  std::vector<T> container;
  const jsize size = env->GetArrayLength(j_container);
  ThrowRuntimeErrorOnException(env);
  container.reserve(size);
  for (jsize i = 0; i < size; ++i) {
    container.emplace_back(convert(
        env, ScopedJavaLocalRef<Java_T>(
            env, (Java_T) env->GetObjectArrayElement(j_container, i)).obj()));
  }
  return container;
}
// --------------------------------------------------------
// -- Methods for converting native types to Java types. --
// --------------------------------------------------------

jstring NativeToJavaString(JNIEnv* env, const char* str);
jstring NativeToJavaString(JNIEnv* env, const std::string& str);

jbyteArray NativeToJavaByteArray(JNIEnv* env, const char* data, size_t size);
jlongArray NativeToJavaLongArray(JNIEnv* env, const int64_t* data, size_t size);
jlongArray NativeToJavaLongArray(JNIEnv* env, const uint64_t* data, size_t size);

jlong NativeToJavaPointer(void* ptr);

// Helper function for converting std::vector<T> into a Java array.
template<typename T, typename Convert>
jobjectArray NativeToJavaObjectArray(JNIEnv* env,
                                     const std::vector<T>& container,
                                     jclass clazz,
                                     Convert convert) {
  jobjectArray j_container = env->NewObjectArray(
      container.size(), clazz, nullptr);
  ThrowRuntimeErrorOnException(env);
  int i = 0;
  for (const T& element: container) {
    env->SetObjectArrayElement(j_container, i,
                               convert(env, element).obj());
    ++i;
  }
  return j_container;
}

jobjectArray NativeToJavaStringArray(JNIEnv* env,
                                     const std::vector<std::string>& container);

}  // namespace monero

#endif  // COMMON_JVM_NATIVE_H_
