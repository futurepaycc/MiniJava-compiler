package ir.ops;

import ir.visitor.IrVisitor;

import java.util.List;

public class Call implements Expression
{	
	String namespace;
	String id;
	List<Expression> parameters;	
	
	public Call(String namespace, String id, List<Expression> parameters)
	{
		this.namespace = namespace;
		this.id = id;
		this.parameters = parameters;		
	}
	
	public String getNamespace()
	{
		return namespace;
	}
	
	public String getId()
	{
		return id;
	}
	
	public List<Expression> getParameters()
	{
		return parameters;
	}
	
	@Override
	public void accept(IrVisitor visitor)
	{
		visitor.visit(this);		
	}
	
	@Override 
	public String toString()
	{
		String paramList = "";
		boolean first = true;
		for(Expression param : parameters)
		{
			if(!first)
			{
				paramList += ", ";
			}
			else
				first = false;
			paramList += param.toString();
			
		}
		return namespace + "." + id + "(" + paramList + ")";
	}

}
