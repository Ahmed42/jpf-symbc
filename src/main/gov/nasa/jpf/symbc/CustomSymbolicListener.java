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
import gov.nasa.jpf.vm.Transition;

import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;

import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMStaticFieldInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
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
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringSymbolic;
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

public class CustomSymbolicListener extends PropertyListenerAdapter implements PublisherExtension {

    /*
     * Locals to preserve the value that was held by JPF prior to changing it in order to turn off state matching during
     * symbolic execution no longer necessary because we run spf stateless
     */

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
            
            
            //HeapChoiceGenerator heapCG = vm.getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
            //PathCondition heapPC = (heapCG==null ? null : heapCG.getCurrentPCheap());

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

			if (ci != null) {
				// Insert symbolic vars for static fields of other classes
				if (insn instanceof PUTSTATIC || insn instanceof GETSTATIC) {

					// System.out.println("GET\t" + insn);
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
									.anyMatch(field -> field.getFieldName().equals(fieldName));

							SymField externalStaticField;
							if (!found) {
								ElementInfo fieldOwner = fieldClassInfo.getModifiableStaticElementInfo();

								Expression fieldSymVar = Helper.initializeStaticField(fieldInfo, fieldClassInfo,
										currentThread, "");

								externalStaticField = new SymField(fieldSymVar, fieldOwner, fieldInfo,
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
					if (methodName.equals(symbolicMethodSimpleName)) {

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

							if (attr instanceof SymbolicInteger || attr instanceof SymbolicReal
									|| attr instanceof StringSymbolic) {

								// Record: The symbolic var, owning object, field info

								String fieldName = ((Expression) attr).stringPC();

								boolean found = transformedSymFields.stream()
										.anyMatch(field -> field.getFieldName().equals(fieldName));

								if (!found) {
									TransformedSymField changedField = new TransformedSymField((Expression) attr, fieldOwner, fieldInfo,
											currentThread);
									transformedSymFields.add(changedField);
									//System.out.println(insn + "\t=+=+=\t" + changedField);
								}

							} 
						}

					}

				}

		
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
            
            
            /*MethodInfo mei = insn.getMethodInfo();
            String meName = mei.getName();
            
            if(meName.equals(symbolicMethodSimpleName)) {
            	System.out.println(ti.getExecutedInstructions() + "\t" + insn + "\t" + ti.isFirstStepInsn());
            }*/
            
            //System.out.println();

            if (insn instanceof JVMInvokeInstruction) {
                JVMInvokeInstruction md = (JVMInvokeInstruction) insn;
                String methodName = md.getInvokedMethodName();
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
                    Object[] argValues = md.getArgumentValues(ti);
                    String argValuesStr = "";
                    for (int i = 0; i < argValues.length; i++) {
                        argValuesStr = argValuesStr + argValues[i];
                        if ((i + 1) < argValues.length)
                            argValuesStr = argValuesStr + ",";
                    }
                    //methodSummary.setArgValues(argValuesStr);
                    byte[] argTypes = mi.getArgumentTypes();
                    String argTypesStr = "";
                    for (int i = 0; i < argTypes.length; i++) {
                        argTypesStr = argTypesStr + argTypes[i];
                        if ((i + 1) < argTypes.length)
                            argTypesStr = argTypesStr + ",";
                    }
                    //methodSummary.setArgTypes(argTypesStr);

                    // get the symbolic values (changed from constructing them here)
                    String symValuesStr = "";
                    String symVarNameStr = "";

                    LocalVarInfo[] argsInfo = mi.getArgumentLocalVars();

                    if (argsInfo == null)
                        throw new RuntimeException("ERROR: you need to turn debug option on");

                    int sfIndex = 1; // do not consider implicit param "this"
                    int namesIndex = 1;
                    if (md instanceof INVOKESTATIC) {
                        sfIndex = 0; // no "this" for static
                        namesIndex = 0;
                    }

                    for (int i = 0; i < numberOfArgs; i++) {
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

                    }

                    // get rid of last ","
                    if (symValuesStr.endsWith(",")) {
                        symValuesStr = symValuesStr.substring(0, symValuesStr.length() - 1);
                    }
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
                                IntegerExpression returnAttr = (IntegerExpression) areturn.getReturnAttr(ti);
                                if (returnAttr != null) {
                                    returnString = "Return Value: " + String.valueOf(returnAttr.solution());
                                    result = returnAttr;
                                } else {// concrete
                                    Object val = areturn.getReturnValue(ti);
                                    returnString = "Return Value: " + String.valueOf(val);
                                    // DynamicElementInfo val = (DynamicElementInfo)areturn.getReturnValue(ti);
                                    String tmp = String.valueOf(val);
                                    tmp = tmp.substring(tmp.lastIndexOf('.') + 1); // TODO might need to check this later
                                    result = new SymbolicInteger(tmp);

                                }
                            } else // other types of return
                                returnString = "Return Value: --";
                            
                            
                            Constraint returnTransformation;
                            String returnVarName = "RET"; // TODO append hash of method name to ensure that it doesn't collide with an existing var
                            Expression retOutVar;
                            
                            if(insn instanceof FRETURN || insn instanceof DRETURN) {
                            	retOutVar = new SymbolicReal(returnVarName, MinMax.getVarMinDouble(returnVarName), MinMax.getVarMaxDouble(returnVarName));
                                returnTransformation = new RealConstraint((RealExpression) retOutVar, Comparator.EQ, (RealExpression) result);
                            } else {
                            	long min, max;
                            	
                            	if(insn instanceof LRETURN) {
                            		min = MinMax.getVarMinLong(returnVarName);
                            		max = MinMax.getVarMaxLong(returnVarName);
                            	} else {
                            		min = MinMax.getVarMinInt(returnVarName);
                            		max = MinMax.getVarMaxInt(returnVarName);
                            	}
                            	
                            	retOutVar = new SymbolicInteger(returnVarName, min, max);
                                returnTransformation = new LinearIntegerConstraint((IntegerExpression) retOutVar, Comparator.EQ, (IntegerExpression) result);
                            }
                            
                            // pc.solve();
                            // not clear why this part is necessary
                            /*
                             * if (SymbolicInstructionFactory.concolicMode) { //TODO: cleaner SymbolicConstraintsGeneral
                             * solver = new SymbolicConstraintsGeneral(); PCAnalyzer pa = new PCAnalyzer();
                             * pa.solve(pc,solver); } else pc.solve();
                             */

                            
                              //String pcString = pc.toString(); pcPair = new Pair<String,String>(pcString,returnString);
                              //MethodSummary methodSummary = allSummaries.get(longName); Vector<Pair> pcs =
                              //methodSummary.getPathConditions(); if ((!pcs.contains(pcPair)) &&
                              //(pcString.contains("SYM"))) { methodSummary.addPathCondition(pcPair); }
                              
                              //if(allSummaries.get(longName)!=null) // recursive call longName = longName +
                              //methodSummary.hashCode(); // differentiate the key for recursive calls
                              //allSummaries.put(longName,methodSummary); if (SymbolicInstructionFactory.debugMode) {
                              //System.out.println("*************Summary***************");
                              //System.out.println("PC is:"+pc.toString()); if(result!=null){
                              //System.out.println("Return is:  "+result);
                              //System.out.println("***********************************"); } }
                              // YN
                            
                            
                            
                            
                            /*Vector<Pair<String, Expression>> sFieldsTransforms = new Vector<Pair<String, Expression>>();
                            
                            
                            for(FieldInfo fieldInfo : ci.getDeclaredStaticFields()) {
                            	Object fieldVal = ci.getModifiableStaticElementInfo().getFieldAttr(fieldInfo);
                                
                            	if(fieldVal instanceof Expression) {
                            		String fieldName = fieldInfo.getName();
                            		
  
                            		
                            		Pair<String, Expression> fieldNameTrans = new Pair<>(fieldName, (Expression) fieldVal);
                            		
                            		sFieldsTransforms.add(fieldNameTrans);
                            	} 
                            }
                            Vector<Pair<String, Expression>> iFieldsTransforms = new Vector<Pair<String, Expression>>();
                            */
    						
                            // TODO instance fields transformations
                            
                            //System.out.println(heapPC);
                            //System.out.println("======");
                            
                            
    						PathCondition transformations = new PathCondition();
    						
    						transformations.prependAllConjuncts(returnTransformation);
    						
    						for(TransformedSymField transformedField : transformedSymFields) {
    							transformations.appendAllConjuncts(transformedField.getTransformationConstraint());
    						}
    						
    						
    						
                            SymbolicPathSummary pathSummary = 
                            		new SymbolicPathSummary(pc, heapPC, transformations);
                            

                            
                            SymbolicMethodSummary symbolicMethodSummary = methodsSymbolicSummaries.get(longName);
                            
                            if(!symbolicMethodSummary.containsPathSummary(pathSummary)) {
                            	symbolicMethodSummary.addPathSummary(pathSummary);
                            }
                                
                            
                            externalStaticFields.clear();
    						transformedSymFields.clear();

                            
                        }
                    }
                }
            } /*else if(insn instanceof GETFIELD || insn instanceof GETSTATIC) {
            	MethodInfo mi = insn.getMethodInfo();
                ClassInfo ci = mi.getClassInfo();
                
                
                if (null != ci) {
                	String className = ci.getName();
                    int numberOfArgs = mi.getNumberOfArguments();
                    String methodName = mi.getName();

                    	if(methodName.equals(symbolicMethodSimpleName)) {
                    		System.out.println(ti.getExecutedInstructions() + "\t" +insn);
                    		
                    		ChoiceGenerator heapCG = ti.getVM().getSystemState()
                    				.getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
                    		
                    		if(heapCG != null && heapCG instanceof HeapChoiceGenerator) {
                    			SymbolicInputHeap symInputHeap = ((HeapChoiceGenerator) heapCG).getCurrentSymInputHeap();
                    			
                    			if(symInputHeap != null) {
                        			System.out.println("HEAP: " + symInputHeap + "\n");
                        		}
                    		}
                    		
                    		
                    		
                    		
                    	}
                	
                }
            }*/
            
            /*else if(insn instanceof PUTFIELD || insn instanceof PUTSTATIC) {
            	MethodInfo mi = insn.getMethodInfo();
                ClassInfo ci = mi.getClassInfo();
                
                if (null != ci) {
      
                    String className = ci.getName();
                    String methodName = mi.getName();
                    String longName = mi.getLongName();
                    int numberOfArgs = mi.getNumberOfArguments();
                    
                    

                    if(methodName.equals(symbolicMethodSimpleName)) {
                    	
                    	FieldInfo fieldInfo = ((WriteInstruction) insn).getFieldInfo();
                    	
                    	ClassInfo fieldClassInfo = fieldInfo.getClassInfo();
                    	
                    	ElementInfo fieldOwner = fieldClassInfo.getModifiableStaticElementInfo();
                    	
                    	Object attr = fieldOwner.getFieldAttr(fieldInfo);
                    	
                    	System.out.println("PUT\t" + insn + "\tFIELD: " + attr + "\n");
               
                    }
                	
     
                }
            	
            	
            }*/
        }
    }
    
    public Map<String, SymbolicMethodSummary> getSymbolicSummaries() {
    	return methodsSymbolicSummaries;
    }


    // -------- the publisher interface
    @Override
    public void publishFinished(Publisher publisher) {
       
    }
}
