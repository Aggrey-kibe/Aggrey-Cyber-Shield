package com.aggreyshield.app.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** The single DataStore file for non-sensitive app preferences (never tokens or personal data). */
val Context.settingsDataStore by preferencesDataStore(name = "aggrey_shield_settings")
