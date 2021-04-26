package explicit.rewards;

import java.util.ArrayList;
import java.util.List;

import explicit.Model;
import explicit.Product;

public class WeightedSumMDPRewards implements MDPRewards
{
	int numRewards;
	List<Double> weightsList;
	List<MDPRewards> rewardsList;
	
	public WeightedSumMDPRewards()
	{
		numRewards = 0;
		weightsList = new ArrayList<>();
		rewardsList = new ArrayList<>();
	}
	
	public void addRewards(double weight, MDPRewards rewards)
	{
		numRewards++;
		weightsList.add(weight);
		rewardsList.add(rewards);
	}
	
	@Override
	public double getStateReward(int s)
	{
		double rew = 0.0;
		for (int r = 0; r < numRewards; r++) {
			rew += weightsList.get(r) * rewardsList.get(r).getStateReward(s);
		}
		return rew;
	}

	@Override
	public double getTransitionReward(int s, int i)
	{
		double rew = 0.0;
		for (int r = 0; r < numRewards; r++) {
			rew += weightsList.get(r) * rewardsList.get(r).getTransitionReward(s, i);
		}
		return rew;
	}

	@Override
	public MDPRewards liftFromModel(Product<? extends Model> product)
	{
		WeightedSumMDPRewards rew = new WeightedSumMDPRewards();
		for (int r = 0; r < numRewards; r++) {
			rew.addRewards(weightsList.get(r), rewardsList.get(r).liftFromModel(product));
		}
		return rew;
	}

	@Override
	public boolean hasTransitionRewards()
	{
		for (int r = 0; r < numRewards; r++) {
			if (rewardsList.get(r).hasTransitionRewards()) {
				return true;
			}
		}
		return false;
	}
}
