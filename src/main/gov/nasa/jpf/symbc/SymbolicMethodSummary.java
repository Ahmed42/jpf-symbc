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
    //private Vector<Pair<PathCondition, Expression>> pathsSummaries; // A vector of pairs of path conditions and transformations
    private Vector<SymbolicPathSummary> pathsSummaries;
    
    
    public SymbolicMethodSummary(ClassInfo classInfo, MethodInfo methodInfo) {
    	this.methodInfo = methodInfo;
    	this.classInfo = classInfo;
    	
    	pathsSummaries = new Vector<SymbolicPathSummary>();
    }
    
    public Vector<SymbolicPathSummary> getPathsSummaries() {
    	return pathsSummaries;
    }
    
    // TODO Might need to perform better check than this
    public boolean containsPathSummary(SymbolicPathSummary pathSummary) {
    	return pathsSummaries.contains(pathSummary);
    }
    
    public void addPathSummary(SymbolicPathSummary pathSummary) {
    	pathsSummaries.add(pathSummary);
    }
    
    public String toString() {
    	String summaryStr = "Method name: " + methodInfo.getLongName() + "\n";
    	for(SymbolicPathSummary pathSummary : pathsSummaries) {
    		PathCondition pathCondition = pathSummary.getCondition();
    		//PathCondition heapPathCondition = pathSummary.getHeapCondition();
    		PathCondition transformations = pathSummary.getTransformations();
    		    		
    		
    		summaryStr += "\nPath Condition: \n" 
    					+ pathCondition
    					+ "\nTransformations: \n" 
    					+ transformations
    					+ "\n------------\n";
    	}
    	return summaryStr;
    }
}