package gov.nasa.jpf.symbc.numeric;

import java.util.Map;

import gov.nasa.jpf.symbc.ParsableConstraint;

public class NullConstraint implements ParsableConstraint {
	private ParsableConstraint and;
	private Expression referenceExpression;
	private NullIndicator nullIndicator;
	
	
	public NullConstraint(Expression refExpression, NullIndicator isNull) {
		this.referenceExpression = refExpression;
		this.nullIndicator = isNull;
	}
	
	public NullConstraint(NullConstraint constraint) {
		this.referenceExpression = constraint.referenceExpression;
		this.nullIndicator = constraint.nullIndicator;
	}
	
	@Override
	public ParsableConstraint makeCopy() {
		NullConstraint copy = new NullConstraint(this);
		if(and != null) {
			copy.and = and.makeCopy();
		}
		return copy;
	}
	
	public NullIndicator getNullIndicator() { return nullIndicator; }
	
	public Expression getExpression() { return referenceExpression; }

	@Override
	public ParsableConstraint and() {
		return and;
	}

	@Override
	public ParsableConstraint getTail() {
		return and;
	}

	@Override
	public ParsableConstraint last() {
		ParsableConstraint c= this;
	      while(c.and() != null) {
	          c = c.and();
	      }
	      return c;
	}

	@Override
	public void setAnd(ParsableConstraint t) {
		and = t;
	}

	@Override
	public String stringPC() {
		return referenceExpression  + " " + nullIndicator 
				+ ((and == null) ? "" : " && " + and.stringPC());
	}
	
	@Override
	public String toString() {
		return stringPC();
	}

	@Override
	public void getVarVals(Map<String, Object> varsVals) {
		if(referenceExpression != null) {
			referenceExpression.getVarsVals(varsVals);
		}
		if(and != null) {
			and.getVarVals(varsVals);
		}
		
	}

	@Override
	public void accept(ConstraintExpressionVisitor visitor) {
		// TODO Auto-generated method stub
	}

	@Override
	public String prefix_notation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String prefix_notationPC4Z3() {
		// TODO Auto-generated method stub
		return null;
	}

}
