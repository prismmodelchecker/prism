package explicit.rewards;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit-state storage of state and transition rewards (mutable).
 */
public class StateTransitionRewardsSimple extends StateRewardsSimple
{
	private List<List<Double>> transRewards;
	
	public StateTransitionRewardsSimple(int numStates)
	{
		super(numStates);
		this.transRewards = new ArrayList<List<Double>>();
		for(int i = 0; i < numStates; i++)
		{
			this.transRewards.add(new ArrayList<Double>());
		}
	}
	
	/**
	 * Increase the number of states by {@code numStates}
	 * 
	 * @param numStates Number of newly added states
	 */
	public void addStates(int numStates)
	{
		for(int i = 0; i < numStates; i++)
		{
			this.transRewards.add(new ArrayList<Double>());
		}		
	}
	
	/**
	 * Set the reward of choice {@code c} of state {@code s} to {@code r}.
	 * 
	 * The number of states added so far must be at least {@code s+1}.
	 * 
	 * @param s State
	 * @param c Choice (Transition)
	 * @param r Reward
	 */
	public void setTransitionReward(int s, int c, double r)
	{
		int n = s - transRewards.get(s).size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				transRewards.get(s).add(0.0);
			}
		}
		transRewards.get(s).set(c, r);
	}
	
	@Override
	public double getTransitionReward(int s, int i)
	{
		return transRewards.get(s).get(i);
	}
	
	public String toString()
	{
		return "rews: " + stateRewards + "; rewt: " + transRewards;
	}
}
