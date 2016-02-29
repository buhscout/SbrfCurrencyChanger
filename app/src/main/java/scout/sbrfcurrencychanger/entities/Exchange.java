package scout.sbrfcurrencychanger.entities;

import org.objectsqlite.Annotations.SqlColumn;
import org.objectsqlite.Annotations.SqlPrimaryKey;
import org.objectsqlite.Annotations.SqlTable;

import java.util.Date;

/**
 * Операция обмена
 */
@SqlTable
public class Exchange {

	/**
	 * Конструктор
	 */
	protected Exchange() {
	}

	/**
	 * Конструктор
	 * @param date Дата
	 * @param source Счёт источник
	 * @param destination Счёт назначения
	 * @param value Значение
	 * @param rate Курс обмена
	 */
	public Exchange(Date date, Account source, Account destination, float value, float rate) {
		this();
		mDate = date;
		mSource = source;
		mDestination = destination;
		mValue = value;
		mRate = rate;
	}

	/**
	 * Идентификатор
	 */
	@SqlPrimaryKey(autoIncrement = true)
	private int mId;

	/**
	 * Дата
	 */
	@SqlColumn
	private Date mDate;

	/**
	 * Счёт источник
	 */
	@SqlColumn
	private Account mSource;

	/**
	 * Счёт назначения
	 */
	@SqlColumn
	private Account mDestination;

	/**
	 * Значение
	 */
	@SqlColumn
	private float mValue;

	/**
	 * Курс обмена
	 */
	@SqlColumn
	private float mRate;

	/**
	 * Идентификатор
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Дата
	 */
	public Date getDate() {
		return mDate;
	}

	/**
	 * Счёт источник
	 */
	public Account getSource() {
		return mSource;
	}

	/**
	 * Счёт назначения
	 */
	public Account getDestination() {
		return mDestination;
	}

	/**
	 * Значение
	 */
	public float getValue() {
		return mValue;
	}

	/**
	 * Курс обмена
	 */
	public float getRate() {
		return mRate;
	}

}
