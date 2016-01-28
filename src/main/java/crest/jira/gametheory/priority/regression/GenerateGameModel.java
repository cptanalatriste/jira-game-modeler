package crest.jira.gametheory.priority.regression;

import crest.jira.data.miner.GenerateConsolidatedCsvFiles;
import crest.jira.data.miner.csv.CsvUtils;
import crest.jira.gametheory.priority.model.TestingCsvConfiguration;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class GenerateGameModel {

  private static Logger logger = Logger.getLogger(GenerateGameModel.class.getName());

  private static final String CSV_FILE = GenerateConsolidatedCsvFiles.FOLDER_NAME
      + "Tester_Behaviour_Board_2_1453842786993.csv";

  public static void main(String[] args) {
    List<CSVRecord> csvRecords = CsvUtils.getCsvRecords(CSV_FILE);
    generateRegressionModel(csvRecords);
  }

  private static void generateRegressionModel(List<CSVRecord> csvRecords) {
    List<DataEntry> dataEntryRecords = new ArrayList<>();
    for (CSVRecord csvRecord : csvRecords) {
      dataEntryRecords.add(getDataEntryFromCsv(csvRecord));
    }

    RegressionModel regressionModel = new RegressionModel(dataEntryRecords);
    OLSMultipleLinearRegression regressionInstance = regressionModel.getRegression();

    double[] regressionParameters = regressionInstance.estimateRegressionParameters();
    double rsquared = regressionInstance.calculateRSquared();
    double[][] parametersVariance = regressionInstance.estimateRegressionParametersVariance();

    logger.info("regressionParameters " + Arrays.toString(regressionParameters));
    logger.info("rSquared " + rsquared);
    logger.info("parametersVariance " + Arrays.toString(parametersVariance));
    
  }

  // TODO(cgavidia): Move to a constructor for Test Report
  private static DataEntry getDataEntryFromCsv(CSVRecord csvRecord) {
    return new DataEntry() {

      @Override
      public double[] getRegressorValue() {
        double expectedInflatedFixes = Double
            .parseDouble(csvRecord.get(TestingCsvConfiguration.EXPECTED_INFLATED_FIXES));
        double expectedSevereFixes = Double
            .parseDouble(csvRecord.get(TestingCsvConfiguration.EXPECTED_SEVERE_FIXES));
        double expectedNonSevereFixes = Double
            .parseDouble(csvRecord.get(TestingCsvConfiguration.EXPECTED_NON_SEVERE_FIXES));

        return new double[] { expectedInflatedFixes, expectedSevereFixes, expectedNonSevereFixes };
      }

      @Override
      public double getRegressandValue() {
        return Double.parseDouble(csvRecord.get(TestingCsvConfiguration.NEXT_RELEASE_FIXES));
      }
    };
  }

}
