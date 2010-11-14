package parser.type;

import java.util.*;

public class TypeInterval extends Type 
{
	private static Map<Type, TypeInterval> singletons;
	
	static
	{
		singletons = new HashMap<Type, TypeInterval>();
	}
	
	private Type subType;
	
	public TypeInterval(Type subType)
	{
		this.subType = subType;
	}

	public Type getSubType() 
	{
		return subType;
	}

	public void setSubType(Type subType) 
	{
		this.subType = subType;
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof TypeInterval)
		{
			TypeInterval oi = (TypeInterval)o;
			return (subType.equals(oi.getSubType()));
		}
		
		return false;
	}
	
	public String getTypeString()
	{
		return "interval of " + subType.getTypeString();
	}
	
	public static TypeInterval getInstance(Type subType)
	{
		if (!singletons.containsKey(subType))
			singletons.put(subType, new TypeInterval(subType));
			
		return singletons.get(subType);
	}
}
