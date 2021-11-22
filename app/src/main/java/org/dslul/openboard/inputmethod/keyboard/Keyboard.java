/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.dslul.openboard.inputmethod.keyboard;

import android.util.SparseArray;

import org.dslul.openboard.inputmethod.keyboard.internal.KeyboardParams;
import org.dslul.openboard.inputmethod.latin.common.Constants;
import org.dslul.openboard.inputmethod.latin.common.CoordinateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys.
 * <p>The layout file for a keyboard contains XML that looks like the following snippet:</p>
 * <pre>
 * &lt;Keyboard
 *         latin:keyWidth="10%p"
 *         latin:rowHeight="50px"
 *         latin:horizontalGap="2%p"
 *         latin:verticalGap="2%p" &gt;
 *     &lt;Row latin:keyWidth="10%p" &gt;
 *         &lt;Key latin:keyLabel="A" /&gt;
 *         ...
 *     &lt;/Row&gt;
 *     ...
 * &lt;/Keyboard&gt;
 * </pre>
 */
public class Keyboard {
    @Nonnull
    public final KeyboardId mId;

    /** Total height of the keyboard, including the padding and keys */
    public final int mOccupiedHeight;
    /** Total width of the keyboard, including the padding and keys */
    public final int mOccupiedWidth;

    /** Base height of the keyboard, used to calculate rows' height */
    public final int mBaseHeight;
    /** Base width of the keyboard, used to calculate keys' width */
    public final int mBaseWidth;

    /** The padding above the keyboard */
    public final int mTopPadding;
    /** Default gap between rows */
    public final int mVerticalGap;

    public final int mMostCommonKeyHeight;
    public final int mMostCommonKeyWidth;

    /** More keys keyboard template */
    public final int mMoreKeysTemplate;

    /** Maximum column for more keys keyboard */
    public final int mMaxMoreKeysKeyboardColumn;

    /** List of keys in this keyboard */
    @Nonnull
    private final List<Key> mSortedKeys;
    @Nonnull
    public final List<Key> mShiftKeys;
    @Nonnull
    public final List<Key> mAltCodeKeysWhileTyping;

    private final SparseArray<Key> mKeyCache = new SparseArray<>();

    @Nonnull
    private final ProximityInfo mProximityInfo;

    private final boolean mProximityCharsCorrectionEnabled;

    public Keyboard(@Nonnull final KeyboardParams params) {
        mId = params.mId;
        mOccupiedHeight = params.mOccupiedHeight;
        mOccupiedWidth = params.mOccupiedWidth;
        mBaseHeight = params.mBaseHeight;
        mBaseWidth = params.mBaseWidth;
        mMostCommonKeyHeight = params.mMostCommonKeyHeight;
        mMostCommonKeyWidth = params.mMostCommonKeyWidth;
        mMoreKeysTemplate = params.mMoreKeysTemplate;
        mMaxMoreKeysKeyboardColumn = params.mMaxMoreKeysKeyboardColumn;
        mTopPadding = params.mTopPadding;
        mVerticalGap = params.mVerticalGap;

        mSortedKeys = Collections.unmodifiableList(new ArrayList<>(params.mSortedKeys));
        mShiftKeys = Collections.unmodifiableList(params.mShiftKeys);
        mAltCodeKeysWhileTyping = Collections.unmodifiableList(params.mAltCodeKeysWhileTyping);

        mProximityInfo = new ProximityInfo(params.GRID_WIDTH, params.GRID_HEIGHT,
                mOccupiedWidth, mOccupiedHeight, mMostCommonKeyWidth, mMostCommonKeyHeight,
                mSortedKeys, params.mTouchPositionCorrection);
        mProximityCharsCorrectionEnabled = params.mProximityCharsCorrectionEnabled;
    }

    protected Keyboard(@Nonnull final Keyboard keyboard) {
        mId = keyboard.mId;
        mOccupiedHeight = keyboard.mOccupiedHeight;
        mOccupiedWidth = keyboard.mOccupiedWidth;
        mBaseHeight = keyboard.mBaseHeight;
        mBaseWidth = keyboard.mBaseWidth;
        mMostCommonKeyHeight = keyboard.mMostCommonKeyHeight;
        mMostCommonKeyWidth = keyboard.mMostCommonKeyWidth;
        mMoreKeysTemplate = keyboard.mMoreKeysTemplate;
        mMaxMoreKeysKeyboardColumn = keyboard.mMaxMoreKeysKeyboardColumn;
        mTopPadding = keyboard.mTopPadding;
        mVerticalGap = keyboard.mVerticalGap;

        mSortedKeys = keyboard.mSortedKeys;
        mShiftKeys = keyboard.mShiftKeys;
        mAltCodeKeysWhileTyping = keyboard.mAltCodeKeysWhileTyping;

        mProximityInfo = keyboard.mProximityInfo;
        mProximityCharsCorrectionEnabled = keyboard.mProximityCharsCorrectionEnabled;
    }

    @Nonnull
    public ProximityInfo getProximityInfo() {
        return mProximityInfo;
    }

    /**
     * Return the sorted list of keys of this keyboard.
     * The keys are sorted from top-left to bottom-right order.
     * The list may contain {@link Key.Spacer} object as well.
     * @return the sorted unmodifiable list of {@link Key}s of this keyboard.
     */
    @Nonnull
    public List<Key> getSortedKeys() {
        return mSortedKeys;
    }

    @Nullable
    public Key getKey(final int code) {
        if (code == Constants.CODE_UNSPECIFIED) {
            return null;
        }
        synchronized (mKeyCache) {
            final int index = mKeyCache.indexOfKey(code);
            if (index >= 0) {
                return mKeyCache.valueAt(index);
            }

            for (final Key key : getSortedKeys()) {
                if (key.getCode() == code) {
                    mKeyCache.put(code, key);
                    return key;
                }
            }
            mKeyCache.put(code, null);
            return null;
        }
    }

    @Nonnull
    public int[] getCoordinates(@Nonnull final int[] codePoints) {
        final int length = codePoints.length;
        final int[] coordinates = CoordinateUtils.newCoordinateArray(length);
        for (int i = 0; i < length; ++i) {
            final Key key = getKey(codePoints[i]);
            if (null != key) {
                CoordinateUtils.setXYInArray(coordinates, i,
                        key.getX() + key.getWidth() / 2, key.getY() + key.getHeight() / 2);
            } else {
                CoordinateUtils.setXYInArray(coordinates, i,
                        Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE);
            }
        }
        return coordinates;
    }
}
