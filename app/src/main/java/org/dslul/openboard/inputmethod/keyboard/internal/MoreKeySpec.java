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

package org.dslul.openboard.inputmethod.keyboard.internal;

import android.text.TextUtils;

import org.dslul.openboard.inputmethod.latin.common.Constants;
import org.dslul.openboard.inputmethod.latin.common.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The more key specification object. The more keys are an array of {@link MoreKeySpec}.
 *
 * The more keys specification is comma separated "key specification" each of which represents one
 * "more key".
 * The key specification might have label or string resource reference in it. These references are
 * expanded before parsing comma.
 * Special character, comma ',' backslash '\' can be escaped by '\' character.
 * Note that the '\' is also parsed by XML parser and {@link MoreKeySpec#splitKeySpecs(String)}
 * as well.
 */
// TODO: Should extend the key specification object.
public final class MoreKeySpec {
    public final int mCode;
    @Nullable
    public final String mLabel;
    @Nullable
    public final String mOutputText;

    public MoreKeySpec(@Nonnull final String moreKeySpec, boolean needsToUpperCase,
            @Nonnull final Locale locale) {
        if (moreKeySpec.isEmpty()) {
            throw new KeySpecParser.KeySpecParserError("Empty more key spec");
        }
        final String label = KeySpecParser.getLabel(moreKeySpec);
        mLabel = needsToUpperCase ? StringUtils.toTitleCaseOfKeyLabel(label, locale) : label;
        final int codeInSpec = KeySpecParser.getCode(moreKeySpec);
        final int code = needsToUpperCase ? StringUtils.toTitleCaseOfKeyCode(codeInSpec, locale)
                : codeInSpec;
        if (code == Constants.CODE_UNSPECIFIED) {
            // Some letter, for example German Eszett (U+00DF: "ß"), has multiple characters
            // upper case representation ("SS").
            mCode = Constants.CODE_OUTPUT_TEXT;
            mOutputText = mLabel;
        } else {
            mCode = code;
            final String outputText = KeySpecParser.getOutputText(moreKeySpec);
            mOutputText = needsToUpperCase
                    ? StringUtils.toTitleCaseOfKeyLabel(outputText, locale) : outputText;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 + mCode;
        final String label = mLabel;
        hashCode = hashCode * 31 + (label == null ? 0 : label.hashCode());
        final String outputText = mOutputText;
        hashCode = hashCode * 31 + (outputText == null ? 0 : outputText.hashCode());
        return hashCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MoreKeySpec) {
            final MoreKeySpec other = (MoreKeySpec)o;
            return mCode == other.mCode
                    && TextUtils.equals(mLabel, other.mLabel)
                    && TextUtils.equals(mOutputText, other.mOutputText);
        }
        return false;
    }

    // Constants for parsing.
    private static final char COMMA = Constants.CODE_COMMA;
    private static final char BACKSLASH = Constants.CODE_BACKSLASH;

    /**
     * Split the text containing multiple key specifications separated by commas into an array of
     * key specifications.
     * A key specification can contain a character escaped by the backslash character, including a
     * comma character.
     * Note that an empty key specification will be eliminated from the result array.
     *
     * @param text the text containing multiple key specifications.
     * @return an array of key specification text. Null if the specified <code>text</code> is empty
     * or has no key specifications.
     */
    @Nullable
    public static String[] splitKeySpecs(@Nullable final String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        final int size = text.length();
        // Optimization for one-letter key specification.
        if (size == 1) {
            return text.charAt(0) == COMMA ? null : new String[] { text };
        }

        ArrayList<String> list = null;
        int start = 0;
        // The characters in question in this loop are COMMA and BACKSLASH. These characters never
        // match any high or low surrogate character. So it is OK to iterate through with char
        // index.
        for (int pos = 0; pos < size; pos++) {
            final char c = text.charAt(pos);
            if (c == COMMA) {
                // Skip empty entry.
                if (pos - start > 0) {
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    list.add(text.substring(start, pos));
                }
                // Skip comma
                start = pos + 1;
            } else if (c == BACKSLASH) {
                // Skip escape character and escaped character.
                pos++;
            }
        }
        final String remain = (size - start > 0) ? text.substring(start) : null;
        if (list == null) {
            return remain != null ? new String[] { remain } : null;
        }
        if (remain != null) {
            list.add(remain);
        }
        return list.toArray(new String[list.size()]);
    }
}
