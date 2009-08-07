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
		return "";
	}
}
