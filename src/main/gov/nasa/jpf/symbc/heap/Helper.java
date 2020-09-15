/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package gov.nasa.jpf.symbc.heap;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import gov.nasa.jpf.symbc.arrays.ArrayExpression;
import gov.nasa.jpf.symbc.arrays.ArrayHeapNode;
import gov.nasa.jpf.symbc.arrays.HelperResult;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.NullIndicator;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringHeapNode;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.vm.BooleanFieldInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DoubleFieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.FloatFieldInfo;
import gov.nasa.jpf.vm.IntegerFieldInfo;
import gov.nasa.jpf.vm.KernelState;
import gov.nasa.jpf.vm.LongFieldInfo;
import gov.nasa.jpf.vm.ReferenceFieldInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;

public class Helper {

	//public static SymbolicInteger SymbolicNull = new SymbolicInteger("SymbolicNull"); // hack for handling static fields; may no longer need it

	public static Expression initializeInstanceField(FieldInfo field, ElementInfo eiRef,
			String refChain, String suffix, Map<String, String> typeParamsArgsMapping){
		Expression sym_v = null;
		String name ="";

		name = field.getName();
		String fullName = refChain + "." + name + suffix;
		if (field instanceof IntegerFieldInfo || field instanceof LongFieldInfo) {
				sym_v = new SymbolicInteger(fullName);
		} else if (field instanceof FloatFieldInfo || field instanceof DoubleFieldInfo) {
			sym_v = new SymbolicReal(fullName);
		} else if (field instanceof ReferenceFieldInfo){
			// Check if field is of type parameter
			// How to check: if field generic signature is not empty and mappings is not null
			//	foreach key in the type-arg mapping, check if field.genSig.containts("T<key>;");
			//	- Yes: replace each occurrence of the key in the signature. Store the result in sym_v.typeArgument.
			//		   Inputs:
			//				- Mapping: {T : Ljava/lang/String;, ... }
			//				- field.genSig: TT; OR Lgenerics_exp/Tuple<TT;TT;TGEN;>;
			//		   Outputs
			//				- sym_v.typeArgument: Ljava/lang/Integer; 
			//									OR Lgenerics_exp/Tuple<Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;>;
			
			
			String typeArgument = null;
			
			
			if(field.getGenericSignature() != null && !field.getGenericSignature().isEmpty() 
			   && typeParamsArgsMapping != null) {
				
				typeArgument = field.getGenericSignature();
				
				for(Entry<String, String> typeParamArg : typeParamsArgsMapping.entrySet()) {
					String typeParam = typeParamArg.getKey();
					String typeArg = typeParamArg.getValue();

					typeArgument = typeArgument.replaceAll("T" + typeParam + ";", typeArg);
				}
			}
			
			// Check if field uses generic type:
			// How to check: if typeArgument != null and it contains '<'
			//  - Yes: store field.getTypeClassInfo().getGenericSignature() in sym_v.genericType 
			//		   and sym_v.typeArgument in sym_v.genericTypeInvocation
			//		   Inputs
			//				- field.genSig: Lgenerics_exp/Tuple<TT;TT;TGEN;>;
			//				- sym_v.typeArgument: Lgenerics_exp/Tuple<Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Double;>;
			//		   Outputs:
			//				- sym_v.genericTypeDefinition: field.getTypeClassInfo().getGenericSignature()
			//				- sym_v.genericTypeInvocation: sym_v.typeArgument
			
			
			String genericTypeDefinition = null;
			String genericTypeInvocation = null;
			
			if(typeArgument != null && typeArgument.contains("<")) {
				genericTypeDefinition = field.getTypeClassInfo().getGenericSignature();
				genericTypeInvocation = typeArgument;
			}
			
			// TODO check if generic and String
			/*System.out.println("Initializing field of type: " + field.getType());
			System.out.println("Generic?");
			String genericSig = field.getGenericSignature();
			System.out.println(genericSig);*/
			
			if (field.getType().equals("java.lang.String") || 
					(typeArgument != null && typeArgument.equals("Ljava/lang/String;")))
				sym_v = new StringSymbolic(fullName);
			else {
				sym_v = new SymbolicInteger(fullName);
				((SymbolicInteger) sym_v).genericTypeDefinition = genericTypeDefinition;
				((SymbolicInteger) sym_v).genericTypeInvocation = genericTypeInvocation;
				((SymbolicInteger) sym_v).typeArgument = typeArgument;
			}

		} else if (field instanceof BooleanFieldInfo) {
				//	treat boolean as an integer with range [0,1]
				sym_v = new SymbolicInteger(fullName, 0, 1);
		}
		eiRef.setFieldAttr(field, sym_v);
		return sym_v;
	}

