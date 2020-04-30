package gov.nasa.jpf.symbc;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;

import gov.nasa.jpf.symbc.numeric.Expression;

public class TransformedSymField {
	private Expression symVar; // No other common ancestor to symbolic values exists
	private ElementInfo owningObj;
	private FieldInfo fieldInfo;
	
	public TransformedSymField(Expression sVar, ElementInfo obj, FieldInfo fInfo) {
		symVar = sVar;
		owningObj = obj;
		fieldInfo = fInfo;
	}
	
	public Object getFieldVal() { return owningObj.getFieldAttr(fieldInfo); }
	
	public String getFieldName() { return symVar.stringPC(); }
	
}