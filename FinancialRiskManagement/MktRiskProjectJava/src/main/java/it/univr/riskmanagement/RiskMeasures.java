package it.univr.riskmanagement;

import java.time.LocalDate;
import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * This class contains the risk measures of interest: Value-at-Risk (VaR) and Expected Shortfall (ES) computed using a rolling window
 * approach, based on three different methodologies.
 * All methods in this class are static because they are functions and we do not need to create specific instances to call them. 
 * Each one operates on an array of returns and, where applicable, on the level alpha and the window size (n).
 * 
 * The rolling iteration is applied daily from day n+1 to day N (with n = 250). The three implemented steps are:
 * 1. Historical Simulation (non-parametric): empirical quantile over the window.
 * 2. Normal Approach (parametric): closed-form formulas under the Gaussian distribution, using historical estimates for mean and 
 * standard deviation.
 * 3. Monte Carlo Simulation: simulation of 100,000 Normal scenarios, using historically estimated parameters on log-returns, 
 * followed by the calculation of VaR and ES with the historical formula.
 */

public class RiskMeasures {

	/**
	 * 1.a) HISTORICAL VaR
	 * The historical VaR corresponds to the worst possible realization of the relative returns that can occur in the best 1-alpha 
	 * scenarios. Since the historical method does not make any assumption on the theoretical distribution, this computation is 
	 * entirely based assuming that the relative returns follow the empirical distribution of the observed data. 
	 * This methodology is really representative of the real observed data but cannot describe scenarios that have never occurred 
	 * in the recent past.
	 * This method calculates the historical VaR at the alpha level for a given sample of returns.
     * @param data: the window of returns (n=250 observations)
     * @param alphaVaR: the left-tail probability (e.g., 0.01 for Basel II standards)
     * @return the VaR expressed in percentage terms according to the profit convention 
     */

	public static double computeHistoricalVaR(double[] data, double alphaVaR) {
		// Creation of a copy of the array of returns
		double [] sorted = Arrays.copyOf(data, data.length);
		// Orders the elements of the array in increasing order, so that we can create the empirical distribution 
		Arrays.sort(sorted);
		// This function computes the integer part in order to obtain the index of the alpha-quantile of the empirical distribution 
		int index = (int) Math.floor(alphaVaR*sorted.length);
		
		// if (index >= sorted.length) index = sorted.length -1;
		// if (index<0) index=0;
		
		// Historical VaR formula according to the profit convention. 
		return -sorted[index];
	}

	/**
	 * 1.b) HISTORICAL ES
	 * The historical ES is the average of the worst alpha realizations of the relative returns. Since the historical method 
	 * does not make any assumption on the theoretical distribution, this computation is entirely based assuming that the 
	 * relative returns follow the empirical distribution of the observed data. 
	 * This methodology is really representative of the real observed data but cannot describe scenarios that have never occurred 
	 * in the recent past. This method calculates the historical ES at the alpha level for a given sample of returns.
     * @param data: the window of returns (n=250 observations)
     * @param alphaES: the left-tail probability 
     * @return the ES expressed in percentage terms according to the profit convention 
	 */
	
	public static double computeHistoricalES(double[] data, double alphaES) {
		// Creation of a copy of the array of returns
		double [] sorted = Arrays.copyOf(data, data.length);
		// This method orders the elements of the array in increasing order, so that we can create the empirical distribution
		Arrays.sort(sorted);
		// This variable corresponds to the length of the ordered array.
		int n = sorted.length;
		// This function computes the integer part in order to obtain the index of the alpha-quantile of the empirical distribution 
		int index = (int) Math.floor(alphaES*n);
		
		//if(index<1) index=;
		
		/** 
		 * Computation of the historical ES. We have split the formula into two parts.
		 * The first term represents the average of the worst returns in the alpha tail. 
		 * The second term acts as an adjustment.
		 */
		
		// term 1
		double sumTail = 0.0; 
		// the loop for allows us to compute the summation 
		for (int i= 0; i<index; i++) {
			sumTail = sumTail + sorted[i];
		}
		double term1 = sumTail/(n*alphaES); //this is the arithmetic mean
		
		// term 2
		double term2=0.0;
		if (index<n) {
			double adjustmentTerm = (alphaES-((double)index/n))/alphaES;
			term2 = adjustmentTerm*sorted[index];
		}
		
		// Historical ES formula according to the profit convention 
		return -(term1+term2);
	}
    
