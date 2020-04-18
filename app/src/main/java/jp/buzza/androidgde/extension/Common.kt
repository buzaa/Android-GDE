package jp.buzza.androidgde.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * This type of function is not good
 */
fun <T : Activity> Activity.startActivityWithoutReified(
    context: Context,
    className: Class<T>
) {
    startActivity(Intent(context, className))
}

/**
 * Using inline vs reified is the best choice
 */
inline fun <reified T : Activity> Activity.startActivity(
    context: Context
) {
    startActivity(Intent(context, T::class.java))
}

/*
 * Safe way to get return value
 */
inline fun <reified T> Bundle.getDataOrNull(key: String): T? {
    return getSerializable(key) as? T
}
