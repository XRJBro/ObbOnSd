package com.smartmadsoft.xposed.obbonsd;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.io.File;

public class Preferences extends AppCompatPreferenceActivity {
    Preference prefAlternative;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);

        addPreferencesFromResource(R.layout.preferences);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        prefAlternative = findPreference("enable_alternative");

        if (Build.VERSION.SDK_INT < 21)
            prefAlternative.setEnabled(false);

        prefAlternative.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                printAltSummary(preference, (boolean) o);
                return true;
            }
        });
        printAltSummary(prefAlternative, ((SwitchPreference)prefAlternative).isChecked());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void printAltSummary(Preference preference, boolean enabled) {
        String path = getAltSummary(enabled);
        if (path == null)
            preference.setSummary("No path found");
        else
            preference.setSummary("Detected: " + path);
    }

    String getAltSummary(boolean enabled) {
        if (enabled)
            return getRealExternal2();
        else
            return getRealExternal1();
    }

    String getRealExternal1() {
        String secondaryStorage = System.getenv("SECONDARY_STORAGE");
        if (secondaryStorage == null)
            return null;
        return secondaryStorage.split(":")[0];
    }

    String getRealExternal2() {
        if (Build.VERSION.SDK_INT >= 21) {
            File[] dirs = this.getExternalMediaDirs();
            for (File dir : dirs) {
                if (Environment.isExternalStorageRemovable(dir)) {
                    String absolutePath = dir.getAbsolutePath();
                    int c = absolutePath.indexOf("/Android/");
                    String path = absolutePath.substring(0, c);
                    savePath(path);
                    return path;
                }
            }
        }
        return null;
    }

    void savePath(String path) {
        SharedPreferences settings = getPreferenceManager().getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("path", path);
        editor.commit();
    }

}
