package crest.jira.gametheory.priority.regression;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.SecondMoment;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.List;

public class RegressionModel {

  private double[] regressandValues;
  private double[] expectedRegressandValues;
  private double[][] regressorValues;
  private OLSMultipleLinearRegression regression;

  /**
   * Creates a regression instance and adds the dataset as samples.
   * 
   * @param dataSet
   *          Data set for regression.
   */
  public RegressionModel(List<? extends DataEntry> dataSet) {
    this.regression = new OLSMultipleLinearRegression();
    processDataSet(dataSet);

    regression.newSampleData(regressandValues, regressorValues);
  }

  private void processDataSet(List<? extends DataEntry> dataSet) {
    int numberOfRecords = dataSet.size();
    regressandValues = new double[numberOfRecords];
    expectedRegressandValues = new double[numberOfRecords];
    regressorValues = new double[numberOfRecords][];

    for (int index = 0; index < numberOfRecords; index += 1) {
      DataEntry dataPoint = dataSet.get(index);
      regressandValues[index] = dataPoint.getRegressandValue();
      expectedRegressandValues[index] = dataPoint.getExpectedRegresandValue();
      regressorValues[index] = dataPoint.getRegressorValue();
    }
  }

  public OLSMultipleLinearRegression getRegression() {
    return regression;
  }

  /**
   * Calculate the r-squared for a list of predicted values.
   * 
   * @return r2-score
   */
  public double calculareRSquared() {
    RealVector predictedY = new ArrayRealVector(this.expectedRegressandValues);
    return 1 - calculateResidualSumOfSquares(predictedY) / calculateTotalSumOfSquares();

  }

  private double calculateResidualSumOfSquares(RealVector predictedY) {
    RealVector residuals = calculateResiduals(predictedY);
    return residuals.dotProduct(residuals);
  }

  private RealVector calculateResiduals(RealVector predictedY) {
    RealVector yvector = new ArrayRealVector(regressandValues);
    return yvector.subtract(predictedY);
  }

  private double calculateTotalSumOfSquares() {
    return new SecondMoment().evaluate(regressandValues);
  }

}
