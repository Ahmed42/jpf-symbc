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

//
//Copyright (C) 2005 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.symbc.numeric;

import gov.nasa.jpf.symbc.ParsableConstraint;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.arrays.ArrayConstraint;
import gov.nasa.jpf.symbc.arrays.ArrayExpression;
import gov.nasa.jpf.symbc.arrays.InitExpression;
import gov.nasa.jpf.symbc.arrays.RealArrayConstraint;
import gov.nasa.jpf.symbc.arrays.RealStoreExpression;
import gov.nasa.jpf.symbc.arrays.SelectExpression;
import gov.nasa.jpf.symbc.arrays.StoreExpression;
import gov.nasa.jpf.symbc.arrays.StringByteArrayExpression;
import gov.nasa.jpf.symbc.mixednumstrg.SpecialIntegerExpression;
import gov.nasa.jpf.symbc.mixednumstrg.SpecialOperator;
import gov.nasa.jpf.symbc.numeric.solvers.IncrementalListener;
import gov.nasa.jpf.symbc.numeric.solvers.IncrementalSolver;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemCoral;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemGeneral;

import gov.nasa.jpf.symbc.numeric.solvers.ProblemZ3;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemZ3BitVector;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemZ3BitVectorIncremental;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemZ3Incremental;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemZ3Optimize;
import gov.nasa.jpf.symbc.string.DerivedStringExpression;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringOperator;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.SymbolicCharAtInteger;
import gov.nasa.jpf.symbc.string.SymbolicHashCode;
import gov.nasa.jpf.symbc.string.SymbolicIndexOf2Integer;
import gov.nasa.jpf.symbc.string.SymbolicIndexOfChar2Integer;
import gov.nasa.jpf.symbc.string.SymbolicIndexOfCharInteger;
import gov.nasa.jpf.symbc.string.SymbolicIndexOfInteger;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOf2Integer;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOfChar2Integer;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOfCharInteger;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOfInteger;
import gov.nasa.jpf.symbc.string.SymbolicLengthInteger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.SeqExpr;
import com.microsoft.z3.SeqSort;


// parses PCs

public class PCParser {
  static ProblemGeneral pb;
  static public Map<SymbolicReal, Object>	symRealVar =new HashMap<SymbolicReal,Object>(); // a map between symbolic real variables and DP variables
  static Map<SymbolicInteger,Object>	symIntegerVar = new HashMap<SymbolicInteger,Object>(); // a map between symbolic variables and DP variables
  //static Boolean result; // tells whether result is satisfiable or not
  static int tempVars = 0; //Used to construct "or" clauses

  static public Map<StringSymbolic, Object> symStringVar = new HashMap<StringSymbolic,Object>();
  
