package scout.sbrfcurrencychanger.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    AlarmReceiver mAlarmReceiver = new AlarmReceiver();

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            mAlarmReceiver.setAlarm(context);
        }
    }

}