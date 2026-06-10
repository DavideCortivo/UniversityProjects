package it.univr.riskmanagement;

import java.io.IOException;
import java.util.Arrays;

import java.time.LocalDate;


import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * This class takes the output of DataCollectionAndPlotting to compute portfolio returns
 * and produce the corresponding time series.
 * We apply the profit convention across the entire project. This means that returns/capital requirements are shown as positive 
 * numbers. In the test class, we will plot VaR and ES both in monetary and percentage terms, representing respectively capital 
 * requirements (amount of money we need to absorb a potential loss and reach acceptability) and the percentage potential loss 
 * w.r.t. the total budget. 
 * The investment time horizon is one day.
 */

public class DataManagement {
	
	/*
	Use the methods in DataCollectionAndPlotting to initialize the vectors containing prices and dates.
	@array pricesAsset1: prices of asset 1 (NEM)
	@array pricesAsset2: prices of asset 2 (LLY)
	@array dates: corresponding dates
	*/
	 
	private double[] pricesAsset1;
	private double[] pricesAsset2;
	private LocalDate[] dates;
	
	/**
	 * This constructor allows us to load historical prices and dates series from the Excel files in order to compute portfolio returns. 
	 * The assets we have chosen are Newmont Goldcorp (NEM) and Eli Lilly (LLY), because they have correlation around zero. 
	 * They belong, respectively, to the miner and the health-care sectors.
	 * This constructor initializes the three arrays (prices for both assets and dates) by calling the methods from DataCollectionAndPlotting.
	 * @throws IOException (if an error occurs while reading the excel file).
	 * 
	 */
	public DataManagement() throws IOException {
		
		/* we use the method getHistoricalPricesStock1() to fill the array with the series of historical opening prices, for the 
		 * first asset (NEM) */
		pricesAsset1 = DataCollectionAndPlotting.getHistoricalPricesStock1();
		/* we use the method getHistoricalPricesStock2() to fill the array with the series of historical opening prices for the 
		 * second asset (LLY) */
		pricesAsset2 = DataCollectionAndPlotting.getHistoricalPricesStock2();
		/* this array contains the trading dates of both assets */
		dates = DataCollectionAndPlotting.getDates();
	}
	
	/**
	 * This method computes the series of relative portfolio returns. Given the portfolio weights (weight1 and weight2) and the 
	 * daily relative returns rNEM and rLLY of the two assets, the portfolio return at day t is calculated as: 
	 * R(t) = weight1 * (( S1(t) / S1(t-1) )-1)+ weight2 * (( S2(t) / S2(t-1) )-1).
     * We set a 50/50 allocation.
     * @param budget1: capital invested in Asset 1 (NEM)
     * @param budget2: capital invested in Asset 2 (LLY)
     * @return a series of portfolio returns with length N - 1 (where N is the number of prices)
     */
	
