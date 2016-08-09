package parser;

public class EvaluateContextMutableState extends EvaluateContextState
{
	public EvaluateContextMutableState(State state)
	{
		super(state);
	}

	public EvaluateContextMutableState(Values constantValues, State state)
	{
		super(constantValues, state);
	}

	public EvaluateContextMutableState setVariables(Object[] assignment)
	{
		varValues = assignment;
		return this;
	}
}
