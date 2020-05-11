package gov.nasa.jpf.symbc;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.RealConstraint;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.MinMax;

public class TransformedSymField extends SymField {
	private Expression symVarOutput; // Output variable name for a transformed field
	
	public TransformedSymField(Expression sVar, ElementInfo owningObj, 
			FieldInfo fInfo, ThreadInfo curThread) {
		super(sVar, owningObj, fInfo, curThread);

		symVarOutput = createOutputSymVar(sVar, fInfo);
	}
	
	
	private Expression createOutputSymVar(Expression sVar, FieldInfo fInfo) {
		String varName = sVar.stringPC() +  "_out";
		Expression outVar = null;
		
		if(fInfo.isBooleanField()) {
			outVar = new SymbolicInteger(varName, 0, 1);
			
		} else if(fInfo.isByteField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinByte(varName), MinMax.getVarMaxByte(varName));
			
		} else if(fInfo.isCharField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinChar(varName), MinMax.getVarMaxChar(varName));
			
		} else if(fInfo.isShortField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinShort(varName), MinMax.getVarMaxChar(varName));
			
		} else if(fInfo.isIntField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinInt(varName), MinMax.getVarMaxInt(varName));
			
		} else if(fInfo.isLongField()) {
			outVar = new SymbolicInteger(varName, MinMax.getVarMinLong(varName), MinMax.getVarMaxLong(varName));
			
		} else if(fInfo.isFloatingPointField() || fInfo.isDoubleField()) {
			outVar = new SymbolicReal(varName, MinMax.getVarMinDouble(varName), MinMax.getVarMaxDouble(varName));
		} else if(fInfo.isReference()) {
			// TODO might need to create a different var for strings
			outVar = new SymbolicInteger(varName);
		}
		// TODO might need to add arrays
		return outVar;
	}
	
	public Constraint getTransformationConstraint() {
		Constraint transformation = null;
		
		if(fieldInfo.isFloatingPointField() || fieldInfo.isDoubleField()) {
			transformation = new RealConstraint((RealExpression) symVarOutput, 
					Comparator.EQ, (RealExpression) getFieldVal());
		} else {
			transformation = new LinearIntegerConstraint((IntegerExpression) symVarOutput, 
					Comparator.EQ, (IntegerExpression) getFieldVal());
		}
		
		return transformation;
	}
}
