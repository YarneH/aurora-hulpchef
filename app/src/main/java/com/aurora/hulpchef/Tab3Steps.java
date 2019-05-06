package com.aurora.hulpchef;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aurora.souschefprocessor.recipe.Recipe;

import java.util.Objects;

/**
 * Class defining the functionality of the recipe steps tab.
 */
public class Tab3Steps extends Fragment {
    /**
     * Array with the text in every step.
     */
    private String[] mDescriptionSteps = null;
    /**
     * Adapter for filling the different step cards.
     */
    private StepsPagerAdapter mStepsPagerAdapter;

    /**
     * Viewpager for swiping and navigating through the different cards.
     */
    private ViewPager mViewPager;

    /**
     * Default constructor. Is empty.
     */
    public Tab3Steps() {
        // Default constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.tab_3_steps, container, false);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mStepsPagerAdapter = new StepsPagerAdapter(getChildFragmentManager());
        mViewPager = rootView.findViewById(R.id.vp_steps);

        RecipeViewModel mRecipe = ViewModelProviders
                .of(Objects.requireNonNull(this.getActivity()))
                .get(RecipeViewModel.class);
        mRecipe.getRecipe().observe(this, (Recipe recipe) -> {
            if (recipe == null) {
                return;
            }
            mStepsPagerAdapter.notifyDataSetChanged();
            mDescriptionSteps = extractDescriptionSteps(recipe);
            mViewPager.setAdapter(mStepsPagerAdapter);
        });

        return rootView;
    }

    /**
     * Extracts the descriptions of the different steps.
     *
     * @param recipe where to extract the steps from.
     * @return Array with extracted descriptions. Holds as much elements as there are steps.
     */
    public static String[] extractDescriptionSteps(Recipe recipe) {
        int stepsCount = recipe.getRecipeSteps().size();
        String[] steps = new String[stepsCount];

        for (int i = 0; i < stepsCount; i++) {
            steps[i] = recipe.getRecipeSteps().get(i).getDescription();
        }
        return steps;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class StepsPagerAdapter extends FragmentStatePagerAdapter {

        public StepsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return StepPlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Return total pages.
            if (mDescriptionSteps == null) {
                return 0;
            }
            return mDescriptionSteps.length;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }
    }

}
