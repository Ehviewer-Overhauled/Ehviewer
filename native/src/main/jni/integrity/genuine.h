#include <stdbool.h>

#define GENUINE_NAME {0x62, 0x63, 0x23, 0x69, 0x66, 0x64, 0x79, 0x67, 0x71, 0x2e, 0x6f, 0x67, 0x68, 0x6b, 0x6c, 0x68, 0x71, 0x6d, 0x7b, 0x7e, 0x6e, 0x7e, 0x23, 0x6b, 0x67, 0x66, 0x78, 0x77, 0x64, 0x65, 0x73, 0x0}
#define GENUINE_SIZE 0x02d1
#define GENUINE_HASH 0x9a99ba0c

#ifdef NDEBUG
/* genuine false handler */
#define GENUINE_FALSE_CRASH
// #define GENUINE_FALSE_NATIVE

/* genuine fake handler */
#define GENUINE_FAKE_CRASH
// #define GENUINE_FAKE_NATIVE

/* genuine overlay handler */
// #define GENUINE_OVERLAY_CRASH
// #define GENUINE_OVERLAY_NATIVE

/* genuine odex handler */
// #define GENUINE_ODEX_CRASH
// #define GENUINE_ODEX_NATIVE

/* genuine dex handler */
// #define GENUINE_DEX_CRASH
// #define GENUINE_DEX_NATIVE

/* genuine proxy handler */
// #define GENUINE_PROXY_CRASH
// #define GENUINE_PROXY_NATIVE

/* genuine error handler */
#define GENUINE_ERROR_CRASH
// #define GENUINE_ERROR_NATIVE

/* genuine fatal handler */
#define GENUINE_FATAL_CRASH
// #define GENUINE_FATAL_NATIVE

/* genuine noapk handler */
#define GENUINE_NOAPK_CRASH
// #define GENUINE_NOAPK_NATIVE
#endif

bool checkGenuine(JNIEnv *env);
