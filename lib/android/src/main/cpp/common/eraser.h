#ifndef COMMON_ERASER_H_
#define COMMON_ERASER_H_

#include <openssl/crypto.h>

namespace monero {

#pragma clang diagnostic push
#pragma ide diagnostic ignored "NotImplementedFunctions"

// Eraser clears buffers.  Construct it with a buffer or object and the
// destructor will ensure that it is zeroed.
class Eraser {
 public:
  // Not implemented.  If this gets used, we want a link error.
  template <typename T> explicit Eraser(T* t);

  template <typename T>
  explicit Eraser(T& t) : m_buf(reinterpret_cast<char*>(&t)), m_size(sizeof(t)) {}

  template <size_t N> explicit Eraser(char (&arr)[N]) : m_buf(arr), m_size(N) {}

  Eraser(void* buf, size_t size) : m_buf(static_cast<char*>(buf)), m_size(size) {}
  ~Eraser() { OPENSSL_cleanse(m_buf, m_size); }

 private:
  Eraser(const Eraser&);
  void operator=(const Eraser&);

  char* m_buf;
  size_t m_size;
};

#pragma clang diagnostic pop

}  // namespace monero

#endif  // COMMON_ERASER_H_
