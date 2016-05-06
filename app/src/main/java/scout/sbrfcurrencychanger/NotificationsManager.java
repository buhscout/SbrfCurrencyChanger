package scout.sbrfcurrencychanger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import scout.sbrfcurrencychanger.entities.Exchange;
import scout.sbrfcurrencychanger.view.MainActivity;

/**
 * Уведомления
 */
public class NotificationsManager {

    private static int notifyId;

    /**
     * Уведомление обмена
     * @param context Контекст
     * @param exchange Экземпляк класса обмена
     */
    public static void ChangeNotify(Context context, Exchange exchange) {
        PushNotify(context, "Обмен валюты", exchange.getSource().getCurrency().getCode() + " -> " + exchange.getDestination().getCurrency().getCode()
                + ": " + Repository.getDecimalFormat().format(exchange.getValue()) + exchange.getSource().getCurrency().getSymbol()
                + " по курсу " + Repository.getDecimalFormat().format(exchange.getRate()));
    }

    public static void PushNotify(Context context, String header, String message) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_ALL) // звук, вибро и диодный индикатор выставляются по умолчанию
                .setSmallIcon(R.drawable.ic_launcher) // большая картинка
                //.setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.hungrycat))
                //.setWhen(System.currentTimeMillis()) //отображаемое время уведомления
                .setAutoCancel(true)  //уведомление закроется по клику на него
                .setContentTitle(header)  //заголовок уведомления
                //.setTicker(message) //текст, который отобразится вверху статус-бара при создании уведомления
                .setContentText(message); // Текст уведомленимя

        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifyId++, notification);
    }
}
