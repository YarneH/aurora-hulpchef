package com.aurora.hulpchef;

import android.arch.lifecycle.ViewModel;

import com.aurora.souschefprocessor.recipe.Recipe;
import com.aurora.souschefprocessor.recipe.RecipeStep;
import com.aurora.souschefprocessor.recipe.RecipeTimer;

import java.util.ArrayList;

/**
 * ViewModel specifically for the timers. Keeps track of all the timers in the recipe,
 * and holds the running information of each of them.
 */
public class RecipeTimerViewModel extends ViewModel {

    /**
     * List with all LiveDataTimers.
     */
    private ArrayList<ArrayList<LiveDataTimer>> mAllTimers = null;

    /**
     * Initialize the timers in a recipe.
     * Put all timers in a LiveDataTimer, and store them in an array.
     * <p>
     * Always call init before doing anything else with this class!
     *
     * @param recipe The recipe to extract the timers from.
     */
    public void init(Recipe recipe) {
        if (mAllTimers != null) {
            // init was already called.
            return;
        }
        // Put timers in nested ArrayList.
        mAllTimers = new ArrayList<>();
        for (RecipeStep step : recipe.getRecipeSteps()) {
            ArrayList<LiveDataTimer> stepTimers = new ArrayList<>();
            for (RecipeTimer timer : step.getRecipeTimers()) {
                stepTimers.add(new LiveDataTimer(timer));
            }
            mAllTimers.add(stepTimers);
        }
    }

    /**
     * Get the timer at a specific index at a specific step.
     *
     * @param stepIndex  the step to get the timer from
     * @param timerIndex the index of the requested timer in the step
     * @return The requested timer.
     */
    public LiveDataTimer getTimerInStep(int stepIndex, int timerIndex) {
        return mAllTimers.get(stepIndex).get(timerIndex);
    }
}
