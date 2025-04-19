#include "logging.h"

#include "common/debug.h"

#include "jni_cache.h"

#include "easylogging++.h"

namespace monero {

class JvmEasyLoggingDispatcher : public el::LogDispatchCallback {
 protected:
  void handle(const el::LogDispatchData* data) override {
    const auto& log_msg = data->logMessage();
    if (data->dispatchAction() == el::base::DispatchAction::None) {
      return;
    }
    dispatch(
        log_msg->logger()->id(),
        log_msg->level(),
        data->logMessage()->logger()->logBuilder()->build(log_msg, false));
  }

 private:
  static void dispatch(const std::string& tag, el::Level level, const std::string& msg) {
    JvmLogSink::instance()->write(tag, logging_level(level), msg);
  }

  static LoggingLevel logging_level(el::Level level) {
    switch (level) {
      case el::Level::Debug: return DEBUG;
      case el::Level::Info: return INFO;
      case el::Level::Warning: return WARN;
      case el::Level::Error: return ERROR;
      case el::Level::Fatal: return ASSERT;
      case el::Level::Verbose: // fall-through
      case el::Level::Trace:   // fall-through
      default: return VERBOSE;
    }
  }
};

#define EL_BASE_FORMAT "%msg"
#define EL_TRACE_FORMAT "[%fbase:%line] " EL_BASE_FORMAT

void InitializeEasyLogging() {
  el::Configurations c;
  c.setGlobally(el::ConfigurationType::ToFile, "false");
  c.setGlobally(el::ConfigurationType::ToStandardOutput, "false");
  c.setGlobally(el::ConfigurationType::Format,
                std::string(EL_BASE_FORMAT));
  // VERBOSE, INFO, WARNING, ERROR, FATAL, are set to default by Level::Global
  c.set(el::Level::Debug, el::ConfigurationType::Format,
        std::string(EL_TRACE_FORMAT));
  c.set(el::Level::Trace, el::ConfigurationType::Format,
        std::string(EL_TRACE_FORMAT));

  el::Loggers::setDefaultConfigurations(c, true);
  el::Loggers::setCategories("*:global");
  el::Loggers::addFlag(el::LoggingFlag::HierarchicalLogging);
  el::Loggers::addFlag(el::LoggingFlag::CreateLoggerAutomatically);

  el::Helpers::installLogDispatchCallback<JvmEasyLoggingDispatcher>(
      "JvmEasyLoggingDispatcher");
  el::Helpers::uninstallLogDispatchCallback<el::base::DefaultLogDispatchCallback>(
      "DefaultLogDispatchCallback");
}

void JvmLogSink::write(const std::string& tag,
                       LoggingLevel priority,
                       const std::string& msg) {
  LOG_FATAL_IF(m_logger.is_null(), "Logger not set");
  const int pri_idx = static_cast<int>(priority);
  JNIEnv* env = GetJniEnv();
  ScopedJavaLocalRef<jstring> j_tag(env, NativeToJavaString(env, tag));
  ScopedJavaLocalRef<jstring> j_msg(env, NativeToJavaString(env, msg));

  CallVoidMethod(env, m_logger.obj(), Logger_logFromNative,
                 pri_idx, j_tag.obj(), j_msg.obj());
}

void JvmLogSink::set_logger(JNIEnv* env, const JavaRef<jobject>& logger) {
  m_logger = logger;
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_internal_NativeLoaderKt_nativeSetLogger(
    JNIEnv* env,
    jclass clazz,
    jobject j_logger) {
  JvmLogSink::instance()->set_logger(env, JavaParamRef<jobject>(j_logger));
}

}  // namespace monero
