package com.hoc.comicapp.koin

import com.hoc.comicapp.data.ComicRepositoryImpl
import com.hoc.comicapp.domain.repository.ComicRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
  single { ComicRepositoryImpl(get(), get(), get()) } bind ComicRepository::class
}