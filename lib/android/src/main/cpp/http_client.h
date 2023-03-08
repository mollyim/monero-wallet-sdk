#ifndef HTTP_CLIENT_H_
#define HTTP_CLIENT_H_

#include "jvm.h"
#include "fd.h"

#include "net/abstract_http_client.h"

namespace monero {

using AbstractHttpClient = epee::net_utils::http::abstract_http_client;

class RemoteNodeClient : public AbstractHttpClient {
 public:
  RemoteNodeClient(
      JNIEnv* env,
      const JvmRef<jobject>& wallet_native)
      : m_wallet_native(env, wallet_native) {}

  bool set_proxy(const std::string& address) override;
  void set_server(std::string host,
                  std::string port,
                  boost::optional<epee::net_utils::http::login> user,
                  epee::net_utils::ssl_options_t ssl_options) override;
  void set_auto_connect(bool auto_connect) override;
  bool connect(std::chrono::milliseconds timeout) override;
  bool disconnect() override;
  bool is_connected(bool* ssl) override;
  bool invoke(const boost::string_ref uri,
              const boost::string_ref method,
              const boost::string_ref body,
              std::chrono::milliseconds timeout,
              const epee::net_utils::http::http_response_info** ppresponse_info,
              const epee::net_utils::http::fields_list& additional_params) override;
  bool invoke_get(const boost::string_ref uri,
                  std::chrono::milliseconds timeout,
                  const std::string& body,
                  const epee::net_utils::http::http_response_info** ppresponse_info,
                  const epee::net_utils::http::fields_list& additional_params) override;
  bool invoke_post(const boost::string_ref uri,
                   const std::string& body,
                   std::chrono::milliseconds timeout,
                   const epee::net_utils::http::http_response_info** ppresponse_info,
                   const epee::net_utils::http::fields_list& additional_params) override;
  uint64_t get_bytes_sent() const override;
  uint64_t get_bytes_received() const override;

 public:
  struct HttpResponse {
    int code;
    std::string content_type;
    ScopedFd body;
  };

 private:
  const ScopedJvmGlobalRef<jobject> m_wallet_native;
  epee::net_utils::http::http_response_info m_response_info;
};

using HttpClientFactory = epee::net_utils::http::http_client_factory;

class RemoteNodeClientFactory : public HttpClientFactory {
 public:
  RemoteNodeClientFactory(
      JNIEnv* env,
      const JvmRef<jobject>& wallet_native)
      : m_wallet_native(env, wallet_native) {}

  std::unique_ptr<AbstractHttpClient> create() override {
    return std::unique_ptr<AbstractHttpClient>(new RemoteNodeClient(getJniEnv(),
                                                                    m_wallet_native));
  }

 private:
  const ScopedJvmGlobalRef<jobject> m_wallet_native;
};

}  // namespace monero

#endif  // HTTP_CLIENT_H_
