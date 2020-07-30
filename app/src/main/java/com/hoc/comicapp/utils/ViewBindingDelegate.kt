package com.hoc.comicapp.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import timber.log.Timber
import java.lang.reflect.Method
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ViewBindingDelegate<T : ViewBinding>(
  private val fragment: Fragment,
  private val viewBindingFactory: (View) -> T,
  private var onDestroy: ((T) -> Unit)?,
) : ReadOnlyProperty<Fragment, T> {
  private var binding: T? = null

  init {
    fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onCreate(owner: LifecycleOwner) {
        fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
          Timber.d("$fragment::view::viewLifecycleOwnerLiveData")

          viewLifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
              onDestroy?.invoke(binding!!)
              binding = null
              viewLifecycleOwner.lifecycle.removeObserver(this)
              Timber.d("$fragment::view::onDestroy")
            }
          })
        }
      }

      override fun onDestroy(owner: LifecycleOwner) {
        fragment.lifecycle.removeObserver(this)
      }
    })
  }

  override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
    binding?.let { return it }

    if (!fragment.viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
      error("Attempt to get view binding when fragment view is destroyed")
    }

    return viewBindingFactory(thisRef.requireView()).also { binding = it }
  }
}

@MainThread
inline fun <reified T : ViewBinding> Fragment.viewBinding(noinline onDestroy: (T.() -> Unit)? = null): ViewBindingDelegate<T> {
  var bindMethod: Method? = null

  return ViewBindingDelegate(
    this,
    { view ->
      (bindMethod ?: T::class.java
        .getMethod("bind", View::class.java)
        .also { bindMethod = it })
        .invoke(null, view) as T
    },
    onDestroy
  )
}

@MainThread
fun <T : ViewBinding> AppCompatActivity.viewBinding(factory: (LayoutInflater) -> T) =
  lazy(NONE) { factory(layoutInflater) }

inline val ViewGroup.inflater: LayoutInflater get() = LayoutInflater.from(context)