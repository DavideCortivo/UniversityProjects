package net.finmath.tree.assetderivativevaluation.products;

import java.util.function.DoubleUnaryOperator;

import it.univr.fima.barrieroptionsformulas.BarrierOptions.BarrierType;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.TreeModel;

public class BarrierOptionsProducts extends AbstractNonPathDependentProduct {

	private final double strike;
	private final CallOrPut callOrPut;
	private final double barrier; 
	private final BarrierType barrierType;
	private  double rebate; 


	/**
	 * This is the constructor for the barriers which do not include a rebate, hence do not provide the investor with an amount of money nor if the in-option is not activated 
	 * neither if the out-barrier is breached
	 * @param maturity: the last time T in the time discretization 0=t_0<t_1<..<t_n=T
	 * @param strike: the price of the underlying which has been defined during the negotiation
	 * @param barrier: the predefined price level of the underlying asset that, if reached, determines the activation (knock-in) or the termination (knock-out) of the option  
	 * @param callOrPut: the type of option 
	 * @param barrierType: the type of barrier (up or down)
	 */
	public BarrierOptionsProducts(double maturity, double strike, double barrier, CallOrPut callOrPut, BarrierType barrierType) {
		super(maturity);
		this.strike=strike;
		this.barrier=barrier;
		this.callOrPut=callOrPut;
		this.barrierType=barrierType;
	}
	/**
	 * This is the constructor for the barriers which include the rebate 
	 * @param maturity: the last time T in the time discretization 0=t_0<t_1<..<t_n=T
	 * @param strike: the price of the underlying which has been defined during the negotiation
	 * @param barrier: the predefined price level of the underlying asset that, if reached, determines the activation (knock-in) or the termination (knock-out) of the option  
	 * @param rebate: the predetermined value that is given to the investor if the knock-in option is not activated or if the knock-out barrier is breached 
	 * @param callOrPut: the type of option 
	 * @param barrierType: the type of barrier (up or down)
	 */
	public BarrierOptionsProducts(double maturity, double strike, double barrier, double rebate, CallOrPut callOrPut, BarrierType barrierType) {
		super(maturity);
		this.strike=strike;
		this.barrier=barrier;
		this.rebate=rebate;
		this.callOrPut=callOrPut;
		this.barrierType=barrierType;
	}

	/**
	 * We have inherited this method from the class AbstractNonPathDependentProduct. This method produces the payoff function, which will be used in the following computations. 
	 * The variable s represents the stock price, which is necessary to compute the payoff.
	 */
	@Override
	public DoubleUnaryOperator getPayOffFunction() {
		return s -> {
			/* if the option is a Call, the payoff is computed as the difference between the initial price and the strike */
			if (callOrPut == CallOrPut.CALL) {
				return Math.max(s-strike, 0.0);
			} else {
				/* otherwise (if the option is a Put), the payoff is computed as the difference between the strike and the initial price. */
				return Math.max(strike-s, 0.0);
			}
		};
	}

	/**
	 * We have inherited this method from the class AbstractNonPathDependentProduct. This method is fundamental to route in and out barriers and associate them to the specific method 
	 * for the price computation. 
	 * @return one of the two methods for the computation of the price, according to the barrier type.
	 * If we have a in-barrier, the program calls the method getValuesViaInOutParity; otherwise, the program calls the method getValuesForOutOption. Both methods 
	 * will be specified in the following code lines. 
	 */
	@Override
	public RandomVariable[] getValues(double evaluationTime, TreeModel model) {
		validate (evaluationTime, model);

		if(isInOption()) {
			return getValuesViaInOutParity (evaluationTime, model);
		}
		return getValuesForOutOption(evaluationTime, model);
	}

	/**
	 * This is a boolean method.
	 * @return TRUE if we have an in-barrier (down-in or up-in).
	 */
	private boolean isInOption() {
		return barrierType== BarrierType.DOWN_IN || barrierType == BarrierType.UP_IN;
	}

	/**
	 * This method checks if the spot price has hit the barrier in the case of a down-out barrier.  
	 * @param spot, the price of the underlying
	 * @return TRUE if the price of the underlying remains below the barrier; FALSE, if the underlying price goes above the barrier.
	 */
	private boolean isKnockedOut(double spot) {
		if (barrierType == BarrierType.DOWN_OUT) {
			return spot <= barrier;
		} else {
			return spot >= barrier; 
		}
	}

