/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.android.internal.R;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 */
public class ClockHeader extends TextView {
    private boolean mAttached;
    private Calendar mCalendar;
    private String mClockFormatString;
    private SimpleDateFormat mClockFormat;

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    private static final int AM_PM_STYLE = AM_PM_STYLE_GONE;

    // Junk
    private final String Junk_Pulldown_Settings = "JUNK_PULLDOWN_SETTINGS";
	private final String HEADER_CLOCK_SHOW = "header_clock_show";
	private final String HEADER_CLOCK_COLOR = "header_clock_color";
	private final String HEADER_CLOCK_SIZE = "header_clock_size";
	private SharedPreferences sp;
    private boolean mHeaderClockShow = true;
    private int mHeaderClockColor = 0xffffffff;
    private int mHeaderClockSize = 22;    
    // End Junk
    
    public ClockHeader(Context context) {
        this(context, null);
    }

    public ClockHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            // Junk
            filter.addAction(Junk_Pulldown_Settings);
            //End Junk
            
            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
        }

        // Junk
 		Context settingsContext = getContext();
		try {
			settingsContext = getContext().createPackageContext("com.android.settings",0);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		
		sp = settingsContext.getSharedPreferences("Junk_Settings", Context.MODE_PRIVATE);
 		
 		mHeaderClockShow = sp.getBoolean(HEADER_CLOCK_SHOW, mHeaderClockShow);
		mHeaderClockColor = sp.getInt(HEADER_CLOCK_COLOR, mHeaderClockColor);
		mHeaderClockSize = sp.getInt(HEADER_CLOCK_SIZE, mHeaderClockSize);
		// End Junk
		
        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = Calendar.getInstance(TimeZone.getDefault());

        // Junk
        setVisibility(mHeaderClockShow ? VISIBLE : GONE);
        if (mHeaderClockShow) updateClock();
        // End Junk
        
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Junk
            if (action.equals(Junk_Pulldown_Settings)) {
            	mHeaderClockShow = intent.getBooleanExtra(HEADER_CLOCK_SHOW, mHeaderClockShow);
  	   			mHeaderClockColor = intent.getIntExtra(HEADER_CLOCK_COLOR, mHeaderClockColor);	
            	mHeaderClockSize = intent.getIntExtra(HEADER_CLOCK_SIZE, mHeaderClockSize);
            }
            // End Junk
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
                if (mClockFormat != null) {
                    mClockFormat.setTimeZone(mCalendar.getTimeZone());
                }
            }
            // Junk
            setVisibility(mHeaderClockShow ? VISIBLE : GONE);
            if (mHeaderClockShow) updateClock();
            // End Junk
        }
    };

    final void updateClock() {
    	// Junk
        setTextColor(mHeaderClockColor);
        setTextSize(mHeaderClockSize);
    	// End Junk
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        setText(getSmallTime());
    }

    private final CharSequence getSmallTime() {
        Context context = getContext();
        boolean b24 = DateFormat.is24HourFormat(context);
        int res;

        if (b24) {
            res = R.string.twenty_four_hour_time_format;
        } else {
            res = R.string.twelve_hour_time_format;
        }

        final char MAGIC1 = '\uEF00';
        final char MAGIC2 = '\uEF01';

        SimpleDateFormat sdf;
        String format = context.getString(res);
        if (!format.equals(mClockFormatString)) {
            /*
             * Search for an unquoted "a" in the format string, so we can
             * add dummy characters around it to let us find it again after
             * formatting and change its size.
             */
            if (AM_PM_STYLE != AM_PM_STYLE_NORMAL) {
                int a = -1;
                boolean quoted = false;
                for (int i = 0; i < format.length(); i++) {
                    char c = format.charAt(i);

                    if (c == '\'') {
                        quoted = !quoted;
                    }
                    if (!quoted && c == 'a') {
                        a = i;
                        break;
                    }
                }

                if (a >= 0) {
                    // Move a back so any whitespace before AM/PM is also in the alternate size.
                    final int b = a;
                    while (a > 0 && Character.isWhitespace(format.charAt(a-1))) {
                        a--;
                    }
                    format = format.substring(0, a) + MAGIC1 + format.substring(a, b)
                        + "a" + MAGIC2 + format.substring(b + 1);
                }
            }

            mClockFormat = sdf = new SimpleDateFormat(format);
            mClockFormatString = format;
        } else {
            sdf = mClockFormat;
        }
        String result = sdf.format(mCalendar.getTime());

        if (AM_PM_STYLE != AM_PM_STYLE_NORMAL) {
            int magic1 = result.indexOf(MAGIC1);
            int magic2 = result.indexOf(MAGIC2);
            if (magic1 >= 0 && magic2 > magic1) {
                SpannableStringBuilder formatted = new SpannableStringBuilder(result);
                if (AM_PM_STYLE == AM_PM_STYLE_GONE) {
                    formatted.delete(magic1, magic2+1);
                } else {
                    if (AM_PM_STYLE == AM_PM_STYLE_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, magic1, magic2,
                                          Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    formatted.delete(magic2, magic2 + 1);
                    formatted.delete(magic1, magic1 + 1);
                }
                return formatted;
            }
        }
 
        return result;

    }
}

