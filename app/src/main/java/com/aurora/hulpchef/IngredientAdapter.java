package com.aurora.hulpchef;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.aurora.hulpchef.utilities.StringUtilities;
import com.aurora.souschefprocessor.recipe.ListIngredient;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for populating the ingredient list.
 */
public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.CardIngredientViewHolder> {
    /**
     * List with ingredients.
     */
    private final List<ListIngredient> mIngredients;
    /**
     * Holds the original amount of servings (directly from the recipe)
     */
    private int mOriginalAmountOfServings;
    /**
     * Actual amount of servings (set by the user).
     */
    private int mChosenAmountOfServings;
    /**
     * Contains whether or not a checkbox is checked, for each ingredient.
     */
    private boolean[] mChecked;

    /**
     * Constructs the adapter with a list
     *
     * @param ingredients              list for construction
     * @param originalAmountOfServings the number of servings in the original text
     */
    public IngredientAdapter(List<ListIngredient> ingredients, int originalAmountOfServings) {
        mChecked = new boolean[ingredients.size()];
        this.mIngredients = ingredients;
        mChosenAmountOfServings = originalAmountOfServings;
        mOriginalAmountOfServings = originalAmountOfServings;
    }

    /**
     * Change the amount of people that is being cooked for.
     * Updates all ingredient amounts.
     *
     * @param chosenAmount new amount of people.
     */
    public void setChoseAmountOfServings(int chosenAmount) {
        mChosenAmountOfServings = chosenAmount;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardIngredientViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.ingredient_item, viewGroup, false);
        return new CardIngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardIngredientViewHolder cardIngredientViewHolder, int i) {
        cardIngredientViewHolder.bind();
    }

    @Override
    public int getItemCount() {
        return mIngredients.size();
    }

    /**
     * ViewHolder for the ingredients.
     */
    public class CardIngredientViewHolder extends RecyclerView.ViewHolder {
        /**
         * Error margin on the ingredient amounts.
         * The conversion is willing to make a mistake of ROUND_EPSILON
         * to display fractions. Currently at 5%.
         */
        private static final double ROUND_EPSILON = 0.05;

        /**
         * View for the ingredient name.
         */
        private TextView mIngredientName;
        /**
         * View for the ingredient amount.
         */
        private TextView mIngredientAmount;
        /**
         * View for the ingredient unit.
         */
        private TextView mIngredientUnit;
        /**
         * The full ingredient card, used to register clicks.
         */
        private CardView mIngredientCard;
        /**
         * Checkbox indicating if an ingredient is available, or whatever the user wants.
         */
        private CheckBox mCheckbox;

        /**
         * Initialises views inside the layout.
         *
         * @param itemView containing view
         */
        public CardIngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            mIngredientName = itemView.findViewById(R.id.tv_ingredient_name);
            mIngredientAmount = itemView.findViewById(R.id.tv_ingredient_amount);
            mIngredientUnit = itemView.findViewById(R.id.tv_ingredient_unit);
            mIngredientCard = itemView.findViewById(R.id.cv_ingredient_item);
            mCheckbox = itemView.findViewById(R.id.cb_ingredient_checked);

            mCheckbox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                    mChecked[getAdapterPosition()] = isChecked);
        }

        /**
         * populate individual views with the correct data
         */
        private void bind() {
            ListIngredient ingredient = mIngredients.get(getAdapterPosition());

            String nameWithoutQuantityAndUnit = ingredient.getName();
            // if it is possible to capitalize the first letter, capitalize.
            if (nameWithoutQuantityAndUnit.length() > 1) {
                nameWithoutQuantityAndUnit = nameWithoutQuantityAndUnit.substring(0, 1).toUpperCase(Locale.getDefault())
                        + nameWithoutQuantityAndUnit.substring(1);
            }

            // Calculate the amount of the ingredient
            double amount = ingredient.getQuantity() / mOriginalAmountOfServings * mChosenAmountOfServings;

            // Set Textviews
            mIngredientAmount.setText(StringUtilities.toDisplayQuantity(amount));
            mIngredientName.setText(nameWithoutQuantityAndUnit);
            mIngredientUnit.setText(ingredient.getUnit());

            // Set checkboxes correctly
            mCheckbox.setChecked(mChecked[getAdapterPosition()]);
        }
    }
}
