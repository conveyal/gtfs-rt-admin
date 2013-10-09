package utils;

public class IdValuePair<T> implements Comparable<IdValuePair> {
	
	public String id;
	public T value;
	
	public IdValuePair(String i, T v) {
		id = i;
		value = v;
	}

	@Override
	public int compareTo(IdValuePair arg0) {
		return ((Comparable)value).compareTo((Comparable)arg0.value);
	}
}