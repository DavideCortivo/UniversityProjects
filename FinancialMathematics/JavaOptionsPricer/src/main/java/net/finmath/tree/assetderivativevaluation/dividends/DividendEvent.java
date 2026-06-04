package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * This interface extends DividendModel and forces every dividend type we are going to create 
 * to have a precise time reference, which is necessary to create tree models.
 */

public interface DividendEvent extends DividendModel {

 /**
  * @return an array whose elements will be defined in the classes in which this interface is implemented.
  */
double [] getTime();


}