	/**
	 * This method is a stochastic process (array of random variables). Every index of the array corresponds to the number of random variables which describes the 
	 * dynamics of the in-barrier price. This method therefore allows us to compute the price of the in-barrier.
	 * allows us to compute the price of the in-barrier 
	 * @param evaluationTime: the moment at which valuation is performed.
	 * @param model: the tree model providing the time step.
	 * @return inValues: the in-barrier prices at a specific point of time using the formulas of the In-And-Out Parity 
	 */
	private RandomVariable[] getValuesViaInOutParity(final double evaluationTime, final TreeModel model) {
		/* We have created the object european to call the method getValues, which allows us to compute the values of the European option, that will be implemented 
		 * in the formulas of the In-And-Out Parity.
		 */
		EuropeanNonPathDependent european = new EuropeanNonPathDependent(getMaturity(), getPayOffFunction());
		RandomVariable [] europeanValues = european.getValues(evaluationTime, model); 


		/* Given a in barrier, we can define the corresponding out-barrier. */
		BarrierType outType; //down out type
		if(this.barrierType==BarrierType.DOWN_IN) {
			outType = BarrierType.DOWN_OUT;
		} else {
			outType = BarrierType.UP_OUT;
		}

		/* 
		 * We have created an object of this specific concrete class. This option has no rebate in order to compute the value of the in-barrier using the In-And-Out Parity.
		 * We have defined the value of the out-barrier without rebate as π - R. If the price of the underlying touches the barrier, the payoff corresponds to R; otherwise, if
		 * it does not touch it, we have that the payoff is S - K. 
		 * For example, considering the case of a Call option, if the underlying price touches the barrier, the value of the out-barrier without rebate is R - R = 0: as a consequence, 
		 * according to the In-And-Out Parity, the in-barrier value is equal to the value of the European option (Veur) - 0, hence to Veur. Otherwise, if the underlying 
		 * price does not touch the barrier, the value of the out-barrier is St - K - R: hence, according to the In-And-Out Parity, the in-barrier value (Vin) is equal to: 
		 * Vin = St - K - (St - K - R) = R.
		 */
		BarrierOptionsProducts outOptionNoRebate = new BarrierOptionsProducts(getMaturity(), strike, barrier, 0.0, callOrPut, outType) {

			@Override
			public DoubleUnaryOperator getPayOffFunction() {
				return s -> BarrierOptionsProducts.this.getPayOffFunction().applyAsDouble(s) - rebate;
			}

		};

		/*
		 * We now use the object we have created to call the initial method of this class. Since the object refers to an out-barrier, the method route us to 
		 * the method getValuesForOutOpt, which we will define in the following lines. This allows us the array of the random variables which refers to the values of the 
		 * out-barriers.
		 */
		RandomVariable [] outValues = outOptionNoRebate.getValues(evaluationTime, model); 

		/*
		 * We now create the new stochastic process of the values of the in-barrier. We can compute these values through the difference between the values of the European option
		 * and the out-barrier we have just computed in the process before. The for loop allows the computation through the reiteration of the In-And-Out Parity formula.
		 */
		RandomVariable [] inValues = new RandomVariable [europeanValues.length];
		for(int i=0; i<europeanValues.length; i++) {
			inValues[i] = europeanValues[i].sub(outValues[i]);
		}
		return inValues; 		
	}

	/**
	 * This method allows us to obtain the out-barrier price, which are necessary to compute the in-barrier prices and to price the out-barriers themselves.
	 * @param evaluationTime: the moment at which valuation is performed.
	 * @param model: the tree model providing the time step.
	 * @return levels: the out-barrier prices 
	 */
	private RandomVariable[] getValuesForOutOption(final double evaluationTime, final TreeModel model) {
		final int k0 = timeToIndex(evaluationTime, model);
		final int n  = model.getNumberOfTimes() - 1;
		final RandomVariable[] levels = new RandomVariable[n - k0 + 1];

		/*
		 * This method allows us to define the generic payoff the out-barrier. We have used a ternary operator which describes the two cases: if the underlying is knocked out, 
		 * then the payoff corresponds to the rebate R; otherwise, the payoff is computed via the specific payoff function (for the Call or for the Put).
		 */
		DoubleUnaryOperator payOffWithBarrier  = s -> isKnockedOut(s) ? rebate : getPayOffFunction().applyAsDouble(s);

		/* This method fills the array with the payoffs, considering the function for their computation we have defined (getPayOffFunction). */
		levels[n - k0] = model.getTransformedValuesAtGivenTimeRV(model.getLastTime(), payOffWithBarrier);   


		for (int k = n - 1; k >= k0; --k) {
			final RandomVariable continuation = model.getConditionalExpectationRV(levels[(k + 1) - k0], k);
			final RandomVariable spotK   = model.getSpotAtGivenTimeIndexRV(k);

			double[] contArray = continuation.getRealizations(); /* array of conditional expected values */
			double[] spotArray = spotK.getRealizations(); /* array of the underlying prices */
			double[] currentLevel = new double[spotArray.length]; /* array of option prices at time k */

			for(int i=0; i< spotArray.length; i++) {
				/*
				 * If the underlying price corresponding to the i element of the array spotArray touches the barrier, the i_th-option price of the array currentLevel correspond to
				 * the rebate; otherwise, if the underlying price does not touch the barrier, the i_th-option price corresponds to the i_th-conditional expected value.
				 */
				if(isKnockedOut(spotArray[i])) {
					currentLevel [i]=rebate;	
				} else {
					currentLevel [i] = contArray[i];
				}

			}


			levels[k - k0] = new RandomVariableFromDoubleArray(k * model.getTimeStep(), currentLevel); /* here we have added the array of option prices. */

		}


		return levels;

	}
}
