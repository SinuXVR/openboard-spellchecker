/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.dslul.openboard.inputmethod.latin.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import org.dslul.openboard.inputmethod.compat.AppWorkaroundsUtils;
import org.dslul.openboard.inputmethod.latin.InputAttributes;
import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.inputmethod.latin.common.StringUtils;
import org.dslul.openboard.inputmethod.latin.utils.AsyncResultHolder;
import org.dslul.openboard.inputmethod.latin.utils.ResourceUtils;
import org.dslul.openboard.inputmethod.latin.utils.ScriptUtils;
import org.dslul.openboard.inputmethod.latin.utils.TargetPackageInfoGetterTask;

import java.util.Arrays;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * When you call the constructor of this class, you may want to change the current system locale by
 * using {@link org.dslul.openboard.inputmethod.latin.utils.RunInLocale}.
 */
// Non-final for testing via mock library.
public class SettingsValues {
    private static final String TAG = SettingsValues.class.getSimpleName();
    // "floatMaxValue" and "floatNegativeInfinity" are special marker strings for
    // Float.NEGATIVE_INFINITE and Float.MAX_VALUE. Currently used for auto-correction settings.
    private static final String FLOAT_MAX_VALUE_MARKER_STRING = "floatMaxValue";
    private static final String FLOAT_NEGATIVE_INFINITY_MARKER_STRING = "floatNegativeInfinity";
    private static final int TIMEOUT_TO_GET_TARGET_PACKAGE = 5; // seconds
    public static final float DEFAULT_SIZE_SCALE = 1.0f; // 100%

    // From resources:
    public final SpacingAndPunctuations mSpacingAndPunctuations;
    public final int mDelayInMillisecondsToUpdateOldSuggestions;
    public final long mDoubleSpacePeriodTimeout;
    // From configuration:
    public final Locale mLocale;
    public final boolean mHasHardwareKeyboard;
    public final int mDisplayOrientation;
    // From preferences, in the same order as xml/prefs.xml:
    public final boolean mUsePersonalizedDicts;
    public final boolean mUseDoubleSpacePeriod;
    public final boolean mBlockPotentiallyOffensive;
    public final boolean mSpaceTrackpadEnabled;
    public final boolean mDeleteSwipeEnabled;
    // Use bigrams to predict the next word when there is no input for it yet
    public final boolean mBigramPredictionEnabled;
    public final boolean mShowAppIcon;
    public final boolean mIsShowAppIconSettingInPreferences;
    public final boolean mEnableMetricsLogging;
    // Use split layout for keyboard.
    public final boolean mIsSplitKeyboardEnabled;
    public final int mScreenMetrics;

    // From the input box
    @Nonnull
    public final InputAttributes mInputAttributes;

    // Deduced settings
    private final boolean mAutoCorrectEnabled;
    public final float mAutoCorrectionThreshold;
    public final float mPlausibilityThreshold;
    public final boolean mAutoCorrectionEnabledPerUserSettings;
    private final boolean mSuggestionsEnabledPerUserSettings;
    public final boolean mIncognitoModeEnabled;
    private final AsyncResultHolder<AppWorkaroundsUtils> mAppWorkarounds;

    // Debug settings
    public final boolean mIsInternal;

    public SettingsValues(final Context context, final SharedPreferences prefs, final Resources res,
                          @Nonnull final InputAttributes inputAttributes) {
        mLocale = res.getConfiguration().locale;
        // Get the resources
        mDelayInMillisecondsToUpdateOldSuggestions =
                res.getInteger(R.integer.config_delay_in_milliseconds_to_update_old_suggestions);
        mSpacingAndPunctuations = new SpacingAndPunctuations(res);

        // Store the input attributes
        mInputAttributes = inputAttributes;

        // Get the settings preferences
        mUsePersonalizedDicts = prefs.getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true);
        mUseDoubleSpacePeriod = prefs.getBoolean(Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, true)
                && inputAttributes.mIsGeneralTextInput;
        mBlockPotentiallyOffensive = Settings.readBlockPotentiallyOffensive(prefs, res);
        mAutoCorrectEnabled = Settings.readAutoCorrectEnabled(prefs, res);
        final String autoCorrectionThresholdRawValue = mAutoCorrectEnabled
                ? res.getString(R.string.auto_correction_threshold_mode_index_modest)
                : res.getString(R.string.auto_correction_threshold_mode_index_off);
        mBigramPredictionEnabled = readBigramPredictionEnabled(prefs, res);
        mDoubleSpacePeriodTimeout = res.getInteger(R.integer.config_double_space_period_timeout);
        mHasHardwareKeyboard = Settings.readHasHardwareKeyboard(res.getConfiguration());
        mEnableMetricsLogging = prefs.getBoolean(Settings.PREF_ENABLE_METRICS_LOGGING, true);
        mIsSplitKeyboardEnabled = prefs.getBoolean(Settings.PREF_ENABLE_SPLIT_KEYBOARD, false);
        mScreenMetrics = Settings.readScreenMetrics(res);

