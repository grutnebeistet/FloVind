package com.solutions.grutne.flovind

import android.app.PendingIntent.getActivity
import com.solutions.grutne.flovind.sync.SyncUtils
import timber.log.Timber

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceScreen


/**
 * Created by Adrian on 03/11/2017.
 */

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override
    fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_preferences)
        Timber.d("OnCreate PREFF")
        val sharedPreferences = preferenceScreen.sharedPreferences
        if (sharedPreferences.getString(getString(R.string.pref_map_type_key),
                getString(R.string.map_type_def_value)) == getString(R.string.map_type_def_value))
            findPreference(getString(R.string.map_pref_key)).isEnabled=true

        val count = preferenceScreen.preferenceCount
        for (i in 0 until count) {
            val p = preferenceScreen.getPreference(i)
            if (p !is android.support.v7.preference.CheckBoxPreference) {
                val value = sharedPreferences.getString(p.key, "")
                setPreferenceSummary(p, value)
            }
        }
    }

    private fun setPreferenceSummary(preference: Preference, value: Any?) {
        val stringValue = value!!.toString()
        val key = preference.key
        Timber.d("setPreferenceSummary, value, key: $value, $key")

        if (preference is ListPreference) {
            /* For list preferences, look up the correct display value in */
            /* the preference's 'entries' list (since they have separate labels/values). */
            val prefIndex = preference.findIndexOfValue(stringValue)
            if (prefIndex >= 0) {
                Timber.d("set summary: " + preference.entries[prefIndex])
                preference.setSummary(preference.entries[prefIndex])
                if (key == getString(R.string.pref_map_type_key))
                    if (value != getString(R.string.map_type_def_value))
                        findPreference(getString(R.string.map_pref_key)).isEnabled = false
                    else
                        findPreference(getString(R.string.map_pref_key)).isEnabled = true
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.summary = stringValue
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        Timber.d("onSharedPreferenceChanged, string: " + s)
        if (s == getString(R.string.notify_hours_key) || s == getString(R.string.pref_enable_notifications_key)) {
            SyncUtils.startImmediateSync(context!!) // TODO instead of parsing again, just query in make notifics
        }
        val preference = findPreference(s)
        if (null != preference) {
            if (preference !is android.support.v7.preference.CheckBoxPreference) {
                setPreferenceSummary(preference, sharedPreferences.getString(s, ""))
            }
        }
    }

    override
    fun onStop() {
        super.onStop()
        // unregister the preference change listener
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override
    fun onStart() {
        super.onStart()
        // register the preference change listener
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }
}
