package com.aurora.hulpchef;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.aurora.auroralib.ExtractedText;
import com.aurora.auroralib.translation.TranslationServiceCaller;
import com.aurora.souschefprocessor.facade.RecipeDetectionException;
import com.aurora.souschefprocessor.facade.SouschefProcessorCommunicator;
import com.aurora.souschefprocessor.recipe.Recipe;

import java.util.List;

/**
 * Holds the data of a recipe. Is responsible for keeping that data up to date,
 * and updating the UI when necessary.
 */
public class RecipeViewModel extends AndroidViewModel {
    /**
     * When initialising Souschef, poll every MILLIS_BETWEEN_UPDATES milliseconds
     * for updates on the progressbar. This could also be done with an observable.
     */
    private static final int MILLIS_BETWEEN_UPDATES = 500;

    /**
     * The amount of steps it takes to detect a recipe.
     * This is used to pick the interval updates of the progress bar.
     * These steps are hard-coded-counted. This means that when the implementation
     * of the Souschef-processor takes longer or shorter, this value must be changed.
     */
    private static final int DETECTION_STEPS = 4;

    /**
     * The maximum amount of people you can cook for.
     */
    private static final int MAX_PEOPLE = 80;

    /**
     * Stop actively updating the progressbar after MAX_WAIT_TIME.
     */
    private static final int MAX_WAIT_TIME = 15000;

    /**
     * Percentages in 100%
     */
    private static final double MAX_PERCENTAGE = 100.0;

    /**
     * Default amount of people
     */
    private static final int DEFAULT_SERVINGS_AMOUNT = 4;

    /**
     * LiveData of the current amount of people. Used for changing the amount of people,
     * especially tab 2.
     */
    private MutableLiveData<Integer> mCurrentPeople;

    /**
     * LiveData of the progress. Used to update the UI according to the progress.
     */
    private MutableLiveData<Integer> mProgressStep;

    /**
     * This LiveData value updates when the initialisation is finished.
     */
    private MutableLiveData<Boolean> mInitialised;

    /**
     * When the recipe is set, this value changes -> all observers act.
     */
    private MutableLiveData<Recipe> mRecipe = new MutableLiveData<>();

    /**
     * This LiveData value updates when the processing has failed
     */
    private MutableLiveData<Boolean> mProcessingFailed = new MutableLiveData<>();

    /**
     * This LiveData value updates when the processing has failed and sets the failing message
     */
    private MutableLiveData<String> mFailureMessage = new MutableLiveData<>();

    /**
     * This LiveData value updates when the amount of people is not found and set to default
     */
    private MutableLiveData<Boolean> mDefaultAmountSet = new MutableLiveData<>();

    /**
     * Indicates whether or not this recipe is already being processed
     */
    private boolean isBeingProcessed = false;

    /**
     * boolean that keeps track of the current language, is true if the language is Dutch, false if the language is
     * English
     */
    private boolean isDutch = false;

    /**
     * The original processed recipe if the processing has succeeded
     */
    private Recipe mEnglishRecipe;

    /**
     * The translated recipe if the translation has succeeded
     */
    private Recipe mDutchRecipe;

    /**
     * A private helper that is responsible with calling the translation service
     */
    private TranslationServiceCaller mTranslationServiceCaller;

    /**
     * The context of the application.
     * <p>
     * The only use of context is to get the Souschef NLP model loaded.
     * This leak is not an issue.
     */
    @SuppressLint("StaticFieldLeak")
    private Context mContext;