  //	 Converts IntegerExpression's into DP's IntExp's
  static Object getExpression(IntegerExpression eRef) {
    assert eRef != null;
    
    if(eRef instanceof IntegerConstant) {
    	return pb.makeIntConst(((IntegerConstant) eRef).value);
    }
    
    
    assert !(eRef instanceof IntegerConstant);

    /*if (eRef instanceof SymbolicInteger) {
      Object dp_var = symIntegerVar.get(eRef);
      if (dp_var == null) {
        dp_var = pb.makeIntVar(((SymbolicInteger)eRef).getName(),
            ((SymbolicInteger)eRef)._min, ((SymbolicInteger)eRef)._max);
        symIntegerVar.put((SymbolicInteger)eRef, dp_var);
      }
      return dp_var;
    }*/
    if(eRef instanceof SpecialIntegerExpression) {
    	SpecialIntegerExpression specialExpr = (SpecialIntegerExpression) eRef;
    	if(specialExpr.op == SpecialOperator.VALUEOF) {
    		StringExpression strExprToParse = specialExpr.opr;
    		
    		// ctx.stringToInt(arg0)
    		return pb.makeStringToInt(getExpression(strExprToParse));
    	}
    } else if(eRef instanceof SymbolicInteger) {
    	return getExpression((SymbolicInteger) eRef);
    }
    
    

    Operator    opRef;
    IntegerExpression	e_leftRef;
    IntegerExpression	e_rightRef;

    if(eRef instanceof BinaryLinearIntegerExpression) {
      opRef = ((BinaryLinearIntegerExpression)eRef).op;
      e_leftRef = ((BinaryLinearIntegerExpression)eRef).left;
      e_rightRef = ((BinaryLinearIntegerExpression)eRef).right;
    } else { // bin non lin expr
      if(pb instanceof ProblemCoral || pb instanceof ProblemZ3 || pb instanceof ProblemZ3Optimize || pb instanceof ProblemZ3BitVector ||
          pb instanceof ProblemZ3Incremental || pb instanceof ProblemZ3BitVectorIncremental) {
        opRef = ((BinaryNonLinearIntegerExpression)eRef).op;
        e_leftRef = ((BinaryNonLinearIntegerExpression)eRef).left;
        e_rightRef = ((BinaryNonLinearIntegerExpression)eRef).right;
      }
      else
        throw new RuntimeException("## Error: Binary Non Linear Expression " + eRef);
    }
    switch(opRef){
      case PLUS:
        if (e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.plus(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.plus(getExpression(e_leftRef),((IntegerConstant)e_rightRef).value);
        else
          return pb.plus(getExpression(e_leftRef),getExpression(e_rightRef));
      case MINUS:
        if (e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.minus(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.minus(getExpression(e_leftRef),((IntegerConstant)e_rightRef).value);
        else
          return pb.minus(getExpression(e_leftRef),getExpression(e_rightRef));
      case MUL:
        if (e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.mult(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.mult(((IntegerConstant)e_rightRef).value,getExpression(e_leftRef));
        else {
          if(pb instanceof ProblemCoral || pb instanceof ProblemZ3|| pb instanceof ProblemZ3Optimize ||  pb instanceof ProblemZ3BitVector ||
          pb instanceof ProblemZ3Incremental || pb instanceof ProblemZ3BitVectorIncremental)
            return pb.mult(getExpression(e_leftRef),getExpression(e_rightRef));
          else
            throw new RuntimeException("## Error: Binary Non Linear Operation");
        }
      case DIV:
        if (e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant) // TODO: this might not be linear
          return pb.div(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.div(getExpression(e_leftRef),((IntegerConstant)e_rightRef).value);
        else {
          if(pb instanceof ProblemCoral || pb instanceof ProblemZ3|| pb instanceof ProblemZ3Optimize || pb instanceof ProblemZ3BitVector ||
          pb instanceof ProblemZ3Incremental || pb instanceof ProblemZ3BitVectorIncremental)
            return pb.div(getExpression(e_leftRef),getExpression(e_rightRef));
          else
            throw new RuntimeException("## Error: Binary Non Linear Operation");
        }
      case REM:
        if (e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant) // TODO: this might not be linear
          return pb.rem(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.rem(getExpression(e_leftRef),((IntegerConstant)e_rightRef).value);
        else {
          if(pb instanceof ProblemCoral || pb instanceof ProblemZ3|| pb instanceof ProblemZ3Optimize || pb instanceof ProblemZ3BitVector ||
          pb instanceof ProblemZ3Incremental || pb instanceof ProblemZ3BitVectorIncremental)
            return pb.rem(getExpression(e_leftRef),getExpression(e_rightRef));
          else
            throw new RuntimeException("## Error: Binary Non Linear Operation");
        }
      case AND:
        if(e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.and(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.and(((IntegerConstant)e_rightRef).value,getExpression(e_leftRef));
        else
          return pb.and(getExpression(e_leftRef),getExpression(e_rightRef));
      case OR:
        if(e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.or(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.or(((IntegerConstant)e_rightRef).value,getExpression(e_leftRef));
        else
          return pb.or(getExpression(e_leftRef),getExpression(e_rightRef));
      case XOR:
        if(e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.xor(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.xor(((IntegerConstant)e_rightRef).value,getExpression(e_leftRef));
        else
          return pb.xor(getExpression(e_leftRef),getExpression(e_rightRef));
      case SHIFTR:
        if(e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.shiftR(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.shiftR(getExpression(e_leftRef),((IntegerConstant)e_rightRef).value);
        else
          return pb.shiftR(getExpression(e_leftRef),getExpression(e_rightRef));
      case SHIFTUR:
        if(e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.shiftUR(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.shiftUR(getExpression(e_leftRef),((IntegerConstant)e_rightRef).value);
        else
          return pb.shiftUR(getExpression(e_leftRef),getExpression(e_rightRef));
      case SHIFTL:
        if(e_leftRef instanceof IntegerConstant && e_rightRef instanceof IntegerConstant)
          throw new RuntimeException("## Error: this is not a symbolic expression"); //
        else if (e_leftRef instanceof IntegerConstant)
          return pb.shiftL(((IntegerConstant)e_leftRef).value,getExpression(e_rightRef));
        else if (e_rightRef instanceof IntegerConstant)
          return pb.shiftL(getExpression(e_leftRef),((IntegerConstant)e_rightRef).value);
        else
          return pb.shiftL(getExpression(e_leftRef),getExpression(e_rightRef));
      default:
        throw new RuntimeException("## Error: Binary Non Linear Operation");
    }


  }


  // Converts RealExpression's into DP RealExp's
  static Object getExpression(RealExpression eRef) {
    assert eRef != null;
    assert !(eRef instanceof RealConstant);

    
    if (eRef instanceof SymbolicReal) {
      Object dp_var = symRealVar.get(eRef);
      if (dp_var == null) {
        dp_var = pb.makeRealVar(((SymbolicReal)eRef).getName(),
            ((SymbolicReal)eRef)._min, ((SymbolicReal)eRef)._max);
        symRealVar.put((SymbolicReal)eRef, dp_var);
      }
      return dp_var;
    }

    if(eRef instanceof BinaryRealExpression) {
      Operator    opRef;
      RealExpression	e_leftRef;
      RealExpression	e_rightRef;
      opRef = ((BinaryRealExpression)eRef).op;
      e_leftRef = ((BinaryRealExpression)eRef).left;
      e_rightRef = ((BinaryRealExpression)eRef).right;

      switch(opRef){
        case PLUS:
          if (e_leftRef instanceof RealConstant && e_rightRef instanceof RealConstant)
            return pb.constant(((RealConstant)e_leftRef).value + ((RealConstant)e_rightRef).value);
          //throw new RuntimeException("## Error: this is not a symbolic expression"); //
          else if (e_leftRef instanceof RealConstant)
            return pb.plus(((RealConstant)e_leftRef).value, getExpression(e_rightRef));
          else if (e_rightRef instanceof RealConstant)
            return pb.plus(getExpression(e_leftRef),((RealConstant)e_rightRef).value);
          else
            return pb.plus(getExpression(e_leftRef),getExpression(e_rightRef));
        case MINUS:
          if (e_leftRef instanceof RealConstant && e_rightRef instanceof RealConstant)
            throw new RuntimeException("## Error: this is not a symbolic expression"); //
          else if (e_leftRef instanceof RealConstant)
            return pb.minus(((RealConstant)e_leftRef).value,getExpression(e_rightRef));
          else if (e_rightRef instanceof RealConstant)
            return pb.minus(getExpression(e_leftRef),((RealConstant)e_rightRef).value);
          else
            return pb.minus(getExpression(e_leftRef),getExpression(e_rightRef));
        case MUL:
          if (e_leftRef instanceof RealConstant && e_rightRef instanceof RealConstant)
            throw new RuntimeException("## Error: this is not a symbolic expression"); //
          else if (e_leftRef instanceof RealConstant)
            return pb.mult(((RealConstant)e_leftRef).value,getExpression(e_rightRef));
          else if (e_rightRef instanceof RealConstant)
            return pb.mult(((RealConstant)e_rightRef).value,getExpression(e_leftRef));
          else
            return pb.mult(getExpression(e_leftRef),getExpression(e_rightRef));
        case DIV:
          if (e_leftRef instanceof RealConstant && e_rightRef instanceof RealConstant)
            throw new RuntimeException("## Error: this is not a symbolic expression"); //
          else if (e_leftRef instanceof RealConstant)
            return pb.div(((RealConstant)e_leftRef).value,getExpression(e_rightRef));
          else if (e_rightRef instanceof RealConstant)
            return pb.div(getExpression(e_leftRef),((RealConstant)e_rightRef).value);
          else
            return pb.div(getExpression(e_leftRef),getExpression(e_rightRef));
        case AND:
          if (e_leftRef instanceof RealConstant && e_rightRef instanceof RealConstant)
            throw new RuntimeException("## Error: this is not a symbolic expression"); //
          else if (e_leftRef instanceof RealConstant)
            return pb.and(((RealConstant)e_leftRef).value,getExpression(e_rightRef));
          else if (e_rightRef instanceof RealConstant)
            return pb.and(((RealConstant)e_rightRef).value,getExpression(e_leftRef));
          else
            return pb.and(getExpression(e_leftRef),getExpression(e_rightRef));

        default:
          throw new RuntimeException("## Error: Expression " + eRef);
      }
    }

    if(eRef instanceof MathRealExpression) {
      MathFunction funRef;
      RealExpression	e_arg1Ref;
      RealExpression	e_arg2Ref;

      funRef = ((MathRealExpression)eRef).op;
      e_arg1Ref = ((MathRealExpression)eRef).arg1;
      e_arg2Ref = ((MathRealExpression)eRef).arg2;
      switch(funRef){
        case SIN: return pb.sin(getExpression(e_arg1Ref));
        case COS: return pb.cos(getExpression(e_arg1Ref));
        case EXP: return pb.exp(getExpression(e_arg1Ref));
        case ASIN: return pb.asin(getExpression(e_arg1Ref));
        case ACOS:return pb.acos(getExpression(e_arg1Ref));
        case ATAN: return pb.atan(getExpression(e_arg1Ref));
        case LOG:return pb.log(getExpression(e_arg1Ref));
        case TAN:return pb.tan(getExpression(e_arg1Ref));
        case SQRT:return pb.sqrt(getExpression(e_arg1Ref));
        case POW:
          if (e_arg2Ref instanceof RealConstant)
            return pb.power(getExpression(e_arg1Ref),((RealConstant)e_arg2Ref).value);
          else if (e_arg1Ref instanceof RealConstant)
            return pb.power(((RealConstant)e_arg1Ref).value,getExpression(e_arg2Ref));
          else
            return pb.power(getExpression(e_arg1Ref),getExpression(e_arg2Ref));
        case ATAN2:
          if (e_arg2Ref instanceof RealConstant)
            return pb.atan2(getExpression(e_arg1Ref),((RealConstant)e_arg2Ref).value);
          else if (e_arg1Ref instanceof RealConstant)
            return pb.atan2(((RealConstant)e_arg1Ref).value,getExpression(e_arg2Ref));
          else
            return pb.atan2(getExpression(e_arg1Ref),getExpression(e_arg2Ref));
        default:
          throw new RuntimeException("## Error: Expression " + eRef);
      }
    }

    throw new RuntimeException("## Error: Expression " + eRef);
  }

  //public Map<SymbolicReal, Object> getSymRealVar() {
  //return symRealVar;
  //}


  //public Map<SymbolicInteger, Object> getSymIntegerVar() {
  //return symIntegerVar;
  //}


  static public boolean createDPMixedConstraint(MixedConstraint cRef) { // TODO

    Comparator c_compRef = cRef.getComparator();
    RealExpression c_leftRef = (RealExpression)cRef.getLeft();
    IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();
    assert (c_compRef == Comparator.EQ);

    if (c_leftRef instanceof SymbolicReal && c_rightRef instanceof SymbolicInteger) {
      //pb.post(new MixedEqXY((RealVar)(getExpression(c_leftRef)),(IntDomainVar)(getExpression(c_rightRef))));
      pb.post(pb.mixed(getExpression(c_leftRef),getExpression(c_rightRef)));
    }
    else if (c_leftRef instanceof SymbolicReal) { // c_rightRef is an IntegerExpression
      Object tmpi = pb.makeIntVar(c_rightRef + "_" + c_rightRef.hashCode(),(int)(((SymbolicReal)c_leftRef)._min), (int)(((SymbolicReal)c_leftRef)._max));
      if (c_rightRef instanceof IntegerConstant)
        pb.post(pb.eq(((IntegerConstant)c_rightRef).value,tmpi));
      else
        pb.post(pb.eq(getExpression(c_rightRef),tmpi));
      //pb.post(new MixedEqXY((RealVar)(getExpression(c_leftRef)),tmpi));
      pb.post(pb.mixed(getExpression(c_leftRef),tmpi));

    }
    else if (c_rightRef instanceof SymbolicInteger) { // c_leftRef is a RealExpression
      Object tmpr = pb.makeRealVar(c_leftRef + "_" + c_leftRef.hashCode(), ((SymbolicInteger)c_rightRef)._min, ((SymbolicInteger)c_rightRef)._max);
      if(c_leftRef instanceof RealConstant)
        pb.post(pb.eq(tmpr, ((RealConstant)c_leftRef).value));
      else
        pb.post(pb.eq(tmpr, getExpression(c_leftRef)));
      //pb.post(new MixedEqXY(tmpr,(IntDomainVar)(getExpression(c_rightRef))));
      pb.post(pb.mixed(tmpr,getExpression(c_rightRef)));
    }
    else
      assert(false); // should not be reachable

    return true;
  }

  static public boolean createDPRealConstraint(RealConstraint cRef) {

    Comparator c_compRef = cRef.getComparator();
    RealExpression c_leftRef = (RealExpression)cRef.getLeft();
    RealExpression c_rightRef = (RealExpression)cRef.getRight();

    switch(c_compRef){
      case EQ:
        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
          if (!(((RealConstant) c_leftRef).value == ((RealConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof RealConstant) {
          pb.post(pb.eq(((RealConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof RealConstant) {
          pb.post(pb.eq(getExpression(c_leftRef),((RealConstant)c_rightRef).value));
        }
        else
          pb.post(pb.eq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case NE:
        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
          if (!(((RealConstant) c_leftRef).value != ((RealConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof RealConstant) {
          pb.post(pb.neq(((RealConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof RealConstant) {
          pb.post(pb.neq(getExpression(c_leftRef),((RealConstant)c_rightRef).value));
        }
        else
          pb.post(pb.neq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case LT:
        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
          if (!(((RealConstant) c_leftRef).value < ((RealConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof RealConstant) {
          pb.post(pb.lt(((RealConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof RealConstant) {
          pb.post(pb.lt(getExpression(c_leftRef),((RealConstant)c_rightRef).value));
        }
        else
          pb.post(pb.lt(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case GE:
        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
          if (!(((RealConstant) c_leftRef).value >= ((RealConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof RealConstant) {
          pb.post(pb.geq(((RealConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof RealConstant) {
          pb.post(pb.geq(getExpression(c_leftRef),((RealConstant)c_rightRef).value));
        }
        else
          pb.post(pb.geq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case LE:
        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
          if (!(((RealConstant) c_leftRef).value <= ((RealConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof RealConstant) {
          pb.post(pb.leq(((RealConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof RealConstant) {
          pb.post(pb.leq(getExpression(c_leftRef),((RealConstant)c_rightRef).value));
        }
        else
          pb.post(pb.leq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case GT:
        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
          if (!(((RealConstant) c_leftRef).value > ((RealConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof RealConstant) {
          pb.post(pb.gt(((RealConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof RealConstant) {
          pb.post(pb.gt(getExpression(c_leftRef),((RealConstant)c_rightRef).value));
        }
        else
          pb.post(pb.gt(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
    }
    return true;
  }

  //Added by Gideon, to handle CNF style constraints???
  static public boolean createDPLinearOrIntegerConstraint (LogicalORLinearIntegerConstraints c) {
    List<Object> orList = new ArrayList<Object>();

    for (LinearIntegerConstraint cRef: c.getList()) {
      Comparator c_compRef = cRef.getComparator();
      IntegerExpression c_leftRef = (IntegerExpression)cRef.getLeft();
      IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();
      //Removed all return false: why?
      switch(c_compRef){
        case EQ:
          if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
            if (((IntegerConstant) c_leftRef).value == ((IntegerConstant) c_rightRef).value)
              return true;
          }
          else if (c_leftRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_rightRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar, part1));
            Object cc = pb.eq(((IntegerConstant)c_leftRef).value, tempVar);
            orList.add(cc);
          }
          else if (c_rightRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_leftRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt(""));
            pb.post(pb.eq(tempVar, part1)); tempVars++;
            orList.add(pb.eq(tempVar,((IntegerConstant)c_rightRef).value));
          }
          else {
            Object part1 = getExpression(c_leftRef);
            Object part2 = getExpression(c_rightRef);
            Object tempVar1 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            Object tempVar2 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar1, part1));
            pb.post(pb.eq(tempVar2, part2));
            orList.add(pb.eq(tempVar1,tempVar2));
          }
          break;
        case NE:
          if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
            if (((IntegerConstant) c_leftRef).value != ((IntegerConstant) c_rightRef).value)
              return true;
          }
          else if (c_leftRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_rightRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar, part1));
            Object cc = pb.neq(((IntegerConstant)c_leftRef).value, tempVar);
            orList.add(cc);
          }
          else if (c_rightRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_leftRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt(""));
            pb.post(pb.eq(tempVar, part1)); tempVars++;
            orList.add(pb.neq(tempVar,((IntegerConstant)c_rightRef).value));
          }
          else {
            Object part1 = getExpression(c_leftRef);
            Object part2 = getExpression(c_rightRef);
            Object tempVar1 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            Object tempVar2 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar1, part1));
            pb.post(pb.eq(tempVar2, part2));
            orList.add(pb.neq(tempVar1,tempVar2));
          }
          break;
        case LT:
          if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
            if (((IntegerConstant) c_leftRef).value < ((IntegerConstant) c_rightRef).value)
              return true;
          }
          else if (c_leftRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_rightRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar, part1));
            Object cc = pb.lt(((IntegerConstant)c_leftRef).value, tempVar);
            orList.add(cc);
          }
          else if (c_rightRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_leftRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt(""));
            pb.post(pb.eq(tempVar, part1)); tempVars++;
            orList.add(pb.lt(tempVar,((IntegerConstant)c_rightRef).value));
          }
          else {
            Object part1 = getExpression(c_leftRef);
            Object part2 = getExpression(c_rightRef);
            Object tempVar1 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            Object tempVar2 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar1, part1));
            pb.post(pb.eq(tempVar2, part2));
            orList.add(pb.lt(tempVar1,tempVar2));
          }
          break;
        case GE:
          if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
            if (((IntegerConstant) c_leftRef).value >= ((IntegerConstant) c_rightRef).value)
              return true;
          }
          else if (c_leftRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_rightRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar, part1));
            Object cc = pb.geq(((IntegerConstant)c_leftRef).value, tempVar);
            orList.add(cc);
          }
          else if (c_rightRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_leftRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt(""));
            pb.post(pb.eq(tempVar, part1)); tempVars++;
            orList.add(pb.geq(tempVar,((IntegerConstant)c_rightRef).value));
          }
          else {
            Object part1 = getExpression(c_leftRef);
            Object part2 = getExpression(c_rightRef);
            Object tempVar1 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            Object tempVar2 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar1, part1));
            pb.post(pb.eq(tempVar2, part2));
            orList.add(pb.geq(tempVar1,tempVar2));
          }
          break;
        case LE:
          if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
            if (((IntegerConstant) c_leftRef).value <= ((IntegerConstant) c_rightRef).value)
              return true;
          }
          else if (c_leftRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_rightRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar, part1));
            Object cc = pb.leq(((IntegerConstant)c_leftRef).value, tempVar);
            orList.add(cc);
          }
          else if (c_rightRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_leftRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt(""));
            pb.post(pb.eq(tempVar, part1)); tempVars++;
            orList.add(pb.leq(tempVar,((IntegerConstant)c_rightRef).value));
          }
          else {
            Object part1 = getExpression(c_leftRef);
            Object part2 = getExpression(c_rightRef);
            Object tempVar1 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            Object tempVar2 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar1, part1));
            pb.post(pb.eq(tempVar2, part2));
            orList.add(pb.leq(tempVar1,tempVar2));
          }
          break;
        case GT:
          if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
            if (((IntegerConstant) c_leftRef).value > ((IntegerConstant) c_rightRef).value)
              return true;
          }
          else if (c_leftRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_rightRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar, part1));
            Object cc = pb.gt(((IntegerConstant)c_leftRef).value, tempVar);
            orList.add(cc);
          }
          else if (c_rightRef instanceof IntegerConstant) {
            Object part1 = getExpression(c_leftRef);
            Object tempVar = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt(""));
            pb.post(pb.eq(tempVar, part1)); tempVars++;
            orList.add(pb.gt(tempVar,((IntegerConstant)c_rightRef).value));
          }
          else {
            Object part1 = getExpression(c_leftRef);
            Object part2 = getExpression(c_rightRef);
            Object tempVar1 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            Object tempVar2 = pb.makeIntVar("mytemp" + tempVars, MinMax.getVarMinInt(""), MinMax.getVarMaxInt("")); tempVars++;
            pb.post(pb.eq(tempVar1, part1));
            pb.post(pb.eq(tempVar2, part2));
            orList.add(pb.gt(tempVar1,tempVar2));
          }
          break;
      }
    }
    //System.out.println("[SymbolicConstraintsGeneral] orList: " + orList.toString());
    if (orList.size() == 0) return true;
    Object constraint_array[] = new Object[orList.size()];
    orList.toArray(constraint_array);

    pb.postLogicalOR(constraint_array);

    return true;

  }

  static public boolean createDPLinearIntegerConstraint(LinearIntegerConstraint cRef) {

    Comparator c_compRef = cRef.getComparator();

    IntegerExpression c_leftRef = (IntegerExpression)cRef.getLeft();
    IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();

    switch(c_compRef){
      case EQ:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value == ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.eq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.eq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.eq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case NE:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value != ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.neq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.neq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.neq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case LT:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value < ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.lt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.lt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.lt(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case GE:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value >= ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.geq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.geq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.geq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case LE:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value <= ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.leq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.leq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.leq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case GT:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value > ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.gt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.gt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.gt(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
    }
    return true;
  }

  static public boolean createDPNonLinearIntegerConstraint(NonLinearIntegerConstraint cRef) {

    Comparator c_compRef = cRef.getComparator();

    IntegerExpression c_leftRef = (IntegerExpression)cRef.getLeft();
    IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();

    switch(c_compRef){
      case EQ:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value == ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.eq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.eq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.eq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case NE:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value != ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.neq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.neq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.neq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case LT:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value < ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.lt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.lt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.lt(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case GE:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value >= ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.geq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.geq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.geq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case LE:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value <= ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.leq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.leq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.leq(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
      case GT:
        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
          if (!(((IntegerConstant) c_leftRef).value > ((IntegerConstant) c_rightRef).value))
            return false;
          else
            return true;
        }
        else if (c_leftRef instanceof IntegerConstant) {
          pb.post(pb.gt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef)));
        }
        else if (c_rightRef instanceof IntegerConstant) {
          pb.post(pb.gt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value));
        }
        else
          pb.post(pb.gt(getExpression(c_leftRef),getExpression(c_rightRef)));
        break;
    }
    return true;
  }
  //static Map<String,Boolean> dpMap = new HashMap<String,Boolean>();

  // Added by Aymeric to support symbolic Arrays
  public static boolean createArrayConstraint(ArrayConstraint cRef) {
    Comparator c_compRef = cRef.getComparator();

    SelectExpression selex = null;
        StoreExpression stoex = null;
        IntegerExpression sel_right = null;
        ArrayExpression sto_right = null;
        InitExpression initex = null;
        if (cRef.getLeft() instanceof SelectExpression) {
          selex = (SelectExpression)cRef.getLeft();
          sel_right = (IntegerExpression)cRef.getRight();
        } else if (cRef.getLeft() instanceof StoreExpression) {
           stoex = (StoreExpression)cRef.getLeft();
           sto_right = (ArrayExpression)cRef.getRight();
        } else if (cRef.getLeft() instanceof InitExpression) {
           initex = (InitExpression)cRef.getLeft();
        } else {
            throw new RuntimeException("ArrayConstraint is not select or store or init");
        }

        switch(c_compRef) {
        case EQ:

            if (selex != null && sel_right != null) {
                // The array constraint is a select
                ArrayExpression ae = (ArrayExpression) selex.arrayExpression;
                pb.post(pb.eq(pb.select(pb.makeArrayVar(ae.getName()),
                  (selex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)selex.indexExpression).value) :
getExpression(selex.indexExpression)),
                  (sel_right instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)sel_right).value) :
getExpression(sel_right)));
                break;
            }
            if (stoex != null && sto_right != null) {
                // The array constraint is a store
                ArrayExpression ae = (ArrayExpression) stoex.arrayExpression;
                ArrayExpression newae = (ArrayExpression) sto_right;
                pb.post(pb.eq(pb.store(pb.makeArrayVar(ae.getName()),
                  (stoex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)stoex.indexExpression).value) :
getExpression(stoex.indexExpression),
                  (stoex.value instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)stoex.value).value) :
getExpression(stoex.value)),
                   pb.makeArrayVar(newae.getName())));
                break;
            }
            if (initex != null) {
              // The array constraint is an initialization
              ArrayExpression ae = (ArrayExpression) initex.arrayExpression;
              IntegerConstant init_value = new IntegerConstant(initex.isRef? -1 : 0);
              pb.post(pb.init_array(pb.makeArrayVar(ae.getName()), pb.makeIntConst(init_value.value)));
              break;
            }
            throw new RuntimeException("ArrayConstraint is not correct select or store or init");
        case NE:
            if (selex != null && sel_right != null) {
                // The array constraint is a select
                ArrayExpression ae = (ArrayExpression) selex.arrayExpression;
                pb.post(pb.neq(pb.select(pb.makeArrayVar(ae.getName()), getExpression(selex.indexExpression)),
getExpression(sel_right)));
                break;
            }
            if (stoex != null && sto_right != null) {
                // The array constraint is a store
                ArrayExpression ae = (ArrayExpression)stoex.arrayExpression;
                ArrayExpression newae = (ArrayExpression) sto_right;
                pb.post(pb.neq(pb.store(pb.makeArrayVar(ae.getName()), getExpression(stoex.indexExpression),
getExpression(stoex.value)), newae));
                break;
            }
            throw new RuntimeException("ArrayConstraint is not correct select or store");
        default:
            throw new RuntimeException("ArrayConstraint is not select or store");
        }
        return true;
    }

public static boolean createRealArrayConstraint(final RealArrayConstraint cRef) {
        final Comparator c_compRef = cRef.getComparator();


        SelectExpression selex = null;
        RealStoreExpression stoex = null;
        RealExpression sel_right = null;
        ArrayExpression sto_right = null;
        if (cRef.getLeft() instanceof SelectExpression) {
             selex = (SelectExpression)cRef.getLeft();
             sel_right = (RealExpression)cRef.getRight();
        } else if (cRef.getLeft() instanceof RealStoreExpression) {
           stoex = (RealStoreExpression)cRef.getLeft();
           sto_right = (ArrayExpression)cRef.getRight();
        } else {
           throw new RuntimeException("ArrayConstraint is not select or store");
        }

        switch(c_compRef) {
        case EQ:

            if (selex != null && sel_right != null) {
                // The array constraint is a select
                ArrayExpression ae = selex.arrayExpression;
                pb.post(pb.eq(pb.realSelect(pb.makeRealArrayVar(ae.getName()),
                  (selex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)selex.indexExpression).value) :
getExpression(selex.indexExpression)),
                  (sel_right instanceof RealConstant) ? pb.makeRealConst(((RealConstant)sel_right).value) :
getExpression(sel_right)));
                break;
            }
            if (stoex != null && sto_right != null) {
                // The array constraint is a store
                ArrayExpression ae = stoex.arrayExpression;
                ArrayExpression newae = sto_right;
                pb.post(pb.eq(pb.realStore(pb.makeRealArrayVar(ae.getName()),
                  (stoex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)stoex.indexExpression).value) :
getExpression(stoex.indexExpression),
                  (stoex.value instanceof RealConstant) ? pb.makeRealConst(((RealConstant)stoex.value).value) :
getExpression(stoex.value)),
                   pb.makeRealArrayVar(newae.getName())));
                break;
            }
            throw new RuntimeException("ArrayConstraint is not correct select or store");
        case NE:
            if (selex != null && sel_right != null) {
                // The array constraint is a select
                ArrayExpression ae = selex.arrayExpression;
                pb.post(pb.neq(pb.realSelect(pb.makeRealArrayVar(ae.getName()), getExpression(selex.indexExpression)),
getExpression(sel_right)));
                break;
            }
            if (stoex != null && sto_right != null) {
                // The array constraint is a store
                ArrayExpression ae = stoex.arrayExpression;
                ArrayExpression newae = sto_right;
                pb.post(pb.neq(pb.realStore(pb.makeRealArrayVar(ae.getName()), getExpression(stoex.indexExpression),
getExpression(stoex.value)), newae));
                break;
            }
            throw new RuntimeException("ArrayConstraint is not correct select or store");
        default:
            throw new RuntimeException("ArrayConstraint is not select or store");
        }
        return true;
    }

  /**
   * Merges the given path condition with the given ProblemGeneral object (i.e. the solver).
   * Normally the merging means only adding the assertions from the path condition to the 
   * solver's internal representation.
   * 
   * @param pc PathCondition
   * @param pbtosolve ProblemGeneral
   * @return the merged ProblemGener al object; NULL if problem is unsat
   */
  public static ProblemGeneral parse_old(PathCondition pc, ProblemGeneral pbtosolve) {
    pb=pbtosolve;


    symRealVar = new HashMap<SymbolicReal,Object>();
    symIntegerVar = new HashMap<SymbolicInteger,Object>();
    //result = null;
    tempVars = 0;

    Constraint cRef = (Constraint) pc.header;

    if(pb instanceof IncrementalSolver) {
      //If we use an incremental solver, then we push the context
      //*before* adding the constraint header
    	//Corina: not needed as the push is done in the listener
      //((IncrementalSolver)pb).push();

      //Note that for an incremental solver
      //we only add the constraint header
      if(addConstraint(cRef) == false) {
        return null;
      }
    } else {
      //For a non-incremental solver,
      //we submit the *entire* pc to the solver
      while (cRef != null) {
        if(addConstraint(cRef) == false) {
          return null;
        }
        cRef = (Constraint) cRef.and();
      }
    }

    return pb;
  }

  private static boolean addConstraint(Constraint cRef) {
    boolean constraintResult = true;

    if (cRef instanceof RealConstraint)
      constraintResult= createDPRealConstraint((RealConstraint)cRef);// create choco real constraint
    else if (cRef instanceof LinearIntegerConstraint)
      constraintResult= createDPLinearIntegerConstraint((LinearIntegerConstraint)cRef);// create choco linear integer constraint
    else if (cRef instanceof MixedConstraint)
      // System.out.println("Mixed Constraint");
      constraintResult= createDPMixedConstraint((MixedConstraint)cRef);
    else if (cRef instanceof LogicalORLinearIntegerConstraints) {
      //if (!(pb instanceof ProblemChoco)) {
      //  throw new RuntimeException ("String solving only works with Choco for now");
      //}
      //System.out.println("[SymbolicConstraintsGeneral] reached");
      constraintResult= createDPLinearOrIntegerConstraint((LogicalORLinearIntegerConstraints)cRef);

    } else if (cRef instanceof ArrayConstraint) {
        if (pb instanceof ProblemZ3|| pb instanceof ProblemZ3Optimize || pb instanceof ProblemZ3Incremental || pb instanceof ProblemZ3BitVector || pb instanceof ProblemZ3BitVectorIncremental) {
            constraintResult = createArrayConstraint((ArrayConstraint)cRef);
        } else {
            throw new RuntimeException("## Error : Array constraints only handled by z3. Try specifying a z3 instance as symbolic.dp");
        }
    } else if (cRef instanceof RealArrayConstraint) {
        if (pb instanceof ProblemZ3|| pb instanceof ProblemZ3Optimize || pb instanceof ProblemZ3Incremental || pb instanceof ProblemZ3BitVector || pb instanceof ProblemZ3BitVectorIncremental) {
            constraintResult = createRealArrayConstraint((RealArrayConstraint)cRef);
        } else {
            throw new RuntimeException("## Error : Array constraints only handled by z3. Try specifying a z3 instance as symbolic.dp");
        }
    } 
    else {
      //System.out.println("## Warning: Non Linear Integer Constraint (only coral or z3 can handle it)" + cRef);
      if(pb instanceof ProblemCoral || pb instanceof ProblemZ3|| pb instanceof ProblemZ3Optimize || pb instanceof ProblemZ3BitVector || pb instanceof ProblemZ3Incremental || pb instanceof ProblemZ3BitVectorIncremental)
        constraintResult= createDPNonLinearIntegerConstraint((NonLinearIntegerConstraint)cRef);
      else
        throw new RuntimeException("## Error: Non Linear Integer Constraint not handled " + cRef);
    }

    return constraintResult; //false -> not sat

  }






  // =========================================================
  
  public static ProblemGeneral parse(PathCondition pc, ProblemGeneral pbtosolve) {
	    pb=pbtosolve;
	    

	    symRealVar = new HashMap<SymbolicReal,Object>();
	    symIntegerVar = new HashMap<SymbolicInteger,Object>();
	    symStringVar = new HashMap<StringSymbolic,Object>();
	    //result = null;
	    tempVars = 0;

	    Object dpConstraint = parseToDPConstraint(pc.header, pb);
	    
	    
	    if(pb.isFalse(dpConstraint)) {
	    	return null; // unsat
	    } else {
	    	pb.post(dpConstraint);
	    }

	    return pb;
	  }
  
  public static Object parseToDPConstraint(ParsableConstraint constraint, ProblemGeneral pbtosolve) {
	    pb=pbtosolve;
	    
	    //Constraint constraint = pc.header;
	    Object dpConstraint = null;
	    
	    while (constraint != null) {
	    	Object newDPConstraint = null;
	    	if(constraint instanceof RealConstraint) {
	    		newDPConstraint = buildDPRealConstraint((RealConstraint)constraint);
	    	} else if(constraint instanceof LinearIntegerConstraint) {
	    		newDPConstraint = buildDPLinearIntegerConstraint((LinearIntegerConstraint)constraint);
	    	} else if(constraint instanceof MixedConstraint) {
	    		newDPConstraint = buildDPMixedConstraint((MixedConstraint)constraint);
	    	} else if(constraint instanceof ArrayConstraint) {
	    		newDPConstraint = buildDPArrayConstraint((ArrayConstraint)constraint);
	    	} else if(constraint instanceof RealArrayConstraint) {
	    		newDPConstraint = buildDPRealArrayConstraint((RealArrayConstraint)constraint);
	    	} else if(constraint instanceof LogicalGroupingConstraint) {
	    		LogicalGroupingConstraint logicalGroupedConstraint = ((LogicalGroupingConstraint) constraint);
	    		List<ParsableConstraint> groupedConstraints = logicalGroupedConstraint.getList();
	    		
	    		Object groupedDPConstraints = parseToDPConstraint(groupedConstraints.get(0), pb);
	    		
	    		for(int i = 1; i < groupedConstraints.size(); i++) {
	    			Object subDPConstraint = parseToDPConstraint(groupedConstraints.get(i), pb);
	    			
	    			
	    			if(logicalGroupedConstraint.getOperator() == LogicalGroupingConstraint.Operator.AND) {
	    				groupedDPConstraints = pb.and(groupedDPConstraints, subDPConstraint);
	    				
	    				if(pb.isFalse(groupedDPConstraints)) {
	    					break;
	    				}
	    			} else { // OR operator case
	    				
	    				groupedDPConstraints = pb.or(groupedDPConstraints,  subDPConstraint);
	    				
	    				if(pb.isTrue(groupedDPConstraints)) {
	    					break;
	    				}
	    			}
	    		}
	    		
	    		if(logicalGroupedConstraint.negated) {
	    			groupedDPConstraints = pb.not(groupedDPConstraints);	
	    		}
	    		
	    		newDPConstraint = groupedDPConstraints;
	    	} else if(constraint instanceof StringConstraint) {
	    		newDPConstraint = buildDPStringConstraint((StringConstraint) constraint);
	    	} else if(constraint instanceof NullConstraint) {
	    		NullConstraint nullConstraint = (NullConstraint) constraint;
	    		
	    		Expression expression = nullConstraint.getExpression();
	    		Object dpExpression = null;
	    		
	    		if(expression instanceof StringExpression) {
	    			dpExpression = getExpression((StringExpression) expression);
	    		} else if(expression instanceof SymbolicInteger) {
	    			dpExpression = getExpression((SymbolicInteger) expression);
	    		} else if(expression instanceof IntegerExpression) {
	    			dpExpression = getExpression((IntegerExpression) expression);
	    		} else if(expression instanceof RealExpression) {
	    			dpExpression = getExpression((RealExpression) expression);
	    		}
	    		
	    		if(nullConstraint.getNullIndicator() == NullIndicator.NULL) {
	    			newDPConstraint = pb.makeIsNull(dpExpression);
	    		} else {
	    			newDPConstraint = pb.not(pb.makeIsNull(dpExpression));
	    		}
	    	}
	    	
	    	if(pb.isFalse(newDPConstraint)) { // unsat
	    		return newDPConstraint;
	    	}
	    	
	    	if(dpConstraint == null) { // First iteration
	    		dpConstraint = newDPConstraint;
	    	} else {
	    		dpConstraint = pb.and(dpConstraint, newDPConstraint);
	    	}
	    	
	    	constraint = constraint.and();
	    }
	    
	    return dpConstraint;
	  }
  
  
  
  
  static public Object getExpression(SymbolicInteger exp) {
	  Object dpExpr = null;
	  
	  if(exp instanceof SymbolicCharAtInteger) {
		  // "some string".at(3);
		  SymbolicCharAtInteger sExp = (SymbolicCharAtInteger) exp;
		  
		  StringExpression sourceExp = sExp.getExpression();
		  IntegerExpression indexExp = sExp.getIndex();
		  
		  //mkAt (SeqExpr s, IntExpr index)
		  //dpExpr = pb.makeAt(sourceExp, indexExp);
		  //mkCharAt (SeqExpr s, IntExpr index)
		  dpExpr = pb.makeCharAt(getExpression(sourceExp), getExpression(indexExp));
		  
	  } else if(exp instanceof SymbolicIndexOfInteger) {
		  // "some string".indexOf("str");
		  SymbolicIndexOfInteger sExp = (SymbolicIndexOfInteger) exp;
		  StringExpression sourceExp = sExp.getSource();
		  StringExpression targetExp = sExp.getExpression();
		  
		// ctx.mkIndexOf(arg0, arg1, 0);
		  dpExpr = pb.makeIndexOfStr(getExpression(sourceExp), getExpression(targetExp));
		  
	  } else if(exp instanceof SymbolicIndexOf2Integer) {
		  // "some string".indexOf("str", 2);
		  
		  SymbolicIndexOf2Integer sExp = (SymbolicIndexOf2Integer) exp;
		  StringExpression sourceExp = sExp.getSource();
		  StringExpression targetExp = sExp.getExpression();
		  IntegerExpression minIndexExp = sExp.getMinIndex();
		  
		  // ctx.mkIndexOf(arg0, arg1, arg2);
		  dpExpr = pb.makeIndexOfStr(getExpression(sourceExp), getExpression(targetExp), getExpression(minIndexExp));
		  
	  } else if(exp instanceof SymbolicIndexOfCharInteger) {
		 // "some string".indexOf(0x41);
		  
		  SymbolicIndexOfCharInteger sExp = (SymbolicIndexOfCharInteger) exp;
		  
		  StringExpression sourceExp = sExp.getSource();
		  IntegerExpression targetExp = sExp.getExpression();
		  
		// ctx.mkIndexOf(arg0, arg1, 0);
		  dpExpr = pb.makeIndexOfChar(getExpression(sourceExp), getExpression(targetExp));
		  
	  } else if(exp instanceof SymbolicIndexOfChar2Integer) {
		  // "some string".indexOf(0x41, 3);
		  
		  SymbolicIndexOfChar2Integer sExp = (SymbolicIndexOfChar2Integer) exp;
		  
		  StringExpression sourceExp = sExp.getSource();
		  IntegerExpression targetExp = sExp.getExpression();
		  IntegerExpression minIndexExp = sExp.getMinDist();
		  
		 // ctx.mkIndexOf(arg0, arg1, arg2);
		  dpExpr = pb.makeIndexOfChar(getExpression(sourceExp), getExpression(targetExp), getExpression(minIndexExp));
		  
	  } else if(exp instanceof SymbolicLastIndexOf2Integer) {
		  
	  } else if(exp instanceof SymbolicLastIndexOfChar2Integer) {
		  
	  } else if(exp instanceof SymbolicLastIndexOfCharInteger) {
		  
	  } else if(exp instanceof SymbolicLastIndexOfInteger) {
		  
	  } else if(exp instanceof SymbolicLengthInteger) {
		  SymbolicLengthInteger sExp = (SymbolicLengthInteger) exp;
		  
		  StringExpression strExpr = sExp.getExpression();
		  
		  // ctx.mkLength(arg0)
		  dpExpr = pb.makeLength(getExpression(strExpr));
	  } else if(exp instanceof SymbolicHashCode) {
		  SymbolicHashCode sExp = (SymbolicHashCode) exp;
		  
		  StringExpression strExpr = sExp.getExpression();
		  
		  dpExpr = pb.makeHashCode(getExpression(strExpr));
		  
	  } else {
		  dpExpr = symIntegerVar.get(exp);
	      if (dpExpr == null) {
	    	  dpExpr = pb.makeIntVar(((SymbolicInteger)exp).getName(),
	            ((SymbolicInteger)exp)._min, ((SymbolicInteger)exp)._max);
	        symIntegerVar.put((SymbolicInteger)exp, dpExpr);
	      }
	  }
	  
	  return dpExpr;
  }
  
  static public Object getExpression(StringExpression exp) {
	  Object dpExpression = null;
	  
	  
	  if(exp instanceof DerivedStringExpression) {
		  DerivedStringExpression derivedExp = (DerivedStringExpression) exp;
		  
		  StringExpression left = derivedExp.left;
		  Object leftDP = left != null? getExpression(left) : null;
		  
		  StringOperator operator = derivedExp.op;
		  
		  StringExpression right = derivedExp.right;
		  Object rightDP = right != null? getExpression(right) : null;
		  
		  Expression[] operands = derivedExp.oprlist;
		  Object[] operandsDP = null;
		  
		  if(operands != null) {
			  operandsDP = new Object[operands.length];
			  
			  for(int i = 0; i < operands.length; i++) {
				  Expression operand = operands[i];
				  
				  if(operand != null) {
					  if(operand instanceof IntegerConstant) {
						  operandsDP[i] = pb.makeIntConst(((IntegerConstant) operand).value);
					  } else if(operand instanceof RealConstant) {
						  operandsDP[i] = pb.makeRealConst(((RealConstant) operand).value);
					  } else if(operand instanceof IntegerExpression) {
						  operandsDP[i] = getExpression((IntegerExpression) operand);
					  } else if(operand instanceof RealExpression) {
						  operandsDP[i] = getExpression((RealExpression) operand);
					  } else if(operand instanceof StringExpression) {
						  operandsDP[i] = getExpression((StringExpression) operand);
					  }
				  }
			  }
		  }

		  
		  switch(operator) {
			  case CONCAT:
				    dpExpression = pb.makeConcat(leftDP, rightDP);
			  		break;
			  case REPLACE:
			  		break;
			  case TRIM:
			  		break;
			  case SUBSTRING:
				   Object originalStrDPExpr = operandsDP[0];
				   Object startIndexDPExpr = operandsDP[1];
				   Object endIndexDPExpr = operandsDP[2];
				   
				   Object lengthDPExpr = pb.minus(endIndexDPExpr, startIndexDPExpr);
				   
				   dpExpression = pb.makeSubstring(originalStrDPExpr, startIndexDPExpr, lengthDPExpr);
				  
			  		break;
			  case REPLACEFIRST:
				  // ctx.mkReplace(arg0, arg1, arg2) 
			  		break;
			  case REPLACEALL:  
			  		break;
			  case TOLOWERCASE:
			  		break;
			  case TOUPPERCASE:
			  		break;
			  case VALUEOF:
				  	Object numDPExpr = operandsDP[0];
				  	
				  	if(operands[0] instanceof IntegerExpression) {
				  		dpExpression = pb.makeIntToString(numDPExpr);
				  	}
				  	
			  		break;
		  }
		  
		  
	  } else if (exp instanceof StringConstant) {
		  StringConstant strExp = (StringConstant) exp;
		  dpExpression = pb.makeStringConst(strExp.value);
		  
	  } else if(exp instanceof StringSymbolic) {
		  StringSymbolic strSymExp = (StringSymbolic) exp;
		  
		  dpExpression = symStringVar.get(strSymExp);
		  
		  if(dpExpression == null) {
			//IntegerExpression lengthExpr = strSymExp._length();
			//Object lengthDPExpr = getExpression(lengthExpr);
			
			dpExpression = pb.makeStringVar(strSymExp.getName());
			symStringVar.put(strSymExp, dpExpression);
		  }
		  
	  } else {
		  assert(false);
	  }
	  
	  return dpExpression;
  }
  
  static public Object buildDPStringConstraint(StringConstraint constraint) {
	  
	  StringExpression left = constraint.getLeft();
	  Object leftDP = left == null? null : getExpression(left);
	  
	  StringComparator comp = constraint.getComparator();
	  
	  StringExpression right = constraint.getRight();
	  Object rightDP = right == null? null : getExpression(right);
	  
	  Object constraintDP = null;
	  
	  switch(comp) {
	  	case EQ:
	  	case EQUALS:
	  		constraintDP = pb.eq(leftDP, rightDP);
	  		break;
	  	case NE:
	  	case NOTEQUALS:
	  		constraintDP = pb.neq(leftDP, rightDP);
	  		break;
	  	case EQUALSIGNORECASE:
	  		break;
	  	case NOTEQUALSIGNORECASE:
	  		break;
	  	case STARTSWITH:
	  		constraintDP = pb.makeStartsWith(leftDP, rightDP);
	  		break;
	  	case NOTSTARTSWITH:
	  		constraintDP = pb.not(pb.makeStartsWith(leftDP, rightDP));
	  		break;
	  	case ENDSWITH:
	  		constraintDP = pb.makeEndsWith(leftDP, rightDP);
	  		break;
	  	case NOTENDSWITH:
	  		constraintDP = pb.not(pb.makeEndsWith(leftDP, rightDP));
	  		break;
	  	case CONTAINS:
	  		constraintDP = pb.makeConstains(leftDP, rightDP);
	  		break;
	  	case NOTCONTAINS:
	  		constraintDP = pb.not(pb.makeConstains(leftDP, rightDP));
	  		break;
	  	case ISINTEGER:
	  		// Use regex to do this
	  		break;
	  	case NOTINTEGER:
	  		break;
	  	case ISFLOAT:
	  		break;
	  	case NOTFLOAT:
	  		break;
	  	case ISLONG:
	  		break;
	  	case NOTLONG:
	  		break;
	  	case ISDOUBLE:
	  		break;
	  	case NOTDOUBLE:
	  		break;
	  	case ISBOOLEAN:
	  		break;
	  	case NOTBOOLEAN:
	  		break;
	  	case EMPTY:
	  		constraintDP = pb.makeIsEmpty(rightDP);
	  		break;
	  	case NOTEMPTY:
	  		constraintDP = pb.not(pb.makeIsEmpty(rightDP));
	  		break;
	  	case MATCHES:
	  		break;
	  	case NOMATCHES:
	  		break;
	  	case REGIONMATCHES:
	  		break;
	  	case NOREGIONMATCHES:
	  		break;
	  } 
	  
	  return constraintDP;
  }
  
  
  static public Object buildDPMixedConstraint(MixedConstraint cRef) {
	  Comparator c_compRef = cRef.getComparator();
	    RealExpression c_leftRef = (RealExpression)cRef.getLeft();
	    IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();
	    assert (c_compRef == Comparator.EQ);
	    
	    Object constraint = null;

	    if (c_leftRef instanceof SymbolicReal && c_rightRef instanceof SymbolicInteger) {
	      //pb.post(new MixedEqXY((RealVar)(getExpression(c_leftRef)),(IntDomainVar)(getExpression(c_rightRef))));
	    	constraint = pb.mixed(getExpression(c_leftRef),getExpression(c_rightRef));
	    }
	    else if (c_leftRef instanceof SymbolicReal) { // c_rightRef is an IntegerExpression
	      Object tmpi = pb.makeIntVar(c_rightRef + "_" + c_rightRef.hashCode(),(int)(((SymbolicReal)c_leftRef)._min), (int)(((SymbolicReal)c_leftRef)._max));
	      if (c_rightRef instanceof IntegerConstant)
	    	  constraint = pb.eq(((IntegerConstant)c_rightRef).value,tmpi);
	      else
	    	  constraint = pb.eq(getExpression(c_rightRef),tmpi);
	      //pb.post(new MixedEqXY((RealVar)(getExpression(c_leftRef)),tmpi));
	      constraint = pb.and(constraint, pb.mixed(getExpression(c_leftRef),tmpi));
	    }
	    else if (c_rightRef instanceof SymbolicInteger) { // c_leftRef is a RealExpression
	      Object tmpr = pb.makeRealVar(c_leftRef + "_" + c_leftRef.hashCode(), ((SymbolicInteger)c_rightRef)._min, ((SymbolicInteger)c_rightRef)._max);
	      if(c_leftRef instanceof RealConstant)
	    	  constraint = pb.eq(tmpr, ((RealConstant)c_leftRef).value);
	      else
	    	  constraint = pb.eq(tmpr, getExpression(c_leftRef));
	      //pb.post(new MixedEqXY(tmpr,(IntDomainVar)(getExpression(c_rightRef))));
	      constraint = pb.and(constraint, pb.mixed(tmpr,getExpression(c_rightRef)));
	    }
	    else
	      assert(false); // should not be reachable

	    return constraint;
  }
  
  static public Object buildDPRealConstraint(RealConstraint cRef) {
	  Comparator c_compRef = cRef.getComparator();
	    RealExpression c_leftRef = (RealExpression)cRef.getLeft();
	    RealExpression c_rightRef = (RealExpression)cRef.getRight();
	    
	    Object dpConstraint = null;
	    
	    switch(c_compRef){
	      case EQ:
	        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
	          if (!(((RealConstant) c_leftRef).value == ((RealConstant) c_rightRef).value))
	            return pb.makeFalse();
	          else
	            return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof RealConstant) {
	        	dpConstraint = pb.eq(((RealConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof RealConstant) {
	        	dpConstraint = pb.eq(getExpression(c_leftRef),((RealConstant)c_rightRef).value);
	        }
	        else
	        	dpConstraint = pb.eq(getExpression(c_leftRef),getExpression(c_rightRef));
	        break;
	      case NE:
	        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
	          if (!(((RealConstant) c_leftRef).value != ((RealConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof RealConstant) {
	        	dpConstraint = pb.neq(((RealConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof RealConstant) {
	        	dpConstraint = pb.neq(getExpression(c_leftRef),((RealConstant)c_rightRef).value);
	        }
	        else
	        	dpConstraint = pb.neq(getExpression(c_leftRef),getExpression(c_rightRef));
	        break;
	      case LT:
	        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
	          if (!(((RealConstant) c_leftRef).value < ((RealConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof RealConstant) {
	        	dpConstraint = pb.lt(((RealConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof RealConstant) {
	        	dpConstraint = pb.lt(getExpression(c_leftRef),((RealConstant)c_rightRef).value);
	        }
	        else
	        	dpConstraint = pb.lt(getExpression(c_leftRef),getExpression(c_rightRef));
	        break;
	      case GE:
	        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
	          if (!(((RealConstant) c_leftRef).value >= ((RealConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof RealConstant) {
	        	dpConstraint = pb.geq(((RealConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof RealConstant) {
	        	dpConstraint = pb.geq(getExpression(c_leftRef),((RealConstant)c_rightRef).value);
	        }
	        else
	        	dpConstraint = pb.geq(getExpression(c_leftRef),getExpression(c_rightRef));
	        break;
	      case LE:
	        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
	          if (!(((RealConstant) c_leftRef).value <= ((RealConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof RealConstant) {
	        	dpConstraint = pb.leq(((RealConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof RealConstant) {
	        	dpConstraint = pb.leq(getExpression(c_leftRef),((RealConstant)c_rightRef).value);
	        }
	        else
	        	dpConstraint = pb.leq(getExpression(c_leftRef),getExpression(c_rightRef));
	        break;
	      case GT:
	        if (c_leftRef instanceof RealConstant && c_rightRef instanceof RealConstant) {
	          if (!(((RealConstant) c_leftRef).value > ((RealConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof RealConstant) {
	        	dpConstraint = pb.gt(((RealConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof RealConstant) {
	        	dpConstraint = pb.gt(getExpression(c_leftRef),((RealConstant)c_rightRef).value);
	        }
	        else
	        	dpConstraint = pb.gt(getExpression(c_leftRef),getExpression(c_rightRef));
	        break;
	    }
	    return dpConstraint;
  }
  
  // TODO: add getExpression()s to parse subclasses of SymbolicInteger (e.g., SymbolicCharAtInteger)
  static public Object buildDPLinearIntegerConstraint(LinearIntegerConstraint cRef) {
	  Comparator c_compRef = cRef.getComparator();

	    IntegerExpression c_leftRef = (IntegerExpression)cRef.getLeft();
	    IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();

	    switch(c_compRef){
	      case EQ:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value == ((IntegerConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	          return pb.eq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	          return pb.eq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	          return pb.eq(getExpression(c_leftRef),getExpression(c_rightRef));

	      case NE:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value != ((IntegerConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	          return pb.neq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	          return pb.neq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.neq(getExpression(c_leftRef),getExpression(c_rightRef));

	      case LT:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value < ((IntegerConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.lt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.lt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.lt(getExpression(c_leftRef),getExpression(c_rightRef));

	      case GE:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value >= ((IntegerConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.geq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.geq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.geq(getExpression(c_leftRef),getExpression(c_rightRef));

	      case LE:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value <= ((IntegerConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.leq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.leq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.leq(getExpression(c_leftRef),getExpression(c_rightRef));

	      case GT:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value > ((IntegerConstant) c_rightRef).value))
	        	  return pb.makeFalse();
	          else
	        	  return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.gt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.gt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.gt(getExpression(c_leftRef),getExpression(c_rightRef));

	    }
	    // We shouldn't arrive here
	    return null;
  }
  
  static public Object buildDPNonLinearIntegerConstraint(NonLinearIntegerConstraint cRef) {
	  Comparator c_compRef = cRef.getComparator();

	    IntegerExpression c_leftRef = (IntegerExpression)cRef.getLeft();
	    IntegerExpression c_rightRef = (IntegerExpression)cRef.getRight();

	    switch(c_compRef){
	      case EQ:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value == ((IntegerConstant) c_rightRef).value))
	            return pb.makeFalse();
	          else
	            return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	          return pb.eq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.eq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.eq(getExpression(c_leftRef),getExpression(c_rightRef));
	        
	      case NE:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value != ((IntegerConstant) c_rightRef).value))
	            return pb.makeFalse();
	          else
	            return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.neq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.neq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.neq(getExpression(c_leftRef),getExpression(c_rightRef));
	        
	      case LT:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value < ((IntegerConstant) c_rightRef).value))
	            return pb.makeFalse();
	          else
	            return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.lt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.lt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.lt(getExpression(c_leftRef),getExpression(c_rightRef));
	        
	      case GE:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value >= ((IntegerConstant) c_rightRef).value))
	            return pb.makeFalse();
	          else
	            return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.geq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.geq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.geq(getExpression(c_leftRef),getExpression(c_rightRef));
	        
	      case LE:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value <= ((IntegerConstant) c_rightRef).value))
	            return pb.makeFalse();
	          else
	            return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.leq(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.leq(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.leq(getExpression(c_leftRef),getExpression(c_rightRef));
	        
	      case GT:
	        if (c_leftRef instanceof IntegerConstant && c_rightRef instanceof IntegerConstant) {
	          if (!(((IntegerConstant) c_leftRef).value > ((IntegerConstant) c_rightRef).value))
	            return pb.makeFalse();
	          else
	            return pb.makeTrue();
	        }
	        else if (c_leftRef instanceof IntegerConstant) {
	        	return pb.gt(((IntegerConstant)c_leftRef).value,getExpression(c_rightRef));
	        }
	        else if (c_rightRef instanceof IntegerConstant) {
	        	return pb.gt(getExpression(c_leftRef),((IntegerConstant)c_rightRef).value);
	        }
	        else
	        	return pb.gt(getExpression(c_leftRef),getExpression(c_rightRef));
	        
	    }
	    return null;
  }
  
  static public Object buildDPArrayConstraint(ArrayConstraint cRef) {
	  Comparator c_compRef = cRef.getComparator();

	    SelectExpression selex = null;
	        StoreExpression stoex = null;
	        IntegerExpression sel_right = null;
	        ArrayExpression sto_right = null;
	        InitExpression initex = null;
	        if (cRef.getLeft() instanceof SelectExpression) {
	          selex = (SelectExpression)cRef.getLeft();
	          sel_right = (IntegerExpression)cRef.getRight();
	        } else if (cRef.getLeft() instanceof StoreExpression) {
	           stoex = (StoreExpression)cRef.getLeft();
	           sto_right = (ArrayExpression)cRef.getRight();
	        } else if (cRef.getLeft() instanceof InitExpression) {
	           initex = (InitExpression)cRef.getLeft();
	        } else {
	            throw new RuntimeException("ArrayConstraint is not select or store or init");
	        }

	        switch(c_compRef) {
	        case EQ:

	            if (selex != null && sel_right != null) {
	                // The array constraint is a select
	                ArrayExpression ae = (ArrayExpression) selex.arrayExpression;
	                if(ae instanceof StringByteArrayExpression) {
	                	// stringExpr[indexExpr] == sel_right
	                	
	                	IntegerExpression indexExpr = selex.indexExpression;
	                	StringExpression stringExpr = ((StringByteArrayExpression) ae).getStringExpression();
	                	
	                	Object indexDPExpr;
	                	
	                	if(indexExpr instanceof IntegerConstant) {
	                		indexDPExpr = pb.makeIntConst(((IntegerConstant) indexExpr).value);
	                	} else {
	                		indexDPExpr = getExpression(indexExpr);
	                	}
	                	
	                	// stringExpr[indexExpr]
	                	Object selectedCharDPExpr = pb.makeCharAt(getExpression(stringExpr), indexDPExpr);
	                	
	                	Object rightSideDPExpr;
	                	
	                	if(sel_right instanceof IntegerConstant) {
	                		rightSideDPExpr = pb.makeIntConst(((IntegerConstant) sel_right).value);
	                	} else {
	                		rightSideDPExpr = getExpression(sel_right);
	                	}
	                	
	                	return pb.eq(selectedCharDPExpr, rightSideDPExpr);
	                			
	                } else {
	                
	                
	                return pb.eq(pb.select(pb.makeArrayVar(ae.getName()),
	                  (selex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)selex.indexExpression).value) :
	getExpression(selex.indexExpression)),
	                  (sel_right instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)sel_right).value) :
	getExpression(sel_right));
	                
	                }
	                
	            }
	            if (stoex != null && sto_right != null) {
	                // The array constraint is a store
	                ArrayExpression ae = (ArrayExpression) stoex.arrayExpression;
	                ArrayExpression newae = (ArrayExpression) sto_right;
	                
	                if(ae instanceof StringByteArrayExpression) {
	                	// (stringExpression[indexExpr] = value) == sto_right
	                	StringExpression stringExpression = ((StringByteArrayExpression) ae).getStringExpression();	                	
	                	IntegerExpression indexExpr = stoex.indexExpression;
	                	IntegerExpression value = stoex.value;
	                	
	                	Object indexDPExpr;
	                	
	                	if(indexExpr instanceof IntegerConstant) {
	                		indexDPExpr = pb.makeIntConst(((IntegerConstant) indexExpr).value);
	                	} else {
	                		indexDPExpr = getExpression(indexExpr);
	                	}
	                	
	                	Object valueDPExpr;
	                	
	                	if(value instanceof IntegerConstant) {
	                		valueDPExpr = pb.makeIntConst(((IntegerConstant) value).value);
	                	} else {
	                		valueDPExpr = getExpression(value);
	                	}
	                	
	                	Object storeCharAtDPExpr = pb.makeStoreCharAt(getExpression(stringExpression), indexDPExpr, valueDPExpr);
	                	
	                    assert(newae instanceof StringByteArrayExpression);
	                	
	                	StringByteArrayExpression rightSideExpr = (StringByteArrayExpression) newae;
	                	
	                	Object rightSideDPExpr = getExpression(rightSideExpr.getStringExpression());
	                	
	                	return pb.eq(storeCharAtDPExpr, rightSideDPExpr);
	                	
	                } else {
	                	return pb.eq(pb.store(pb.makeArrayVar(ae.getName()),
	      	                  (stoex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)stoex.indexExpression).value) :
	      	getExpression(stoex.indexExpression),
	      	                  (stoex.value instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)stoex.value).value) :
	      	getExpression(stoex.value)),
	      	                   pb.makeArrayVar(newae.getName()));
	                }
	                
	                
	                
	            }
	            if (initex != null) {
	              // The array constraint is an initialization
	              ArrayExpression ae = (ArrayExpression) initex.arrayExpression;
	              IntegerConstant init_value = new IntegerConstant(initex.isRef? -1 : 0);
	              return pb.init_array(pb.makeArrayVar(ae.getName()), pb.makeIntConst(init_value.value));
	              
	            }
	            throw new RuntimeException("ArrayConstraint is not correct select or store or init");
	        case NE:
	            if (selex != null && sel_right != null) {
	                // The array constraint is a select
	                ArrayExpression ae = (ArrayExpression) selex.arrayExpression;
	                return pb.neq(pb.select(pb.makeArrayVar(ae.getName()), getExpression(selex.indexExpression)),
	getExpression(sel_right));
	                
	            }
	            if (stoex != null && sto_right != null) {
	                // The array constraint is a store
	                ArrayExpression ae = (ArrayExpression)stoex.arrayExpression;
	                ArrayExpression newae = (ArrayExpression) sto_right;
	                return pb.neq(pb.store(pb.makeArrayVar(ae.getName()), getExpression(stoex.indexExpression),
	getExpression(stoex.value)), newae);
	                
	            }
	            throw new RuntimeException("ArrayConstraint is not correct select or store");
	        default:
	            throw new RuntimeException("ArrayConstraint is not select or store");
	        }
  }
  
  static public Object buildDPRealArrayConstraint(RealArrayConstraint cRef) {
	  final Comparator c_compRef = cRef.getComparator();


      SelectExpression selex = null;
      RealStoreExpression stoex = null;
      RealExpression sel_right = null;
      ArrayExpression sto_right = null;
      if (cRef.getLeft() instanceof SelectExpression) {
           selex = (SelectExpression)cRef.getLeft();
           sel_right = (RealExpression)cRef.getRight();
      } else if (cRef.getLeft() instanceof RealStoreExpression) {
         stoex = (RealStoreExpression)cRef.getLeft();
         sto_right = (ArrayExpression)cRef.getRight();
      } else {
         throw new RuntimeException("ArrayConstraint is not select or store");
      }

      switch(c_compRef) {
      case EQ:

          if (selex != null && sel_right != null) {
              // The array constraint is a select
              ArrayExpression ae = selex.arrayExpression;
              return pb.eq(pb.realSelect(pb.makeRealArrayVar(ae.getName()),
                (selex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)selex.indexExpression).value) :
getExpression(selex.indexExpression)),
                (sel_right instanceof RealConstant) ? pb.makeRealConst(((RealConstant)sel_right).value) :
getExpression(sel_right));
               
          }
          if (stoex != null && sto_right != null) {
              // The array constraint is a store
              ArrayExpression ae = stoex.arrayExpression;
              ArrayExpression newae = sto_right;
              return pb.eq(pb.realStore(pb.makeRealArrayVar(ae.getName()),
                (stoex.indexExpression instanceof IntegerConstant) ? pb.makeIntConst(((IntegerConstant)stoex.indexExpression).value) :
getExpression(stoex.indexExpression),
                (stoex.value instanceof RealConstant) ? pb.makeRealConst(((RealConstant)stoex.value).value) :
getExpression(stoex.value)),
                 pb.makeRealArrayVar(newae.getName()));
               
          }
          throw new RuntimeException("ArrayConstraint is not correct select or store");
      case NE:
          if (selex != null && sel_right != null) {
              // The array constraint is a select
              ArrayExpression ae = selex.arrayExpression;
              return pb.neq(pb.realSelect(pb.makeRealArrayVar(ae.getName()), getExpression(selex.indexExpression)),
getExpression(sel_right));
               
          }
          if (stoex != null && sto_right != null) {
              // The array constraint is a store
              ArrayExpression ae = stoex.arrayExpression;
              ArrayExpression newae = sto_right;
              return pb.neq(pb.realStore(pb.makeRealArrayVar(ae.getName()), getExpression(stoex.indexExpression),
getExpression(stoex.value)), newae);
               
          }
          throw new RuntimeException("ArrayConstraint is not correct select or store");
      default:
          throw new RuntimeException("ArrayConstraint is not select or store");
      }
  }

}
