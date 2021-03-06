package gov.nasa.jpf.symbc;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.Expression;


public class SymField {
	private Expression symVar; // No other common ancestor to terminal symbolic values (consts and vars) exists
	private ElementInfo fieldOwner;
	protected FieldInfo fieldInfo;
	protected ThreadInfo currentThread;
	protected int owningObjRef;
	
	/*private PCChoiceGenerator pcChoiceGen;
	private int pcChoiceNo;
	
	private HeapChoiceGenerator heapChoiceGen;
	private int heapChoiceNo;*/
	
	private int pcOffset;
	private int pcChoiceNo;
	
	private PathCondition heapPC;
	
	public SymField(Expression sVar, int objRef, ElementInfo owningObj, FieldInfo fInfo, 
			int offset, int choiceNo, PathCondition hPC,
			ThreadInfo curThread) {
		symVar = sVar;
		//objectRef = objRef;
		owningObjRef = objRef;
		fieldOwner = owningObj;
		fieldInfo = fInfo;
		currentThread = curThread;
		
		pcOffset = offset;
		pcChoiceNo = choiceNo;
		heapPC = hPC;
		

	}
	
	public ElementInfo getFieldOwner() { return fieldOwner; } 
	
	public int getPCOffset() { return pcOffset; }
	
	public int getPCChoiceNo() { return pcChoiceNo; }
	
	public PathCondition getHeapPC() { return heapPC; }
	
	
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
		
		// TODO check for static 
		// TODO Test objRef for MJIEnv.NULL (I don't think we need to do this one)
		
		//int objRef = fieldOwner.getObjectRef();
		ElementInfo owner = currentThread.getElementInfo(owningObjRef);
		
		
		Object value = owner.getFieldAttr(fieldInfo);
		
		// TODO deal with null values by adding the appropriate constraint
		
		/*if(value == null && fieldInfo.isIntField()) {
			value = actualOwner.getIntField(fieldInfo);
		}
		
		System.out.println("In getFieldVal():"
				+ "\n\tvalue: " + value
				+ "\n\towner: " + actualOwner
				+ "\n\tref: " + actualOwner.getObjectRef());*/
		
		
		//System.out.println("Field value: " + value);
		
		// Value might not be symbolic
		if(value == null) {
			if(fieldInfo.isBooleanField()) {
				value = owner.getBooleanField(fieldInfo);
			} else if(fieldInfo.isByteField()) {
				value = owner.getByteField(fieldInfo);
			} else if(fieldInfo.isCharField()) {
				value = owner.getCharField(fieldInfo);
			} else if(fieldInfo.isShortField()) {
				value = owner.getShortField(fieldInfo);
			} else if(fieldInfo.isIntField()) {
				value = owner.getIntField(fieldInfo);
			} else if(fieldInfo.isLongField()) {
				value = owner.getLongField(fieldInfo);
			} else if(fieldInfo.isFloatField()) {
				value = owner.getFloatField(fieldInfo);
			} else if(fieldInfo.isDoubleField()) {
				value = owner.getDoubleField(fieldInfo);
			} else if(fieldInfo.isReference()) {
				// TODO handle reference non-string data 
				int ref = owner.getReferenceField(fieldInfo);
				ElementInfo fieldElement = currentThread.getVM().getHeap().get(ref);
				
				// TODO Change to specific string types (String and StringBuffer)
				if(fieldInfo.getType().contains("String")) {
					
					if(fieldElement != null) {
						value = fieldElement.asString();
					}
				} else {
					value = ref;
				}
				
			}
		}
		// TODO might need to add arrays
		
		
		return value;
		
	}
	
	public String getFieldName() { 
		if(isStatic()) {
			return fieldInfo.getClassInfo().getName() + "." +  fieldInfo.getName();
		} else {
			// Does not work in the case a reference has been initialized to an existing object
			// (No symbol for it was created)
			return symVar.stringPC();
		}
		 
	}
	
	public String toString() { 
		return "Field: " + getFieldName() + ", Value: " + getFieldVal(); 
	}
	
	public boolean isStatic() { return fieldInfo.isStatic(); }
	
	/*public String toStringChoiceGen() {
		return "\n\tPC Gen Offset: " + pcChoiceGen.getOffset() + ", PC Choice No: " + pcChoiceNo 
				+ "\n\tHeap Choice Gen: " + heapChoiceGen + ", Heap Choice No: " + heapChoiceNo;
	}*/
	
	/*public boolean equalsChoiceGens(SymField s) {
		return pcChoiceGen == s.pcChoiceGen && heapChoiceGen == s.heapChoiceGen 
				&& pcChoiceNo == s.pcChoiceNo && heapChoiceNo == s.heapChoiceNo;
	}*/
}