package com.aurora.hulpchef;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aurora.hulpchef.utilities.StringUtilities;
import com.aurora.souschefprocessor.recipe.Ingredient;
import com.aurora.souschefprocessor.recipe.Recipe;
import com.aurora.souschefprocessor.recipe.RecipeStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A PlaceholderFragment which is used for each step of the recipe
 */
public class StepPlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    /**
     * A code which is used to replace the quantities in the base description of the step
     */
    private static final String INGREDIENT_CODE = ";1;&!;1";
    /**
     * A (Card)View which is the main view of the step
     */
    private View mRootView;
    /**
     * The MaxHeightRecyclerView used for the ingredients in the step
     */
    private MaxHeightRecyclerView mIngredientList;
    /**
     * A list of original descriptions
     */
    private String[] mDescriptionStep;
    /**
     * A list of the description where the quantities of the ingredients are replaced with the INGREDIENT_CODE
     */
    private String[] mDescriptionBase;
    /**
     * A list of integers representing the start indices of the blocks of a step
     */
    private int[] mStartIndexDescriptionBlocks;
    /**
     * The current RecipeStep, which is represented by the StepPlaceholderFragment
     */
    private RecipeStep mRecipeStep = null;
    /**
     * The original amount of people the recipe is for
     */
    private int mOriginalAmount = 0;
    /**
     * A new, by the user set, amount of people
     */
    private int mCurrentAmount = 0;
    /**
     * A list of the TextView which are used to display the different blocks of the description
     */
    private ArrayList<TextView> mStepTextViews = new ArrayList<>();


    public StepPlaceholderFragment() {
        // Empty constructor (generated by Android Studio)
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static StepPlaceholderFragment newInstance(int sectionNumber) {
        StepPlaceholderFragment fragment = new StepPlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Extract the description of the steps
     *
     * @param recipe the recipe of which the steps will be extracted
     * @return a list of Strings, representing all the different descriptions of the steps
     */
    public static String[] extractDescriptionSteps(Recipe recipe) {
        int stepsCount = recipe.getRecipeSteps().size();
        String[] steps = new String[stepsCount];

        for (int i = 0; i < stepsCount; i++) {
            steps[i] = recipe.getRecipeSteps().get(i).getDescription();
        }
        return steps;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int index = Objects.requireNonNull(getArguments()).getInt(ARG_SECTION_NUMBER);

        // Inflate a CardView with a step and get the View
        mRootView = inflater.inflate(R.layout.fragment_steps, container, false);
        TextView titleTextView = mRootView.findViewById(R.id.tv_title);

        // Set the TextViews
        titleTextView.setText(getString(R.string.section_format, index + 1));

        RecipeViewModel recipeViewModel = ViewModelProviders
                .of(Objects.requireNonNull(getActivity()))
                .get(RecipeViewModel.class);
        recipeViewModel.getRecipe().observe(this, (Recipe recipe) ->
                this.onNewRecipeObserved(inflater, container, recipe, index));
        recipeViewModel.getNumberOfPeople().observe(this, this::update);

        return mRootView;
    }

    /**
     * Helper method called when a new recipe is observed
     *
     * @param inflater  Layout inflater to inflate necessary layouts
     * @param container ViewGroup to inflate in.
     * @param recipe    The observed recipe
     * @param index     The index of the step.
     */
    private void onNewRecipeObserved(LayoutInflater inflater, ViewGroup container, Recipe recipe, int index) {
        if (recipe == null) {
            return;
        }
        mDescriptionStep = extractDescriptionSteps(recipe);

        ViewGroup insertPoint = mRootView.findViewById(R.id.ll_step);

        RecipeTimerViewModel recipeTimerViewModel = ViewModelProviders
                .of(getActivity())
                .get(RecipeTimerViewModel.class);
        recipeTimerViewModel.init(recipe);

        mOriginalAmount = recipe.getNumberOfPeople();
        mRecipeStep = recipe.getRecipeSteps().get(getArguments().getInt(ARG_SECTION_NUMBER));
        mDescriptionBase = new String[mRecipeStep.getRecipeTimers().size() + 1];
        mStartIndexDescriptionBlocks = new int[mRecipeStep.getRecipeTimers().size() + 1];
        mStepTextViews = new ArrayList<>();

        // Sort ingredients on descending beginIndex
        Collections.sort(mRecipeStep.getIngredients(), (l0, l1) ->
                l0.getQuantityPosition().getBeginIndex() + l1.getQuantityPosition().getBeginIndex());

        // Setup the RecyclerView of the ingredients
        mIngredientList = mRootView.findViewById(R.id.rv_ingredient_list);
        mIngredientList.setLayoutManager(new LinearLayoutManager(this.getContext()));

        // Feed Adapter
        StepIngredientAdapter ingredientAdapter =
                new StepIngredientAdapter(mRecipeStep.getIngredients(), recipe.getNumberOfPeople(), mCurrentAmount);
        mIngredientList.setAdapter(ingredientAdapter);

        // Disable the line if there are no ingredients listed
        if (mRecipeStep.getIngredients().size() == 0) {
            mRootView.findViewById(R.id.v_line).setVisibility(View.GONE);
        }

        // Disable OVER_SCROLL effect (scrollbar is always visible, so effect not needed)
        mIngredientList.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mRootView.findViewById(R.id.sv_text_and_timers).setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Keep index of the beginning of a text block to know where to cut the text.
        int beginOfTextBlock = 0;

        // Run over all timers to place them correctly.
        for (int i = 0; i < recipe.getRecipeSteps().get(index).getRecipeTimers().size(); i++) {
            // New card for the timer.
            View timerCard = inflater.inflate(R.layout.timer_card, container, false);
            // New TextView for the recipe description.
            TextView textView = (TextView) inflater.inflate(R.layout.step_textview, container, false);

            // Get timer data in this step of the i'th timer.
            LiveDataTimer liveDataTimer = recipeTimerViewModel.getTimerInStep(index, i);
            // Set the margin of the timer
            int timerMargin = Math.round(getResources().getDimension(R.dimen.timer_margin));
            LinearLayout.LayoutParams layoutParamsTimer = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParamsTimer.gravity = Gravity.CENTER;
            layoutParamsTimer.setMargins(0, timerMargin, 0, timerMargin);
            timerCard.setLayoutParams(layoutParamsTimer);
            // Make new timer-object. Is actually never used after this.
            new UITimer(liveDataTimer, timerCard, this);

            // Set TextViews
            // search for the next place to cut.
            int endOfTextBlock = recipe.getRecipeSteps()
                    .get(index).getRecipeTimers()
                    .get(i).getPosition()
                    .getEndIndex();
            // get the substring and place it in the TextView
            String currentSubstring = mDescriptionStep[index].substring(beginOfTextBlock, endOfTextBlock);
            mDescriptionBase[i] = currentSubstring;
            mStartIndexDescriptionBlocks[i] = beginOfTextBlock;

            // Change all the quantities of the ingredients to the INGREDIENT_CODE
            mDescriptionBase[i] = changeQuantityToCode(mDescriptionBase[i],
                    beginOfTextBlock,
                    endOfTextBlock,
                    mDescriptionStep[index].length());

            // Update the text-block start to be at the beginning of the next piece.
            beginOfTextBlock = endOfTextBlock;

            // Add text and timers to the parent.
            insertPoint.addView(textView);
            insertPoint.addView(timerCard);
            mStepTextViews.add(textView);
        }
        // Check if there is still some text coming after the last timer
        // Repeat.
        if (beginOfTextBlock != mDescriptionStep[index].length()) {
            TextView textView = (TextView) inflater.inflate(R.layout.step_textview, container, false);
            String currentSubstring = mDescriptionStep[index].substring(beginOfTextBlock);
            mDescriptionBase[mDescriptionBase.length - 1] = currentSubstring;
            mStartIndexDescriptionBlocks[mStartIndexDescriptionBlocks.length - 1] = beginOfTextBlock;

            // Change all the quantities of the ingredients to the INGREDIENT_CODE
            mDescriptionBase[mDescriptionBase.length - 1] =
                    changeQuantityToCode(mDescriptionBase[mDescriptionBase.length - 1],
                            beginOfTextBlock,
                            mDescriptionStep[index].length(),
                            mDescriptionStep[index].length());

            insertPoint.addView(textView);
            mStepTextViews.add(textView);
        }

        // Add dots
        this.addDots(inflater, recipe, index);
    }

    /**
     * Changes the quantities of ingredients in description by INGREDIENT_CODE
     *
     * @param description       a block of text where the ingredient might be located
     * @param beginOfTextBlock  the begin index of the block
     * @param endOfTextBlock    the end index of the block
     * @param descriptionLength the length of the whole description
     */
    private String changeQuantityToCode(String description, int beginOfTextBlock, int endOfTextBlock,
                                        int descriptionLength) {
        for (Ingredient ingredient : mRecipeStep.getIngredients()) {
            // Check whether the quantity is in the description of the step
            if (ingredient.getQuantityPosition().getBeginIndex() != 0
                    || ingredient.getQuantityPosition().getEndIndex() != descriptionLength) {

                // Check if the quantity is represent in the current block of the description
                if (ingredient.getQuantityPosition().getBeginIndex() < beginOfTextBlock) {
                    // The current ingredient (and all the following) is located in a text-block which
                    // comes before the current text block
                    break;
                } else if (ingredient.getQuantityPosition().getEndIndex() > endOfTextBlock) {
                    // The current ingredient is located in a text-block which comes after the current
                    // text block. Following ingredients can still be located in this current text block
                    continue;
                }
                // The quantity is in the step and needs to be replaced by INGREDIENT_CODE
                description = description.
                        substring(0, ingredient.getQuantityPosition().getBeginIndex() - beginOfTextBlock)
                        + INGREDIENT_CODE
                        + description.substring(ingredient.getQuantityPosition().getEndIndex() - beginOfTextBlock);
            }
        }

        return description;
    }

    /**
     * Helper-class to add the navigation dots.
     *
     * @param inflater Layout inflater to inflate dot-views
     * @param recipe   used to get the amount of steps.
     * @param index    Index of the step, to see which dots to color.
     */
    private void addDots(LayoutInflater inflater, Recipe recipe, int index) {
        // Add the ImageViews to the LinearLayout for the indicator dots
        LinearLayout linearLayout = mRootView.findViewById(R.id.ll_dots);
        ImageView tempView;
        for (int i = 0; i < recipe.getRecipeSteps().size(); i++) {
            tempView = (ImageView) inflater.inflate(R.layout.dot_image_view, linearLayout, false);
            Drawable dot;
            if (i == index) {
                dot = ContextCompat.getDrawable(getContext(),
                        R.drawable.selected_dot);
            } else {
                dot = ContextCompat.getDrawable(getContext(),
                        R.drawable.not_selected_dot);
            }
            tempView.setImageDrawable(dot);
            linearLayout.addView(tempView);
        }
    }

    /**
     * This function will update the TextView with a new quantity
     *
     * @param newAmount the new set amount of people
     */
    protected void update(int newAmount) {
        ((StepIngredientAdapter) mIngredientList.getAdapter()).setCurrentAmount(newAmount);
        mIngredientList.getAdapter().notifyDataSetChanged();

        // Put ingredients in ascending beginIndex
        Collections.reverse(mRecipeStep.getIngredients());
        mCurrentAmount = newAmount;

        int currentTextView = 0;
        for (int i = 0; i < mStepTextViews.size(); i++) {
            String description = mDescriptionBase[i];
            int endOfTextView;
            // Define the end index of the current TextView
            try {
                endOfTextView = mStartIndexDescriptionBlocks[i + 1];
            } catch (IndexOutOfBoundsException e) {
                // Current TextView is last TextView, so the last index is the length of the description
                endOfTextView = mRecipeStep.getDescription().length();
            }

            // Replace the INGREDIENT_CODEs with the new quantity
            for (Ingredient ingredient : mRecipeStep.getIngredients()) {
                if (
                    // Check if the quantity is valid. This cannot only be the last check, because of
                    // description which only contain 1 block of text
                        ingredient.getQuantityPosition().getBeginIndex() != 0
                                && ingredient.getQuantityPosition().getEndIndex()
                                != mDescriptionStep[currentTextView].length()
                                // Check if the quantity is in current block
                                && ingredient.getQuantityPosition().getEndIndex() >= mStartIndexDescriptionBlocks[i]
                                && ingredient.getQuantityPosition().getEndIndex() < endOfTextView) {

                    // Calculate new quantity and get String representation
                    double newQuantity = ingredient.getQuantity() / mOriginalAmount * mCurrentAmount;
                    String quantityString = StringUtilities.toDisplayQuantity(newQuantity);

                    // Replace first INGREDIENT_CODE: Because of ascending begin index, the first will
                    // always be the right one the replace
                    description = description.replaceFirst(INGREDIENT_CODE, quantityString);
                }
            }


            // Remove optional spaces and dots at the beginning of the block and set the text
            Pattern p = Pattern.compile("\\p{Alpha}");
            Matcher m = p.matcher(description);
            if (m.find()) {
                mStepTextViews.get(i).setText(description.substring(m.start()));
            }
        }

        // Put ingredients back in descending beginIndex
        Collections.reverse(mRecipeStep.getIngredients());
    }
}
