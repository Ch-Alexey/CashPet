package io.github.chalexey.cashpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.chalexey.cashpet.app.ui.CashPetApp
import io.github.chalexey.cashpet.app.ui.theme.CashPetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // CashPetApp owns the selected theme and persists it between launches.
            CashPetTheme {
                CashPetApp()
            }
        }
    }
}
