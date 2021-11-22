package org.dslul.openboard.inputmethod.compat

import android.view.inputmethod.EditorInfo

object EditorInfoCompatUtils {
    // Note that EditorInfo.IME_FLAG_FORCE_ASCII has been introduced
// in API level 16 (Build.VERSION_CODES.JELLY_BEAN).
    private val FIELD_IME_FLAG_FORCE_ASCII = CompatUtils.getField(
            EditorInfo::class.java, "IME_FLAG_FORCE_ASCII")
    private val OBJ_IME_FLAG_FORCE_ASCII: Int? = CompatUtils.getFieldValue(
            null /* receiver */, null /* defaultValue */, FIELD_IME_FLAG_FORCE_ASCII) as Int
    private val FIELD_HINT_LOCALES = CompatUtils.getField(
            EditorInfo::class.java, "hintLocales")

    @kotlin.jvm.JvmStatic
    fun hasFlagForceAscii(imeOptions: Int): Boolean {
        return if (OBJ_IME_FLAG_FORCE_ASCII == null) false else imeOptions and OBJ_IME_FLAG_FORCE_ASCII != 0
    }

    @kotlin.jvm.JvmStatic
    fun imeActionName(imeOptions: Int): String {
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        return when (actionId) {
            EditorInfo.IME_ACTION_UNSPECIFIED -> "actionUnspecified"
            EditorInfo.IME_ACTION_NONE -> "actionNone"
            EditorInfo.IME_ACTION_GO -> "actionGo"
            EditorInfo.IME_ACTION_SEARCH -> "actionSearch"
            EditorInfo.IME_ACTION_SEND -> "actionSend"
            EditorInfo.IME_ACTION_NEXT -> "actionNext"
            EditorInfo.IME_ACTION_DONE -> "actionDone"
            EditorInfo.IME_ACTION_PREVIOUS -> "actionPrevious"
            else -> "actionUnknown($actionId)"
        }
    }
}