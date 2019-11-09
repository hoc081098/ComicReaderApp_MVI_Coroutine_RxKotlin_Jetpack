package com.hoc.comicapp.koin

import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProviderImpl
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProviderImpl
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
  single { RxSchedulerProviderImpl() } bind RxSchedulerProvider::class

  single { CoroutinesDispatchersProviderImpl(get()) } bind CoroutinesDispatchersProvider::class

  single { WorkManager.getInstance(androidContext()) }

  single { getMoshi() }

  single { getChapterJsonAdapter(get()) }

  single { FirebaseAuth.getInstance() }

  single { FirebaseStorage.getInstance() }

  single { FirebaseFirestore.getInstance() }

  single { CoroutineScope(get<CoroutinesDispatchersProvider>().io + SupervisorJob()) }
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