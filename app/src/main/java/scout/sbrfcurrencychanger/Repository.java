package scout.sbrfcurrencychanger;

import android.content.Context;
import android.os.StrictMode;

import org.htmlcleaner.XPatherException;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import scout.sbrfcurrencychanger.dao.SQLiteDao;
import scout.sbrfcurrencychanger.dao.WebDao;
import scout.sbrfcurrencychanger.entities.Account;
import scout.sbrfcurrencychanger.entities.Currency;
import scout.sbrfcurrencychanger.entities.CurrencyRate;
import scout.sbrfcurrencychanger.entities.Exchange;

/**
 * Хранилище
 */
public class Repository {

	/**
	 * Контекст
	 */
	private static Context mContext;

	/**
	 * Менеджер куков
	 */
	private static CookieManager mCookieManager;

	/**
	 * Класс доступа к данным
	 */
	private static WebDao sWebDao;

    /**
     * Список валют
     */
    private static Currency[] mCurrencies;

	/**
	 * Слушатели сохранения счёта
	 */
	private static ArrayList<OnChangeListener<Account>> mOnSaveAccountListeners;

    /**
     * Текущие курсы валют
     */
    private static ArrayList<CurrencyRate> mCurrenciesRates;

    /**
     * Обновление текущих курсов валют
     */
    public static void refreshCurrenciesRates() throws WebDao.SbrfException {
        mCurrenciesRates = getSbrfDao().getCurrentCurrenciesRates();
    }

    /**
     * Текущие курсы валют
     */
    public static ArrayList<CurrencyRate> getCurrenciesRates() throws WebDao.SbrfException
    {
        if(mCurrenciesRates == null) {
            refreshCurrenciesRates();
        }
        return mCurrenciesRates;
    }

    /**
     * Текущий курс валюты
     */
    public static CurrencyRate getCurrencyRate(Currency currency) throws WebDao.SbrfException
    {
        for(CurrencyRate rate : getCurrenciesRates()) {
            if(rate.getCurrency().equals(currency)) {
                return rate;
            }
        }
        return null;
    }

	/**
	 * Добавить слушателя за сохранением счёта
	 * @param onSaveAccountListener Слушатель за сохранением счёта
	 */
	public static void addOnSaveAccountListener(OnChangeListener<Account> onSaveAccountListener) {
		if(onSaveAccountListener == null) {
			return;
		}
		if(mOnSaveAccountListeners == null) {
			mOnSaveAccountListeners = new ArrayList<>();
		}
		mOnSaveAccountListeners.add(onSaveAccountListener);
	}

	/**
	 * Удалить слушателя за сохранением счёта
	 * @param onSaveAccountListener Слушатель за сохранением счёта
	 */
	public static void removeOnSaveAccountListener(OnChangeListener<Account> onSaveAccountListener) {
		if(onSaveAccountListener == null || mOnSaveAccountListeners == null) {
			return;
		}
		mOnSaveAccountListeners.remove(onSaveAccountListener);
	}

    /**
     * Список валют
     */
    public static Currency[] getCurrencies() {
		if (mCurrencies != null) {
			return mCurrencies;
		}
		mCurrencies = SQLiteDao.get(Currency.class);
		if (mCurrencies != null && mCurrencies.length != 0) {
			return mCurrencies;
		}
		mCurrencies = new Currency[3];
		Currency currency = new Currency("RUB", "руб.");
		mCurrencies[0] = SQLiteDao.save(currency);
		currency = new Currency("EUR", "€");
		mCurrencies[1] = SQLiteDao.save(currency);
		currency = new Currency("USD", "$");
		mCurrencies[2] = SQLiteDao.save(currency);
		return mCurrencies;
	}

	/**
	 * Список валют
	 */
	private static Account[] mAccounts;

	/**
	 * Список счетов
	 */
	public static Account[] getAccounts() {
        if (mAccounts == null) {
            mAccounts = SQLiteDao.get(Account.class);
        }
        return mAccounts;
    }

	/**
	 * Список активных счетов
	 */
	public static Account[] getActiveAccounts() {
		List<Account> accountsList = new ArrayList<>();
		for(Account account : getAccounts()) {
			if(account.isActive()) {
				accountsList.add(account);
			}
		}
		Account[] accounts = new Account[accountsList.size()];
		accountsList.toArray(accounts);
		return accounts;
	}

    /**
     * Основной счёт
     */
    public static Account getMainAccount(){
        for(Account account : getAccounts()) {
            if(account.isMain() && account.isActive()) {
                return account;
            }
        }
		for(Account account : getAccounts()) {
			if(account.isActive() && Objects.equals(account.getCurrency().getCode(), "RUB")) {
				account.setMain(true);
				saveAccount(account);
				return account;
			}
		}
        return null;
    }

	/**
	 * Сохранить счёт
	 */
	public static Account[] saveAccount(Account account) {
		account = SQLiteDao.save(account);
		if(mOnSaveAccountListeners != null) {
			for(OnChangeListener<Account> onChangeListener : mOnSaveAccountListeners) {
				onChangeListener.OnChange(account);
			}
		}
		Account[] accounts = getAccounts();
		for (int i = 0; i < accounts.length; i++) {
			if(accounts[i].equals(account)) {
				accounts[i] = account;
				return  accounts;
			}
		}
		mAccounts = new Account[accounts.length + 1];
		System.arraycopy(accounts, 0, mAccounts, 0, accounts.length);
		mAccounts[mAccounts.length - 1] = account;
		return mAccounts;
	}

