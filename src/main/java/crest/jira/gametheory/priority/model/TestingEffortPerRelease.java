package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;
import crest.jira.data.retriever.model.Version;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TestingEffortPerRelease {

  protected static final Integer NEXT_RELEASE = 0;

  private Long developerProductivity;
  private Long testerProductivity;
  private Long releaseInflation;
  private Long releaseNonInflatedSeverity;
  private long releaseReportedNonSeverity;
  private Version release;
  private List<TesterBehaviour> testingResults = null;

  public static Predicate<ExtendedIssue> FIXED_NEXT_RELEASE = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue extendedIssue) {
      return NEXT_RELEASE.equals(extendedIssue.getReleasesToBeFixed());
    }
  };

  public TestingEffortPerRelease() {
    this.testingResults = new ArrayList<>();
  }

  /**
   * Represents the Priority Inflation game for a single release.
   * 
   * @param release
   *          Release.
   * @param reportersPerBoard
   *          Reporters for the current board.
   * @param issuesPerRelease
   *          Issues reported prior to the release.
   */
  public TestingEffortPerRelease(Version release, Set<User> reportersPerBoard,
      List<ExtendedIssue> issuesPerRelease) {
    this.release = release;
    this.testingResults = new ArrayList<>();
    this.developerProductivity = IterableUtils.countMatches(issuesPerRelease, FIXED_NEXT_RELEASE);
    this.releaseInflation = IterableUtils.countMatches(issuesPerRelease, TestReport.INFLATED);
    this.releaseNonInflatedSeverity = IterableUtils.countMatches(issuesPerRelease,
        TestReport.SEVERE_FOUND);
    this.releaseReportedNonSeverity = IterableUtils.countMatches(issuesPerRelease,
        TestReport.NON_SEVERE_REPORTED);
    this.testerProductivity = (long) issuesPerRelease.size();

    for (final User user : reportersPerBoard) {
      Predicate<ExtendedIssue> equalsPredicate = getEqualsPredicate(user);
      List<List<ExtendedIssue>> issuesByUser = IterableUtils.partition(issuesPerRelease,
          equalsPredicate);

      TesterBehaviour testerPlay = new TesterBehaviour(user, this, issuesByUser.get(0));

      if (testerPlay.getTestReport().getIssuesReported() > 0) {
        this.testingResults.add(testerPlay);
      }
    }
  }

  private Predicate<ExtendedIssue> getEqualsPredicate(final User user) {
    return new Predicate<ExtendedIssue>() {
      @Override
      public boolean evaluate(ExtendedIssue issueInCollections) {
        return user.equals(issueInCollections.getIssue().getReporter());
      }
    };
  }

  /**
   * Based on the Tester Behaviors stored, this methods calculates the inflation
   * of a release, the real severity, and the non-severity reported.
   */
  public void calculateReleaseMetrics() {

    this.releaseInflation = 0L;
    this.releaseNonInflatedSeverity = 0L;
    this.releaseReportedNonSeverity = 0;

    for (TesterBehaviour testerBehaviour : this.testingResults) {
      releaseInflation += testerBehaviour.getTestReport().getInflatedReports();
      releaseNonInflatedSeverity += testerBehaviour.getTestReport().getSevereIssuesFound();
      releaseReportedNonSeverity += testerBehaviour.getTestReport().getNonSevereIssuesFound()
          - testerBehaviour.getTestReport().getInflatedReports();
    }
  }

  public Long getDeveloperProductivity() {
    return developerProductivity;
  }

  public void setDeveloperProductivity(Long developerProductivity) {
    this.developerProductivity = developerProductivity;
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

  public List<TesterBehaviour> getTesterBehaviours() {
    return testingResults;
  }

  public Version getRelease() {
    return release;
  }

}
