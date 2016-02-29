package scout.sbrfcurrencychanger.entities;

import org.objectsqlite.Annotations.SqlColumn;
import org.objectsqlite.Annotations.SqlPrimaryKey;
import org.objectsqlite.Annotations.SqlTable;

import java.util.Date;

/**
 * Курс валюты
 */
@SqlTable
public class CurrencyRate {

	/**
	 * Идентификатор
	 */
	@SqlPrimaryKey(autoIncrement = true)
	private Integer mId;

	/**
	 * Валюта
	 */
	@SqlColumn
	private Currency mCurrency;

	/**
	 * Дата
	 */
	@SqlColumn
	private Date mDate;

	/**
	 * Курс покупки
	 */
	@SqlColumn
	private float mPurchasingRate;

	/**
	 * Курс продажи
	 */
	@SqlColumn
	private float mSellingRate;

	/**
	 * Конструктор
	 */
	protected CurrencyRate() {
	}

	/**
	 * Конструктор
	 * @param currency Валюта
	 * @param date Дата
	 * @param sellingRate Курс продажи
	 * @param purchasingRate Курс покупки
	 */
	public CurrencyRate(Currency currency, Date date, float purchasingRate, float sellingRate) {
		this();
		mCurrency = currency;
		mDate = date;
		mPurchasingRate = purchasingRate;
		mSellingRate = sellingRate;
	}

	/**
	 * Валюта
	 */
	public Currency getCurrency() {
		return mCurrency;
	}

	/**
	 * Дата
	 */
	public Date getDate() {
		return mDate;
	}

	/**
	 * Курс продажи
	 */
	public float getSellingRate() {
		return mSellingRate;
	}

	/**
	 * Курс покупки
	 */
	public float getPurchasingRate() {
		return mPurchasingRate;
	}

}
