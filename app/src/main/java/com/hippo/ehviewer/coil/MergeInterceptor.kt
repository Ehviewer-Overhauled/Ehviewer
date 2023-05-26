package com.hippo.ehviewer.coil

import coil.intercept.Interceptor
import coil.request.ImageResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object MergeInterceptor : Interceptor {
    private val pendingContinuationMap: HashMap<String, MutableList<Continuation<Unit>>> = hashMapOf()
    private val pendingContinuationMapLock = Mutex()
    private val notifyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val EMPTY_LIST = mutableListOf<Continuation<Unit>>()
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val req = chain.request
        val key = req.memoryCacheKey?.key?.takeIf { it.startsWith("m/") } ?: return withContext(req.interceptorDispatcher) { chain.proceed(req) }

        pendingContinuationMapLock.lock()
        val existPendingContinuations = pendingContinuationMap[key]
        if (existPendingContinuations == null) {
            pendingContinuationMap[key] = EMPTY_LIST
            pendingContinuationMapLock.unlock()
        } else {
            if (existPendingContinuations === EMPTY_LIST) pendingContinuationMap[key] = mutableListOf()
            pendingContinuationMap[key]!!.apply {
                suspendCancellableCoroutine { continuation ->
                    add(continuation)
                    pendingContinuationMapLock.unlock()
                    continuation.invokeOnCancellation { remove(continuation) }
                }
            }
        }

        try {
            return withContext(req.interceptorDispatcher) { chain.proceed(req) }.apply {
                // Wake all pending continuations shared with the same memory key since we have written it to memory cache
                notifyScope.launch {
                    pendingContinuationMapLock.withLock {
                        pendingContinuationMap.remove(key)?.forEach { it.resume(Unit) }
                    }
                }
            }
        } catch (e: CancellationException) {
            notifyScope.launch {
                pendingContinuationMapLock.withLock {
                    // Wake up a pending continuation to continue executing task
                    val successor = pendingContinuationMap[key]?.removeFirstOrNull()?.apply { resume(Unit) }
                    // If no successor, delete this entry from hashmap
                    successor ?: pendingContinuationMap.remove(key)
                }
            }
            throw e
        } catch (e: Throwable) {
            // Wake all pending continuations since this request is to be failed
            notifyScope.launch {
                pendingContinuationMapLock.withLock {
                    pendingContinuationMap.remove(key)?.forEach { it.resume(Unit) }
                }
            }
            throw e
        }
    }
}
