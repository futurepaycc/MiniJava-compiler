package ir.backend;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import symboltable.Table;
import syntaxtree.MethodDecl;
import ir.Temporary;
import ir.cfgraph.BasicBlock;
import ir.cfgraph.Block;
import ir.cfgraph.CodePoint;
import ir.cfgraph.Conditional;
import ir.cfgraph.Frame;
import ir.ops.ArrayAccess;
import ir.ops.ArrayAssignment;
import ir.ops.Assignment;
import ir.ops.BinOp;
import ir.ops.BinOp.Op;
import ir.ops.Call;
import ir.ops.Identifier;
import ir.ops.IntegerLiteral;
import ir.ops.NewArray;
import ir.ops.RecordAccess;
import ir.ops.RecordAllocation;
import ir.ops.RecordAssignment;
import ir.ops.RecordDeclaration;
import ir.ops.RelationalOp;
import ir.ops.Return;
import ir.ops.SysCall;
import ir.ops.Expression;
import ir.visitor.IrVisitor;

public class X86CodeGenerator implements IrVisitor
{
	private PrintStream out;	
	private HashMap<String, RecordDeclaration> recordMap = new HashMap<String, RecordDeclaration>();
	
	public X86CodeGenerator(PrintStream out, Collection<RecordDeclaration> recordTypes)
	{		
		this.out = out;		
		for(RecordDeclaration recordDecl : recordTypes)
			recordMap.put(recordDecl.getNamespace() + recordDecl.getId(), recordDecl);
	}

	private void emit(String text)
	{
		out.println("\t" +text);
	}
	
	private void emit(String text, String comment)
	{
		out.println("\t" +text + "\t" + "# " + comment);
	}
	

	private void emitLabel(String text)
	{
		out.println(text + ":");
	}

	private void emitComment(String text)
	{
		out.println("\t" + "#" + text);
	}

	private void initFile(String startFrameId)
	{
		emitComment("General constants used for output");
		emitLabel("print_num");
		emit(".ascii \"%d \\0\"");
		emitLabel("newline");
		emit(".ascii \"\\n\\0\"");

		emit(".globl _main");
		emitLabel("_main");
		emitComment("Prologue to _main");
		emit("pushl %ebp");
		emit("movl %esp, %ebp");
		emit("call ___main   #Call c library main");
		emit("call " + startFrameId + "  #call the starting frame");
		emit("leave");
		emit("ret");

	}

	private boolean startFrame;
	private Frame currentFrame;

	@Override
	public void visit(Frame f)
	{
		if (!startFrame)
		{
			startFrame = true;
			initFile(f.getId());
		}

		out.println();
		emitLabel(f.getId());
		emit("pushl %ebp");
		emit("movl %esp, %ebp");
		currentFrame = f;
		int localSize = f.getLocals().size() + f.getTemporaries().size();
		emit("subl $" + (localSize * 4) + " , %esp   #Reserve spsace for locals and temporaries.");
		
		f.getStartingBlock().accept(this);		


		emit("leave");
		emit("ret");
		out.println();
		


	}

	@Override
	public void visit(BasicBlock b)
	{
		for (CodePoint codePoint : b.getCodePoints())
			codePoint.accept(this);
		
		if(b.getSuccessor() != null)
			b.getSuccessor().accept(this);
	}

	@Override
	public void visit(CodePoint c)
	{
		c.getOperation().accept(this);
	}

	private String getOpOpcode(BinOp.Op op)
	{
		switch (op)
		{
		case ADD:
			return "addl";
		case MULT:
			return "imull";
		case SUBTRACT:
			return "subl";
		default:
			return null;
		}
	}

	public void visit(BinOp b)
	{
		b.getSrc1().accept(this);	
		b.getSrc2().accept(this);		
		emit("popl %ebx");
		emit("popl %eax");
		BinOp.Op op = b.getOp();
		if(op != null)
		{
			emit(getOpOpcode(b.getOp()) + " %ebx, %eax");
			emit("pushl %eax");
		}
	}

	@Override
	public void visit(Call call)
	{
		for(int i = call.getParameters().size() - 1; i >= 0; i--)					
			call.getParameters().get(i).accept(this);			
					
		
		emit("call " + call.getId());
		int paramSize = call.getParameters().size() * 4;
		if (paramSize > 0)
			emit("addl $" + paramSize + ", %esp   # Clean up parameters from call");		
		emit("push %eax  #Store results of call onto stack");
	}

	@Override
	public void visit(SysCall call)
	{
		if (call.getId().equals("print") || call.getId().equals("println"))
		{
			for (Expression param : call.getParameters())
			{
				param.accept(this);				
				emit("pushl $print_num");
				emit("call _printf");
				emit("addl $8, %esp   # Pop _printf params off of stack");
			}
		}
		
		if(call.getId().equals("println"))
		{
	    	emitComment("Print new line");
	    	emit("pushl $newline");
	    	emit("call _printf");
	    	emit("Addl $4, %esp");
	    	emitComment("End println");
		}

	}

	private int getIdByIndex(List<Identifier> ids, String id)
	{
		int result = -1;
		for (int i = 0; i < ids.size(); i++)
			if (ids.get(i).getId().equals(id))
				result = i;
		return result;
	}
	
