package org.odk.collect.android.configure;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.odk.collect.android.application.initialization.migration.PreferenceMigrator;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.odk.collect.android.utilities.SharedPreferencesUtils.put;

public class CollectSettingsImporter {

    private final SharedPreferences generalSharedPrefs;
    private final SharedPreferences adminSharedPrefs;
    private final PreferenceMigrator preferenceMigrator;
    private final SettingsValidator settingsValidator;
    private final Map<String, Object> generalDefaults;
    private final Map<String, Object> adminDefaults;

    public CollectSettingsImporter(SharedPreferences generalSharedPrefs, SharedPreferences adminSharedPrefs, PreferenceMigrator preferenceMigrator, SettingsValidator settingsValidator, Map<String, Object> generalDefaults, Map<String, Object> adminDefaults) {
        this.generalSharedPrefs = generalSharedPrefs;
        this.adminSharedPrefs = adminSharedPrefs;
        this.preferenceMigrator = preferenceMigrator;
        this.settingsValidator = settingsValidator;
        this.generalDefaults = generalDefaults;
        this.adminDefaults = adminDefaults;
    }

    public boolean fromJSON(@NonNull String json) {
        if (!settingsValidator.isValid(json)) {
            return false;
        }

        try {
            JSONObject jsonObject = new JSONObject(json);

            JSONObject general = jsonObject.getJSONObject("general");
            importToPrefs(general, generalSharedPrefs);

            JSONObject admin = jsonObject.getJSONObject("admin");
            importToPrefs(admin, adminSharedPrefs);
        } catch (JSONException ignored) {
            // Ignored
        }

        preferenceMigrator.migrate();

        clearUnknownKeys(generalSharedPrefs, generalDefaults);
        clearUnknownKeys(adminSharedPrefs, adminDefaults);

        loadDefaults(generalSharedPrefs, generalDefaults);
        loadDefaults(adminSharedPrefs, adminDefaults);

        return true;
    }

    private void importToPrefs(JSONObject object, SharedPreferences sharedPreferences) throws JSONException {
        Iterator<String> generalKeys = object.keys();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        while (generalKeys.hasNext()) {
            String key = generalKeys.next();
            put(editor, key, object.get(key));
        }

        editor.apply();
    }

    private void loadDefaults(SharedPreferences sharedPreferences, Map<String, Object> defaults) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!sharedPreferences.contains(entry.getKey())) {
                put(editor, entry.getKey(), entry.getValue());
            }
        }

        editor.apply();
    }

    private void clearUnknownKeys(SharedPreferences sharedPreferences, Map<String, Object> defaults) {
        Set<String> keys = sharedPreferences.getAll().keySet();
        for (String key : keys) {
            if (!defaults.containsKey(key)) {
                sharedPreferences.edit().remove(key).apply();
            }
        }
    }
}
