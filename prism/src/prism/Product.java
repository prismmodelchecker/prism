package prism;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.PrismException;

/**
 * Base class for the results of a product operation between a symbolic model and
 * an automaton. Provides infrastructure for converting information on the
 * states between the original model, the automaton and the product model.
 *
 * @param <M> The type of the product model, e.g, DTMC, MDP, ...
 */
public class Product<M extends Model> implements ModelTransformation<M, M>
{
	protected M originalModel = null;
	protected M productModel = null;
	protected JDDNode productStatesOfInterest = null;
	protected JDDVars automatonRowVars = null;

	/**
	 * Constructor.
	 * <br>
	 * Takes ownership of productModel, productStatesOfInterest and automatonRowVars,
	 * clears those when clear() is called.
	 * <br>[ STORES: productModel, productStatesOfInterest, automatonRowVars ]
 	 * @param productModel the product model
 	 * @param originalModel the original model
 	 * @param productStatesOfInterest the statesOfInterest in the product (starting points of the product construction)
 	 * @param automatonRowVars the DD row vars of the automaton
	 */
	public Product(M productModel, M originalModel, JDDNode productStatesOfInterest, JDDVars automatonRowVars) {
		this.originalModel = originalModel;
		this.productModel = productModel;
		this.productStatesOfInterest = productStatesOfInterest;
		this.automatonRowVars = automatonRowVars;
	}

	/**
	 * Get the product model (not a copy).
	 */
	public M getProductModel()
	{
		return productModel;
	}

	@Override
	public M getTransformedModel()
	{
		return getProductModel();
	}

	@Override
	public M getOriginalModel()
	{
		return originalModel;
	}

	/**
	 * Provides access to the row variables of the automaton part of the product.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public JDDVars getAutomatonRowVars() {
		return automatonRowVars;
	}

	/** Clear the product model and the other JDD references */
	public void clear() {
		if (productModel != null) productModel.clear();
		if (productStatesOfInterest != null) JDD.Deref(productStatesOfInterest);
		if (automatonRowVars != null) automatonRowVars.derefAll();
	}

	/**
	 * Project state values from the product model back to the original model. Clears svTransformed.
	 * <br>
	 * Note: This assumes that the product construction results in each state of interest
	 * in the original model having a unique corresponding state in the product,
	 * provided by productStatesOfInterest.
	 * @param svTransformed the state values in the product model
	 * @return the corresponding state values in the original model
	 */
	@Override
	public StateValues projectToOriginalModel(StateValues svTransformed) throws PrismException {
		// Filter against the productStatesOfInterest, i.e.,
		// set values to 0 for all states that do not correspond to the states of interest
		svTransformed.filter(productStatesOfInterest);
		// Then sum over the DD vars introduced for the automata modes to
		// get StateValues in the original model
		StateValues svOriginal = svTransformed.sumOverDDVars(automatonRowVars, originalModel);

		svTransformed.clear();
		return svOriginal;
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return productStatesOfInterest.copy();
	}
}
