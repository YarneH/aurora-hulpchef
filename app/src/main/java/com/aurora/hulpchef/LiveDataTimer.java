package com.aurora.hulpchef;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.CountDownTimer;

import com.aurora.souschefprocessor.recipe.RecipeTimer;

import java.util.Locale;

/**
 * Class that keeps track of a timer, while being independent of the UI.
 * Uses LiveData to update the UI.
 */
public class LiveDataTimer {
    /**
     * The amount of milliseconds in a second. Needed to convert
     * RecipeTimers (which are in seconds) to actual timers.
     */
    private static final int MILLIS = 1000;
    /**
     * Amount of time units in a bigger time unit :)
     * (why is there no name for that? I propose time-babies.)
     */
    private static final int TIME_BABIES = 60;
    /**
     * One of the states of the timer
     */
    protected static final int TIMER_INITIALISED = 0;
    /**
     * One of the states of the timer
     */
    protected static final int TIMER_RUNNING = 1;
    /**
     * One of the states of the timer
     */
    protected static final int TIMER_PAUSED = 2;
    /**
     * The actual timer that can count down.
     */
    private CountDownTimer mCountDownTimer;
    /**
     * The timer representing the actual values from the recipe.
     */
    private RecipeTimer mRecipeTimer;
    /**
     * State of the timer. Is not running when false.
     */
    private boolean mRunning = false;
    /**
     * A boolean representing whether the timer is ringing or not
     */
    private boolean mRinging = false;
    /**
     * The original time received from the user or from the recipe.
     */
    private int mTimeSetByUser;

    private MutableLiveData<Boolean> mFinished = new MutableLiveData<>();

    private MutableLiveData<Boolean> mAlarming = new MutableLiveData<>();

    /**
     * Holds the observable time left until finished.
     * Call observe on this field to keep track of time in the UI.
     */
    private MutableLiveData<Long> mMillisLeft = new MutableLiveData<>();

    /**
     * Indicates the current state of the timer
     * (TIMER_RUNNING, TIMER_PAUSED, TIMER_INITIALISED, TIMER_ALARMING)
     */
    private MutableLiveData<Integer> mTimerState = new MutableLiveData<>();

    /**
     * Create a new timer based on a timer described in a recipe.
     *
     * @param recipeTimer the timer it is based on.
     */
    public LiveDataTimer(RecipeTimer recipeTimer) {
        mRecipeTimer = new RecipeTimer(recipeTimer.getUpperBound(), recipeTimer.getLowerBound(), null);
        mTimeSetByUser = recipeTimer.getLowerBound();
        mFinished.setValue(false);
        mAlarming.setValue(false);
        mMillisLeft.setValue((long) (mTimeSetByUser * MILLIS));
        mRunning = false;
        mRinging = false;
        mTimerState.setValue(TIMER_INITIALISED);
    }

    /**
     * Create a new timer based on upper and lower bound.
     *
     * @param lowerBound lower bound of time. Is swapped in case it is larger than upper
     *                   bound. This swap happens in the RecipeTimer class
     * @param upperBound upper bound of time.
     */
    public LiveDataTimer(int lowerBound, int upperBound) {
        mRecipeTimer = new RecipeTimer(upperBound, lowerBound, null);
        mTimeSetByUser = mRecipeTimer.getLowerBound();
        mFinished.setValue(false);
        mMillisLeft.setValue((long) (mTimeSetByUser * MILLIS));
        mRunning = false;
    }

    /**
     * Resets the timer to it's original time
     */
    public void resetTimer() {
        mFinished.setValue(false);
        mMillisLeft.setValue((long) (mTimeSetByUser * MILLIS));
        mTimerState.setValue(TIMER_INITIALISED);
        mAlarming.setValue(false);
        mRunning = false;
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    /**
     * Toggles playing paused state of the timer.
     */
    public void toggleTimer() {

        if (mAlarming.getValue()) {
            mAlarming.setValue(false);
            return;
        }

        if (mFinished.getValue()) {
            return;
        }

        if (mRunning) {
            mRunning = false;
            mTimerState.setValue(TIMER_PAUSED);
            mCountDownTimer.cancel();
            return;
        }
        this.mCountDownTimer = new CountDownTimer(mMillisLeft.getValue() - 1, MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
                mMillisLeft.setValue(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                mFinished.setValue(true);
                mAlarming.setValue(true);
                //TODO: check if running needs to be set to false
                mRunning = false;
            }
        };
        this.mCountDownTimer.start();
        mTimerState.setValue(TIMER_RUNNING);
        mRunning = true;
    }

    public LiveData<Long> getMillisLeft() {
        return mMillisLeft;
    }

    /**
     * The user can change the time by long-pressing.
     * This can only be done when upperbound != lowerbound.
     * See {@link #canChangeTimer()}.
     * This seems weird, but okay.
     *
     * @param timeInSeconds set time.
     */
    public void setTimeSetByUser(int timeInSeconds) {
        mTimeSetByUser = timeInSeconds;
        resetTimer();
    }

    /**
     * Convert an amount of seconds into a string representation
     *
     * @param amountMilliSeconds long representing the amount of milliseconds
     * @return a String representation of the time
     */
    public static String convertTimeToString(long amountMilliSeconds) {
        int amountSeconds = (int) (amountMilliSeconds / MILLIS);
        // seconds / 3600, or divide twice by 60.
        int amountHours = amountSeconds / TIME_BABIES / TIME_BABIES;
        // subtract the amount of hours first, then divide seconds by 60 to get minutes.
        int amountMins = (amountSeconds % (TIME_BABIES * TIME_BABIES)) / TIME_BABIES;
        // remaining time in seconds.
        int amountSec = amountSeconds % TIME_BABIES;

        String timerText = "";

        // Only add hours when there are hours.
        if (amountHours != 0) {
            timerText += amountHours + ":";
        }
        timerText += String.format(Locale.getDefault(), "%02d:%02d", amountMins, amountSec);

        return timerText;
    }

    /**
     * Returns whether or not the timer can be changed.
     *
     * @return true if timer is changeable.
     */
    public boolean canChangeTimer() {
        return mRecipeTimer.getLowerBound() != mRecipeTimer.getUpperBound();
    }

    /**
     * Get the upper bound on the timer.
     * @return upper bound in seconds
     */
    public int getUpperBound() {
        return mRecipeTimer.getUpperBound();
    }

    /**
     * Get the lower bound on the timer.
     * @return lower bound in seconds
     */
    public int getLowerBound() {
        return mRecipeTimer.getLowerBound();
    }

    /**
     * LiveData with a boolean whether or not the timer is finished.
     * @return LiveData with boolean
     */
    public LiveData<Boolean> getIsFinished() {
        return mFinished;
    }

    /**
     * LiveData with alarming state.
     * @return Livedata with boolean whether or not alarming
     */
    public LiveData<Boolean> isAlarming() {
        return mAlarming;
    }

    /**
     * Get the timer state as Live data.
     * @return LiveData with the state-ID of the timer
     */
    public LiveData<Integer> getTimerState() {
        return mTimerState;
    }

    /**
     * Returns true if the Ringtone is ringing for this timer
     *
     * @return boolean representing whether the Ringtone is ringing or not
     */
    public boolean isRinging(){
        return mRinging;
    }

    /**
     * Set the ringing state of the timer
     * @param ringing true if the Ringtone is ringing, false if not
     */
    public void setRinging(boolean ringing){
        mRinging = ringing;
    }
}
