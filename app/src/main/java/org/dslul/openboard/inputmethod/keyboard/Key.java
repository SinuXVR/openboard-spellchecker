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

import android.content.res.TypedArray;
import android.graphics.Rect;
import android.text.TextUtils;

import org.dslul.openboard.inputmethod.keyboard.internal.KeySpecParser;
import org.dslul.openboard.inputmethod.keyboard.internal.KeyStyle;
import org.dslul.openboard.inputmethod.keyboard.internal.KeyboardParams;
import org.dslul.openboard.inputmethod.keyboard.internal.KeyboardRow;
import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.inputmethod.latin.common.StringUtils;

import java.util.Arrays;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.dslul.openboard.inputmethod.latin.common.Constants.CODE_OUTPUT_TEXT;
import static org.dslul.openboard.inputmethod.latin.common.Constants.CODE_UNSPECIFIED;

/**
 * Class for describing the position and characteristics of a single key in the keyboard.
 */
public class Key implements Comparable<Key> {
    /**
     * The key code (unicode or custom code) that this key generates.
     */
    private final int mCode;

    /** Label to display */
    private final String mLabel;

    /** Flags of the label */
    private final int mLabelFlags;

    private static final int LABEL_FLAGS_PRESERVE_CASE = 0x10000;
    private static final int LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL = 0x40000;


    /** Width of the key, excluding the gap */
    private final int mWidth;
    /** Height of the key, excluding the gap */
    private final int mHeight;
    /**
     * The combined width in pixels of the horizontal gaps belonging to this key, both to the left
     * and to the right. I.e., mWidth + mHorizontalGap = total width belonging to the key.
     */
    private final int mHorizontalGap;
    /**
     * The combined height in pixels of the vertical gaps belonging to this key, both above and
     * below. I.e., mHeight + mVerticalGap = total height belonging to the key.
     */
    private final int mVerticalGap;
    /** X coordinate of the top-left corner of the key in the keyboard layout, excluding the gap. */
    private final int mX;
    /** Y coordinate of the top-left corner of the key in the keyboard layout, excluding the gap. */
    private final int mY;
    /** Hit bounding box of the key */
    @Nonnull
    private final Rect mHitBox = new Rect();

    private final int mHashCode;

    /**
     * Constructor for a key on <code>MoreKeyKeyboard</code> and on <code>MoreSuggestions</code>.
     */
    public Key(@Nullable final String label, final int code,
            final int labelFlags, final int x, final int y,
            final int width, final int height, final int horizontalGap, final int verticalGap) {
        mWidth = width - horizontalGap;
        mHeight = height - verticalGap;
        mHorizontalGap = horizontalGap;
        mVerticalGap = verticalGap;
        mLabelFlags = labelFlags;
        mLabel = label;
        mCode = code;
        // Horizontal gap is divided equally to both sides of the key.
        mX = x + mHorizontalGap / 2;
        mY = y;
        mHitBox.set(x, y, x + width + 1, y + height);

        mHashCode = computeHashCode(this);
    }

    /**
     * Constructor for a key in a <GridRows/>.
     */
    public Key(@Nullable final String label, final int code, final int labelFlags,
               final int x, final int y, final int width, final int height, final KeyboardParams params) {
        mWidth = width - params.mHorizontalGap;
        mHeight = height - params.mVerticalGap;
        mHorizontalGap = params.mHorizontalGap;
        mVerticalGap = params.mVerticalGap;
        mLabelFlags = labelFlags;
        mLabel = label;
        mCode = code;
        // Horizontal gap is divided equally to both sides of the key.
        mX = x + mHorizontalGap / 2;
        mY = y;
        mHitBox.set(x, y, x + width + 1, y + height);

        mHashCode = computeHashCode(this);
    }