	public double[] getPortfolioReturns(double budget1, double budget2) {	
		/* n variable allows us to manage the length of the returns array and define its length.
		 * We have adopted this convention in order to call a single variable in the following loop for. 
		 * And also we adopt a way to avoid bugs setting that the length of the return array must coincide with the minimum length
		 * between the two time series, because if one is longer than the other the for loop would generate problems within our code 
		 * infrastructure. 
		 */
		int n = Math.min(pricesAsset1.length, pricesAsset2.length);
		
		/* We create the array of relative returns. The return at day i uses prices at i-1, thus we have n-1 observations and the 
		 * length is n-1 (because we cannot compute the return in t=0)
		 */
		double [] returns = new double [n-1];
		
		/* We fill the array of portfolio relative returns using a loop for computing the relative returns of each stock daily. 
		 * Then we calculate the corresponding weighted return */
		for (int i=1; i<n; i++) {
			/* we compute the weights of the portfolio from the invested capitals */
			double value1 = (budget1/pricesAsset1[0])*pricesAsset1[i-1];
			double value2 = (budget2/pricesAsset2[0])*pricesAsset2[i-1];
			double weight1 = value1/(value1+value2);
			double weight2 =1-weight1;
			double r1 = (pricesAsset1[i]/pricesAsset1[i-1])-1; //daily relative return of NEM
			double r2 = (pricesAsset2[i]/pricesAsset2[i-1])-1; //daily relative return of LLY
			returns [i-1]= weight1*r1 + weight2*r2;
			
		}
		
		return returns;
	}
	/**
	 * This method gets the relative return of the first asset only, in this case NEM
	 * @return relative returns of NEM
	 */
	public double [] getAsset1Returns() {
		int n =pricesAsset1.length;
        double [] returns1 = new double [n-1];
		
		// We fill the array of returns using a loop for computing the relative returns of the NEM stock daily. 
		for (int i=1; i<n; i++) {
			returns1 [i-1]= (pricesAsset1[i]/pricesAsset1[i-1])-1;
		}
		return returns1; 
	}
	/**
	 * This method gets the relative return of the second asset only, in this case LLY
	 * @return relative returns of NEM
	 */
	public double [] getAsset2Returns() {
		int n = pricesAsset2.length;
        double [] returns2 = new double [n-1];
		
		// We fill the array of returns using a loop for computing the relative returns of the LLY stock daily.
		for (int i=1; i<n; i++) {
			returns2 [i-1]= (pricesAsset2[i]/pricesAsset2[i-1])-1;
		}
		return returns2; 
	}

	
	
	// PLOTS
	
	/**
	 * This method returns the plot of the prices of the first asset (NEM)
	 * @throws IOException (if an error occurs while reading the Excel file).
	 */
	public void plotPricesAsset1() throws IOException {
		DataCollectionAndPlotting.plotData(dates, pricesAsset1, "Prices Newmont Goldcorp");
	}
	
	/**
	 * This method returns the plot of the prices of the second asset (LLY)
	 */
	public void plotPricesAsset2() throws IOException {
		DataCollectionAndPlotting.plotData(dates, pricesAsset2, "Prices Eli LIlly");
	}
	
	/**
     * Calculates the portfolio returns, generates a plot, and returns the series.
     * To maintain temporal alignment on the x-axis, the first date is excluded (it starts from 1, not 0).
     * @param budget1: capital invested in NEM
     * @param budget2: capital invested in LLY
     * @return the portfolio returns series
     * @throws IOException if the plot generation fails
     */
	public double[] plotPortfolioReturns(double budget1, double budget2) throws IOException {
		// fill the array of returns, computing them with the method implemented before
		double[] returns = getPortfolioReturns(budget1, budget2); 
		// creation of a copy of the dates array but discards the first element (element 0) in order to align data and dates
        LocalDate[] datesToPlot = Arrays.copyOfRange(dates, 1, dates.length);
		DataCollectionAndPlotting.plotData(datesToPlot, returns, "Portfolio Returns");
		return returns; 
	}
	
	/**
	 * This method allows us to plot stock prices together in order to make a comparison
	 */
	public void plotPricesTogether() 
	{
	    double[][] data = {pricesAsset1, pricesAsset2};
	    String[] labels = {"NEM ", "LLY"};
	    DataCollectionAndPlotting.plotMultipleData(dates, data, labels, "Asset Prices Comparison");
	}
	
    /**
     * This method allows us to expose the array of dates so that the class RiskMeasure can correctly align the VaR and ES plots 
     * with the price time-line.
     * @return an array of trading dates
     */
	public LocalDate[] getDates() {
		return dates;
	}
	
	
	/**
	 * This method shows the correlation between the two assets relative returns.
	 * @return the correlation between the assets.
	 */
	public double getCorrelation() 
	{
		PearsonsCorrelation correlation = new PearsonsCorrelation();
		return correlation.correlation(getAsset1Returns(), getAsset2Returns());
	}
	
}
