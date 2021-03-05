package util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ArrayMap<K, V> extends AbstractMap<K, V> {

	private static final class ArrayMapEntry<K, V> implements Map.Entry<K, V> {
		private K key;
		private V value;

		public ArrayMapEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V oldValue = value;

			this.value = value;

			return oldValue;
		}

		@Override
		public String toString() {
			return String.join("", key.toString(), "=", value.toString());
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry<?, ?>)) {
				return false;
			}

			Map.Entry<?, ?> e = (Map.Entry<?, ?>)o;

			return Objects.equals(e.getKey(), key)
			    && Objects.equals(e.getValue(), value);
		}

		@Override
		public int hashCode() {
			int keyHash = key == null ? 0 : key.hashCode();
			int valueHash = value == null ? 0 : value.hashCode();

			return keyHash ^ valueHash;
		}
	}

	private ArrayList<Map.Entry<K, V>> map;
	private Set<Map.Entry<K, V>> entrySet;

	public ArrayMap() {
		map = new ArrayList<>();
	}

	public ArrayMap(Map<K, V> map) {
		this();

		putAll(map);
	}

	@Override
	public V put(K key, V value) {
		Map.Entry<K, V> entry = null;

		for (Map.Entry<K, V> e : map) {
			if (Objects.equals(e.getKey(), key)) {
				entry = e;
				break;
			}
		}

		V oldValue = entry == null ? null : entry.getValue();

		if (entry == null) {
			map.add(new ArrayMapEntry<>(key, value));
		} else {
			entry.setValue(value);
		}

		return oldValue;
	}

	@Override
	public boolean containsKey(Object key) {
		for (Map.Entry<K, V> e : map) {
			if (Objects.equals(e.getKey(), key)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public V get(Object key) {
		for (Map.Entry<K, V> e : map) {
			if (Objects.equals(e.getKey(), key)) {
				return e.getValue();
			}
		}

		return null;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		if (entrySet == null) {
			entrySet = new AbstractSet<Map.Entry<K, V>>() {

				@Override
				public Iterator<Map.Entry<K, V>> iterator() {
					return map.iterator();
				}

				@Override
				public int size() {
					return map.size();
				}

			};
		}

		return entrySet;
	}

}