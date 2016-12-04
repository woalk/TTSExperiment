package com.woalk.apps.ttsexperiment;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    //region INTENT_/BUNDLE_EXTRAS
    private static final String EXTRA_SAVED_LOG = "log";
    private static final String EXTRA_SAVED_INPUT = "in";
    private static final String EXTRA_SAVED_LANG = "lng";
    private static final String EXTRA_SAVED_VOICE = "vox";
    private static final String EXTRA_SAVED_PITCH = "pit";
    private static final String EXTRA_SAVED_SPEED = "spe";
    //endregion
    //region constants
    private static final String LOG_TAG = "TTSExperiment";
    private static final int MY_DATA_CHECK_CODE = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2;
    private static final Locale[] MY_LOCALE_LIST = new Locale[]{Locale.US, Locale.UK,
            Locale.GERMANY, Locale.ITALY, Locale.FRANCE, new Locale("es", "ES")};
    private static final String[] MY_LANG_TAG_LIST = new String[]{"en-US", "en-UK",
            "de-DE", "it-IT", "fr-FR", "es-ES"};
    private static final int MAGIC_PROGRESS = 999;
    //endregion

    //region instance variables
    private TextToSpeech mTTS;
    private ArrayAdapter<VoiceWrapper> mVoiceAdapter;
    private int mVoiceSavedSelection = -1;
    private SpeechRecognizer mSpeech;
    //endregion

    //region view instances
    private View mSpeakButton;
    private View mVoiceInButton;
    private View mVoiceInWaitIndicator;
    private TextView mTextLog;
    private ScrollView mTextLogLayout;
    private EditText mTextInput;
    private Spinner mLanguageSpinner;
    private ImageButton mSettingsButton;
    private View mSubheader;
    private View mSubheaderDivider;
    private View mVoiceSelectLayout;
    private Spinner mVoiceSelect;
    private View mPitchSelectLayout;
    private SeekBar mPitchSelect;
    private TextView mPitchValue;
    private View mSpeedSelectLayout;
    private SeekBar mSpeedSelect;
    private TextView mSpeedValue;
    private View mSubheaderDivider2;
    private CheckBox mCheckVoiceInputVariants;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get view instances
        mTextLog = (TextView) findViewById(R.id.text_log);
        mTextLogLayout = (ScrollView) findViewById(R.id.layout_log);
        mTextInput = (EditText) findViewById(R.id.text_input);
        mSpeakButton = findViewById(R.id.button_speak);
        mVoiceInButton = findViewById(R.id.button_voice);
        mVoiceInWaitIndicator = findViewById(R.id.wait_voice);
        mLanguageSpinner = (Spinner) findViewById(R.id.lang_select);
        mSettingsButton = (ImageButton) findViewById(R.id.settings_button);
        mSubheader = findViewById(R.id.subheader);
        mSubheaderDivider = findViewById(R.id.subheader_divider);
        mVoiceSelectLayout = findViewById(R.id.layout_voice_select);
        mVoiceSelect = (Spinner) findViewById(R.id.voice_select);
        mPitchSelectLayout = findViewById(R.id.layout_pitch_select);
        mPitchSelect = (SeekBar) findViewById(R.id.pitch_select);
        mPitchValue = (TextView) findViewById(R.id.pitch_value);
        mSpeedSelectLayout = findViewById(R.id.layout_speed_select);
        mSpeedSelect = (SeekBar) findViewById(R.id.speed_select);
        mSpeedValue = (TextView) findViewById(R.id.speed_value);
        mSubheaderDivider2 = findViewById(R.id.subheader_divider2);
        mCheckVoiceInputVariants = (CheckBox) findViewById(R.id.check_voice_input_variants);

        // check for TTS availability
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

        // check for audio availability
        initAudioPermission();

        // assign speak button click
        mSpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence text = mTextInput.getText();
                if (text.toString().isEmpty()) {
                    // no text to speak
                    return;
                }
                Locale loc = MY_LOCALE_LIST[mLanguageSpinner.getSelectedItemPosition()];
                String prefix = "[" + loc.getISO3Country().toUpperCase() + "] ";
                mTextLog.append(prefix + text + "\n");
                mTextLogLayout.fullScroll(View.FOCUS_DOWN);

                if (mTTS != null) {
                    mTTS.setLanguage(loc);
                    //noinspection ConstantConditions
                    TTSCompat.setVoiceIfAvailable(mTTS,
                            mVoiceAdapter.getItem(mVoiceSelect.getSelectedItemPosition()).get());
                    mTTS.setPitch(magicConvert(mPitchSelect.getProgress()));
                    mTTS.setSpeechRate(magicConvert(mSpeedSelect.getProgress()));
                    TTSCompat.speak(mTTS, text, TextToSpeech.QUEUE_ADD);
                }

                mTextInput.setText(null);
            }
        });

        // assign voice input button
        mVoiceInButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mSpeech != null) {
                            // get language tag to use for recognition
                            String l = MY_LANG_TAG_LIST[mLanguageSpinner.getSelectedItemPosition()];
                            // create the RecognizerIntent
                            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, l);
                            // send Intent to start listening
                            mSpeech.startListening(i);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        // button released, stop listening
                        mSpeech.stopListening();
                        // delay the next possible voice input by 2 seconds
                        // to avoid interference
                        mVoiceInButton.setEnabled(false);
                        mVoiceInWaitIndicator.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mVoiceInButton.setEnabled(true);
                                mVoiceInWaitIndicator.setVisibility(View.GONE);
                            }
                        }, 2000);
                        return true;
                }
                return false;
            }
        });

        // assign language select change event
        mLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    updateVoices();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // do nothing
            }
        });

        // assign settings button click
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mVoiceSelectLayout.getVisibility() == View.VISIBLE) {
                    hideSettings();
                } else {
                    // show settings
                    mSubheaderDivider.setVisibility(View.VISIBLE);
                    mVoiceSelectLayout.setVisibility(View.VISIBLE);
                    mPitchSelectLayout.setVisibility(View.VISIBLE);
                    mSpeedSelectLayout.setVisibility(View.VISIBLE);
                    mSubheaderDivider2.setVisibility(View.VISIBLE);
                    mCheckVoiceInputVariants.setVisibility(View.VISIBLE);
                    // change button icons
                    mSettingsButton.setImageResource(R.drawable.ic_expand_less_white_24px);
                }
            }
        });

        // assign pitch seek change
        mPitchSelect.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
                mPitchValue.setText(String.valueOf(progress - MAGIC_PROGRESS));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mPitchValue.setText(String.valueOf(mPitchSelect.getProgress() - MAGIC_PROGRESS));

        // assign pitch reset (no instance save needed)
        findViewById(R.id.reset_pitch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPitchSelect.setProgress(MAGIC_PROGRESS);
            }
        });

        // assign speed seek change
        mSpeedSelect.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
                mSpeedValue.setText(String.valueOf(progress - MAGIC_PROGRESS));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mSpeedValue.setText(String.valueOf(mSpeedSelect.getProgress() - MAGIC_PROGRESS));

        // assign speed reset (no instance save needed)
        findViewById(R.id.reset_speed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSpeedSelect.setProgress(MAGIC_PROGRESS);
            }
        });

        // restore log, input and settings if there were some saved before (e.g. orientation change)
        // @see #onSaveInstanceState(Bundle)
        if (savedInstanceState != null) {
            mTextLog.setText(savedInstanceState.getCharSequence(EXTRA_SAVED_LOG));
            mTextInput.setText(savedInstanceState.getCharSequence(EXTRA_SAVED_INPUT));
            mLanguageSpinner.setSelection(savedInstanceState.getInt(EXTRA_SAVED_LANG));
            // delay voice selection, because that needs to be populated later
            // @see #onInit(int)
            mVoiceSavedSelection = savedInstanceState.getInt(EXTRA_SAVED_VOICE);
            mPitchSelect.setProgress(savedInstanceState.getInt(EXTRA_SAVED_PITCH));
            mSpeedSelect.setProgress(savedInstanceState.getInt(EXTRA_SAVED_SPEED));
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // find out whether the click was inside the subheader or not
        Rect viewRect = new Rect();
        mSubheader.getGlobalVisibleRect(viewRect);
        if (!viewRect.contains((int) ev.getRawX(), (int) ev.getRawY())
                && mVoiceSelectLayout.getVisibility() == View.VISIBLE) {
            // collapse subheader, if the click was outside and it was expanded
            hideSettings();
        }
        // handle the touch event as it is supposed to be handled
        return super.dispatchTouchEvent(ev);
    }

    private void hideSettings() {
        mSubheaderDivider.setVisibility(View.GONE);
        mVoiceSelectLayout.setVisibility(View.GONE);
        mPitchSelectLayout.setVisibility(View.GONE);
        mSpeedSelectLayout.setVisibility(View.GONE);
        mSubheaderDivider2.setVisibility(View.GONE);
        mCheckVoiceInputVariants.setVisibility(View.GONE);
        // change button icons
        mSettingsButton.setImageResource(R.drawable.ic_settings_white_24px);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // TTS available
                mTTS = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    @Override
    public void onInit(int ttsStatus) {
        if (ttsStatus == TextToSpeech.ERROR) {
            // show error message and abort all post-TTS-initialization
            Toast.makeText(this, R.string.tts_error, Toast.LENGTH_LONG).show();
            return;
        }

        // initialization of TTS finished
        // enable speak button
        mSpeakButton.setEnabled(true);

        // populate voice selector
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mVoiceAdapter = new ArrayAdapter<>(mVoiceSelect.getContext(),
                    android.R.layout.simple_spinner_dropdown_item);
            mVoiceSelect.setAdapter(mVoiceAdapter);
            updateVoices();
        } else {
            // no voice selection, disable
            mVoiceSelect.setAdapter(new ArrayAdapter<>(mVoiceSelect.getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{getString(R.string.default_voice)}));
        }

        // if there was a saved-restored voice index, re-set it now
        if (mVoiceSavedSelection > -1) {
            mVoiceSelect.setSelection(mVoiceSavedSelection);
            mVoiceSavedSelection = -1;
        }
    }

    public void initAudioPermission() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            // cancel initialization, as speech recognition is not available on the system
            Log.w(LOG_TAG, "Speech recognition is not available.");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, R.string.audio_error, Toast.LENGTH_LONG).show();
            }

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initSpeechRecognizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    initSpeechRecognizer();
                }
        }
    }

    public void initSpeechRecognizer() {
        // create speech recognition instance
        mSpeech = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeech.setRecognitionListener(new Recognition());
        // enable voice input button
        mVoiceInButton.setEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(EXTRA_SAVED_LOG, mTextLog.getText());
        outState.putCharSequence(EXTRA_SAVED_INPUT, mTextInput.getText());
        outState.putInt(EXTRA_SAVED_LANG, mLanguageSpinner.getSelectedItemPosition());
        outState.putInt(EXTRA_SAVED_VOICE, mVoiceSelect.getSelectedItemPosition());
        outState.putInt(EXTRA_SAVED_PITCH, mPitchSelect.getProgress());
        outState.putInt(EXTRA_SAVED_SPEED, mSpeedSelect.getProgress());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateVoices() {
        if (mTTS != null && mVoiceAdapter != null) {
            Set<Voice> voices = mTTS.getVoices();

            // reset voice selection
            mVoiceAdapter.clear();
            mVoiceSelect.setSelection(0);

            // add "default voice"
            mVoiceAdapter.add(new VoiceWrapper(this, null));

            for (Voice v : voices) {
                if (v.getLocale().equals(
                        MY_LOCALE_LIST[mLanguageSpinner.getSelectedItemPosition()])) {
                    mVoiceAdapter.add(new VoiceWrapper(this, v));
                }
            }
        }
    }

    /** @see #MAGIC_PROGRESS */
    private float magicConvert(int progress) {
        // magic number 999: progress is in [0, 1999]
        // now in [-999, 1000]
        int number = progress - MAGIC_PROGRESS;
        // result should be in (0, infinity) with 1 being the default ("middle")
        float result;
        if (number < 0) {
            // converting to thousandth
            // number is always negative => 1000 - |number|
            result = (1000f + (number / 1.5f)) / 1000f;
        } else {
            result = 1 + ((number) / 100f);
        }

        Log.d(LOG_TAG, "Magic convert from " + progress + " to " + number
                + " to " + result);
        return result;
    }

    @Override
    public void onBackPressed() {
        if (mVoiceSelectLayout.getVisibility() == View.VISIBLE) {
            hideSettings();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.shutdown();
        }
        if (mSpeech != null) {
            mSpeech.destroy();
        }
        super.onDestroy();
    }

    private class Recognition implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Ready for speech. Params: " + params);
            }
            mTextInput.setEnabled(false);
            mTextInput.setText(R.string.text_voice_input);
        }

        @Override
        public void onBeginningOfSpeech() {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Beginning of speech.");
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "RMS changed to " + rmsdB);
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Buffer received.");
            }
        }

        @Override
        public void onEndOfSpeech() {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "End of speech.");
            }
            mTextInput.setEnabled(true);
            mTextInput.setText(null);
        }

        @Override
        public void onError(int error) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Speech error " + error + ".");
            }
            mTextInput.setEnabled(true);
            mTextInput.setEnabled(true);
            mTextInput.setText(null);
            mSpeech.cancel();
        }

        @Override
        public void onResults(Bundle results) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Speech results received: " + results);
            }
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (data != null) {
                String str = "";

                if (mCheckVoiceInputVariants.isChecked()) {
                    // all possible variants should be printed
                    for (int i = 0; i < data.size(); i++) {
                        if (i > 0) {
                            str += " | ";
                        }
                        str += data.get(i);
                    }
                } else if (data.size() > 0) {
                    // print only the first recognized variant
                    str += data.get(0);
                }

                // get accent color
                int[] attrs = new int[] { R.attr.colorAccent /* index 0 */};
                TypedArray ta = obtainStyledAttributes(attrs);
                int accentColor = ta.getColor(0 /* index */, Color.BLACK);
                ta.recycle();

                str = "[YOU] " + str + "\n";
                SpannableString coloredStr = new SpannableString(str);
                coloredStr.setSpan(new ForegroundColorSpan(accentColor), 0, str.length(), 0);
                mTextLog.append(coloredStr);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Partial speech results received: " + partialResults);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Speech event " + eventType + " received: " + params);
            }
        }
    }

    /**
     * This is a wrapper class for {@link Voice} objects, to display their name in an
     * {@link ArrayAdapter} instead of a non-human-readable default {@link #toString()}.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class VoiceWrapper {
        private final Context mContext;
        private final Voice mInstance;

        public VoiceWrapper(Context context, Voice voice) {
            mContext = context;
            mInstance = voice;
        }

        public Voice get() {
            return mInstance;
        }

        @Override
        public String toString() {
            if (mInstance == null) {
                return mContext.getString(R.string.default_voice);
            } else {
                return mInstance.getName();
            }
        }
    }
}
