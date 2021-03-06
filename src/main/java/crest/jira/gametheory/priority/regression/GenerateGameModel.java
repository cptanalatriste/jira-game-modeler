package crest.jira.gametheory.priority.regression;

import crest.jira.data.miner.GenerateConsolidatedCsvFiles;
import crest.jira.data.miner.csv.CsvUtils;
import crest.jira.gametheory.priority.game.EstimatedGame;
import crest.jira.gametheory.priority.game.ReleaseTestStrategyProfile;
import crest.jira.gametheory.priority.model.TesterBehaviour;
import crest.jira.gametheory.priority.model.TestingCsvConfiguration;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class GenerateGameModel {

  private static Logger logger = Logger.getLogger(GenerateGameModel.class.getName());

  private static final String CSV_FILE = GenerateConsolidatedCsvFiles.FOLDER_NAME
      + "Tester_Behaviour_Board_2_1454688181389.csv";

  private static final int MAX_STRATEGY_VALUE = 1;
  private static final int STRATEGY_SUBSET_SIZE = 3;

  // private static final int NUMBER_OF_PLAYERS = 15; This is the minimum value
  // saaw in data.
  private static final int NUMBER_OF_PLAYERS = 10;

  // private static final int SAMPLES_PER_PROFILE = 500;
  private static final int SAMPLES_PER_PROFILE = 100;

  /**
   * Generates a regression model for the payoff function and estimates the
   * game.
   * 
   * @param args
   *          Not used on this program.
   * @throws IOException
   *           File handling is risky
   */
  public static void main(String[] args) throws IOException {
    List<CSVRecord> csvRecords = CsvUtils.getCsvRecords(CSV_FILE);
    generateRegressionModel(csvRecords);
    estimateGame(csvRecords);
  }

  private static void estimateGame(List<CSVRecord> csvRecords) throws IOException {
    EmpiricalDistribution severeFoundDistribution = new EmpiricalDistribution();
    EmpiricalDistribution nonSevereFoundDistribution = new EmpiricalDistribution();
    EmpiricalDistribution devProductivityRatioDistribution = new EmpiricalDistribution();

    loadDistributionData(csvRecords, severeFoundDistribution, nonSevereFoundDistribution,
        devProductivityRatioDistribution);

    EstimatedGame estimatedGame = new EstimatedGame(STRATEGY_SUBSET_SIZE, MAX_STRATEGY_VALUE,
        NUMBER_OF_PLAYERS);

    logger.info("Profiles available " + estimatedGame.getStrategyProfiles().size());

    for (ReleaseTestStrategyProfile strategyProfile : estimatedGame.getStrategyProfiles()) {
      strategyProfile.calculateAveragePayoffs(SAMPLES_PER_PROFILE, severeFoundDistribution,
          nonSevereFoundDistribution, devProductivityRatioDistribution);
      logger.fine(strategyProfile.toString());
    }

    CsvUtils.generateCsvFile(GenerateConsolidatedCsvFiles.FOLDER_NAME, "Estimated_Game",
        estimatedGame.getStrategyProfiles());

  }

  private static void loadDistributionData(List<CSVRecord> csvRecords,
      EmpiricalDistribution severeFoundDistribution,
      EmpiricalDistribution nonSevereFoundDistribution,
      EmpiricalDistribution devProductivityRatioDistribution) {
    ArrayList<Double> severeIssuesData = new ArrayList<>();
    ArrayList<Double> nonSevereIssuesData = new ArrayList<>();
    HashMap<String, Double> devProductivityData = new HashMap<>();

    for (CSVRecord csvRecord : csvRecords) {
      String release = csvRecord.get(TestingCsvConfiguration.RELEASE);
      double severeIssuesFound = Double
          .parseDouble(csvRecord.get(TestingCsvConfiguration.SEVERE_ISSUES_FOUND));
      double nonSevereIssuesFound = Double
          .parseDouble(csvRecord.get(TestingCsvConfiguration.NON_SEVERE_ISSUES_FOUND));
      double devProductityRatio = Double
          .parseDouble(csvRecord.get(TestingCsvConfiguration.DEVELOPER_PRODUCTIVITY_RATIO));

      severeIssuesData.add(severeIssuesFound);
      nonSevereIssuesData.add(nonSevereIssuesFound);
      devProductivityData.put(release, devProductityRatio);

      logger.fine("release " + release + " devProductityRatio " + devProductityRatio);
    }

    severeFoundDistribution.load(
        ArrayUtils.toPrimitive(severeIssuesData.toArray(new Double[severeIssuesData.size()])));
    nonSevereFoundDistribution.load(ArrayUtils
        .toPrimitive(nonSevereIssuesData.toArray(new Double[nonSevereIssuesData.size()])));
    devProductivityRatioDistribution.load(ArrayUtils.toPrimitive(
        devProductivityData.values().toArray(new Double[devProductivityData.values().size()])));
  }

  private static void generateRegressionModel(List<CSVRecord> csvRecords) {
    List<TesterBehaviour> dataEntryRecords = new ArrayList<>();
    for (CSVRecord csvRecord : csvRecords) {
      dataEntryRecords.add(new TesterBehaviour(csvRecord));
    }

    RegressionModel regressionModel = new RegressionModel(dataEntryRecords);
    OLSMultipleLinearRegression regressionInstance = regressionModel.getRegression();

    double[] regressionParameters = regressionInstance.estimateRegressionParameters();
    double rsquared = regressionInstance.calculateRSquared();
    double[][] parametersVariance = regressionInstance.estimateRegressionParametersVariance();

    logger.info("regressionParameters " + Arrays.toString(regressionParameters));
    logger.info("rSquared " + rsquared);
    logger.info("parametersVariance " + Arrays.toString(parametersVariance));

    double customRSquared = regressionModel.calculareRSquared();
    logger.info("customRSquared " + customRSquared);
  }

}
