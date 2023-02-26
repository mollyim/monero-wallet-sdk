#include "http_client.h"

#include "jni_cache.h"
#include "fd.h"

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

RemoteNodeClient::HttpResponse jvmToHttpResponse(JNIEnv* env, JvmRef<jobject>& j_http_response) {
  jint code = j_http_response.callIntMethod(env, HttpResponse_getCode);
  jstring mime_type = j_http_response.callStringMethod(env, HttpResponse_getContentType);
  jobject body = j_http_response.callObjectMethod(env, HttpResponse_getBody);
  return {
      code,
      (mime_type != nullptr) ? jvmToStdString(env, mime_type) : "",
      ScopedFd(env, ScopedJvmLocalRef<jobject>(env, body))
  };
}

bool RemoteNodeClient::invoke(const boost::string_ref uri,
                              const boost::string_ref method,
                              const boost::string_ref body,
                              std::chrono::milliseconds timeout,
                              const epee::net_utils::http::http_response_info** ppresponse_info,
                              const epee::net_utils::http::fields_list& additional_params) {
  JNIEnv* env = getJniEnv();
  std::ostringstream header;
  for (const auto& p: additional_params) {
    header << p.first << ": " << p.second << "\r\n";
  }
  try {
    ScopedJvmLocalRef<jobject> j_response = {
        env, m_remote_node_client.callObjectMethod(
            env, IRemoteNodeClient_makeRequest,
            nativeToJvmString(env, method.data()).obj(),
            nativeToJvmString(env, uri.data()).obj(),
            nativeToJvmString(env, header.str()).obj(),
            nativeToJvmByteArray(env, body.data(), body.length()).obj()
        )
    };
    m_response_info.clear();
    if (j_response.is_null()) {
      return false;
    }
    HttpResponse http_response = jvmToHttpResponse(env, j_response);
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
    LOGE("Unhandled exception in RemoteNodeClient");
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
