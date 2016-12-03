package com.woalk.apps.ttsexperiment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int MY_DATA_CHECK_CODE = 1;
    private static final Locale[] MY_LOCALE_LIST = new Locale[]{Locale.US, Locale.UK,
            Locale.GERMANY, Locale.ITALY, Locale.FRANCE, new Locale("es", "ES")};
    private static final int MAGIC_PROGRESS = 999;

    //region instance variables
    TextToSpeech mTTS;
    ArrayAdapter<VoiceWrapper> mVoiceAdapter;
    //endregion

    //region view instances
    View mSpeakButton;
    TextView mTextLog;
    ScrollView mTextLogLayout;
    EditText mTextInput;
    Spinner mLanguageSpinner;
    View mSubheaderDivider;
    View mVoiceSelectLayout;
    Spinner mVoiceSelect;
    View mPitchSelectLayout;
    SeekBar mPitchSelect;
    TextView mPitchValue;
    View mSpeedSelectLayout;
    SeekBar mSpeedSelect;
    TextView mSpeedValue;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check for TTS availability
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

        // get view instances
        mTextLog = (TextView) findViewById(R.id.text_log);
        mTextLogLayout = (ScrollView) findViewById(R.id.layout_log);
        mTextInput = (EditText) findViewById(R.id.text_input);
        mSpeakButton = findViewById(R.id.button_speak);
        mLanguageSpinner = (Spinner) findViewById(R.id.lang_select);
        mSubheaderDivider = findViewById(R.id.subheader_divider);
        mVoiceSelectLayout = findViewById(R.id.layout_voice_select);
        mVoiceSelect = (Spinner) findViewById(R.id.voice_select);
        mPitchSelectLayout = findViewById(R.id.layout_pitch_select);
        mPitchSelect = (SeekBar) findViewById(R.id.pitch_select);
        mPitchValue = (TextView) findViewById(R.id.pitch_value);
        mSpeedSelectLayout = findViewById(R.id.layout_speed_select);
        mSpeedSelect = (SeekBar) findViewById(R.id.speed_select);
        mSpeedValue = (TextView) findViewById(R.id.speed_value);

        // assign button click
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
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mVoiceSelectLayout.getVisibility() == View.VISIBLE) {
                    mSubheaderDivider.setVisibility(View.GONE);
                    mVoiceSelectLayout.setVisibility(View.GONE);
                    mPitchSelectLayout.setVisibility(View.GONE);
                    mSpeedSelectLayout.setVisibility(View.GONE);
                    // change button icons
                    ((ImageButton) view).setImageResource(R.drawable.ic_settings_white_24px);
                } else {
                    mSubheaderDivider.setVisibility(View.VISIBLE);
                    mVoiceSelectLayout.setVisibility(View.VISIBLE);
                    mPitchSelectLayout.setVisibility(View.VISIBLE);
                    mSpeedSelectLayout.setVisibility(View.VISIBLE);
                    // change button icons
                    ((ImageButton) view).setImageResource(R.drawable.ic_expand_less_white_24px);
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
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateVoices() {
        if (mVoiceAdapter != null) {
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

        Log.d("TTSExperiment", "Magic convert from " + progress + " to " + number
                + " to " + result);
        return result;
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.shutdown();
        }
        super.onDestroy();
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
