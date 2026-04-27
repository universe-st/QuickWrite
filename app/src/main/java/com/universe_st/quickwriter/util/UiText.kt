package com.universe_st.quickwriter.util

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(@StringRes val resId: Int, val args: Array<out Any> = emptyArray()) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return resId == other.resId && args.contentEquals(other.args)
        }
        override fun hashCode(): Int = resId * 31 + args.contentHashCode()
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }

    companion object {
        fun from(@StringRes resId: Int, vararg args: Any): UiText =
            StringResource(resId, args)

        fun from(text: String): UiText = DynamicString(text)
    }
}
