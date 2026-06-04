package net.finmath.tree.assetderivativevaluation.dividends;
/**
 * This class defines a continuous dividend model whose dividends are paid infinite many times per year.
 * In order to compute dividends, this class implements DividendEvent and MultiplicativeDividendModel, which 
 * introduce the previously defined fundamental methods for the computation of dividends.  
 */
public class ContinuousDividendYield implements MultiplicativeDividendModel {
/*
 * We have defined the following fields:
 * dividendYield: the percentage value of the dividend paid by the stock 
 */
	double dividendYield;

	/**
	 * Constructor of the continuous dividend model
	 * @param dividendYield: dividend yield (q)
	 */
	public ContinuousDividendYield (double dividendYield) {
		this.dividendYield=dividendYield;
		/*
		 * Since the dividend is a percentage of the stock price, it is necessary to bound it to have a value between
		 * 0 and 1. If a not acceptable value is inserted as input of the constructor, the system throws an alert message.
		 */
		if (dividendYield < 0 || dividendYield > 1) {
			throw new IllegalArgumentException("ERROR. The dividend Yield inserted is invalid. You have to put a value between 0 and 1");
		}

	}

	/**
	 * Computes the continuous cumulative dividend factor considering the continuous dividend at a specific time starting from 0; 
	 * Formula: D = e^(-dividendYield*time)
	 * @return the cumulative dividend factor
	 * 
	 */
	@Override
	public double getCumulativeDividendFactor(double time) {
		return Math.exp(-(dividendYield)*time);
	}
	/**
	 * Computes the continuous forward dividend factor between two specific points of time. 
	 * Formula: D = e^(-dividendYield*(time2-time1))
	 * @return the forward dividend yield 
	 */
	@Override
	public double getForwardDividendFactor(double time1, double time2) {
		return  Math.exp(-(dividendYield)*(time2 - time1));
	}

	/**
	 * Since this dividend model is continuous, this methods returns an error because the array of dividend pay dates contains infinite many 
	 * elements and it would not make sense to consider the single dividend pay dates.
	 */
	@Override
	public double[] getTime() {
		throw new UnsupportedOperationException("You can't ask for a dividend dates array in countinuous time since it would be infinite");
	}
	/**
	 * This method returns the dividend yield (q).
	 * @return the dividend yield
	 */
	public double getDividendYield() {
		return dividendYield;
	}




}
