#ifndef COMMON_ARRAYSIZE_H
#define COMMON_ARRAYSIZE_H

// The arraysize(arr) macro returns the # of elements in an array arr.
template <typename T, size_t N>
char (&ArraySizeHelper(T (&array)[N]))[N];

#define arraysize(array) (sizeof(ArraySizeHelper(array)))

#endif  // COMMON_ARRAYSIZE_H
