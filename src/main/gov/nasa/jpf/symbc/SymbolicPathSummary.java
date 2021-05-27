package gov.nasa.jpf.symbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class SymbolicPathSummary {
	
	private PathCondition pathCondition;
	private PathCondition heapCondition;
	private PathCondition condition;
	//private Expression returnTransformation;
	//private Vector<Pair<String, Expression>> staticFieldsTransformations;
	//private Vector<Pair<String, Expression>> instanceFieldsTransformations;
	
	private PathCondition transformations;
	
	private List<TransformedSymField> transformedFields;
	
	public SymbolicPathSummary(PathCondition pCondition,
			PathCondition hCondition,
			PathCondition transforms,
			List<TransformedSymField> tFields) {
		this.pathCondition = pCondition;
		this.heapCondition = hCondition;
		this.transformations = transforms;
		this.transformedFields = new ArrayList<>(tFields);
		
		this.condition = new PathCondition();
		
		if(pCondition != null && pCondition.header != null) {
			this.condition.prependAllConjuncts(pCondition.header.makeCopy());
		} /*else {
			this.condition.prependAllConjuncts(new LinearIntegerConstraint(new IntegerConstant(1), Comparator.EQ, new IntegerConstant(1)));
		}*/
		
		// TODO: add proper makeCopy to spc
		if(pCondition != null && pCondition.spc.count() > 0) {
			this.condition.spc = pCondition.spc.make_copy(pCondition);
		}
		
		
		if(hCondition != null && hCondition.header != null) {
			this.condition.prependAllConjuncts(hCondition.header.makeCopy());
		}
		
		
	}
	
	/**
	 *  Add identity constraints to unchanged fields.
	 */
	public void completeTransformations(Collection<TransformedSymField> allFields) {
		List<ParsableConstraint> identityConstraints = new ArrayList<>();
		
		
		for(TransformedSymField symField : allFields) {

			boolean alreadyExists = transformedFields.contains(symField);
			
			if(!alreadyExists) {
				identityConstraints.add(symField.getIdentityConstraint());
			}

		}
		

		for(ParsableConstraint constraint : identityConstraints) {
			transformations.prependAllConjuncts(constraint.makeCopy());
			//transformations.appendAllConjuncts(constraint.makeCopy());
		}
		
	}
	
	public PathCondition getCondition() { return condition;  }
	public PathCondition getPathCondition() { return pathCondition; }
	public PathCondition getHeapCondition() { return heapCondition; }
	
	public PathCondition getTransformations() { return transformations; }
	
	public List<TransformedSymField> getTransformedFields() { return transformedFields; } 
}