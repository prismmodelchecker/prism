package prism;

public enum ModelType {

	// List of model types (ordered alphabetically)
	CTMC, CTMDP, DTMC, MDP, PTA, STPG;

	public String fullName()
	{
		switch (this) {
		case CTMC:
			return "continuous-time Markov chain";
		case CTMDP:
			return "continuous-time Markov decision process";
		case DTMC:
			return "discrete-time Markov chain";
		case MDP:
			return "Markov decision process";
		case PTA:
			return "probabilistic timed automaton";
		case STPG:
			return "stochastic two-player game";
		}
		// Should never happen
		return "";
	}
	
	public boolean choicesSumToOne()
	{
		switch (this) {
		case DTMC:
		case MDP:
		case PTA:
		case STPG:
			return true;
		case CTMC:
		case CTMDP:
			return false;
		}
		// Should never happen
		return true;
	}
	
	public boolean continuousTime()
	{
		switch (this) {
		case DTMC:
		case MDP:
		case STPG:
			return false;
		case PTA:
		case CTMC:
		case CTMDP:
			return true;
		}
		// Should never happen
		return true;
	}
}
