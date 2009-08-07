package parser.type;

public class TypeInt extends Type 
{
	private static TypeInt singleton;
	
	static
	{
		singleton = new TypeInt();
	}
	
	private TypeInt()
	{		
	}
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeInt);
	}
	
	public String getTypeString()
	{
		return "int";
	}
	
	public static TypeInt getInstance()
	{
		return singleton;
	}
	
	public boolean canAssign(Type type)
	{
		return (type instanceof TypeInt);
	}
}
