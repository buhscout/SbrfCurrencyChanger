package scout.sbrfcurrencychanger.entities;

import org.objectsqlite.Annotations.SqlColumn;
import org.objectsqlite.Annotations.SqlPrimaryKey;
import org.objectsqlite.Annotations.SqlTable;

import java.io.Serializable;

import scout.sbrfcurrencychanger.enums.AccountTypes;

/**
 * Счёт
 */
@SqlTable
public class Account implements Serializable {

	/**
	 * Конструктор
	 */
	protected Account() {
	}

	/**
     * Конструктор
     *
     * @param href Ссылка
     * @param currency Валюта
     * @param accountNumber Номер счёта
     * @param name Наименование
     */
    public Account(Currency currency, String accountNumber, String name, String href) {
		this();
        mHref = href;
        mCurrency = currency;
        mAccountNumber = accountNumber;
        mName = name;
		if(mHref != null) {
			int index = mHref.lastIndexOf('=');
			mCode = index != -1 ? mHref.substring(index + 1) : null;
		}
		if(mCode == null) {
			throw new IllegalArgumentException("Код счёта не найден");
		}
    }

	/**
	 * Ссылка
	 */
	@SqlPrimaryKey
	private String mCode;

	/**
     * Ссылка
     */
	@SqlColumn
    private String mHref;

    /**
     * Наименование
     */
	@SqlColumn
    private String mName;

    /**
     * Номер счёта
     */
	@SqlColumn
    private String mAccountNumber;

    /**
     * Валюта
     */
	@SqlColumn
    private Currency mCurrency;

    /**
     * Баланс
     */
	@SqlColumn
    private float mBalance;

	/**
     * Признак главного счёта
     */
	@SqlColumn
	private boolean mIsMain;

	/**
	 * Признак активного счёта
	 */
	@SqlColumn
	private boolean mIsActive;

	/**
	 * Признак активного счёта
	 */
	public boolean isActive() {
		return mIsActive;
	}

	/**
	 * Признак активного счёта
	 */
	public void setActive(boolean isActive) {
		mIsActive = isActive;
	}

	/**
	 * Признак главного счёта
	 */
	public boolean isMain() {
		return mIsMain;
	}

	/**
	 * Признак главного счёта
	 */
	public void setMain(boolean isMain) {
		mIsMain = isMain;
	}

	/**
	 * Ссылка
	 */
	public String getHref() {
		return mHref;
	}

    /**
     * Баланс
     */
    public float getBalance() {
        return mBalance;
    }

    /**
     * Баланс
     */
    public void setBalance(float mBalance) {
        this.mBalance = mBalance;
    }

    /**
     * Наименование
     */
    public String getName() {
        return mName;
    }

    /**
     * Номер счёта
     */
    public String getAccountNumber() {
        return mAccountNumber;
    }

    /**
     * Идентификатор
     */
    public String getCode() {
        return mCode;
    }

    /**
     * Валюта
     */
    public Currency getCurrency() {
        return mCurrency;
    }

	/**
	 * Тип счёта
	 */
	public AccountTypes getAccountType() {
		if(mHref == null) {
			return AccountTypes.Undefined;
		}
		if(mHref.contains("/cards/")){
			return AccountTypes.Card;
		}
		if(mHref.contains("/accounts/")){
			return AccountTypes.Account;
		}
		return AccountTypes.Undefined;
	}

	@Override
	public boolean equals(Object o) {
		return !(o == null || !(o instanceof Account)) && (this == o || ((Account) o).getCode().equals(getCode()));
	}

	@Override
	public int hashCode() {
		return getCode().hashCode();
	}

}