	public static void initializeInstanceFields(FieldInfo[] fields, ElementInfo eiRef,
			String refChain, Map<String, String> typeParamsArgsMapping){
		for (int i=0; i<fields.length;i++)
			initializeInstanceField(fields[i], eiRef, refChain, "", typeParamsArgsMapping);
	}

	public static Expression initializeStaticField(FieldInfo staticField, ClassInfo ci,
			ThreadInfo ti, String suffix){

		Expression sym_v = null;
		String name ="";

		name = staticField.getName();
		String fullName = ci.getName() + "." + name + suffix;// + "_init";
		if (staticField instanceof IntegerFieldInfo || staticField instanceof LongFieldInfo) {
				sym_v = new SymbolicInteger(fullName);
		} else if (staticField instanceof FloatFieldInfo
				|| staticField instanceof DoubleFieldInfo) {
			sym_v = new SymbolicReal(fullName);
		}else if (staticField instanceof ReferenceFieldInfo){
			if (staticField.getType().equals("java.lang.String"))
				sym_v = new StringSymbolic(fullName);
			else
				sym_v = new SymbolicInteger(fullName);
		} else if (staticField instanceof BooleanFieldInfo) {
				//						treat boolean as an integer with range [0,1]
				sym_v = new SymbolicInteger(fullName, 0, 1);
		}
		StaticElementInfo sei = ci.getModifiableStaticElementInfo();
		if (sei == null) {
			ci.registerClass(ti);
			sei = ci.getStaticElementInfo();
		}
		if (sei.getFieldAttr(staticField) == null) {
			sei.setFieldAttr(staticField, sym_v);
		}
		return sym_v;
	}

	public static void initializeStaticFields(FieldInfo[] staticFields, ClassInfo ci,
			ThreadInfo ti){

		if (staticFields.length > 0) {
			for (int i = 0; i < staticFields.length; i++)
				initializeStaticField(staticFields[i], ci, ti, "");
		}
	}


