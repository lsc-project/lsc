package org.lsc;

import java.util.Map.Entry;

public class MapEntry<T1, T2> implements Entry<T1, T2> {

	T1 key;
	T2 value;
	
	public MapEntry(T1 key, T2 value) {
		this.key = key;
		this.value = value;
	}
	
	public T1 getKey() {
		return key;
	}

	public T2 getValue() {
		return value;
	}

	public T2 setValue(T2 value) {
		this.value = value; 
		return value;
	}

}
