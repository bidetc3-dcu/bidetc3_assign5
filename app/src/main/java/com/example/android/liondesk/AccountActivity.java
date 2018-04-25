package com.example.android.liondesk;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.Display;


public class AccountActivity extends AppCompatActivity {

    private static final String TAG = AccountActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

    // copied from https://github.com/mstummer/extended-preferences-compat/blob/master/app/src/main/java/com/maximumapps/extendedpreferencescompat/MainActivity.java
    // found at https://stackoverflow.com/questions/34983932/howto-use-support-v7-preference-with-appcompat-and-potential-drawbacks

        if (savedInstanceState == null) {
            // Create the fragment only when the activity is created for the first time.
            // i.e. not after orientation changes
            AccountFragment fragment = new AccountFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.account_fragment_holder, fragment, AccountFragment.TAG)
                    .commit();

        }

    }
}

