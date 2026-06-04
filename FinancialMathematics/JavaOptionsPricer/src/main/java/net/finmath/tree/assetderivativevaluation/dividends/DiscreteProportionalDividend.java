package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * This class defines a proportional dividend model whose dividends are paid discretely multiple times a year.
 * In order to compute dividends, this class implements MultiplicativeDividendModel, which 
 * introduce the previously defined fundamental methods for the computation of dividends. 
 */

public class DiscreteProportionalDividend implements MultiplicativeDividendModel {

	/* 
	 * We have defined the following fields:
	 * delta: the percentage drop in the stock price once the single dividend is paid
	 * dividendEvents: the array containing the dividend pay dates 
	 */

	double delta;
	private final double [] dividendEvents;

	/**
	 * This is the constructor for the discrete proportional model. 
	 * @param delta: the percentage drop in the stock price once the single dividend is paid
	 * @param dividendEvents: the array containing the dividend pay dates 
	 */
	public DiscreteProportionalDividend (double delta, double[] dividendEvents) {
		this.delta=delta;
		this.dividendEvents=dividendEvents;
		/**
		 * If the percentage drop was negative, the stock price would increase every time a single dividend is paid, and if it was 
		 * higher than 1, the stock price would become negative. Hence, it is necessary to bound the delta to have a value between
		 * 0 and 1. If a not acceptable value is inserted as input of the constructor, the system throws an alert message.
		 */
		if(delta<0 || delta>1) {
			throw new IllegalArgumentException("ERROR. Delta inserted is invalid. You have to put a value between 0 and 1");
		}
	}

	/**
	 * This method computes the cumulative dividend factor considering multiple dividend events from 0 up to a specific time. 
	 * This is one of the two methods which derives from the implemented class MultiplicativeDividendModel.
	 * @param time: the moment in which the stock is evaluated
	 * @return cumulativeFactor: the cumulative dividend factor up to given time.  
	 */
	@Override
	public double getCumulativeDividendFactor(double time) {
		/*
		 * Since pay dates may not coincide with the date in which we want to compute the stock price, we have decided to associate 
		 * them directly to the following node in the time grid.
		 * If the dividend is not paid, the cumulative factor remains 1; otherwise, if the pay date precedes the node in the time 
		 * grid, the for loop computes the cumulative factor up to the first point in time grid after the pay date. 
		 */
		double cumulativeFactor=1.0;
		if (dividendEvents == null) return cumulativeFactor;
		for (int i=0; i<dividendEvents.length; i++) {
			if (dividendEvents[i] <= time) {
				cumulativeFactor = cumulativeFactor * (1.0 - delta);
			}
		}
		return cumulativeFactor;
	}

	/**
	 * This methods computes the forward dividend factor between two specific times.
	 * This is the other method which derives from the implementation of the class MultiplicativeDividendModel.
	 * @param time1: the first considered point in time 
	 * @param time2: the second considered point in time 
	 * @return the forward dividend factor between time2 and time1, which corresponds to the ratio of the corresponding 
	 * cumulative dividend factors 
	 */
	@Override
	public double getForwardDividendFactor(double time1, double time2) {
		return getCumulativeDividendFactor(time2)/getCumulativeDividendFactor(time1);
	}

	/**
	 * This methods derives from the implementation of the interface DividendEvent.
	 * @return the array containing the dividend pay dates. 
	 */
	@Override
	public double [] getTime() {
		return dividendEvents;
	}






}
