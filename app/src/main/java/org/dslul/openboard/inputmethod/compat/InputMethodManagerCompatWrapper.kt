package org.dslul.openboard.inputmethod.compat

import android.content.Context
import android.view.inputmethod.InputMethodManager

class InputMethodManagerCompatWrapper(context: Context) {

    @kotlin.jvm.JvmField
    val mImm: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

}