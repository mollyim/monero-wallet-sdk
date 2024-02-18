#include "http_client.h"

#include "common/debug.h"

#include "jni_cache.h"

namespace monero {

bool RemoteNodeClient::set_proxy(const std::string& address) {
  // No-op.
  return true;
}

void RemoteNodeClient::set_server(std::string host,
                                  std::string port,
                                  boost::optional<epee::net_utils::http::login> user,
                                  epee::net_utils::ssl_options_t ssl_options) {
  // No-op.
}

void RemoteNodeClient::set_auto_connect(bool auto_connect) {

}

bool RemoteNodeClient::connect(std::chrono::milliseconds timeout) {
  return false;
}

bool RemoteNodeClient::disconnect() {
  return false;
}

bool RemoteNodeClient::is_connected(bool* ssl) {
  return false;
}

RemoteNodeClient::HttpResponse JavaToHttpResponse(JNIEnv* env, jobject obj) {
  jint code = CallIntMethod(env, obj, HttpResponse_getCode);
  ScopedJavaLocalRef<jstring>
      j_mime_type(env, CallStringMethod(env, obj, HttpResponse_getContentType));
  ScopedJavaLocalRef<jobject>
      j_body(env, CallObjectMethod(env, obj, HttpResponse_getBody));
  return {code, j_mime_type.is_null() ? ""
                                      : JavaToNativeString(env, j_mime_type.obj()),
          ScopedFd(env, j_body)};
}

bool RemoteNodeClient::invoke(const boost::string_ref uri,
                              const boost::string_ref method,
                              const boost::string_ref body,
                              std::chrono::milliseconds timeout,
                              const epee::net_utils::http::http_response_info** ppresponse_info,
                              const epee::net_utils::http::fields_list& additional_params) {
  std::ostringstream header;
  for (const auto& p: additional_params) {
    header << p.first << ": " << p.second << "\r\n";
  }
  JNIEnv* env = GetJniEnv();
  try {
    ScopedJavaLocalRef<jstring> j_method(env, NativeToJavaString(env, method.data()));
    ScopedJavaLocalRef<jstring> j_uri(env, NativeToJavaString(env, uri.data()));
    ScopedJavaLocalRef<jstring> j_hdr(env, NativeToJavaString(env, header.str()));
    ScopedJavaLocalRef<jbyteArray>
        j_body(env, NativeToJavaByteArray(env, body.data(), body.length()));
    ScopedJavaLocalRef<jobject>
        j_response = {env, CallObjectMethod(env,
                                            m_wallet_native.obj(),
                                            WalletNative_callRemoteNode,
                                            j_method.obj(),
                                            j_uri.obj(),
                                            j_hdr.obj(),
                                            j_body.obj())};
    m_response_info.clear();
    if (j_response.is_null()) {
      return false;
    }
    HttpResponse http_response = JavaToHttpResponse(env, j_response.obj());
    if (http_response.code == 401) {
      // Handle HTTP unauthorized in the same way as http_simple_client_template.
      return false;
    }
    m_response_info.m_response_code = http_response.code;
    m_response_info.m_mime_tipe = http_response.content_type;
    if (http_response.body.is_valid()) {
      http_response.body.read(&m_response_info.m_body);
    }
  } catch (std::runtime_error& e) {
    LOGE("Unhandled exception: %s", e.what());
    return false;
  }
  if (ppresponse_info) {
    *ppresponse_info = std::addressof(m_response_info);
  }
  return true;
}

bool RemoteNodeClient::invoke_get(const boost::string_ref uri,
                                  std::chrono::milliseconds timeout,
                                  const std::string& body,
                                  const epee::net_utils::http::http_response_info** ppresponse_info,
                                  const epee::net_utils::http::fields_list& additional_params) {
  return invoke(uri, "GET", body, timeout, ppresponse_info, additional_params);
}

bool RemoteNodeClient::invoke_post(const boost::string_ref uri,
                                   const std::string& body,
                                   std::chrono::milliseconds timeout,
                                   const epee::net_utils::http::http_response_info** ppresponse_info,
                                   const epee::net_utils::http::fields_list& additional_params) {
  return invoke(uri, "POST", body, timeout, ppresponse_info, additional_params);
}

uint64_t RemoteNodeClient::get_bytes_sent() const {
  return 0;
}

uint64_t RemoteNodeClient::get_bytes_received() const {
  return 0;
}

}  // namespace monero
