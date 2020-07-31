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
//Copyright (C) 2006 United States Government as represented by the
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

package gov.nasa.jpf.symbc.numeric.solvers;

import java.io.PrintWriter;
import java.util.*;

//TODO: problem: we do not distinguish between ints and reals?
// still needs a lot of work: do not use!


import com.microsoft.z3.*;

import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import symlib.Util;

public class ProblemZ3 extends ProblemGeneral {

  //This class acts as a safeguard to prevent
  //issues when referencing ProblemZ3 in case the z3 libs are
  //not on the ld_library_path. If the
  //Z3 solver object and context were class fields,
  //we would likely encounter a linker error
	private static class Z3Wrapper {
		private Context ctx;
		private Solver solver;

		private static Z3Wrapper instance = null;

		public static Z3Wrapper getInstance() {
			if (instance != null) {
				return instance;
			}
			return instance = new Z3Wrapper();
		}

		private Z3Wrapper() {
			HashMap<String, String> cfg = new HashMap<String, String>();
			cfg.put("model", "true");
			ctx = new Context(cfg);
			solver = ctx.mkSolver();
		}

		public Solver getSolver() {
			return this.solver;
		}

		public Context getCtx() {
			return this.ctx;
		}
	}

	private Solver solver;
	private Context ctx;
	private static final int BIT_VEC_LENGTH = 16;

	// Do we use the floating point theory or linear arithmetic over reals
	private boolean useFpForReals = false;

	public ProblemZ3() {
		Z3Wrapper z3 = Z3Wrapper.getInstance();
		solver = z3.getSolver();
		ctx = z3.getCtx();
		solver.push();
		useFpForReals = SymbolicInstructionFactory.fp;
	}

	public void cleanup() {
		int scopes = solver.getNumScopes();
		if (scopes > 0) {
			solver.pop(scopes);
		}
	}

	public Object makeIntVar(String name, long min, long max) {
		try {
			IntExpr intConst = ctx.mkIntConst(name);
			solver.add(ctx.mkGe(intConst, ctx.mkInt(min)));
			solver.add(ctx.mkLe(intConst, ctx.mkInt(max)));
			return intConst;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in makeIntVar: \n" + e);
	    }
	}

