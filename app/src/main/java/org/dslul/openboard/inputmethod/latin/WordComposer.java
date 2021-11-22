/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.dslul.openboard.inputmethod.latin;

import org.dslul.openboard.inputmethod.annotations.UsedForTesting;
import org.dslul.openboard.inputmethod.event.CombinerChain;
import org.dslul.openboard.inputmethod.event.Event;
import org.dslul.openboard.inputmethod.latin.common.ComposedData;
import org.dslul.openboard.inputmethod.latin.common.Constants;
import org.dslul.openboard.inputmethod.latin.common.CoordinateUtils;
import org.dslul.openboard.inputmethod.latin.common.InputPointers;
import org.dslul.openboard.inputmethod.latin.define.DecoderSpecificConstants;

import java.util.ArrayList;

import javax.annotation.Nonnull;

/**
 * A place to store the currently composing word with information such as adjacent key codes as well
 */
public final class WordComposer {
    private static final int MAX_WORD_LENGTH = DecoderSpecificConstants.DICTIONARY_MAX_WORD_LENGTH;

    public static final int CAPS_MODE_OFF = 0;

    private CombinerChain mCombinerChain;

    // The list of events that served to compose this string.
    private final ArrayList<Event> mEvents;
    private final InputPointers mInputPointers = new InputPointers(MAX_WORD_LENGTH);
    private boolean mIsBatchMode;

    // Cache these values for performance
    private CharSequence mTypedWordCache;
    // This is the number of code points entered so far. This is not limited to MAX_WORD_LENGTH.
    // In general, this contains the size of mPrimaryKeyCodes, except when this is greater than
    // MAX_WORD_LENGTH in which case mPrimaryKeyCodes only contain the first MAX_WORD_LENGTH
    // code points.
    private int mCodePointSize;

    /**
     * Whether the composing word has the only first char capitalized.
     */
    private boolean mIsOnlyFirstCharCapitalized;

    public WordComposer() {
        mCombinerChain = new CombinerChain("");
        mEvents = new ArrayList<>();
        mIsBatchMode = false;
        refreshTypedWordCache();
    }

    public ComposedData getComposedDataSnapshot() {
        return new ComposedData(getInputPointers(), isBatchMode(), mTypedWordCache.toString());
    }

    /**
     * Clear out the keys registered so far.
     */
    public void reset() {
        mCombinerChain.reset();
        mEvents.clear();
        mIsOnlyFirstCharCapitalized = false;
        mIsBatchMode = false;
        refreshTypedWordCache();
    }

    private final void refreshTypedWordCache() {
        mTypedWordCache = mCombinerChain.getComposingWordWithCombiningFeedback();
        mCodePointSize = Character.codePointCount(mTypedWordCache, 0, mTypedWordCache.length());
    }

    /**
     * Number of keystrokes in the composing word.
     * @return the number of keystrokes
     */
    public int size() {
        return mCodePointSize;
    }

    public InputPointers getInputPointers() {
        return mInputPointers;
    }

    /**
     * Process an event and return an event, and return a processed event to apply.
     * @param event the unprocessed event.
     * @return the processed event. Never null, but may be marked as consumed.
     */
    @Nonnull
    public Event processEvent(@Nonnull final Event event) {
        final Event processedEvent = mCombinerChain.processEvent(mEvents, event);
        // The retained state of the combiner chain may have changed while processing the event,
        // so we need to update our cache.
        refreshTypedWordCache();
        mEvents.add(event);
        return processedEvent;
    }

    /**
     * Apply a processed input event.
     *
     * All input events should be supported, including software/hardware events, characters as well
     * as deletions, multiple inputs and gestures.
     *
     * @param event the event to apply. Must not be null.
     */
    public void applyProcessedEvent(final Event event) {
        mCombinerChain.applyProcessedEvent(event);
        final int primaryCode = event.getMCodePoint();
        final int keyX = event.getMX();
        final int keyY = event.getMY();
        final int newIndex = size();
        refreshTypedWordCache();
        // We may have deleted the last one.
        if (0 == mCodePointSize) {
            mIsOnlyFirstCharCapitalized = false;
        }
        if (Constants.CODE_DELETE != event.getMKeyCode()) {
            if (newIndex < MAX_WORD_LENGTH) {
                // In the batch input mode, the {@code mInputPointers} holds batch input points and
                // shouldn't be overridden by the "typed key" coordinates
                // (See {@link #setBatchInputWord}).
                if (!mIsBatchMode) {
                    // TODO: Set correct pointer id and time
                    mInputPointers.addPointerAt(newIndex, keyX, keyY, 0, 0);
                }
            }
            if (0 == newIndex) {
                mIsOnlyFirstCharCapitalized = Character.isUpperCase(primaryCode);
            } else {
                mIsOnlyFirstCharCapitalized = mIsOnlyFirstCharCapitalized
                        && !Character.isUpperCase(primaryCode);
            }
        }
    }

    /**
     * Set the currently composing word to the one passed as an argument.
     * This will register NOT_A_COORDINATE for X and Ys, and use the passed keyboard for proximity.
     * @param codePoints the code points to set as the composing word.
     * @param coordinates the x, y coordinates of the key in the CoordinateUtils format
     */
    public void setComposingWord(final int[] codePoints, final int[] coordinates) {
        reset();
        final int length = codePoints.length;
        for (int i = 0; i < length; ++i) {
            final Event processedEvent =
                    processEvent(Event.createEventForCodePointFromAlreadyTypedText(codePoints[i],
                    CoordinateUtils.xFromArray(coordinates, i),
                    CoordinateUtils.yFromArray(coordinates, i)));
            applyProcessedEvent(processedEvent);
        }
    }

    public boolean isBatchMode() {
        return mIsBatchMode;
    }

    @UsedForTesting
    void addInputPointerForTest(int index, int keyX, int keyY) {
        mInputPointers.addPointerAt(index, keyX, keyY, 0, 0);
    }

    @UsedForTesting
    void setTypedWordCacheForTests(String typedWordCacheForTests) {
        mTypedWordCache = typedWordCacheForTests;
    }
}