	private int getIdentifierStackOffset(String id)
	{
		int paramIndex;
		int localIndex;
		int tempIndex;
		if ((paramIndex = getIdByIndex(currentFrame.getParams(), id))>= 0)
		{
			return 4 * paramIndex + 8;
		}		
		else if ((localIndex = getIdByIndex(currentFrame.getLocals(), id)) >= 0)
		{
			return  -(4 * localIndex + 4);
		}			 
		else if((tempIndex = getIdByIndex(currentFrame.getTemporaries(), id)) >= 0)
		{
			return -(tempIndex * 4 + currentFrame.getLocals().size() * 4 + 4);
		}
		else
			return -1;
		
	}

	private boolean rValue = true;
	
	@Override
	public void visit(Identifier i)
	{
		int currentStackOffset = getIdentifierStackOffset(i.getId());
		if(rValue)
			emit("pushl " + (currentStackOffset != 0 ? currentStackOffset : "") +"(%ebp)");
		else
		{
			emit("lea " + (currentStackOffset != 0 ? currentStackOffset : "") +"(%ebp), %eax");
			emit("pushl %eax");
			rValue = true;
		}
	}

	@Override
	public void visit(IntegerLiteral l)
	{
		emit("pushl $" + l.getValue());
	}

	@Override
	public void visit(Temporary t)
	{
				
		
	}	
	
	@Override
	public void visit(ArrayAccess a)
	{		
		emitComment("Access an array element");
		a.getReference().accept(this);
		emit("popl %esi   #Load array reference");
		a.getIndex().accept(this);
		emit("popl %eax   #Load array index");
		emit("incl %eax   #Skip the size word at the beginning");
		emit("push (%esi, %eax, $4)    # Store contents of value at offset onto stack");
	}
	
	

	@Override
	public void visit(NewArray n)
	{
		emitComment("Allocate new array");
		n.getSize().accept(this);
		emit("pop %eax");
		emit("mull $4, %eax");
		emit("push %eax");
		emit("call _malloc");
		emit("push %eax");		
	}	
	

	@Override
	public void visit(ArrayAssignment a)
	{
		emitComment("Assign to an array element");
		a.getSrc().accept(this);
		a.getDest().accept(this);
		a.getDestIndex().accept(this);
		emit("popl %eax   #Load array index");
		emit("popl %esi   #Load array reference");		
		emit("incl %eax   #Skip the size word at the beginning");
		emit("pushl (%esi, %eax, $4)    # Store contents of value at offset onto stack");		
	}
	
	@Override
	public void visit(RecordDeclaration r)
	{
		recordMap.put(r.getNamespace() + r.getId(), r);
	}

	@Override
	public void visit(RecordAccess r)
	{	
		//Assume 4 bytes for now
		int offset = r.getIndex() * 4;
		
		r.accept(this);
		emit("pop %esi");
		emit("push " + offset + "(%esi)");		
	}

	@Override
	public void visit(RecordAssignment r)
	{		
		
	}

	@Override
	public void visit(Assignment assignment)
	{
		rValue = true;
		assignment.getSrc().accept(this);
		rValue = false;
		assignment.getDest().accept(this);
		emit("pop %eax");
		emit("pop %ebx");
		emit("movl %ebx, (%eax)");
	}
	
	private int conditionCount;	
	@Override
	public void visit(Conditional b)
	{
		int conditionCount = this.conditionCount++;
		b.getTest().accept(this);
		emit("popl %eax");
		emit("movl $1, %ebx");
		emit("je cond_true_" + conditionCount);
		
		emitLabel("cond_false_" + conditionCount);
		b.getFalseBlock().accept(this);
		emit("jmp cond_end_" + conditionCount, "Jump to end of condition");
		emitLabel("cond_true_" + conditionCount);
		b.getTrueBlock().accept(this);
		
		emitLabel("cond_end_" + conditionCount);	
		
		if(b.getSuccessor() != null)
			b.getSuccessor().accept(this);
		
	}

	@Override
	public void visit(RecordAllocation a)
	{
		RecordDeclaration decl = recordMap.get(a.getNamespace() + a.getTypeId());
		if(decl.getFieldCount() > 0)
		{
			emit("push $" + (decl.getFieldCount() * 4) + "     # Push object size onto stack");
			emit("call _malloc");		
			emit("$addl $4, %esp");
			emit("$push %eax");
		}
		else
			emit("push $0      # Push placeholder address onto stack");
	}

	@Override
	public void visit(Return r)
	{
		rValue = true;
		r.getSource().accept(this);		
		emit("popl %eax", "Pop return value");		
	}
	
	private String getJumpInstruction(RelationalOp.Op op)
	{
		switch(op)
		{
		case LTE:
			return "jle";
		case LT:
			return "jl";
		case EQ:
			return "je";			
		default:
			throw new RuntimeException("Unrecognized relational operation.");
		}
	}
	

	int relationalCount;
	@Override
	public void visit(RelationalOp r)
	{
		int relationalCount = this.relationalCount++; 
		r.getSrc1().accept(this);		
		r.getSrc2().accept(this);
		emit("Popl %ebx", "Pop second operand");
		emit("popl %eax", "Pop first operand");
		emit("cmp %ebx, %eax", "Compare operands");		
		emit(getJumpInstruction(r.getOp()) + " relational_true_" + relationalCount);
		emitLabel("relational_false_" + relationalCount);
		emit("pushl $0");
		emit("jmp relational_end_"+ relationalCount);
		emitLabel("relational_true_" + relationalCount);
		emit("pushl $1");
		emitLabel("relational_end_"+ relationalCount);		
	}


}
