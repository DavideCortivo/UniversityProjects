package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * This interface extends DividendModel and DividendEvent in order to force every concrete class 
 * in which it is implemented (namely, the classes of the dividend types) to define two new fundamental methods
 * for the computation of dividends and the already defined method getTime().
 * This interface will be imported in AbstractRecombingTreeModel to introduce in its constructor the specific 
 * dividend model type defined in the concrete classes ProportionalDividend, DiscreteProportionalDividend and 
 * ContinuousDividendYield.
 */
public interface MultiplicativeDividendModel extends DividendModel, DividendEvent {

	/**
	 * This method allows us to compute the dividend factor from the initial time up to the given time 
	 * (provided as input in brackets).
	 * @return the cumulative dividend factor
	 */
	double getCumulativeDividendFactor(double time);

	/**
	 * This method allows us to compute the dividend factor between two given points in time (the inputs of the method).
	 * @return the forward dividend factor 
	 */
	double getForwardDividendFactor(double time1, double time2);

}
