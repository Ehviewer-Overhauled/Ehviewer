project(genuine C)

add_library(genuine STATIC
        am-proxy.c
        apk-sign-v2.c
        common.c
        genuine.c
        inline.c
        openat.c
        path.c
        plt.c
        pm.c)
target_compile_options(genuine PUBLIC
        -Oz -Wall -Wextra -Wshadow -Werror -fvisibility=hidden)
target_include_directories(genuine PUBLIC
        integrity)
