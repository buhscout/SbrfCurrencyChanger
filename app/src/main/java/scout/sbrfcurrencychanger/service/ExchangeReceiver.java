package scout.sbrfcurrencychanger.service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import scout.sbrfcurrencychanger.NotificationsManager;

public class ExchangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        //NotificationsManager.PushNotify(context, "Проверка", "Работает", 999);
        new Thread(new Runnable() {
            @Override
            public void run() {
                context.startService(new Intent(context, ExchangeService.class));
            }
        }).start();
    }

}