package utils;

public class IdValuePair implements Comparable<IdValuePair> {
	
	public String id;
	public String value;
	
	public IdValuePair(String i, String v) {
		id = i;
		value = v;
	}

	@Override
	public int compareTo(IdValuePair arg0) {
		return value.compareTo(arg0.value);
	}
}