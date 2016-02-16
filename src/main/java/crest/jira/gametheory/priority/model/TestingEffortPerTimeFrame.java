package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.miner.report.model.ExtendedUser;

import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class TestingEffortPerTimeFrame {

  private static Logger logger = Logger.getLogger(TestingEffortPerTimeFrame.class.getName());
  protected static final Integer NEXT_RELEASE = 0;
  public static final int MINIMUM_INVOLVEMENT = 3;

  private Long developerProductivity;
  private Long releaseRejection;
  private Long testerProductivity;
  private Double developerProductivityRatio;
  private Long releaseInflation;
  private Long releaseNonInflatedSeverity;
  private long releaseReportedNonSeverity;
  private double averageForInflationRatio;
  private double medianForInflationRatio;
  private double varianceForInflationRatio;
  private double releaseSeverityRatio = 0.0;

  private String timeFrame;
  private List<TesterBehaviour> testingResults = null;

  public static Predicate<ExtendedIssue> FIXED_NEXT_RELEASE = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue extendedIssue) {
      return NEXT_RELEASE.equals(extendedIssue.getReleasesToBeFixed());
    }
  };

  public static final Predicate<ExtendedIssue> RELEASE_REJECTIONS = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue extendedIssue) {
      return extendedIssue.getIssue().getResolution() != null
          && !extendedIssue.isAcceptedByDevTeam();
    }
  };

  public TestingEffortPerTimeFrame() {
    this.testingResults = new ArrayList<>();
  }

  /**
   * Represents the Priority Inflation game for a single release.
   * 
   * @param timeFrame
   *          Release.
   * @param reportersPerBoard
   *          Reporters for the current board.
   * @param totalIssuesPerRelease
   *          Issues reported prior to the release.
   */
  public TestingEffortPerTimeFrame(String timeFrame, Set<ExtendedUser> reportersPerBoard,
      List<ExtendedIssue> totalIssuesPerRelease) {

    this.timeFrame = timeFrame;
    this.testingResults = new ArrayList<>();
    List<ExtendedIssue> filteredIssuesPerRelease = new ArrayList<>();

    for (final ExtendedUser extendedUser : reportersPerBoard) {
      Predicate<ExtendedIssue> equalsPredicate = getEqualsPredicate(extendedUser);
      List<List<ExtendedIssue>> issuesByUser = IterableUtils.partition(totalIssuesPerRelease,
          equalsPredicate);

      List<ExtendedIssue> issueListByUser = issuesByUser.get(0);
      TesterBehaviour testerPlay = new TesterBehaviour(extendedUser, this, issueListByUser);

      // We're only interested in the issues reported by testers involved in the
      // project.
      if (testerPlay.getTestReport().getIssuesReported() > MINIMUM_INVOLVEMENT) {
        this.testingResults.add(testerPlay);
        extendedUser.reportParticipation();

        filteredIssuesPerRelease.addAll(issueListByUser);
      }
    }

    calculateReleaseMetrics(filteredIssuesPerRelease);
  }

  private void calculateReleaseMetrics(List<ExtendedIssue> issuesForAnalysis) {

    this.developerProductivity = IterableUtils.countMatches(issuesForAnalysis, FIXED_NEXT_RELEASE);
    this.releaseRejection = IterableUtils.countMatches(issuesForAnalysis, RELEASE_REJECTIONS);
    this.testerProductivity = (long) issuesForAnalysis.size();
    this.developerProductivityRatio = this.developerProductivity
        / ((double) this.testerProductivity);
    this.releaseInflation = IterableUtils.countMatches(issuesForAnalysis, TestReport.INFLATED);
    this.releaseNonInflatedSeverity = IterableUtils.countMatches(issuesForAnalysis,
        TestReport.SEVERE_FOUND);
    this.releaseReportedNonSeverity = IterableUtils.countMatches(issuesForAnalysis,
        TestReport.NON_SEVERE_REPORTED);

    if (this.testerProductivity > 0) {
      this.releaseSeverityRatio = this.releaseNonInflatedSeverity
          / (double) this.testerProductivity;
    }

    IterableUtils.forEach(this.testingResults, new Closure<TesterBehaviour>() {

      @Override
      public void execute(TesterBehaviour testerBehaviour) {
        testerBehaviour.calculateRegressionFields();
      }
    });
  }

  private Predicate<ExtendedIssue> getEqualsPredicate(final ExtendedUser extendedUser) {
    return new Predicate<ExtendedIssue>() {
      @Override
      public boolean evaluate(ExtendedIssue issueInCollections) {
        return extendedUser.getUser().equals(issueInCollections.getIssue().getReporter());
      }
    };
  }

  /**
   * Calculates consolidated inflation information with respect to this release.
   */
  public void calculateInflationRatioMetrics() {
    this.averageForInflationRatio = 0;
    this.varianceForInflationRatio = 0;

    DescriptiveStatistics inflationRatioStats = new DescriptiveStatistics();

    for (TesterBehaviour testerBehaviour : this.testingResults) {
      testerBehaviour.calculateInflatioRatio();
      inflationRatioStats.addValue(testerBehaviour.getInflationRatio());
    }

    this.averageForInflationRatio = inflationRatioStats.getMean();
    this.varianceForInflationRatio = inflationRatioStats.getVariance();
    this.medianForInflationRatio = inflationRatioStats.getPercentile(50);

    logger.fine("this.testingResults.size() " + this.testingResults.size());
    logger.fine("this.averageForInflationRatio " + this.averageForInflationRatio);
    logger.fine("this.varianceForInflationRatio " + this.varianceForInflationRatio);

  }

  /**
   * Based on the Tester Behaviors stored, this methods calculates the inflation
   * of a release, the real severity, and the non-severity reported.
   */
  public void updateReleaseMetrics() {

    this.releaseInflation = 0L;
    this.releaseNonInflatedSeverity = 0L;
    this.releaseReportedNonSeverity = 0;
    this.testerProductivity = 0L;

    for (TesterBehaviour testerBehaviour : this.testingResults) {
      releaseInflation += testerBehaviour.getTestReport().getInflatedReports();
      releaseNonInflatedSeverity += testerBehaviour.getTestReport().getSevereIssuesFound();
      releaseReportedNonSeverity += testerBehaviour.getTestReport().getNonSevereIssuesFound()
          - testerBehaviour.getTestReport().getInflatedReports();
      testerProductivity += testerBehaviour.getTestReport().getIssuesReported();
    }

    this.developerProductivity = (long) (this.testerProductivity * this.developerProductivityRatio);
    logger.fine("this.testerProductivity " + this.testerProductivity
        + " this.developerProductivityRatio " + this.developerProductivityRatio
        + " this.developerProductivity " + this.developerProductivity);
  }

  public Long getDeveloperProductivity() {
    return developerProductivity;
  }

  public void setDeveloperProductivity(Long developerProductivity) {
    this.developerProductivity = developerProductivity;
  }

  public Double getDeveloperProductivityRatio() {
    return this.developerProductivityRatio;
  }

  public void setDeveloperProductivityRatio(Double developerProductivityRatio) {
    this.developerProductivityRatio = developerProductivityRatio;
  }

  public Long getReleaseInflation() {
    return releaseInflation;
  }

  public Long getReleaseNonInflatedSeverity() {
    return releaseNonInflatedSeverity;
  }

  public long getReleaseReportedNonSeverity() {
    return releaseReportedNonSeverity;
  }

  public Long getTestTeamProductivity() {
    return testerProductivity;
  }

  public double getAverageForInflationRatio() {
    return averageForInflationRatio;
  }

  public double getVarianceForInflationRatio() {
    return varianceForInflationRatio;
  }

  public double getMedianForInflationRatio() {
    return medianForInflationRatio;
  }

  public double getReleaseSeverityRatio() {
    return releaseSeverityRatio;
  }

  public Long getReleaseRejection() {
    return releaseRejection;
  }

  public List<TesterBehaviour> getTesterBehaviours() {
    return testingResults;
  }

  public String getTimeFrame() {
    return timeFrame;
  }

}
