package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class Compiler {

	static {
		// Pazi da 4 foldera mora da budu Source folderi inace baca exception
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public static void main(String[] args) throws Exception {

		Logger log = Logger.getLogger(Compiler.class);

		Reader br = null;
		try {

			// Dodati argument "test/program.mj" u konfiguraciji kao prvi argument
			String MJfilePath = args[0];

			File sourceCode = new File(MJfilePath);
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());

			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);

			log.info("===================================PARSIRANJE==========================================");
			MJParser p = new MJParser(lexer);
			Symbol s = p.parse(); // pocetak parsiranja

			Program prog = (Program) (s.value);
			Tab.init();
			// ispis sintaksnog stabla
			log.info("===================================SINTAKSNA OBRADA====================================");
			log.info("\n" + prog.toString(""));
			log.info("===================================SEMANTICKA OBRADA===================================");

			// ispis prepoznatih programskih konstrukcija
			SemanticAnalyzer v = new SemanticAnalyzer();
			prog.traverseBottomUp(v);

			log.info("=======================================================================================\n");
			SymbolTablePrinter.tsdump();

			if (!p.errorDetected && v.passed()) {
				log.info("Parsiranje uspesno zavrseno!");
				log.info("=======================================================================================\n");
				log.info("Pocinje faza generisanja MJVM koda!");

				// Dodati argument "test/program.obj" u konfiguraciji kao prvi argument
				String ObjfilePath = args[1];
				File objFile = new File(ObjfilePath);

				if (objFile.exists())
					objFile.delete();

				CodeGenerator g = new CodeGenerator();
				prog.traverseBottomUp(g);
				Code.mainPc = g.getMainPc();
				Code.dataSize = v.getProgramVarsNumber();
				int codeSize = Code.pc;
				if (codeSize + 14 <= 8000) {
					Code.write(new FileOutputStream(objFile));
					log.info("Zavrsena faza generisanja MJVM koda!");
				} else {
					log.error("Izvorni kod programa ne sme biti veci od 8 KB !");
				}

			} else {
				log.error("Parsiranje NIJE uspesno zavrseno!");
			}

		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e1) {
					log.error(e1.getMessage(), e1);
				}
		}

	}

}
