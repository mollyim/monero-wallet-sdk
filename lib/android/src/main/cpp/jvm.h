#ifndef JVM_H_
#define JVM_H_

#include <jni.h>

#include <stdexcept>
#include <string>
#include <vector>

#include "common.h"

namespace monero {

// Get a JNIEnv* usable on this thread, regardless of whether it's a native
// thread or a Java thread.  Attach the thread to the JVM if necessary.
JNIEnv* getJniEnv();

// This method should be called from JNI_OnLoad.
JNIEnv* initializeJvm(JavaVM* jvm, int version);

// Checks for any Java exception and clears currently thrown exception.
static bool inline checkException(JNIEnv* env, bool log_exception = true) {
  if (env->ExceptionCheck()) {
    if (log_exception) env->ExceptionDescribe();
    env->ExceptionClear();
    return true;
  }
  return false;
}

static void inline throwRuntimeErrorOnException(JNIEnv* env) {
  if (checkException(env, false)) {
    throw std::runtime_error("Uncaught exception returned from Java call");
  }
}

// --------------------------------------------------------------------------
// Originally these classes are from WebRTC and Chromium.
// --------------------------------------------------------------------------

// Generic base class for ScopedJvmLocalRef and ScopedJvmGlobalRef.
template<typename T>
class JvmRef;

// Template specialization of JvmRef, which acts as the base class for all
// other JvmRef<> template types. This allows you to e.g. pass JvmRef<jstring>
// into a function taking const JvmRef<jobject>&.
template<>
class JvmRef<jobject> {
 public:
  virtual jobject obj() const { return m_obj; }
  bool is_null() const {
    // This is not valid for weak references. For weak references first make a
    // ScopedJvmLocalRef or ScopedJvmGlobalRef before checking if it is null.
    return m_obj == nullptr;
  }

  virtual jclass getClass(JNIEnv* env) const;

  jmethodID getMethodId(JNIEnv* env,
                        const char* name,
                        const char* signature) const;
  jmethodID getStaticMethodId(JNIEnv* env,
                              const char* name,
                              const char* signature) const;
  void callVoidMethod(JNIEnv* env, jmethodID method_id, ...) const;
  jint callIntMethod(JNIEnv* env, jmethodID method_id, ...) const;
  jboolean callBooleanMethod(JNIEnv* env, jmethodID method_id, ...) const;
  jobject callObjectMethod(JNIEnv* env, jmethodID method_id, ...) const;
  jstring callStringMethod(JNIEnv* env, jmethodID method_id, ...) const;

 protected:
  constexpr JvmRef() : m_obj(nullptr) {}
  explicit JvmRef(jobject obj) : m_obj(obj) {}
  jobject m_obj;

 private:
  JvmRef(const JvmRef&) = delete;
  JvmRef& operator=(const JvmRef&) = delete;
};

template<typename T>
class JvmRef : public JvmRef<jobject> {
 public:
  T obj() const { return static_cast<T>(m_obj); }

 protected:
  JvmRef() : JvmRef<jobject>(nullptr) {}
  explicit JvmRef(T obj) : JvmRef<jobject>(obj) {}

 private:
  JvmRef(const JvmRef&) = delete;
  JvmRef& operator=(const JvmRef&) = delete;
};

// Template specialization of JvmRef for Java's Class objects.
template<>
class JvmRef<jclass> : public JvmRef<jobject> {
 public:
  jclass obj() const { return static_cast<jclass>(m_obj); }

  jclass getClass() const { return obj(); }

  jclass getClass(JNIEnv* env) const { return getClass(); }

  void callStaticVoidMethod(JNIEnv* env, jmethodID method_id, ...) const;
  jobject callStaticObjectMethod(JNIEnv* env, jmethodID method_id, ...) const;

  jobject newObject(JNIEnv* env, jmethodID method_id, ...) const;

 protected:
  constexpr JvmRef() noexcept: JvmRef<jobject>() {}
  explicit JvmRef(jclass obj) : JvmRef<jobject>(obj) {}

