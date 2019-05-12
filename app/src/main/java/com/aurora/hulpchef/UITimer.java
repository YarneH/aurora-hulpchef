package com.aurora.hulpchef;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LifecycleOwner;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aurora.hulpchef.utilities.TimerRingtone;

/**
 * A UI class responsible for filling in the UI with timer data.
 */
public class UITimer {
    /**
     * Time constant: seconds in an hour.
     */
    private static final int AMOUNT_SEC_IN_HOUR = 3600;
    /**
     * Time constant: seconds in half an hour.
     */
    private static final int AMOUNT_SEC_IN_HALF_HOUR = 1800;
    /**
     * Time constant: seconds in a quarter hour.
     */
    private static final int AMOUNT_SEC_IN_QUARTER = 900;
    /**
     * Time constant: seconds in a minute.
     */
    private static final int AMOUNT_SEC_IN_MIN = 60;
    /**
     * The time it takes before the color of a timer changes when it is alarming.
     */
    private static final int CHANGE_COLOR_MILLISEC_DELAY = 250;
    /**
     * The amount of milliseconds in a second. Needed to convert
     * RecipeTimers (which are in seconds) to actual timers.
     */
    private static final int MILLIS = 1000;
    /**
     * Time constant: seconds in a minute.
     */
    private static final int MINUTE_STEP = 60;
    /**
     * Time constant: seconds in half a minute.
     */
    private static final int HALF_MINUTE_STEP = 30;
    /**
     * Time constant: seconds in 15 seconds (?!?).
     */
    private static final int QUARTER_MINUTE_STEP = 15;
    /**
     * Time constant: the amount of seconds in exactly one second.
     */
    private static final int SECOND_STEP = 1;
    /**
     * Maximum percentage. Preventing magic numbers.
     */
    private static final int PERCENT = 100;
    /**
     * Data container for timers.
     */
    private final LiveDataTimer mLiveDataTimer;
    /**
     * View where the timer is displayed.
     */
    private View mTimerCard;
    /**
     * A handler for the flickering of the card, when the timer is alarming
     */
    private Handler mHandler = null;
    /**
     * A boolean representing whether the color of the card is dark
     */
    private boolean mColorDark = true;

    /**
     * Sets up text and timer views.
     *
     * @param liveDataTimer timer data container
     * @param timerCard     view where to put the timer. Should be a timer_card.xml
     * @param owner         LifeCycleOwner responsible for the LiveData objects.
     *                      Normally the activity.
     */
    public UITimer(LiveDataTimer liveDataTimer, View timerCard, LifecycleOwner owner) {
        this.mLiveDataTimer = liveDataTimer;
        this.mTimerCard = timerCard;

        // set timer observer to update text field.
        TextView timerText = timerCard.findViewById(R.id.tv_timer);
        liveDataTimer.getMillisLeft().observe(owner, (Long millisLeft) -> {
            if (millisLeft != null) {
                timerText.setText(LiveDataTimer.convertTimeToString(millisLeft));
            }
        });

        setOnClickListeners();

        this.mLiveDataTimer.isAlarming().observe(owner, this::setAlarm);

        this.mLiveDataTimer.getTimerState().observe(owner, this::setIconsAndBackground);

    }

    /**
     * Create a new onClickListener and onLongClickListener for the Timer TextView.
     * <p>
     * Uses toggleTimer.
     */
    private void setOnClickListeners() {

        mTimerCard.setOnClickListener((View v) -> mLiveDataTimer.toggleTimer());
        mTimerCard.setOnLongClickListener((View v) -> {
            mLiveDataTimer.resetTimer();
            return true;
        });
        if (mLiveDataTimer.canChangeTimer()) {
            mTimerCard.findViewById(R.id.iv_edit_icon).setOnClickListener((View v) -> setTimerPopup());
        }
    }

