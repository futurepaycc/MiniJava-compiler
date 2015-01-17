package compiler;import syntaxtree.Program;import frontend.generated.*;import ir.Declaration;import ir.Frame;import ir.visitor.StringVisitor;import java.io.FileNotFoundException;import java.io.PrintWriter;import java.util.Collection;import visitor.*;public class RamCompiler {    static PrintWriter debug = new PrintWriter(System.out);    public static void main(String[] args) throws FileNotFoundException {        Program root = null;        try {            // from standard input            if (args.length == 0)                root = new frontend.generated.RamParser(System.in).Goal();            else {                java.io.InputStream is = new java.io.FileInputStream(new java.io.File(args[0]));                root = new frontend.generated.RamParser(is).Goal();            }        } catch (ParseException e) {            System.err.println(e.toString());            return;        } catch (java.io.FileNotFoundException e) {            System.err.println("File Not Found: "+ e);            return;        }                  System.out.println("Program lexed and parsed successfully");                System.out.println("Abstract syntax tree built");                // prints AST        root.accept(new ASTPrintVisitor());                // build symbol table        BuildSymbolTableVisitor v = new BuildSymbolTableVisitor();  // note that this is necessary so we remember our table        root.accept(v); // build symbol table        System.out.println("Symbol Table built");                // print symbol table        System.out.println("Begin print of symbol table");        System.out.println(v.getSymTab());        System.out.println("End print of symbol table");                // perform type checking        TypeCheckVisitor typeChecker = new TypeCheckVisitor(v.getSymTab());        root.accept(typeChecker);        System.out.println("Semantic Analysis: Type Checking complete");                if(typeChecker.getErrorMsg().getHasErrors())        {            System.out.println("Error in front-end. Exiting.");            System.exit(1);        }                IrGenerator irGenerator = new IrGenerator(v.getSymTab());        root.accept(irGenerator);                Collection<Declaration> declarations = irGenerator.getDeclarationList();                java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(new java.io.File(args[0]).toString() +".s"));        ir.backend.X86CodeGenerator backend = new ir.backend.X86CodeGenerator(ps);        StringVisitor stringVisitor = new StringVisitor(System.out);                System.out.println("IR:");                for(Declaration declaration : declarations)        {        	declaration.accept(stringVisitor);        	declaration.accept(backend);        }            }}