project(genuine C)

add_library(genuine STATIC
        am-proxy.c
        apk-sign-v2.c
        common.c
        genuine.c
        inline.c
        openat.c
        path.c
        plt.c)
target_include_directories(genuine PUBLIC integrity)
