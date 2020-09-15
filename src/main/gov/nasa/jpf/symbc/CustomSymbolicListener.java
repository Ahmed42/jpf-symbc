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

package gov.nasa.jpf.symbc;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.choice.IntIntervalGenerator;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;

import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.jvm.bytecode.RETURN;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMStaticFieldInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.arrays.ArrayExpression;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.bytecode.INVOKESTATIC;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.heap.Helper;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.vm.bytecode.WriteInstruction;
import gov.nasa.jpf.jvm.bytecode.GETSTATIC;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;

import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.MinMax;
import gov.nasa.jpf.symbc.numeric.NullIndicator;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;

import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.RealConstraint;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;


import gov.nasa.jpf.util.Pair;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Arrays;

public class CustomSymbolicListener extends PropertyListenerAdapter implements PublisherExtension {

    /*
     * Locals to preserve the value that was held by JPF prior to changing it in order to turn off state matching during
     * symbolic execution no longer necessary because we run spf stateless
     */
	
	public static int id = 0;
	
    private String currentMethodName = "";
    
    private Map<String, SymbolicMethodSummary> methodsSymbolicSummaries;
    
    private List<TransformedSymField> transformedSymFields;
    
    private List<SymField> externalStaticFields;
    
    // TODO change to full signature
    private String symbolicMethodSimpleName;

    public CustomSymbolicListener(Config conf, JPF jpf, String sMethodSimpleName) {
        jpf.addPublisherExtension(ConsolePublisher.class, this);
        methodsSymbolicSummaries = new HashMap<String, SymbolicMethodSummary>();
        symbolicMethodSimpleName = sMethodSimpleName;
        transformedSymFields = new ArrayList<>();
        externalStaticFields = new ArrayList<>();
        
        id++;
    }

    @Override
    public void propertyViolated(Search search) {

        VM vm = search.getVM();

        ChoiceGenerator<?> cg = vm.getChoiceGenerator();
        if (!(cg instanceof PCChoiceGenerator)) {
            ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
            while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
                prev_cg = prev_cg.getPreviousChoiceGenerator();
            }
            cg = prev_cg;
        }
        if ((cg instanceof PCChoiceGenerator) && ((PCChoiceGenerator) cg).getCurrentPC() != null) {
            PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
            String error = search.getLastError().getDetails();
            error = "\"" + error.substring(0, error.indexOf("\n")) + "...\"";
 
            if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
                SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
                PCAnalyzer pa = new PCAnalyzer();
                
                pa.solve(pc, solver);
               
            } else {
            	
                pc.solve();
            }
            
            System.out.println("An exception has been thrown.");
            System.out.println("Path constraints: " + pc);
            
            
            HeapChoiceGenerator heapCG = vm.getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
            PathCondition heapPC = (heapCG==null ? null : heapCG.getCurrentPCheap());

            System.out.println("Heap constraints: " + heapPC);
            
            // TODO put the error details in the result/transformation of the method
            // TODO add static fields transformations
            //SymbolicPathSummary pathSummary = new SymbolicPathSummary(pc, heapPC, null);
            //SymbolicMethodSummary symbolicMethodSummary = methodsSymbolicSummaries.get(currentMethodName);
            
            //System.out.println("test");
            
            // TODO add the following, even though it might not be possible for such case to happen
            //if(symbolicMethodSummary == null) {
            //	symbolicMethodSummary = new SymbolicMethodSummary(className, methodName);
            //}

