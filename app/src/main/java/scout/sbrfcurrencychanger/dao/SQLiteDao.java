package scout.sbrfcurrencychanger.dao;

import android.content.Context;

import org.objectsqlite.ObjectSQLite;

/**
 * DAO для доступа к базе данных
 */
public class SQLiteDao {

	/**
	 * SQL DAO
	 */
	private static ObjectSQLite mDao;

	/**
	 * Контекст
	 */
	private static Context mContext;

	/**
	 * SQL DAO
	 */
	private static ObjectSQLite getDao() {
		if(mDao == null) {
			mDao = new ObjectSQLite(mContext);
		}
		return mDao;
	}

	/**
	 * Инициализация
	 * @param context Контекст
	 */
	public static void initialize(Context context) {
		mContext = context;
	}

	/**
	 * Получить данные
	 * @param type Тип данных
	 * @param <T> Тип данных
	 * @param id Значение идентификатора
	 */
	public static <T> T get(Class<T> type, Object id) {
		return getDao().get(type, id);
	}

	/**
	 * Получить данные
	 * @param type Тип данных
	 * @param <T> Тип данных
	 */
	public static <T> T[] get(Class<T> type) {
		if(mContext == null) {
			return null;
		}
		return getDao().get(type);
	}

	/**
	 * Получить данные
	 * @param type Тип данных
	 * @param whereConditions условия WHERE
	 * @param <T> Тип данных
	 */
	public static <T> T[] getWhere(Class<T> type, String orderByFields, String... whereConditions) {
		return getDao().getWhere(type, 0, orderByFields, whereConditions);
	}

	/**
	 * Получить данные
	 * @param type Тип данных
	 * @param whereConditions условия WHERE
	 * @param recordsCount Количество строк
	 * @param orderByFields Условия сортировки
	 * @param <T> Тип данных
	 */
	public static <T> T[] getWhere(Class<T> type, int recordsCount, String orderByFields, String... whereConditions) {
		return getDao().getWhere(type, recordsCount, orderByFields, whereConditions);
	}

	/**
	 * Сохранение элемента
	 * @param entity Элемент для сохранения
	 */
	public static <T> T save(T entity) {
		return getDao().save(entity);
	}

	/**
	 * Удаление элемента
	 * @param entity Элемент для удаления
	 */
	public static <T> void delete(T entity) {
		getDao().deleteEntity(entity);
	}

	/**
	 * Создаёт таблицы для заданного типа, если они не существуют
	 * @param type Тип для создания таблиц
	 */
	public static void createTableIfNotFound(Class type) {
		getDao().createTablesIfNotExists(type);
	}

}
