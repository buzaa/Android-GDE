package jp.buzza.androidgde.extension

import android.app.Activity
import android.content.Context
import android.content.Intent

fun <T : Activity> Activity.startActivityWithoutReified(
    context: Context,
    className: Class<T>
) {
    startActivity(Intent(context, className))
}

inline fun <reified T : Activity> Activity.startActivity(
    context: Context
) {
    startActivity(Intent(context, T::class.java))
}
