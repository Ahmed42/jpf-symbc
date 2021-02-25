package gov.nasa.jpf.symbc;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ClassInfo;
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
import gov.nasa.jpf.symbc.numeric.NullIndicator;
import gov.nasa.jpf.symbc.numeric.RealConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;

public class TransformedSymField extends SymField {
	private Expression symVarOutput; // Output variable name for a transformed field
	private ParsableConstraint identityConstraint;
	
	// This is for TransformedSymFields that are assigned locally created objects.
	//private List<TransformedSymField> fields;
	
	public static int localsCounter;
	
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
			
			if(fInfo.getType().equals("java.lang.String") || sVar instanceof StringSymbolic) {
				outVar = new StringSymbolic(varName);
				identityConstraint = new StringConstraint((StringSymbolic)outVar, StringComparator.EQUALS, (StringExpression)sVar);
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
	
	public ParsableConstraint getIdentityConstraint() { return identityConstraint; }
	
	public void getTransformationConstraint(PathCondition transformations, Map<String, String> obj2Name,
			Map<String, String> fieldName2ObjName, Map<String, List<Expression>> objName2Fields) {
		//Object transformation = null;
		Object rightSide = getFieldVal();
		
		
		// TODO deal with null values by adding the appropriate constraint
		if(fieldInfo.isFloatingPointField() || fieldInfo.isDoubleField()) {
			RealExpression rightSideExpr;
			
			if(rightSide instanceof RealExpression) {
				rightSideExpr = (RealExpression) rightSide;
			} else {
				rightSideExpr = new RealConstant((double)rightSide);
			}
			
			
			transformations._addDet(Comparator.EQ, (RealExpression) symVarOutput, rightSideExpr);
			//transformation = new RealConstraint((RealExpression) symVarOutput, Comparator.EQ, rightSideExpr);
			
			/*transformation = new RealConstraint((RealExpression) symVarOutput, 
					Comparator.EQ, (RealExpression) getFieldVal());*/
			// fieldInfo.getType should be enough. Why are we check symVarOutput class?
		} else if(fieldInfo.getType().equals("java.lang.String") || symVarOutput instanceof StringExpression) {
				StringExpression rightSideExpr = null;
				
				if(rightSide instanceof StringExpression) {
					rightSideExpr = (StringExpression) rightSide;
				} else if(rightSide instanceof String) {
					rightSideExpr = new StringConstant((String)rightSide);
				} 
				// TODO might need to check for StringBuilder and maybe Regex as well
				
				//assert(rightSideExpr != null);
				
				transformations.spc._addDet(StringComparator.EQUALS, rightSideExpr, (StringExpression) symVarOutput);
				//transformation = new StringConstraint((StringExpression) symVarOutput, StringComparator.EQUALS, rightSideExpr);
		} else if(fieldInfo.isBooleanField()  || fieldInfo.isByteField()  || fieldInfo.isCharField()  
				|| fieldInfo.isShortField()  || fieldInfo.isIntField()  || fieldInfo.isLongField()){
				IntegerExpression rightSideExpr = null;
				
				// TODO need to add cases for char, byte, and short.
				if(rightSide instanceof IntegerExpression) {
					rightSideExpr = (IntegerExpression) rightSide;
				} else if(rightSide instanceof Long){
					rightSideExpr = new IntegerConstant((Long)rightSide);
				} else if(rightSide instanceof Integer) {
					rightSideExpr = new IntegerConstant((Integer)rightSide);
				} else if(rightSide instanceof Boolean) {
					// TODO create boolean expression types (I don't think we necessarily need those)
					if((Boolean)rightSide) {
						rightSideExpr = new IntegerConstant(1);
					} else {
						rightSideExpr = new IntegerConstant(0);
					}
					
				}
				
				assert(rightSideExpr != null);
				
				transformations._addDet(Comparator.EQ, (IntegerExpression) symVarOutput, rightSideExpr);
				//transformation = new LinearIntegerConstraint((IntegerExpression) symVarOutput, Comparator.EQ, rightSideExpr);
		} else if(fieldInfo.isReference()) {
				
				// object is symbolic
				if(rightSide instanceof SymbolicInteger) {
					transformations._addDet(Comparator.EQ, (IntegerExpression) symVarOutput, (SymbolicInteger) rightSide);
					//transformation = new LinearIntegerConstraint((IntegerExpression) symVarOutput, Comparator.EQ, (SymbolicInteger) rightSide);
				} else { // concrete
					//ElementInfo fieldElement = (ElementInfo) rightSide;
					getConcreteObjectFieldsTransformations(currentThread, ((SymbolicInteger) symVarOutput).getName(), 
							(Integer) rightSide, transformations, obj2Name, fieldName2ObjName, objName2Fields);
					
					// Temporary. For debugging purposes. Call getLocalObjectFieldsTransformations instead
					//SymbolicInteger rightSideExpr = new SymbolicInteger(fieldElement.toString());
					//transformation = new LinearIntegerConstraint((IntegerExpression) symVarOutput, Comparator.EQ, rightSideExpr);
				}
		}
			
			/*transformation = new LinearIntegerConstraint((IntegerExpression) symVarOutput, 
					Comparator.EQ, (IntegerExpression) getFieldVal());*/
		

	}
	
	/** Given an object reference and symbolic name, returns the object and its fields transformations.
	 *  This is to be used with objects instantiated locally.
	 *  
	 * @param currentThread	Current thread object.
	 * @param fieldOrRetName	Name of the transformed field or 'RET' to denote the returned value.
	 * @param objRef		Reference value of the object. Reference is assumed to be not null.
	 * @param transformations	Conjunction of constraints describing the transformations of the object and its fields.
	 * @param obj2Name		Maps local objects names, which fiefieldName2ObjNamelds transformation where already added, to the name initially chosen,
	 * 						so as to avoid repeating transformations constraints.
	 * @param fieldName2ObjName	Maps fields to the assigned local object. We use this in conjunction with obj2RefFields to construct the correspondence constraints.
	 * @param obj2RefFields	Maps local objects, which fields transformation where already added, to their fields. 
	 * 						We use this to add the correspondence between local objects
	 * 						and their fields across different versions before construction of VC.
	 * 
	 * Example:
	 * For objSymName="obj.field", transformations after return:
	 * 
	 * obj.field = newObj10 /\ newObj10.field1 = 12 /\ newObj10.field2 = "Initial String" /\ newObj10.field3 = newObj13
	 * /\ newObj13.field1 = 10 /\ newObj13.field2 = someGivenParam ... etc
	 * 
	 * The method continues to recursively add constraints until a null reference is encountered.
	 * 
	 * TODO: perhaps add a maximum depth
	 * 
	 */
	public static void getConcreteObjectFieldsTransformations(ThreadInfo currentThread, String fieldOrRetName, int objRef, 
			PathCondition transformations, Map<String, String> obj2Name,
			Map<String, String> fieldName2ObjName, Map<String, List<Expression>> objName2Fields) {//, Map<SymbolicInteger, List<SymbolicInteger>> obj2RefFields) {
		
		SymbolicInteger symFieldOrRet = new SymbolicInteger(fieldOrRetName);
		
		System.out.println(obj2Name);
		
		ElementInfo objElement = currentThread.getVM().getHeap().get(objRef);
		
		if(objElement == null) {
			transformations._addDet(symFieldOrRet, NullIndicator.NULL);
			transformations._addDet(Comparator.EQ, symFieldOrRet, -1);
			return;
		}
		
		String objSymName = fieldOrRetName.replace('.', '_') + "_local";
		
		SymbolicInteger symObj = new SymbolicInteger(objSymName);
		
		transformations._addDet(symFieldOrRet, NullIndicator.NOTNULL);
		transformations._addDet(Comparator.EQ, symFieldOrRet, symObj);
				
		fieldName2ObjName.put(fieldOrRetName, objSymName);
		
		String existingObjName = obj2Name.get(objElement.toString());
		boolean transformationsExist = existingObjName != null;
		
		if(!transformationsExist) { // Check if element has been encountered before
			// First time. Add to map
			obj2Name.put(objElement.toString(), objSymName);
		} else {
			// Add aliasing expression
			transformations._addDet(Comparator.EQ, new SymbolicInteger(existingObjName), symObj);
		}
		
		
		ClassInfo classInfo = objElement.getClassInfo();
		
		FieldInfo[] fieldsInfos = classInfo.getDeclaredInstanceFields();
		
		
		List<Expression> objFields = new ArrayList<Expression>();
		objName2Fields.put(objSymName, objFields);
		

		
		for(FieldInfo fieldInfo : fieldsInfos) {
			String fieldName = fieldInfo.getName();
			String fullFieldName = objSymName + "." + fieldName;
			
			Object symbolicFieldValue = objElement.getFieldAttr(fieldInfo);
			long longConcreteFieldValue = 0;
			
			if(fieldInfo.isBooleanField() ) {
				boolean booleanFieldValue = objElement.getBooleanField(fieldInfo);
				longConcreteFieldValue = booleanFieldValue? 1 : 0;
			}
			else if(fieldInfo.isByteField() ) {
				longConcreteFieldValue = objElement.getByteField(fieldInfo);
			}
			else if(fieldInfo.isCharField() ) {
				longConcreteFieldValue = objElement.getCharField(fieldInfo);
			}
			else if(fieldInfo.isShortField() ) {
				longConcreteFieldValue = objElement.getShortField(fieldInfo);
			}
			else if(fieldInfo.isIntField() ) {
				longConcreteFieldValue = objElement.getIntField(fieldInfo);
			}
			else if(fieldInfo.isLongField() ) {
				longConcreteFieldValue = objElement.getLongField(fieldInfo);
			} else {
				double doubleConcreteFieldValue;
				if(fieldInfo.isFloatField()) {
					doubleConcreteFieldValue = objElement.getFloatField(fieldInfo);
				}
				else if(fieldInfo.isDoubleField()) {
					doubleConcreteFieldValue = objElement.getDoubleField(fieldInfo);
				} else {
					
					int fieldRef = 0;
					ElementInfo fieldElement = null;
					
					if(fieldInfo.isReference() ) {
						fieldRef = objElement.getReferenceField(fieldInfo);
						fieldElement = currentThread.getVM().getHeap().get(fieldRef);
					}
					else if(fieldInfo.isArrayField() ) {
						// TODO deal with arrays
					} else {
						// Should be unreachable
						assert false: "You shouldn't be here.";
					}

					// Field is reference
					
					
					if(symbolicFieldValue != null) { // symbolic
						
						if(fieldInfo.getType().equals("java.lang.String")) { // string object
							StringSymbolic fieldSymVar = new StringSymbolic(fullFieldName);
							
							transformations.spc._addDet(StringComparator.EQUALS, (StringExpression)symbolicFieldValue, fieldSymVar);
							
							objFields.add(fieldSymVar);
							//System.out.println("\tField: " + fieldSymVar + "\tValue: " + symbolicFieldValue);
						} else {
							SymbolicInteger fieldSymVar = new SymbolicInteger(fullFieldName);
							
							transformations._addDet(Comparator.EQ, fieldSymVar, (SymbolicInteger) symbolicFieldValue);
							
							objFields.add(fieldSymVar);
							//System.out.println("\tField: " + fieldSymVar + "\tValue: " + symbolicFieldValue);
						}
						
						
						continue;
					} else { // concrete
						System.out.println("FieldInfo: " + fieldInfo);
						
						if(fieldElement == null) {
							Expression fieldSymVar = fieldInfo.getType().equals("java.lang.String")? 
														new StringSymbolic(fullFieldName) : 
														new SymbolicInteger(fullFieldName);
							transformations._addDet(fieldSymVar, NullIndicator.NULL);
							
							if(!fieldInfo.getType().equals("java.lang.String")) {
								transformations._addDet(Comparator.EQ, (SymbolicInteger) fieldSymVar, -1);
							}
							
							
							objFields.add(fieldSymVar);
						} else {
							// TODO might want to do some preprocessing
							String fieldObjName = fieldElement.toString();
							
							if(fieldInfo.getType().equals("java.lang.String")) { // string object
								StringSymbolic fieldSymVar = new StringSymbolic(fullFieldName);
								
								transformations.spc._addDet(StringComparator.EQUALS, fieldSymVar, fieldElement.asString());
								transformations._addDet(fieldSymVar, NullIndicator.NOTNULL);
								
								
								objFields.add(fieldSymVar);
							} else {
								/*SymbolicInteger fieldSymVar = new SymbolicInteger(fullFieldName);
								SymbolicInteger fieldObjSymVar = new SymbolicInteger(fieldObjName);
								
								transformations._addDet(Comparator.EQ, fieldSymVar, fieldObjSymVar);
								transformations._addDet(fieldSymVar, NullIndicator.NOTNULL);
								
								System.out.println("\tField: " + fieldSymVar + "\tValue: " + fieldObjSymVar);
								
								objFields.add(fieldSymVar);*/
								
								objFields.add(new SymbolicInteger(fullFieldName));
								//getConcreteObjectFieldsTransformations(currentThread, null, fieldRef, transformations, obj2Fields);
								//String fieldLocalObjName = fieldOrRetName + "." + fieldName;
								getConcreteObjectFieldsTransformations(currentThread, fullFieldName, fieldRef, transformations, obj2Name,
										fieldName2ObjName, objName2Fields);
							}
							
							
						}
						continue;
					}
					
				}
				
				// Field is floating point
				SymbolicReal fieldSymVar = new SymbolicReal(fullFieldName);
				
				if(symbolicFieldValue != null) { // symbolic
					transformations._addDet(Comparator.EQ, fieldSymVar, (RealExpression) symbolicFieldValue);
					System.out.println("\tField: " + fieldSymVar + "\tValue: " + symbolicFieldValue);
				} else { // concrete
					transformations._addDet(Comparator.EQ, fieldSymVar, doubleConcreteFieldValue);
					System.out.println("\tField: " + fieldSymVar + "\tValue: " + doubleConcreteFieldValue);
				}
				
				objFields.add(fieldSymVar);
				continue;
				
			}
			
			// Field is integer or boolean
			SymbolicInteger fieldSymVar = new SymbolicInteger(fullFieldName);
			if(symbolicFieldValue != null) { // symbolic
				transformations._addDet(Comparator.EQ, fieldSymVar, (IntegerExpression) symbolicFieldValue);
				System.out.println("\tField: " + fieldSymVar + "\tValue: " + symbolicFieldValue);
			} else { // concrete
				transformations._addDet(Comparator.EQ, fieldSymVar, longConcreteFieldValue);
				System.out.println("\tField: " + fieldSymVar + "\tValue: " + longConcreteFieldValue);
			}
			
			objFields.add(fieldSymVar);
		}
		
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
