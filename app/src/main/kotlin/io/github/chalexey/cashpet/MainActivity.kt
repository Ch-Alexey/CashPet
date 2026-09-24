package io.github.chalexey.cashpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chalexey.cashpet.content.ContentFiles

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskCount = ContentFiles.taskCount()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SkeletonScreen(taskCount)
                }
            }
        }
    }
}

@Composable
fun SkeletonScreen(taskCount: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.skeleton_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.skeleton_tasks, taskCount), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.skeleton_note), style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(widthDp = 360)
@Composable
private fun SkeletonScreenPreview() {
    MaterialTheme { SkeletonScreen(taskCount = 26) }
}
