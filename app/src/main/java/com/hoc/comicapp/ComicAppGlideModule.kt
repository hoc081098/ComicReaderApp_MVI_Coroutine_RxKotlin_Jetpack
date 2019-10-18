package com.hoc.comicapp

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@GlideModule
class ComicAppGlideModule : AppGlideModule() {
  override fun applyOptions(context: Context, builder: GlideBuilder) {
    val diskCacheSizeBytes = 200 * 1024 * 1024L // 200 MB

    builder.setDiskCache(
      InternalCacheDiskCacheFactory(
        context,
        diskCacheSizeBytes
      )
    )
  }

  override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
    val client = getUnsafeOkHttpClient()
    registry.replace(
      GlideUrl::class.java,
      InputStream::class.java,
      OkHttpUrlLoader.Factory(client)
    )
  }

  private fun getUnsafeOkHttpClient(): OkHttpClient {
    val trustAllCerts = arrayOf<TrustManager>(
      object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
      }
    )

    val sslContext = SSLContext.getInstance("SSL").apply {
      init(null, trustAllCerts, SecureRandom())
    }
    val sslSocketFactory = sslContext.socketFactory

    return OkHttpClient
      .Builder()
      .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
      .hostnameVerifier(HostnameVerifier { _, _ -> true })
      .build()
  }
}