 private:
  JvmRef(const JvmRef&) = delete;
  JvmRef& operator=(const JvmRef&) = delete;
};

// Holds a local reference to a JNI method parameter.
template<typename T>
class JvmParamRef : public JvmRef<T> {
 public:
  // Assumes that `obj` is a parameter passed to a JNI method from Java.
  // Does not assume ownership as parameters should not be deleted.
  explicit JvmParamRef(T obj) : JvmRef<T>(obj) {}
//  JvmParamRef(JNIEnv*, T obj) : JvmRef<T>(obj) {}

 private:
  JvmParamRef(const JvmParamRef&) = delete;
  JvmParamRef& operator=(const JvmParamRef&) = delete;
};

// Holds a local reference to a Java object. The local reference is scoped
// to the lifetime of this object.
// Instances of this class may hold onto any JNIEnv passed into it until
// destroyed. Therefore, since a JNIEnv is only suitable for use on a single
// thread, objects of this class must be created, used, and destroyed, on a
// single thread.
// Therefore, this class should only be used as a stack-based object and from a
// single thread. If you wish to have the reference outlive the current
// callstack (e.g. as a class member) or you wish to pass it across threads,
// use a ScopedJvmGlobalRef instead.
template<typename T>
class ScopedJvmLocalRef : public JvmRef<T> {
 public:
  constexpr ScopedJvmLocalRef() : m_env(nullptr) {}
  explicit constexpr ScopedJvmLocalRef(std::nullptr_t) : m_env(nullptr) {}

  ScopedJvmLocalRef(JNIEnv* env, const JvmRef<T>& other) : m_env(env) {
    Reset(other.obj(), OwnershipPolicy::RETAIN);
  }
  // Allow constructing e.g. ScopedJvmLocalRef<jobject> from
  // ScopedJvmLocalRef<jstring>.
  template<typename G>
  ScopedJvmLocalRef(ScopedJvmLocalRef<G>&& other) : m_env(other.m_env) {
    Reset(other.Release(), OwnershipPolicy::ADOPT);
  }
  ScopedJvmLocalRef(const ScopedJvmLocalRef& other) : m_env(other.m_env) {
    Reset(other.obj(), OwnershipPolicy::RETAIN);
  }

  // Assumes that `obj` is a reference to a Java object and takes
  // ownership of this reference.  This should preferably not be used
  // outside of JNI helper functions.
  ScopedJvmLocalRef(JNIEnv* env, T obj) : JvmRef<T>(obj), m_env(env) {}

  ~ScopedJvmLocalRef() {
    if (m_obj != nullptr)
      m_env->DeleteLocalRef(m_obj);
  }

  void operator=(const ScopedJvmLocalRef& other) {
    Reset(other.obj(), OwnershipPolicy::RETAIN);
  }
  void operator=(ScopedJvmLocalRef&& other) {
    Reset(other.Release(), OwnershipPolicy::ADOPT);
  }

  // Releases the reference to the caller. The caller *must* delete the
  // reference when it is done with it. Note that calling a Java method
  // is *not* a transfer of ownership and Release() should not be used.
  T Release() {
    T obj = static_cast<T>(m_obj);
    m_obj = nullptr;
    return obj;
  }

 private:
  using JvmRef<T>::m_obj;

  enum OwnershipPolicy {
    // The scoped object takes ownership of an object by taking over an existing
    // ownership claim.
    ADOPT,
    // The scoped object will retain the the object and any initial ownership is
    // not changed.
    RETAIN
  };

  void Reset(T obj, OwnershipPolicy policy) {
    if (m_obj != nullptr)
      m_env->DeleteLocalRef(m_obj);
    m_obj = (obj != nullptr && policy == OwnershipPolicy::RETAIN)
            ? m_env->NewLocalRef(obj)
            : obj;
  }

  // This class is only good for use on the thread it was created on so
  // it's safe to cache the non-threadsafe JNIEnv* inside this object.
  JNIEnv* const m_env;
};

// Holds a global reference to a Java object. The global reference is scoped
// to the lifetime of this object. This class does not hold onto any JNIEnv*
// passed to it, hence it is safe to use across threads (within the constraints
// imposed by the underlying Java object that it references).
template<typename T>
class ScopedJvmGlobalRef : public JvmRef<T> {
 public:
  using JvmRef<T>::m_obj;

