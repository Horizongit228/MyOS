package com.myos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.myos.app.data.LocalStore
import com.myos.app.data.SecureKeyStore
import com.myos.app.ui.MyOsApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { LocalStore(applicationContext) }
            val secrets = remember { SecureKeyStore(applicationContext) }
            MyOsApp(store, secrets)
        }
    }
}