	/**
	 * This method iteratively applies computeHistoricalVaR using a rolling window. For each day, starting from day n+1=251, 
	 * the method extracts the n=250 most recent observations to calculate the historical VaR. Since the estimation requires 
	 * a full window of data, the resulting series is shorter than the input, with a total length of data.length - windowLength.
	 * @param data: the complete series of portfolio returns
	 * @param alphaVaR: the VaR level (tail probability)
	 * @param windowLength: the size of the estimation window (n = 250) corresponding to the estimation sample
	 * @return the historical time series of daily VaR estimates
	 * @throws IllegalArgumentException if the window size exceeds the available data points
	 */
	public static double[] iterateHistoricalVaR(double[] data, double alphaVaR, int windowLength) throws  IllegalArgumentException{
		// The window length cannot be higher than the number of returns in the array 
		if(windowLength>= data.length)
			throw new IllegalArgumentException("windowLength cannot be > number of observations");
		// number of daily VaR estimates 
		int n = data.length - windowLength; 
		// This array contains the VaR series and its length is equal to the number of its daily estimates 
		double [] varSeries = new double [n];
		// This loop for fills the array of Var series 
		for (int i=0; i<n; i++) {
			// slide the window one day forward at each iteration, in order to get the following VaR in the series
			double [] window = Arrays.copyOfRange(data, i, i+windowLength);
			varSeries [i]= computeHistoricalVaR(window, alphaVaR); //we compute the historical VaR with the previous method for each index 
		}

        return varSeries;
	}	
	
	/**
	 * This method iteratively applies computeHistoricalES using a rolling window. For each day, starting from day n+1=251, 
	 * the method extracts the n=250 most recent observations to calculate the historical ES. Since the estimation requires a 
	 * full window of data, the resulting series is shorter than the input, with a total length of data.length - windowLength.
	 * @param data: the complete series of portfolio returns
	 * @param alphaES: the ES level (tail probability)
	 * @param windowLength: the size of the estimation window (n = 250) corresponding to the estimation sample
	 * @return the historical time series of daily ES estimates
	 * @throws IllegalArgumentException if the window size exceeds the available data points
	 */
	public static double[] iterateHistoricalES(double[] data, double alphaES, int windowLength) throws  IllegalArgumentException{
		// The window length cannot be higher than the number of returns in the array 
		if(windowLength>= data.length)
			throw new IllegalArgumentException("windowLength cannot be > number of observations");
		// number of daily ES estimates 
		int n = data.length - windowLength; //number of daily VaR estimates
		// This array contains the ES series and its length is equal to the number of its daily estimates 
		double [] esSeries = new double [n];
		// This loop for fills the array of ES series 
		for (int i=0; i<n; i++) {
			// slide the window one day forward at each iteration, in order to get the following ES in the series
			double [] window = Arrays.copyOfRange(data, i, i+windowLength);
			esSeries [i]= computeHistoricalES(window, alphaES); //we compute the historical ES with the previous method for each index 
		}
		return esSeries;
	}
	
	// Produce plots of historical VaR and ES.
	
	/** This method plots the rolling historical VaR series.
	 * @param dates: the trading dates 
	 * @param data: the complete series of portfolio returns
	 * @param alphaVaR: the VaR level (tail probability)
	 * @param windowLength: the size of the estimation window (n = 250) corresponding to the estimation sample
	 * @return the plot of historical VaR
	 * @throws IllegalArgumentException if there is not time aligment
	 */
	public static void plotIterateHistoricalVaR(LocalDate[] dates, double[] data, double alphaVaR, int windowLength) throws  IllegalArgumentException{
	  
		double [] varSeries = iterateHistoricalVaR(data, alphaVaR, windowLength); //this array allows us to create the following plot
		LocalDate[] plotDates = Arrays.copyOfRange(dates, windowLength+1, dates.length); //we create a copy of the array of trading dates  according to the simulation horizon
		//creation of the plot of historical VaR series
		DataCollectionAndPlotting.plotData(plotDates,varSeries , "Historical VaR ( alpha: "+alphaVaR+" )"); 
			
	}
	
