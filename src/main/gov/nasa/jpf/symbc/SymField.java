package gov.nasa.jpf.symbc;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.symbc.numeric.Expression;

public class SymField {
	private Expression symVar; // No other common ancestor to symbolic values exists
	private ElementInfo fieldOwner;
	//private int objectRef;
	private FieldInfo fieldInfo;
	private ThreadInfo currentThread;
	
	public SymField(Expression sVar, ElementInfo owningObj, FieldInfo fInfo, ThreadInfo curThread) {
		symVar = sVar;
		//objectRef = objRef;
		fieldOwner = owningObj;
		fieldInfo = fInfo;
		currentThread = curThread;
	}
	
	public Object getFieldVal() {
		/*ElementInfo fieldOwner;
		if(isStatic()) {
			fieldOwner = fieldInfo.getClassInfo().getModifiableStaticElementInfo();
		} else {
			fieldOwner = currentThread.getModifiableElementInfo(objectRef);
		}
		
		if(fieldOwner == null) {
			System.out.println("Owner is null :(");
			return null;
		}*/
		
		//System.out.println(fieldOwner.getFieldAttr(fieldInfo));
		Object value = fieldOwner.getFieldAttr(fieldInfo);
		
		if(value == null) {
			if(fieldInfo.isBooleanField()) {
				value = fieldOwner.getBooleanField(fieldInfo);
			} else if(fieldInfo.isByteField()) {
				value = fieldOwner.getByteField(fieldInfo);
			} else if(fieldInfo.isCharField()) {
				value = fieldOwner.getCharField(fieldInfo);
			} else if(fieldInfo.isShortField()) {
				value = fieldOwner.getShortField(fieldInfo);
			} else if(fieldInfo.isIntField()) {
				value = fieldOwner.getIntField(fieldInfo);
			} else if(fieldInfo.isLongField()) {
				value = fieldOwner.getLongField(fieldInfo);
			} else if(fieldInfo.isFloatingPointField()) {
				value = fieldOwner.getFloatField(fieldInfo);
			} else if(fieldInfo.isDoubleField()) {
				value = fieldOwner.getDoubleField(fieldInfo);
			} else if(fieldInfo.isReference()) {
				value = fieldOwner.getReferenceField(fieldInfo);
			}
		}
		// TODO might need to add arrays
		
		
		return value;
		
	}
	
	public String getFieldName() { 
		if(isStatic()) {
			return fieldInfo.getClassInfo().getName() + "." +  fieldInfo.getName();
		} else {
			return symVar.stringPC();
		}
		 
	}
	
	public String toString() { 
		return "Field: " + getFieldName() + ", Value: " + getFieldVal(); 
	}
	
	public boolean isStatic() { return fieldInfo.isStatic(); }
	
}