    /**
     * Create a key with the given top-left coordinate and extract its attributes from a key
     * specification string, Key attribute array, key style, and etc.
     *
     * @param keySpec the key specification.
     * @param keyAttr the Key XML attributes array.
     * @param style the {@link KeyStyle} of this key.
     * @param params the keyboard building parameters.
     * @param row the row that this key belongs to. row's x-coordinate will be the right edge of
     *        this key.
     */
    public Key(@Nullable final String keySpec, @Nonnull final TypedArray keyAttr,
               @Nonnull final KeyStyle style, @Nonnull final KeyboardParams params,
               @Nonnull final KeyboardRow row) {
        mHorizontalGap = isSpacer() ? 0 : params.mHorizontalGap;
        mVerticalGap = params.mVerticalGap;

        final float horizontalGapFloat = mHorizontalGap;
        final int rowHeight = row.getRowHeight();
        mHeight = rowHeight - mVerticalGap;

        final float keyXPos = row.getKeyX(keyAttr);
        final float keyWidth = row.getKeyWidth(keyAttr, keyXPos);
        final int keyYPos = row.getKeyY();

        // Horizontal gap is divided equally to both sides of the key.
        mX = Math.round(keyXPos + horizontalGapFloat / 2);
        mY = keyYPos;
        mWidth = Math.round(keyWidth - horizontalGapFloat);
        mHitBox.set(Math.round(keyXPos), keyYPos, Math.round(keyXPos + keyWidth) + 1,
                keyYPos + rowHeight);
        // Update row to have current x coordinate.
        row.setXPos(keyXPos + keyWidth);

        mLabelFlags = style.getFlags(keyAttr, R.styleable.Keyboard_Key_keyLabelFlags)
                | row.getDefaultKeyLabelFlags();
        final boolean needsToUpcase = needsToUpcase(mLabelFlags, params.mId.mElementId);
        final Locale localeForUpcasing = params.mId.getLocale();

        final int code = KeySpecParser.getCode(keySpec);
        if ((mLabelFlags & LABEL_FLAGS_FROM_CUSTOM_ACTION_LABEL) != 0) {
            mLabel = params.mId.mCustomActionLabel;
        } else if (code >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // This is a workaround to have a key that has a supplementary code point in its label.
            // Because we can put a string in resource neither as a XML entity of a supplementary
            // code point nor as a surrogate pair.
            mLabel = new StringBuilder().appendCodePoint(code).toString();
        } else {
            final String label = KeySpecParser.getLabel(keySpec);
            mLabel = needsToUpcase
                    ? StringUtils.toTitleCaseOfKeyLabel(label, localeForUpcasing)
                    : label;
        }

        String outputText = KeySpecParser.getOutputText(keySpec);
        if (needsToUpcase) {
            outputText = StringUtils.toTitleCaseOfKeyLabel(outputText, localeForUpcasing);
        }
        // Choose the first letter of the label as primary code if not specified.
        if (code == CODE_UNSPECIFIED && TextUtils.isEmpty(outputText)
                && !TextUtils.isEmpty(mLabel)) {
            if (StringUtils.codePointCount(mLabel) == 1) {
                mCode = mLabel.codePointAt(0);
            } else {
                // In some locale and case, the character might be represented by multiple code
                // points, such as upper case Eszett of German alphabet.
                mCode = CODE_OUTPUT_TEXT;
            }
        } else if (code == CODE_UNSPECIFIED && outputText != null) {
            if (StringUtils.codePointCount(outputText) == 1) {
                mCode = outputText.codePointAt(0);
            } else {
                mCode = CODE_OUTPUT_TEXT;
            }
        } else {
            mCode = needsToUpcase ? StringUtils.toTitleCaseOfKeyCode(code, localeForUpcasing)
                    : code;
        }

        mHashCode = computeHashCode(this);
    }

    private static boolean needsToUpcase(final int labelFlags, final int keyboardElementId) {
        if ((labelFlags & LABEL_FLAGS_PRESERVE_CASE) != 0) return false;
        switch (keyboardElementId) {
        case KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED:
        case KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED:
        case KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED:
        case KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCK_SHIFTED:
            return true;
        default:
            return false;
        }
    }

    private static int computeHashCode(final Key key) {
        return Arrays.hashCode(new Object[] {
                key.mX,
                key.mY,
                key.mWidth,
                key.mHeight,
                key.mCode,
                key.mLabel,
                key.mLabelFlags,
                // Key can be distinguishable without the following members.
                // key.mOptionalAttributes.mAltCode,
                // key.mOptionalAttributes.mDisabledIconId,
                // key.mOptionalAttributes.mPreviewIconId,
                // key.mHorizontalGap,
                // key.mVerticalGap,
                // key.mOptionalAttributes.mVisualInsetLeft,
                // key.mOptionalAttributes.mVisualInsetRight,
                // key.mMaxMoreKeysColumn,
        });
    }

