package crest.jira.gametheory.priority.regression;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.List;

public class RegressionModel {

  private double[] regressandValues;
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
    regressorValues = new double[numberOfRecords][];

    for (int index = 0; index < numberOfRecords; index += 1) {
      DataEntry dataPoint = dataSet.get(index);
      regressandValues[index] = dataPoint.getRegressandValue();
      regressorValues[index] = dataPoint.getRegressorValue();
    }
  }

  public OLSMultipleLinearRegression getRegression() {
    return regression;
  }

}
