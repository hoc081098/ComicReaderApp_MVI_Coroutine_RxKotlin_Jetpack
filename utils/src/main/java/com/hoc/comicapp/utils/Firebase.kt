package com.hoc.comicapp.utils

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import timber.log.Timber

fun DocumentReference.snapshots(): Observable<DocumentSnapshot> {
  return Observable.create { emitter: ObservableEmitter<DocumentSnapshot> ->
    val registration = addSnapshotListener listener@{ documentSnapshot, exception ->
      if (exception !== null && !emitter.isDisposed) {
        return@listener emitter.onError(exception)
      }
      if (documentSnapshot != null && !emitter.isDisposed) {
        emitter.onNext(documentSnapshot)
      }
    }
    emitter.setCancellable {
      registration.remove()
      Timber.d("Remove snapshot listener $this")
    }
  }
}

fun Query.snapshots(): Observable<QuerySnapshot> {
  return Observable.create { emitter: ObservableEmitter<QuerySnapshot> ->
    val registration = addSnapshotListener listener@{ querySnapshot, exception ->
      if (exception !== null && !emitter.isDisposed) {
        return@listener emitter.onError(exception)
      }
      if (querySnapshot != null && !emitter.isDisposed) {
        emitter.onNext(querySnapshot)
      }
    }
    emitter.setCancellable {
      registration.remove()
      Timber.d("Remove snapshot listener $this")
    }
  }
}
