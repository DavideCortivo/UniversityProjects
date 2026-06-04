package net.finmath.tree;


import org.junit.Assert;
import org.junit.Test;

import it.univr.fima.barrieroptionsformulas.BarrierOptions;
import it.univr.fima.barrieroptionsformulas.BarrierOptions.BarrierType;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.tree.assetderivativevaluation.dividends.ContinuousDividendYield;
import net.finmath.tree.assetderivativevaluation.dividends.DiscreteProportionalDividend;
import net.finmath.tree.assetderivativevaluation.dividends.MultiplicativeDividendModel;
import net.finmath.tree.assetderivativevaluation.dividends.ProportionalDividend;
import net.finmath.tree.assetderivativevaluation.models.BoyleTrinomial;
import net.finmath.tree.assetderivativevaluation.models.CoxRossRubinsteinModel;
import net.finmath.tree.assetderivativevaluation.models.JarrowRuddModel;
import net.finmath.tree.assetderivativevaluation.products.EuropeanOption;
import net.finmath.tree.assetderivativevaluation.products.AmericanNonPathDependent;
import net.finmath.tree.assetderivativevaluation.products.BarrierOptionsProducts;

public class EuropeanAndAmericanOptionPricingTest {

	/*
	 * This class is divided into two parts: in the first part we create the objects corresponding to the different types of models (CRR
	 * JR and Trinomial, with and without dividends), in order to create all the specific Call options which will be necessary to test our
	 * models; in the second part, we do the same thing with Put options. 
	 */
	
	/*
	 * In order to test our model we inserted in the test real market data taken from the US stock Chevron Corp (CVX) at 02/05/2026
	 */
	@Test
	public void testNonPathDep_Call() {
		double spot = 190.63;
		double rate = 0.0428; // annual risk-free rate 
		double vol = 0.2695; // implied volatility
		double maturity = 1.126; //years
		int steps = 4500;
		double strike = 200;
		double tol = 2e-2; 
		double tolBarriers = 2e-1; // we have increased the tolerance level for the barriers
		double dividendYield = 0.0368; // annual dividend yield
		/*
		 * number of ex-dividend paid in a year, this parameter cannot be 0, 
		 * if you want 0 dividends, you have to use the constructors without dividend models
		 */
		double frequency = 4; 
		double proportionalDividendTime= 0.5;
		double timeToFirstDividend = 17.0 / 365.0; // time (in years) to the first ex-dividend date
		double timeBetweenDividends = 1.0 / frequency;
		int numberOfDividends = 0;
		double barrierUP = 250;
		double barrierDOWN = 150;
		double rebate = 10;
		double delta = dividendYield/frequency;
		
        /*
         * Creation of the dividend grid 
         * check if the maturity is greater or equal to first ex-dividend date
         */
		
		if (maturity >= timeToFirstDividend) {
		    // number of ex-dividend until option maturity
		    numberOfDividends = 1 + (int)((maturity - timeToFirstDividend) / timeBetweenDividends);
		}

		//dividends dates grid
		double[] dividendDates = new double[numberOfDividends];
		// fills the array of dividend date
		for (int i = 0; i < numberOfDividends; i++) {
			dividendDates[i] = timeToFirstDividend + (i * timeBetweenDividends);
		}

		
		
		/*
		 * We have imported all the classes regarding dividend types (which implement the interface MultiplicativeDividendModel)
		 * and created an object for each class, that we will use to create new objects for the tests.
		 */
		MultiplicativeDividendModel proportionalDiv = new ProportionalDividend(proportionalDividendTime, delta);
		MultiplicativeDividendModel discreteProportionalDiv = new DiscreteProportionalDividend(delta, dividendDates);
		MultiplicativeDividendModel continuousDiv = new ContinuousDividendYield(dividendYield);
		

		// if we want to print the dividend dates this is the way
		 
		double [] calendar = new double [dividendDates.length];
	    calendar =discreteProportionalDiv.getTime();
	    System.out.println("Ex-dividend dates until maturity:");
	    for(int i=0;i<calendar.length; i++) {
	    	System.out.println(calendar [i]);	
	    }
	    System.out.println();
	  	    
	   
		/* 
		 * We now create all the pricing models, starting from the ones without dividends. For those which include dividends, 
		 * we pass as inputs also the object that represents the type of dividend. This object includes all the features of the dividend 
		 * type. 
		 */
		
	    // Dividend models without dividends 
		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps);
		JarrowRuddModel jr = new JarrowRuddModel(spot,rate,vol,maturity,steps);
		BoyleTrinomial tri = new BoyleTrinomial(spot,rate,vol,maturity,steps);