    /**
     * Set the icons and background according to the state of the timer
     * @param timerState The current state of the timer
     */
    private void setIconsAndBackground(int timerState) {
        ImageView imageView = mTimerCard.findViewById(R.id.iv_timer_icon);
        View contentView = mTimerCard.findViewById(R.id.cl_timer_content);

        // Check whether the edit icon has to be displayed
        if (mLiveDataTimer.canChangeTimer()) {
            if (timerState == LiveDataTimer.TIMER_INITIALISED) {
                mTimerCard.findViewById(R.id.iv_edit_icon).setVisibility(View.VISIBLE);
            } else {
                mTimerCard.findViewById(R.id.iv_edit_icon).setVisibility(View.GONE);
            }
        }

        // Change color and icon according to the timer state
        if (timerState == LiveDataTimer.TIMER_RUNNING) {
            imageView.setImageResource(R.drawable.ic_pause_white);
            contentView.setBackgroundColor(mTimerCard.getResources().getColor(R.color.colorPrimary));
        } else if (timerState == LiveDataTimer.TIMER_PAUSED) {
            imageView.setImageResource(R.drawable.ic_play_white);
            contentView.setBackgroundColor(mTimerCard.getResources().getColor(R.color.colorPrimaryDark));
        } else if (timerState == LiveDataTimer.TIMER_INITIALISED) {
            imageView.setImageResource(R.drawable.ic_timer_white);
            contentView.setBackgroundColor(mTimerCard.getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    /**
     * Sets alarm of the timer
     *
     * @param alarming a boolean representing the timer going off
     */
    private void setAlarm(boolean alarming) {
        setFlickering(alarming);

        if (alarming && !mLiveDataTimer.isRinging()) {
            TimerRingtone.getInstance().addRingingTimer();
            mLiveDataTimer.setRinging(true);
        } else if (!alarming && mLiveDataTimer.isRinging()) {
            TimerRingtone.getInstance().removeRingingTimer();
            mLiveDataTimer.setRinging(false);
        }
    }

    /**
     * Set the flickering of a timer on or off
     * @param flicker a boolean, true if flickering must be turned on, false otherwise
     */
    private void setFlickering(boolean flicker) {
        View contentView = mTimerCard.findViewById(R.id.cl_timer_content);

        if (flicker) {
            if (mHandler == null) {
                mHandler = new Handler();
            }

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mColorDark) {
                        contentView.setBackgroundColor(
                                mTimerCard.getResources().getColor(R.color.colorPrimary));
                        mColorDark = false;
                    } else {
                        contentView.setBackgroundColor(
                                mTimerCard.getResources().getColor(R.color.colorPrimaryDark));
                        mColorDark = true;
                    }
                    mHandler.postDelayed(this, CHANGE_COLOR_MILLISEC_DELAY);
                }
            }, CHANGE_COLOR_MILLISEC_DELAY);

        } else {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
        }
    }

    /**
     * Creates a popup and show it to the user.
     * The user then can select the value between the upper and lower bound he/she prefers
     */
    private void setTimerPopup() {
        // Calculate the difference and the associated step for the Seekbar
        int difference = mLiveDataTimer.getUpperBound() - mLiveDataTimer.getLowerBound();
        int step;
        if (difference >= AMOUNT_SEC_IN_HOUR) {
            step = MINUTE_STEP;
        } else if (difference >= AMOUNT_SEC_IN_HALF_HOUR) {
            step = HALF_MINUTE_STEP;
        } else if (difference > AMOUNT_SEC_IN_QUARTER) {
            step = QUARTER_MINUTE_STEP;
        } else {
            step = SECOND_STEP;
        }

        // Initiate the LayoutInflater and inflate the Popup layout
        LayoutInflater li = LayoutInflater.from(mTimerCard.getContext());
        // using null as root is allowed here since it is a promptView
        @SuppressLint("InflateParams")
        View promptView = li.inflate(R.layout.prompt_timer_card, null);
        SeekBar seekBar = promptView.findViewById(R.id.sk_timer);
        if (mLiveDataTimer.getMillisLeft().getValue() != null) {
            int currentProgress = convertSecondsToProgress((int) (mLiveDataTimer.getMillisLeft().getValue() / MILLIS));
            seekBar.setProgress(currentProgress, true);
        }

        // Get the TextView of the popup and set to the initial value
        final TextView seekBarValue = promptView.findViewById(R.id.tv_timer);
        seekBarValue.setText(LiveDataTimer.convertTimeToString(mLiveDataTimer.getMillisLeft().getValue()));

        // Set the listener of the SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = convertProgressToSeconds(progress, step);
                seekBarValue.setText(LiveDataTimer.convertTimeToString(newValue * MILLIS));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Auto-generated method stub (not needed)
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Auto-generated method stub (not needed)
            }
        });

        // Build the actual popup and show it to the user
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mTimerCard.getContext());
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Ok", (DialogInterface dialogInterface, int id) -> {
                    int timeSetByUser = convertProgressToSeconds(seekBar.getProgress(), step);
                    mLiveDataTimer.setTimeSetByUser(timeSetByUser);
                });
        alertDialogBuilder.create().show();
    }

    /**
     * Calculate the amount of seconds set by user using the progress of the SeekBar
     *
     * @param progress The progress of the SeekBar, representing a value between 0 and 100
     * @return The amount of seconds currently chosen with the SeekBar
     */
    private int convertProgressToSeconds(int progress, int step) {
        int difference = mLiveDataTimer.getUpperBound() - mLiveDataTimer.getLowerBound();
        double progressValue = ((double) (difference)) * progress / PERCENT;
        int incrementValue = (int) Math.floor(progressValue / (double) step) * step;

        return mLiveDataTimer.getLowerBound() + incrementValue;
    }

    /**
     * Calculate the progress of the SeekBar using an amount of seconds
     *
     * @param seconds The amount of seconds currently chosen by the user
     * @return The amount of progress of the SeekBar
     */
    private int convertSecondsToProgress(int seconds) {
        int difference = mLiveDataTimer.getUpperBound() - mLiveDataTimer.getLowerBound();
        int relativeDifference = seconds - mLiveDataTimer.getLowerBound();
        return (int) ((relativeDifference / (double) difference) * PERCENT);
    }
}
