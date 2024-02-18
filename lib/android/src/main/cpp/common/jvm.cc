#include "jvm.h"

#include <string>
#include <sys/prctl.h>
#include <pthread.h>
#include <unistd.h>

#include "debug.h"

namespace monero {

static JavaVM* g_jvm = nullptr;

static pthread_once_t g_jni_ptr_once = PTHREAD_ONCE_INIT;

// Key for per-thread JNI interface.  Non-NULL in threads attached to `g_jvm` by
// attachCurrentThread(), NULL in unattached threads and threads that
// were attached by the JVM because of a Java->native call.
static pthread_key_t g_jni_ptr;

// Return thread ID as a string.
static std::string GetThreadId() {
  char buf[21];  // Big enough to hold decimal digits for 64-bits plus NULL
  snprintf(buf, sizeof(buf), "%lu", static_cast<long>(gettid()));
  return {buf};
}

// Return the current thread's name.
static std::string GetThreadName() {
  char name[17] = {0};  // In Android thread's name can be up to 16 bytes long
  if (prctl(PR_GET_NAME, name) < 0) {
    return {"<noname>"};
  }
  return {name};
}

static JNIEnv* AttachCurrentThread() {
  JNIEnv* env = nullptr;
  std::string name(GetThreadName() + " - " + GetThreadId());
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
JNIEnv* GetJniEnv() {
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
      return AttachCurrentThread();
    }
  }
  return reinterpret_cast<JNIEnv*>(env);
}

// This function only runs on threads where `g_jni_ptr` is non-NULL, meaning
// we were responsible for originally attaching the thread, so are responsible
// for detaching it now.
static void ThreadDestructor(void* prev_jni_ptr) {
  auto* env = reinterpret_cast<JNIEnv*>(prev_jni_ptr);
  if (!env) {
    return;
  }
  jint status = g_jvm->DetachCurrentThread();
  LOG_FATAL_IF(status != JNI_OK, "Failed to detach thread: %d", status);
}

static void CreateJniPtrKey() {
  LOG_FATAL_IF(pthread_key_create(&g_jni_ptr, &ThreadDestructor));
}

JNIEnv* InitializeJvm(JavaVM* jvm, int version) {
  LOG_ASSERT(!g_jvm);
  g_jvm = jvm;
  LOG_ASSERT(g_jvm);

  LOG_FATAL_IF(pthread_once(&g_jni_ptr_once, &CreateJniPtrKey));

  void* env = nullptr;
  if (jvm->GetEnv(&env, version) != JNI_OK) {
    return nullptr;
  }

  return static_cast<JNIEnv*>(env);
}

}  // namespace monero