            //symbolicMethodSummary.addPathSummary(pathSummary);
           
        }
    }

    
	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
		if (!vm.getSystemState().isIgnored()) {
			Instruction insn = instructionToExecute;

			MethodInfo mi = insn.getMethodInfo();
			String methodName = mi.getName();
			ClassInfo ci = mi.getClassInfo();
			String longName = mi.getLongName();
			
			/*if (methodName.equals(symbolicMethodSimpleName)) {
				System.out.println("Executing: " + instructionToExecute.getPosition() + "\t" + instructionToExecute);
			}*/
			
			if (ci != null) {
				
				
				
				// Get the latest choice generator of type PCChoiceGenerator or HeapChoiceGenerator
				PCChoiceGenerator[] pcChoiceGens = vm.getChoiceGeneratorsOfType(PCChoiceGenerator.class);
				HeapChoiceGenerator[] heapChoiceGens = vm.getChoiceGeneratorsOfType(HeapChoiceGenerator.class);
				
				
				
				PCChoiceGenerator pcChoiceGen = vm.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
				HeapChoiceGenerator heapChoiceGen = vm.getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
				
				if(mi.getLongName().equals("parse(String)")) {
					StackFrame frame = currentThread.getModifiableTopFrame();
					System.out.println("breakpoint");
				}
				
				int pcChoiceNo = 0, pcOffset = 0;
				
				if(pcChoiceGen != null) {
					pcChoiceNo = pcChoiceGen.getNextChoice();
					pcOffset = pcChoiceGen.getOffset();
				} 
				
				PathCondition heapPC = null;
				
				if(heapChoiceGen != null) {
					heapPC = heapChoiceGen.getCurrentPCheap();
				} 
				
				// Insert symbolic vars for static fields of other classes
				if (insn instanceof PUTSTATIC || insn instanceof GETSTATIC) {
					

					// System.out.println("GET\t" + insn);
					// TODO we need to remove the method name check
					if (methodName.equals(symbolicMethodSimpleName)) {
						FieldInfo fieldInfo = ((JVMStaticFieldInstruction) insn).getFieldInfo();

						ClassInfo fieldClassInfo = fieldInfo.getClassInfo();

						// Static field of a different class
						if (!fieldClassInfo.equals(ci)) {

							if (!mi.isClinit(fieldClassInfo)) {
								fieldClassInfo.initializeClass(currentThread);
							}

							String fieldName = fieldClassInfo.getName() + "." + fieldInfo.getName();

							boolean found = externalStaticFields.stream()
									.anyMatch(field -> field.getFieldName().equals(fieldName) &&
											containsTrasformation(pcChoiceGens, field) &&
											containsTrasformation(heapChoiceGens, field));

							SymField externalStaticField;
							if (!found) {
								ElementInfo fieldOwner = fieldClassInfo.getModifiableStaticElementInfo();

								Expression fieldSymVar = Helper.initializeStaticField(fieldInfo, fieldClassInfo,
										currentThread, "");

								externalStaticField = new SymField(fieldSymVar, 0, fieldOwner, fieldInfo,
										pcOffset, pcChoiceNo, heapPC,
										currentThread);
								externalStaticFields.add(externalStaticField);
							} else {
								// For debugging purposes
								externalStaticField = externalStaticFields.stream()
										.filter(field -> field.getFieldName().equals(fieldName)).findFirst().get();
							}
							
							//System.out.println("External static field:");
							//System.out.println("\t" + insn + "\t=+=+=\t" + externalStaticField);
						} else {
							// String fieldName = fieldClassInfo.getName() + "." + fieldInfo.getName();
							ElementInfo fieldOwner = fieldClassInfo.getModifiableStaticElementInfo();
							Object value = fieldOwner.getFieldAttr(fieldInfo);

							//System.out.println("Internal static field:");
							//System.out.println("\t" + insn + "\t=+=+=\t" + "Value: " + value);
						}
					}
				}

				// Identify transformed fields
				if (insn instanceof PUTFIELD || insn instanceof PUTSTATIC) {
					//if (methodName.equals(symbolicMethodSimpleName)) {

						FieldInfo fieldInfo = ((WriteInstruction) insn).getFieldInfo();

						ClassInfo fieldClassInfo = fieldInfo.getClassInfo();

						ElementInfo fieldOwner = null;

						int objRef = MJIEnv.NULL;
						if (insn instanceof PUTFIELD) {
							StackFrame frame = currentThread.getTopFrame();
							// StackFrame frame = currentThread.getModifiableTopFrame();
							objRef = frame.peek(((PUTFIELD) insn).getFieldSize());

							if (objRef != MJIEnv.NULL) {
								fieldOwner = currentThread.getElementInfo(objRef);
							}

						} else { // PUTSTATIC
							// This seems to work.
							// Something related to class initialization.
							if (!mi.isClinit(fieldClassInfo)) {
								fieldClassInfo.initializeClass(currentThread);
							}
							fieldOwner = fieldClassInfo.getStaticElementInfo();
							
							
						}

						if (fieldOwner != null) {
							Object attr = fieldOwner.getFieldAttr(fieldInfo);
							
							/*System.out.println("PUTFIELD: " 
											+ "\n\tvalue: "+ attr
											+ "\n\towner: " +  fieldOwner
											+ "\n\tOwner ref: " + objRef);*/

							if (attr instanceof SymbolicInteger 
									|| attr instanceof SymbolicReal
									|| attr instanceof StringSymbolic
									|| attr instanceof ArrayExpression) {

								// Record: The symbolic var, owning object, field info

								String fieldName = ((Expression) attr).stringPC();
								
								
								// Check if the field we are about to create a symbol for already exists, and is on the same current search path
								boolean found = transformedSymFields.stream()
										.anyMatch(field -> field.getFieldName().equals(fieldName) &&
												containsTrasformation(pcChoiceGens, field) &&
												containsTrasformation(heapChoiceGens, field));
								
								
    							
								if (!found) { 
									TransformedSymField changedField = 
											new TransformedSymField((Expression) attr,
													objRef, fieldOwner, fieldInfo, 
													pcOffset, pcChoiceNo, heapPC,
													currentThread);
									
									transformedSymFields.add(changedField);
								} 

							} 
						}

					//}

				}
				
				/*if(insn instanceof JVMReturnInstruction) {
					
					//PCChoiceGenerator pccg = vm.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
					PathCondition pc = pcChoiceGen.getCurrentPC();
					
					if(!pc.flagSolved || !heapPC.flagSolved) {
						return;
					}
					
					PathCondition transformations = new PathCondition();
					
					// We add a trivially true constraint
                	Constraint returnTransformation = new LinearIntegerConstraint(new IntegerConstant(1), Comparator.EQ, new IntegerConstant(1));
					
					transformations.prependAllConjuncts(returnTransformation);
					
					//PCChoiceGenerator[] pcChoiceGens = vm.getChoiceGeneratorsOfType(PCChoiceGenerator.class);
					//HeapChoiceGenerator[] heapChoiceGens = vm.getChoiceGeneratorsOfType(HeapChoiceGenerator.class);
					
					List<TransformedSymField> pathTransformedFields = new ArrayList<TransformedSymField>();
					
					for(TransformedSymField transformedField : transformedSymFields) {    							
						boolean isPresentInPCGC = containsTrasformation(pcChoiceGens, transformedField);
						boolean isPresentInHeapGC = containsTrasformation(heapChoiceGens, transformedField);
						
						if(isPresentInHeapGC && isPresentInPCGC) {
							pathTransformedFields.add(transformedField);
							
							Object transformationToAdd = transformedField.getTransformationConstraint();
							
							if(transformationToAdd instanceof Constraint) {
								transformations.prependAllConjuncts((Constraint) transformationToAdd);
							} else if(transformationToAdd instanceof StringConstraint) {
								transformations.spc._addDet((StringConstraint) transformationToAdd);
							}
							
							
						}
					}

					
                    SymbolicPathSummary pathSummary = 
                    		new SymbolicPathSummary(pc, heapPC, transformations, pathTransformedFields);
                    

                    
                    SymbolicMethodSummary symbolicMethodSummary = methodsSymbolicSummaries.get(longName);
                    
                    if(!symbolicMethodSummary.containsPathSummary(pathSummary)) {
                    	symbolicMethodSummary.addPathSummary(pathSummary);
                    }
                        
                    System.out.println("Choice Gen PC: " + pc);
                    System.out.println("-------------------");
                    System.out.println("Choice Gen HC: " + heapPC);
                    System.out.println("===================");
                    System.out.println("Trasformations: " + transformations);
                    System.out.println("\n");
					
					Heap heap = vm.getHeap();
					
					System.out.println("Checking out the heap BEFORE Return..");
					
					Iterable<ElementInfo> liveObjs = heap.liveObjects();
					
					for(ElementInfo liveObj : liveObjs) {
						boolean inCurrentThread = liveObj.getReferencingThreads().contains(currentThread);
						int ref = liveObj.getObjectRef();
						String content = "<none>";
						
						if(liveObj.isStringObject()) {
							content = liveObj.asString();
						}
						
						// && liveObj.getType().contains("String")
						if(inCurrentThread && ref >= 300 && liveObj.getType().contains("String")) {
							System.out.println("\t" + ref + ":" +  liveObj.getType() + ":" + content);
						}
						
						 
					}
				}*/

		
			}
		}
	}
    
    @Override
    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction,
            Instruction executedInstruction) {

        if (!vm.getSystemState().isIgnored()) {
            Instruction insn = executedInstruction;
            // SystemState ss = vm.getSystemState();
            ThreadInfo ti = currentThread;
            Config conf = vm.getConfig();
            
            
            MethodInfo mei = insn.getMethodInfo();
            String meName = mei.getName();
            
            //if(meName.equals(symbolicMethodSimpleName)) {
            	//System.out.println(ti.getExecutedInstructions() + "\t" + insn + "\t" + ti.isFirstStepInsn());
            	//System.out.println("Executed: " + executedInstruction.getPosition() + "\t" + executedInstruction);
            //}
            
            //System.out.println();

            if (insn instanceof JVMInvokeInstruction) {
                JVMInvokeInstruction md = (JVMInvokeInstruction) insn;
                String methodName = md.getInvokedMethodName();
                //System.out.println("method: " + methodName);
                int numberOfArgs = md.getArgumentValues(ti).length;

                

                MethodInfo mi = md.getInvokedMethod();
                ClassInfo ci = mi.getClassInfo();
                String className = ci.getName();

                StackFrame sf = ti.getTopFrame();
                String shortName = methodName;
                String longName = mi.getLongName();
                if (methodName.contains("("))
                    shortName = methodName.substring(0, methodName.indexOf("("));

                if (!mi.equals(sf.getMethodInfo()))
                    return;
                
                
                if ((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
                        || BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null)) {
                	
                	
                	
                    //MethodSummary methodSummary = new MethodSummary();

                    //methodSummary.setMethodName(className + "." + shortName);
                    /*Object[] argValues = md.getArgumentValues(ti);
                    String argValuesStr = "";
                    for (int i = 0; i < argValues.length; i++) {
                        argValuesStr = argValuesStr + argValues[i];
                        if ((i + 1) < argValues.length)
                            argValuesStr = argValuesStr + ",";
                    }*/
                    //methodSummary.setArgValues(argValuesStr);
                    /*byte[] argTypes = mi.getArgumentTypes();
                    String argTypesStr = "";
                    for (int i = 0; i < argTypes.length; i++) {
                        argTypesStr = argTypesStr + argTypes[i];
                        if ((i + 1) < argTypes.length)
                            argTypesStr = argTypesStr + ",";
                    }*/
                    //methodSummary.setArgTypes(argTypesStr);

                    // get the symbolic values (changed from constructing them here)
                    /*String symValuesStr = "";
                    String symVarNameStr = "";

                    LocalVarInfo[] argsInfo = mi.getArgumentLocalVars();

                    if (argsInfo == null)
                        throw new RuntimeException("ERROR: you need to turn debug option on");
                        */
                	
                    //int sfIndex = 1; // do not consider implicit param "this"
                    /*int namesIndex = 1;
                    if (md instanceof INVOKESTATIC) {
                        sfIndex = 0; // no "this" for static
                        namesIndex = 0;
                    }*/

                    /*for (int i = 0; i < numberOfArgs; i++) {
                        Expression expLocal = (Expression) sf.getLocalAttr(sfIndex);
                        if (expLocal != null) // symbolic
                            symVarNameStr = expLocal.toString();
                        else
                            symVarNameStr = argsInfo[namesIndex].getName() + "_CONCRETE" + ",";
                        // TODO: what happens if the argument is an array?
                        symValuesStr = symValuesStr + symVarNameStr + ",";
                        sfIndex++;
                        namesIndex++;
                        if (argTypes[i] == Types.T_LONG || argTypes[i] == Types.T_DOUBLE)
                            sfIndex++;

                    }*/

                    // get rid of last ","
                    /*if (symValuesStr.endsWith(",")) {
                        symValuesStr = symValuesStr.substring(0, symValuesStr.length() - 1);
                    }*/
                    //methodSummary.setSymValues(symValuesStr);

                    currentMethodName = longName;
                    //allSummaries.put(longName, methodSummary);
                    
                    SymbolicMethodSummary symbolicMethodSummary = new SymbolicMethodSummary(ci, mi);
                    methodsSymbolicSummaries.put(longName, symbolicMethodSummary);
                }
            } else if (insn instanceof JVMReturnInstruction) {
            	
            	
                MethodInfo mi = insn.getMethodInfo();
                ClassInfo ci = mi.getClassInfo();
                
                
                if (null != ci) {
                    String className = ci.getName();
                    String methodName = mi.getName();
                    String longName = mi.getLongName();
                    int numberOfArgs = mi.getNumberOfArguments();
                    

                    if (((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
                            || BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null))) {
                    	
                    	
                    	//System.out.println("path!");
                        ChoiceGenerator<?> cg = vm.getChoiceGenerator();
                        //System.out.println(cg);
                        if (!(cg instanceof PCChoiceGenerator)) {
                            ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
                            while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
                                prev_cg = prev_cg.getPreviousChoiceGenerator();
                            }
                            cg = prev_cg;
                        }
                        //System.out.println(cg);
                        if ((cg instanceof PCChoiceGenerator) && ((PCChoiceGenerator) cg).getCurrentPC() != null) {
                            PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
                            //System.out.println("PATH!" + pc);
                            // pc.solve(); //we only solve the pc
                            if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
                                SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
                                PCAnalyzer pa = new PCAnalyzer();
                                pa.solve(pc, solver);
                            } else
                                pc.solve();
                            
                            if (!PathCondition.flagSolved) {
                                return;
                            }
                            
                            
                            HeapChoiceGenerator heapCG = vm.getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
                            PathCondition heapPC = (heapCG==null ? null : heapCG.getCurrentPCheap());
                            
                            
                            String returnString = "";

                            Expression result = null;
                            
                            PathCondition transformations = new PathCondition();
                            Map<String, String> obj2Name = new HashMap<>();
                            Map<String, String> fieldName2ObjName = new HashMap<>();
                            Map<String, List<Expression>> objName2Fields = new HashMap<>();
                            TransformedSymField.localsCounter = 0;
                            
                            if (insn instanceof IRETURN) {
                                IRETURN ireturn = (IRETURN) insn;
                                int returnValue = ireturn.getReturnValue();
                                IntegerExpression returnAttr = (IntegerExpression) ireturn.getReturnAttr(ti);
                                if (returnAttr != null) {
                                    returnString = "Return Value: " + String.valueOf(returnAttr.solution());
                                    result = returnAttr;
                                } else { // concrete
                                    returnString = "Return Value: " + String.valueOf(returnValue);
                                    result = new IntegerConstant(returnValue);
                                }
                                
                                                                
                            } else if (insn instanceof LRETURN) {
                                LRETURN lreturn = (LRETURN) insn;
                                long returnValue = lreturn.getReturnValue();
                                IntegerExpression returnAttr = (IntegerExpression) lreturn.getReturnAttr(ti);
                                if (returnAttr != null) {
                                    returnString = "Return Value: " + String.valueOf(returnAttr.solution());
                                    result = returnAttr;
                                } else { // concrete
                                    returnString = "Return Value: " + String.valueOf(returnValue);
                                    result = new IntegerConstant((int) returnValue);
                                }
                                
                            } else if (insn instanceof DRETURN) {
                                DRETURN dreturn = (DRETURN) insn;
                                double returnValue = dreturn.getReturnValue();
                                RealExpression returnAttr = (RealExpression) dreturn.getReturnAttr(ti);
                                if (returnAttr != null) {
                                    returnString = "Return Value: " + String.valueOf(returnAttr.solution());
                                    result = returnAttr;
                                } else { // concrete
                                    returnString = "Return Value: " + String.valueOf(returnValue);
                                    result = new RealConstant(returnValue);
                                }
                            } else if (insn instanceof FRETURN) {

                                FRETURN freturn = (FRETURN) insn;
                                double returnValue = freturn.getReturnValue();
                                RealExpression returnAttr = (RealExpression) freturn.getReturnAttr(ti);
                                if (returnAttr != null) {
                                    returnString = "Return Value: " + String.valueOf(returnAttr.solution());
                                    result = returnAttr;
                                } else { // concrete
                                    returnString = "Return Value: " + String.valueOf(returnValue);
                                    result = new RealConstant(returnValue);
                                }

                            } else if (insn instanceof ARETURN) {
                                ARETURN areturn = (ARETURN) insn;
                                Expression returnAttr = (Expression) areturn.getReturnAttr(ti);
                                if (returnAttr != null) {
                                		//returnString = "Return Value: " + String.valueOf(returnAttr.solution());
                                        result = returnAttr;
                                } else {// concrete
                                    ElementInfo val = (ElementInfo) areturn.getReturnValue(ti);
                                    
                                    
                                    	if(val != null &&val.isStringObject()) { // concrete string
                                    		result = new StringConstant((String) val.asString());
                                    	} else { // concrete non-string (might be null)
                                    		int objRef = areturn.getReturnValue();
                                        	TransformedSymField.getConcreteObjectFieldsTransformations(currentThread, "RET", objRef, transformations, 
                                        			obj2Name, fieldName2ObjName, objName2Fields);

                                    	}
                                     
                                    
                                    
                                    
                                    
                                    /*if(val != null && val.isStringObject()) {
                                    	result = new StringConstant((String) val.asString());
                                    } else {
                                    	returnString = "Return Value: " + String.valueOf(val);
                                        // DynamicElementInfo val = (DynamicElementInfo)areturn.getReturnValue(ti);
                                        String tmp = String.valueOf(val);
                                        //tmp = tmp.substring(tmp.lastIndexOf('.') + 1); // TODO might need to check this later
                                        result = new SymbolicInteger(tmp);
                                    }*/
     
                                }
                            } else // other types of return
                                returnString = "Return Value: --";
                            
                            
                            
                            
                            //ParsableConstraint returnTransformation = null;
                            String returnVarName = "RET"; // TODO append hash of method name to ensure that it doesn't collide with an existing var
                            Expression retOutVar;
                            
                            if(insn instanceof FRETURN || insn instanceof DRETURN) {
                            	retOutVar = new SymbolicReal(returnVarName, MinMax.getVarMinDouble(returnVarName), MinMax.getVarMaxDouble(returnVarName));
                            	
                            	transformations._addDet(Comparator.EQ, retOutVar, result);
                            	
                                //returnTransformation = new RealConstraint((RealExpression) retOutVar, Comparator.EQ, (RealExpression) result);
                            } else if(insn instanceof ARETURN && result instanceof StringExpression) {
                            	retOutVar = new StringSymbolic(returnVarName);
                            	
                            	transformations.spc._addDet(StringComparator.EQUALS, (StringExpression) retOutVar, (StringExpression) result); 
                            	//returnTransformation = new StringConstraint((StringExpression) retOutVar, StringComparator.EQUALS, (StringExpression) result);
                            } else if (insn instanceof IRETURN || insn instanceof LRETURN) {
                            	long min, max;
                            	
                            	if(insn instanceof LRETURN) {
                            		min = MinMax.getVarMinLong(returnVarName);
                            		max = MinMax.getVarMaxLong(returnVarName);
                            	} else {
                            		min = MinMax.getVarMinInt(returnVarName);
                            		max = MinMax.getVarMaxInt(returnVarName);
                            	}
                            	
                            	retOutVar = new SymbolicInteger(returnVarName, min, max);
                                
                            	transformations._addDet(Comparator.EQ, retOutVar, result);
                            	//returnTransformation = new LinearIntegerConstraint((IntegerExpression) retOutVar, Comparator.EQ, (IntegerExpression) result);
                                
                            } //else {
                            	// We add a trivially true constraint
                            	//returnTransformation = new LinearIntegerConstraint(new IntegerConstant(1), Comparator.EQ, new IntegerConstant(1));
                            //}
                            
                            
                            /*Heap heap = vm.getHeap();
        					
        					System.out.println("Checking out the heap AFTER Return..");
        					
        					Iterable<ElementInfo> liveObjs = heap.liveObjects();
        					
        					for(ElementInfo liveObj : liveObjs) {
        						boolean inCurrentThread = liveObj.getReferencingThreads().contains(currentThread);
        						int ref = liveObj.getObjectRef();
        						String content = "<none>";
        						
        						if(liveObj.isStringObject()) {
        							content = liveObj.asString();
        						}
        						
        						// && liveObj.getType().contains("String")
        						if(inCurrentThread && ref >= 300 && liveObj.getType().contains("String")) {
        							System.out.println("\t" + ref + ":" +  liveObj.getType() + ":" + content);
        						}
        						
        						 
        					}*/
                            
                            
                            
                            // Debugging .. 
                            
    						//PathCondition transformations = new PathCondition();
    						
    						
    						//transformations.prependAllConjuncts(t);
    						
    						//transformations.prependAllConjuncts(returnTransformation);
    						
    						PCChoiceGenerator[] pcChoiceGens = vm.getChoiceGeneratorsOfType(PCChoiceGenerator.class);
    						HeapChoiceGenerator[] heapChoiceGens = vm.getChoiceGeneratorsOfType(HeapChoiceGenerator.class);
    						
    						List<TransformedSymField> pathTransformedFields = new ArrayList<TransformedSymField>();
    						
    						for(TransformedSymField transformedField : transformedSymFields) {    							
    							boolean isPresentInPCGC = containsTrasformation(pcChoiceGens, transformedField);
    							boolean isPresentInHeapGC = containsTrasformation(heapChoiceGens, transformedField);
    							
    							if(isPresentInHeapGC && isPresentInPCGC) {
    								pathTransformedFields.add(transformedField);
    								
    								transformedField.getTransformationConstraint(transformations, obj2Name, fieldName2ObjName, objName2Fields);
    								
    								System.out.println(obj2Name);
    								
    								/*if(transformationToAdd instanceof Constraint) {
    									transformations.prependAllConjuncts((Constraint) transformationToAdd);
    								} else if(transformationToAdd instanceof StringConstraint) {
    									transformations.spc._addDet((StringConstraint) transformationToAdd);
    								}*/
    								
    								
    							}
    						}
    	
    						
                            SymbolicPathSummary pathSummary = 
                            		new SymbolicPathSummary(pc, heapPC, transformations, pathTransformedFields);
                            

                            
                            SymbolicMethodSummary symbolicMethodSummary = methodsSymbolicSummaries.get(longName);
                            
                            symbolicMethodSummary.fieldName2ObjName.putAll(fieldName2ObjName);
                            symbolicMethodSummary.objName2Fields.putAll(objName2Fields);
                            
                            if(!symbolicMethodSummary.containsPathSummary(pathSummary)) {
                            	symbolicMethodSummary.addPathSummary(pathSummary);
                            }
                                
                            System.out.println("Choice Gen PC: " + pc);
                            System.out.println("-------------------");
                            System.out.println("Choice Gen HC: " + heapPC);
                            System.out.println("===================");
                            System.out.println("Trasformations: " + transformations);
                            System.out.println("===================");
                            System.out.println("Arrays transformations: " + pc.arrayExpressions);
                            System.out.println("\n");
                           

                            
                        }
                    }
                }
            } /*else if (insn instanceof PUTFIELD) {
            	MethodInfo mi = insn.getMethodInfo();
            	String methodName = mi.getName();
            	if (methodName.equals(symbolicMethodSimpleName)) {

					FieldInfo fieldInfo = ((WriteInstruction) insn).getFieldInfo();

					ClassInfo fieldClassInfo = fieldInfo.getClassInfo();

					ElementInfo fieldOwner = null;

					int objRef = MJIEnv.NULL;
			
						StackFrame frame = currentThread.getTopFrame();
						// StackFrame frame = currentThread.getModifiableTopFrame();
						objRef = frame.peek(((PUTFIELD) insn).getFieldSize());

						if (objRef != MJIEnv.NULL) {
							fieldOwner = currentThread.getElementInfo(objRef);
						}

					if (fieldOwner != null) {
						Object attr = fieldOwner.getFieldAttr(fieldInfo);
						
						if(attr == null && fieldInfo.isIntField()) {
							attr = fieldOwner.getIntField(fieldInfo);
						}
						
						System.out.println("After exec PUTFIELD: "  
											+ "\n\tvalue: " + attr
											+ "\n\towner: " +  fieldOwner
											+ "\n\tOwner ref: " + objRef);
					}
            	}
            }*/
        }
    }
    
    /**
     * 
     * @return true if the path given by PC choice generators `choiceGens` defines `field` at some point
     */
    private boolean containsTrasformation(PCChoiceGenerator[] choiceGens, SymField field) {
    	return Arrays.stream(choiceGens).anyMatch(choiceGen ->  choiceGen.getOffset() == field.getPCOffset() 
    			&& choiceGen.getNextChoice() == field.getPCChoiceNo());
    }
    
    /**
     * 
     * @return true if the path given by Heap CGs `choiceGens` defines `field` at some point
     */
    private boolean containsTrasformation(HeapChoiceGenerator[] choiceGens, SymField field) {
    	return Arrays.stream(choiceGens).anyMatch(choiceGen ->  choiceGen.getCurrentPCheap().equals(field.getHeapPC()));
    }
    
    public Map<String, SymbolicMethodSummary> getSymbolicSummaries() {
    	return methodsSymbolicSummaries;
    }


    // -------- the publisher interface
    @Override
    public void publishFinished(Publisher publisher) {
       
    }
}
