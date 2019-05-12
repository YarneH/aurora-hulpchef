package com.aurora.hulpchef.utilities;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A singleton class for the Ringtone playing when one or multiple timers finishes
 */
public class TimerRingtone {
    /**
     * The singleton instance
     */
    private static final TimerRingtone mInstance = new TimerRingtone();

    /**
     * The Ringtone used as TimerRingtone
     */
    private Ringtone mRingtone = null;

    /**
     * The amount of timers ringing
     */
    private AtomicInteger mAmountRinging = new AtomicInteger(0);

    /**
     * A boolean representing whether the TimerRingtone has been initiated
     */
    private boolean mInitialized = false;

    /**
     * The private constructor for the TimerRingtone-singleton
     */
    private TimerRingtone() {
        // Private constructor for singleton
    }

    /**
     * Get the TimerRingtone instance
     *
     * @return the instance
     */
    public static TimerRingtone getInstance() {
        return mInstance;
    }

    /**
     * Initiate the TimerRingtone with a Context
     * @param context The context to which the Ringtone is connected
     */
    public void initialize(Context context) {
        if (!mInitialized) {
            // Preparing the ringtone for the alarm
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alert == null) {
                // alert is null, using backup
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                // in case this is also null again
                if (alert == null) {
                    // alert backup is null, using 2nd backup
                    alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
            }
            mRingtone = RingtoneManager.getRingtone(context, alert);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mRingtone.setLooping(true);
            }

            mInitialized = true;
        }
    }

    /**
     * Add a ringing timer to the Ringtone
     */
    public void addRingingTimer() {
        mAmountRinging.incrementAndGet();
        updateRingtone();
    }

    /**
     * Remove a ringing timer from the Ringtone
     */
    public void removeRingingTimer() {
        mAmountRinging.decrementAndGet();
        updateRingtone();
    }

    /**
     * Update the Ringtone according to the amount of ringing timers
     */
    private void updateRingtone() {
        if (mAmountRinging.get() > 0 && !mRingtone.isPlaying()) {
            mRingtone.play();
        } else if (mAmountRinging.get() == 0 && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
    }
}
