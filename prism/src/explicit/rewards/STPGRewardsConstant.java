package explicit.rewards;

/**
 * Explicit storage of constant game rewards.
 */
public class STPGRewardsConstant implements STPGRewards
{
	private double dsReward;
	private double transReward;
	
	public STPGRewardsConstant(double dsReward, double transReward)
	{
		this.dsReward = dsReward;
		this.transReward = transReward;
	}

	@Override
	public double getDistributionSetReward(int s, int d)
	{
		return this.dsReward;
	}

	@Override
	public int getTransitionRewardCount(int s, int ds, int d)
	{
		return 1;
	}

	@Override
	public double getTransitionReward(int s, int d, int t, int i)
	{
		return this.transReward;
	}
	
	@Override
	public void clearRewards(int s)
	{
		//do nothing
		return;
	}

}
