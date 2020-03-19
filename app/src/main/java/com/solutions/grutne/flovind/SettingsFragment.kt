package com.solutions.grutne.flovind

import timber.log.Timber

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.solutions.grutne.flovind.utils.NotificationUtils
import java.util.concurrent.TimeUnit


/**
 * Created by Adrian on 03/11/2017.
 */

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override
    fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_preferences)
        Timber.d("OnCreate PREFF")
        val sharedPreferences = preferenceScreen.sharedPreferences

        val count = preferenceScreen.preferenceCount
        for (i in 0 until count) {
            val p = preferenceScreen.getPreference(i)
            if (p !is CheckBoxPreference) {
                val value = sharedPreferences.getString(p.key, "")
                setPreferenceSummary(p, value)
            }
        }
    }

    private fun setPreferenceSummary(preference: Preference, value: Any?) {
        val stringValue = value!!.toString()
        val key = preference.key

        if (preference is ListPreference) {
            /* For list preferences, look up the correct display value in */
            /* the preference's 'entries' list (since they have separate labels/values). */
            val prefIndex = preference.findIndexOfValue(stringValue)
            if (prefIndex >= 0) {
                preference.setSummary(preference.entries[prefIndex])
                if (key == getString(R.string.pref_map_type_key))
                    findPreference<Preference>(getString(R.string.map_pref_key))?.isEnabled = value != getString(R.string.map_type_def_value)
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.summary = stringValue
        }

        findPreference<Preference>(getString(R.string.map_pref_key))?.isEnabled =
                preferenceScreen.sharedPreferences.getString(getString(R.string.pref_map_type_key),
                        getString(R.string.map_type_def_value)) == getString(R.string.map_type_def_value)

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (context == null) return

        Timber.d("onSharedPreferenceChanged, key:$key ")
        if (key == getString(R.string.pref_enable_notifications_key) || key == getString(R.string.notify_hours_key)) {
            val enableNotification = sharedPreferences.getBoolean(getString(R.string.pref_enable_notifications_key), false)
            val prefOffset = PreferenceManager.getDefaultSharedPreferences(context).getString(
                    context!!.getString(R.string.notify_hours_key), context!!.getString(R.string.notify_hours_default))

            if (enableNotification) {
                val userOffset = TimeUnit.HOURS.toMillis(prefOffset!!.toLong())
                NotificationUtils.prepareNotification(context!!.applicationContext, userOffset = userOffset)
            } else {
                NotificationUtils.cancelNotification(context!!.applicationContext)
            }
        }
        val preference = findPreference<Preference>(key)
        if (null != preference) {
            if (preference !is CheckBoxPreference) {
                setPreferenceSummary(preference, sharedPreferences.getString(key, ""))
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
