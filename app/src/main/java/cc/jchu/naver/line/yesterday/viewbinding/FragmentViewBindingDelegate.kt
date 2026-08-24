package cc.jchu.naver.line.yesterday.viewbinding

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentViewBindingDelegate<T : ViewBinding>(
    fragment: Fragment,
    private val bind: (View) -> T,
) : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {
    private var binding: T? = null

    init {
        fragment.viewLifecycleOwnerLiveData.observe(fragment) { owner ->
            owner.lifecycle.addObserver(this)
        }
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        check(thisRef.viewLifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            "Cannot access ${property.name} after the Fragment view is destroyed."
        }
        return binding ?: bind(thisRef.requireView()).also { binding = it }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        binding = null
        owner.lifecycle.removeObserver(this)
    }
}

fun <T : ViewBinding> Fragment.viewBinding(bind: (View) -> T) =
    FragmentViewBindingDelegate(this, bind)
