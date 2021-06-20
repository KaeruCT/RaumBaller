package jgame.platform;
import android.app.*;
import android.os.Bundle;
import android.content.res.AssetManager;

import android.hardware.*;
import android.view.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;

import android.content.Intent;
import android.content.DialogInterface;
import android.net.Uri;

import java.util.*;

import android.content.SharedPreferences;
import android.preference.*;


public class JGPreferences extends PreferenceActivity {

	JGEngine eng;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		eng = JGEngine.current_engine;

        setPreferenceScreen(createPreferenceHierarchy());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        PreferenceCategory currentPrefCat = new PreferenceCategory(this);
		currentPrefCat.setTitle("Preferences");
		boolean catAdded=false;
		for (Enumeration e=eng.settings_array.elements();e.hasMoreElements();) {
			Object el = e.nextElement();
			if (el instanceof SettingsTitle) {
		        currentPrefCat = new PreferenceCategory(this);
				currentPrefCat.setTitle(((SettingsTitle)el).titledesc);
				catAdded=false;
			} else {
				if (!catAdded) {
        			root.addPreference(currentPrefCat);
					catAdded=true;
				}
				((Setting)el).addMenuItem(eng,this,currentPrefCat);
			}
		}


//        // Launch preferences
//        PreferenceCategory launchPrefCat = new PreferenceCategory(this);
//        launchPrefCat.setTitle(R.string.launch_preferences);
//        root.addPreference(launchPrefCat);
//
//        /*
//         * The Preferences screenPref serves as a screen break (similar to page
//         * break in word processing). Like for other preference types, we assign
//         * a key here so that it is able to save and restore its instance state.
//         */
//        // Screen preference
//        PreferenceScreen screenPref = getPreferenceManager().createPreferenceScreen(this);
//        screenPref.setKey("screen_preference");
//        screenPref.setTitle(R.string.title_screen_preference);
//        screenPref.setSummary(R.string.summary_screen_preference);
//        launchPrefCat.addPreference(screenPref);
//
//        /*
//         * You can add more preferences to screenPref that will be shown on the
//         * next screen.
//         */
//
//        // Example of next screen toggle preference
//        CheckBoxPreference nextScreenCheckBoxPref = new CheckBoxPreference(this);
//        nextScreenCheckBoxPref.setKey("next_screen_toggle_preference");
//        nextScreenCheckBoxPref.setTitle(R.string.title_next_screen_toggle_preference);
//        nextScreenCheckBoxPref.setSummary(R.string.summary_next_screen_toggle_preference);
//        screenPref.addPreference(nextScreenCheckBoxPref);
//
//        // Intent preference
//        PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(this);
//        intentPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW)
//                .setData(Uri.parse("http://www.android.com")));
//        intentPref.setTitle(R.string.title_intent_preference);
//        intentPref.setSummary(R.string.summary_intent_preference);
//        launchPrefCat.addPreference(intentPref);
//
//        // Preference attributes
//        PreferenceCategory prefAttrsCat = new PreferenceCategory(this);
//        prefAttrsCat.setTitle(R.string.preference_attributes);
//        root.addPreference(prefAttrsCat);
//
//        // Visual parent toggle preference
//        CheckBoxPreference parentCheckBoxPref = new CheckBoxPreference(this);
//        parentCheckBoxPref.setTitle(R.string.title_parent_preference);
//        parentCheckBoxPref.setSummary(R.string.summary_parent_preference);
//        prefAttrsCat.addPreference(parentCheckBoxPref);
//
//        // Visual child toggle preference
//        // See res/values/attrs.xml for the <declare-styleable> that defines
//        // TogglePrefAttrs.
//        TypedArray a = obtainStyledAttributes(R.styleable.TogglePrefAttrs);
//        CheckBoxPreference childCheckBoxPref = new CheckBoxPreference(this);
//        childCheckBoxPref.setTitle(R.string.title_child_preference);
//        childCheckBoxPref.setSummary(R.string.summary_child_preference);
//        childCheckBoxPref.setLayoutResource(
//                a.getResourceId(R.styleable.TogglePrefAttrs_android_preferenceLayoutChild,
//                        0));
//        prefAttrsCat.addPreference(childCheckBoxPref);
//        a.recycle();

        return root;
    }


	@Override
	protected void onResume() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();

	}

	@Override
	protected void onPause() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
	}

}

