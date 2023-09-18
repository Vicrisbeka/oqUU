package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupFirebase()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SplashFragment())
                .commit()
        }
    }

    fun setupFirebase() {
        // Define Firebase Remote Config settings
        val remoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // Example: Set the minimum fetch interval to 1 hour
            .build()
        // Initialize Firebase Remote Config with the specified settings
        Firebase.remoteConfig.setConfigSettingsAsync(remoteConfigSettings)

        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("REMOTE CONFIG", remoteConfig.getString("open_ai_token"))
                    RemoteConfigs.conf[RemoteConfiguration.OPEN_AI] =
                        remoteConfig.getString("open_ai_token")
                    RemoteConfigs.conf[RemoteConfiguration.SPEECH_AI] =
                        remoteConfig.getString("speech_ai_token")
                } else {
                    Toast.makeText(
                        this,
                        "Server is not working now... Sorry",
                        Toast.LENGTH_LONG
                    )
                }
            }
    }
}