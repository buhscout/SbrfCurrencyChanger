package scout.sbrfcurrencychanger.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Scout on 10.05.2016.
 */
public class ExecReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context ctx, final Intent intent) {
        Log.d("ExecReceiver", "SchedulerEventReceiver.onReceive() called");
        Intent eventService = new Intent(ctx, ExchangeService.class);
        ctx.startService(eventService);
    }
}
