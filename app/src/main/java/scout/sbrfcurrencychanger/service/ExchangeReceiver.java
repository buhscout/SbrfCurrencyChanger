package scout.sbrfcurrencychanger.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExchangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Intent service = new Intent(context, ExchangeService.class);
        context.startService(service);
    }

}