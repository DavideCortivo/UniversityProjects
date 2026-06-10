package it.univr.riskmanagement;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Scanner;

/**
 * The test class outputs all plots taking the model parameters as inputs.
 * This class collects all model parameters and allows to graphically represent risk measures.
 * We have decided to make the program more efficient: instead of repeatedly calling the plotting methods in the RiskMeasures class, 
 * the risk measure arrays are built only once using the iterate methods from the RiskMeasures class (historical, normal and Monte Carlo). 
 * These arrays are then passed to the plotting methods in the DataCollectionAndPlotting class, specifically to plotMultipleData 
 * when comparing multiple series on the same plot. In this way, computationally intensive calculations (particularly Monte Carlo, 
 * which simulates 100,000 scenarios per window) are performed only once for each series, and all subsequent plots reuse the existing 
 * results.
 */

public class Tests {
	
	/**
	 * This method contains the entire test sequence: it creates an object of the DataManagement class, 
	 * sets the capital invested in the two assets, generates the preliminary plots (prices 
	 * of the two assets, portfolio returns, and overlaid prices), and finally calculates 
	 * and plots the risk measures using the three methodologies specified in the project
	 * @throws IOException if an error occurs while reading the Excel files containing the 
	 * historical price series
	 */
	public static void main(String[] args) throws IOException {
		// Creation of a new object of the class DataManagement object, which we will use to load prices and calculate portfolio returns 
		DataManagement tester = new DataManagement();
		double budget1 = 5000;//capital allocated to the NEM stock 
		double budget2 = 5000;//caoital allocated to the LLY stock
		double totalBudget = budget1+budget2; // total budget
		double weight1 = budget1/totalBudget; //proportion of the total budget allocated to the NEM stock
		double weight2 = budget2/totalBudget; //proportion of the total budget allocated to the LLY stock
		
		
		/*
		 * We set this input giver in order to give the user the choice to plot the risk measure type of interest.
		 * You have to insert on the console one of these two strings: percentage or monetary 
		 */
		Scanner inputGiver = new Scanner(System.in);
		System.out.println("Insert risk measure type: percentage or monetary:");
		String riskMeasureType = inputGiver.nextLine();
		
		tester.plotPricesAsset1(); //it plots the trajectory of the prices of NEM from the beginning of 2019 to the end of 2025
		tester.plotPricesAsset2(); //it plots the trajectory of the prices of LLY from the beginning of 2019 to the end of 2025
		tester.plotPortfolioReturns(budget1,budget2); // it plots the portfolio returns from the beginning of 2019 to the end of 2025
		tester.plotPricesTogether();//it plots both trajectories from the beginning of 2019 to the end of 2025
		
		
		// It returns the portfolio returns series, which serves as the primary input for risk measure calculations 
		double[] returns = tester.getPortfolioReturns(budget1, budget2);
		double [] returns1 = tester.getAsset1Returns();
		double [] returns2 = tester.getAsset2Returns();
		
		int windowLength = 250; //rolling window size
		double alphaVaR = 0.01; //VaR alpha level 
		double alphaES = 0.025; //ES alpha level 
		
		
		/*
		 * In this section, all risk measure arrays are calculated only once. Our strategy is to perform the heavy computations here 
		 * and reuse the resulting arrays for all subsequent plots, in order to avoid redundant rolling window calculations. 
		 * For the Monte Carlo method specifically, we use the combined iterateMonteCarlo method which returns both VaR and ES 
		 * simultaneously, halving the computation time compared to calling the two methods separately.
		 */
		
		// Rolling series of historical VaR and ES 
		double[] histVaR = RiskMeasures.iterateHistoricalVaR(returns, alphaVaR, windowLength);
		double[] histES  = RiskMeasures.iterateHistoricalES(returns, alphaES, windowLength);

		// Rolling series of parametric Normal VaR and ES 
		double[] normVaR = RiskMeasures.iterateNormalVaR(returns, alphaVaR, windowLength);
		double[] normES  = RiskMeasures.iterateNormalES(returns, alphaES, windowLength);

		// Rolling series of Monte Carlo VaR and ES 
		double[][] mcResults = RiskMeasures.iterateMonteCarlo(returns1, returns2, alphaVaR, alphaES, windowLength, weight1, weight2);
		double[] mcVaR = mcResults[0];
		double[] mcES  = mcResults[1];
		
		// creation of the arrays of risk measures in monetary terms
		double [] histVaRM = new double [histVaR.length];
		double [] normVaRM = new double [normVaR.length];
		double [] mcVaRM = new double [mcVaR.length];
		double [] histESM = new double [histES.length];
		double [] normESM = new double [normES.length];
		double [] mcESM = new double [mcES.length];
		
		// we compute VaR and ES in monetary terms thanks to this loop for, which fills the arrays
		for (int i=0; i< histVaR.length; i++) {
			histVaRM[i] = histVaR[i]*totalBudget;
			normVaRM[i] = normVaR[i]*totalBudget;
			mcVaRM[i] = mcVaR[i]*totalBudget;
			histESM[i] = histES[i]*totalBudget;
			normESM[i] = normES[i]*totalBudget;
			mcESM[i] = mcES[i]*totalBudget;
		}
		
		/* Constructs the date array aligned with the rolling series: starting from windowLength +1, it ensures that every 
		* estimates has its corresponding date. */
		LocalDate[] plotDates = Arrays.copyOfRange(tester.getDates(), windowLength + 1, tester.getDates().length);	

		/*
		 * Once the risk measure arrays are obtained, we pass them directly to the plotMultipleData method of the 
		 * DataCollectionAndPlotting class. This allows to create comparison plots without any recalculation: the same array can be reused 
		 * across multiple different plots, whether for comparing methodologies or for comparing VaR and ES within the same methodology.
		 */

		/* We implement a loop to print different types of risk measures: in percentage or monetary terms
		 * If the user does not insert "percentage" or "monetary", the program, after plotting prices and computing Monte Carlo, keeps
		 * asking to insert a correct string. if another unacceptable string is inserted, the program keeps the same calculations and asks
		 * immediately for a correct string.
		 */
		boolean validInput = false;

		while (!validInput) {

			switch(riskMeasureType) {
			case "percentage" :
				// Method for plotting of the single historical risk measures in percentage terms 
				DataCollectionAndPlotting.plotData(plotDates, histVaR, "Historical VaR" );
				DataCollectionAndPlotting.plotData(plotDates, histES, "Historical ES" );
				
				// Method for plotting of the single parametric Normal risk measures in percentage terms 
				DataCollectionAndPlotting.plotData(plotDates, normVaR, " Normal VaR" );
				DataCollectionAndPlotting.plotData(plotDates, normES, " Normal ES" );
				
				// Method for the plotting of the single Monte Carlo risk measures in percentage terms 
				DataCollectionAndPlotting.plotData(plotDates, mcVaR, " Monte Carlo VaR" );
				DataCollectionAndPlotting.plotData(plotDates, mcES, " Monte Carlo ES" );

				// Plot A: Historical VaR vs ES in percentage terms 
				double[][] histTog = {histVaR, histES};
				String[] labelsHist = {"Historical VaR (1%)", "Historical ES (2.5%)"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, histTog, labelsHist, "Historical: VaR vs ES");

				// Plot B: Normal VaR vs ES in percentage terms 
				double[][] normTog = {normVaR, normES};
				String[] labelNorm = {"Normal VaR (1%)", "Normal ES (2.5%)"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, normTog, labelNorm, "Normal: VaR vs ES");

				// Plot C: Monte Carlo VaR and ES in percentage terms 
				double[][] mcTog = {mcVaR, mcES};
				String[] labelsMC = {"MC VaR (1%)", "MC ES (2.5%)"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, mcTog, labelsMC, "Monte Carlo: VaR vs ES");

				// Plot D: Comparison of all VaR methodologies in percentage terms 
				double[][] allVaR = {histVaR, normVaR, mcVaR};
				String[] labelsVaR = {"Historical VaR (1%)", "Normal VaR (1%)", "Monte Carlo VaR (1%)"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, allVaR, labelsVaR, "VaR: Historical vs Normal vs Monte Carlo");

				// Plot E: Comparison of all ES methodologies in percentage terms 
				double[][] allES = {histES, normES, mcES};
				String[] labelsES = {"Historical ES (2.5%)", "Normal ES (2.5%)", "Monte Carlo ES (2.5%)"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, allES, labelsES, "ES: Historical vs Normal vs Monte Carlo");	
				validInput = true;
				break; 

				//Plotting in monetary terms
			case "monetary":	

				// Method for plotting the single historical risk measures in monetary terms 
				DataCollectionAndPlotting.plotData(plotDates, histVaRM, "Historical VaR in monetary terms" );
				DataCollectionAndPlotting.plotData(plotDates, histESM, "Historical ES in monetary terms" );


				// Method for plotting the single parametric Normal risk measures in monetary terms 
				DataCollectionAndPlotting.plotData(plotDates, normVaRM, " Normal VaR in monetary terms" );
				DataCollectionAndPlotting.plotData(plotDates, normESM, " Normal ES in monetary terms" );


				// Method for the plotting the single Monte Carlo risk measures in monetary terms
				DataCollectionAndPlotting.plotData(plotDates, mcVaRM, " Monte Carlo VaR in monetary terms" );
				DataCollectionAndPlotting.plotData(plotDates, mcESM, " Monte Carlo ES in monetary terms" );


				// Plot A: Historical VaR vs ES in monetary terms
				double[][] histTogM = {histVaRM, histESM};
				String[] labelsHistM = {"Historical VaR (1%) in monetary terms", "Historical ES (2.5%) in monetary terms"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, histTogM, labelsHistM, "Historical: VaR vs ES in monetary terms");

				// Plot B: Normal VaR vs ES in monetary terms
				double[][] normTogM = {normVaRM, normESM};
				String[] labelNormM = {"Normal VaR (1%) in monetary terms", "Normal ES (2.5%) in monetary terms"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, normTogM, labelNormM, "Normal: VaR vs ES in monetary terms");

				// Plot C: Monte Carlo VaR and ES in monetary terms 
				double[][] mcTogM = {mcVaRM, mcESM};
				String[] labelsMCM = {"MC VaR (1%) in monetary terms", "MC ES (2.5%) in monetary terms"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, mcTogM, labelsMCM, "Monte Carlo: VaR vs ES in monetary terms");

				// Plot D: Comparison of all VaR methodologies in monetary terms
				double[][] allVaRM = {histVaRM, normVaRM, mcVaRM};
				String[] labelsVaRM = {"Historical VaR (1%) in monetary terms", "Normal VaR (1%) in monetary terms", "Monte Carlo VaR (1%) in monetary terms"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, allVaRM, labelsVaRM, "VaR in monetary terms: Historical vs Normal vs Monte Carlo");

				// Plot E: Comparison of all ES methodologies in monetary terms 
				double[][] allESM = {histESM, normESM, mcESM};
				String[] labelsESM = {"Historical ES (2.5%)in monetary terms", "Normal ES (2.5%) in monetary terms", "Monte Carlo ES (2.5%) in monetary terms"};
				DataCollectionAndPlotting.plotMultipleData(plotDates, allESM, labelsESM, "ES in monetary terms: Historical vs Normal vs Monte Carlo");
				validInput = true;
				break;

			// if an incorrect string is inserted, the program throws this message
			default: System.out.println("Insert a valid string: percentage or monetary");
			riskMeasureType = inputGiver.nextLine();
			break;

			}
		}
	}
}	




		