package net.finmath.tree.assetderivativevaluation.models;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.assetderivativevaluation.AbstractRecombiningTreeModel;
import net.finmath.tree.assetderivativevaluation.dividends.ContinuousDividendYield;
import net.finmath.tree.assetderivativevaluation.dividends.MultiplicativeDividendModel;

/**
 * This class represents the approximation of a Black-Scholes model via the Jarrow-Rudd model.
 * It extends ApproximatingBinomialModel. The only method that is implemented here computes the values
 * of the up and down movements of the Binomial model.
 *
 * @author Andrea Mazzon
 *
 */

public class JarrowRuddModel extends AbstractRecombiningTreeModel {

	/**
	 * Constructs a Jarrow-Rudd model using an equidistant time step.
	 *
	 * @param spotPrice the initial price of the asset modeled by the process
	 * @param riskFreeRate the number r such that the value of a risk-free bond at time T is e^(rT)
	 * @param volatility the log-volatility of the Black-Scholes model
	 * @param lastTime the last time T in the time discretization 0=t_0<...<t_n=T
	 * @param timeStep the length t_k - t_{k-1} of the time step
	 */
	public JarrowRuddModel(double spotPrice, double riskFreeRate, double volatility, double lastTime, double timeStep) {
		super(spotPrice, riskFreeRate, volatility, lastTime, timeStep);
	}

	/**
	 * Constructs a Jarrow-Rudd model using a given number of time steps.
	 *
	 * @param spotPrice the initial price of the asset modeled by the process
	 * @param riskFreeRate the number r such that the value of a risk-free bond at time T is e^(rT)
	 * @param volatility the log-volatility of the Black-Scholes model
	 * @param lastTime the last time T in the time discretization 0=t_0<...<t_n=T
	 * @param numberOfTimes the number of equally spaced time steps
	 */
	public JarrowRuddModel(double spotPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes) {
		super(spotPrice, riskFreeRate, volatility, lastTime, numberOfTimes);
	}
	
	//----------------------------------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * We have created a constructor overload to add dividends to tree models. In order to this, we have used the Dependency Injection 
	 * of the class MultiplicativeDividendModel. We have imported this class and inserted it as input of the new constructor, in order to
	 * build new objects with a specific dividend type.
	 * Since this class extends AbstractRecombiningTreeModel, it is not necessary to create the variable MultiplicativeDividendModel 
	 * dividendModels.
	 * @param spotPrice         The initial price of the asset modeled by the process
	 * @param riskFreeRate   	The number r such that the value of a risk-free bond at time T is e^(rT)
	 * @param volatility	 	The log-volatility of the Black-Scholes model
	 * @param lastTime  		The last time T in the time discretization 0=t_0<t_1<..<t_n=T
	 * @param numberOfTimes     The number of equally spaced time steps
	 * @param dividendModel 	The chosen type of dividend model 
	 */
	public JarrowRuddModel (double spotPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes, MultiplicativeDividendModel dividendModel) {
		super(spotPrice, riskFreeRate, volatility, lastTime, numberOfTimes, dividendModel );
	}
	

	/** Abstract hook to implement in the underlying classes
	 * Binomial: at k level there are k+1 states
	 * @param  k level of depth
	 * */
	@Override
	public int statesAt(int k) {
		return k + 1;
	}
	/** Build S_k[i] with agreement i = #down => S0 * u^(k-i) * d^i.
	 * @param  k level of depth */
	@Override
	protected RandomVariable buildSpotLevel(int k) {
		double s0    = getInitialPrice();
		double dt    = getTimeStep();
		double sigma = getVolatility();
		double r     = getRiskFreeRate();
		double muStar;
		
		/*
		 * If the dividend model is continuous, the probabilities of the tree needs to be changed.
		 * We have imported the concrete class of the continuous dividend and created a new object of the class 
		 * ContinuousDividendYield which is necessary to call the method getDividendYield(), which gives q. 
		 * If the dividend model is discrete, it is not necessary to change the probabilities.
		 */
		if(dividendModel instanceof ContinuousDividendYield) {
			ContinuousDividendYield contDiv = (ContinuousDividendYield) dividendModel;
		    muStar = (r -contDiv.getDividendYield() - 0.5*sigma*sigma)* dt;
		} else {
		 muStar = (r - 0.5*sigma*sigma) * dt;
		}
		
		double nu     = sigma * Math.sqrt(dt);

		double u = Math.exp(muStar + nu);
		double d = Math.exp(muStar - nu);

		double[] level = new double[k + 1];
		/* The dividend factor is the factor that modifies the price in the specific time node */
		double dividendFactor = 1;
		/*
		 * If the model has dividends and they are not continuous, the dividend factor is computed via the specific date of the time 
		 * grid (currentTime) and the method getCumulativeDividendFactor, which derives from MultiplicativeDividendModel; otherwise, 
		 * the dividend factor remains equal to 1.
		 */
		if (dividendModel != null && !(dividendModel instanceof ContinuousDividendYield)) {
			double currentTime = getTime(k);
			dividendFactor  = dividendModel.getCumulativeDividendFactor(currentTime);
		}
		for (int i = 0; i <= k; i++) {
			int ups   = k - i;
			int downs = i;
			level[i] = s0 * Math.pow(u, ups) * Math.pow(d, downs)*dividendFactor; /** we added dividendFactor here */
		}
		return new RandomVariableFromDoubleArray(k*dt, level);
	}

	/**
	 * Discounted conditional expectation : V_k[i] = df() * ( q * V_{k+1}[i] + (1-q) * V_{k+1}[i+1] ).
	 */
	@Override
	protected RandomVariable conditionalExpectation(RandomVariable vNext, int k) {
		final double r = getRiskFreeRate();
		final double dt = getTimeStep();

		final double disc = Math.exp(-r * dt);
		final double p = 0.5;

		final double[] next = vNext.getRealizations();
		final int expected = k + 2;
		if (next == null || next.length != expected) {
			throw new IllegalArgumentException("vNext length " + (next == null ? "null" : next.length)
					+ " != expected " + expected + " for time index k=" + k);
		}

		final int nHere = statesAt(k);
		final double[] res = new double[nHere];
		for (int i = 0; i < nHere; i++) {
			res[i] = disc * (p * next[i] + (1.0 - p) * next[i + 1]);
		}

		return new RandomVariableFromDoubleArray(k * dt, res);
	}

	@Override
	public int getNumberOfBranches(int timeIndex, int stateIndex) {
		return 2;
	}

	@Override
	public double getTransitionProbability(int timeIndex, int stateIndex, int branchIndex) {
		// Convention: 0 = up, 1 = down (Jarrow-Rudd uses p=0.5)
		switch(branchIndex) {
			case 0: return 0.5;
			case 1: return 0.5;
			default: throw new IllegalArgumentException("Invalid branchIndex " + branchIndex + " for binomial model.");
		}
	}

	@Override
	public int[] getChildStateIndexShift() {
		// Convention: childIndex = parentIndex + shift[branchIndex]
		// For binomial recombining trees: up keeps index, down increases index by 1.
		return new int[] { 0, 1 };
	}

}
