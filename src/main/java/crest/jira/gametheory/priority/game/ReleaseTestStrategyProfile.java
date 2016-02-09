package crest.jira.gametheory.priority.game;

import crest.jira.data.miner.csv.CsvExportSupport;
import crest.jira.gametheory.priority.model.TesterBehaviour;
import crest.jira.gametheory.priority.model.TestingCsvConfiguration;
import crest.jira.gametheory.priority.model.TestingEffortPerRelease;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ReleaseTestStrategyProfile implements CsvExportSupport {

  private static Logger logger = Logger.getLogger(ReleaseTestStrategyProfile.class.getName());
  private static final String PAYOFF_NOT_READY = "NOT READY";

  private List<SummaryStatistics> payoffStatistics;
  private TestingEffortPerRelease releaseTestingEffort;
  private int numberOfPlayers;
  private List<Double> strategySubset;

  /**
   * Initializes a Strategy Profile by setting player actions.
   * 
   * @param strategySubset
   *          Strategies available
   * @param playerStrategies
   *          Array of strategies.
   */
  public ReleaseTestStrategyProfile(Double[] strategySubset, double[] playerStrategies) {
    this.numberOfPlayers = playerStrategies.length;
    this.strategySubset = Arrays.asList(strategySubset);
    this.payoffStatistics = new ArrayList<>();
    this.releaseTestingEffort = new TestingEffortPerRelease();

    for (double playerStrategy : playerStrategies) {
      this.payoffStatistics.add(new SummaryStatistics());

      TesterBehaviour testerBehaviour = new TesterBehaviour(releaseTestingEffort);
      testerBehaviour.setInflationRatio(playerStrategy);

      releaseTestingEffort.getTesterBehaviours().add(testerBehaviour);
    }
  }

  public List<TesterBehaviour> getTesterBehaviours() {
    return releaseTestingEffort.getTesterBehaviours();
  }

  /**
   * Calculates the sample payoffs for this profile by repeating for a number of
   * samples.
   * 
   * @param samplesPerProfile
   *          Number of samples.
   * @param severeFoundDistribution
   *          Distribution for severe issues.
   * @param nonSevereFoundDistribution
   *          Distribution for non-severe issues.
   * @param devProductivityRatioDistribution
   *          Distribution for developer productivity.
   */
  public void calculateAveragePayoffs(int samplesPerProfile,
      EmpiricalDistribution severeFoundDistribution,
      EmpiricalDistribution nonSevereFoundDistribution,
      EmpiricalDistribution devProductivityRatioDistribution) {

    logger.fine("Calculating payoff for profile " + this.toString());
    logger.fine("Executing " + samplesPerProfile + " samples");


    for (int sampleIndex = 0; sampleIndex < samplesPerProfile; sampleIndex += 1) {
      // Assigning the stochastic data necessary for payoff calculations.
      double devProductivityRatio = devProductivityRatioDistribution.getNextValue();
      this.releaseTestingEffort.setDeveloperProductivityRatio(devProductivityRatio);

      for (TesterBehaviour testerBehaviour : releaseTestingEffort.getTesterBehaviours()) {
        // TODO(cgavidia): The distributions are based on a high-productivity
        // team with a high-number of testers.
        // The simplifications might overstate this.
        double severeIssuesFound = severeFoundDistribution.getNextValue();
        double nonSevereIssuesFound = nonSevereFoundDistribution.getNextValue();

        testerBehaviour.getTestReport().configureReport(severeIssuesFound, nonSevereIssuesFound,
            testerBehaviour.getInflationRatio());
      }

      // Performing payoff calculations.
      this.releaseTestingEffort.updateReleaseMetrics();

      for (int playerIndex = 0; playerIndex < releaseTestingEffort.getTesterBehaviours()
          .size(); playerIndex += 1) {
        TesterBehaviour testerBehaviour = releaseTestingEffort.getTesterBehaviours()
            .get(playerIndex);
        // TODO(cgavidia): Review 5the invariant enforcement
        // testerBehaviour.enforceInvariants();
        testerBehaviour.calculateRegressionFields();

        double payoff = testerBehaviour.getExpectedFixes();
        payoffStatistics.get(playerIndex).addValue(payoff);
      }
    }
  }

  @Override
  public String toString() {
    String strategyAsString = "";

    for (int playerIndex = 0; playerIndex < numberOfPlayers; playerIndex += 1) {
      TesterBehaviour testerBehaviour = this.releaseTestingEffort.getTesterBehaviours()
          .get(playerIndex);
      SummaryStatistics payoffStatistic = this.payoffStatistics.get(playerIndex);

      strategyAsString += "PLAYER " + playerIndex;
      strategyAsString += " Strategy " + testerBehaviour.getInflationRatio();

      String averagePayoff = PAYOFF_NOT_READY;
      if (payoffStatistic.getN() > 0) {
        averagePayoff = Double.toString(payoffStatistic.getMean());
      }
      strategyAsString += " Payoff " + averagePayoff;
      strategyAsString += "\t";

    }

    return strategyAsString;
  }

  @Override
  public List<Object> getCsvRecord() {
    List<Double> strategyValueList = new ArrayList<>();
    List<Integer> strategyIndexList = new ArrayList<>();
    List<Double> payoffValueList = new ArrayList<>();

    List<Object> recordAsList = new ArrayList<>();

    for (int playerIndex = 0; playerIndex < this.numberOfPlayers; playerIndex += 1) {
      TesterBehaviour testerBehaviour = this.releaseTestingEffort.getTesterBehaviours()
          .get(playerIndex);
      SummaryStatistics payoffStatistic = this.payoffStatistics.get(playerIndex);

      double strategyValue = testerBehaviour.getInflationRatio();
      int strategyIndex = strategySubset.indexOf(strategyValue);
      double payoffValue = payoffStatistic.getMean();

      strategyValueList.add(strategyValue);
      strategyIndexList.add(strategyIndex);
      payoffValueList.add(payoffValue);
    }

    recordAsList.addAll(strategyIndexList);
    recordAsList.addAll(payoffValueList);
    recordAsList.addAll(strategyValueList);

    return recordAsList;
  }

  @Override
  public String[] getCsvHeader() {
    List<String> headerAsList = new ArrayList<String>();
    for (int playerIndex = 0; playerIndex < this.numberOfPlayers; playerIndex += 1) {
      headerAsList.add(TestingCsvConfiguration.STRATEGY_INDEX + playerIndex);
    }

    for (int playerIndex = 0; playerIndex < this.numberOfPlayers; playerIndex += 1) {
      headerAsList.add(TestingCsvConfiguration.PAYOFF_VALUE + playerIndex);
    }

    for (int playerIndex = 0; playerIndex < this.numberOfPlayers; playerIndex += 1) {
      headerAsList.add(TestingCsvConfiguration.STRATEGY_VALUE + playerIndex);
    }

    return headerAsList.toArray(new String[headerAsList.size()]);

  }

}
