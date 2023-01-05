package com.hippo.ehviewer.dao

interface BasicDao<T> {
    fun list(): List<T>
    fun insert(t: T): Long
}