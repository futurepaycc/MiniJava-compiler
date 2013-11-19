package visitor;import syntaxtree.*;import symboltable.*;import visitor.error.ErrorMsg;public class TypeCheckVisitor extends DepthFirstVisitor{    private ErrorMsg error = new ErrorMsg();        private RamClass currClass;    private RamMethod currMethod;    private Table symbolTable;        private TypeCheckExpVisitor expVisitor;    public TypeCheckVisitor(Table s)    {        symbolTable = s;        expVisitor = new TypeCheckExpVisitor(error, s);    }            private TypeCheckExpVisitor getExpVisitor()    {        expVisitor.currClass = currClass;        expVisitor.currMethod = currMethod;        return expVisitor;    }        // MainClass m;    // ClassDeclList cl;    public void visit(Program n)    {        n.m.accept(this);        for (int i = 0; i < n.cl.size(); i++)        {            n.cl.elementAt(i).accept(this);        }    }    // Identifier i1,i2;    // Statement s;    public void visit(MainClass n)    {        String i1 = n.i1.toString();        currClass = symbolTable.getClass(i1);        n.i2.accept(this);        n.s.accept(this);    }    // Identifier i;    // VarDeclList vl;    // MethodDeclList ml;    public void visit(ClassDeclSimple n)    {        String id = n.i.toString();        currClass = symbolTable.getClass(id);        for (int i = 0; i < n.vl.size(); i++)        {            n.vl.elementAt(i).accept(this);        }        for (int i = 0; i < n.ml.size(); i++)        {            n.ml.elementAt(i).accept(this);        }    }    // Identifier i;    // Identifier j;    // VarDeclList vl;    // MethodDeclList ml;    public void visit(ClassDeclExtends n)    {        String id = n.i.toString();        currClass = symbolTable.getClass(id);        n.j.accept(this);        for (int i = 0; i < n.vl.size(); i++)        {            n.vl.elementAt(i).accept(this);        }        for (int i = 0; i < n.ml.size(); i++)        {            n.ml.elementAt(i).accept(this);        }    }    // Type t;    // Identifier i;    public void visit(VarDecl n)    {        n.t.accept(this);        n.i.accept(this);    }    // Type t;    // Identifier i;    // FormalList fl;    // VarDeclList vl;    // StatementList sl;    // Exp e;    public void visit(MethodDecl n)    {        n.t.accept(this);        String id = n.i.toString();        currMethod = currClass.getMethod(id);        Type retType = currMethod.type();        for (int i = 0; i < n.fl.size(); i++)        {            n.fl.elementAt(i).accept(this);        }        for (int i = 0; i < n.vl.size(); i++)        {            n.vl.elementAt(i).accept(this);        }        for (int i = 0; i < n.sl.size(); i++)        {            n.sl.elementAt(i).accept(this);        }        if (symbolTable.compareTypes(retType, n.e.accept(getExpVisitor())) == false)        {            error.complain("Wrong return type for method " + id);                    }    }    // Type t;    // Identifier i;    public void visit(Formal n)    {        n.t.accept(this);        n.i.accept(this);    }    // Exp e;    // Statement s1,s2;    public void visit(If n)    {        if (!(n.e.accept(getExpVisitor()) instanceof BooleanType))        {            error.complain("The condition of if must be of type boolean");                    }        n.s1.accept(this);        n.s2.accept(this);    }    // Exp e;    // Statement s;    public void visit(While n)    {        if (!(n.e.accept(getExpVisitor()) instanceof BooleanType))        {            error.complain("The condition of while must be of type boolean");                    }        n.s.accept(this);    }    // Exp e;    // Statement s;    public void visit(ForEach n)    {                if (!(n.iterator.accept(getExpVisitor()) instanceof IntegerType))        {            error.complain("The type of foreach declaration must be of type integer");                    }        if(!(n.source.accept(getExpVisitor()) instanceof IntArrayType))        {            error.complain("The source of a foreach must be of type integer array");        }        n.statement.accept(this);    }        // Exp e;    public void visit(Print n)    {        for (Exp exp : n.e)        {            if (!(exp.accept(getExpVisitor()) instanceof IntegerType))            {                error.complain("The argument of System.out.println must be" + " of type int");                            }        }    }    // Identifier i;    // Exp e;    public void visit(Assign n)    {        Type t1 = symbolTable.getVarType(currMethod, currClass, n.i.toString());        Type t2 = n.e.accept(getExpVisitor());        if (symbolTable.compareTypes(t1, t2) == false)        {            error.complain("Type error in assignment to " + n.i.toString());                    }    }    // Identifier i;    // Exp e1,e2;    public void visit(ArrayAssign n)    {        Type typeI = symbolTable.getVarType(currMethod, currClass, n.i.toString());        if (!(typeI instanceof IntArrayType))        {            error.complain("The identifier in an array assignment" + "must be of type int []");                    }        if (!(n.e1.accept(getExpVisitor()) instanceof IntegerType))        {            error.complain("The first expression in an array assignment must be of type int");                    }        if (!(n.e1.accept(getExpVisitor()) instanceof IntegerType))        {            error.complain("The second expression in an array assignment must be of type int");                    }    }        public ErrorMsg getErrorMsg()    {        return error;    }}