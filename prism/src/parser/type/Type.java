package parser.type;

public abstract class Type 
{
	public abstract String getTypeString();
	
	public boolean canAssign(Type type)
	{
		return false;
	}
	
	public String toString()
	{
		return getTypeString();
	}
}