	/** This method plots the rolling historical ES series.
	  * @param dates: the trading dates 
	  * @param data: the complete series of portfolio returns
	  * @param alphaES: the ES level (tail probability)
	  * @param windowLength: the size of the estimation window (n = 250) corresponding to the estimation sample
	  * @return the plot of historical ES
	  * @throws IllegalArgumentException if there is not time aligment
	  */
	public static void plotIterateHistoricalES(LocalDate[] dates, double[] data, double alphaES, int windowLength) throws  IllegalArgumentException{
		
		double [] esSeries = iterateHistoricalES(data, alphaES, windowLength); //this array allows us to create the following plot 
		
		LocalDate[] plotDates = Arrays.copyOfRange(dates, windowLength+1, dates.length); //we create a copy of the array of trading dates  according to the simulation horizon
		//creation of the plot of historical ES series
		DataCollectionAndPlotting.plotData(plotDates, esSeries , "Historical ES ( alpha: "+alphaES+" )"); 
		
	}
	
    /**
     * 2) The parametric Normal VaR and ES.
     * Under the assumption of Gaussian returns, risk measures are derived via the inverted cumulative distribution function and the 
     * density function of a Normal distribution with mean mu and standard deviation sigma. 
     * Compared to the historical method, this approach produces a much smoother time series since it is less sensitive to "jumps" caused 
     * by extreme observations entering or exiting the rolling window. However, it tends to underestimate risk when actual market 
     * returns show heavy tails that deviate from the normal distribution.
     * mu and sigma are estimated using the same 250-day rolling window employed in the historical computation.

    /**
     * 2.a) The parametric Normal Var.
     * The parametric Normal VaR corresponds to the worst possible realization of the relative returns that can occur in the best 1-alpha 
     * scenarios, assuming that the returns follow a Normal distribution.
	 * This method calculates the parametric Normal VaR at the alpha level for a given sample of returns.
     * @param data: the window of returns (n=250 observations)
     * @param alphaVaR: the left-tail probability (e.g., 0.01 for Basel II standards)
     * @return the VaR expressed in percentage terms according to the profit convention
     */
     public static double computeNormalVaR(double[] data, double alphaVaR) {

        /*
         * Thanks to the importation of the package org.apache.commons.math3.distribution.NormalDistribution, we can compute the mean 
         * and the standard deviation of the array of relative returns (the sample).
         */
        double mu    = computeMean(data);    
        double sigma = computeStd(data, mu);
        
        NormalDistribution stdNormal = new NormalDistribution(); //we create a Standard Normal distribution (object of the class NormalDistribution)
        /* This variable corresponds to the alpha-quantile we can obtain by plugging the level alpha into the inverted cumulative 
         * probability distribution of the Standard Normal */
        double zAlpha = stdNormal.inverseCumulativeProbability(alphaVaR); //Phi^-1(alpha)

        return -mu - zAlpha * sigma; //Parametric Normal VaR formula according to the profit convention
       
    }
     
