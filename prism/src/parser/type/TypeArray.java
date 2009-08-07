package parser.type;

import java.util.*;

public class TypeArray extends Type 
{
	private static Map<Type, TypeArray> singletons;
	
	static
	{
		singletons = new HashMap<Type, TypeArray>();
	}
	
	private Type subType;
	
	private TypeArray()
	{
		this.subType = null;
	}
	
	public TypeArray(Type subType)
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
		if (o instanceof TypeArray)
		{
			TypeArray oa = (TypeArray)o;
			return (subType.equals(oa.getSubType()));
		}
		
		return false;
	}
	
	public String getTypeString()
	{
		return "array of " + subType.getTypeString();
	}
	
	public static TypeArray getInstance(Type subType)
	{
		if (singletons.containsKey(subType))
			singletons.put(subType, new TypeArray(subType));
			
		return singletons.get(subType);
	}
}