	  public static HeapNode addNewHeapNode(ClassInfo typeClassInfo, ThreadInfo ti, Object attr,
			  PathCondition pcHeap, SymbolicInputHeap symInputHeap,
			  int numSymRefs, HeapNode[] prevSymRefs, boolean setShared) {
		  
		  String className = typeClassInfo.getName();
		  
		  
		  // Check if object is of a generic type
		  //	- Yes: Parse the generic type definition and generic type invocation to obtain the object's type, 
		  //		   and the type parameters and arguments.
		  //		   Instantiate the object using the obtained type.
		  //		   Construct type parameters and arguments mapping, and pass them to the objects fields.
		  //	Inputs: 
		  //			- attr.genericTypeInvocation: Lgenerics_exp/Tuple<Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Double;>;
		  //			- attr.genericTypeDefinition or typeClassInfo.genSig: <T:Ljava/lang/Object;S:Ljava/lang/Object;GEN:Ljava/lang/Object;>Ljava/lang/Object;
		  //	Outputs:
		  //			- generics_exp.Tuple
		  //
		  //			- (T: Ljava/lang/String;)		
		  //			- (S: Ljava/lang/Integer;)		
		  //			- (GEN: Ljava/lang/Double;)		
		  //	
		  //	- No: check if attr.typeArgument is not null.
		  //			- Yes: process typeArgument to obtain the type, and use it to initialize the object. 
		  //			
		  //			Inputs: Ljava/lang/String;
		  //			Output: java.lang.String
		  //
		  
		  SymbolicInteger symVar = (SymbolicInteger) attr;
		  
		  String objectType = null;
		  Map<String, String> typeParamsArgsMapping = new HashMap<String, String>();
		  
		  
		  System.out.println("Handling: " + symVar);
		  System.out.println("genericTypeInvocation: " + symVar.genericTypeInvocation);
		  System.out.println("genericTypeDefinition: " + symVar.genericTypeDefinition);
		  System.out.println("getGenericSignature: " + typeClassInfo.getGenericSignature());
		  System.out.println("typeArgument: " + symVar.typeArgument);
		  
		  if(symVar.genericTypeInvocation != null && !symVar.genericTypeInvocation.isEmpty()) {
			  String genericTypeInvocation = symVar.genericTypeInvocation;
			  String genericTypeDef = symVar.genericTypeDefinition != null && !symVar.genericTypeDefinition.isEmpty()? 
					  symVar.genericTypeDefinition : typeClassInfo.getGenericSignature();
			  
			  objectType = genericTypeInvocation.replaceFirst("<.*", "").replaceFirst("^L", "").replaceAll("/", ".");
			  
			  ArrayList<String> typeArgs = getGenericTypeInvocationArguments(symVar.genericTypeInvocation);
			  ArrayList<String> typeParams = getGenericTypeParameters(genericTypeDef);
			  
			  
			  for(int i = 0; i < typeParams.size(); i++) {
				  typeParamsArgsMapping.put(typeParams.get(i), typeArgs.get(i));
			  }
			  
		  } else if(symVar.typeArgument != null && !symVar.typeArgument.isEmpty()) {
			  objectType = symVar.typeArgument.replaceFirst("^L", "").replaceAll("/", ".").replaceAll(";", "");
		  }
		  
		  if(objectType != null) {
			  System.out.println("Instantiating: " + symVar + "\tof type: " + objectType);
			  
			  //ClassLoaderInfo classLoaderInfo = ti.getSystemClassLoaderInfo();
			  ClassLoaderInfo classLoaderInfo = ClassLoaderInfo.getCurrentClassLoader();
			  typeClassInfo = classLoaderInfo.getResolvedClassInfo(objectType);
		  }
		  
		  /*System.out.println("Symbol name: " + attr);
		  System.out.println("typeClassInfo.name: " + typeClassInfo.getName());
		  System.out.println("typeClassInfo.genSig: " + typeClassInfo.getGenericSignature());
		  if(((SymbolicInteger) attr).genericTypeInvocation != null) {
			  
			  System.out.println("genericTypeInvocation: " + ((SymbolicInteger) attr).genericTypeInvocation);
		  }*/
		  
		  
		  if(typeClassInfo.isAbstract()) {
			  System.out.println("Creating heap node for abstract field!");
			  
			  if(typeClassInfo.getName().contains("java.util.Map")) {
				  System.out.println("Creating heap node for Map field!");
				  ClassLoaderInfo classLoaderInfo = ti.getSystemClassLoaderInfo();
				  ClassInfo concreteClassInfo = classLoaderInfo.getResolvedClassInfo("java.util.HashMap");
				  typeClassInfo = concreteClassInfo;
			  }
		  }
		  int daIndex = ti.getHeap().newObject(typeClassInfo, ti).getObjectRef();
		  ti.getHeap().registerPinDown(daIndex);
		  String refChain = ((SymbolicInteger) attr).getName(); // + "[" + daIndex + "]"; // do we really need to add daIndex here?
		  SymbolicInteger newSymRef = new SymbolicInteger( refChain);
		  
		  newSymRef.genericTypeDefinition = ((SymbolicInteger) attr).genericTypeDefinition;
		  newSymRef.genericTypeInvocation = ((SymbolicInteger) attr).genericTypeInvocation;
		  newSymRef.typeArgument = ((SymbolicInteger) attr).typeArgument;
		  
		  ElementInfo eiRef =  ti.getModifiableElementInfo(daIndex);//ti.getElementInfo(daIndex); // TODO to review!
		  if(setShared) {
			  eiRef.setShared(ti,true);//??
		  }
		  //daIndex.getObjectRef() -> number

		  // neha: this change allows all the fields in the class hierarchy of the
		  // object to be initialized as symbolic and not just its instance fields

		  int numOfFields = eiRef.getNumberOfFields();
		  FieldInfo[] fields = new FieldInfo[numOfFields];
		  for(int fieldIndex = 0; fieldIndex < numOfFields; fieldIndex++) {
			  fields[fieldIndex] = eiRef.getFieldInfo(fieldIndex);
		  }

		  Helper.initializeInstanceFields(fields, eiRef,refChain, typeParamsArgsMapping);

		  //neha: this change allows all the static fields in the class hierarchy
		  // of the object to be initialized as symbolic and not just its immediate
		  // static fields
		  ClassInfo superClass = typeClassInfo;
		  while(superClass != null) {
			  FieldInfo[] staticFields = superClass.getDeclaredStaticFields();
			  Helper.initializeStaticFields(staticFields, superClass, ti);
			  superClass = superClass.getSuperClass();
		  }

          // Put symbolic array in PC if we create a new array.
          if (typeClassInfo.isArray()) {
              String typeClass = typeClassInfo.getType();
              ArrayExpression arrayAttr = null;
              if (typeClass.charAt(1) != 'L') {
                  //arrayAttr = new ArrayExpression(eiRef.toString());
            	  arrayAttr = new ArrayExpression(refChain);
            	  
            	  System.out.println("Array type: " + typeClass);
            	  System.out.println("Array: " + arrayAttr);
              } else {
                  //arrayAttr = new ArrayExpression(eiRef.toString(), typeClass.substring(2, typeClass.length() -1));
            	  arrayAttr = new ArrayExpression(refChain, typeClass.substring(2, typeClass.length() -1));

              }
              //ti.getVM().getLastChoiceGeneratorOfType(PCChoiceGenerator.class).getCurrentPC().arrayExpressions.put(eiRef.toString(), arrayAttr);
              PCChoiceGenerator choiceGen = ti.getVM().getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
              PathCondition pc = choiceGen.getCurrentPC();
              pc.arrayExpressions.put(refChain, arrayAttr);
              choiceGen.setCurrentPC(pc);
          }

		  // create new HeapNode based on above info
		  // update associated symbolic input heap
		  HeapNode n= new HeapNode(daIndex,typeClassInfo,newSymRef);
		  symInputHeap._add(n);
		  //pcHeap._addDet(Comparator.NE, newSymRef, new IntegerConstant(-1));
		  pcHeap._addDet(newSymRef, NullIndicator.NOTNULL);
		  //pcHeap._addDet(Comparator.EQ, newSymRef, new IntegerConstant(numSymRefs));
		  for (int i=0; i< numSymRefs; i++)
			  pcHeap._addDet(Comparator.NE, n.getSymbolic(), prevSymRefs[i].getSymbolic());
		  return n;
	  }