	public Object makeRealVar(String name, double min, double max) {
		try {
			if (useFpForReals) {
				Expr expr = ctx.mkConst(name, ctx.mkFPSortDouble());
				solver.add(ctx.mkFPGt((FPExpr) expr, ctx.mkFP(min, ctx.mkFPSortDouble())));
				solver.add(ctx.mkFPLt((FPExpr) expr, ctx.mkFP(max, ctx.mkFPSortDouble())));
				return expr;
			} else {
				RealExpr expr = ctx.mkRealConst(name);
				solver.add(ctx.mkGe(expr, ctx.mkReal("" + min)));
				solver.add(ctx.mkLe(expr, ctx.mkReal("" + max)));
				return expr;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
	    }
	}

	public Object eq(long value, Object exp){
		try {
			return ctx.mkEq( ctx.mkInt(value), (Expr)exp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
	    }
	}

	public Object eq(Object exp, long value){

		return ctx.mkEq(ctx.mkInt(value), (Expr)exp);
	}

	// should we use Expr or ArithExpr?
	public Object eq(Object exp1, Object exp2){
		try{
			if(exp1 instanceof BitVecExpr) {
				exp1 = ctx.mkBV2Int((BitVecExpr)exp1, false);
			}
			
			if(exp2 instanceof BitVecExpr) {
				exp2 = ctx.mkBV2Int((BitVecExpr)exp2, false);
			}
			
			return  ctx.mkEq((Expr)exp1, (Expr)exp2);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	// TODO: should convert double to rational
//	public Object eq(double value, Object exp){
//		try{
//			return  ctx.MkEq(ctx.MkReal(arg0, arg1), (RealExpr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}

//	public Object eq(Object exp, double value){
//		try{
//			return  ctx.MkEq(ctx.MkReal(arg0, arg1), (RealExpr)exp);;
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}

	public Object neq(long value, Object exp){
		return ctx.mkNot(ctx.mkEq(ctx.mkInt(value), (Expr)exp));
	}


	public Object neq(Object exp, long value){
		return ctx.mkNot(ctx.mkEq(ctx.mkInt(value), (Expr) exp));
	}

	public Object neq(Object exp1, Object exp2){
		try{
			if(exp1 instanceof BitVecExpr) {
				exp1 = ctx.mkBV2Int((BitVecExpr)exp1, false);
			}
			
			if(exp2 instanceof BitVecExpr) {
				exp2 = ctx.mkBV2Int((BitVecExpr)exp2, false);
			}
			
			return  ctx.mkNot(ctx.mkEq((Expr)exp1, (Expr)exp2));
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
	    }
	}

	public Object not(Object exp1){
		try{
			return  ctx.mkNot((BoolExpr)exp1);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	// TODO: convert doubles to rationals
//	public Object neq(double value, Object exp){
//		try{
//			return  vc.notExpr(vc.eqExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
//	public Object neq(Object exp, double value){
//		try{
//			return  vc.notExpr(vc.eqExpr((Expr)exp, vc.ratExpr(Double.toString(value), base)));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}

	public Object leq(long value, Object exp){
		try{
			return  ctx.mkLe(ctx.mkInt(value), (IntExpr)exp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object leq(Object exp, long value){
		try{
			return  ctx.mkLe((IntExpr)exp,ctx.mkInt(value));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object leq(Object exp1, Object exp2){
		try{
			return  ctx.mkLe((ArithExpr)exp1, (ArithExpr)exp2);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

//	public Object leq(double value, Object exp){
//		try{
//			return  vc.leExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
//	public Object leq(Object exp, double value){
//		try{
//			return  vc.leExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
	public Object geq(long value, Object exp){
		try{
			return  ctx.mkGe(ctx.mkInt(value),(IntExpr)exp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object geq(Object exp, long value){
		try{
			IntExpr expInt = exp instanceof BitVecExpr? ctx.mkBV2Int((BitVecExpr)exp, false) : (IntExpr) exp;
			return  ctx.mkGe(expInt,ctx.mkInt(value));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object geq(Object exp1, Object exp2){
		try{
			//return  ctx.mkGe((ArithExpr)exp1,(ArithExpr)exp2);
			
			ArithExpr exp1Arith = exp1 instanceof BitVecExpr? ctx.mkBV2Int((BitVecExpr)exp1, false) : (ArithExpr) exp1;
			ArithExpr exp2Arith = exp2 instanceof BitVecExpr? ctx.mkBV2Int((BitVecExpr)exp2, false) : (ArithExpr) exp2;
			return  ctx.mkGe(exp1Arith, exp2Arith);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

//	public Object geq(double value, Object exp){
//		try{
//			return  vc.geExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
//	public Object geq(Object exp, double value){
//		try{
//			return  vc.geExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
	public Object lt(long value, Object exp){
		try{
			if(exp instanceof IntExpr) {
				return ctx.mkLt(ctx.mkInt(value),(IntExpr)exp);
			} else {
				BitVecExpr valueBV = ctx.mkBV(value, BIT_VEC_LENGTH);
				BitVecExpr expBV = (BitVecExpr) exp;
				return ctx.mkBVULT(valueBV, expBV);
			}			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object lt(Object exp, long value){
		try{
			if(exp instanceof IntExpr) {
				return  ctx.mkLt((IntExpr)exp,ctx.mkInt(value));
			} else {
				BitVecExpr valueBV = ctx.mkBV(value, BIT_VEC_LENGTH);
				BitVecExpr expBV = (BitVecExpr) exp;
				return ctx.mkBVULT(expBV, valueBV);
			}	
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object lt(Object exp1, Object exp2){
		try{
			ArithExpr exp1Arith = exp1 instanceof BitVecExpr? ctx.mkBV2Int((BitVecExpr)exp1, false) : (ArithExpr) exp1;
			ArithExpr exp2Arith = exp2 instanceof BitVecExpr? ctx.mkBV2Int((BitVecExpr)exp2, false) : (ArithExpr) exp2;
			return  ctx.mkLt(exp1Arith, exp2Arith);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

//	public Object lt(double value, Object exp){
//		try{
//			return  vc.ltExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
//	public Object lt(Object exp, double value){
//		try{
//			return  vc.ltExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
//
	public Object gt(long value, Object exp){
		try{
			return  ctx.mkGt(ctx.mkInt(value),(IntExpr)exp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object gt(Object exp, long value){
		try{
			return  ctx.mkGt((IntExpr)exp,ctx.mkInt(value));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

	public Object gt(Object exp1, Object exp2){
		try{
			return  ctx.mkGt((ArithExpr)exp1,(ArithExpr)exp2);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);

	    }
	}

//	public Object implies(Object exp1, Object exp2){
//		try{
//			return  vc.impliesExpr((Expr)exp1, (Expr)exp2);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}

//	public Object gt(double value, Object exp){
//		try{
//			return  vc.gtExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
//	public Object gt(Object exp, double value){
//		try{
//			return  vc.gtExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//
//	    }
//	}
//
//
//
//
	public Object plus(long value, Object exp) {
		try{
			return  ctx.mkAdd(new ArithExpr[] { ctx.mkInt(value), (IntExpr)exp});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object plus(Object exp, long value) {
		try{
			return  ctx.mkAdd(new ArithExpr[] { ctx.mkInt(value), (IntExpr)exp});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object plus(Object exp1, Object exp2) {
		try{
			return  ctx.mkAdd(new ArithExpr[] { (ArithExpr)exp1, (ArithExpr)exp2});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

//	public Object plus(double value, Object exp) {
//		try{
//			return  vc.plusExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}
//
//	public Object plus(Object exp, double value) {
//		try{
//			return  vc.plusExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}

	public Object minus(long value, Object exp) {
		try{
			return  ctx.mkSub(new ArithExpr[] { ctx.mkInt(value), (IntExpr)exp});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object minus(Object exp, long value) {
		try{
			return  ctx.mkSub(new ArithExpr[] {(IntExpr)exp, ctx.mkInt(value)});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object minus(Object exp1, Object exp2) {
		try{
			return  ctx.mkSub(new ArithExpr[] { (ArithExpr)exp1, (ArithExpr)exp2});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

//	public Object minus(double value, Object exp) {
//		try{
//			return  vc.minusExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}
//
//	public Object minus(Object exp, double value) {
//		try{
//			return  vc.minusExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}

	public Object mult(long value, Object exp) {
		try{
			return  ctx.mkMul(new ArithExpr[] {(IntExpr)exp, ctx.mkInt(value)});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object mult(Object exp, long value) {
		try{
			return  ctx.mkMul(new ArithExpr[] {(IntExpr)exp, ctx.mkInt(value)});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object mult(Object exp1, Object exp2) {
		try{
			return  ctx.mkMul(new ArithExpr[] {(ArithExpr)exp1, (ArithExpr)exp2});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}
//	public Object mult(double value, Object exp) {
//		try{
//			return  vc.multExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}
//	public Object mult(Object exp, double value) {
//		try{
//			return  vc.multExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}
//
//

	public Object div(long value, Object exp) {
		try{
			return  ctx.mkDiv(ctx.mkInt(value), (IntExpr)exp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object div(Object exp, long value) {
		try{
			return  ctx.mkDiv((IntExpr)exp,ctx.mkInt(value));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object div(Object exp1, Object exp2) {
		try{
			return  ctx.mkDiv((ArithExpr)exp1,(ArithExpr)exp2);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	public Object rem(Object exp, long value) {// added by corina
		try{

			return  ctx.mkRem((IntExpr) exp, ctx.mkInt(value));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}
	public Object rem(long value, Object exp) {// added by corina
		try{

			return  ctx.mkRem(ctx.mkInt(value), (IntExpr) exp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}
	public Object rem(Object exp1, Object exp2) {// added by corina
		try{
			if(exp2 instanceof Integer)
				return  ctx.mkRem((IntExpr)exp1,ctx.mkInt((Integer)exp2));
			return  ctx.mkRem((IntExpr) exp1, (IntExpr) exp2);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

//	public Object div(double value, Object exp) {
//		try{
//			return  vc.divideExpr(vc.ratExpr(Double.toString(value), base), (Expr)exp);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}
//	public Object div(Object exp, double value) {
//		try{
//			return  vc.divideExpr((Expr)exp, vc.ratExpr(Double.toString(value), base));
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
//		}
//	}

    public long getIntValue(Object dpVar) {
        try {
            Model model = solver.getModel();
            return Long.parseLong((model.evaluate((IntExpr) dpVar, false)).toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
        }
    }

	public Boolean solve() {
        try {
            if (Status.SATISFIABLE == solver.check()) {
                return true;
            } else {
                return false;
            }
        } catch(Exception e){
        	e.printStackTrace();
        	throw new RuntimeException("## Error Z3: " + e);
        }
	}

	public void post(Object constraint) {
		try{
			solver.add((BoolExpr)constraint);
		} catch (Exception e) {
			e.printStackTrace();
        	throw new RuntimeException("## Error posting constraint to Z3 \n" + e);
	    }
	}

	@Override
	public Object eq(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPEq(ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkEq(ctx.mkReal("" + value), (Expr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: eq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object eq(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPEq((FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkEq((Expr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: eq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object neq(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkNot(ctx.mkFPEq(ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp));
			} else {
				return ctx.mkNot(ctx.mkEq(ctx.mkReal("" + value), (Expr) exp));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: neq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object neq(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkNot(ctx.mkFPEq((FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64())));
			} else {
				return ctx.mkNot(ctx.mkEq((Expr) exp, ctx.mkReal("" + value)));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: neq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object leq(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPLEq(ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkLe(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: leq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object leq(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPLEq((FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkLe((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: leq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object geq(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPGEq(ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkGe(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: geq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object geq(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPGEq((FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkGe((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: geq(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object lt(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPLt(ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkLt(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: lt(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object lt(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPLt((FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkLt((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: lt(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object gt(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPGt(ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkGt(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: gt(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object gt(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPGt((FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkGt((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: gt(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object plus(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPAdd(ctx.mkFPRoundNearestTiesToEven(), ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkAdd(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: plus(Object, double) failed.\n" + e);
		}
	}

	@Override
	public Object plus(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPAdd(ctx.mkFPRoundNearestTiesToEven(), (FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkAdd((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: plus(Object, double) failed.\n" + e);
		}
	}

	@Override
	public Object minus(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPSub(ctx.mkFPRoundNearestTiesToEven(), ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkSub(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: minus(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object minus(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPSub(ctx.mkFPRoundNearestTiesToEven(), (FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkSub((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: minus(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object mult(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPMul(ctx.mkFPRoundNearestTiesToEven(), ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkMul(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: mult(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object mult(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPMul(ctx.mkFPRoundNearestTiesToEven(), (FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkMul((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: mult(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object div(double value, Object exp) {
		try {
			if (useFpForReals) {
				return ctx.mkFPDiv(ctx.mkFPRoundNearestTiesToEven(), ctx.mkFPNumeral(value, ctx.mkFPSort64()), (FPExpr) exp);
			} else {
				return ctx.mkDiv(ctx.mkReal("" + value), (ArithExpr) exp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: div(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object div(Object exp, double value) {
		try {
			if (useFpForReals) {
				return ctx.mkFPDiv(ctx.mkFPRoundNearestTiesToEven(), (FPExpr) exp, ctx.mkFPNumeral(value, ctx.mkFPSort64()));
			} else {
				return ctx.mkDiv((ArithExpr) exp, ctx.mkReal("" + value));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: div(double, Object) failed.\n" + e);
		}
	}

	@Override
	public Object and(long value, Object exp) {
		try {
			BitVecExpr expBV = exp instanceof BitVecExpr? (BitVecExpr) exp : ctx.mkInt2BV(BIT_VEC_LENGTH, (IntExpr) exp);
			BitVecExpr exp2BV = ctx.mkBV(value, BIT_VEC_LENGTH);
			
			return ctx.mkBVAND(expBV, exp2BV);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	@Override
	public Object and(Object exp, long value) {
		throw new RuntimeException("## Error Z3 \n");
	}

	// Implements both boolean and bitwise and
	@Override
	public Object and(Object exp1, Object exp2) {
		if(exp1 instanceof BoolExpr && exp2 instanceof BoolExpr) {
			return ctx.mkAnd((BoolExpr) exp1, (BoolExpr) exp2);
		} else {
			BitVecExpr exp1BV;
			BitVecExpr exp2BV;
			
			if(exp1 instanceof BitVecExpr) {
				exp1BV = (BitVecExpr) exp1;
			} else if(exp1 instanceof IntExpr) {
				exp1BV = ctx.mkInt2BV(BIT_VEC_LENGTH, (IntExpr)exp1);
			} else {
				throw new RuntimeException("## Error Z3: Given expression(s) is neither numerical nor boolean. \n");
			}
			
			if(exp2 instanceof BitVecExpr) {
				exp2BV = (BitVecExpr) exp2;
			} else if(exp2 instanceof IntExpr) {
				exp2BV = ctx.mkInt2BV(BIT_VEC_LENGTH, (IntExpr)exp2);
			} else {
				throw new RuntimeException("## Error Z3: Given expression(s) is neither numerical nor boolean. \n");
			}
		
			return ctx.mkBVAND(exp1BV, exp2BV);
		} 
	}

	@Override
	public Object or(long value, Object exp) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object or(Object exp, long value) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object or(Object exp1, Object exp2) {
		if(exp1 instanceof BoolExpr && exp2 instanceof BoolExpr) {
			return ctx.mkOr((BoolExpr) exp1, (BoolExpr) exp2);
		} else {
			throw new RuntimeException("## Error Z3 \n");
		}
		
	}

	@Override
	public Object xor(long value, Object exp) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object xor(Object exp, long value) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object xor(Object exp1, Object exp2) {
		try {
			BitVecExpr left = exp1 instanceof BitVecExpr? (BitVecExpr) exp1 : ctx.mkInt2BV(BIT_VEC_LENGTH, (IntExpr) exp1);
			BitVecExpr right = exp2 instanceof BitVecExpr? (BitVecExpr) exp2 : ctx.mkInt2BV(BIT_VEC_LENGTH, (IntExpr) exp2);
			
			BitVecExpr result = ctx.mkBVXOR(left, right);
			
			return result;
		} catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
        }
	}

	@Override
	public Object shiftL(long value, Object exp) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object shiftL(Object exp, long value) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object shiftL(Object exp1, Object exp2) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object shiftR(long value, Object exp) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object shiftR(Object exp, long value) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object shiftR(Object exp1, Object exp2) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object shiftUR(long value, Object exp) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object shiftUR(Object exp, long value) {
		try {
			BitVecExpr targetBV = exp instanceof BitVecExpr? (BitVecExpr) exp : ctx.mkInt2BV(BIT_VEC_LENGTH, (IntExpr) exp);
			BitVecExpr shiftVal = ctx.mkBV(value, BIT_VEC_LENGTH);
			
			BitVecExpr result = ctx.mkBVLSHR(targetBV, shiftVal);
			
			return result;
		} catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
        }
	}

	@Override
	public Object shiftUR(Object exp1, Object exp2) {
		throw new RuntimeException("## Error Z3 \n");
	}

	@Override
	public Object mixed(Object exp1, Object exp2) {
		try {
			return ctx.mkEq((Expr) exp1, (Expr) exp2);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

    @Override
    public double getRealValueInf(Object dpVar) {
        throw new RuntimeException("## Error Z3 \n");//return 0;
    }

	@Override
	public double getRealValueSup(Object dpVar) {
		// TODO Auto-generated method stub
	    throw new RuntimeException("## Error Z3 \n");//return 0;
	}

	@Override
	public double getRealValue(Object dpVar) {
		try {
            Model model = solver.getModel();
            String strResult = model.eval((Expr) dpVar, true).toString().replaceAll("\\s+", "");
            Expr temp = model.eval((Expr) dpVar, false);
            if (temp instanceof com.microsoft.z3.RatNum) {
                strResult = ((com.microsoft.z3.RatNum) temp).toDecimalString(10);
            }
            return Double.parseDouble(strResult.replace('?', '0'));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
        }
	}

	@Override
	public void postLogicalOR(Object[] constraint) {
		// TODO Auto-generated method stub
		throw new RuntimeException("## Error Z3 \n");
	}

    // Added by Aymeric to support arrays
    @Override
    public Object makeArrayVar(String name) {
        try {
            Sort int_type = ctx.mkIntSort();
            return ctx.mkArrayConst(name, int_type, int_type);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }

    @Override
    public Object makeRealArrayVar(String name) {
        try {
            Sort int_type = ctx.mkIntSort();
            Sort real_type = ctx.mkRealSort();
            return ctx.mkArrayConst(name, int_type, real_type);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }

    @Override
    public Object select(Object exp1, Object exp2) {
        try {
        	IntExpr index = exp2 instanceof BitVecExpr ? ctx.mkBV2Int((BitVecExpr)exp2, false) : (IntExpr) exp2;
            return ctx.mkSelect((ArrayExpr)exp1, index);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }

    @Override
    public Object store(Object exp1, Object exp2, Object exp3) {
        try {
        	IntExpr exp2Int = exp2 instanceof BitVecExpr? ctx.mkBV2Int((BitVecExpr) exp2, false) : (IntExpr) exp2;
            return ctx.mkStore((ArrayExpr)exp1, exp2Int, (Expr)exp3);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }

    @Override
    public Object realSelect(Object exp1, Object exp2) {
        try {
            return ctx.mkSelect((ArrayExpr)exp1, (IntExpr)exp2);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }

    @Override
    public Object realStore(Object exp1, Object exp2, Object exp3) {
        try {
            return ctx.mkStore((ArrayExpr)exp1, (IntExpr)exp2, (RealExpr)exp3);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }

    @Override
    public Object init_array(Object exp1, Object exp2) {
      try {
        // forall i. exp1[i] == exp2
        Expr[] foralls = new Expr[1];
        String name = ((ArrayExpr)exp1).toString() + "!init";
        foralls[0] = ctx.mkIntConst(name); 
        Expr body = ctx.mkEq(ctx.mkSelect((ArrayExpr)exp1, (IntExpr)foralls[0]), (IntExpr)exp2);
        return ctx.mkForall(foralls, body, 1, null, null, null, null);
      } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
      }
    }

    @Override
    public Object makeIntConst(long value) {
        try {
            return ctx.mkInt(value);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }

    @Override
    public Object makeRealConst(double value) {
        try {
            return ctx.mkReal("" + value);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
        }
    }
    
    // ======
    
    @Override
    public Object makeFalse() {
		return ctx.mkFalse();
	}
	
    @Override
	public Object makeTrue() {
		return ctx.mkTrue();
	}

    @Override
    public boolean isTrue(Object exp) {
		try {
			return ((BoolExpr) exp).isTrue();
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		}
	}

    @Override
	public boolean isFalse(Object exp) {
		try {
			return ((BoolExpr) exp).isFalse();
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		}
	}
    
    // String operations
    
    @Override
    public Object makeStringConst(String value) {
    	try {
			return ctx.mkString(value);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeStringVar(String name) {
    	try {
    		SeqSort stringSort = ctx.getStringSort();
    		Expr strVar = ctx.mkConst(name, stringSort);
    		//IntExpr lengthExpr = ctx.mkLength((SeqExpr) strVar);
    		//BoolExpr constraint = ctx.mkEq(lengthExpr, (Expr) lengthDPExpr);
    		//solver.add(constraint);
    		return strVar;
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeConcat(Object stringDPExpr1, Object stringDPExpr2) {
    	try {
			return ctx.mkConcat((SeqExpr) stringDPExpr1, (SeqExpr) stringDPExpr2);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeSubstring(Object originalStrDPExpr, Object startIndexDPExpr, Object lengthDPExpr) {
    	try {
			return ctx.mkExtract((SeqExpr) originalStrDPExpr, (IntExpr) startIndexDPExpr, (IntExpr) lengthDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    /*
     * Returns a substring of length 1 at the specified index.
     */
    @Override
    public Object makeAt(Object originalStrDPExpr, Object indexDPExpr) {
    	try {
			return ctx.mkAt((SeqExpr) originalStrDPExpr, (IntExpr) indexDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    private static int charAtBVCounter = 0;
    
    /*
     * Returns an integer instead of a String.
     * TODO: might need to add a name to identify the const.
     */
    @Override
    public Object makeCharAt(Object originalStrDPExpr, Object indexDPExpr) {
    	try {
			SeqExpr resultStr = ctx.mkAt((SeqExpr) originalStrDPExpr, (IntExpr) indexDPExpr);
			
			
			BitVecExpr resultBV = ctx.mkBVConst("charAt_" + charAtBVCounter, 8);
			
			charAtBVCounter++;
			
			BoolExpr strBVConstraint = ctx.mkEq(resultStr, ctx.mkUnit(resultBV));
			solver.add(strBVConstraint);
			
			IntExpr resultInt = ctx.mkBV2Int(resultBV, false);
			return resultInt;
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
	
    @Override
	public Object makeIndexOfStr(Object originalStrDPExpr, Object targetStrDPExpr) {
    	try {
    		return makeIndexOfStr(originalStrDPExpr, targetStrDPExpr, ctx.mkInt(0));
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
	
    @Override
	public Object makeIndexOfStr(Object originalStrDPExpr, Object targetStrDPExpr, Object startIndexDPExpr) {
    	try {
			return ctx.mkIndexOf((SeqExpr) originalStrDPExpr, (SeqExpr) targetStrDPExpr, (IntExpr) startIndexDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
	
    @Override
	public Object makeIndexOfChar(Object originalStrDPExpr, Object targetCharDPExpr) {
    	try {
    		return makeIndexOfChar(originalStrDPExpr, targetCharDPExpr, ctx.mkInt(0));
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
	
    @Override
	public Object makeIndexOfChar(Object originalStrDPExpr, Object targetCharDPExpr, Object startIndexDPExpr) {
    	try {
    		// "Sorts String and (Seq (_ BitVec 16)) are incompatible"
			 BitVecExpr targetCharBV = ctx.mkInt2BV(8, (IntExpr) targetCharDPExpr);
			 SeqExpr targetCharStr = ctx.mkUnit(targetCharBV);
			 return makeIndexOfStr(originalStrDPExpr, targetCharStr, startIndexDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeLength(Object stringDPExpr) {
    	try {
    		return ctx.mkLength((SeqExpr) stringDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeIntToString(Object intDPExpr) {
    	try {
    		return ctx.intToString((IntExpr) intDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeStringToInt(Object strDPExpr) {
    	try {
    		return ctx.stringToInt((SeqExpr) strDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeStartsWith(Object prefixDPExpr, Object strDPExpr) {
    	try {
    		return ctx.mkPrefixOf((SeqExpr) prefixDPExpr, (SeqExpr) strDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeEndsWith(Object suffixDPExpr, Object strDPExpr) {
    	try {
    		return ctx.mkSuffixOf((SeqExpr) suffixDPExpr, (SeqExpr) strDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeConstains(Object strDPExpr, Object substrDPExpr) {
    	try {
    		return ctx.mkContains((SeqExpr) strDPExpr, (SeqExpr) substrDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    /*
     * Store a numerical value in a string at a specified index.
     */
    @Override
    public Object makeStoreCharAt(Object strDPExpr, Object indexDPExpr, Object valueDPExpr) {
    	try {
    		IntExpr lengthOfFirstHalf = (IntExpr) indexDPExpr;
    		SeqExpr firstHalf = ctx.mkExtract((SeqExpr) strDPExpr, ctx.mkInt(0), lengthOfFirstHalf);
    		
    		SeqExpr middle = ctx.mkUnit( ctx.mkInt2BV(8, (IntExpr)valueDPExpr));
    		
    		IntExpr startOfSecondHalf = (IntExpr) ctx.mkAdd((IntExpr) indexDPExpr, ctx.mkInt(1));
    		IntExpr totalLength = ctx.mkLength((SeqExpr) strDPExpr);
    		IntExpr lengthOfSecondHalf = (IntExpr) ctx.mkSub(totalLength, (IntExpr) indexDPExpr);
    		SeqExpr secondHalf = ctx.mkExtract((SeqExpr) strDPExpr, startOfSecondHalf, lengthOfSecondHalf);
    		
    		SeqExpr result = ctx.mkConcat(firstHalf, middle, secondHalf);
    		
    		return result;
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeIsEmpty(Object strDPExpr) {
    	try {
    		return ctx.mkEq(ctx.mkLength((SeqExpr) strDPExpr), ctx.mkInt(0));
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeIsNull(Object dpExpr) {
    	try {
    		FuncDecl isNullDecl = ctx.mkFuncDecl("isNull", new Sort[] { ((Expr)dpExpr).getSort() }, ctx.getBoolSort());
    		return ctx.mkEq(ctx.mkApp(isNullDecl, (Expr) dpExpr), ctx.mkTrue());
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
    
    @Override
    public Object makeHashCode(Object strDPExpr) {
		try {
			// ctx.getIntSort()
			FuncDecl isNullDecl = ctx.mkFuncDecl("hashCode", new Sort[] { ctx.getStringSort() }, ctx.mkBitVecSort(BIT_VEC_LENGTH));
    		return ctx.mkApp(isNullDecl, (Expr) strDPExpr);
		} catch(Exception e) {
			e.printStackTrace();
            throw new RuntimeException("## Error Z3 : Exception caught in Z3 JNI: " + e);
		} 
	}
}
