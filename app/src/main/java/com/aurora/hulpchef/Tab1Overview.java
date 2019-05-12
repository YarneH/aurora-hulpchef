package com.aurora.hulpchef;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.aurora.souschefprocessor.recipe.Recipe;

/**
 * Class defining the functionality of the overview tab.
 */
public class Tab1Overview extends Fragment {

    /**
     * Define the preferences category for the settings
     */
    public static final String SETTINGS_PREFERENCES = "Instellingen";
    /**
     * The key value for storing whether the recipe is displayed in imperial or metric units.
     */
    public static final String VERTAAL = "Vertaal naar Nederlands";

    /**
     * Contains the description of a recipe.
     */
    private TextView mDescriptionTextView = null;
    /**
     * The card containing the settings. Is GONE by default.
     */
    private CardView mSettingsCard = null;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab_1_overview, container, false);
        mDescriptionTextView = rootView.findViewById(R.id.tv_recipe_description);

        // Add the floating action button to change the settings.
        FloatingActionButton mSettingsFab = rootView.findViewById(R.id.fab_settings);
        mSettingsFab.setOnClickListener(v -> onSettingsClicked());

        // Get the card and make it disappear on clicking outside of it.
        mSettingsCard = rootView.findViewById(R.id.cv_settings);
        rootView.setOnClickListener(v -> mSettingsCard.setVisibility(View.GONE));

        // Switch for changing the units.
        Switch mToggleImperial = rootView.findViewById(R.id.switch_toggle_imperial);
        // Change the settings in sharedPreferences.
        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        boolean imperial = sharedPreferences.getBoolean(VERTAAL, false);
        mToggleImperial.setChecked(imperial);
        mToggleImperial.setOnCheckedChangeListener((v, isChecked) -> onSwitchToggled(isChecked));

        // Listen for recipe changes.
        RecipeViewModel mRecipe = ViewModelProviders.of(requireActivity()).get(RecipeViewModel.class);
        mRecipe.getRecipe().observe(this, (Recipe recipe) -> {
            if (recipe == null) {
                return;
            }
            mDescriptionTextView.setText(recipe.getDescription());
            mDescriptionTextView.setMovementMethod(new ScrollingMovementMethod());
        });
        return rootView;
    }

    /**
     * Handle what happens on clicking the settings-FAB.
     *
     */
    private void onSettingsClicked() {
        if (mSettingsCard.getVisibility() == View.VISIBLE) {
            mSettingsCard.setVisibility(View.GONE);
        } else {
            mSettingsCard.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Change the sharedPreferences on toggling the switch to imperial and back.
     *
     * @param isChecked the toggled state
     */
    private void onSwitchToggled(boolean isChecked) {
        SharedPreferences sharedPreferences = requireActivity().
                getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(VERTAAL, isChecked);
        editor.apply();
    }
}
