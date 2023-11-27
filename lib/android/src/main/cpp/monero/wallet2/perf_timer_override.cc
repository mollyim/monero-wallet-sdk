#include "perf_timer.h"

namespace tools {

el::Level performance_timer_log_level = el::Level::Info;

PerformanceTimer::PerformanceTimer(bool paused) {
  // No-op.
}

PerformanceTimer::~PerformanceTimer() {
  // No-op.
}

LoggingPerformanceTimer::LoggingPerformanceTimer(const std::string& s,
                                                 const std::string& cat,
                                                 uint64_t unit,
                                                 el::Level l) {
  // No-op.
}

LoggingPerformanceTimer::~LoggingPerformanceTimer() {
  // No-op.
}

}  // namespace tools
