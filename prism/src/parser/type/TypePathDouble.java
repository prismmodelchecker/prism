package parser.type;

public class TypePathDouble extends Type 
{
	private static TypePathDouble singleton;
	
	static
	{
		singleton = new TypePathDouble();
	}
	
	private TypePathDouble()
	{		
	}	
	
	public boolean equals(Object o)
	{
		return (o instanceof TypePathDouble);
	}
	
	public String getTypeString()
	{
		return "path-double";
	}
	
	public static TypePathDouble getInstance()
	{
		return singleton;
	}
}