     /**
      * 2.b) The parametric Normal ES.
      * The parametric Normal ES corresponds to the average of the worst alpha realizations of the relative returns, assuming that the 
      * returns follow the distribution of a Normal with parameters mu and sigma. This method calculates the parametric Normal ES at 
      * the alpha level for a given sample of returns.
      * @param data: the window of returns (n=250 observations)
      * @param alphaES: the left-tail probability (e.g., 0.025)
      * @return the ES expressed in percentage terms according to the profit convention 
      */
    public static double computeNormalES(double[] data, double alphaES) {

        //computation of the mean and the standard deviation of the sample
        double mu    = computeMean(data);
        double sigma = computeStd(data, mu);

        NormalDistribution stdNormal = new NormalDistribution(); //we create a Standard Normal distribution
        /* This variable corresponds to the alpha-quantile, which we can obtain by plugging the level alpha into the inverted cumulative 
         * probability distribution of the Standard Normal. */
        double zAlpha   = stdNormal.inverseCumulativeProbability(alphaES); //Phi^-1(alpha)
        /* This variable corresponds to the value of the Standard Normal distribution function computed in the alpha-quantile 
         * we have found above. */
        double phiAlpha = stdNormal.density(zAlpha); //Phi'(Phi^-1(alpha))                     
       
        return -mu + (phiAlpha / alphaES) * sigma; //Parametric Normal ES formula according to the profit convention
        
    }

    /*
     * This method iterates the parametric Normal VaR by rolling parametric Normal VaR series. We have the same sliding-window structure as the 
     * historical version. At each step the window data is used to re-estimate mu and sigma. 
     */
    public static double[] iterateNormalVaR(double[] data, double alphaVaR, int windowLength)
            throws IllegalArgumentException {
    	// The window length cannot be higher than the number of returns in the array 
        if (windowLength >= data.length)
            throw new IllegalArgumentException("Window length must be less than data length.");
        // number of daily VaR estimates 
        int n = data.length - windowLength;
        // This array contains the VaR series and its length is equal to the number of its daily estimates 
        double[] varSeries = new double[n];
        // This loop for fills the array of VaR series 
        for (int i = 0; i < n; i++) {
        	// slide the window one day forward at each iteration, in order to get the following VaR in the series
            double[] window = Arrays.copyOfRange(data, i, i + windowLength);
            varSeries[i] = computeNormalVaR(window, alphaVaR); //here we use another method to compute the parametric Normal VaR
        }

        return varSeries;
    }

    /*
     * This method iterates the parametric Normal ES by rolling parametric Normal ES series. We have the same sliding-window structure 
     * as the historical version.
     * At each step the window data is used to re-estimate mu and sigma. 
     */
    public static double[] iterateNormalES(double[] data, double alphaES, int windowLength)
            throws IllegalArgumentException {
    	// The window length cannot be higher than the number of returns in the array 
        if (windowLength >= data.length)
            throw new IllegalArgumentException("Window length must be less than data length.");
        // number of daily ES estimates 
        int n = data.length - windowLength;
        // This array contains the ES series and its length is equal to the number of its daily estimates 
        double[] esSeries = new double[n];
        // This loop for fills the array of ES series 
        for (int i = 0; i < n; i++) {
        	// slide the window one day forward at each iteration, in order to get the following ES in the series
            double[] window = Arrays.copyOfRange(data, i, i + windowLength); 
            esSeries[i] = computeNormalES(window, alphaES); //here we use another method to compute the parametric Normal ES
        }

        return esSeries;
    }

    /**
     This method plots the rolling parametric Normal VaR series. It has the same date alignment as the Historical plot.
     */
    public static void plotIterateNormalVaR(LocalDate[] dates, double[] data,  double alphaVaR, int windowLength)
                                            throws IllegalArgumentException {

        double[] varSeries = iterateNormalVaR(data, alphaVaR, windowLength); //here we use another method to compute the parametric Normal VaR
        LocalDate[] plotDates = Arrays.copyOfRange(dates, windowLength + 1, dates.length); //we create a copy of the array of trading dates 
        //creation of the plot of the parametric Normal VaR series
        DataCollectionAndPlotting.plotData(plotDates, varSeries,
                "Normal VaR (α=" + alphaVaR + ")");
    }

    /**
    This method plots the rolling parametric Normal ES series. It has the same date alignment as the Historical plot.
    */
    public static void plotIterateNormalES(LocalDate[] dates, double[] data, double alphaES, int windowLength)
                                           throws IllegalArgumentException {

        double[] esSeries  = iterateNormalES(data, alphaES, windowLength); //here we use another method to compute the parametric Normal ES
        LocalDate[] plotDates = Arrays.copyOfRange(dates, windowLength + 1, dates.length); //we create a copy of the array of trading dates 
        //creation of the plot of the parametric Normal ES series 
        DataCollectionAndPlotting.plotData(plotDates, esSeries,
                "Normal ES (α=" + alphaES + ")");
    }

