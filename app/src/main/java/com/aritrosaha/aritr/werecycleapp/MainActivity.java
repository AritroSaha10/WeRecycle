package com.aritrosaha.aritr.werecycleapp;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    enum FragmentType{
        INDEX,
        SCANNER,
        SETTINGS
    }

    public static Boolean startedFlag = true;
    private String TAG = "MainActivity";
    FragmentType currentFragmentType;
    BottomNavigationView bottomNavigation;

    // checking if this will help with performance, since this fragment has to always init camerax
    IndexFragment indexFragment;
    ScannerFragment scannerFragment;
    SettingsFragmentPreferenceScreen settingsFragmentPreferenceScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (startedFlag){
            if (sharedPreferences.getBoolean("night_mode", false)){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            startedFlag = false;

            if (getSupportFragmentManager().findFragmentById(R.id.container) instanceof IndexFragment){
                ((IndexFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.container))).mProgressDialog.dismiss();
            }


            recreate();
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);

        // listener for BMV
        BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
                item -> {
                    switch (item.getItemId()) {
                        case R.id.navigation_index:
                            openIndex();
                            return true;

                        case R.id.navigation_scanner:
                            openScanner();
                            return true;

                        case R.id.navigation_settings:
                            openSettings(false);
                            return true;
                    }
                    return false;
                };

        bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);

        if (SettingsFragmentPreferenceScreen.openSettingsNotifier){
            // open settings if that was previous
            openSettings(true);
            SettingsFragmentPreferenceScreen.openSettingsNotifier = false;
        } else {
            // start off by opening index
            openIndex();
        }
    }

    void openIndex() {
        if (currentFragmentType != FragmentType.INDEX)
        {
            // setup fragment and replace in frame layout
            if (indexFragment == null) indexFragment = new IndexFragment();
            final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, indexFragment);

            // check if this is the first time its loading to stop fragment from going away if back button is pressed
            if (currentFragmentType != null) transaction.addToBackStack(null);

            // set currentFragmentType and commit the fragment changes
            currentFragmentType = FragmentType.INDEX;
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
        }
    }

    void openScanner() {
        if (currentFragmentType != FragmentType.SCANNER)
        {
            // set fragment type
            currentFragmentType = FragmentType.SCANNER;

            // to save resources, use the same fragment instead of creating a new one.
            // in this case, we check if the var is null (if it has not been launched yet). if yes,
            // then initialize it
            if (scannerFragment == null){
                scannerFragment = new ScannerFragment();
            }

            // setup and commit the fragment changes
            final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, scannerFragment);
            transaction.addToBackStack(null);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
        }
    }

    void openSettings(Boolean override) {
        if (currentFragmentType != FragmentType.SETTINGS || override)
        {
            // set fragment type
            currentFragmentType = FragmentType.SETTINGS;


            if (settingsFragmentPreferenceScreen == null) settingsFragmentPreferenceScreen = new SettingsFragmentPreferenceScreen();

            final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, settingsFragmentPreferenceScreen);
            transaction.addToBackStack(null);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
        }
    }
}
