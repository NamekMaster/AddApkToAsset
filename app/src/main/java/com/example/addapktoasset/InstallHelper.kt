package com.example.addapktoasset

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InstallHelper(private val context: Context) {

    suspend fun installApk(localApk: File) {
        coroutineScope {
            launch { waitInstallComplete() }
            launch {
                with(context.packageManager.packageInstaller) {
                    val sessionId = createSession(
                        PackageInstaller.SessionParams(
                            PackageInstaller.SessionParams.MODE_FULL_INSTALL
                        )
                    )
                    openSession(sessionId).use { session ->
                        session.openWrite(localApk.name, 0, localApk.length()).use { output ->
                            localApk.inputStream().use { input ->
                                input.copyTo(output)
                                session.fsync(output)
                            }
                        }

                        val intent = Intent(InstallReceiver.action)
                        val callback = android.app.PendingIntent.getBroadcast(
                            context,
                            sessionId,
                            intent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        logger.d("commit session");
                        session.commit(callback.intentSender)
                    }
                }
            }
        }
    }

    private suspend fun waitInstallComplete() {
        val intentFilter = IntentFilter().apply {
            addAction(InstallReceiver.action)
        }
        suspendCoroutine<Unit> {
            logger.d("wait install complete, register broadcast receiver")
            context.registerReceiver(object: InstallReceiver() {

                override fun onSuccess() {
                    logger.d("install success")
                    complete()
                }

                override fun onOther(msg: String?) {
                    logger.d("install complete: $msg")
                    complete()
                }

                override fun onPendingUseAction() {
                    logger.d("install pending use action")
//                    complete()
                }

                private fun complete() {
                    context.unregisterReceiver(this)
                    it.resume(Unit)
                }
            }, intentFilter)
        }
    }

    companion object {
        private val logger
            get() = Timber.tag(InstallHelper::class.java.simpleName)
    }
}

private abstract class InstallReceiver: BroadcastReceiver() {
    companion object {
        val action = InstallReceiver::class.qualifiedName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (action == intent.action) {
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val activityIntent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                        } else {
                            intent.getParcelableExtra(Intent.EXTRA_INTENT)
                        }
                    context.startActivity(activityIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    onPendingUseAction()
                }

                PackageInstaller.STATUS_SUCCESS -> {
                    onSuccess()
                }

                else -> {
                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    onOther(msg)
                }
            }
        }
    }

    abstract fun onSuccess()

    abstract fun onOther(msg: String?)

    abstract fun onPendingUseAction()
}
