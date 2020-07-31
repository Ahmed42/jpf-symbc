package gov.nasa.jpf.symbc.numeric;

public enum NullIndicator {
	NULL("is NULL") { public NullIndicator not() { return NOTNULL; } },
	NOTNULL("is NOT NULL") { public NullIndicator not() { return NULL; } };
	
	private String str;
	
	NullIndicator(String str) {
		this.str = str;
	}
	
	public String toString() { return str; }
	
	public abstract NullIndicator not();
}
