#ifndef WALLET_FD_H_
#define WALLET_FD_H_

#include <unistd.h>

#include <sstream>

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream.hpp>

#include "common/jvm.h"

#include "jni_cache.h"

namespace monero {

// Utility class to hold a file descriptor and call 'close' automatically
// on scope exit.
class ScopedFd {
 public:
  ScopedFd() : m_fd(-1) {}

  explicit ScopedFd(int fd) : m_fd(fd) {}

  ScopedFd(ScopedFd&& other) : m_fd(other.m_fd) {
    other.m_fd = -1;
  }

  ScopedFd(JNIEnv* env, const JvmRef<jobject>& parcel_file_descriptor) : m_fd(-1) {
    if (!parcel_file_descriptor.is_null()) {
      m_fd = parcel_file_descriptor.callIntMethod(env, ParcelFileDescriptor_detachFd);
    }
  }

  ~ScopedFd() {
    close();
  }

  int fd() const { return m_fd; }

  bool is_valid() const { return m_fd >= 0; }

  void close() {
    if (is_valid()) {
      int save_errno = errno;
      ::close(m_fd);
      m_fd = -1;
      errno = save_errno;
    }
  }

  void read(std::string* buf) const {
    using namespace boost::iostreams;
    stream<file_descriptor_source> stream(m_fd, never_close_handle);
    std::ostringstream ss;
    ss << stream.rdbuf();
    *buf = ss.str();
  }

 private:
  int m_fd;

 private:
  ScopedFd(const ScopedFd&) = delete;
  ScopedFd& operator=(const ScopedFd&) = delete;
};

}  // namespace monero

#endif  // WALLET_FD_H_