    /*
     * 3) Montecarlo Var and ES.
     * The Monte Carlo method combines the two previous steps: on the one hand, we assume that the relative returns have Normal 
     * distribution over the 250-day rolling window; on the other hand, VaR and ES are calculated empirically but here the sample is 
     * simulated, not historical. 
     * This methodology randomly draws values of a Standard Uniform distribution (which goes from 0 to 1) and uses them (pseudorandom 
     * numbers) to generate quantiles through the inverted probability distribution function of a Standard Normal. 
     * These quantiles represent the simulated realizations, which can be infinite
     * According to the Central Limit Theorem, as the number of simulations increases, the results converge toward the Normal parametric 
     * results. 
     */

    /**
     * This method iteratively applies Monte Carlo simulations to return both VaR and ES series in a single pass.
     * For each window, we take the relative returns of both assets, convert it to log-returns, then we estimate mu and sigma historically
     * for each asset and do a Monte Carlo simulation on the log-returns, using two different seeds in order to differentiate simulations.
     * This allows us to reach independence between the assets. 
     * Finally, the method switches from the simulated log-returns to relative returns and compute the relative portfolio returns, 
     * which we will use for the computation of historical VaR and ES. The method generates and sorts 100,000 scenarios only once, 
     * extracting both VaR and ES from the same sorted array. 
     * We have chosen to compute both risk measures in one single operation in order to optimize the time computation and efficiency.
     * @param data1: the complete series of relative returns of asset 1 (NEM)
     * @param data2: the complete series of relative returns of asset 2 (LLY)
     * @param alphaVaR: the VaR level
     * @param alphaES: the ES level 
     * @param windowLength: the size of the estimation window (e.g., $n = 250$)
     * @param weight1: the proportion of portfolio budget allocated to the first asset (NEM)
     * @param weight2: the proportion of portfolio budget allocated to the second asset (LLY)
     * @return a 2xN matrix where index [0] contains the VaR series and index [1] contains the ES series
     */
    public static double[][] iterateMonteCarlo(double[] data1, double [] data2, double alphaVaR, double alphaES, int windowLength, double weight1, double weight2)  throws IllegalArgumentException {                                    
    	/* The window length cannot be higher than the number of returns in both arrays. Even if just one of the two arrays' length is 
    	 * higher than the window length, the program throws an alert. */
    	if (windowLength >= data1.length || windowLength >= data2.length)
            throw new IllegalArgumentException("Window length must be less than data length.");
    	
    	// number of daily VaR and ES estimates
        int n = Math.min(data1.length, data2.length)- windowLength;
        // These arrays contain the VaR and ES series and their length is equal to the number of their daily estimates 
        double[] varSeries = new double[n];
        double[] esSeries  = new double[n];
       
        // This loop for fills the arrays of Var and ES series
        for (int j = 0; j < n; j++) {
        	// slides the window one day forward at each iteration, in order to get the following VaR and ES
            double[] window1 = Arrays.copyOfRange(data1, j, j + windowLength);
            double[] window2 = Arrays.copyOfRange(data2, j, j + windowLength);
           
            // we create two new arrays to host the log-returns of the assets, which will be necessary just for Monte Carlo estimation 
            double[] logWindow1 = new double[windowLength];
            double[] logWindow2 = new double[windowLength];
            
            // this loop for allows us to witch to log-returns for both assets
            for (int i = 0; i < windowLength; i++) {
                logWindow1[i] = Math.log(1.0 + window1[i]);
                logWindow2[i] = Math.log(1.0 + window2[i]);
            }
            
            /* Historical estimators for both assets from the rolling window, we have computed them through the methods we have 
             created in the class MonteCarloSimulation */
            double mu1   = computeMean(logWindow1);
            double sigma1 = computeStd(logWindow1, mu1);
            
            double mu2   = computeMean(logWindow2);
            double sigma2 = computeStd(logWindow2, mu2);

            // Generation of 100,000 simulated returns from the Normal distribution (mu, sigma) of the log-returns (once per window translation). 
            double[] simulatedLog1 = MonteCarloSimulation.simulate(mu1, sigma1, 2L*j);
            double[] simulatedLog2 = MonteCarloSimulation.simulate(mu2, sigma2, 2L*j+1);
            
            // Switch back to relative returns and computation of the portfolio returns
            // we define the length of the array containing the portfolio returns 
            int simulationLength = Math.min(simulatedLog1.length, simulatedLog2.length);
            //creation of the array of the portfolio returns 
            double [] simulatedPtf = new double [simulationLength];
            // this for loop fills the array of portfolio returns
            for (int i = 0; i < simulationLength; i++) {
                double simulatedReturn1 = Math.exp(simulatedLog1[i]) - 1.0;
                double simulatedReturn2 = Math.exp(simulatedLog2[i]) - 1.0;
               simulatedPtf[i]= weight1*simulatedReturn1 + weight2*simulatedReturn2;
           
            }
            // Computation of the historical risk measures on the simulated sample.
            varSeries[j] = computeHistoricalVaR(simulatedPtf, alphaVaR);
            esSeries[j]  = computeHistoricalES (simulatedPtf, alphaES);
        }
        // 2xN matrix where index [0] contains the VaR series and index [1] contains the ES series.
        return new double[][]{ varSeries, esSeries };
    }

    
    //---------------------------------------------------------------------------------------------------------------------------------------------------
    
