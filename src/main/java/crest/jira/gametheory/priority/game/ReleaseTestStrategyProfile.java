package crest.jira.gametheory.priority.game;

import crest.jira.gametheory.priority.model.TesterBehaviour;
import crest.jira.gametheory.priority.model.TestingEffortPerRelease;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.List;

public class ReleaseTestStrategyProfile {

  private List<SummaryStatistics> payoffStatistics;
  private TestingEffortPerRelease releaseTestingEffort;

  /**
   * Initializes a Strategy Profile by setting player actions.
   * 
   * @param playerStrategies
   *          Array of strategies.
   */
  public ReleaseTestStrategyProfile(double[] playerStrategies) {
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
   * @param devProductivityDistribution
   *          Distribution for developer productivity.
   */
  public void calculateAveragePayoffs(int samplesPerProfile,
      EmpiricalDistribution severeFoundDistribution,
      EmpiricalDistribution nonSevereFoundDistribution,
      EmpiricalDistribution devProductivityDistribution) {

    for (int index = 0; index < samplesPerProfile; index += 1) {
      // Assigning the stochastic data necessary for payoff calculations.
      long developerProductivity = (long) devProductivityDistribution.getNextValue();
      this.releaseTestingEffort.setDeveloperProductivity(developerProductivity);

      for (TesterBehaviour testerBehaviour : releaseTestingEffort.getTesterBehaviours()) {
        double severeIssuesFound = severeFoundDistribution.getNextValue();
        double nonSevereIssuesFound = nonSevereFoundDistribution.getNextValue();
        double inflatedReports = nonSevereIssuesFound * testerBehaviour.getInflationRatio();
        long nonSevereIssuesReported = (long) (nonSevereIssuesFound - inflatedReports);

        testerBehaviour.getTestReport().setSevereIssuesFound((long) severeIssuesFound);
        testerBehaviour.getTestReport().setNonSevereIssuesFound((long) nonSevereIssuesFound);
        testerBehaviour.getTestReport().setInflatedReports((long) inflatedReports);
        testerBehaviour.getTestReport().setNonSevereIssuesReported(nonSevereIssuesReported);
      }

      // Performing payoff calculations.
      this.releaseTestingEffort.calculateReleaseMetrics();

      for (int playerIndex = 0; playerIndex < releaseTestingEffort.getTesterBehaviours()
          .size(); playerIndex += 1) {
        TesterBehaviour testerBehaviour = releaseTestingEffort.getTesterBehaviours()
            .get(playerIndex);
        // TODO(cgavidia): Review the invariant enforcement
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

    for (int playerIndex = 0; playerIndex < payoffStatistics.size(); playerIndex += 1) {
      TesterBehaviour testerBehaviour = this.releaseTestingEffort.getTesterBehaviours()
          .get(playerIndex);
      SummaryStatistics payoffStatistic = this.payoffStatistics.get(playerIndex);

      strategyAsString += "PLAYER " + playerIndex;
      strategyAsString += " Strategy " + testerBehaviour.getInflationRatio();
      strategyAsString += " Payoff " + payoffStatistic.getMean();
      strategyAsString += "\t";
    }

    return strategyAsString;
  }

}
