package org.dslul.openboard.inputmethod.compat

import android.text.TextUtils
import android.util.Log
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object CompatUtils {
    private val TAG = CompatUtils::class.java.simpleName
    fun getClass(className: String?): Class<*>? {
        return try {
            Class.forName(className!!)
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    fun getMethod(targetClass: Class<*>?, name: String?,
                  vararg parameterTypes: Class<*>?): Method? {
        if (targetClass == null || TextUtils.isEmpty(name)) {
            return null
        }
        try {
            return targetClass.getMethod(name!!, *parameterTypes)
        } catch (e: SecurityException) { // ignore
        } catch (e: NoSuchMethodException) {
        }
        return null
    }

    fun getField(targetClass: Class<*>?, name: String?): Field? {
        if (targetClass == null || TextUtils.isEmpty(name)) {
            return null
        }
        try {
            return targetClass.getField(name!!)
        } catch (e: SecurityException) { // ignore
        } catch (e: NoSuchFieldException) {
        }
        return null
    }

    fun getConstructor(targetClass: Class<*>?,
                       vararg types: Class<*>?): Constructor<*>? {
        if (targetClass == null || types == null) {
            return null
        }
        try {
            return targetClass.getConstructor(*types)
        } catch (e: SecurityException) { // ignore
        } catch (e: NoSuchMethodException) {
        }
        return null
    }

    fun newInstance(constructor: Constructor<*>?, vararg args: Any?): Any? {
        if (constructor == null) {
            return null
        }
        try {
            return constructor.newInstance(*args)
        } catch (e: InstantiationException) {
            Log.e(TAG, "Exception in newInstance", e)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Exception in newInstance", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Exception in newInstance", e)
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Exception in newInstance", e)
        }
        return null
    }

    operator fun invoke(receiver: Any?, defaultValue: Any?,
                        method: Method?, vararg args: Any?): Any? {
        if (method == null) {
            return defaultValue
        }
        try {
            return method.invoke(receiver, *args)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Exception in invoke", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Exception in invoke", e)
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Exception in invoke", e)
        }
        return defaultValue
    }

    fun getFieldValue(receiver: Any?, defaultValue: Any?,
                      field: Field?): Any? {
        if (field == null) {
            return defaultValue
        }
        try {
            return field[receiver]
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Exception in getFieldValue", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Exception in getFieldValue", e)
        }
        return defaultValue
    }
}