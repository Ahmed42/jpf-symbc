package gov.nasa.jpf.symbc;

import java.util.Vector;

import gov.nasa.jpf.util.Pair;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class SymbolicPathSummary {
	
	private PathCondition condition;
	private PathCondition heapCondition;
	private Expression returnTransformation;
	private Vector<Pair<String, Expression>> staticFieldsTransformations;
	private Vector<Pair<String, Expression>> instanceFieldsTransformations;
	
	public SymbolicPathSummary(PathCondition condition,
			PathCondition heapCondition,
			Expression returnTransformation, 
			Vector<Pair<String, Expression>> staticFieldsTransformations,
			Vector<Pair<String, Expression>> instanceFieldsTransformations) {
		this.condition = condition;
		this.heapCondition = heapCondition;
		this.returnTransformation = returnTransformation;
		this.staticFieldsTransformations = staticFieldsTransformations;
		this.instanceFieldsTransformations = instanceFieldsTransformations;
	}
	
	public PathCondition getCondition() { return condition; }
	
	public PathCondition getHeapCondition() { return heapCondition; }
	
	public Expression getReturnTransformation() { return returnTransformation; }
	
	public Vector<Pair<String, Expression>> getSFieldsTransformations() { return staticFieldsTransformations; }
	
}