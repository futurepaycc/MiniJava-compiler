package ir;

import ir.visitor.IrVisitor;

public class ArrayAccess implements Value
{
	
	private DataType type;
	private Value index;
	
	
	public ArrayAccess(DataType type, Value index)
	{
		this.type = type;
		this.index = index;
	}

	@Override
	public void accept(IrVisitor visitor)
	{
		visitor.visit(this);		
	}

	public DataType getType()
	{
		return type;
	}

	public Value getIndex()
	{
		return index;
	}
}
