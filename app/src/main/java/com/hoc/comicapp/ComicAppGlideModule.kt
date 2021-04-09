package com.hoc.comicapp

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import com.bumptech.glide.module.AppGlideModule
import com.hoc.comicapp.ImageHeaders.headersFor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import timber.log.Timber
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
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
    registry.replace(
      String::class.java,
      InputStream::class.java,
      HeaderedLoader.Factory()
    )
  }
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
    .hostnameVerifier { _, _ -> true }
    .addInterceptor(
      HttpLoggingInterceptor(
        object : HttpLoggingInterceptor.Logger {
          override fun log(message: String) = Timber.tag("Glide_OkHttp").d(message)
        }
      )
        .apply { level = (if (BuildConfig.DEBUG) HEADERS else NONE) }
    )
    .build()
}

private class HeaderedLoader(concreteLoader: ModelLoader<GlideUrl?, InputStream?>?) :
  BaseGlideUrlLoader<String>(concreteLoader) {
  override fun handles(model: String): Boolean = true

  override fun getUrl(
    model: String,
    width: Int,
    height: Int,
    options: Options?,
  ): String = model

  override fun getHeaders(
    model: String?,
    width: Int,
    height: Int,
    options: Options?,
  ): Headers = headersFor(model).let { map ->
    if (map.isEmpty()) {
      Headers.DEFAULT
    } else {
      LazyHeaders.Builder()
        .apply { map.forEach { (k, v) -> addHeader(k, v) } }
        .build()
    }
  }

  class Factory : ModelLoaderFactory<String, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, InputStream> {
      return HeaderedLoader(
        multiFactory.build(
          GlideUrl::class.java,
          InputStream::class.java
        )
      )
    }

    override fun teardown() = Unit
  }
}

/**
 * Workaround for security image
 */
object ImageHeaders {
  fun headersFor(model: String?): Map<String, String> {
    Timber.d("url=$model")
    model ?: return emptyMap()

    if ("mangakakalot" in model) {
      return mapOf(
        "user-agent" to USER_AGENT,
        "referer" to "https://mangakakalot.com/",
        "cookie" to "__cfduid=d92a49507fe881e99fffddfad020ecb271612495383",
      )
    }

    if ("s8.mkklcdnv8.com" in model) {
      val regex = "https://s8.mkklcdnv8.com/mangakakalot/.+/(.+)/(.+)/.+".toRegex()
      val (name, chapter) = regex.find(model)!!.destructured
      val referer = "https://manganelo.com/chapter/$name/$chapter"
      Timber.d("url=$model -> referer=$referer")

      return mapOf(
        "user-agent" to USER_AGENT,
        "referer" to referer,
      )
    }

    return emptyMap()
  }

  private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Mobile Safari/537.36"
}
