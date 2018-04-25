package com.example.android.liondesk;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;


/**
 * @author Caroline Bidet  <caroline.bidet3@mail.dcu.ie>
 * @version 1.0                 (current version number of program)
 * @since 2018-04-12          (the version of the package this class was first added to)
 * <p>
 * <p>
 * <h2>Class Description:</h2>
 * AccountFragment class: records the user application preferences.
 * <p>
 * The 3 preferences which can be set are the user email address, their password
 * and whether they want to receive an automatic email confirmation of their booking.
 * <p>
 * <p>
 * <h2>Citation:</h2>
 * This class is adapted from the code in
 * //http://www.cs.dartmouth.edu/~campbell/cs65/code/fragmentpreference.zip
 * <p>
 * Retrieved on 2nd January 2018.
 * <p>
 * And also from the DCU HDSD SDA course notes from page 187.
 */

public class AccountFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    static final String TAG = AccountFragment.class.getSimpleName();

    // Create the preferences
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {


        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    // restaures the preferences on resume
    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) pref;
            if (!pref.getKey().equalsIgnoreCase("email")) {

                pref.setSummary(editTextPref.getText());
            }
        }
    }

}


