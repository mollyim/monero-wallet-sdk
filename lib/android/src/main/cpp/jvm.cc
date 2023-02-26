#include "jvm.h"

#include <sys/prctl.h>
#include <pthread.h>
#include <unistd.h>

namespace monero {

static JavaVM* g_jvm = nullptr;

static pthread_once_t g_jni_ptr_once = PTHREAD_ONCE_INIT;

// Key for per-thread JNI interface.  Non-NULL in threads attached to `g_jvm` by
// attachCurrentThread(), NULL in unattached threads and threads that
// were attached by the JVM because of a Java->native call.
static pthread_key_t g_jni_ptr;

// Return thread ID as a string.
static std::string getThreadId() {
  char buf[21];  // Big enough to hold decimal digits for 64-bits plus NULL
  snprintf(buf, sizeof(buf), "%lu", static_cast<long>(gettid()));
  return {buf};
}

// Return the current thread's name.
static std::string getThreadName() {
  char name[17] = {0};  // In Android thread's name can be up to 16 bytes long
  if (prctl(PR_GET_NAME, name) < 0) {
    return {"<noname>"};
  }
  return {name};
}

static JNIEnv* attachCurrentThread() {
  JNIEnv* env = nullptr;
  std::string name(getThreadName() + " - " + getThreadId());
  JavaVMAttachArgs args;
  args.version = JNI_VERSION_1_6;
  args.name = name.c_str();
  args.group = nullptr;
  LOG_FATAL_IF(g_jvm->AttachCurrentThread(&env, &args) != JNI_OK,
               "Failed to attach thread");
  LOG_FATAL_IF(pthread_setspecific(g_jni_ptr, env));
  return env;
}

// Return the `JNIEnv*` for the current thread.
JNIEnv* getJniEnv() {
  void* env = pthread_getspecific(g_jni_ptr);
  if (!env) {
    // Call GetEnv() first to detect if the thread already is attached.
    // This is done to avoid setting up `g_jni_ptr` on a Java thread.
    jint status = g_jvm->GetEnv(&env, JNI_VERSION_1_6);
    LOG_FATAL_IF(((env != nullptr) && (status != JNI_OK)) ||
                 ((env == nullptr) && (status != JNI_EDETACHED)),
                 "Unexpected GetEnv ret: %d:%p", status, env);
    if (status != JNI_OK) {
      // Can race safely. Attaching a thread that is already attached is a no-op.
      return attachCurrentThread();
    }
  }
  return reinterpret_cast<JNIEnv*>(env);
}

// This function only runs on threads where `g_jni_ptr` is non-NULL, meaning
// we were responsible for originally attaching the thread, so are responsible
// for detaching it now.
static void threadDestructor(void* prev_jni_ptr) {
  auto* env = reinterpret_cast<JNIEnv*>(prev_jni_ptr);
  if (!env) {
    return;
  }
  jint status = g_jvm->DetachCurrentThread();
  LOG_FATAL_IF(status != JNI_OK, "Failed to detach thread: %d", status);
}

static void createJniPtrKey() {
  LOG_FATAL_IF(pthread_key_create(&g_jni_ptr, &threadDestructor));
}

JNIEnv* initializeJvm(JavaVM* jvm, int version) {
  LOG_ASSERT(!g_jvm);
  g_jvm = jvm;
  LOG_ASSERT(g_jvm);

  LOG_FATAL_IF(pthread_once(&g_jni_ptr_once, &createJniPtrKey));

  void* env = nullptr;
  if (jvm->GetEnv(&env, version) != JNI_OK) {
    return nullptr;
  }

  return static_cast<JNIEnv*>(env);
}

jclass JvmRef<jobject>::getClass(JNIEnv* env) const {
  jclass clazz = env->GetObjectClass(obj());
  LOG_FATAL_IF(checkException(env));
  return clazz;
}

void JvmRef<jobject>::callVoidMethod(JNIEnv* env,
                                     jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  env->CallVoidMethodV(obj(), method_id, args);
  throwRuntimeErrorOnException(env);
}

jint JvmRef<jobject>::callIntMethod(JNIEnv* env,
                                    jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  jint ret = env->CallIntMethodV(obj(), method_id, args);
  throwRuntimeErrorOnException(env);
  return ret;
}

jboolean JvmRef<jobject>::callBooleanMethod(JNIEnv* env,
                                            jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  jboolean ret = env->CallBooleanMethodV(obj(), method_id, args);
  throwRuntimeErrorOnException(env);
  return ret;
}

jobject JvmRef<jobject>::callObjectMethod(JNIEnv* env,
                                          jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  jobject ret = env->CallObjectMethodV(obj(), method_id, args);
  throwRuntimeErrorOnException(env);
  return ret;
}

jstring JvmRef<jobject>::callStringMethod(JNIEnv* env,
                                          jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  auto ret = (jstring) env->CallObjectMethodV(obj(), method_id, args);
  throwRuntimeErrorOnException(env);
  return ret;
}

jmethodID JvmRef<jobject>::getMethodId(JNIEnv* env,
                                       const char* name,
                                       const char* signature) const {
  jmethodID ret = env->GetMethodID(getClass(env), name, signature);
  LOG_FATAL_IF(checkException(env),
               "Error during GetMethodID: %s, %s", name, signature);
  return ret;
}

jmethodID JvmRef<jobject>::getStaticMethodId(JNIEnv* env,
                                             const char* name,
                                             const char* signature) const {
  jmethodID ret = env->GetStaticMethodID(getClass(env), name, signature);
  LOG_FATAL_IF(checkException(env),
               "Error during GetStaticMethodID: %s, %s", name, signature);
  return ret;
}

void JvmRef<jclass>::callStaticVoidMethod(JNIEnv* env,
                                          jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  env->CallStaticVoidMethodV(obj(), method_id, args);
}

jobject JvmRef<jclass>::callStaticObjectMethod(JNIEnv* env,
                                               jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  return env->CallStaticObjectMethodV(obj(), method_id, args);
}

jobject JvmRef<jclass>::newObject(JNIEnv* env, jmethodID method_id, ...) const {
  va_list args;
  va_start(args, method_id);
  jobject new_obj = env->NewObjectV(obj(), method_id, args);
  LOG_FATAL_IF(checkException(env));
  LOG_ASSERT(new_obj);
  return new_obj;
}

ScopedJvmLocalRef<jclass> findClass(JNIEnv* env, const char* name) {
  ScopedJvmLocalRef<jclass> clazz(env, env->FindClass(name));
  LOG_FATAL_IF(clazz.is_null(), "%s", name);
  return clazz;
}

ScopedJvmLocalRef<jstring> nativeToJvmString(JNIEnv* env, const char* str) {
  jstring j_str = env->NewStringUTF(str);
  LOG_FATAL_IF(checkException(env));
  return {env, j_str};
}

ScopedJvmLocalRef<jstring> nativeToJvmString(JNIEnv* env, const std::string& str) {
  return nativeToJvmString(env, str.c_str());
}

ScopedJvmLocalRef<jbyteArray> nativeToJvmByteArray(JNIEnv* env,
                                                   const char* bytes,
                                                   size_t len) {
  LOG_FATAL_IF(len > INT_MAX);
  auto j_len = (jsize) len;
  jbyteArray j_array = env->NewByteArray(j_len);
  LOG_FATAL_IF(checkException(env));
  env->SetByteArrayRegion(j_array, 0, j_len,
                          reinterpret_cast<const jbyte*>(bytes));
  LOG_FATAL_IF(checkException(env));
  return {env, j_array};
}

ScopedJvmLocalRef<jbyteArray> nativeToJvmByteArray(
    JNIEnv* env,
    const std::vector<char>& bytes) {
  return nativeToJvmByteArray(env, bytes.data(), bytes.size());
}

jlong nativeToJvmPointer(void* ptr) {
  static_assert(sizeof(intptr_t) <= sizeof(jlong), "");
  return reinterpret_cast<intptr_t>(ptr);
}

std::string jvmToStdString(JNIEnv* env, const JvmRef<jstring>& j_string) {
  const char* chars = env->GetStringUTFChars(j_string.obj(), nullptr);
  LOG_FATAL_IF(checkException(env));
  const jsize len = env->GetStringUTFLength(j_string.obj());
  LOG_FATAL_IF(checkException(env));
  std::string str(chars, len);
  env->ReleaseStringUTFChars(j_string.obj(), chars);
  LOG_FATAL_IF(checkException(env));
  return str;
}

std::string jvmToStdString(JNIEnv* env, jstring j_string) {
  return jvmToStdString(env, JvmParamRef<jstring>(j_string));
}

std::vector<char> jvmToNativeByteArray(JNIEnv* env,
                                       const JvmRef<jbyteArray>& j_array) {
  const jsize len = env->GetArrayLength(j_array.obj());
  LOG_FATAL_IF(checkException(env));
  std::vector<char> v(len);
  env->GetByteArrayRegion(j_array.obj(), 0, len,
                          reinterpret_cast<jbyte*>(&v[0]));
  LOG_FATAL_IF(checkException(env));
  return v;
}



}  // namespace monero