    /*
     * WE HAVE COMPLETED THE FOLLOWING METHODS AS REQUESTED IN THE PROJECT WORK BUT WE HAVE DECIDED NOT TO USE THEM AND TO IMPLEMENT
     * ANOTHER ONE IN ORDER TO BE MORE EFFICIENT. INSTEAD WE USE A DIRECT WAY TO COMPUTE THE RISK MEASURES IN THE TEST CLASS
     */
    
    /**
     * This method returns only the Monte Carlo VaR series. It relies on iterateMonteCarlo to avoid generating and sorting 
     * the simulated sample twice when VaR and ES are requested separately. 
     * @param data1: the complete series of returns of asset 1 (NEM)
     * @param data2: the complete series of returns of asset 2 (LLY)
     * @paramVaR alpha: the VaR level
     * @param windowLength: the size of the estimation window (e.g., n = 250)
     * @param weight1: the proportion of portfolio budget allocated to the first asset (NEM)
     * @param weight2: the proportion of portfolio budget allocated to the second asset (LLY)
     * @return the time series of daily Monte Carlo VaR estimates
     * @throws IllegalArgumentException if the window size is greater than or equal to the number of available observations 
     */ 
    public static double[] iterateMonteCarloVaR(double[] data1, double[] data2, double alphaVaR, int windowLength, double weight1, double weight2 )
            throws IllegalArgumentException {
    	//computation of both VaR and ES with the alpha level of VaR but then returns only the VaR. 
        return iterateMonteCarlo(data1, data2, alphaVaR, alphaVaR, windowLength, weight1, weight2)[0];
    }

    /**
     * This method returns only the Monte Carlo ES series. It relies on iterateMonteCarlo to avoid generating and sorting 
     * the simulated sample twice when VaR and ES are requested separately. 
     * @param data1: the complete series of returns of asset 1 (NEM)
     * @param data2: the complete series of returns of asset 2 (LLY)
     * @param alphaES: the ES level
     * @param windowLength: the size of the estimation window (e.g., n = 250)
     * @param weight1: the proportion of portfolio budget allocated to the first asset (NEM)
     * @param weight2: the proportion of portfolio budget allocated to the second asset (LLY)
     * @return the time series of daily Monte Carlo VaR estimates
     * @throws IllegalArgumentException if the window size is greater than or equal to the number of available observations 
     */
    public static double[] iterateMonteCarloES(double[] data1, double[] data2, double alphaES, int windowLength, double weight1,double weight2)
            throws IllegalArgumentException {
    	//computation of both VaR and ES with the alpha level of VaR but then returns only the ES. 
        return iterateMonteCarlo(data1, data2, alphaES, alphaES, windowLength,  weight1, weight2)[1];
    }

