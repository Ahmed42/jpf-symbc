package gov.nasa.jpf.symbc;

import java.util.Vector;

import gov.nasa.jpf.util.Pair;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class SymbolicPathSummary {
	
	private PathCondition condition;
	private Expression returnTransformation;
	private Vector<Pair<String, Expression>> staticFieldsTransformations;
	// TODO add instance fields transformations
	
	public SymbolicPathSummary(PathCondition condition, 
			Expression returnTransformation, 
			Vector<Pair<String, Expression>> staticFieldsTransformations) {
		this.condition = condition;
		this.returnTransformation = returnTransformation;
		this.staticFieldsTransformations = staticFieldsTransformations;
	}
	
	public PathCondition getCondition() { return condition; }
	
	public Expression getReturnTransformation() { return returnTransformation; }
	
	public Vector<Pair<String, Expression>> getSFieldsTransformations() { return staticFieldsTransformations; }
	
}