	  public static HelperResult addNewArrayHeapNode(ClassInfo typeClassInfo, ThreadInfo ti, Object attr,
			  PathCondition pcHeap, SymbolicInputHeap symInputHeap,
			  int numSymRefs, HeapNode[] prevSymRefs, boolean setShared, IntegerExpression indexAttr, int arrayRef) {
		  int daIndex = ti.getHeap().newObject(typeClassInfo, ti).getObjectRef();
		  ti.getHeap().registerPinDown(daIndex);
		  //String refChain = ((ArrayExpression) attr).getName(); // + "[" + daIndex + "]"; // do we really need to add daIndex here?
		  String refChain = ((ArrayExpression) attr).getName() + "_atIndex_" + indexAttr;
		  SymbolicInteger newSymRef = new SymbolicInteger(refChain);
		  ElementInfo eiRef =  ti.getModifiableElementInfo(daIndex);//ti.getElementInfo(daIndex); // TODO to review!
		  if(setShared) {
			  eiRef.setShared(ti,true);//??
		  }
		  //daIndex.getObjectRef() -> number

		  // neha: this change allows all the fields in the class hierarchy of the
		  // object to be initialized as symbolic and not just its instance fields

		  int numOfFields = eiRef.getNumberOfFields();
		  FieldInfo[] fields = new FieldInfo[numOfFields];
		  for(int fieldIndex = 0; fieldIndex < numOfFields; fieldIndex++) {
			  fields[fieldIndex] = eiRef.getFieldInfo(fieldIndex);
		  }

		  Helper.initializeInstanceFields(fields, eiRef,refChain, null);

		  //neha: this change allows all the static fields in the class hierarchy
		  // of the object to be initialized as symbolic and not just its immediate
		  // static fields
		  ClassInfo superClass = typeClassInfo;
		  while(superClass != null) {
			  FieldInfo[] staticFields = superClass.getDeclaredStaticFields();
			  Helper.initializeStaticFields(staticFields, superClass, ti);
			  superClass = superClass.getSuperClass();
		  }

          // Put symbolic array in PC if we create a new array.
          if (typeClassInfo.isArray()) {
              String typeClass = typeClassInfo.getType();
              ArrayExpression arrayAttr = null;
              if (typeClass.charAt(1) != 'L') {
                  arrayAttr = new ArrayExpression(eiRef.toString());
              } else {
                  arrayAttr = new ArrayExpression(eiRef.toString(), typeClass.substring(2, typeClass.length() -1));
              }
              ti.getVM().getLastChoiceGeneratorOfType(PCChoiceGenerator.class).getCurrentPC().arrayExpressions.put(eiRef.toString(), arrayAttr);
          }

		  // create new HeapNode based on above info
		  // update associated symbolic input heap
          newSymRef.isLazyInitialized = true;
		  ArrayHeapNode n= new ArrayHeapNode(daIndex,typeClassInfo,newSymRef, indexAttr, arrayRef);
		  symInputHeap._add(n);
		  //pcHeap._addDet(Comparator.NE, newSymRef, new IntegerConstant(-1));
		  //pcHeap._addDet(Comparator.EQ, newSymRef, new IntegerConstant(numSymRefs));
		  pcHeap._addDet(newSymRef, NullIndicator.NOTNULL);
		  for (int i=0; i< numSymRefs; i++)
			  pcHeap._addDet(Comparator.NE, n.getSymbolic(), prevSymRefs[i].getSymbolic());
		  HelperResult result = new HelperResult(n, daIndex);
          return result;
	  }

