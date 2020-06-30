package gov.nasa.jpf.symbc;

import java.util.Map;

import gov.nasa.jpf.symbc.numeric.ConstraintExpressionVisitor;

public interface ParsableConstraint {
	public ParsableConstraint makeCopy();
	
	public ParsableConstraint and();
	
	public ParsableConstraint getTail();
	
	public ParsableConstraint last();
	
	public void setAnd(ParsableConstraint t);
	
	public String stringPC();
	
	public void getVarVals(Map<String,Object> varsVals);
	
	public void accept(ConstraintExpressionVisitor visitor);
	
	public String prefix_notation();
	
	public String prefix_notationPC4Z3();
	
}
