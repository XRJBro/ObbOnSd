package com.smartmadsoft.xposed.obbonsd;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;

public class Preferences extends AppCompatPreferenceActivity {
    Preference prefAlternative;
    Preference prefLabelPathInternal;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);

        addPreferencesFromResource(R.layout.preferences);

        if (Deluxe.showBottomAd(getApplicationContext(), this)) {
            setContentView(R.layout.main);

            // Load an ad into the AdMob banner view.
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        prefAlternative = findPreference("enable_alternative");
        prefLabelPathInternal = findPreference("label-path_internal");

        if (Build.VERSION.SDK_INT < 21)
            prefAlternative.setEnabled(false);

        prefAlternative.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                printAltSummary(preference, (boolean) o);
                return true;
            }
        });
        printAltSummary(prefAlternative, ((SwitchPreference) prefAlternative).isChecked());

        printPathInternalSummary(prefLabelPathInternal);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_ads:
                Deluxe.openPlayStore(getApplicationContext());
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (Deluxe.showMenu(getApplicationContext()))
            getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    void printAltSummary(Preference preference, boolean enabled) {
        String path = getAltSummary(enabled);
        if (path == null)
            preference.setSummary("No path found");
        else
            preference.setSummary("Detected: " + path);
    }

    void printPathInternalSummary(Preference preference) {
        String path = getRealInternal();
        if (path == null || path.equals(""))
            preference.setSummary("No path found");
        else {
            preference.setSummary("Detected: " + path);
            savePath(null, path);
        }
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

    String getRealInternal() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    String getRealExternal2() {
        if (Build.VERSION.SDK_INT >= 21) {
            File[] dirs = this.getExternalMediaDirs();
            for (File dir : dirs) {
                if (Environment.isExternalStorageRemovable(dir)) {
                    String absolutePath = dir.getAbsolutePath();
                    int c = absolutePath.indexOf("/Android/");
                    String path = absolutePath.substring(0, c);
                    savePath(path, null);
                    return path;
                }
            }
        }
        return null;
    }

    void savePath(String path, String pathInternal) {
        SharedPreferences settings = getPreferenceManager().getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        if (path != null)
            editor.putString("path", path);
        if (pathInternal != null)
            editor.putString("path_internal", pathInternal);
        editor.commit();
    }

}
