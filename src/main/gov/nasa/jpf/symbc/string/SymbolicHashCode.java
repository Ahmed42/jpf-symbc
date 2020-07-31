package gov.nasa.jpf.symbc.string;

import gov.nasa.jpf.symbc.numeric.SymbolicInteger;

public class SymbolicHashCode extends SymbolicInteger {
	StringExpression stringExpression;
	
	public SymbolicHashCode(String name, StringExpression strExpr) {
		super(name);
		
		stringExpression = strExpr;
	}
	
	public StringExpression getExpression() {
		return stringExpression;
	}

}
