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

import android.os.Build;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.Arrays;

import static org.dslul.openboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.ASCII_CAPABLE;
import static org.dslul.openboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.EMOJI_CAPABLE;
import static org.dslul.openboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.IS_ADDITIONAL_SUBTYPE;
import static org.dslul.openboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET;
import static org.dslul.openboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME;
import static org.dslul.openboard.inputmethod.latin.common.Constants.Subtype.KEYBOARD_MODE;

public final class AdditionalSubtypeUtils {

    private AdditionalSubtypeUtils() {
        // This utility class is not publicly instantiable.
    }

    private static InputMethodSubtype createAdditionalSubtypeInternal(
            final String localeString, final String keyboardLayoutSetName,
            final boolean isAsciiCapable, final boolean isEmojiCapable) {
        final int nameId = SubtypeLocaleUtils.getSubtypeNameId(localeString, keyboardLayoutSetName);
        final String platformVersionDependentExtraValues = getPlatformVersionDependentExtraValue(
                localeString, keyboardLayoutSetName, isAsciiCapable, isEmojiCapable);
        final int platformVersionIndependentSubtypeId =
                getPlatformVersionIndependentSubtypeId(localeString, keyboardLayoutSetName);
        // NOTE: In KitKat and later, InputMethodSubtypeBuilder#setIsAsciiCapable is also available.
        // TODO: Use InputMethodSubtypeBuilder#setIsAsciiCapable when appropriate.
        return new InputMethodSubtype(nameId,
                0, localeString, KEYBOARD_MODE,
                platformVersionDependentExtraValues,
                false /* isAuxiliary */, false /* overrideImplicitlyEnabledSubtype */,
                platformVersionIndependentSubtypeId);
    }

    public static InputMethodSubtype createDummyAdditionalSubtype(
            final String localeString, final String keyboardLayoutSetName) {
        return createAdditionalSubtypeInternal(localeString, keyboardLayoutSetName,
                false /* isAsciiCapable */, false /* isEmojiCapable */);
    }

    /**
     * Returns the extra value that is optimized for the running OS.
     * <p>
     * Historically the extra value has been used as the last resort to annotate various kinds of
     * attributes. Some of these attributes are valid only on some platform versions. Thus we cannot
     * assume that the extra values stored in a persistent storage are always valid. We need to
     * regenerate the extra value on the fly instead.
     * </p>
     * @param localeString the locale string (e.g., "en_US").
     * @param keyboardLayoutSetName the keyboard layout set name (e.g., "dvorak").
     * @param isAsciiCapable true when ASCII characters are supported with this layout.
     * @param isEmojiCapable true when Unicode Emoji characters are supported with this layout.
     * @return extra value that is optimized for the running OS.
     * @see #getPlatformVersionIndependentSubtypeId(String, String)
     */
    private static String getPlatformVersionDependentExtraValue(final String localeString,
            final String keyboardLayoutSetName, final boolean isAsciiCapable,
            final boolean isEmojiCapable) {
        final ArrayList<String> extraValueItems = new ArrayList<>();
        extraValueItems.add(KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName);
        if (isAsciiCapable) {
            extraValueItems.add(ASCII_CAPABLE);
        }
        if (SubtypeLocaleUtils.isExceptionalLocale(localeString)) {
            extraValueItems.add(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                    SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName));
        }
        if (isEmojiCapable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            extraValueItems.add(EMOJI_CAPABLE);
        }
        extraValueItems.add(IS_ADDITIONAL_SUBTYPE);
        return TextUtils.join(",", extraValueItems);
    }

    /**
     * Returns the subtype ID that is supposed to be compatible between different version of OSes.
     * <p>
     * From the compatibility point of view, it is important to keep subtype id predictable and
     * stable between different OSes. For this purpose, the calculation code in this method is
     * carefully chosen and then fixed. Treat the following code as no more or less than a
     * hash function. Each component to be hashed can be different from the corresponding value
     * that is used to instantiate {@link InputMethodSubtype} actually.
     * For example, you don't need to update <code>compatibilityExtraValueItems</code> in this
     * method even when we need to add some new extra values for the actual instance of
     * {@link InputMethodSubtype}.
     * </p>
     * @param localeString the locale string (e.g., "en_US").
     * @param keyboardLayoutSetName the keyboard layout set name (e.g., "dvorak").
     * @return a platform-version independent subtype ID.
     * @see #getPlatformVersionDependentExtraValue(String, String, boolean, boolean)
     */
    private static int getPlatformVersionIndependentSubtypeId(final String localeString,
            final String keyboardLayoutSetName) {
        // For compatibility reasons, we concatenate the extra values in the following order.
        // - KeyboardLayoutSet
        // - AsciiCapable
        // - UntranslatableReplacementStringInSubtypeName
        // - EmojiCapable
        // - isAdditionalSubtype
        final ArrayList<String> compatibilityExtraValueItems = new ArrayList<>();
        compatibilityExtraValueItems.add(KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName);
        compatibilityExtraValueItems.add(ASCII_CAPABLE);
        if (SubtypeLocaleUtils.isExceptionalLocale(localeString)) {
            compatibilityExtraValueItems.add(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                    SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName));
        }
        compatibilityExtraValueItems.add(EMOJI_CAPABLE);
        compatibilityExtraValueItems.add(IS_ADDITIONAL_SUBTYPE);
        final String compatibilityExtraValues = TextUtils.join(",", compatibilityExtraValueItems);
        return Arrays.hashCode(new Object[] {
                localeString,
                KEYBOARD_MODE,
                compatibilityExtraValues,
                false /* isAuxiliary */,
                false /* overrideImplicitlyEnabledSubtype */ });
    }
}
