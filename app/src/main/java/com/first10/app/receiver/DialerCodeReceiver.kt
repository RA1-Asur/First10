package com.first10.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.first10.app.ui.MainActivity

class DialerCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // Dialing *#*#1010#*#* launches First 10 even if launcher icon is hidden
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(launchIntent)
    }
}
