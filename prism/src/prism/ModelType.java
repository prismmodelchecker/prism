package prism;

public enum ModelType {

	// List of model types (ordered alphabetically)
	CTMC, CTMDP, DTMC, MDP, PTA, STPG;

	/**
	 * Get the full name, in words, of the this model type.
	 */
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
	
	/**
	 * Get the PRISM keyword for this model type.
	 */
	public String keyword()
	{
		switch (this) {
		case CTMC:
			return "ctmc";
		case CTMDP:
			return "ctmdp";
		case DTMC:
			return "dtmc";
		case MDP:
			return "mdp";
		case PTA:
			return "pta";
		case STPG:
			return "stpg";
		}
		// Should never happen
		return "";
	}
	
	/**
	 * Do the transitions in a choice sum to 1 for this model type?
	 * Can also use this to test whether models uses rates or probabilities.
	 */
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
	
	/**
	 * Are time delay continuous for this model type?
	 */
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
	
	/**
	 * Does this model allow nondeterministic choices?
	 */
	public boolean nondeterministic()
	{
		switch (this) {
		case DTMC:
		case CTMC:
			return false;
		case MDP:
		case STPG:
		case PTA:
		case CTMDP:
			return true;
		}
		// Should never happen
		return true;
	}
}
