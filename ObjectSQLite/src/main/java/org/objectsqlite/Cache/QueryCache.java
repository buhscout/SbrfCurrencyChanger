package org.objectsqlite.Cache;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Кэш запросов
 */
public class QueryCache {

	/**
	 * Максимальный размер, байт
	 */
	private static double mMaxSize = 0;

	/**
	 * Запросы
	 */
	private static Map<Class, CacheQuery> mQueries = new HashMap<>();

	/**
	 * Максимальный размер, Мб
	 */
	public static double getMaxSize() {
		return mMaxSize / 1048576;
	}

	/**
	 * Максимальный размер, Мб
	 */
	public static void setMaxSize(double maxSize) {
		mMaxSize = maxSize * 1048576;
	}

	/**
	 * Вставка в кэш
	 * @param type
	 * 		Тип класса
	 * @param query
	 * 		Запрос
	 */
	public static void put(Class type, String query) {
		if (!mQueries.containsKey(type)) {
			mQueries.put(type, new CacheQuery(query));
		}
		if(mMaxSize > 0) {
			adjustCacheSize();
		}
	}

	/**
	 * Вставка в кэш
	 * @param type
	 * 		Тип класса
	 */
	public static String get(Class type) {
		CacheQuery query = mQueries.get(type);
		return query != null ? query.getQuery() : null;
	}

	/**
	 * Подогнать размер кэша к заданному размеру
	 */
	private static void adjustCacheSize() {
		SortedMap<Long, Class> queries = new TreeMap<>();
		double size = 0;
		for(Map.Entry<Class, CacheQuery> typeQuery : mQueries.entrySet()) {
				queries.put(typeQuery.getValue().getLastAccessTime(), typeQuery.getKey());
				size += typeQuery.getValue().getSize();
		}
		for(Map.Entry<Long, Class> query : queries.entrySet()) {
			if(size <= mMaxSize) {
				break;
			}
			size -= mQueries.get(query.getValue()).getSize();
			mQueries.remove(query.getValue());
		}
	}

	/**
	 * Элемент кэша запросов
	 */
	private static class CacheQuery {

		/**
		 * Конструктор
		 * @param query Запрос
		 */
		public CacheQuery(String query) {
			mQuery = query;
			mLastAccessTime = System.currentTimeMillis();
		}

		/**
		 * Запрос
		 */
		private String mQuery;

		/**
		 * Последняя дата использования
		 */
		private Long mLastAccessTime;

		/**
		 * Запрос
		 */
		public String getQuery() {
			mLastAccessTime = System.currentTimeMillis();
			return mQuery;
		}

		/**
		 * Последняя дата использования
		 */
		public Long getLastAccessTime() {
			return mLastAccessTime;
		}

		public int getSize() {
			return mQuery.length() * 2 + 38;
		}

	}

}