   /**
    * This method generates the plot for the rolling Monte Carlo VaR series, maintaining the same temporal alignment used for 
    * the historical and parametric Normal plots.
    * @param dates: the trading dates
    * @param data1: the complete series of returns of asset 1 (NEM)
    * @param data2: the complete series of returns of asset 2 (LLY)
    * @param alphaVaR: the alpha level for the VaR
    * @param windowLength: the size of the estimation window (e.g., n = 250)
    * @param weight1: the proportion of portfolio budget allocated to the first asset (NEM)
    * @param weight2: the proportion of portfolio budget allocated to the second asset (LLY)
    * @throws IllegalArgumentException if the window size is incompatible with the length of the data array
    */
    public static void plotIterateMonteCarloVaR(LocalDate[] dates, double[] data1, double[] data2, double alphaVaR, int windowLength, double weight1, double weight2)
                                               throws IllegalArgumentException {
    	//here we use another method to compute the MC VaR
        double[]    varSeries = iterateMonteCarloVaR(data1, data2, alphaVaR, windowLength, weight1, weight2); 
        //we create a copy of the array of trading dates 
        LocalDate[] plotDates = Arrays.copyOfRange(dates, windowLength + 1, dates.length); 
        //creation of the plot of the Monte Carlo VaR series 
        DataCollectionAndPlotting.plotData(plotDates, varSeries,
                "Monte Carlo VaR (α=" + alphaVaR + ")");
    }

    /**
     * This method generates the plot for the rolling Monte Carlo ES series, maintaining the same temporal alignment used for 
     * the historical and Normal plots.
     * @param dates: the trading dates
     * @param data1: the complete series of returns of asset 1 (NEM)
     * @param data2: the complete series of returns of asset 2 (LLY)
     * @param alphaES: the alpha level for the ES
     * @param windowLength: the size of the estimation window (e.g., n = 250)
     * @param weight1: the proportion of portfolio budget allocated to the first asset (NEM)
     * @param weight2: the proportion of portfolio budget allocated to the second asset (LLY)
     * @throws IllegalArgumentException if the window size is incompatible with the length of the data array
     */
    public static void plotIterateMonteCarloES(LocalDate[] dates, double[] data1, double[] data2,
                                                double alphaES, int windowLength, double weight1, double weight2)
            throws IllegalArgumentException {
    	//here we use another method to compute the MC ES
        double[]    esSeries  = iterateMonteCarloES(data1, data2, alphaES, windowLength, weight1, weight2); 
        //we create a copy of the array of trading dates 
        LocalDate[] plotDates = Arrays.copyOfRange(dates, windowLength + 1, dates.length); 
      //creation of the plot of the Monte Carlo ES series 
        DataCollectionAndPlotting.plotData(plotDates, esSeries,
                "Monte Carlo ES (β=" + alphaES + ")");
    }
    
    //---------------------------------------------------------------------------------------------------------------------------------------------------
    
    // These methods are shared by the parametric Normal and Monte Carlo risk measures. 
    
    // The mean mu represents the average daily portfolio return over the estimation window. 
  static double computeMean(double[] data) {
    	// This loop for allows us to compute the mean 
        double sum = 0.0;
        for (int i = 0; i < data.length; i++) {
            sum = sum + data[i]; 
        }
        return sum / data.length;
    }

    // The standard deviation sigma of the sample
  static double computeStd(double[] data, double mean) {
    	// This loop for allows us to compute the sample standard deviation 
        double sumSq = 0.0;
        for (int i = 0; i < data.length; i++) {
            double deviation = data[i] - mean; //calculate the deviation of the element [i] from the mean
            sumSq = sumSq + (deviation * deviation); //square the deviation and add it to the running sum
        }
        return Math.sqrt(sumSq / data.length);
    }
}
    
    


    

    
    
    
    

	
	
	

