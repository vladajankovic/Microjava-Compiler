package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.Addition;
import rs.ac.bg.etf.pp1.ast.ArrayDesignatorIndex;
import rs.ac.bg.etf.pp1.ast.ArrayName;
import rs.ac.bg.etf.pp1.ast.ArrayToIterateOver;
import rs.ac.bg.etf.pp1.ast.BasicDesignator;
import rs.ac.bg.etf.pp1.ast.BreakStmt;
import rs.ac.bg.etf.pp1.ast.ConditionValid;
import rs.ac.bg.etf.pp1.ast.ContinueStmt;
import rs.ac.bg.etf.pp1.ast.DesignatorArrayAssignStmt;
import rs.ac.bg.etf.pp1.ast.DesignatorAssignStmtOK;
import rs.ac.bg.etf.pp1.ast.DesignatorDecrementStmt;
import rs.ac.bg.etf.pp1.ast.DesignatorFunctionStmt;
import rs.ac.bg.etf.pp1.ast.DesignatorIncrementStmt;
import rs.ac.bg.etf.pp1.ast.Division;
import rs.ac.bg.etf.pp1.ast.ElseBegin;
import rs.ac.bg.etf.pp1.ast.ElseEnd;
import rs.ac.bg.etf.pp1.ast.FactorBoolean;
import rs.ac.bg.etf.pp1.ast.FactorCharacter;
import rs.ac.bg.etf.pp1.ast.FactorDesignator;
import rs.ac.bg.etf.pp1.ast.FactorFunction;
import rs.ac.bg.etf.pp1.ast.FactorNewArray;
import rs.ac.bg.etf.pp1.ast.FactorNumber;
import rs.ac.bg.etf.pp1.ast.ForEachStmt;
import rs.ac.bg.etf.pp1.ast.ForeachDummyEnd;
import rs.ac.bg.etf.pp1.ast.ForeachDummyStart;
import rs.ac.bg.etf.pp1.ast.ImmutableIterator;
import rs.ac.bg.etf.pp1.ast.IncludeOptDesignator;
import rs.ac.bg.etf.pp1.ast.IsEqual;
import rs.ac.bg.etf.pp1.ast.IsGreater;
import rs.ac.bg.etf.pp1.ast.IsGreaterEqual;
import rs.ac.bg.etf.pp1.ast.IsLess;
import rs.ac.bg.etf.pp1.ast.IsLessEqual;
import rs.ac.bg.etf.pp1.ast.IsNotEqual;
import rs.ac.bg.etf.pp1.ast.MatchedWhileStmt;
import rs.ac.bg.etf.pp1.ast.MethodDecl;
import rs.ac.bg.etf.pp1.ast.MethodSignature;
import rs.ac.bg.etf.pp1.ast.Modulo;
import rs.ac.bg.etf.pp1.ast.MultipleCondFacts;
import rs.ac.bg.etf.pp1.ast.MultipleCondTerms;
import rs.ac.bg.etf.pp1.ast.MultipleFactorTerms;
import rs.ac.bg.etf.pp1.ast.MultipleTermExprs;
import rs.ac.bg.etf.pp1.ast.Multiplication;
import rs.ac.bg.etf.pp1.ast.NegativeFactor;
import rs.ac.bg.etf.pp1.ast.NoOptDesignator;
import rs.ac.bg.etf.pp1.ast.NoPrintNum;
import rs.ac.bg.etf.pp1.ast.OneCondExpr;
import rs.ac.bg.etf.pp1.ast.PrintExprStmt;
import rs.ac.bg.etf.pp1.ast.PrintNum;
import rs.ac.bg.etf.pp1.ast.ReadStmt;
import rs.ac.bg.etf.pp1.ast.ReturnExprStmt;
import rs.ac.bg.etf.pp1.ast.ReturnStmt;
import rs.ac.bg.etf.pp1.ast.SingleCondFact;
import rs.ac.bg.etf.pp1.ast.SingleCondTerm;
import rs.ac.bg.etf.pp1.ast.Subtraction;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.ast.ThenEnd;
import rs.ac.bg.etf.pp1.ast.TwoCondExprs;
import rs.ac.bg.etf.pp1.ast.UnmatchedWhileStmt;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.ac.bg.etf.pp1.ast.WhileConditonStart;
import rs.ac.bg.etf.pp1.ast.WhileDummyEnd;
import rs.ac.bg.etf.pp1.ast.WhileDummyStart;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;

	public int getMainPc() {
		return mainPc;
	}

	public void setMainPc(int mainPc) {
		this.mainPc = mainPc;
	}

	private List<String> addop = new ArrayList<String>();
	private List<String> mulop = new ArrayList<String>();
	private List<String> relop = new ArrayList<String>();

	private static List<Boolean> arrRefList = new ArrayList<Boolean>();

	private static List<Obj> designatorArrayList = new ArrayList<Obj>();

	private static List<Integer> andAddr = new ArrayList<Integer>();
	private static List<Integer> orAddr = new ArrayList<Integer>();
	private static List<Integer> elseAddr = new ArrayList<Integer>();
	private static List<Integer> skipElseAddr = new ArrayList<Integer>();
	private static List<Integer> skipWhile = new ArrayList<Integer>();
	private static List<Integer> conditionWhile = new ArrayList<Integer>();
	private static List<Integer> breakJump = new ArrayList<Integer>();
	private static List<Integer> breakCount = new ArrayList<Integer>();
	private static Obj iterator = null;
	private static Obj foreachArray = null;
	private static int foreachArrayType = 0;
	private static List<Integer> foreachEndJump = new ArrayList<Integer>();
	private static List<Integer> foreachConditionAdr = new ArrayList<Integer>();

	
	@Override
	public void visit(MethodSignature ms) {

		// Set mainPc if method is main
		if (ms.getMethodName().equalsIgnoreCase("main")) {
			mainPc = Code.pc;
		}

		// Set method address
		ms.obj.setAdr(Code.pc);

		// Get number of method parameters and local variables
		SyntaxNode parent = ms.getParent();

		VarCounter vc = new VarCounter();
		parent.traverseTopDown(vc);

		FormParamCounter fc = new FormParamCounter();
		parent.traverseTopDown(fc);

		// Generate entry code
		Code.put(Code.enter);
		Code.put(fc.getCount());
		Code.put(fc.getCount() + vc.getCount());

	}

	@Override
	public void visit(MethodDecl MethodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	/*************************************************************************/

	@Override
	public void visit(ReturnStmt ReturnStmt) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	@Override
	public void visit(ReturnExprStmt ReturnExprStmt) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	@Override
	public void visit(PrintExprStmt st) {
		switch (st.getExpr().struct.getKind()) {
		case Struct.Int:
			Code.put(Code.print);
			break;
		case Struct.Char:
			Code.put(Code.bprint);
			break;
		case Struct.Bool:
			Code.put(Code.print);
			break;
		}
	}

	@Override
	public void visit(NoPrintNum NoPrintNum) {
		Code.loadConst(0);
	}

	@Override
	public void visit(PrintNum num) {
		Code.loadConst(num.getPrintNum().intValue());
	}

	@Override
	public void visit(ReadStmt st) {

		Obj node = st.getDesignator().obj;

		if (node.getType().getKind() != Struct.Array) {
			switch (node.getType().getKind()) {
			case Struct.Int:
				Code.put(Code.read);
				break;
			case Struct.Char:
				Code.put(Code.bread);
				break;
			case Struct.Bool:
				Code.put(Code.read);
				break;
			}
			Code.store(node);
		}
		// else ako je element niza
		else {
			arrRefList.remove(arrRefList.size() - 1);
			switch (node.getType().getElemType().getKind()) {
			case Struct.Int:
				Code.put(Code.read);
				break;
			case Struct.Char:
				Code.put(Code.bread);
				break;
			case Struct.Bool:
				Code.put(Code.read);
				break;
			}
			switch (node.getType().getElemType().getKind()) {
			case Struct.Int:
				Code.put(Code.astore);
				break;
			case Struct.Char:
				Code.put(Code.bastore);
				break;
			case Struct.Bool:
				Code.put(Code.astore);
				break;
			}

		}

	}

	@Override
	public void visit(ArrayToIterateOver atoi) {
		arrRefList.remove(arrRefList.size() - 1);
		foreachArray = atoi.getDesignator().obj;
		foreachArrayType = foreachArray.getType().getElemType().getKind();
	}

	@Override
	public void visit(ImmutableIterator it) {
		iterator = it.obj;
	}

	@Override
	public void visit(ForeachDummyStart ForeachDummyStart) {
		
		breakCount.add(0);

		// Load array, 0 (init index)
		Code.load(foreachArray);
		Code.loadConst(0);

		foreachConditionAdr.add(Code.pc);
		Code.put(Code.dup);
		Code.load(foreachArray);
		Code.put(Code.arraylength);

		// If index is greater than array lenght end the loop
		Code.putFalseJump(Code.lt, 0);
		foreachEndJump.add(Code.pc - 2);

		Code.put(Code.dup2);
		switch (foreachArrayType) {
		case Struct.Int:
			Code.put(Code.aload);
			break;
		case Struct.Char:
			Code.put(Code.baload);
			break;
		case Struct.Bool:
			Code.put(Code.aload);
			break;
		}
		Code.store(iterator);

	}

	@Override
	public void visit(ForeachDummyEnd ForeachDummyEnd) {

		Code.loadConst(1);
		Code.put(Code.add);
		Code.putJump(foreachConditionAdr.get(foreachConditionAdr.size() - 1));

		Code.fixup(foreachEndJump.get(foreachEndJump.size() - 1));
		
		int bcount = breakCount.remove(breakCount.size() - 1).intValue();
		for (int i = 0; i < bcount; i++) {
			Integer adr = breakJump.remove(breakJump.size() - 1);
			Code.fixup(adr);
		}

	}

	@Override
	public void visit(ForEachStmt ForEachStmt) {
		// Pop the array adres and index from stack of last foreach loop
		Code.put(Code.pop);
		Code.put(Code.pop);

		foreachEndJump.remove(foreachEndJump.size() - 1);
		foreachConditionAdr.remove(foreachConditionAdr.size() - 1);
	}

	/*************************************************************************/

	@Override
	public void visit(DesignatorAssignStmtOK ds) {

		// Store the stack value in variable
		Obj node = ds.getDesignator().obj;

		if (node.getType().getKind() != Struct.Array) {
			Code.store(ds.getDesignator().obj);
			return;
		}

		if (node.getType().getKind() == Struct.Array) {
			if (arrRefList.remove(arrRefList.size() - 1).booleanValue() == true) {
				Code.store(ds.getDesignator().obj);
			} else {
				switch (node.getType().getElemType().getKind()) {
				case Struct.Int:
					Code.put(Code.astore);
					break;
				case Struct.Char:
					Code.put(Code.bastore);
					break;
				case Struct.Bool:
					Code.put(Code.astore);
					break;
				}
			}
		}

	}

	@Override
	public void visit(DesignatorIncrementStmt ds) {
		Obj node = ds.getDesignator().obj;

		if (node.getType().getKind() != Struct.Array) {
			Code.load(node);
			Code.put(Code.const_1);
			Code.put(Code.add);
			Code.store(node);
		}
		// else ako je element niza
		else {
			arrRefList.remove(arrRefList.size() - 1);
			Code.put(Code.dup2);
			switch (node.getType().getElemType().getKind()) {
			case Struct.Int:
				Code.put(Code.aload);
				break;
			case Struct.Char:
				Code.put(Code.baload);
				break;
			case Struct.Bool:
				Code.put(Code.aload);
				break;
			}
			Code.put(Code.const_1);
			Code.put(Code.add);
			switch (node.getType().getElemType().getKind()) {
			case Struct.Int:
				Code.put(Code.astore);
				break;
			case Struct.Char:
				Code.put(Code.bastore);
				break;
			case Struct.Bool:
				Code.put(Code.astore);
				break;
			}
		}

	}

	@Override
	public void visit(DesignatorDecrementStmt ds) {
		Obj node = ds.getDesignator().obj;

		if (node.getType().getKind() != Struct.Array) {
			Code.load(node);
			Code.put(Code.const_1);
			Code.put(Code.sub);
			Code.store(node);
		}
		// else ako je element niza
		else {
			arrRefList.remove(arrRefList.size() - 1);
			Code.put(Code.dup2);
			switch (node.getType().getElemType().getKind()) {
			case Struct.Int:
				Code.put(Code.aload);
				break;
			case Struct.Char:
				Code.put(Code.baload);
				break;
			case Struct.Bool:
				Code.put(Code.aload);
				break;
			}
			Code.put(Code.const_1);
			Code.put(Code.sub);
			switch (node.getType().getElemType().getKind()) {
			case Struct.Int:
				Code.put(Code.astore);
				break;
			case Struct.Char:
				Code.put(Code.bastore);
				break;
			case Struct.Bool:
				Code.put(Code.astore);
				break;
			}
		}

	}

	@Override
	public void visit(DesignatorFunctionStmt st) {

		Obj node = st.getDesignator().obj;

		int offset = node.getAdr() - Code.pc;

		Code.put(Code.call);
		Code.put2(offset);

		// If the function has return value it must be removed because it is unused
		if (node.getType() != Tab.noType) {
			Code.put(Code.pop);
		}
	}

	@Override
	public void visit(DesignatorArrayAssignStmt st) {
		Obj niz = st.getDesignator().obj;
		arrRefList.remove(arrRefList.size() - 1);
		int cnt = designatorArrayList.size();

		Code.putJump(Code.pc + 5);

		int trapAddress = Code.pc;

		Code.put(Code.trap);
		Code.put(1);

		Code.load(niz);
		Code.put(Code.arraylength);
		Code.loadConst(cnt - 1);
		Code.putFalseJump(Code.gt, trapAddress);

		for (int i = cnt - 1; i >= 0; i--) {

			Code.load(niz);
			Code.loadConst(i);
			switch (niz.getType().getElemType().getKind()) {
			case Struct.Int:
				Code.put(Code.aload);
				break;
			case Struct.Char:
				Code.put(Code.baload);
				break;
			case Struct.Bool:
				Code.put(Code.aload);
				break;
			}

			Obj node = designatorArrayList.get(i);

			if (node == Tab.noObj) {
				Code.put(Code.pop);
				continue;
			}

			if (node.getType().getKind() != Struct.Array) {
				Code.store(node);
			} else {
				switch (node.getType().getElemType().getKind()) {
				case Struct.Int:
					Code.put(Code.astore);
					break;
				case Struct.Char:
					Code.put(Code.bastore);
					break;
				case Struct.Bool:
					Code.put(Code.astore);
					break;
				}
			}
		}

		designatorArrayList.clear();
	}

	@Override
	public void visit(IncludeOptDesignator iod) {
		Obj node = iod.getDesignator().obj;
		designatorArrayList.add(node);
	}

	@Override
	public void visit(NoOptDesignator NoOptDesignator) {
		designatorArrayList.add(Tab.noObj);
	}

	/*************************************************************************/

	@Override
	public void visit(ThenEnd ThenEnd) {
		// Set the addres to jump to the else branch
		if (!elseAddr.isEmpty()) {
			Integer adr = elseAddr.remove(elseAddr.size() - 1);
			Code.fixup(adr.intValue());
		}
	}

	@Override
	public void visit(ElseBegin ElseBegin) {
		// If the Then branch was visited, skip the else branch
		Code.putJump(0);
		skipElseAddr.add(Code.pc - 2);
	}

	@Override
	public void visit(ElseEnd ElseEnd) {
		// Set the entry point of the else branch
		if (!skipElseAddr.isEmpty()) {
			Integer adr = skipElseAddr.remove(skipElseAddr.size() - 1);
			Code.fixup(adr.intValue());
		}
	}

	@Override
	public void visit(ConditionValid ConditionValid) {
		// At this point the resut didnt jump to the Then branch so from here on out
		// we are jumping beyond the Then branch (to the Else branch if it exists)
		Code.putJump(0);
		elseAddr.add(Code.pc - 2);

		for (Integer i : orAddr) {
			Code.fixup(i.intValue());
		}
		orAddr.clear();
	}

	@Override
	public void visit(WhileDummyStart WhileDummyStart) {
		// At theis point, the result is false and skips the while loop
		Code.putJump(0);
		skipWhile.add(Code.pc - 2);

		for (Integer i : orAddr) {
			Code.fixup(i.intValue());
		}
		orAddr.clear();

		breakCount.add(0);
	}

	@Override
	public void visit(WhileDummyEnd WhileDummyEnd) {
		// At the end of the while loop, jump to test condition again
		Integer condition = conditionWhile.remove(conditionWhile.size() - 1);
		Code.putJump(condition);
		// Set the exit point of the while loop
		if (!skipWhile.isEmpty()) {
			Integer adr = skipWhile.remove(skipWhile.size() - 1);
			Code.fixup(adr.intValue());
		}

		int bcount = breakCount.remove(breakCount.size() - 1).intValue();
		for (int i = 0; i < bcount; i++) {
			Integer adr = breakJump.remove(breakJump.size() - 1);
			Code.fixup(adr);
		}

	}

	@Override
	public void visit(WhileConditonStart WhileConditonStart) {
		conditionWhile.add(Code.pc);
	}

	@Override
	public void visit(BreakStmt BreakStmt) {
		// Place jump out of surounding loop, patching when the loop ends
		Code.putJump(0);
		breakJump.add(Code.pc - 2);
		breakCount.set(breakCount.size() - 1, breakCount.get(breakCount.size() - 1) + 1);
	}

	@Override
	public void visit(ContinueStmt continueStmt) {
		SyntaxNode parent = continueStmt.getParent();
		while (parent.getClass() != ForEachStmt.class && parent.getClass() != MatchedWhileStmt.class
				&& parent.getClass() != UnmatchedWhileStmt.class) {
			parent = parent.getParent();
		}
		if (parent.getClass() == ForEachStmt.class) {
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(foreachConditionAdr.get(foreachConditionAdr.size() - 1));
		}
		if (parent.getClass() == MatchedWhileStmt.class) {
			Code.putJump(conditionWhile.get(conditionWhile.size() - 1));
		}
		if (parent.getClass() == UnmatchedWhileStmt.class) {
			Code.putJump(conditionWhile.get(conditionWhile.size() - 1));
		}
		
	}

	@Override
	public void visit(MultipleCondTerms MultipleCondTerms) {
		// If at least one Boolean of the or operation is true, jump to Then branch
		Code.putJump(0);
		orAddr.add(Code.pc - 2);

		for (Integer i : andAddr) {
			Code.fixup(i.intValue());
		}
		andAddr.clear();
	}

	@Override
	public void visit(SingleCondTerm SingleCondTerm) {
		// If the first Boolean of the or operation is true, jump to Then branch
		Code.putJump(0);
		orAddr.add(Code.pc - 2);

		for (Integer i : andAddr) {
			Code.fixup(i.intValue());
		}
		andAddr.clear();
	}

	@Override
	public void visit(MultipleCondFacts MultipleCondFacts) {
		// Test if result of relation or if the variable is true operation is true
		// if its false, skip all next and operatons until first or operation
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0);
		andAddr.add(Code.pc - 2);
	}

	@Override
	public void visit(SingleCondFact SingleCondFact) {
		// Test if result of relation or if the variable is true operation is true
		// if its false, skip all next and operatons until first or operation
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0);
		andAddr.add(Code.pc - 2);
	}

	@Override
	public void visit(TwoCondExprs tce) {

		// Test if relation is true
		int op = 0;
		switch (relop.remove(relop.size() - 1)) {
		case "==":
			op = Code.eq;
			break;
		case "!=":
			op = Code.ne;
			break;
		case ">=":
			op = Code.ge;
			break;
		case ">":
			op = Code.gt;
			break;
		case "<=":
			op = Code.le;
			break;
		case "<":
			op = Code.lt;
			break;
		}

		Code.putFalseJump(op, Code.pc + 7);
		Code.loadConst(1);
		Code.putJump(Code.pc + 4);
		Code.loadConst(0);
	}

	@Override
	public void visit(OneCondExpr OneCondExpr) {
		// Single variable test if its true
		Code.loadConst(1);
		Code.putFalseJump(Code.eq, Code.pc + 7);
		Code.loadConst(1);
		Code.putJump(Code.pc + 4);
		Code.loadConst(0);
	}

	@Override
	public void visit(IsEqual IsEqual) {
		relop.add("==");
	}

	@Override
	public void visit(IsNotEqual IsNotEqual) {
		relop.add("!=");
	}

	@Override
	public void visit(IsLessEqual IsLessEqual) {
		relop.add("<=");
	}

	@Override
	public void visit(IsLess IsLess) {
		relop.add("<");
	}

	@Override
	public void visit(IsGreaterEqual IsGreaterEqual) {
		relop.add(">=");
	}

	@Override
	public void visit(IsGreater IsGreater) {
		relop.add(">");
	}

	/*************************************************************************/

	@Override
	public void visit(Addition Addition) {
		addop.add("+");
	}

	@Override
	public void visit(Subtraction Subtraction) {
		addop.add("-");
	}

	@Override
	public void visit(MultipleTermExprs MultipleTermExprs) {
		switch (addop.remove(addop.size() - 1)) {
		case "+":
			Code.put(Code.add);
			break;
		case "-":
			Code.put(Code.sub);
			break;
		}
	}

	/*************************************************************************/

	@Override
	public void visit(Multiplication Multiplication) {
		mulop.add("*");
	}

	@Override
	public void visit(Division Division) {
		mulop.add("/");
	}

	@Override
	public void visit(Modulo Modulo) {
		mulop.add("%");
	}

	@Override
	public void visit(MultipleFactorTerms MultipleFactorTerms) {
		switch (mulop.remove(mulop.size() - 1)) {
		case "*":
			Code.put(Code.mul);
			break;
		case "/":
			Code.put(Code.div);
			break;
		case "%":
			Code.put(Code.rem);
			break;
		}
	}

	/*************************************************************************/

	@Override
	public void visit(NegativeFactor NegativeFactor) {
		Code.put(Code.neg);
	}

	@Override
	public void visit(FactorDesignator fd) {
		Obj node = fd.getDesignator().obj;

		if (node.getType().getKind() != Struct.Array) {
			Code.load(node);
		} else {
			// ako je niz
			boolean ref = arrRefList.remove(arrRefList.size() - 1);
			if (ref) {
				Code.load(node);
			}
			// else ako je element niza
			else {
				switch (node.getType().getElemType().getKind()) {
				case Struct.Int:
					Code.put(Code.aload);
					break;
				case Struct.Char:
					Code.put(Code.baload);
					break;
				case Struct.Bool:
					Code.put(Code.aload);
					break;
				}
			}

		}
	}

	@Override
	public void visit(FactorFunction ff) {
		Obj node = ff.getDesignator().obj;

		if (node.getName().equalsIgnoreCase("ord")) {
			return;
		}
		if (node.getName().equalsIgnoreCase("chr")) {
			return;
		}
		if (node.getName().equalsIgnoreCase("len")) {
			Code.put(Code.arraylength);
			return;
		}

		int offset = node.getAdr() - Code.pc;

		Code.put(Code.call);
		Code.put2(offset);
	}

	@Override
	public void visit(FactorNewArray fna) {
		Code.put(Code.newarray);

		switch (fna.getType().struct.getKind()) {
		case Struct.Int:
			Code.put(1);
			break;
		case Struct.Char:
			Code.put(0);
			break;
		case Struct.Bool:
			Code.put(1);
			break;
		}
	}

	@Override
	public void visit(FactorNumber num) {
		// Load the node in Code
		Code.loadConst(num.getNumFact().intValue());
	}

	@Override
	public void visit(FactorCharacter chr) {
		// Load the node in Code
		char c = chr.getCharFact().charValue();
		int val = (int) c;
		Code.loadConst(val);
	}

	@Override
	public void visit(FactorBoolean bool) {
		// Load the node in Code
		Code.loadConst(bool.getBoolFact().equalsIgnoreCase("true") ? 1 : 0);
	}

	/*************************************************************************/

	@Override
	public void visit(BasicDesignator bd) {
		Obj node = bd.obj;

		if (node.getType().getKind() == Struct.Array) {
			arrRefList.add(true);
		}

	}

	@Override
	public void visit(ArrayName a) {
		Code.load(a.obj);
	}

	@Override
	public void visit(ArrayDesignatorIndex ArrayDesignatorIndex) {
		arrRefList.add(false);
	}

}
