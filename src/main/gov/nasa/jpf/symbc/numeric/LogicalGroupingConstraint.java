package gov.nasa.jpf.symbc.numeric;

import java.util.ArrayList;
import java.util.List;

public class LogicalGroupingConstraint extends Constraint {
	private List<Constraint> list;
	private Operator operator;
	public boolean negated;
	
	public static enum Operator { AND, OR };
	
	
	public LogicalGroupingConstraint() {
		super(null, null, null);
		list = new ArrayList<Constraint>();
		negated = false;
	}
	
	public LogicalGroupingConstraint(LogicalGroupingConstraint lgc) {
		super(null, null, null);
		this.list = lgc.list; // Shallow
		this.operator = lgc.operator;
		this.negated = lgc.negated;
	}
	
	public LogicalGroupingConstraint(List<Constraint> l, Operator op, boolean negated) {
		super(null, null, null);
		this.list = l; // Shallow
		this.operator = op;
		this.negated = negated;
	}
	
	public LogicalGroupingConstraint(List<Constraint> l, Operator op) {
		this(l, op, false);
	}
	
	public LogicalGroupingConstraint(Operator op, boolean negated) {
		super(null, null, null);
		this.list = new ArrayList<Constraint>();
		this.negated = negated;
		this.operator = op;
	}
	
	public LogicalGroupingConstraint(Operator op) {
		this(op, false);
	}
	
	public LogicalGroupingConstraint(boolean negated) {
		this(Operator.AND, negated);
	}
	
	@Override
	public Constraint makeCopy() {
		LogicalGroupingConstraint copy = new LogicalGroupingConstraint(this);
		if(this.and != null) {
    		copy.and = this.and.makeCopy();
    	}
    	return copy;
	}
	
	public List<Constraint> getList() { return list; }
	public Operator getOperator() { return operator; }
	
	public void addToList(Constraint constraint) {
		//if(!list.contains(constraint)) {
			list.add(constraint);
		//}
	}

	@Override
	public Constraint not() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported");
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if(list == null || list.isEmpty()) {
			return "";
		}
		
		Constraint con = list.get(0);
		
		sb.append(negated? "~" : "");
		
		if(list.size() > 1) sb.append("(");
		
		sb.append(con.toString());
		
		
		for(int i = 1; i<list.size(); i++) {
			if(operator == Operator.AND) {
				sb.append(" && ");
			} else {
				sb.append(" || ");
			}
			
			sb.append("\n");
			
			sb.append(list.get(i).toString());
		}
		
		if(list.size() > 1) sb.append(")");
		
		if(and != null) {
			sb.append(" && \n");
			sb.append(and.toString());
		}
		
	
		return sb.toString();
	}
	
	@Override
	public String stringPC() {
		return toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof LogicalGroupingConstraint)) {
			return false;
		}
		
		LogicalGroupingConstraint con = (LogicalGroupingConstraint) o;
		
		boolean result = this.list.equals(con.list) &&
		this.negated == con.negated &&
		this.operator == con.operator;
		
		if(this.and == null) {
			result = result && (con.and == null);
		} else {
			result = result && (this.and.equals(con));
		}
		
		return result;
		
	}

}
