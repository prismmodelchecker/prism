package parser.type;

public class TypePathInt extends Type 
{
	private static TypePathInt singleton;
	
	static
	{
		singleton = new TypePathInt();
	}

	private TypePathInt()
	{		
	}
	
	public boolean equals(Object o)
	{
		return (o instanceof TypePathInt);
	}
	
	public String getTypeString()
	{
		return "path-int";
	}
	
	public static TypePathInt getInstance()
	{
		return singleton;
	}
}