    private boolean equalsInternal(final Key o) {
        if (this == o) return true;
        return o.mX == mX
                && o.mY == mY
                && o.mWidth == mWidth
                && o.mHeight == mHeight
                && o.mCode == mCode
                && TextUtils.equals(o.mLabel, mLabel)
                && o.mLabelFlags == mLabelFlags;
    }

    @Override
    public int compareTo(Key o) {
        if (equalsInternal(o)) return 0;
        if (mHashCode > o.mHashCode) return 1;
        return -1;
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof Key && equalsInternal((Key)o);
    }

    public int getCode() {
        return mCode;
    }

    @Nullable
    public String getLabel() {
        return mLabel;
    }

    public void markAsLeftEdge(final KeyboardParams params) {
        mHitBox.left = params.mLeftPadding;
    }

    public void markAsRightEdge(final KeyboardParams params) {
        mHitBox.right = params.mOccupiedWidth - params.mRightPadding;
    }

    public void markAsTopEdge(final KeyboardParams params) {
        mHitBox.top = params.mTopPadding;
    }

    public final boolean isSpacer() {
        return this instanceof Spacer;
    }

    /**
     * Gets the width of the key in pixels, excluding the gap.
     * @return The width of the key in pixels, excluding the gap.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Gets the height of the key in pixels, excluding the gap.
     * @return The height of the key in pixels, excluding the gap.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Gets the x-coordinate of the top-left corner of the key in pixels, excluding the gap.
     * @return The x-coordinate of the top-left corner of the key in pixels, excluding the gap.
     */
    public int getX() {
        return mX;
    }

    /**
     * Gets the y-coordinate of the top-left corner of the key in pixels, excluding the gap.
     * @return The y-coordinate of the top-left corner of the key in pixels, excluding the gap.
     */
    public int getY() {
        return mY;
    }

    @Nonnull
    public Rect getHitBox() {
        return mHitBox;
    }

    /**
     * Returns the square of the distance to the nearest edge of the key and the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the square of the distance of the point from the nearest edge of the key
     */
    public int squaredDistanceToEdge(final int x, final int y) {
        final int left = getX();
        final int right = left + mWidth;
        final int top = getY();
        final int bottom = top + mHeight;
        final int edgeX = x < left ? left : (x > right ? right : x);
        final int edgeY = y < top ? top : (y > bottom ? bottom : y);
        final int dx = x - edgeX;
        final int dy = y - edgeY;
        return dx * dx + dy * dy;
    }

    static class KeyBackgroundState {
        private final int[] mReleasedState;
        private final int[] mPressedState;

        private KeyBackgroundState(final int ... attrs) {
            mReleasedState = attrs;
            mPressedState = Arrays.copyOf(attrs, attrs.length + 1);
            mPressedState[attrs.length] = android.R.attr.state_pressed;
        }

        public int[] getState(final boolean pressed) {
            return pressed ? mPressedState : mReleasedState;
        }

        public static final KeyBackgroundState[] STATES = {
            // 0: BACKGROUND_TYPE_EMPTY
            new KeyBackgroundState(android.R.attr.state_empty),
            // 1: BACKGROUND_TYPE_NORMAL
            new KeyBackgroundState(),
            // 2: BACKGROUND_TYPE_FUNCTIONAL
            new KeyBackgroundState(),
            // 3: BACKGROUND_TYPE_STICKY_OFF
            new KeyBackgroundState(android.R.attr.state_checkable),
            // 4: BACKGROUND_TYPE_STICKY_ON
            new KeyBackgroundState(android.R.attr.state_checkable, android.R.attr.state_checked),
            // 5: BACKGROUND_TYPE_ACTION
            new KeyBackgroundState(android.R.attr.state_active),
            // 6: BACKGROUND_TYPE_SPACEBAR
            new KeyBackgroundState(),
        };
    }

    public static class Spacer extends Key {
        public Spacer(final TypedArray keyAttr, final KeyStyle keyStyle,
                final KeyboardParams params, final KeyboardRow row) {
            super(null /* keySpec */, keyAttr, keyStyle, params, row);
        }

        /**
         * This constructor is being used only for divider in more keys keyboard.
         */
        protected Spacer(final KeyboardParams params, final int x, final int y, final int width,
                final int height) {
            super(null /* label */, CODE_UNSPECIFIED, 0 /* labelFlags */, x, y, width,
                    height, params.mHorizontalGap, params.mVerticalGap);
        }
    }
}
