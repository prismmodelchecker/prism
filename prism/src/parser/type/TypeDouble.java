package parser.type;

public class TypeDouble extends Type 
{
	private static TypeDouble singleton;
	
	static
	{
		singleton = new TypeDouble();
	}
	
	private TypeDouble()
	{		
	}	
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeDouble);
	}
	
	public String getTypeString()
	{
		return "double";
	}
	
	public static TypeDouble getInstance()
	{
		return singleton;
	}
	
	public boolean canAssign(Type type)
	{
		return (type instanceof TypeDouble || type instanceof TypeInt);
	}
}
