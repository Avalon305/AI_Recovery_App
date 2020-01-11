package com.bdl.airecovery.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.xutils.common.util.LogUtil;

public class SwitchSignalBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction.equals("init_locate")) {
            if (intent.getStringExtra("seat_motor") != null) {
                if (intent.getStringExtra("seat_motor").equals("top_limit")) {
                    LogUtil.e("=====收到广播======top_limit");
                } else if (intent.getStringExtra("seat_motor").equals("bot_limit")) {
                    LogUtil.e("=====收到广播======bot_limit");
                }
            }
        }
    }
}
