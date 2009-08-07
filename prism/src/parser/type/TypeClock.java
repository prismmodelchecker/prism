package parser.type;

public class TypeClock extends Type 
{
	private static TypeClock singleton;
	
	static
	{
		singleton = new TypeClock();
	}
	
	private TypeClock()
	{		
	}	
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeClock);
	}
	
	public String getTypeString()
	{
		return "clock";
	}
	
	public static TypeClock getInstance()
	{
		return singleton;
	}
	
	public boolean canAssign(Type type)
	{
		return (type instanceof TypeClock);
	}
}