    /**
     * Listener that listens to changes in the shared preferences. It is used to check when the user
     * changes the settings from metric to imperial or back.
     * <p>
     * Must be a variable of this class to prevent garbage collection and stop listening
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mListener = null;

    /**
     * Constructor that initialises the pipeline and LiveData.
     *
     * @param application Needed for the initialisation and lifetime of a viewModel
     */
    public RecipeViewModel(@NonNull Application application) {
        super(application);
        this.mContext = application;
        mProgressStep = new MutableLiveData<>();
        mProgressStep.setValue(0);
        mInitialised = new MutableLiveData<>();
        mInitialised.setValue(false);
        mCurrentPeople = new MutableLiveData<>();
        mCurrentPeople.setValue(0);
        mProcessingFailed.setValue(false);
        mDefaultAmountSet.setValue(false);
        SouschefProcessorCommunicator.createAnnotationPipelines();
        SharedPreferences sharedPreferences = application.getSharedPreferences(
                Tab1Overview.SETTINGS_PREFERENCES,
                Context.MODE_PRIVATE);
        mListener = (SharedPreferences preferences, String key) -> {
            if (key.equals(Tab1Overview.VERTAAL)) {
                boolean toDutch = preferences.getBoolean(key, false);
                translate(toDutch);
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(mListener);
        mTranslationServiceCaller = new TranslationServiceCaller(application);

    }

    /**
     * Converts all the units in the recipe
     *
     * @param toDutch boolean that indicates if the units should be translated to Dutch, if false it translated Dutch to
     *                English
     */
    public void translate(boolean toDutch) {

        // only translate if the target and source language are not equal
        if (isDutch != toDutch) {
            // get the recipe value
            Recipe recipe = mRecipe.getValue();

            if (recipe != null) {

                List<String> sentences = recipe.createSentencesToTranslate();
                String source;
                String target;

                if (recipe.equals(mEnglishRecipe)) {
                    // do the translation only if the dutch version has not been initialized
                    if (mDutchRecipe == null) {
                        source = "en";
                        target = "nl";

                        new TranslationTask(sentences, source, target,
                                mTranslationServiceCaller).execute();

                    } else {
                        // post the dutch recipe
                        mRecipe.postValue(mDutchRecipe);
                    }
                    isDutch = true;
                } else {
                    // post the english recipe
                    mRecipe.postValue(mEnglishRecipe);
                    isDutch = false;
                }
            }
        }
    }

    public LiveData<String> getFailureMessage() {
        return mFailureMessage;
    }

    /**
     * Get the progress LiveData object
     *
     * @return live progress
     */
    public LiveData<Integer> getProgressStep() {
        return mProgressStep;
    }

    /**
     * Get the actual progress, in percentages.
     *
     * @return progress-percentage
     */
    public int getProgress() {
        if (mProgressStep == null || mProgressStep.getValue() == null) {
            return 0;
        }
        return (int) (MAX_PERCENTAGE / DETECTION_STEPS * mProgressStep.getValue());
    }

    /**
     * Initialise the data from plain text.
     *
     * @param plainText where to extract recipe from.
     */
    public void initialiseWithPlainText(String plainText) {
        if (mInitialised != null && mInitialised.getValue() != null && mInitialised.getValue()) {
            return;
        }
        (new ProgressUpdate()).execute();
        (new SouschefInit(plainText)).execute();
    }

    /**
     * Initialise the data with {@link ExtractedText}.
     *
     * @param extractedText where to get recipe from.
     */
    public void initialiseWithExtractedText(ExtractedText extractedText) {
        if (mInitialised != null && mInitialised.getValue() != null && mInitialised.getValue()) {
            return;
        }
        (new ProgressUpdate()).execute();
        (new SouschefInit(extractedText)).execute();

    }

    /**
     * Initialise data directly with a recipe.
     *
     * @param recipe the recipe for data extraction.
     */
    public void initialiseWithRecipe(Recipe recipe) {
        RecipeViewModel.this.mRecipe.setValue(recipe);
        if (mRecipe.getValue().getNumberOfPeople() == -1) {
            mRecipe.getValue().setNumberOfPeople(DEFAULT_SERVINGS_AMOUNT);
            mDefaultAmountSet.setValue(true);
        }
        RecipeViewModel.this.mCurrentPeople.setValue(recipe.getNumberOfPeople());
        isDutch = false;
        mEnglishRecipe = recipe;
        mInitialised.setValue(true);
        if (isPreferenceSetToDutch()) {
            translate(true);
        }
    }

    private boolean isPreferenceSetToDutch() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Tab1Overview.SETTINGS_PREFERENCES,
                Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Tab1Overview.VERTAAL, false);
    }

    public LiveData<Boolean> getInitialised() {
        return mInitialised;
    }

    public LiveData<Integer> getNumberOfPeople() {
        return mCurrentPeople;
    }

    public LiveData<Recipe> getRecipe() {
        return mRecipe;
    }

    public LiveData<Boolean> getProcessFailed() {
        return mProcessingFailed;
    }

    public LiveData<Boolean> getDefaultAmountSet() {
        return mDefaultAmountSet;
    }

    /**
     * Increment the amount of people.
     * A maximum of {@value MAX_PEOPLE} people can be cooked for.
     */
    public void incrementPeople() {
        if (mCurrentPeople == null || mCurrentPeople.getValue() == null) {
            return;
        }
        if (mCurrentPeople.getValue() < MAX_PEOPLE) {
            mCurrentPeople.setValue(mCurrentPeople.getValue() + 1);
        }
    }

