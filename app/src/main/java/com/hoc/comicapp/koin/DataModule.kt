package com.hoc.comicapp.koin

import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.data.MockComicRepositoryImpl
import org.koin.dsl.module.module

val dataModule = module {
//  single { ComicRepositoryImpl(get(), get(), get()) } bind ComicRepository::class

  single { MockComicRepositoryImpl(get()) } bind ComicRepository::class
}