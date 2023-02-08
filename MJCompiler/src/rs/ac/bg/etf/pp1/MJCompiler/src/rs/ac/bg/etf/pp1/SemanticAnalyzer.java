package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	public static final Struct boolType = new Struct(Struct.Bool);
	private static final String is_true = "true";
	private static final String global = "Globalna";
	private static final String local = "Lokalna";

	private int programVarsNumber = 0;
	private static int methodsVarsNumber = 0;
	private static Struct currentType = null;
	private static Obj currentMethod = null;
	private static List<Obj> methodCallStack = new ArrayList<Obj>();
	private static List<Struct> methodArgumentList = new ArrayList<Struct>();
	private static List<Obj> designatorArrayAssign = new ArrayList<Obj>();
	private static List<Obj> immutableIterrators = new ArrayList<Obj>();
	private static List<String> func_args = new ArrayList<String>();
	private static boolean missingArgumens = false;
	private static boolean returnFound = false;
	private static String relop = "";
	private static List<Boolean> arrRefList = new ArrayList<Boolean>();
	private static int looplevel = 0;

	private static boolean main_function_found = false;

	private static int currentScopeLevel = 0;

	private String decodeKind(int i) {
		switch (i) {
		case 0:
			return "void";
		case 1:
			return "int";
		case 2:
			return "char";
		case 3:
			return "Array";
		case 4:
			return "";
		case 5:
			return "bool";
		}
		return "";
	}

	public int getProgramVarsNumber() {
		return programVarsNumber;
	}


	public SemanticAnalyzer() {
		Scope universe = Tab.currentScope();
		universe.addToLocals(new Obj(Obj.Type, "bool", boolType));
	}

	Logger log = Logger.getLogger(getClass());

	boolean errorDetected = false;

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.info(msg.toString());
	}

	/*************************** VISIT_METODE *********************************/

	/***************************
	 * PROGRAM
	 *************************************************************************************************************************************/

	@Override
	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getNameOfProgram(), Tab.noType);
		Tab.openScope();
		currentScopeLevel = 0;
	}

	@Override
	public void visit(Program program) {

		programVarsNumber = Tab.currentScope().getnVars();

		if (programVarsNumber > 65536) {
			report_error("Ne sme se koristiti vise od 65536 globalnih pormenljivih!", null);
		}

		if (main_function_found == false) {
			report_error("Glavna funkcija: void main() {...} nije pronadjena!", null);
		}

		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
	}

	/***************************
	 * TYPE
	 *************************************************************************************************************************************/

	@Override
	public void visit(Type type) {
		Obj node = Tab.find(type.getTypeName());
		if (node == Tab.noObj) {
			report_error("Semanticka greska na liniji " + type.getLine() + ": Nije pronadjen tip '" + type.getTypeName()
					+ "' u tabeli simbola! ", null);
			type.struct = Tab.noType;
		} else {
			if (node.getKind() == Obj.Type) {
				type.struct = node.getType();
			} else {
				report_error("Semanticka greska na liniji " + type.getLine() + ": Ime '" + type.getTypeName()
						+ "' ne predstavlja tip!", null);
				type.struct = Tab.noType;
			}
		}

		currentType = type.struct;
	}

	/***************************
	 * CONSTANT NUMBER
	 *******************************************************************************************************************************/

	@Override
	public void visit(ConstantNumber constantNumber) {
		Obj node = Tab.find(constantNumber.getConstName());
		if (node == Tab.noObj) {
			if (currentType == Tab.intType) {
				Obj constNum = Tab.insert(Obj.Con, constantNumber.getConstName(), currentType);
				constNum.setAdr(constantNumber.getConstNum().intValue());
			} else {
				String type = null;
				if (currentType.getKind() == 2)
					type = "char";
				if (currentType.getKind() == 5)
					type = "bool";
				report_error("Semanticka greska na liniji " + constantNumber.getLine() + ":  Globalna konstanta '"
						+ constantNumber.getConstName() + "' je tipa '" + type.toString()
						+ "' a dodeljuje joj se vrednost tipa 'int'", null);
			}
		} else {
			report_error("Semanticka greska na liniji " + constantNumber.getLine() + ": Globalna konstanta '"
					+ constantNumber.getConstName() + "' je vec definisana! ", null);
		}
	}

	/***************************
	 * CONSTANT CHARACTER
	 **************************************************************************************************************************/

	@Override
	public void visit(ConstantCharacter constantCharacter) {
		Obj node = Tab.find(constantCharacter.getConstName());
		if (node == Tab.noObj) {
			if (currentType == Tab.charType) {
				Obj constChar = Tab.insert(Obj.Con, constantCharacter.getConstName(), currentType);
				char c = constantCharacter.getConstChar().charValue();
				constChar.setAdr((int) c);
			} else {
				String type = null;
				if (currentType.getKind() == 1)
					type = "int";
				if (currentType.getKind() == 5)
					type = "bool";
				report_error("Semanticka greska na liniji " + constantCharacter.getLine() + ": Globalna konstanta '"
						+ constantCharacter.getConstName() + "' je tipa '" + type.toString()
						+ "' a dodeljuje joj se vrednost tipa 'char'", null);
			}

		} else {
			report_error("Semanticka greska na liniji " + constantCharacter.getLine() + ": Globalna konstanta '"
					+ constantCharacter.getConstName() + "' je vec definisana! ", null);
		}
	}

	/***************************
	 * CONSTANT BOOLEAN
	 *******************************************************************************************************************************/

	@Override
	public void visit(ConstantBoolean constantBoolean) {
		Obj node = Tab.find(constantBoolean.getConstName());
		if (node == Tab.noObj) {
			if (currentType == boolType) {
				Obj constBool = Tab.insert(Obj.Con, constantBoolean.getConstName(), currentType);
				String s = constantBoolean.getConstBool();
				if (s.equals(is_true)) {
					constBool.setAdr(1);
				} else {
					constBool.setAdr(0);
				}
			} else {
				String type = null;
				if (currentType.getKind() == 2)
					type = "char";
				if (currentType.getKind() == 5)
					type = "bool";
				report_error("Semanticka greska na liniji " + constantBoolean.getLine() + ": Globalna konstanta '"
						+ constantBoolean.getConstName() + "' je tipa '" + type.toString()
						+ "' a dodeljuje joj se vrednost tipa 'char'", null);
			}
		} else {
			report_error("Semanticka greska na liniji " + constantBoolean.getLine() + ": Globalna konstanta '"
					+ constantBoolean.getConstName() + "' je vec definisana! ", null);
		}
	}

	/***************************
	 * VARIABLE DECLARATION
	 *************************************************************************************************************/

	@Override
	public void visit(SimpleVariable var) {
		Obj node = Tab.currentScope().findSymbol(var.getVarName());
		if (node == null) {
			Tab.insert(Obj.Var, var.getVarName(), currentType);
		} else {
			String scopeString = currentScopeLevel == 0 ? global : local;
			String f = currentScopeLevel == 0 ? "" : " u funkciji '" + currentMethod.getName() + "' ";
			report_error("Semanticka greska na liniji " + var.getLine() + ": " + scopeString + " promenljiva '"
					+ var.getVarName() + "' je vec deklarisana" + f + "! ", null);
		}
	}

	@Override
	public void visit(ArrayVariable arr) {
		Obj node = Tab.currentScope().findSymbol(arr.getVarName());
		if (node == null) {
			Struct arrOfType = new Struct(Struct.Array);
			arrOfType.setElementType(currentType);
			Tab.insert(Obj.Var, arr.getVarName(), arrOfType);
		} else {
			String scopeString = currentScopeLevel == 0 ? global : local;
			String f = currentScopeLevel == 0 ? "" : " u funkciji '" + currentMethod.getName() + "' ";
			report_error("Semanticka greska na liniji " + arr.getLine() + ": " + scopeString + " promenljiva '"
					+ arr.getVarName() + "' je vec deklarisana" + f + "! ", null);
		}
	}

	/***************************
	 * METHOD DECLARATION
	 *************************************************************************************************************/

	@Override
	public void visit(VoidMethod voidMethod) {
		currentType = Tab.noType;
	}

	@Override
	public void visit(MethodSignature methodSignature) {
		Obj node = Tab.find(methodSignature.getMethodName());
		if (node == Tab.noObj) {
			currentMethod = Tab.insert(Obj.Meth, methodSignature.getMethodName(), currentType);
			methodSignature.obj = currentMethod;
			Tab.openScope();
			currentScopeLevel++;
		} else {
			report_error("Semanticka greska na liniji " + methodSignature.getLine() + ": Metoda '"
					+ methodSignature.getMethodName() + "' je vec definisana! ", null);
		}
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		if (currentMethod == null) return;
		methodsVarsNumber = Tab.currentScope.getnVars();

		if (methodsVarsNumber > 256) {
			report_error("Greska na liniji " + methodDecl.getLine() + ": Funkcija '" + currentMethod.getName()
					+ "' ne sme imati vise od 256 lokalnih promenljivih!", null);
		}
		if (!returnFound && (currentMethod.getType() != Tab.noType)) {
			report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funkcija '"
					+ currentMethod.getName() + "' nema return iskaz!", null);
		}
		if (methodDecl.getMethodSignature().getMethodName().equals("main") && (currentMethod.getType() == Tab.noType)
				&& (currentMethod.getLevel() == 0)) {
			main_function_found = true;
		}

		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		currentScopeLevel--;
		currentMethod = null;
		returnFound = false;
	}

	/***************************
	 * FORM PARS
	 *************************************************************************************************************/

	@Override
	public void visit(SimpleParameter param) {
		if (currentMethod == null) return;
		Obj node = Tab.currentScope().findSymbol(param.getParName());
		if (node == null) {
			Tab.insert(Obj.Var, param.getParName(), param.getType().struct);
			currentMethod.setLevel(currentMethod.getLevel() + 1);
		} else {
			report_error("Semanticka greska na liniji " + param.getLine() + ": Formalni parametar '"
					+ param.getParName() + "', metode '" + currentMethod.getName() + "' je vec definisan! ", null);
		}
	}

	@Override
	public void visit(ArrayParameter array) {
		if (currentMethod == null) return;
		Obj node = Tab.currentScope().findSymbol(array.getParName());
		if (node == null) {
			Struct arrOfType = new Struct(Struct.Array);
			arrOfType.setElementType(currentType);
			Tab.insert(Obj.Var, array.getParName(), arrOfType);
			currentMethod.setLevel(currentMethod.getLevel() + 1);
		} else {
			report_error("Semanticka greska na liniji " + array.getLine() + ": Formalni parametar '"
					+ array.getParName() + "', metode '" + currentMethod.getName() + "' je vec definisan! ", null);
		}
	}

	/***************************
	 * DESIGNATOR
	 *************************************************************************************************************/

	@Override
	public void visit(BasicDesignator var) {
		Obj node = Tab.currentScope().findSymbol(var.getVarName());
		if (node == null) {
			node = Tab.find(var.getVarName());
		}
		if (node == Tab.noObj) {
			report_error("Semanticka greska na liniji " + var.getLine() + ": '" + var.getVarName()
					+ "' nema deklaraciju u programu!", null);
			var.obj = Tab.noObj;
			return;
		}
		if (node.getKind() == Obj.Type) {
			report_error("Semanticka greska na liniji " + var.getLine() + ": Tip '" + var.getVarName()
					+ "' se ne moze koristiti kao promenljiva!", null);
			var.obj = Tab.noObj;
			return;
		} else {
			if (node.getKind() == Obj.Meth) {
				methodCallStack.add(node);
				var.obj = node;
				report_info("Detektovan simbol: Linija " + var.getLine() + " , Simbol "
						+ var.getVarName() + " , " + "Meth " + node.getName() + ": "
						+ decodeKind(node.getType().getKind()) + ", " + node.getAdr() + ", " + node.getLevel(), null);
				return;
			}
			if (node.getKind() == Obj.Var && node.getType().getKind() == Struct.Array) {
				arrRefList.add(true);
				var.obj = node;
				report_info("Detektovan simbol: Linija " + var.getLine() + " , Simbol " + var.getVarName() + " , "
						+ "Var " + node.getName() + ": Arr of " + decodeKind(node.getType().getElemType().getKind())
						+ ", " + node.getAdr() + ", " + node.getLevel(), null);
				return;
			}
			if (node.getKind() == Obj.Con) {
				var.obj = node;
				report_info("Detektovan simbol: Linija " + var.getLine() + " , Simbol " + var.getVarName() + " , "
						+ "Con " + node.getName() + ": " + decodeKind(node.getType().getKind()) + ", " + node.getAdr()
						+ ", " + node.getLevel(), null);
				return;
			}
			var.obj = node;
			report_info("Detektovan simbol: Linija " + var.getLine() + " , Simbol " + var.getVarName() + " , " + "Var "
					+ node.getName() + ": " + decodeKind(node.getType().getKind()) + ", " + node.getAdr() + ", "
					+ node.getLevel(), null);

		}
	}

	@Override
	public void visit(ArrayDesignatorIndex arr) {
		Obj node = arr.getArrayName().obj;
		if (node == Tab.noObj) {
			arr.obj = Tab.noObj;
		} else {
			if (arr.getExpr().struct == Tab.intType) {
				arr.obj = node;
				arrRefList.add(false);
				report_info(
						"Detektovan pristup elementu niza: Linija " + arr.getLine() + " , Simbol "
								+ arr.getArrayName().getArrName() + "[idx] , " + "Var " + node.getName() + "[idx]: "
								+ decodeKind(node.getType().getElemType().getKind()) + ", " + node.getAdr() + ", " + node.getLevel(),
						null);
			} else {
				report_error("Semanticka greska na liniji " + arr.getLine() + ": Indeks niza '"
						+ arr.getArrayName().getArrName() + "' mora biti tipa int!", null);
				arr.obj = Tab.noObj;
			}
		}
	}

	@Override
	public void visit(ArrayName arr) {
		Obj node = Tab.currentScope().findSymbol(arr.getArrName());
		if (node == null) {
			node = Tab.find(arr.getArrName());
		}
		if (node == Tab.noObj) {
			report_error("Semanticka greska na liniji " + arr.getLine() + ": Promenljiva '" + arr.getArrName()
					+ "' nema deklaraciju u programu!", null);
			arr.obj = Tab.noObj;
		} else {
			if (node.getType().getKind() == Struct.Array) {
				arr.obj = node;
				report_info("Detektovan simbol: Linija " + arr.getLine() + " , Simbol " + arr.getArrName() + " , "
						+ "Var " + node.getName() + ": Arr of " + decodeKind(node.getType().getElemType().getKind())
						+ ", " + node.getAdr() + ", " + node.getLevel(), null);
			} else {
				report_error("Semanticka greska na liniji " + arr.getLine() + ": Promenljiva '" + arr.getArrName()
						+ "' nije tipa Array!", null);
				arr.obj = Tab.noObj;
			}
		}
	}

	/***************************
	 * STATEMENT
	 *************************************************************************************************************/

	@Override
	public void visit(WhileDummyStart WhileDummyStart) {
		looplevel++;
	}

	@Override
	public void visit(WhileDummyEnd WhileDummyEnd) {
		looplevel--;
	}
	
	@Override
	public void visit(ForeachDummyStart ForeachDummyStart) {
		looplevel++;
	}
	
	@Override
	public void visit(ForeachDummyEnd ForeachDummyEnd) {
		looplevel--;
	}

	@Override
	public void visit(BreakStmt breakStmt) {
		if (looplevel == 0) {
			report_error("Semanticka greska na liniji " + breakStmt.getParent().getLine()
					+ ": Naredba 'break' se mora nalaziti u okviru while ili foreach petlje!", null);
		}
	}

	@Override
	public void visit(ContinueStmt continueStmt) {
		if (looplevel == 0) {
			report_error("Semanticka greska na liniji " + continueStmt.getParent().getLine()
					+ ": Naredba 'continue' se mora nalaziti u okviru while ili foreach petlje!", null);
		}
	}

	@Override
	public void visit(ReadStmt st) {
		Obj var = st.getDesignator().obj;
		if (var == Tab.noObj || var.getKind() != Obj.Var || (var.getType().getKind() == Struct.Array
				&& arrRefList.get(arrRefList.size() - 1).booleanValue() == true)) {
			if (var.getType().getKind() == Struct.Array)
				arrRefList.remove(arrRefList.size() - 1);
			methodCallStack.clear();
			report_error("Semanticka greska na liniji " + st.getParent().getLine()
					+ ": Argument funkcije 'read' moze biti samo promenljiva ili element niza", null);
			return;
		}
		if (immutableIterrators.contains(var)) {
			report_error("Semanticka greska na liniji " + st.getLine() + ": Iteratoru se ne moze menjati vrednost!",
					null);
			return;
		}
		Struct t = var.getType();
		if (t.getKind() == Struct.Array) {
			t = var.getType().getElemType();
			arrRefList.remove(arrRefList.size() - 1);
		}
		if (t == Tab.intType || t == Tab.charType || t == boolType) {
			/* ok */
		} else {
			report_error("Semanticka greska na liniji " + st.getParent().getLine()
					+ ": Argument funkcije 'read' mora biti tipa int, char ili bool!", null);
		}
	}

	@Override
	public void visit(PrintExprStmt st) {
		Struct t = st.getExpr().struct;
		if (t == Tab.intType || t == Tab.charType || t == boolType) {
			/* ok */
		} else {
			report_error("Semanticka greska na liniji " + st.getParent().getLine()
					+ ": Argument funkcije 'print' mora biti tipa int, char ili bool!", null);
		}
	}

	@Override
	public void visit(ReturnStmt st) {
		if (currentMethod == null) {
			report_error("Semanticka greska na liniji " + st.getParent().getLine()
					+ ": Naredba 'return' se ne sme nalaziti van definicije funkcije!", null);
			return;
		}
		if (currentMethod.getType() != Tab.noType) {
			report_error(
					"Semanticka greska na liniji " + st.getParent().getLine() + ": Funkcija nije deklarisana kao void!",
					null);
		}
		returnFound = true;
		/* ok */
	}

	@Override
	public void visit(ReturnExprStmt st) {
		if (currentMethod == null) {
			report_error("Semanticka greska na liniji " + st.getParent().getLine()
					+ ": Naredba 'return' se ne sme nalaziti van definicije funkcije!", null);
			return;
		}
		if (currentMethod.getType() != st.getExpr().struct) {
			report_error("Semanticka greska na liniji " + st.getParent().getLine()
					+ ": Funkcija mora da vrati vrednost tipa '" + decodeKind(currentMethod.getType().getKind()) + "'!",
					null);
		}
		returnFound = true;
		/* ok */
	}

	@Override
	public void visit(ArrayToIterateOver var) {
		if (var.getDesignator().obj.getType().getKind() == Struct.Array
				&& arrRefList.get(arrRefList.size() - 1).booleanValue()) {
			arrRefList.remove(arrRefList.size() - 1);
			var.struct = var.getDesignator().obj.getType().getElemType();
		} else {
			var.struct = Tab.noType;
			methodCallStack.clear();
			report_error("Semanticka greska na liniji " + var.getParent().getLine()
					+ ": Foreach petlja se moze izvrsavati samo nad nizovima!", null);
		}
	}

	@Override
	public void visit(ImmutableIterator it) {
		Obj node = Tab.currentScope().findSymbol(it.getVarName());
		if (node == null) {
			node = Tab.find(it.getVarName());
		}
		if (node == Tab.noObj) {
			report_error("Semanticka greska na liniji " + it.getLine() + ": '" + it.getVarName()
					+ "' nema deklaraciju u programu!", null);
			return;
		}
		if (node.getKind() == Obj.Type) {
			report_error("Semanticka greska na liniji " + it.getLine() + ": Tip '" + it.getVarName()
					+ "' se ne moze koristiti kao promenljiva!", null);
			node = Tab.noObj;
			return;
		} else {
			if (node.getKind() == Obj.Var && node.getType().getKind() != Struct.Array) {
				it.obj = node;
				report_info("Detektovan simbol: Linija " + it.getLine() + " , Simbol " + it.getVarName() + " , " + "Var "
						+ node.getName() + ": " + decodeKind(node.getType().getKind()) + ", " + node.getAdr() + ", "
						+ node.getLevel(), null);
			} else {
				report_error("Semanticka greska na liniji " + it.getLine() + ": '" + it.getVarName()
						+ "' mora biti lokalna ili globalna promenljiva!", null);
			}
			node = Tab.noObj;
		}
	}

	@Override
	public void visit(ForEachLoop loop) {
		Struct t = loop.getArrayToIterateOver().struct;
		if (t == Tab.noType || t == null)
			return;
		Obj var = loop.getImmutableIterator().obj;
		if (var == Tab.noObj || var == null)
			return;
		if (t.compatibleWith(var.getType())) {
			if (immutableIterrators.contains(var)) {
				report_error("Semanticka greska na liniji " + loop.getLine() + ": Promenljiva '" + var.getName()
						+ "' se vec koristi kao iterator u spoljasnjoj foreach petlji!", null);
				return;
			}
			immutableIterrators.add(var);
		} else {
			report_error("Semanticka greska na liniji " + loop.getLine() + ": Promenljiva '" + var.getName()
					+ "' nije kompatibilna sa tipom '" + decodeKind(t.getKind()) + "' niza nad kojim se iterira!",
					null);
		}
	}

	@Override
	public void visit(ForEachStmt st) {
		if (!immutableIterrators.isEmpty()) {
			immutableIterrators.remove(immutableIterrators.size() - 1);
		}
		arrRefList.clear();
	}

	/***************************
	 * DESIGNATOR STATEMENT
	 *************************************************************************************************************/

	@Override
	public void visit(DesignatorAssignStmtOK ds) {
		Obj var = ds.getDesignator().obj;
		if (var == Tab.noObj) return;
		if (var.getKind() == Obj.Var) {
			if (var.getType().getKind() == Struct.Array) {
				Struct t = var.getType();

				if (arrRefList.remove(arrRefList.size() - 1).booleanValue() == false) {
					t = var.getType().getElemType();
				}

				if (t.compatibleWith(ds.getExpr().struct)) {
					/* ok */
				} else {
					report_error("Semanticka greska na liniji " + ds.getLine()
							+ ": Greska u dodeli vrednosti promenljivoj '" + var.getName() + "' !", null);
				}

			} else { // promenljiva nije niz

				if (immutableIterrators.contains(var)) {
					report_error(
							"Semanticka greska na liniji " + ds.getLine() + ": Iteratoru se ne moze menjati vrednost!",
							null);
					return;
				}

				Struct t = var.getType();
				if (t.compatibleWith(ds.getExpr().struct)) {
					/* ok */
				} else {
					report_error("Semanticka greska na liniji " + ds.getLine()
							+ ": Greska u dodeli vrednosti promenljivoj '" + var.getName() + "' !", null);
				}
			}
		} else if (var.getKind() == Obj.Con) {
			report_error("Semanticka greska na liniji " + ds.getLine() + ": Konstntama se ne moze menjati vrednost!",
					null);
		} else {
			methodCallStack.clear();
			report_error("Semanticka greska na liniji " + ds.getLine()
					+ ": Sa leve strane znaka '=' mora stojati promenljiva ili element niza!", null);
		}
		arrRefList.clear();
	}

	@Override
	public void visit(DesignatorIncrementStmt ds) {
		Obj var = ds.getDesignator().obj;
		if (var.getKind() == Obj.Var) {

			if (var.getType().getKind() == Struct.Array) {
				if (arrRefList.remove(arrRefList.size() - 1).booleanValue() == true) {
					report_error("Semanticka greska na liniji " + ds.getLine()
							+ ": Operacija '++' ne funkcionise na niz, mora element niza!", null);
				} else {
					Struct t = var.getType().getElemType();
					if (t.compatibleWith(Tab.intType)) {
						/* ok */
					} else {
						report_error("Semanticka greska na liniji " + ds.getLine()
								+ ": Operacija '++' radi samo nad promenljivama tipa int!", null);
					}
				}
			} else {
				if (immutableIterrators.contains(var)) {
					report_error(
							"Semanticka greska na liniji " + ds.getLine() + ": Iteratoru se ne moze menjati vrednost!",
							null);
					return;
				}
				Struct t = var.getType();
				if (t.compatibleWith(Tab.intType)) {
					/* ok */
				} else {
					report_error("Semanticka greska na liniji " + ds.getLine()
							+ ": Operacija '++' radi samo nad promenljivama tipa int!", null);
				}
			}
		} else if (var.getKind() == Obj.Con) {
			report_error("Semanticka greska na liniji " + ds.getLine() + ": Konstntama se ne moze menjati vrednost!",
					null);
		} else {
			methodCallStack.clear();
			report_error("Semanticka greska na liniji " + ds.getLine()
					+ ": Operacija '++' radi samo na promenljivama ili elementima niza!", null);
		}
	}

	@Override
	public void visit(DesignatorDecrementStmt ds) {
		Obj var = ds.getDesignator().obj;
		if (var.getKind() == Obj.Var) {
			if (var.getType().getKind() == Struct.Array) {
				if (arrRefList.remove(arrRefList.size() - 1).booleanValue() == true) {
					report_error("Semanticka greska na liniji " + ds.getLine()
							+ ": Operacija '--' ne funkcionise na niz, mora element niza!", null);
				} else {
					Struct t = var.getType().getElemType();
					if (t.compatibleWith(Tab.intType)) {
						/* ok */
					} else {
						report_error("Semanticka greska na liniji " + ds.getLine()
								+ ": Operacija '--' radi samo nad promenljivama tipa int!", null);
					}
				}
			} else {
				if (immutableIterrators.contains(var)) {
					report_error(
							"Semanticka greska na liniji " + ds.getLine() + ": Iteratoru se ne moze menjati vrednost!",
							null);
					return;
				}
				Struct t = var.getType();
				if (t.compatibleWith(Tab.intType)) {
					/* ok */
				} else {
					report_error("Semanticka greska na liniji " + ds.getLine()
							+ ": Operacija '--' radi samo nad promenljivama tipa int!", null);
				}
			}
		} else if (var.getKind() == Obj.Con) {
			report_error("Semanticka greska na liniji " + ds.getLine() + ": Konstntama se ne moze menjati vrednost!",
					null);
		} else {
			methodCallStack.clear();
			report_error("Semanticka greska na liniji " + ds.getLine()
					+ ": Operacija '--' radi samo na promenljivama ili elementima niza!", null);
		}
	}

	@Override
	public void visit(DesignatorFunctionStmt ds) {
		Obj d = ds.getDesignator().obj;
		if (d != Tab.noObj) {
			if (d.getKind() == Obj.Meth) {
				func_args.clear();
				methodCallStack.clear();
				methodArgumentList.clear();
				/* ok */
			} else {
				report_error("Semanticka greska na liniji " + ds.getLine() + ": '" + d.getName() + "' nije funkcija!",
						null);
			}
		}
	}

	@Override
	public void visit(DesignatorArrayAssignStmt ds) {
		Obj niz = ds.getDesignator().obj;
		if (niz.getType().getKind() == Struct.Array) {

			if (arrRefList.remove(arrRefList.size() - 1).booleanValue() == false) {
				report_error("Semanticka greska na liniji " + ds.getParent().getLine()
						+ ": Sa desne strane znaka '=' mora biti niz!", null);
			} else {
				while (!designatorArrayAssign.isEmpty()) {
					Obj var = designatorArrayAssign.remove(0);
					if (var == Tab.noObj)
						continue;
					if (immutableIterrators.contains(var)) {
						report_error("Semanticka greska na liniji " + ds.getLine()
								+ ": Iteratoru se ne moze menjati vrednost!", null);
						return;
					}
					Struct t = var.getType();
					if (var.getType().getKind() == Struct.Array) {
						t = var.getType().getElemType();
					}
					if (t.compatibleWith(niz.getType().getElemType())) {
						/* ok */
					} else {
						report_error("Semanticka greska na liniji " + ds.getParent().getLine()
								+ ": Nekompatibilnost tipova izmedju elemenata niza '" + niz.getName() + "' i polja '"
								+ var.getName() + "'!", null);
					}
				}
			}
		} else {
			report_error("Semanticka greska na liniji " + ds.getLine() + ": Sa desne strane znaka '=' mora biti niz!",
					null);
		}
		methodCallStack.clear();
	}

	@Override
	public void visit(NoOptDesignator NoOptDesignator) {
		designatorArrayAssign.add(Tab.noObj);
	}

	@Override
	public void visit(IncludeOptDesignator d) {
		Obj var = d.getDesignator().obj;
		if (var.getKind() == Obj.Con) {
			report_error("Semanticka greska na liniji " + d.getLine() + ": Konstntama se ne moze menjati vrednost!",
					null);
			return;
		}
		if (var.getKind() == Obj.Var) {
			if (var.getType().getKind() == Struct.Array) {
				if (arrRefList.remove(arrRefList.size() - 1).booleanValue()) {
					report_error(
							"Semanticka greska na liniji " + d.getLine()
									+ ": Ne sme se koristiti niz sa leve strane, samo promenljive i elementi niza!",
							null);
					designatorArrayAssign.add(Tab.noObj);
					return;
				}
			}
			designatorArrayAssign.add(var);
		} else {
			report_error("Semanticka greska na liniji " + d.getLine()
					+ ": Smeju da se koristite samo promenljive i elementi niza!", null);
			designatorArrayAssign.add(Tab.noObj);
		}
	}

	/***************************
	 * ACTUAL PARS
	 *************************************************************************************************************/

	@Override
	public void visit(NoActPar noActPar) {
		if (methodCallStack.isEmpty()) {
			return;
		}
		Obj method = methodCallStack.remove(methodCallStack.size() - 1);
		if (method.getLevel() == 0) {
			missingArgumens = false;
		} else {
			report_error("Semanticka greska na liniji " + noActPar.getParent().getLine() + ": Funkcija '"
					+ method.getName() + "' zahteva argumente!", null);
			missingArgumens = true;
		}
	}

	@Override
	public void visit(IncludeActPar includeActPar) {
		if (methodCallStack.isEmpty()) {
			methodArgumentList.clear();
			return;
		}
		Obj method = methodCallStack.remove(methodCallStack.size() - 1);
		int nArgs = method.getLevel();
		if (nArgs == methodArgumentList.size()) {
			if (method.getLocalSymbols().isEmpty()) {
				report_error("Semanticka greska na liniji " + includeActPar.getLine()
						+ ": Nije dozvoljen rekurzivan poziv funkcija!", null);
				return;
			}
			Object[] params = method.getLocalSymbols().toArray();
			for (int i = 0; i < nArgs; i++) {
				Struct t = methodArgumentList.remove(methodArgumentList.size() - 1);
				if (t == Tab.noType) {
					report_error(
							"Semanticka greska na liniji " + includeActPar.getLine() + ": Tipovi argumenata funkcije '"
									+ method.getName() + "' se ne poklapaju sa njenom definicijom!",
							null);
					missingArgumens = true;
					methodArgumentList.clear();
					return;
				}
				if (method.getName().equals("len")) {
					if (t.getKind() == Struct.Array) {
						missingArgumens = false;
						return;
					} else {
						report_error("Semanticka greska na liniji " + includeActPar.getLine()
								+ ": Tipovi argumenata funkcije '" + method.getName()
								+ "' se ne poklapaju sa njenom definicijom!", null);
						missingArgumens = true;
						methodArgumentList.clear();
						return;
					}
				}
				if (((Obj) params[i]).getType().compatibleWith(t) == false) {
					report_error(
							"Semanticka greska na liniji " + includeActPar.getLine() + ": Tipovi argumenata funkcije '"
									+ method.getName() + "' se ne poklapaju sa njenom definicijom!",
							null);
					missingArgumens = true;
					methodArgumentList.clear();
					return;
				}
			}
			missingArgumens = false;
			String args = "";
			for (int i = 0; i < nArgs; i++) {
				args = args + ", " + func_args.remove(func_args.size() - 1);
			}
			args = args.replaceFirst(",", "");
			report_info("Detektovano koriscenje simbola kao argumenti funkcije: Linija " + includeActPar.getLine()
					+ ", Funkcija " + method.getName() + ", Simboli" + args, null);
		} else {
			report_error("Semanticka greska na liniji " + includeActPar.getLine() + ": Funkcija '" + method.getName()
					+ "' zahteva " + nArgs + " argumenata, a prosledjeno joj je " + methodArgumentList.size() + " !",
					null);
			missingArgumens = true;
			methodArgumentList.clear();
		}
	}

	@Override
	public void visit(OneExprPar par) {
		methodArgumentList.add(par.getExpr().struct);
	}

	@Override
	public void visit(MultipleExprPars par) {
		methodArgumentList.add(par.getExpr().struct);
	}

	/***************************
	 * EXPRESIONS
	 *************************************************************************************************************/

	@Override
	public void visit(SingleTermExpr expr) {
		expr.struct = expr.getTerm().struct;
	}

	@Override
	public void visit(MultipleTermExprs mtxs) {
		Struct t1 = mtxs.getTerm().struct;
		Struct t2 = mtxs.getExpr().struct;
		if (t1 == Tab.intType && t2 == Tab.intType) {
			mtxs.struct = t1;
		} else {
			report_error("Semanticka greska na liniji " + mtxs.getLine() + ": Tipovi '" + decodeKind(t1.getKind())
					+ "' i '" + decodeKind(t2.getKind()) + "' nisu kompatibilni za operaciju + , - !", null);
			mtxs.struct = Tab.noType;
		}
	}

	/***************************
	 * TERMS
	 *************************************************************************************************************/

	@Override
	public void visit(SingleFactorTerm term) {
		term.struct = term.getSFactor().struct;
	}

	@Override
	public void visit(MultipleFactorTerms mfts) {
		Struct t1 = mfts.getSFactor().struct;
		Struct t2 = mfts.getTerm().struct;
		if (t1 == Tab.intType && t2 == Tab.intType) {
			mfts.struct = t1;
		} else {
			report_error("Semanticka greska na liniji " + mfts.getLine() + ": Tipovi '" + decodeKind(t1.getKind())
					+ "' i '" + decodeKind(t2.getKind()) + "' nisu kompatibilni za operaciju * , / , % !", null);
			mfts.struct = Tab.noType;
		}
	}

	/***************************
	 * SIGNED FACTOR
	 *************************************************************************************************************/

	@Override
	public void visit(PositiveFactor sfactor) {
		sfactor.struct = sfactor.getFactor().struct;
	}

	@Override
	public void visit(NegativeFactor nfactor) {
		Struct t = nfactor.getFactor().struct;
		if (t == Tab.intType) {
			nfactor.struct = t;
		} else {
			report_error("Semanticka greska na liniji " + nfactor.getLine() + ": Tip '" + decodeKind(t.getKind())
					+ "' ne moze imati negativnu vrednost!", null);
			nfactor.struct = Tab.noType;
		}
	}

	/***************************
	 * FACTOR
	 *************************************************************************************************************/

	@Override
	public void visit(FactorNumber factor) {
		factor.struct = Tab.intType;
		if (!methodCallStack.isEmpty()) {
			func_args.add("'"+factor.getNumFact().toString()+"'");
		}
	}

	@Override
	public void visit(FactorCharacter factor) {
		factor.struct = Tab.charType;
		if (!methodCallStack.isEmpty()) {
			func_args.add("'"+factor.getCharFact().toString()+"'");
		}
	}

	@Override
	public void visit(FactorBoolean factor) {
		factor.struct = boolType;
		if (!methodCallStack.isEmpty()) {
			func_args.add("'"+factor.getBoolFact()+"'");
		}
	}

	@Override
	public void visit(FactorNewArray factor) {
		Struct t = factor.getExpr().struct;
		if (t == Tab.intType) {
			factor.struct = new Struct(Struct.Array, factor.getType().struct);
		} else {
			report_error("Semanticka greska na liniji " + factor.getLine()
					+ ": Dinamicko alociranje niza mora imati int vrednost za broj elemenata niza!", null);
			factor.struct = Tab.noType;
		}
	}

	@Override
	public void visit(FactorExpresion factor) {
		factor.struct = factor.getExpr().struct;
	}

	@Override
	public void visit(FactorDesignator factor) {
		Obj d = factor.getDesignator().obj;
		if (d != Tab.noObj) {
			if (d.getType().getKind() == Struct.Array
					&& arrRefList.get(arrRefList.size() - 1).booleanValue() == false) {
				arrRefList.remove(arrRefList.size() - 1);
				factor.struct = d.getType().getElemType();
				if (!methodCallStack.isEmpty()) {
					func_args.add(d.getName());
				}
			} else if (d.getType().getKind() == Struct.Array
					&& arrRefList.get(arrRefList.size() - 1).booleanValue() == true) {
				arrRefList.remove(arrRefList.size() - 1);
				factor.struct = d.getType();
				if (!methodCallStack.isEmpty()) {
					func_args.add(d.getName());
				}
			} else if (d.getKind() == Obj.Meth) {
				methodCallStack.remove(methodCallStack.size() - 1);
				factor.struct = Tab.noType;
			} else {
				factor.struct = d.getType();
				if (!methodCallStack.isEmpty()) {
					func_args.add(d.getName());
				}
			}
		} else {
			factor.struct = Tab.noType;
		}
	}

	@Override
	public void visit(FactorFunction factor) {
		Obj d = factor.getDesignator().obj;
		if (d != Tab.noObj) {
			if (d.getKind() == Obj.Meth) {
				if (!missingArgumens) {
					factor.struct = d.getType();
					report_info("Detektovan poziv globalne funkcije: Linija " + factor.getLine() + " , Simbol "
							+ d.getName() + " , " + "Meth " + d.getName() + ": "
							+ decodeKind(d.getType().getKind()) + ", " + d.getAdr() + ", " + d.getLevel(), null);
					if (d.getType() == Tab.noType) {
						factor.struct = Tab.noType;
						report_error("Semanticka greska na liniji " + factor.getLine() + ": Funkcija '" + d.getName()
						+ "' nema povratnu vrednost i ne moze se koristiti u izrazima!", null);
					}
					if (!methodCallStack.isEmpty()) {
						func_args.add(d.getName());
					}
				} else {
					factor.struct = Tab.noType;
				}
			} else {
				report_error("Semanticka greska na liniji " + factor.getLine() + ": Promenljiva '" + d.getName()
						+ "' nije funkcija!", null);
				factor.struct = Tab.noType;
			}
		} else {
			factor.struct = Tab.noType;
		}
	}

	/***************************
	 * CONDITION
	 *************************************************************************************************************/

	@Override
	public void visit(SingleCondTerm cond) {
		if (cond.getCondTerm().struct != boolType) {
			report_error("Semanticka greska na liniji " + cond.getLine() + ": Tip izraza u uslovu mora biti 'bool' !",
					null);
			cond.struct = Tab.noType;
		} else {
			cond.struct = boolType;
		}
	}

	@Override
	public void visit(MultipleCondTerms cond) {
		Struct c1 = cond.getCondTerm().struct;
		Struct c2 = cond.getCondition().struct;
		if (c1 == Tab.noType || c2 == Tab.noType) {
			report_error("Semanticka greska na liniji " + cond.getLine() + ": Tip izraza u uslovu mora biti 'bool' !",
					null);
			cond.struct = Tab.noType;
		} else {
			cond.struct = boolType;
		}
	}

	/***************************
	 * CONDITION TERMS
	 *************************************************************************************************************/

	@Override
	public void visit(SingleCondFact term) {
		if (term.getCondFact().struct != boolType) {
			term.struct = Tab.noType;
		} else {
			term.struct = boolType;
		}
	}

	@Override
	public void visit(MultipleCondFacts term) {
		Struct c1 = term.getCondFact().struct;
		Struct c2 = term.getCondTerm().struct;
		if (c1 == Tab.noType || c2 == Tab.noType) {
			term.struct = Tab.noType;
		} else {
			term.struct = boolType;
		}
	}

	/***************************
	 * CONDITION FACTS
	 *************************************************************************************************************/

	@Override
	public void visit(OneCondExpr fact) {
		if (fact.getExpr().struct != boolType) {
			fact.struct = Tab.noType;
		} else {
			fact.struct = boolType;
		}
	}

	@Override
	public void visit(TwoCondExprs expr) {
		Struct c1 = expr.getExpr().struct;
		Struct c2 = expr.getExpr1().struct;
		if (!c1.compatibleWith(c2)) {
			report_error("Semanticka greska na liniji " + expr.getLine()
					+ ": Tipovi u izrazu nisu kompatibilni za poredjenje!", null);
			expr.struct = Tab.noType;
		} else {
			if (c1.getKind() == Struct.Array && c2.getKind() == Struct.Array) {
				if (relop.matches("==") || relop.matches("!=")) {
					expr.struct = boolType;
				} else {
					report_error("Semanticka greska na liniji " + expr.getLine()
							+ ": Nizovi se mogu porediti samo sa operatorima '==' i '!=' !", null);
					expr.struct = Tab.noType;
				}
			} else {
				expr.struct = boolType;
			}
		}
	}

	/***************************
	 * RELATIONAL OPERATIONS
	 *************************************************************************************************************/

	@Override
	public void visit(IsEqual IsEqual) {
		relop = "==";
	}

	@Override
	public void visit(IsNotEqual IsNotEqual) {
		relop = "!=";
	}

	@Override
	public void visit(IsLessEqual IsLessEqual) {
		relop = "<=";
	}

	@Override
	public void visit(IsLess IsLess) {
		relop = "<";
	}

	@Override
	public void visit(IsGreaterEqual IsGreaterEqual) {
		relop = ">=";
	}

	@Override
	public void visit(IsGreater IsGreater) {
		relop = ">";
	}

	/************************************************************************/

	public boolean passed() {
		return !errorDetected;
	}

}