    /**
     * Decrement the amount of people.
     * Decrementing cannot go below 1.
     */
    public void decrementPeople() {
        if (mCurrentPeople == null || mCurrentPeople.getValue() == null) {
            return;
        }
        if (mCurrentPeople.getValue() > 1) {
            mCurrentPeople.setValue(mCurrentPeople.getValue() - 1);
        }
    }

    public boolean isBeingProcessed() {
        return isBeingProcessed;
    }

    public void setBeingProcessed(boolean isBeingProcessed) {
        this.isBeingProcessed = isBeingProcessed;
    }

    /**
     * Async task executing the logic for the progress bar.
     * If leaked, it will stop after {@value MAX_WAIT_TIME} milliseconds.
     */
    @SuppressLint("StaticFieldLeak")
    class ProgressUpdate extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            int upTime = 0;
            try {
                while (!mInitialised.getValue()) {
                    Thread.sleep(MILLIS_BETWEEN_UPDATES);
                    upTime += MILLIS_BETWEEN_UPDATES;

                    publishProgress(SouschefProcessorCommunicator.getProgressAnnotationPipelines());
                    if (SouschefProcessorCommunicator.getProgressAnnotationPipelines()
                            >= DETECTION_STEPS || upTime > MAX_WAIT_TIME) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Log.e(RecipeViewModel.class.getSimpleName(), "Caught interruptedException");
                Thread.currentThread().interrupt();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressStep.setValue(values[0]);
        }
    }

    /**
     * Async taks executing the Souschef initialisation.
     */
    @SuppressLint("StaticFieldLeak")
    class SouschefInit extends AsyncTask<Void, String, Recipe> {

        private ExtractedText mExtractedText;

        SouschefInit(String text) {
            this.mExtractedText = ExtractedText.fromJson(text);
        }

        SouschefInit(ExtractedText extractedText) {
            this.mExtractedText = extractedText;
        }

        @Override
        protected Recipe doInBackground(Void... voids) {
            // Progressupdates are in demostate

            SouschefProcessorCommunicator comm = SouschefProcessorCommunicator.createCommunicator(mContext);
            if (comm != null) {

                try {

                    if (mExtractedText.getSections() == null) {
                        throw new RecipeDetectionException("The received text from Aurora did " +
                                "not contain sections" +
                                ", make sure you can open this type of file. If the problem" +
                                " persists, please send feedback in Aurora");
                    }
                    return (Recipe) comm.pipeline(mExtractedText);

                } catch (RecipeDetectionException rde) {
                    Log.d("FAILURE", rde.getMessage());
                    mFailureMessage.postValue(rde.getMessage());
                    mProcessingFailed.postValue(true);

                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Recipe recipe) {
            // only initialize if the processing has not failed
            if (!mProcessingFailed.getValue()) {
                initialiseWithRecipe(recipe);
            }
        }
    }

    /**
     * A private task that calls the {@link TranslationServiceCaller#translateOperation(List, String, String)} method
     * and will post the result
     */
    private  class TranslationTask extends AsyncTask<Void, Void, List<String>> {
        private List<String> mSentences;
        private String mSourceLanguage;
        private String mDestinationLanguage;
        private TranslationServiceCaller mTranslationServiceCaller;


        TranslationTask(List<String> sentences, String sourceLanguage, String destinationLanguage,
                        TranslationServiceCaller translationServiceCaller) {
            this.mSentences = sentences;
            this.mSourceLanguage = sourceLanguage;
            this.mDestinationLanguage = destinationLanguage;
            this.mTranslationServiceCaller = translationServiceCaller;

        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            List<String> result = mTranslationServiceCaller.translateOperation(mSentences,
                    mSourceLanguage, mDestinationLanguage);
            Log.d(getClass().getSimpleName(), result.toString());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(List<String> translatedSentences) {
            if (!translatedSentences.isEmpty()) {
                Log.d(getClass().getSimpleName(), translatedSentences.toString());
                mDutchRecipe = mRecipe.getValue().getTranslatedRecipe(translatedSentences.toArray(new String[0]));
                mRecipe.postValue(mDutchRecipe);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);


                builder.setMessage("Het lukte niet dit recept te vertalen. Bent u met het internet geconnecteerd?")
                        .setTitle("Er ging iets mis...");

                // set the dutch flag back to false
                isDutch = false;

                AlertDialog dialog = builder.create();
                dialog.setCancelable(true);
                dialog.show();



            }

        }
    }
}

