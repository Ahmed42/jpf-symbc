package gov.nasa.jpf.symbc;

import java.util.Vector;

import gov.nasa.jpf.util.Pair;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class SymbolicPathSummary {
	
	private PathCondition pathCondition;
	private PathCondition heapCondition;
	private PathCondition condition;
	//private Expression returnTransformation;
	//private Vector<Pair<String, Expression>> staticFieldsTransformations;
	//private Vector<Pair<String, Expression>> instanceFieldsTransformations;
	
	private PathCondition transformations;
	
	public SymbolicPathSummary(PathCondition pCondition,
			PathCondition hCondition,
			PathCondition transforms) {
		this.pathCondition = pCondition;
		this.heapCondition = hCondition;
		this.transformations = transforms;
		
		
		this.condition = new PathCondition();
		
		this.condition.prependAllConjuncts(pCondition.header.makeCopy());
		this.condition.appendAllConjuncts(hCondition.header.makeCopy());
	}
	
	public PathCondition getCondition() { return condition;  }
	public PathCondition getPathCondition() { return pathCondition; }
	public PathCondition getHeapCondition() { return heapCondition; }
	
	public PathCondition getTransformations() { return transformations; }
	
}