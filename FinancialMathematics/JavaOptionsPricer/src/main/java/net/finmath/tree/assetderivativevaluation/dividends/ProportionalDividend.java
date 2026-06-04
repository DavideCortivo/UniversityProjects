package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * This class defines the proportional dividend model, which is a particular case of the discrete proportional dividend model.
 * The single dividend is therefore paid once a year, hence we have decided to extend the class DiscreteProportionalDividend and 
 * bound the array of the pay dates to contain just one element.
 */

public class ProportionalDividend extends DiscreteProportionalDividend {
	/**
	 * Constructor of the ProportionalDividend class
	 * @param time: the array containing the dividend pay dates (in this case a unique one) 
	 * @param delta: the percentage drop in the stock price once the single dividend is paid
	 */

	public ProportionalDividend(double time, double delta) {
		super(delta, new double[] {time});


	}

}
