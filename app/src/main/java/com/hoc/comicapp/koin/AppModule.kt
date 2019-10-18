package com.hoc.comicapp.koin

import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProviderImpl
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProviderImpl
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
  single { RxSchedulerProviderImpl() } bind RxSchedulerProvider::class

  single { CoroutinesDispatcherProviderImpl(get()) } bind CoroutinesDispatcherProvider::class

  single { WorkManager.getInstance(androidContext()) }

  single { getMoshi() }

  single { getChapterJsonAdapter(get()) }

  single { FirebaseAuth.getInstance() }

  single { FirebaseStorage.getInstance() }
}

private fun getChapterJsonAdapter(moshi: Moshi): JsonAdapter<ComicDetail.Chapter> {
  return moshi.adapter<ComicDetail.Chapter>(ComicDetail.Chapter::class.java)
}

private fun getMoshi(): Moshi {
  return Moshi
    .Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
}