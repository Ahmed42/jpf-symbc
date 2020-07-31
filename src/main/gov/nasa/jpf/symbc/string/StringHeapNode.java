package gov.nasa.jpf.symbc.string;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.symbc.heap.HeapNode;
import gov.nasa.jpf.symbc.numeric.Expression;

public class StringHeapNode extends HeapNode {
	private StringSymbolic stringSymVar;
	
	public StringHeapNode(int indx, ClassInfo tClassInfo, StringSymbolic strSymVar) {
		super(indx, tClassInfo, null);
		
		stringSymVar = strSymVar;
	}
	
	public void setStringSymbolic(StringSymbolic symVar) {
		stringSymVar = symVar;
	}
	
	public StringSymbolic getStringSymbolic() {
		return stringSymVar;
	}
}
