package org.objectsqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Debug;
import android.util.Log;
import android.util.Pair;

import org.objectsqlite.Annotations.SqlColumn;
import org.objectsqlite.Annotations.SqlPrimaryKey;
import org.objectsqlite.Annotations.SqlTable;
import org.objectsqlite.Cache.ObjectCache;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DAO для доступа к SQLite
 */
public class ObjectSQLite extends SQLiteOpenHelper {

	/**
	 * Версия БД
	 */
	private static final int DB_VERSION = 1;

	/**
	 * Метка запроса в логах
	 */
	private static final String QueryTag = "SQL_Query";

	/**
	 * Список существующих таблиц
	 */
	private List<String> mExistsTables;

	/**
	 * Конструктор
	 * @param context Контекст
	 */
	public ObjectSQLite(Context context) {
		this(context, context.getPackageName());
	}

	/**
	 * Конструктор
	 * @param context Контекст
	 * @param dbName Имя базы данных
	 */
	public ObjectSQLite(Context context, String dbName) {
		super(context, dbName, null, DB_VERSION);
	}

	/**
	 * Список существующих таблиц
	 */
	public List<String> getExistsTables() {
		if(mExistsTables != null) {
			return mExistsTables;
		}
		mExistsTables = new ArrayList<>();
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor;
		String queryText = "SELECT name FROM sqlite_master WHERE type = 'table';";
		if (Debug.isDebuggerConnected()) {
			Log.v(QueryTag, queryText);
		}
		try {
			db.beginTransactionNonExclusive();
			cursor = db.rawQuery(queryText, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		for(cursor.moveToFirst(); cursor.moveToNext();) {
			mExistsTables.add(cursor.getString(0));
		}
		return mExistsTables;
	}

	/**
	 * Создаёт таблицы для заданного типа, если они не существуют
	 * @param type Тип для создания таблиц
	 */
	public <T> void createTablesIfNotExists(Class<T> type) {
		QueryBuilder<T> qManager = new QueryBuilder<>(type);
		List<Pair<String, String>> queries = qManager.getCreate();
		SQLiteDatabase db = getWritableDatabase();
		List<String> existsTables = getExistsTables();
		List<Pair<String, String>> exists = new ArrayList<>();
		for (Pair<String, String> table : queries) {
			if (existsTables.contains(table.first)) {
				exists.add(table);
			}
		}
		queries.removeAll(exists);
		if (queries.isEmpty()) {
			return;
		}
		try {
			db.beginTransaction();
			for (Pair<String, String> table : queries) {
				if (Debug.isDebuggerConnected()) {
					Log.v(QueryTag, table.second);
				}
				db.execSQL(table.second);
				mExistsTables.add(table.first);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Преобразование строки таблицы в экземпляр класса
	 * @param type Тип
	 * @param cursor Курсор таблицы
	 */
	private <T> T getEntity(Class<T> type, String parentAlias, Cursor cursor, int deepLevel) {
		SqlTable sqlTableAnnotation = type.getAnnotation(SqlTable.class);
		boolean isCacheable = sqlTableAnnotation != null && sqlTableAnnotation.cacheable();
		if (isCacheable) {
			Class fieldType = null;
			String fieldName = null;
			String parentField = null;
			if(deepLevel == 0) {
				Field pkField = ReflectionHelper.getKeyField(type);
				if(pkField != null) {
					fieldName = pkField.getName();
					fieldType = pkField.getType();
				}
			} else {
				if(parentAlias != null) {
					int index = parentAlias.lastIndexOf("_");
					if(index != -1) {
						fieldName = parentAlias.substring(index + 1);
						parentField = parentAlias.substring(0, index);
					} else {
						fieldName = parentAlias;
					}
				}
				fieldType = type;
			}
			if (fieldName != null) {
				Integer keyColumnIndex = null;
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					String columnName = cursor.getColumnName(i);
					int index = columnName.indexOf("_");
					if (!columnName.substring(0, index).equals(String.valueOf(deepLevel == 0 ? 0 : deepLevel - 1))) {
						continue;
					}
					int lastIndex = columnName.lastIndexOf('_');
					if ((!fieldName.equals(columnName.substring(lastIndex + 1)) && !fieldName.equals(columnName.substring(lastIndex + 2)))
							|| (lastIndex > index && parentField != null && !parentField.isEmpty() && !parentField.equals(columnName.substring(index + 1, lastIndex)))) {
						continue;
					}
					keyColumnIndex = i;
					break;
				}
				if (keyColumnIndex != null) {
					Object keyValue = getCursorColumnValue(cursor, keyColumnIndex, fieldType);
					Object cacheEntity = ObjectCache.get(type, keyValue);
					if (cacheEntity != null) {
						return (T) cacheEntity;
					}
					if(deepLevel > 0) {
						cacheEntity = get(type, keyValue);
						return cacheEntity != null ? (T) cacheEntity : null;
					}
				}
			}
		}
		T entity;
		Constructor<T> constructor;
		try {
			constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			entity = constructor.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		for (String columnName : cursor.getColumnNames()) {
			int index = columnName.indexOf("_");
			if (!columnName.substring(0, index).equals(String.valueOf(deepLevel))) {
				continue;
			}
			int lastIndex = columnName.lastIndexOf('_');
			if (parentAlias != null && !parentAlias.isEmpty() && !parentAlias.equals(columnName.substring(index + 1, lastIndex))) {
				continue;
			}
			Field field;
			try {
				field = type.getDeclaredField(columnName.substring(lastIndex + 1));
			} catch (NoSuchFieldException e) {
				continue;
			}
			field.setAccessible(true);
			index = cursor.getColumnIndex(columnName);
			try {
				Class fieldType = field.getType();
				SqlTable annotation = field.getType().getAnnotation(SqlTable.class);
				if (annotation != null) {
					String fName = ReflectionHelper.getAnnotatedFieldName(field);
					if(parentAlias != null && !fName.isEmpty()) {
						fName = "_" + fName;
					}
					Object value = getEntity(field.getType(), (parentAlias == null ? "" : parentAlias) + fName, cursor, deepLevel + 1);
					field.set(entity, value);
					continue;
				}
				switch (cursor.getType(index)) {
					case Cursor.FIELD_TYPE_STRING:
						field.set(entity, cursor.getString(index));
						break;
					case Cursor.FIELD_TYPE_FLOAT:
						if (fieldType == Double.class || fieldType == Double.TYPE) {
							field.setDouble(entity, cursor.getDouble(index));
						} else {
							field.setFloat(entity, cursor.getFloat(index));
						}
						break;
					case Cursor.FIELD_TYPE_INTEGER:
						if (fieldType == Date.class) {
							field.set(entity, new Date(cursor.getLong(index)));
						} else if (fieldType == Short.class || fieldType == Short.TYPE) {
							field.setShort(entity, cursor.getShort(index));
						} else if (fieldType == Long.class || fieldType == Long.TYPE) {
							field.setLong(entity, cursor.getLong(index));
						} else if (fieldType == Boolean.class || fieldType == Boolean.TYPE) {
							field.setBoolean(entity, cursor.getInt(index) == 1);
						} else {
							field.set(entity, cursor.getInt(index));
						}
						break;
					case Cursor.FIELD_TYPE_BLOB:
						field.set(entity, cursor.getBlob(index));
						break;
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		if (isCacheable) {
			ObjectCache.put(entity);
		}
		return entity;
	}

	private Object getCursorColumnValue(Cursor cursor, int index, Class fieldType) {
		Object keyValue = null;
		switch (cursor.getType(index)) {
			case Cursor.FIELD_TYPE_STRING:
				keyValue = cursor.getString(index);
				break;
			case Cursor.FIELD_TYPE_FLOAT:
				if (fieldType == Double.class || fieldType == Double.TYPE) {
					keyValue = cursor.getDouble(index);
				} else {
					keyValue = cursor.getFloat(index);
				}
				break;
			case Cursor.FIELD_TYPE_INTEGER:
				if (fieldType == Date.class) {
					keyValue = new Date(cursor.getLong(index));
				} else if (fieldType == Short.class || fieldType == Short.TYPE) {
					keyValue = cursor.getShort(index);
				} else if (fieldType == Long.class || fieldType == Long.TYPE) {
					keyValue = cursor.getLong(index);
				} else if (fieldType == Boolean.class || fieldType == Boolean.TYPE) {
					keyValue = cursor.getInt(index) == 1;
				} else {
					keyValue = cursor.getInt(index);
				}
				break;
			case Cursor.FIELD_TYPE_BLOB:
				keyValue = cursor.getBlob(index);
				break;
		}
		return keyValue;
	}

	/**
	 * Получение экземпляра класса по ключевому полю из БД
	 * @param type Тип требуемого экземпляра
	 * @param keyValue Значение ключа
	 * @param <T> Тип требуемого экземпляра
	 */
	public <T> T get(Class<T> type, Object keyValue) {
		if(type == null) {
			throw new IllegalArgumentException("Тип требуемого экземпляра класса не задан");
		}
		if(keyValue == null) {
			throw new IllegalArgumentException("Значение ключа не задано");
		}
		SqlTable annotation = type.getAnnotation(SqlTable.class);
		if(annotation == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не помечен аннотацией @SqlTable", type.getName()));
		}
		boolean isCacheable = annotation.cacheable();
		Object value;
		if(isCacheable) {
			value = ObjectCache.get(type, keyValue);
			if (value != null) {
				return (T) value;
			}
		}
		T[] values = getData(type, new QueryBuilder<>(type).getSelectByKey(keyValue));
		return values.length > 0 ? values[0] : null;
	}

	/**
	 * Получение всех экземпляров класса из БД
	 * @param type Тип требуемых экземпляров
	 * @param <T> Тип требуемых экземпляров
	 */
	public <T> T[] get(Class<T> type) {
		return getData(type, new QueryBuilder<>(type).getSelect());
	}

	/**
	 * Получить данные
	 * @param type Тип данных
	 * @param whereConditions Условия WHERE
	 * @param recordsCount Количество строк
	 * @param orderByFields Условия сортировки
	 * @param <T> Тип данных
	 */
	public <T> T[] getWhere(Class<T> type, int recordsCount, String orderByFields, String... whereConditions) {
		return getData(type, new QueryBuilder<>(type).getSelectWhere(recordsCount, orderByFields, whereConditions));
	}

	private <T> T[] getData(Class<T> type, String queryText) {
		if(type == null) {
			throw new IllegalArgumentException("Тип требуемого экземпляра класса не задан");
		}
		if(Debug.isDebuggerConnected()) {
			Log.v(QueryTag, queryText);
		}
		Cursor cursor;
		SQLiteDatabase db = getReadableDatabase();
		try {
			db.beginTransactionNonExclusive();
			cursor = db.rawQuery(queryText, null);
			db.setTransactionSuccessful();
		} catch (SQLiteException e) {
			db.endTransaction();
			if(e.getMessage() != null && e.getMessage().startsWith("no such table")) {
				createTablesIfNotExists(type);
				return getData(type, queryText);
			}
			throw e;
		}
		db.endTransaction();
		T[] data = (T[]) Array.newInstance(type, cursor.getCount());
		if(cursor.moveToFirst()) {
			for (int i = 0; i < cursor.getCount(); i++, cursor.moveToNext()) {
				data[i] = getEntity(type, null, cursor, 0);
			}
		}
		return data;
	}

	/**
	 * Порверка на существование экземпляра класса в БД
	 * @param entity Экземпляр класса
	 * @param <T> Тип экземпляра класса
	 */
	public <T> boolean isExists(T entity) {
		if(entity == null) {
			throw new IllegalArgumentException("Экземпляр класса не задан");
		}
		QueryBuilder<T> queryBuilder = new QueryBuilder<>((Class<T>) entity.getClass());
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = null;
		Field keyField = ReflectionHelper.getKeyField(entity.getClass());
		try {
			if(keyField != null) {
				keyField.setAccessible(true);
				if(keyField.get(entity) == null) {
					return false;
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		String queryText = queryBuilder.getExists(entity);
		if(Debug.isDebuggerConnected()) {
			Log.v(QueryTag, queryText);
		}
		try {
			db.beginTransactionNonExclusive();
			cursor = db.rawQuery(queryText, null);
			db.setTransactionSuccessful();
		} catch (SQLiteException e) {
			if(e.getMessage().startsWith("no such table")) {
				return false;
			}
			throw e;
		} finally {
			db.endTransaction();
		}
		return cursor.moveToFirst() && cursor.getCount() > 0;
	}

	/**
	 * Сохранение экземпляра класса в БД
	 * @param entity Экземпляр класса
	 * @param <T> Тип экземпляра класса
	 */
	public <T> T save(T entity) {
		if(entity == null) {
			throw new IllegalArgumentException("Экземпляр класса не задан");
		}
		SqlTable annotation = entity.getClass().getAnnotation(SqlTable.class);
		if(annotation == null) {
			throw new IllegalArgumentException(String.format("Класс '%s' не помечен аннотацией @SqlTable", entity.getClass().getName()));
		}
		boolean isCacheable = annotation.cacheable();
		Object value = null;
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.beginTransaction();
			entity = saveEntity(entity, true);
			db.setTransactionSuccessful();
		} catch (SQLiteException e) {
			db.endTransaction();
			if(e.getMessage().startsWith("no such table")) {
				createTablesIfNotExists(entity.getClass());
				entity = save(entity);
				if(isCacheable) {
					ObjectCache.put(entity);
				}
				return entity;
			}
			throw e;
		}
		db.endTransaction();
		if(isCacheable) {
			ObjectCache.put(entity);
		}
		return entity;
	}

	/**
	 * Сохранение экземпляра класса в БД
	 * @param entity Экземпляр класса
	 * @param updateIfExists Обновление (UPDATE), если уже существует
	 * @param <T> Тип экземпляра класса
	 */
	private <T> T saveEntity(T entity, boolean updateIfExists) {
		if(entity == null) {
			return null;
		}
		boolean isExists = isExists(entity);
		if(isExists && !updateIfExists) {
			return entity;
		}
		Field keyField = null;
		Integer incrementKeyValue = -1;
		QueryBuilder<T> queryBuilder = new QueryBuilder<>((Class<T>) entity.getClass());
		for(Map.Entry<String, Field> entry : queryBuilder.getFields().entrySet()) {
			SqlColumn colAnnotation = entry.getValue().getAnnotation(SqlColumn.class);
			SqlPrimaryKey pkAnnotation = entry.getValue().getAnnotation(SqlPrimaryKey.class);
			if(pkAnnotation == null && colAnnotation != null && !colAnnotation.cascadeInsert() && !colAnnotation.cascadeUpdate()) {
				continue;
			}
			if(pkAnnotation != null) {
				keyField = entry.getValue();
				if(pkAnnotation.autoIncrement()) {
					keyField.setAccessible(true);
					try {
						incrementKeyValue = (Integer)keyField.get(entity);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
			Class fieldType = entry.getValue().getType();
			if (!fieldType.isAnnotationPresent(SqlTable.class)) {
				continue;
			}
			entry.getValue().setAccessible(true);
			Object value;
			try {
				value = entry.getValue().get(entity);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			}
			if (value == null) {
				continue;
			}
			SqlColumn sqlColumn = entry.getValue().getAnnotation(SqlColumn.class);
			saveEntity(value, sqlColumn.cascadeUpdate());
		}
		SQLiteDatabase db = getWritableDatabase();
		String query = isExists ? queryBuilder.getUpdate(entity) : queryBuilder.getInsert(entity);
		if(Debug.isDebuggerConnected()) {
			Log.v(QueryTag, query);
		}
		db.execSQL(query);
		if (incrementKeyValue == null) {
			query = "SELECT LAST_INSERT_ROWID();";
			if(Debug.isDebuggerConnected()) {
				Log.v(QueryTag, query);
			}
			Cursor cursor = db.rawQuery(query, null);
			cursor.moveToFirst();
			try {
				keyField.set(entity, cursor.getInt(0));
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return entity;
	}

    /**
	 * Удаление экземпляра класса в БД
	 * @param entity Экземпляр класса
	 * @param <T> Тип экземпляра класса
	 */
    public <T> void deleteEntity(T entity) {
        if(entity == null) {
            throw new IllegalArgumentException("Экземпляр класса не задан");
        }
		Class<T> type = (Class<T>)entity.getClass();
        SqlTable annotation = entity.getClass().getAnnotation(SqlTable.class);
        if(annotation == null) {
            throw new IllegalArgumentException(String.format("Класс '%s' не помечен аннотацией @SqlTable", entity.getClass().getName()));
        }
        Field keyField = ReflectionHelper.getKeyField(entity.getClass());
        Object keyValue = null;
        try {
            if(keyField != null) {
                keyField.setAccessible(true);
                keyValue = keyField.get(entity);
                if(keyValue == null) {
                    return;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
		boolean isCacheable = annotation.cacheable();
		if(isCacheable) {
			ObjectCache.remove(type, keyValue);
		}
		SQLiteDatabase db = getWritableDatabase();
		QueryBuilder<T> queryBuilder = new QueryBuilder<>(type);
		String query = queryBuilder.getDelete(entity);
		if(Debug.isDebuggerConnected()) {
			Log.v(QueryTag, query);
		}
		db.execSQL(query);
	}

	/**
	 * Создание БД
	 * @param sqLiteDatabase БД
	 */
	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
	}

	/**
	 * Обновление БД
	 * @param sqLiteDatabase БД
	 * @param i Текущая версия
	 * @param i1 Новая версия
	 */
	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
	}

}
