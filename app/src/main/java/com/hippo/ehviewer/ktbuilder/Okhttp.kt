package com.hippo.ehviewer.ktbuilder

import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okio.FileSystem
import okio.Path
import splitties.init.appCtx

inline fun httpClient(builder: OkHttpClient.Builder.() -> Unit): OkHttpClient = OkHttpClient.Builder().apply(builder).build()
inline fun httpClient(client: OkHttpClient, builder: OkHttpClient.Builder.() -> Unit): OkHttpClient = client.newBuilder().apply(builder).build()
fun OkHttpClient.Builder.cache(directory: Path, maxSize: Long, fileSystem: FileSystem = FileSystem.SYSTEM) = cache(Cache(directory, maxSize, fileSystem))
inline fun OkHttpClient.Builder.chunker(builder: ChuckerInterceptor.Builder.() -> Unit) = addInterceptor(ChuckerInterceptor.Builder(appCtx).apply(builder).build())
