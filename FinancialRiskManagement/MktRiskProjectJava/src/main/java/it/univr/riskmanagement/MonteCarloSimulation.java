package it.univr.riskmanagement;
import java.time.LocalDate;

import  org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;

/** 
 * This class contains the simulation engine for the computation of Monte Carlo VaR and ES, which will be performed in the class
 * RiskMeasures. It generates random samples from a Normal distribution calibrated to the historical parameters of the current 
 * rolling windowand exposes them for risk-measure computation, hence it handles randomness. 
 * The underlying assumption is that the 1-day log-returns of the portfolio are, by definition, normally distributed. 
 * This is the same normality assumption as in the computation of the parametric Normal risk measures, but here we exploit it 
 * to create a sample of simulated log-returns. 
 *  This approach confirms the results obtained with the parametric Normal method, since the Monte Carlo result converges to 
 *  the parametric one as the number of simulations increases. Moreover, it is much more flexible and allows to obtain much more data
 *  with respect to the historical methodology, since it relies on simulated data.
 *  Each rolling window uses a different seed (equal to the window index), so successive windows are statistically independent. In fact,
 *  the same seed always produces the same 100,000 draws and would not ensure independence.
 */

public class MonteCarloSimulation {

	/*
	 * This is the number of Monte Carlo simulations per window. The value is set to 100,000 as a compromise between estimation 
	 * stability and reasonable computation time. With 100,000 draws and alpha = 1%, the empirical quantile is calculated based on 
	 * the 1000 worst simulated realizations. This is sufficient for comparison with the results of the Normal method. 
	 * This parameter is defined as a public constant so it can be easily increased when a more precise estimate is required.
	 */
	  public static final int numberOfSimulations = 100000;

	  /**
	   * This method represents the heart of the simulation: it generates numberOfSimulations independent draws from a Normal 
	   * distribution with mean mu and standard deviation sigma, both calibrated on the current historical window data. 
	   * This directly implements the requirement to use historical estimators for the distribution parameters.
	   * It uses the JDKRandomGenerator from the package Apache Commons Math, initialized with an explicit seed: by passing the 
	   * window index as the seed, the generators remain independent from one window to the next, yet are deterministic within 
	   * a single execution of the program.
	   * @param mu: the sample mean estimated on the historical window
	   * @param sigma: the sample standard deviation estimated on the historical window
	   * @param seed: the window index [t], used as the seed for the pseudo-random generator
	   * @return an array containing the numberOfSimulations simulated realizations of portfolio returns
	   */
	    public static double[] simulate(double mu, double sigma, long seed) {

	       // Pseudo-random generator initialized with the seed: the same seed always produces the same sequence 
	    	JDKRandomGenerator rg = new JDKRandomGenerator((int) seed);

	    	// Normal distribution calibrated on the historical mu and sigma parameters of the current window 
	    	NormalDistribution normal = new NormalDistribution(rg, mu, sigma);

	    	/* 
	    	 * This function extracts random realizations from the Standard Uniform (the so-called pseudorandom numbers) and plugs them into the
	    	 * inverted distribution function of the Normal distribution with parameters mu and sigma we have just created. We can thus
	    	 * obtains the quantiles that represent our random returns. 
	    	 */
	        return normal.sample(numberOfSimulations);
	    }

	   
	}



