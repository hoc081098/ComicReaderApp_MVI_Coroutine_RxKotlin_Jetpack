package com.hoc.comicapp.data.firebase.user

import android.net.Uri
import com.hoc.comicapp.data.firebase.entity._User
import com.hoc.comicapp.utils.Either
import io.reactivex.rxjava3.core.Observable

interface FirebaseAuthUserDataSource {
  fun userObservable(): Observable<Either<Throwable, _User?>>

  suspend fun signOut()

  suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?,
  )

  suspend fun login(email: String, password: String)
}
