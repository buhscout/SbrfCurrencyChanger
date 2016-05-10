package scout.sbrfcurrencychanger.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            Intent intent1 = new Intent(context, ExchangeService.class);
            context.startService(intent1);
        }
    }

}