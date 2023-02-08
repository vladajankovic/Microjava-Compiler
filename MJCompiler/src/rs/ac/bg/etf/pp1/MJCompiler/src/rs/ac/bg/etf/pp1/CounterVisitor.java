package rs.ac.bg.etf.pp1;


import rs.ac.bg.etf.pp1.ast.ArrayParameter;
import rs.ac.bg.etf.pp1.ast.ArrayVariable;
import rs.ac.bg.etf.pp1.ast.SimpleParameter;
import rs.ac.bg.etf.pp1.ast.SimpleVariable;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class CounterVisitor extends VisitorAdaptor {
	
	protected int count;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public static class FormParamCounter extends CounterVisitor{

		@Override
		public void visit(SimpleParameter SimpleParameter) {
			count++;
		}
		
		@Override
		public void visit(ArrayParameter ArrayParameter) {
			count++;
		}
		
		
	}
	
	public static class VarCounter extends CounterVisitor{

		@Override
		public void visit(SimpleVariable SimpleVariable) {
			count++;
		}
		
		@Override
		public void visit(ArrayVariable ArrayVariable) {
			count++;
		}
		
	}

}
