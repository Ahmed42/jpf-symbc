package gov.nasa.jpf.symbc.arrays;

import gov.nasa.jpf.symbc.string.StringExpression;

public class StringByteArrayExpression extends ArrayExpression {

	private StringExpression stringExpression;
	
	public StringByteArrayExpression(String name, StringExpression stringExpr) {
		super(name);
		
		stringExpression = stringExpr;
	}
	
	public StringByteArrayExpression(StringByteArrayExpression prev) {
		super(prev);
		
		stringExpression = prev.getStringExpression();
	}
	
	public StringByteArrayExpression makeCopy() {
		StringByteArrayExpression byteArrExpr = new StringByteArrayExpression(this);
		
		return byteArrExpr;
	}

	public StringExpression getStringExpression() {
		return stringExpression;
	}
}
