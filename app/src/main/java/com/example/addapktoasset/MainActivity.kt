package com.example.addapktoasset

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.addapktoasset.ui.theme.AddApkToAssetTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())
        setContent {
            AddApkToAssetTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val scope  = remember {
        MainScope()
    }
    val context = LocalContext.current
    val installHelper = remember {
        InstallHelper(context = context)
    }
    Button(
        modifier = modifier,
        onClick = {
            scope.launch {
                val localApk = copyApkToLocal(context)
                installHelper.installApk(localApk) }
        }
    ) {
        Text(text = "Click me to install the app2")
    }
}

fun copyApkToLocal(context: Context): File {
    val apkName = "app2-release.apk"
    val inputStream = context.assets.open(apkName)
    val localApk = context.getFileStreamPath(apkName)
    localApk.delete()
    localApk.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
        inputStream.close()
    }
    return localApk
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AddApkToAssetTheme {
        Greeting("Android")
    }
}