#ifndef COMMON_H_
#define COMMON_H_

#include <stddef.h>
#include <android/log.h>

// Default local tag
#ifndef LOG_TAG
#define LOG_TAG "MoneroJNI"
#endif

// Low-level debug macros.  Log messages are not scrubbed.
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
// For release builds, disable verbose logging.
#ifndef NDEBUG
#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__))
#else
#define LOGV(...) ((void)0)
#endif

// Log a fatal error.  If the given condition fails, this stops program
// execution like a normal assertion, but also generating the given message.
// It always evaluates its argument and it is NOT stripped from release builds,
// so it is OK for `cond` to have side effects.  Note that the condition
// test is -inverted- from the normal assert() semantics.
#define LOG_FATAL_IF(cond, ...) \
    ( (__builtin_expect((cond)!=0, 0)) \
    ? ((void) __print_assert(#cond, LOG_TAG, ## __VA_ARGS__)) \
    : (void)0 )

#define LOG_FATAL(...) \
    ( ((void) __print_assert(NULL, LOG_TAG, ## __VA_ARGS__)) )

// Assertion that generates a log message when the assertion fails.
// Stripped out of release builds.
#ifndef NDEBUG
#define LOG_ASSERT(cond, ...) LOG_FATAL_IF(!(cond), ## __VA_ARGS__)
#else
#define LOG_ASSERT(cond, ...) ((void)0)
#endif

#define __second(dummy, second, ...)  second
#define __rest(first, ...)            , ## __VA_ARGS__

#define __print_assert(cond, tag, fmt...) \
    __android_log_assert(cond, tag, __second(0, ## fmt, NULL) __rest(fmt))

// The arraysize(arr) macro returns the # of elements in an array arr.
template <typename T, size_t N>
char (&ArraySizeHelper(T (&array)[N]))[N];

#define arraysize(array) (sizeof(ArraySizeHelper(array)))

#endif  // COMMON_H_
