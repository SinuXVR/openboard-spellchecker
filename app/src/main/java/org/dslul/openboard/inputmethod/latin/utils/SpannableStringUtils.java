/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.text.Spanned;
import android.text.TextUtils;

import org.dslul.openboard.inputmethod.annotations.UsedForTesting;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpannableStringUtils {

    /**
     * Splits the given {@code charSequence} with at occurrences of the given {@code regex}.
     * <p>
     * This is equivalent to
     * {@code charSequence.toString().split(regex, preserveTrailingEmptySegments ? -1 : 0)}
     * except that the spans are preserved in the result array.
     * </p>
     * @param charSequence the character sequence to be split.
     * @param regex the regex pattern to be used as the separator.
     * @param preserveTrailingEmptySegments {@code true} to preserve the trailing empty
     * segments. Otherwise, trailing empty segments will be removed before being returned.
     * @return the array which contains the result. All the spans in the <code>charSequence</code>
     * is preserved.
     */
    @UsedForTesting
    public static CharSequence[] split(final CharSequence charSequence, final String regex,
            final boolean preserveTrailingEmptySegments) {
        // A short-cut for non-spanned strings.
        if (!(charSequence instanceof Spanned)) {
            // -1 means that trailing empty segments will be preserved.
            return charSequence.toString().split(regex, preserveTrailingEmptySegments ? -1 : 0);
        }

        // Hereafter, emulate String.split for CharSequence.
        final ArrayList<CharSequence> sequences = new ArrayList<>();
        final Matcher matcher = Pattern.compile(regex).matcher(charSequence);
        int nextStart = 0;
        boolean matched = false;
        while (matcher.find()) {
            sequences.add(charSequence.subSequence(nextStart, matcher.start()));
            nextStart = matcher.end();
            matched = true;
        }
        if (!matched) {
            // never matched. preserveTrailingEmptySegments is ignored in this case.
            return new CharSequence[] { charSequence };
        }
        sequences.add(charSequence.subSequence(nextStart, charSequence.length()));
        if (!preserveTrailingEmptySegments) {
            for (int i = sequences.size() - 1; i >= 0; --i) {
                if (!TextUtils.isEmpty(sequences.get(i))) {
                    break;
                }
                sequences.remove(i);
            }
        }
        return sequences.toArray(new CharSequence[sequences.size()]);
    }
}
