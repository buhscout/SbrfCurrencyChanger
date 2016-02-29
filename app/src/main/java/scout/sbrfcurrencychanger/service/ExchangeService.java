package scout.sbrfcurrencychanger.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import org.htmlcleaner.XPatherException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scout.sbrfcurrencychanger.NotificationsManager;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.dao.WebDao;
import scout.sbrfcurrencychanger.entities.Account;
import scout.sbrfcurrencychanger.entities.Currency;
import scout.sbrfcurrencychanger.entities.CurrencyRate;
import scout.sbrfcurrencychanger.entities.Exchange;

public class ExchangeService extends Service {

    private static double mChangeCurrencyBorder = 0.005;
    private static final int INTERVAL = 43200000; // 12 часов
    private static final int FIRST_RUN = 43200000; // 60 seconds
    private static final int REQUEST_CODE = 11223344;
    //private static String TAG = "scout.sbrfcurrencychanger.service";

    @Override
    public void onDestroy() {
        //Log.e(TAG, TAG + " Service destroyed");
        //Intent intent = new Intent(this, ExchangeReceiver.class);
        //AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //alarmManager.cancel(PendingIntent.getBroadcast(this, REQUEST_CODE, intent, 0));
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.e(TAG, TAG + "Service started");
        final Context context = this;
        Intent intentReciv = new Intent(context, ExchangeReceiver.class);
        if (PendingIntent.getBroadcast(context, REQUEST_CODE, intentReciv, PendingIntent.FLAG_NO_CREATE) == null) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intentReciv, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + FIRST_RUN, INTERVAL, pendingIntent);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Repository.initialize(context);
                Exchange(context);
                stopSelf();
            }}).start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Действия
     */
    enum Action {
        None,
        Buy,
        Sell
    }

    /**
     * Анализ курсов
     * @param currency Валюта
     */
    private Action CurrencyAnalysis(Currency currency) {
        Calendar mDateFrom = Repository.getToday();
        mDateFrom.add(Calendar.DAY_OF_YEAR, -30);
        List<CurrencyRate> rates = Repository.getCurrenciesRates(currency, mDateFrom.getTime(), Repository.getToday().getTime());
        if (rates.size() > 2) {
            float sumPRates = 0;
            float sumSRates = 0;
            int count = 0;
            for (CurrencyRate rate : rates) {
                sumPRates += rate.getPurchasingRate();
                sumSRates += rate.getSellingRate();
                count++;
            }
            float avgPRate = sumPRates / count;
            float avgSRate = sumSRates / count;

            float extremumValue = 0;
            CurrencyRate lastRate = rates.get(rates.size() - 1);
            if (lastRate.getPurchasingRate() > avgPRate) {
                for (int i = rates.size() - 1; i >= 0; i--) {
                    float prate = rates.get(i).getPurchasingRate();
                    if (prate <= avgPRate) {
                        break;
                    }
                    if (prate > extremumValue) {
                        extremumValue = prate;
                    }
                }
                if (extremumValue - extremumValue * mChangeCurrencyBorder > lastRate.getPurchasingRate() && lastRate.getPurchasingRate() > avgPRate + (avgPRate * mChangeCurrencyBorder)) {
                    return Action.Sell;
                } else {
                    return Action.None;
                }
            } else if (lastRate.getSellingRate() < avgSRate) {
                for (int i = rates.size() - 1; i >= 0; i--) {
                    float srate = rates.get(i).getSellingRate();
                    if (srate >= avgSRate) {
                        break;
                    }
                    if (srate < extremumValue) {
                        extremumValue = srate;
                    }
                }
                if (extremumValue + (extremumValue * mChangeCurrencyBorder) < lastRate.getSellingRate() && lastRate.getSellingRate() < avgSRate - (avgSRate * mChangeCurrencyBorder)) {
                    return Action.Buy;
                } else {
                    return Action.None;
                }
            }
        }
        return Action.None;
    }

    /**
     * Обмен
     */
    public void Exchange(Context context) {
        Account mainAccount = Repository.getMainAccount();
        if (mainAccount == null) {
            return;
        }
        List<Account> buyAccounts = new ArrayList<>();
        Boolean ratesRefreshed = false;
        try {
            Repository.refreshAccounts();
        } catch (WebDao.SbrfException | XPatherException e) {
            e.printStackTrace();
        }
        for (Account account : Repository.getActiveAccounts()) {
            if (account.isMain()) {
                continue;
            }
            switch (CurrencyAnalysis(account.getCurrency())) {
                case Buy: {
                    //recommend = "Покупать";
                    buyAccounts.add(account);
                    break;
                }
                case Sell:
                    //recommend = "Продавать";
                    if (account.getBalance() > 0) {
                        try {
                            if (!ratesRefreshed) {
                                Repository.refreshCurrenciesRates();
                                ratesRefreshed = true;
                            }
                            Exchange buyExchange = Repository.getLastBuyExchange(account.getCurrency());
                            CurrencyRate currentRate;
                            currentRate = Repository.getCurrencyRate(account.getCurrency());
                            if (currentRate != null && (buyExchange == null || buyExchange.getRate() < currentRate.getPurchasingRate())) {
                                if(new WebDao(context).changeCurrency(account, mainAccount, account.getBalance(), currentRate.getPurchasingRate())) {
                                    Exchange exchange = Repository.saveExchange(account, mainAccount, account.getBalance(), currentRate.getPurchasingRate());
                                    NotificationsManager.ChangeNotify(context, exchange);
                                }
                            }
                        } catch (WebDao.SbrfException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    //NotificationsManager.PushNotify(context, "Обменник", account.getCurrency().getCode() + ": Держать", 2);
                    //recommend = "Держать";
                    break;
            }
        }
        if (!buyAccounts.isEmpty()) {
            HashMap<Account, Float> accountsRatio = new HashMap<>();
            try {
                if (!ratesRefreshed) {
                    Repository.refreshCurrenciesRates();
                }
                if (buyAccounts.size() > 1) {
                    Calendar mDateFrom = Repository.getToday();
                    mDateFrom.add(Calendar.DAY_OF_YEAR, -30);
                    float sumSRates = 0;
                    CurrencyRate[] rates = Repository.getCurrenciesRates(mDateFrom.getTime(), Repository.getToday().getTime());
                    for (Account item : buyAccounts) {
                        int count = 0;
                        for (CurrencyRate rate : rates) {
                            if (rate.getCurrency().equals(item.getCurrency())) {
                                sumSRates += rate.getSellingRate();
                                count++;
                            }
                        }
                        float avgSRate = 1;
                        if (rates.length > 2) {
                            avgSRate = sumSRates / count;
                        }
                        CurrencyRate rate = Repository.getCurrencyRate(item.getCurrency());
                        if (rate == null) {
                            continue;
                        }
                        if (mainAccount.getBalance() >= rate.getSellingRate()) {
                            accountsRatio.put(item, rate.getSellingRate() / avgSRate);
                        }
                    }
                    float summator = 0;
                    for (Map.Entry<Account, Float> unit : accountsRatio.entrySet()) {
                        summator += unit.getValue();
                    }
                    for (Map.Entry<Account, Float> unit : accountsRatio.entrySet()) {
                        unit.setValue(unit.getValue() / summator);
                    }
                } else {
                    accountsRatio.put(buyAccounts.get(0), (float) 1);
                }
                for (Map.Entry<Account, Float> unit : accountsRatio.entrySet()) {
                    CurrencyRate rate = Repository.getCurrencyRate(unit.getKey().getCurrency());
                    if (rate == null) {
                        continue;
                    }
                    if(new WebDao(context).changeCurrency(mainAccount, unit.getKey(), unit.getValue(), rate.getSellingRate())) {
                        Exchange exchange = Repository.saveExchange(mainAccount, unit.getKey(), unit.getValue(), rate.getSellingRate());
                        NotificationsManager.ChangeNotify(context, exchange);
                    }
                }
            } catch (WebDao.SbrfException e) {
                e.printStackTrace();
            }
        }
    }

}
