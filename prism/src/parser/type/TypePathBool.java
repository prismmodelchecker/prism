package parser.type;

public class TypePathBool extends Type 
{
	private static TypePathBool singleton;
	
	static
	{
		singleton = new TypePathBool();
	}
	
	private TypePathBool()
	{		
	}
	
	public boolean equals(Object o)
	{
		return (o instanceof TypePathBool);
	}
	
	public String getTypeString()
	{
		return "path-bool";
	}
	
	public static TypePathBool getInstance()
	{
		return singleton;
	}
}
