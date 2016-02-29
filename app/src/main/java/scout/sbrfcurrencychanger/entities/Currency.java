package scout.sbrfcurrencychanger.entities;

import org.objectsqlite.Annotations.SqlColumn;
import org.objectsqlite.Annotations.SqlPrimaryKey;
import org.objectsqlite.Annotations.SqlTable;

import java.io.Serializable;

/**
 * Валюта
 */
@SqlTable(cacheable = true)
public class Currency implements Serializable {

    /**
     * Конструктор
     */
	protected Currency(){
    }

	/**
	 * Конструктор
	 * @param code Код
	 */
	public Currency(String code) {
		if(code == null || code.equals("")){
			throw new IllegalArgumentException("Код не задан");
		}
		mCode = code;
	}

    /**
     * Конструктор
     * @param code Код
	 * @param symbol Символ
     */
    public Currency(String code, String symbol) {
     	this(code);
        mSymbol = symbol;
    }

    /**
     * Код
     */
	@SqlPrimaryKey
    private String mCode;

    /**
     * Символ
     */
	@SqlColumn
    private String mSymbol;

	/**
     * Код
     */
    public String getCode() {
        return mCode;
    }

    /**
     * Символ
     */
    public String getSymbol() {
        return mSymbol;
    }

	@Override
	public boolean equals(Object o) {
		return !(o == null || !(o instanceof Currency)) && (this == o || ((Currency) o).getCode().equals(getCode()));
	}

	@Override
	public int hashCode() {
		return getCode().hashCode();
	}
}