	  public static StringHeapNode addNewStringHeapNode(ClassInfo typeClassInfo, ThreadInfo ti, Object attr,
			  PathCondition pcHeap, SymbolicInputHeap symInputHeap,
			  int numSymRefs, HeapNode[] prevSymRefs, boolean setShared) {
		  
		  int daIndex = ti.getHeap().newString("", ti).getObjectRef(); 
		  ti.getHeap().registerPinDown(daIndex);
		  ElementInfo eiRef =  ti.getModifiableElementInfo(daIndex);
		  if(setShared) {
			  eiRef.setShared(ti,true);//??
		  }
		  
		  StringSymbolic symVar = (StringSymbolic) attr;
		  String symStringName = ((StringSymbolic) attr).getName();
		  StringSymbolic newSymVar = new StringSymbolic(symStringName);
		  newSymVar.isLazyInitialized = true;
		  
		  StringHeapNode node = new StringHeapNode(daIndex, typeClassInfo, newSymVar);
		  symInputHeap._add(node); 
		  pcHeap._addDet(newSymVar, NullIndicator.NOTNULL);
		  
		  for(int i = 0; i < numSymRefs; i++) {
			  pcHeap.spc._addDet(StringComparator.NE, node.getStringSymbolic(), ((StringHeapNode)prevSymRefs[i]).getStringSymbolic());
		  }
		  
		  return node;
	  }

	  
	  /*
	   * Input: Lgenerics_exp/Tuple<Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Double;>;
	   * 
	   * Output:
	   * 	Ljava/lang/String;
	   * 	Ljava/lang/Integer;
	   * 	Ljava/lang/Double
	   * 
	   */
	public static ArrayList<String> getGenericTypeInvocationArguments(String genericTypeInvocation) {
		ArrayList<String> args = new ArrayList<String>();

		int leftBracket = genericTypeInvocation.indexOf('<');

		int i = leftBracket + 1;
		while (i < genericTypeInvocation.length()) {
			assert genericTypeInvocation.charAt(i) == 'L';

			int semiIndex = genericTypeInvocation.indexOf(';', i);
			leftBracket = genericTypeInvocation.indexOf('<', i);

			if (leftBracket == -1 || semiIndex < leftBracket) {
				String arg = genericTypeInvocation.substring(i, semiIndex + 1);
				args.add(arg);
				i = semiIndex + 1;
			} else {
				int openBracketsCounter = 1;
				int j = leftBracket + 1;

				for (; openBracketsCounter > 0; j++) {
					if (genericTypeInvocation.charAt(j) == '<') {
						openBracketsCounter++;
					} else if (genericTypeInvocation.charAt(j) == '>') {
						openBracketsCounter--;
					}
				}

				assert genericTypeInvocation.charAt(j + 1) == ';';

				String arg = genericTypeInvocation.substring(i, j + 1);

				args.add(arg);

				i = j + 2;
			}
		}

		return args;
	}
	  