	/**
	 * Обновить данные по счетам
	 */
	public static void refreshAccounts() throws WebDao.SbrfException, XPatherException {
		for (Account account : getAccounts()) {
			for (Account sbrfAccount : getSbrfDao().getAccounts()) {
                float balance = sbrfAccount.getBalance();
                if(Objects.equals(account.getCurrency().getCode(), "RUB")) {
                    balance -= 10;
                }
				if(account.equals(sbrfAccount) && account.getBalance() != balance) {
					account.setBalance(balance);
					saveAccount(account);
				}
			}
		}
	}

	/**
	 * Текущая дата
	 */
	public static Calendar getToday() {
		Calendar today = Calendar.getInstance();
		setStartOfDay(today);
		return today;
	}

	/**
	 * Задать начало дня
	 * @param calendar Календарь
	 */
	private static void setStartOfDay(Calendar calendar) {
		calendar.set(Calendar.AM_PM, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR, 0);
	}

	/**
	 * История курсов валют
	 * @param currency Валюта
	 * @param dateFrom Начало периода
	 * @param dateTo Окончание периода
	 */
	public static List<CurrencyRate> getCurrenciesRates(Currency currency, Date dateFrom, Date dateTo) {
		ArrayList<CurrencyRate> rates = new ArrayList<>();
		for(CurrencyRate rate : getCurrenciesRates(dateFrom, dateTo)) {
			if(rate.getCurrency().equals(currency)) {
				rates.add(rate);
			}
		}
		return rates;
	}

	/**
	 * История курсов валют
	 * @param dateFrom Начало периода
	 * @param dateTo Окончание периода
	 */
	public static CurrencyRate[] getCurrenciesRates(Date dateFrom, Date dateTo) {
		CurrencyRate[] currenciesRates = SQLiteDao.getWhere(CurrencyRate.class,
				"Date",
				"Date >= " + dateFrom.getTime(),
				"Date <= " + dateTo.getTime());
		Date lastDate = dateTo;
		for(CurrencyRate rate : currenciesRates) {
			//SQLiteDao.delete(rate);

			if(rate.getDate().before(lastDate)) {
				lastDate = rate.getDate();
			}
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(lastDate);
		setStartOfDay(calendar);
		if(calendar.getTime().after(dateFrom)) {
			CurrencyRate[] rates = getSbrfDao().getCurrenciesRates(dateFrom, lastDate);
			if(rates == null) {
				return currenciesRates;
			}
			for(CurrencyRate rate : rates) {
				SQLiteDao.save(rate);
			}
			currenciesRates = SQLiteDao.getWhere(CurrencyRate.class,
					"Date",
					"Date >= " + dateFrom.getTime(),
					"Date <= " + dateTo.getTime());
		}
		return currenciesRates;
	}

	/**
	 * История обмена
	 * @param dateFrom Начало периода
	 * @param dateTo Окончание периода
	 */
	public static Exchange[] getExchanges(Date dateFrom, Date dateTo) {
		return SQLiteDao.getWhere(Exchange.class,
                "Date DESC",
				"Date >= " + dateFrom.getTime(),
				"Date <= " + dateTo.getTime());
	}

    /**
     * Получить последнюю запись о покупке
     * @param currency Валюта
     */
    public static Exchange getLastBuyExchange(Currency currency) {
        Exchange[] exchanges = SQLiteDao.getWhere(Exchange.class,
                1,
                "Date DESC",
                "Destination == '" + currency.getCode() + "'");
        if(exchanges.length > 0) {
            return exchanges[0];
        }
        return null;
    }

	/**
	 * Сохранить обмен в истории
	 * @param srcAccount Счёт источник
	 * @param dstAccount Счёт назначения
	 * @param value Значение
	 * @param rate Курс обмена
	 */
	public static Exchange saveExchange(Account srcAccount, Account dstAccount, float value, float rate) {
		Exchange exchange = new Exchange(Calendar.getInstance().getTime(), srcAccount, dstAccount, value, rate);
		return SQLiteDao.save(exchange);
	}

	/**
	 * Контекст
	 */
	/*public static Context getMainContext(){
		return mMainContext;
	}*/

	/**
	 * Менеджер куков
	 */
	/* public static CookieManager getCookieManager(){
		return mCookieManager;
	} */

	/**
	 * Класс доступа к данным сбербанка
	 */
	public static WebDao getSbrfDao(){
		if(mCookieManager == null) {
			mCookieManager = new CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(mCookieManager);
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		return sWebDao != null ? sWebDao : (sWebDao = new WebDao(mContext));
	}

	/**
	 * Инициализация
	 * @param context Контекст
	 */
	public static void initialize(Context context) {
		mContext = context;
        SQLiteDao.initialize(context);
	}

	/**
	 * Форматтер чисел с плавающей запятой
	 */
	public static DecimalFormat getDecimalFormat() {
		DecimalFormat decimalFormat = new DecimalFormat();
		decimalFormat.setMaximumFractionDigits(2);
		decimalFormat.setMinimumFractionDigits(0);
		decimalFormat.setDecimalSeparatorAlwaysShown(false);
		return decimalFormat;
	}

}
