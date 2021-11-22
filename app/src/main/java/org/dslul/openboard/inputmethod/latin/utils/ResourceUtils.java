/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dslul.openboard.inputmethod.latin.utils;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;

import org.dslul.openboard.inputmethod.annotations.UsedForTesting;

import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

public final class ResourceUtils {
    private static final String TAG = ResourceUtils.class.getSimpleName();

    private ResourceUtils() {
        // This utility class is not publicly instantiable.
    }

    @SuppressWarnings("serial")
    static class DeviceOverridePatternSyntaxError extends Exception {
        public DeviceOverridePatternSyntaxError(final String message, final String expression) {
            this(message, expression, null);
        }

        public DeviceOverridePatternSyntaxError(final String message, final String expression,
                final Throwable throwable) {
            super(message + ": " + expression, throwable);
        }
    }

    /**
     * Find the condition that fulfills specified key value pairs from an array of
     * "condition,constant", and return the corresponding string constant. A condition is
     * "pattern1[:pattern2...] (or an empty string for the default). A pattern is
     * "key=regexp_value" string. The condition matches only if all patterns of the condition
     * are true for the specified key value pairs.
     *
     * For example, "condition,constant" has the following format.
     *  - HARDWARE=mako,constantForNexus4
     *  - MODEL=Nexus 4:MANUFACTURER=LGE,constantForNexus4
     *  - ,defaultConstant
     *
     * @param keyValuePairs attributes to be used to look for a matched condition.
     * @param conditionConstantArray an array of "condition,constant" elements to be searched.
     * @return the constant part of the matched "condition,constant" element. Returns null if no
     * condition matches.
     * @see org.dslul.openboard.inputmethod.latin.utils.ResourceUtilsTests#testFindConstantForKeyValuePairsRegexp()
     */
    @UsedForTesting
    static String findConstantForKeyValuePairs(final HashMap<String, String> keyValuePairs,
            final String[] conditionConstantArray) {
        if (conditionConstantArray == null || keyValuePairs == null) {
            return null;
        }
        String foundValue = null;
        for (final String conditionConstant : conditionConstantArray) {
            final int posComma = conditionConstant.indexOf(',');
            if (posComma < 0) {
                Log.w(TAG, "Array element has no comma: " + conditionConstant);
                continue;
            }
            final String condition = conditionConstant.substring(0, posComma);
            if (condition.isEmpty()) {
                Log.w(TAG, "Array element has no condition: " + conditionConstant);
                continue;
            }
            try {
                if (fulfillsCondition(keyValuePairs, condition)) {
                    // Take first match
                    if (foundValue == null) {
                        foundValue = conditionConstant.substring(posComma + 1);
                    }
                    // And continue walking through all conditions.
                }
            } catch (final DeviceOverridePatternSyntaxError e) {
                Log.w(TAG, "Syntax error, ignored", e);
            }
        }
        return foundValue;
    }

    private static boolean fulfillsCondition(final HashMap<String,String> keyValuePairs,
            final String condition) throws DeviceOverridePatternSyntaxError {
        final String[] patterns = condition.split(":");
        // Check all patterns in a condition are true
        boolean matchedAll = true;
        for (final String pattern : patterns) {
            final int posEqual = pattern.indexOf('=');
            if (posEqual < 0) {
                throw new DeviceOverridePatternSyntaxError("Pattern has no '='", condition);
            }
            final String key = pattern.substring(0, posEqual);
            final String value = keyValuePairs.get(key);
            if (value == null) {
                throw new DeviceOverridePatternSyntaxError("Unknown key", condition);
            }
            final String patternRegexpValue = pattern.substring(posEqual + 1);
            try {
                if (!value.matches(patternRegexpValue)) {
                    matchedAll = false;
                    // And continue walking through all patterns.
                }
            } catch (final PatternSyntaxException e) {
                throw new DeviceOverridePatternSyntaxError("Syntax error", condition, e);
            }
        }
        return matchedAll;
    }

    public static float getFloatFromFraction(final Resources res, final int fractionResId) {
        return res.getFraction(fractionResId, 1, 1);
    }

    public static float getDimensionOrFraction(final TypedArray a, final int index, final int base,
            final float defValue) {
        final TypedValue value = a.peekValue(index);
        if (value == null) {
            return defValue;
        }
        if (isFractionValue(value)) {
            return a.getFraction(index, base, base, defValue);
        } else if (isDimensionValue(value)) {
            return a.getDimension(index, defValue);
        }
        return defValue;
    }

    public static int getEnumValue(final TypedArray a, final int index, final int defValue) {
        final TypedValue value = a.peekValue(index);
        if (value == null) {
            return defValue;
        }
        if (isIntegerValue(value)) {
            return a.getInt(index, defValue);
        }
        return defValue;
    }

    public static boolean isFractionValue(final TypedValue v) {
        return v.type == TypedValue.TYPE_FRACTION;
    }

    public static boolean isDimensionValue(final TypedValue v) {
        return v.type == TypedValue.TYPE_DIMENSION;
    }

    public static boolean isIntegerValue(final TypedValue v) {
        return v.type >= TypedValue.TYPE_FIRST_INT && v.type <= TypedValue.TYPE_LAST_INT;
    }

    public static boolean isStringValue(final TypedValue v) {
        return v.type == TypedValue.TYPE_STRING;
    }
}