	  /*
	   * Input: <T:Ljava/lang/Object;S:Ljava/lang/Object;GEN:Ljava/lang/Object;>Ljava/lang/Object;
	   * 
	   * Output:
	   * 	T
	   * 	S
	   * 	GEN
	   * 
	   */
	  public static ArrayList<String> getGenericTypeParameters(String genericType) {
		  assert genericType.charAt(0) == '<';
		  
		  ArrayList<String> params = new ArrayList<String>();
		  
		  //int openBrackets = 1;
		  int i = 1;
		  
		  System.out.println("genType: " + genericType);
		  while(genericType.charAt(i) != '>') {
			  int colonIndex = genericType.indexOf(':', i);
			  
			  String param = genericType.substring(i, colonIndex);
			  params.add(param);
			  
			  int semiIndex = genericType.indexOf(';', i);
			  int leftBracket = genericType.indexOf('<', i);
			  
			  if(leftBracket == -1 || semiIndex < leftBracket) {
				  i = semiIndex + 1;
			  } else {
				  int openBrackets = 1;
				  int j = leftBracket + 1;
				  
				  for(; openBrackets > 0; j++) {
					  if(genericType.charAt(j) == '<') {
						  openBrackets++;
					  } else if(genericType.charAt(j) == '>') {
						  openBrackets--;
					  }
				  }
				  
				  i = j + 2;
			  }
		  }
		  
		  return params;
	  }

	  
	  public static ClassInfo getTypeClassInfo(String typeSig) {
		  String normalType = typeSig.replaceFirst("<.*", "").replaceFirst("^L", "").replaceAll("/", ".").replaceAll(";", "");
		  
		  ClassLoaderInfo classLoaderInfo = ClassLoaderInfo.getCurrentClassLoader();
		  ClassInfo typeClassInfo = classLoaderInfo.getResolvedClassInfo(normalType);
		  
		  return typeClassInfo;
	  }

}
