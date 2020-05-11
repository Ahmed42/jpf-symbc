package gov.nasa.jpf.symbc;

import java.util.Vector;

import gov.nasa.jpf.util.Pair;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class SymbolicPathSummary {
	
	private PathCondition condition;
	private PathCondition heapCondition;
	//private Expression returnTransformation;
	//private Vector<Pair<String, Expression>> staticFieldsTransformations;
	//private Vector<Pair<String, Expression>> instanceFieldsTransformations;
	
	private PathCondition transformations;
	
	public SymbolicPathSummary(PathCondition condition,
			PathCondition heapCondition,
			PathCondition transformations) {
		this.condition = condition;
		this.heapCondition = heapCondition;
		this.transformations = transformations;
	}
	
	public PathCondition getCondition() { return condition; }
	
	public PathCondition getHeapCondition() { return heapCondition; }
	
	public PathCondition getTransformations() { return transformations; }
	
}