        // Compute other readable settings
        mShowAppIcon = Settings.readShowSetupWizardIcon(prefs, context);
        mIsShowAppIconSettingInPreferences = prefs.contains(Settings.PREF_SHOW_SETUP_WIZARD_ICON);
        mAutoCorrectionThreshold = readAutoCorrectionThreshold(res,
                autoCorrectionThresholdRawValue);
        mPlausibilityThreshold = Settings.readPlausibilityThreshold(res);
        mAutoCorrectionEnabledPerUserSettings = mAutoCorrectEnabled;
                //&& !mInputAttributes.mInputTypeNoAutoCorrect;
        mSuggestionsEnabledPerUserSettings = !mInputAttributes.mIsPasswordField &&
                readSuggestionsEnabled(prefs);
        mIncognitoModeEnabled = Settings.readAlwaysIncognitoMode(prefs) || mInputAttributes.mNoLearning;
        mIsInternal = Settings.isInternal(prefs);
        final float defaultKeyPreviewShowUpStartScale = ResourceUtils.getFloatFromFraction(
                res, R.fraction.config_key_preview_show_up_start_scale);
        final float defaultKeyPreviewDismissEndScale = ResourceUtils.getFloatFromFraction(
                res, R.fraction.config_key_preview_dismiss_end_scale);
        mDisplayOrientation = res.getConfiguration().orientation;
        mAppWorkarounds = new AsyncResultHolder<>("AppWorkarounds");
        final PackageInfo packageInfo = TargetPackageInfoGetterTask.getCachedPackageInfo(
                mInputAttributes.mTargetApplicationPackageName);
        if (null != packageInfo) {
            mAppWorkarounds.set(new AppWorkaroundsUtils(packageInfo));
        } else {
            new TargetPackageInfoGetterTask(context, mAppWorkarounds)
                    .execute(mInputAttributes.mTargetApplicationPackageName);
        }
        mSpaceTrackpadEnabled = Settings.readSpaceTrackpadEnabled(prefs);
        mDeleteSwipeEnabled = Settings.readDeleteSwipeEnabled(prefs);
    }

    public boolean isMetricsLoggingEnabled() {
        return mEnableMetricsLogging;
    }

    public boolean isApplicationSpecifiedCompletionsOn() {
        return mInputAttributes.mApplicationSpecifiedCompletionOn;
    }

    public boolean needsToLookupSuggestions() {
        return mInputAttributes.mShouldShowSuggestions
                && (mAutoCorrectionEnabledPerUserSettings || isSuggestionsEnabledPerUserSettings());
    }

    public boolean isSuggestionsEnabledPerUserSettings() {
        return mSuggestionsEnabledPerUserSettings;
    }

    public boolean isPersonalizationEnabled() {
        return mUsePersonalizedDicts;
    }

    public boolean isWordSeparator(final int code) {
        return mSpacingAndPunctuations.isWordSeparator(code);
    }

    public boolean isWordConnector(final int code) {
        return mSpacingAndPunctuations.isWordConnector(code);
    }

    public boolean isWordCodePoint(final int code) {
        return Character.isLetter(code) || isWordConnector(code)
                || Character.COMBINING_SPACING_MARK == Character.getType(code);
    }

    public boolean isUsuallyPrecededBySpace(final int code) {
        return mSpacingAndPunctuations.isUsuallyPrecededBySpace(code);
    }

    public boolean isUsuallyFollowedBySpace(final int code) {
        return mSpacingAndPunctuations.isUsuallyFollowedBySpace(code);
    }

    public boolean shouldInsertSpacesAutomatically() {
        return mInputAttributes.mShouldInsertSpacesAutomatically;
    }

    public boolean isSameInputType(final EditorInfo editorInfo) {
        return mInputAttributes.isSameInputType(editorInfo);
    }

    public boolean hasSameOrientation(final Configuration configuration) {
        return mDisplayOrientation == configuration.orientation;
    }

    private static final String SUGGESTIONS_VISIBILITY_HIDE_VALUE_OBSOLETE = "2";

    private static boolean readSuggestionsEnabled(final SharedPreferences prefs) {
        if (prefs.contains(Settings.PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE)) {
            final boolean alwaysHide = SUGGESTIONS_VISIBILITY_HIDE_VALUE_OBSOLETE.equals(
                    prefs.getString(Settings.PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE, null));
            prefs.edit()
                    .remove(Settings.PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE)
                    .putBoolean(Settings.PREF_SHOW_SUGGESTIONS, !alwaysHide)
                    .apply();
        }
        return prefs.getBoolean(Settings.PREF_SHOW_SUGGESTIONS, true);
    }

    private static boolean readBigramPredictionEnabled(final SharedPreferences prefs,
                                                       final Resources res) {
        return prefs.getBoolean(Settings.PREF_BIGRAM_PREDICTIONS, res.getBoolean(
                R.bool.config_default_next_word_prediction));
    }

    private static float readAutoCorrectionThreshold(final Resources res,
                                                     final String currentAutoCorrectionSetting) {
        final String[] autoCorrectionThresholdValues = res.getStringArray(
                R.array.auto_correction_threshold_values);
        // When autoCorrectionThreshold is greater than 1.0, it's like auto correction is off.
        final float autoCorrectionThreshold;
        try {
            final int arrayIndex = Integer.parseInt(currentAutoCorrectionSetting);
            if (arrayIndex >= 0 && arrayIndex < autoCorrectionThresholdValues.length) {
                final String val = autoCorrectionThresholdValues[arrayIndex];
                if (FLOAT_MAX_VALUE_MARKER_STRING.equals(val)) {
                    autoCorrectionThreshold = Float.MAX_VALUE;
                } else if (FLOAT_NEGATIVE_INFINITY_MARKER_STRING.equals(val)) {
                    autoCorrectionThreshold = Float.NEGATIVE_INFINITY;
                } else {
                    autoCorrectionThreshold = Float.parseFloat(val);
                }
            } else {
                autoCorrectionThreshold = Float.MAX_VALUE;
            }
        } catch (final NumberFormatException e) {
            // Whenever the threshold settings are correct, never come here.
            Log.w(TAG, "Cannot load auto correction threshold setting."
                    + " currentAutoCorrectionSetting: " + currentAutoCorrectionSetting
                    + ", autoCorrectionThresholdValues: "
                    + Arrays.toString(autoCorrectionThresholdValues), e);
            return Float.MAX_VALUE;
        }
        return autoCorrectionThreshold;
    }

    private static boolean needsToShowVoiceInputKey(final SharedPreferences prefs,
                                                    final Resources res) {
        // Migrate preference from {@link Settings#PREF_VOICE_MODE_OBSOLETE} to
        // {@link Settings#PREF_VOICE_INPUT_KEY}.
        if (prefs.contains(Settings.PREF_VOICE_MODE_OBSOLETE)) {
            final String voiceModeMain = res.getString(R.string.voice_mode_main);
            final String voiceMode = prefs.getString(
                    Settings.PREF_VOICE_MODE_OBSOLETE, voiceModeMain);
            final boolean shouldShowVoiceInputKey = voiceModeMain.equals(voiceMode);
            prefs.edit()
                    .putBoolean(Settings.PREF_VOICE_INPUT_KEY, shouldShowVoiceInputKey)
                    // Remove the obsolete preference if exists.
                    .remove(Settings.PREF_VOICE_MODE_OBSOLETE)
                    .apply();
        }
        return prefs.getBoolean(Settings.PREF_VOICE_INPUT_KEY, true);
    }

    public String dump() {
        final StringBuilder sb = new StringBuilder("Current settings :");
        sb.append("\n   mSpacingAndPunctuations = ");
        sb.append("" + mSpacingAndPunctuations.dump());
        sb.append("\n   mDelayInMillisecondsToUpdateOldSuggestions = ");
        sb.append("" + mDelayInMillisecondsToUpdateOldSuggestions);
        sb.append("" + mUsePersonalizedDicts);
        sb.append("\n   mUseDoubleSpacePeriod = ");
        sb.append("" + mUseDoubleSpacePeriod);
        sb.append("\n   mBlockPotentiallyOffensive = ");
        sb.append("" + mBlockPotentiallyOffensive);
        sb.append("\n   mBigramPredictionEnabled = ");
        sb.append("" + mBigramPredictionEnabled);
        sb.append("\n   mLocale = ");
        sb.append("" + mLocale);
        sb.append("\n   mInputAttributes = ");
        sb.append("" + mInputAttributes);
        sb.append("\n   mAutoCorrectEnabled = ");
        sb.append("" + mAutoCorrectEnabled);
        sb.append("\n   mAutoCorrectionThreshold = ");
        sb.append("" + mAutoCorrectionThreshold);
        sb.append("\n   mAutoCorrectionEnabledPerUserSettings = ");
        sb.append("" + mAutoCorrectionEnabledPerUserSettings);
        sb.append("\n   mSuggestionsEnabledPerUserSettings = ");
        sb.append("" + mSuggestionsEnabledPerUserSettings);
        sb.append("\n   mDisplayOrientation = ");
        sb.append("" + mDisplayOrientation);
        sb.append("\n   mAppWorkarounds = ");
        final AppWorkaroundsUtils awu = mAppWorkarounds.get(null, 0);
        sb.append("" + (null == awu ? "null" : awu.toString()));
        sb.append("\n   mIsInternal = ");
        sb.append("" + mIsInternal);
        return sb.toString();
    }
}
