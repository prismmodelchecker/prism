package parser.type;

public class TypeBool extends Type 
{
	private static TypeBool singleton;
	
	static
	{
		singleton = new TypeBool();
	}	
	
	private TypeBool()
	{		
	}
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeBool);
	}
	
	public String getTypeString()
	{
		return "bool";
	}
	
	public static TypeBool getInstance()
	{
		return singleton;
	}
	
	public boolean canAssign(Type type)
	{
		return (type instanceof TypeBool);
	}
}
