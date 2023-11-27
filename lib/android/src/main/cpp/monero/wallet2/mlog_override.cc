#include <string>

#include "easylogging++.h"

void mlog_configure(const std::string& filename_base,
                    bool console,
                    const std::size_t max_log_file_size,
                    const std::size_t max_log_files) {
  // No-op.
}

extern "C" {

// Log helpers for C code.  Used only in module 'randomx' for simple error messages.
// To make it easy, printf formatting is ignored and it's assumed the source
// FILE and LINE will wrongly identify these functions instead of the callers.

bool merror(const char* category, const char* format, ...) {
  CLOG(ERROR, category) << format;
  return true;
}

bool mwarning(const char* category, const char* format, ...) {
  CLOG(WARNING, category) << format;
  return true;
}

bool minfo(const char* category, const char* format, ...) {
  CLOG(INFO, category) << format;
  return true;
}

bool mdebug(const char* category, const char* format, ...) {
  CLOG(DEBUG, category) << format;
  return true;
}

bool mtrace(const char* category, const char* format, ...) {
  CLOG(TRACE, category) << format;
  return true;
}

}  // extern C
