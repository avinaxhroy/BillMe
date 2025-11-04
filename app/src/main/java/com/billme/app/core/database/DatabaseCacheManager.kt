package com.billme.app.core.database

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseCacheManager @Inject constructor() {
    
    private val productCache = LruCache<Long, Any>(100)
    
    fun <T> cacheProduct(id: Long, product: T) {
        productCache.put(id, product as Any)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> getProduct(id: Long): T? {
        return productCache.get(id) as? T
    }
    
    fun clearCache() {
        productCache.evictAll()
    }
    
    fun getCacheSize(): Int = productCache.size()
}
