package gov.nasa.jpf.symbc;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ChoiceGenerator;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstraint;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.MinMax;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;

public class TransformedSymField extends SymField {
	private Expression symVarOutput; // Output variable name for a transformed field
	private Constraint identityConstraint;
	
	public TransformedSymField(Expression sVar, int objRef, ElementInfo owningObj, FieldInfo fInfo, 
			int offset, int choiceNo, PathCondition heapPC,
			ThreadInfo curThread) {
		super(sVar, objRef, owningObj, fInfo, offset, choiceNo, heapPC, curThread);

		createOutputSymVar(sVar, fInfo);
	}
	
	 
	
	// TODO could be refactored
	private void createOutputSymVar(Expression sVar, FieldInfo fInfo) {
		String varName = sVar.stringPC() +  "_out";
		Expression outVar = null;
		
		if(fInfo.isBooleanField()) {
			outVar = new SymbolicInteger(varName, 0, 1);
			identityConstraint = new LinearIntegerConstraint((IntegerExpression)outVar, Comparator.EQ, (IntegerExpression)sVar);
			
		} else if(fInfo.isByteField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinByte(varName), MinMax.getVarMaxByte(varName));
			identityConstraint = new LinearIntegerConstraint((IntegerExpression)outVar, Comparator.EQ, (IntegerExpression)sVar);
			
		} else if(fInfo.isCharField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinChar(varName), MinMax.getVarMaxChar(varName));
			identityConstraint = new LinearIntegerConstraint((IntegerExpression)outVar, Comparator.EQ, (IntegerExpression)sVar);
			
		} else if(fInfo.isShortField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinShort(varName), MinMax.getVarMaxChar(varName));
			identityConstraint = new LinearIntegerConstraint((IntegerExpression)outVar, Comparator.EQ, (IntegerExpression)sVar);
			
		} else if(fInfo.isIntField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinInt(varName), MinMax.getVarMaxInt(varName));
			identityConstraint = new LinearIntegerConstraint((IntegerExpression)outVar, Comparator.EQ, (IntegerExpression)sVar);
			
		} else if(fInfo.isLongField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinLong(varName), MinMax.getVarMaxLong(varName));
			identityConstraint = new LinearIntegerConstraint((IntegerExpression)outVar, Comparator.EQ, (IntegerExpression)sVar);

		} else if(fInfo.isFloatingPointField() || fInfo.isDoubleField()) {
			outVar = new SymbolicReal(varName, MinMax.getVarMinDouble(varName), MinMax.getVarMaxDouble(varName));
			identityConstraint = new RealConstraint((RealExpression)outVar, Comparator.EQ, (RealExpression)sVar);

		} else if(fInfo.isReference()) {
			// TODO might need to create a different var for strings
			
			if(fInfo.getType().equals("java.lang.String")) {
				outVar = new StringSymbolic(varName);
			} else {
				outVar = new SymbolicInteger(varName);
				identityConstraint = new LinearIntegerConstraint((IntegerExpression)outVar, Comparator.EQ, (IntegerExpression)sVar);
			}
			
			
		} else {
			assert(false);
		}
		// TODO might need to add arrays
		symVarOutput = outVar;
	}
	
	public Constraint getIdentityConstraint() { return identityConstraint; }
	
	public Object getTransformationConstraint() {
		Object transformation = null;
		Object rightSide = getFieldVal();
		
		
		// TODO deal with null values by adding the appropriate constraint
		if(fieldInfo.isFloatingPointField() || fieldInfo.isDoubleField()) {
			RealExpression rightSideExpr;
			
			if(rightSide instanceof RealExpression) {
				rightSideExpr = (RealExpression) rightSide;
			} else {
				rightSideExpr = new RealConstant((double)rightSide);
			}
			
			
			transformation = new RealConstraint((RealExpression) symVarOutput, Comparator.EQ, rightSideExpr);
			
			/*transformation = new RealConstraint((RealExpression) symVarOutput, 
					Comparator.EQ, (RealExpression) getFieldVal());*/
		} else if(fieldInfo.getType().equals("java.lang.String")) {
				StringExpression rightSideExpr = null;
				
				if(rightSide instanceof StringExpression) {
					rightSideExpr = (StringExpression) rightSide;
				} else if(rightSide instanceof String) {
					rightSideExpr = new StringConstant((String)rightSide);
				} 
				// TODO might need to check for StringBuilder and maybe Regex as well
				
				assert(rightSideExpr != null);
				
				transformation = 
						new StringConstraint((StringExpression) symVarOutput, StringComparator.EQUALS, rightSideExpr);
		} else {
				IntegerExpression rightSideExpr = null;
				
				if(rightSide instanceof IntegerExpression) {
					rightSideExpr = (IntegerExpression) rightSide;
				} else if(rightSide instanceof Long){
					rightSideExpr = new IntegerConstant((Long)rightSide);
				} else if(rightSide instanceof Integer) {
					rightSideExpr = new IntegerConstant((Integer)rightSide);
				} else if(rightSide instanceof Boolean) {
					// TODO create boolean expression types
					if((Boolean)rightSide) {
						rightSideExpr = new IntegerConstant(1);
					} else {
						rightSideExpr = new IntegerConstant(0);
					}
					
				}
				
				assert(rightSideExpr != null);
				
				transformation = new LinearIntegerConstraint((IntegerExpression) symVarOutput, Comparator.EQ, rightSideExpr);
		}
			
			/*transformation = new LinearIntegerConstraint((IntegerExpression) symVarOutput, 
					Comparator.EQ, (IntegerExpression) getFieldVal());*/
		
		
		return transformation;
	}

	public String getSymOutVarName() { return symVarOutput.stringPC(); }
	
	/**
	 * This is to simplify set and comparison operations when adding identity constraints for unchanged fields.
	 */
	@Override
	public boolean equals(Object o) {
		if(o instanceof TransformedSymField) {
			return ((TransformedSymField) o).getSymOutVarName().equals(getSymOutVarName());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return getSymOutVarName().hashCode();
	}
}
