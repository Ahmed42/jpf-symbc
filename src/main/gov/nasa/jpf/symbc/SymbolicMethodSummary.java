package gov.nasa.jpf.symbc;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.util.Pair;
import java.util.Vector;

public class SymbolicMethodSummary {
	private MethodInfo methodInfo;
    private ClassInfo classInfo;
    private Vector<Pair<PathCondition, Expression>> pathsSummaries; // A vector of pairs of path conditions and transformations
    
    public SymbolicMethodSummary(ClassInfo classInfo, MethodInfo methodInfo) {
    	this.methodInfo = methodInfo;
    	this.classInfo = classInfo;
    	
    	pathsSummaries = new Vector<Pair<PathCondition, Expression>>();
    }
    
    public Vector<Pair<PathCondition, Expression>> getPathsSummaries() {
    	return pathsSummaries;
    }
    
    // TODO Might need to perform better check than this
    public boolean containsPathSummary(Pair<PathCondition, Expression> pathSummary) {
    	return pathsSummaries.contains(pathSummary);
    }
    
    public void addPathSummary(Pair<PathCondition, Expression> pathSummary) {
    	pathsSummaries.add(pathSummary);
    }
    
    public String toString() {
    	String summaryStr = "Method name: " + methodInfo.getLongName() + "\n";
    	for(Pair<PathCondition, Expression> pathSummary : pathsSummaries) {
    		PathCondition pathCondition = pathSummary._1;
    		Expression transformation = pathSummary._2;
    		summaryStr += "Path condition: \n" + pathCondition + "\nTransformation: \n" + transformation + "\n------------\n";
    	}
    	return summaryStr;
    }
}