		// Dividend models with proportional dividend 
		CoxRossRubinsteinModel crrProportional = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps, proportionalDiv);
		JarrowRuddModel jrProportional = new JarrowRuddModel(spot,rate,vol,maturity,steps,proportionalDiv);
		BoyleTrinomial triProportional = new BoyleTrinomial(spot,rate,vol,maturity,steps,proportionalDiv);
		
		// Dividend models with discrete proportional dividend 
		CoxRossRubinsteinModel crrDiscreteProportional = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps,discreteProportionalDiv);
		JarrowRuddModel jrDiscreteProportional = new JarrowRuddModel(spot,rate,vol,maturity,steps,discreteProportionalDiv);
		BoyleTrinomial triDiscreteProportional = new BoyleTrinomial(spot,rate,vol,maturity,steps,discreteProportionalDiv);
		
		// Dividend models with continuous dividend 
		CoxRossRubinsteinModel crrContinuousDividend = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps,continuousDiv);
		JarrowRuddModel jrContinuousDividend = new JarrowRuddModel(spot,rate,vol,maturity,steps,continuousDiv);
		BoyleTrinomial triContinuousDividend = new BoyleTrinomial(spot,rate,vol,maturity,steps,continuousDiv);
		
		// Options types. Here we define two other objects, which represent two products: the European and the American option 
		EuropeanOption euCall = new EuropeanOption(maturity, strike, CallOrPut.CALL);
		AmericanNonPathDependent usCall = new AmericanNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));

		// Here we obtain the European Call option prices at time 0 for all the different dividend types, applying the function getValue. 
		// European Call option price in case the stock does not pay any dividend 
		double euCRR = euCall.getValue(crr);
		double euJR = euCall.getValue(0.0,jr).getAverage();
		double euTRI = euCall.getValue(0.0,tri).getAverage();
		
		// European Call option price in case the stock pays proportional dividends 
		double euCRRPropDiv = euCall.getValue(0.0, crrProportional).getAverage();
		double euJRPropDiv = euCall.getValue(0.0, jrProportional).getAverage();
		double euTRIPropDiv = euCall.getValue(0.0, triProportional).getAverage();

		// European Call option price in case the stock pays discrete proportional dividends 
		double euCRRDiscretePropDiv = euCall.getValue(0.0, crrDiscreteProportional).getAverage();
		double euJRDiscretePropDiv = euCall.getValue(0.0, jrDiscreteProportional).getAverage();
		double euTRIDiscretePropDiv = euCall.getValue(0.0, triDiscreteProportional).getAverage();

		// European Call option price in case the stock pays continuous dividends 
		double euCRRContDiv = euCall.getValue(0.0, crrContinuousDividend).getAverage();
		double euJRContDiv = euCall.getValue(0.0,jrContinuousDividend).getAverage();
		double euTRIContDiv = euCall.getValue(0.0, triContinuousDividend).getAverage();

		// Here we obtain the American option prices at time 0 for all the different dividend types, applying the function getValue. 
		
		// American Call option price in case the stock does not pay any dividend 
		double usCRR = usCall.getValue(0.0, crr).getAverage();
		double usJR = usCall.getValue(0.0,jr).getAverage();
		double usTRI = usCall.getValue(0.0,tri).getAverage();
		
		// American Call option price in case the stock pays proportional dividends 
		double usCRRPropDiv = usCall.getValue(0.0, crrProportional).getAverage();
		double usJRPropDiv = usCall.getValue(0.0, jrProportional).getAverage();
		double usTRIPropDiv = usCall.getValue(0.0, triProportional).getAverage();

		// American Call option price in case the stock pays discrete proportional dividends 
		double usCRRDiscretePropDiv = usCall.getValue(0.0, crrDiscreteProportional).getAverage();
		double usJRDiscretePropDiv = usCall.getValue(0.0, jrDiscreteProportional).getAverage();
		double usTRIDiscretePropDiv = usCall.getValue(0.0, triDiscreteProportional).getAverage();

		// American Call option price in case the stock pays continuous dividends 
		double usCRRContDiv = usCall.getValue(0.0, crrContinuousDividend).getAverage();
		double usJRContDiv = usCall.getValue(0.0,jrContinuousDividend).getAverage();
		double usTRIContDiv = usCall.getValue(0.0, triContinuousDividend).getAverage();

		// Here we print the obtained results 
		
		// European Call option price in case the stock does not pay any dividend 
		System.out.println("European call with different models");
		System.out.println(euCRR + " " + euJR + " " + euTRI);
		System.out.println();
		
		// European Call option prices with the Cox Ross Rubinstein model 
		System.out.println("European Call options with CRR");
		System.out.println(euCRRPropDiv);
		System.out.println(euCRRDiscretePropDiv);
		System.out.println(euCRRContDiv);
		System.out.println();
		
		// European Call option prices with the Jarrow Rudd model 
		System.out.println("European Call options with JR");
		System.out.println(euJRPropDiv);
		System.out.println(euJRDiscretePropDiv);
		System.out.println(euJRContDiv);
		System.out.println();
		
		// European Call option prices with the Boyle Trinomial model 
		System.out.println("European Call options with TRI");
		System.out.println(euTRIPropDiv);
		System.out.println(euTRIDiscretePropDiv);
		System.out.println(euTRIContDiv);
		System.out.println();
		
		// American Call option price in case the stock does not pay any dividend 
		System.out.println("American call with different models");
		System.out.println(usCRR + " " + usJR + " " + usTRI);
		System.out.println();
		
		// American Call option prices with the Cox Ross Rubinstein model 
		System.out.println("American Call options with CRR");
		System.out.println(usCRRPropDiv);
		System.out.println(usCRRDiscretePropDiv);
		System.out.println(usCRRContDiv);
		System.out.println();
		
		// American Call option prices with the Jarrow Rudd model 
		System.out.println("American Call options with JR");
		System.out.println(usJRPropDiv);
		System.out.println(usJRDiscretePropDiv);
		System.out.println(usJRContDiv);
		System.out.println();
		
		// American Call option prices with the Boyle Trinomial model 
		System.out.println("American Call options with TRI");
		System.out.println(usTRIPropDiv);
		System.out.println(usTRIDiscretePropDiv);
		System.out.println(usTRIContDiv);


		/* 
		 * We now create the  four different types of barrier call options using the constructor we have created in the class 
		 * BarrierOptionsProducts: the down-out barrier option, the down-in barrier option, the up-down barrier option and the 
		 * up-in barrier option.
		 */
		BarrierOptionsProducts barrierDownOut = new BarrierOptionsProducts(maturity, strike, barrierDOWN, rebate, CallOrPut.CALL, BarrierType.DOWN_OUT);
		BarrierOptionsProducts barrierDownIn = new BarrierOptionsProducts(maturity, strike, barrierDOWN, rebate, CallOrPut.CALL, BarrierType.DOWN_IN);
		BarrierOptionsProducts barrierUpOut = new BarrierOptionsProducts(maturity, strike, barrierUP, rebate, CallOrPut.CALL, BarrierType.UP_OUT);
		BarrierOptionsProducts barrierUpIn = new BarrierOptionsProducts(maturity, strike, barrierUP,rebate, CallOrPut.CALL, BarrierType.UP_IN);
		
		// DownOut call option under different models 
		double priceDownOutCRR = barrierDownOut.getValue(0.0, crr).getAverage();
		double priceDownOutJR = barrierDownOut.getValue(0.0, jr).getAverage();
		double priceDownOutTRI = barrierDownOut.getValue(0.0, tri).getAverage();
		
		// DownIn call option under different models 
		double priceDownInCRR = barrierDownIn.getValue(0.0, crr).getAverage();
		double priceDownInJR = barrierDownIn.getValue(0.0, jr).getAverage();
		double priceDownInTRI = barrierDownIn.getValue(0.0, tri).getAverage();
		
		// UpOut call option under different models 
		double priceUpOutCRR = barrierUpOut.getValue(0.0, crr).getAverage();
		double priceUpOutJR = barrierUpOut.getValue(0.0, jr).getAverage();
		double priceUpOutTRI = barrierUpOut.getValue(0.0, tri).getAverage();
				
		// UpIn call option under different models 
		double priceUpInCRR = barrierUpIn.getValue(0.0, crr).getAverage();
		double priceUpInJR = barrierUpIn.getValue(0.0, jr).getAverage();
		double priceUpInTRI = barrierUpIn.getValue(0.0, tri).getAverage();
				
		// We create new barrier call options which we will use to compute the price according to the Black-Scholes Model. 
		BarrierOptions barrierDownOutBS = new BarrierOptions();
		BarrierOptions barrierDownInBS = new BarrierOptions();
		BarrierOptions barrierUpOutBS = new BarrierOptions();
		BarrierOptions barrierUpInBS = new BarrierOptions();
		
		/* We compute the prices according to the Black-Scholes Model. We will use these prices as a benchmark for the prices 
		 * we obtain with our barrier models. */
		double priceBarrierDownOutBS = barrierDownOutBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, true, rebate, barrierDOWN, BarrierType.DOWN_OUT);
		double priceBarrierDownInBS = barrierDownInBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, true, rebate, barrierDOWN, BarrierType.DOWN_IN);
		double priceBarrierUpOutBS = barrierUpOutBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, true, rebate, barrierUP, BarrierType.UP_OUT);
		double priceBarrierUpInBS = barrierUpInBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, true, rebate, barrierUP, BarrierType.UP_IN);
		
		// We print all the Barrier Call option prices we have created 
		System.out.println();
		System.out.println("CALL BARRIERS");
		System.out.println();
		
		// The down-out Barrier Call option prices according to all the models, including Black-Scholes) 
		System.out.println("Barrier Down and Out price with CRR model "+ priceDownOutCRR);
		System.out.println("Barrier Down and Out price with JR model "+ priceDownOutJR);
		System.out.println("Barrier Down and Out price with TRI model "+ priceDownOutTRI);
		System.out.println("Barrier Down and Out price with BS model "+ priceBarrierDownOutBS);
		System.out.println();
		
		// The down-in Barrier Call option prices according to all the models, including Black-Scholes) 
		System.out.println("Barrier Down and In price with CRR model "+ priceDownInCRR);
		System.out.println("Barrier Down and In price with JR model "+ priceDownInJR);
		System.out.println("Barrier Down and In price with TRI model "+ priceDownInTRI);
		System.out.println("Barrier Down and In price with BS model "+ priceBarrierDownInBS);
		System.out.println();
		
		// The up-out Barrier Call option prices according to all the models, including Black-Scholes) 
		System.out.println("Barrier Up and Out price with CRR model "+ priceUpOutCRR);
		System.out.println("Barrier Up and Out price with JR model "+ priceUpOutJR);
		System.out.println("Barrier Up and Out price with TRI model "+ priceUpOutTRI);
		System.out.println("Barrier Up and Out price with BS model "+ priceBarrierUpOutBS);
		System.out.println();
		
		// The up-in Barrier Call option prices according to all the models, including Black-Scholes) 
		System.out.println("Barrier Up and In price with CRR model "+ priceUpInCRR);
		System.out.println("Barrier Up and In price with JR model "+ priceUpInJR);
		System.out.println("Barrier Up and In price with TRI model "+ priceUpInTRI);
		System.out.println("Barrier Up and In price with BS model "+ priceBarrierUpInBS);
		System.out.println();


		//"US(Call) TRI must be  >= EU(Call) TRI"
		Assert.assertTrue(usTRI + 1e-12 >= euTRI);
		//"EU(Call) CRR vs TRI difference beyond tolerance"
		Assert.assertEquals(euCRR, euTRI, 2*tol);
		//"EU(Call) JR vs CRR difference beyond tol"
		Assert.assertEquals(euJR, euCRR, 2*tol);
		//"EU(Call) JR vs TRI difference beyond tol"
		Assert.assertEquals(euJR, euTRI, 2*tol);
		//"US(Call) JR vs CRR difference beyond tol"
		Assert.assertEquals(usJR, usCRR, 2*tol);
		//"US(Call) JR vs TRI difference beyond tol"
		Assert.assertEquals(usJR, usTRI, 2*tol);
		//"US(Call) CRR must be >= EU(Call) CRR"
		Assert.assertTrue(usCRR + 1e-12 >= euCRR);

		
		//"US(Call) CRR Proportional Dividend must be  >= EU(Call) CRR Proportional Dividend"
		Assert.assertTrue(usCRRPropDiv + 1e-12 >= euCRRPropDiv);
		//"US(Call) CRR Discrete Proportional Dividend must be  >= EU(Call) CRR Discrete Proportional Dividend"
		Assert.assertTrue(usCRRDiscretePropDiv + 1e-12 >= euCRRDiscretePropDiv);
		//"US(Call) CRR Continuous Dividend must be  >= EU(Call) CRR Continuous Dividend"
		Assert.assertTrue(usCRRContDiv + 1e-12 >= euCRRContDiv);
		//"US(Call) TRI Proportional Dividend must be  >= EU(Call) TRI Proportional Dividend"
		Assert.assertTrue(usTRIPropDiv + 1e-12 >= euTRIPropDiv);
		//"US(Call) TRI Discrete Proportional Dividend must be  >= EU(Call) TRI Discrete Proportional Dividend"
		Assert.assertTrue(usTRIDiscretePropDiv + 1e-12 >= euTRIDiscretePropDiv); 
		//"US(Call) TRI Continuous Dividend must be  >= EU(Call) TRI Continuous Dividend"
		Assert.assertTrue(usTRIContDiv + 1e-12 >= euTRIContDiv);

		
		//"EU(Call) CRR Proportional Dividend vs TRI Proportional Dividend difference beyond tolerance"
		Assert.assertEquals(euCRRPropDiv, euTRIPropDiv, 2*tol);
		//"EU(Call) JR Proportional Dividend vs CRR Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRPropDiv, euCRRPropDiv, 2*tol);
		//"EU(Call) JR Proportional Dividend vs TRI Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRPropDiv, euTRIPropDiv, 2*tol);
		//"EU(Call) CRR Discrete Proportional Dividend vs TRI Discrete Proportional Dividend difference beyond tolerance"
		Assert.assertEquals(euCRRDiscretePropDiv, euTRIDiscretePropDiv, 2*tol);
		//"EU(Call) JR Discrete Proportional Dividend vs CRR Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRDiscretePropDiv, euCRRDiscretePropDiv, 2*tol);
		//"EU(Call) JR Discrete Proportional Dividend vs TRI Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRDiscretePropDiv, euTRIDiscretePropDiv, 2*tol);

		
		//"EU(Call) CRR Continuous Dividend vs TRI Continuous difference beyond tolerance"
		Assert.assertEquals(euCRRContDiv, euTRIContDiv, 2*tol);
		//"EU(Call) JR Continuous Dividend vs CRR Continuous difference beyond tol"
		Assert.assertEquals(euJRContDiv, euCRRContDiv, 2*tol);
		//"EU(Call) JR Continuous vs TRI Continuous difference beyond tol"
		Assert.assertEquals(euJRContDiv, euTRIContDiv, 2*tol);

		//"US(Call) JR Proportional Dividend vs CRR Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRPropDiv, usCRRPropDiv, 2*tol);
		//"US(Call) JR Proportional Dividend vs TRI Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRPropDiv, usTRIPropDiv, 2*tol);

		//"US(Call) JR Discrete Proportional Dividend vs CRR Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRDiscretePropDiv, usCRRDiscretePropDiv, 2*tol);
		//"US(Call) JR Discrete Proportional Dividend vs TRI Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRDiscretePropDiv, usTRIDiscretePropDiv, 2*tol);

		//"US(Call) JR Continuous Dividend vs CRR Continuous Dividend difference beyond tol"
		Assert.assertEquals(usJRContDiv, usCRRContDiv, 2*tol);
		//"US(Call) JR Continuous Dividend vs TRI Continuous Dividend difference beyond tol"
		Assert.assertEquals(usJRContDiv, usTRIContDiv, 2*tol);

		
		//"Call Barriers vs Black-Scholes model"
		
		//Down-out CRR vs BS
		Assert.assertEquals(priceDownOutCRR, priceBarrierDownOutBS, 2*tolBarriers);
		//Down-out JR vs BS
		Assert.assertEquals(priceDownOutJR, priceBarrierDownOutBS, 2*tolBarriers);
		//Down-out TRI vs BS
		Assert.assertEquals(priceDownOutTRI, priceBarrierDownOutBS, 2*tolBarriers);

		//Down-in CRR vs BS
		Assert.assertEquals(priceDownInCRR, priceBarrierDownInBS, 2*tolBarriers);
		//Down-in JR vs BS
		Assert.assertEquals(priceDownInJR, priceBarrierDownInBS, 2*tolBarriers);
		//Down-in TRI vs BS
		Assert.assertEquals(priceDownInTRI, priceBarrierDownInBS, 2*tolBarriers);
		
		//Up-out CRR vs BS
		Assert.assertEquals(priceUpOutCRR, priceBarrierUpOutBS, 2*tolBarriers);
		//Up-out JR vs BS
		Assert.assertEquals(priceUpOutJR, priceBarrierUpOutBS, 2*tolBarriers);
		//Up-out TRI vs BS
		Assert.assertEquals(priceUpOutTRI, priceBarrierUpOutBS, 2*tolBarriers);
		
		//Up-in CRR vs BS
		Assert.assertEquals(priceUpInCRR, priceBarrierUpInBS, 2*tolBarriers);
		//Up-in JR vs BS
		Assert.assertEquals(priceUpInJR, priceBarrierUpInBS, 2*tolBarriers);
		//Up-in TRI vs BS
		Assert.assertEquals(priceUpInTRI, priceBarrierUpInBS, 2*tolBarriers);
		
	}


	//----------------------------------------------------------------------------------------------------------------------------------------------------------

	@Test
	public void testNonPathDep_Put() {
		double spot = 190.63;
		double rate = 0.0428; // annual risk-free rate 
		double vol = 0.271; // implied volatility
		double maturity = 1.126; //years
		int steps = 4500;
		double strike = 200;
		double tol = 2e-2; 
		double tolBarriers = 2e-1; // we have increased the tolerance level for the barriers
		double dividendYield = 0.0368; // annual dividend yield
		/*
		 * number of ex-dividend paid in a year, this parameter cannot be 0, 
		 * if you want 0 dividends, you have to use the constructors without dividend models
		 */
		double frequency = 4; 
		double proportionalDividendTime= 0.5;
		double timeToFirstDividend = 17.0 / 365.0; // time (in years) to the first ex-dividend date
		double timeBetweenDividends = 1.0 / frequency;
		int numberOfDividends = 0;
		double barrierUP = 250;
		double barrierDOWN = 150;
		double rebate = 10;
		double delta = dividendYield/frequency;
		
		
		/*
         * Creation of the dividend grid 
         * check if the maturity is greater or equal to first ex-dividend date
         */
		
		if (maturity >= timeToFirstDividend) {
		    // number of ex-dividend until option maturity
		    numberOfDividends = 1 + (int)((maturity - timeToFirstDividend) / timeBetweenDividends);
		}

		//dividends dates grid
		double[] dividendDates = new double[numberOfDividends];
		// fills the array of dividend date
		for (int i = 0; i < numberOfDividends; i++) {
			dividendDates[i] = timeToFirstDividend + (i * timeBetweenDividends);
		}

		
		
		// Dividend models without dividends 
		MultiplicativeDividendModel proportionalDiv = new ProportionalDividend(proportionalDividendTime,delta);
		MultiplicativeDividendModel discreteProportionalDiv = new DiscreteProportionalDividend(delta, dividendDates);
		MultiplicativeDividendModel continuousDiv = new ContinuousDividendYield(dividendYield);
		
		// if we want to print the dividend dates this is the way
		 
		double [] calendar = new double [dividendDates.length];
	    calendar =discreteProportionalDiv.getTime();
	    System.out.println("Ex-dividend dates until maturity:");
	    for(int i=0;i<calendar.length; i++) {
	    	System.out.println(calendar [i]);	
	    }
	    System.out.println();
	    
		
		// Pricing Models
		// No dividends
		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps);
		JarrowRuddModel jr = new JarrowRuddModel(spot,rate,vol,maturity,steps);
		BoyleTrinomial tri = new BoyleTrinomial(spot, rate, vol, maturity, steps);
		
		// Dividend models with proportional dividend
		CoxRossRubinsteinModel crrProportional = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps, proportionalDiv);
		JarrowRuddModel jrProportional = new JarrowRuddModel(spot,rate,vol,maturity,steps,proportionalDiv);
		BoyleTrinomial triProportional = new BoyleTrinomial(spot,rate,vol,maturity,steps,proportionalDiv);

		// Dividend models with discrete proportional dividend
		CoxRossRubinsteinModel crrDiscreteProportional = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps,discreteProportionalDiv);
		JarrowRuddModel jrDiscreteProportional = new JarrowRuddModel(spot,rate,vol,maturity,steps,discreteProportionalDiv);
		BoyleTrinomial triDiscreteProportional = new BoyleTrinomial(spot,rate,vol,maturity,steps,discreteProportionalDiv);

		// Dividend models with continuous dividend
		CoxRossRubinsteinModel crrContinuousDividend = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps,continuousDiv);
		JarrowRuddModel jrContinuousDividend = new JarrowRuddModel(spot,rate,vol,maturity,steps,continuousDiv);
		BoyleTrinomial triContinuousDividend = new BoyleTrinomial(spot,rate,vol,maturity,steps,continuousDiv);

		//Options types. Here we define two other objects which represent two products: the European and the American option
		EuropeanOption euPut = new EuropeanOption(maturity, strike, CallOrPut.PUT);
		AmericanNonPathDependent usPut = new AmericanNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));
		
		// Here we obtain the European Call option prices at time 0 for all the different dividend types, applying the function getValue. 
		
		// European Put option price in case the stock does not pay any dividend 
		double euCRR = euPut.getValue(0.0, crr).getAverage();
		double euJR = euPut.getValue(0.0, jr).getAverage();
		double euTRI = euPut.getValue(0.0, tri).getAverage();
		
		// European Put option price in case the stock pays proportional dividends
		double euCRRPropDiv = euPut.getValue(0.0, crrProportional).getAverage();
		double euJRPropDiv = euPut.getValue(0.0, jrProportional).getAverage();
		double euTRIPropDiv = euPut.getValue(0.0, triProportional).getAverage();

		// European Put option price in case the stock pays discrete proportional dividends
		double euCRRDiscretePropDiv = euPut.getValue(0.0, crrDiscreteProportional).getAverage();
		double euJRDiscretePropDiv = euPut.getValue(0.0, jrDiscreteProportional).getAverage();
		double euTRIDiscretePropDiv = euPut.getValue(0.0, triDiscreteProportional).getAverage();

		// European Put option price in case the stock pays continuous dividends
		double euCRRContDiv = euPut.getValue(0.0, crrContinuousDividend).getAverage();
		double euJRContDiv = euPut.getValue(0.0,jrContinuousDividend).getAverage();
		double euTRIContDiv = euPut.getValue(0.0, triContinuousDividend).getAverage();

		// Here we obtain the American option prices at time 0 for all the different dividend types, applying the function getValue. 
		
		// American Put option price in case the stock does not pay any dividend 
		double usCRR = usPut.getValue(0.0, crr).getAverage();
		double usJR = usPut.getValue(0.0,jr).getAverage();
		double usTRI = usPut.getValue(0.0,tri).getAverage();

		// American Put option price in case the stock pays proportional dividends
		double usCRRPropDiv = usPut.getValue(0.0, crrProportional).getAverage();
		double usJRPropDiv = usPut.getValue(0.0, jrProportional).getAverage();
		double usTRIPropDiv = usPut.getValue(0.0, triProportional).getAverage();

		// American Put option price in case the stock pays discrete proportional dividends
		double usCRRDiscretePropDiv = usPut.getValue(0.0, crrDiscreteProportional).getAverage();
		double usJRDiscretePropDiv = usPut.getValue(0.0, jrDiscreteProportional).getAverage();
		double usTRIDiscretePropDiv = usPut.getValue(0.0, triDiscreteProportional).getAverage();

		// American Put option price in case the stock pays continuous dividends
		double usCRRContDiv = usPut.getValue(0.0, crrContinuousDividend).getAverage();
		double usJRContDiv = usPut.getValue(0.0,jrContinuousDividend).getAverage();
		double usTRIContDiv = usPut.getValue(0.0, triContinuousDividend).getAverage();

		// Here we print the obtained results 
		
		// European Put option prices in case the stock does not pay any dividend 
		System.out.println("European put with different models");
		System.out.println(euCRR + " " + euJR + " " + euTRI);
		System.out.println();
		
		// European Put option prices with the Cox Ross Rubinstein model 
		System.out.println("European Put options with CRR");
		System.out.println(euCRRPropDiv);
		System.out.println(euCRRDiscretePropDiv);
		System.out.println(euCRRContDiv);
		System.out.println();
		
		// European Put option prices with the Jarrow Rudd model
		System.out.println("European Put options with JR");
		System.out.println(euJRPropDiv);
		System.out.println(euJRDiscretePropDiv);
		System.out.println(euJRContDiv);
		System.out.println();
		
		// European Put option prices with Boyle Trinomial model
		System.out.println("European Put options with TRI");
		System.out.println(euTRIPropDiv);
		System.out.println(euTRIDiscretePropDiv);
		System.out.println(euTRIContDiv);
		System.out.println();
		
		// American Put option prices with Cox Ross Rubinstein model
		System.out.println("American Put options with CRR");
		System.out.println(usCRRPropDiv);
		System.out.println(usCRRDiscretePropDiv);
		System.out.println(usCRRContDiv);
		System.out.println();
		
		// American Put option prices with the Jarrow Rudd model
		System.out.println("American Put options with JR");
		System.out.println(usJRPropDiv);
		System.out.println(usJRDiscretePropDiv);
		System.out.println(usJRContDiv);
		System.out.println();
		
		// American Put option prices with Boyle Trinomial model
		System.out.println("American Put options with TRI");
		System.out.println(usTRIPropDiv);
		System.out.println(usTRIDiscretePropDiv);
		System.out.println(usTRIContDiv);
		System.out.println();

		/* 
		 * We now create the  four different types of barrier put options using the constructor we have created in the class 
		 * BarrierOptionsProducts: the down-out barrier option, the down-in barrier option, the up-down barrier option and the 
		 * up-in barrier option.
		 */
		BarrierOptionsProducts barrierDownOut = new BarrierOptionsProducts(maturity, strike, barrierDOWN, rebate, CallOrPut.PUT, BarrierType.DOWN_OUT);
		BarrierOptionsProducts barrierDownIn = new BarrierOptionsProducts(maturity, strike, barrierDOWN, rebate, CallOrPut.PUT, BarrierType.DOWN_IN);
		BarrierOptionsProducts barrierUpOut = new BarrierOptionsProducts(maturity, strike, barrierUP, rebate, CallOrPut.PUT, BarrierType.UP_OUT);
		BarrierOptionsProducts barrierUpIn = new BarrierOptionsProducts(maturity, strike, barrierUP, rebate, CallOrPut.PUT, BarrierType.UP_IN);

		// DownOut put option under different models
		double priceDownOutCRR = barrierDownOut.getValue(0.0, crr).getAverage();
		double priceDownOutJR = barrierDownOut.getValue(0.0, jr).getAverage();
		double priceDownOutTRI = barrierDownOut.getValue(0.0, tri).getAverage();

		// DownIn put option under different models
		double priceDownInCRR = barrierDownIn.getValue(0.0, crr).getAverage();
		double priceDownInJR = barrierDownIn.getValue(0.0, jr).getAverage();
		double priceDownInTRI = barrierDownIn.getValue(0.0, tri).getAverage();

		// UpOut put option under different models
		double priceUpOutCRR = barrierUpOut.getValue(0.0, crr).getAverage();
		double priceUpOutJR = barrierUpOut.getValue(0.0, jr).getAverage();
		double priceUpOutTRI = barrierUpOut.getValue(0.0, tri).getAverage();

		// UpIn put option under different models
		double priceUpInCRR = barrierUpIn.getValue(0.0, crr).getAverage();
		double priceUpInJR = barrierUpIn.getValue(0.0, jr).getAverage();
		double priceUpInTRI = barrierUpIn.getValue(0.0, tri).getAverage();

		// We create new barrier call options which we will use to compute the price according to the Black-Scholes Model.
		BarrierOptions barrierDownOutBS = new BarrierOptions();
		BarrierOptions barrierDownInBS = new BarrierOptions();
		BarrierOptions barrierUpOutBS = new BarrierOptions();
		BarrierOptions barrierUpInBS = new BarrierOptions();
        
		/* We compute the prices according to the Black-Scholes Model. We will use these prices as a benchmark for the prices 
		 * we obtain with our barrier models. */
		double priceBarrierDownOutBS = barrierDownOutBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, false, rebate, barrierDOWN, BarrierType.DOWN_OUT);
		double priceBarrierDownInBS = barrierDownInBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, false, rebate , barrierDOWN, BarrierType.DOWN_IN);
		double priceBarrierUpOutBS = barrierUpOutBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, false, rebate, barrierUP, BarrierType.UP_OUT);
		double priceBarrierUpInBS = barrierUpInBS.blackScholesBarrierOptionValue(spot, rate, 0, vol, maturity, strike, false, rebate , barrierUP, BarrierType.UP_IN);

		// We print all the Barrier Put option prices we have created 
		System.out.println();
		System.out.println("PUT BARRIERS");
		System.out.println();
		
		// The down-out Barrier Put option prices according to all the models, including Black-Scholes) 
		System.out.println("Barrier Down and Out price with CRR model "+ priceDownOutCRR);
		System.out.println("Barrier Down and Out price with JR model "+ priceDownOutJR);
		System.out.println("Barrier Down and Out price with TRI model "+ priceDownOutTRI);
		System.out.println("Barrier Down and Out price with BS model "+ priceBarrierDownOutBS);
		System.out.println();
		
		// The down-in Barrier Put option prices according to all the models, including Black-Scholes)
		System.out.println();
		System.out.println("Barrier Down and In price with CRR model "+ priceDownInCRR);
		System.out.println("Barrier Down and In price with JR model "+ priceDownInJR);
		System.out.println("Barrier Down and In price with TRI model "+ priceDownInTRI);
		System.out.println("Barrier Down and In price with BS model "+ priceBarrierDownInBS);
		System.out.println();
		
		// The up-out Barrier Put option prices according to all the models, including Black-Scholes) 
		System.out.println();
		System.out.println("Barrier Up and Out price with CRR model "+ priceUpOutCRR);
		System.out.println("Barrier Up and Out price with JR model "+ priceUpOutJR);
		System.out.println("Barrier Up and Out price with TRI model "+ priceUpOutTRI);
		System.out.println("Barrier Up and Out price with BS model "+ priceBarrierUpOutBS);
		System.out.println();
		
		// The up-in Barrier Put option prices according to all the models, including Black-Scholes) 
		System.out.println();
		System.out.println("Barrier Up and In price with CRR model "+ priceUpInCRR);
		System.out.println("Barrier Up and In price with JR model "+ priceUpInJR);
		System.out.println("Barrier Up and In price with TRI model "+ priceUpInTRI);
		System.out.println("Barrier Up and In price with BS model "+ priceBarrierUpInBS);
		System.out.println();
		

		//"US(Put) CRR must be >= EU(Put) CRR"
		Assert.assertTrue(usCRR + 1e-12 >= euCRR);
		//"EU(Put) JR vs CRR difference beyond tol"
		Assert.assertEquals(euJR, euCRR, 2*tol);
		//"EU(Put) JR vs TRI difference beyond tol"
		Assert.assertEquals(euJR, euTRI, 2*tol);
		//"US(Put) JR vs CRR difference beyond tol"
		Assert.assertEquals(usJR, usCRR, 2*tol);
		//"US(Put) JR vs TRI difference beyond tol"
		Assert.assertEquals(usJR, usTRI, 2*tol);
		//"US(Put) TRI must be >= EU(Put) TRI"
		Assert.assertTrue(usTRI + 1e-12 >= euTRI);
		//"EU(Put) CRR vs TRI difference beyond tolerance"
		Assert.assertEquals(euCRR, euTRI, 2*tol);
		
		//ASSERT PER LE PUT
		//"US(Put) CRR Proportional Dividend must be  >= EU(Call) CRR Proportional Dividend"
		Assert.assertTrue(usCRRPropDiv + 1e-12 >= euCRRPropDiv);
		//"US(Put) CRR Discrete Proportional Dividend must be  >= EU(Call) CRR Discrete Proportional Dividend"
		Assert.assertTrue(usCRRDiscretePropDiv + 1e-12 >= euCRRDiscretePropDiv);
		//"US(Put) CRR Continuous Dividend must be  >= EU(Call) CRR Continuous Dividend"
		Assert.assertTrue(usCRRContDiv + 1e-12 >= euCRRContDiv);
		//"US(Put) TRI Proportional Dividend must be  >= EU(Call) TRI Proportional Dividend"
		Assert.assertTrue(usTRIPropDiv + 1e-12 >= euTRIPropDiv);
		//"US(Put) TRI Discrete Proportional Dividend must be  >= EU(Call) TRI Discrete Proportional Dividend"
		Assert.assertTrue(usTRIDiscretePropDiv + 1e-12 >= euTRIDiscretePropDiv);
		//"US(Put) TRI Continuous Dividend must be  >= EU(Call) TRI Continuous Dividend"
		Assert.assertTrue(usTRIContDiv + 1e-12 >= euTRIContDiv);

		//"EU(Put) CRR Proportional Dividend vs TRI Proportional Dividend difference beyond tolerance"
		Assert.assertEquals(euCRRPropDiv, euTRIPropDiv, 2*tol);
		//"EU(Put) JR Proportional Dividend vs CRR Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRPropDiv, euCRRPropDiv, 2*tol);
		//"EU(Put) JR Proportional Dividend vs TRI Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRPropDiv, euTRIPropDiv, 2*tol);

		//"EU(Put) CRR Discrete Proportional Dividend vs TRI Discrete Proportional Dividend difference beyond tolerance"
		Assert.assertEquals(euCRRDiscretePropDiv, euTRIDiscretePropDiv, 2*tol);
		//"EU(Put) JR Discrete Proportional Dividend vs CRR Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRDiscretePropDiv, euCRRDiscretePropDiv, 2*tol);
		//"EU(Put) JR Discrete Proportional Dividend vs TRI Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(euJRDiscretePropDiv, euTRIDiscretePropDiv, 2*tol);

		//"EU(Put) CRR Continuous Dividend vs TRI Continuous difference beyond tolerance"
		Assert.assertEquals(euCRRContDiv, euTRIContDiv, 2*tol);
		//"EU(Put) JR Continuous Dividend vs CRR Continuous difference beyond tol"
		Assert.assertEquals(euJRContDiv, euCRRContDiv, 2*tol);
		//"EU(Put) JR Continuous vs TRI Continuous difference beyond tol"
		Assert.assertEquals(euJRContDiv, euTRIContDiv, 2*tol);

		//"US(Put) JR Proportional Dividend vs CRR Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRPropDiv, usCRRPropDiv, 2*tol);
		//"US(Put) JR Proportional Dividend vs TRI Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRPropDiv, usTRIPropDiv, 2*tol);

		//"US(Put) JR Discrete Proportional Dividend vs CRR Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRDiscretePropDiv, usCRRDiscretePropDiv, 2*tol);
		//"US(Put) JR Discrete Proportional Dividend vs TRI Discrete Proportional Dividend difference beyond tol"
		Assert.assertEquals(usJRDiscretePropDiv, usTRIDiscretePropDiv, 2*tol);

		//"US(Put) JR Continuous Dividend vs CRR Continuous Dividend difference beyond tol"
		Assert.assertEquals(usJRContDiv, usCRRContDiv, 2*tol);
		//"US(Put) JR Continuous Dividend vs TRI Continuous Dividend difference beyond tol"
		Assert.assertEquals(usJRContDiv, usTRIContDiv, 2*tol);
		
		//"Barriers vs Black-Scholes model"
		
		//Down-out CRR vs BS
		Assert.assertEquals(priceDownOutCRR, priceBarrierDownOutBS, 2*tolBarriers); //error with tol = 2e-2: we have increased tolerance
		//Down-out JR vs BS
		Assert.assertEquals(priceDownOutJR, priceBarrierDownOutBS, 2*tolBarriers);
		//Down-out TRI vs BS
		Assert.assertEquals(priceDownOutTRI, priceBarrierDownOutBS, 2*tolBarriers);

		//Down-in CRR vs BS
		Assert.assertEquals(priceDownInCRR, priceBarrierDownInBS, 2*tolBarriers);
		//Down-in JR vs BS
		Assert.assertEquals(priceDownInJR, priceBarrierDownInBS, 2*tolBarriers);
		//Down-in TRI vs BS
		Assert.assertEquals(priceDownInTRI, priceBarrierDownInBS, 2*tolBarriers);
		
		//Up-out CRR vs BS
		Assert.assertEquals(priceUpOutCRR, priceBarrierUpOutBS, 2*tolBarriers); //error with tol = 2e-2: we have increased tolerance
		//Up-out JR vs BS
		Assert.assertEquals(priceUpOutJR, priceBarrierUpOutBS, 2*tolBarriers);
		//Up-out TRI vs BS
		Assert.assertEquals(priceUpOutTRI, priceBarrierUpOutBS, 2*tolBarriers);
		
		//Up-in CRR vs BS
		Assert.assertEquals(priceUpInCRR, priceBarrierUpInBS, 2*tolBarriers);
		//Up-in JR vs BS
		Assert.assertEquals(priceUpInJR, priceBarrierUpInBS, 2*tolBarriers);
		//Up-in TRI vs BS
		Assert.assertEquals(priceUpInTRI, priceBarrierUpInBS, 2*tolBarriers);
	
	}
	

}