  ScopedJvmGlobalRef() = default;
  explicit constexpr ScopedJvmGlobalRef(std::nullptr_t) {}
  ScopedJvmGlobalRef(JNIEnv* env, const JvmRef<T>& other)
      : JvmRef<T>(static_cast<T>(env->NewGlobalRef(other.obj()))) {}
  explicit ScopedJvmGlobalRef(const ScopedJvmLocalRef<T>& other)
      : ScopedJvmGlobalRef(other.env(), other) {}
  ScopedJvmGlobalRef(ScopedJvmGlobalRef&& other)
      : JvmRef<T>(other.Release()) {}

  ~ScopedJvmGlobalRef() {
    if (m_obj != nullptr)
      getJniEnv()->DeleteGlobalRef(m_obj);
  }

  void operator=(const JvmRef<T>& other) {
    JNIEnv* env = getJniEnv();
    if (m_obj != nullptr) {
      env->DeleteGlobalRef(m_obj);
    }
    m_obj = other.is_null() ? nullptr : env->NewGlobalRef(other.obj());
  }

  void operator=(std::nullptr_t) {
    if (m_obj != nullptr) {
      getJniEnv()->DeleteGlobalRef(m_obj);
    }
    m_obj = nullptr;
  }

  // Releases the reference to the caller. The caller *must* delete the
  // reference when it is done with it. Note that calling a Java method
  // is *not* a transfer of ownership and Release() should not be used.
  T Release() {
    T obj = static_cast<T>(m_obj);
    m_obj = nullptr;
    return obj;
  }

 private:
  ScopedJvmGlobalRef(const ScopedJvmGlobalRef&) = delete;
  ScopedJvmGlobalRef& operator=(const ScopedJvmGlobalRef&) = delete;
};

// Returns a class local reference from a fully-qualified name.
// This method must be called on a context capable of loading Java classes.
ScopedJvmLocalRef<jclass> findClass(JNIEnv* env, const char* name);

// Methods for converting native types to Java types.
ScopedJvmLocalRef<jstring> nativeToJvmString(JNIEnv* env, const char* str);
ScopedJvmLocalRef<jstring> nativeToJvmString(JNIEnv* env, const std::string& str);
ScopedJvmLocalRef<jbyteArray> nativeToJvmByteArray(
    JNIEnv* env,
    const char* bytes,
    size_t len);
ScopedJvmLocalRef<jbyteArray> nativeToJvmByteArray(
    JNIEnv* env,
    const std::vector<char>& bytes);
jlong nativeToJvmPointer(void* ptr);

// Helper function for converting std::vector<T> into a Java array.
template<typename T, typename Convert>
ScopedJvmLocalRef<jobjectArray> nativeToJvmObjectArray(
    JNIEnv* env,
    const std::vector<T>& container,
    jclass clazz,
    Convert convert) {
  ScopedJvmLocalRef<jobjectArray> j_container(
      env, env->NewObjectArray(container.size(), clazz, nullptr));
  int i = 0;
  for (const T& element: container) {
    env->SetObjectArrayElement(j_container.obj(), i,
                               convert(env, element).obj());
    ++i;
  }
  return j_container;
}

template<typename T, typename Convert>
std::vector<T> jvmToNativeVector(
    JNIEnv* env,
    const JvmRef<jobjectArray>& j_container,
    Convert convert) {
  std::vector<T> container;
  const jsize size = env->GetArrayLength(j_container.obj());
  container.reserve(size);
  for (jsize i = 0; i < size; ++i) {
    container.emplace_back(convert(
        env, ScopedJvmLocalRef<jobject>(
            env, env->GetObjectArrayElement(j_container.obj(), i))));
  }
  LOG_FATAL_IF(checkException(env));
  return container;
}

// Converts a Java string to a std string in modified UTF-8 encoding
std::string jvmToStdString(JNIEnv* env, const JvmRef<jstring>& j_string);
std::string jvmToStdString(JNIEnv* env, jstring j_string);

std::vector<char> jvmToNativeByteArray(JNIEnv* env,
                                       const JvmRef<jbyteArray>& j_array);

}  // namespace monero

#endif  // JVM_H_
