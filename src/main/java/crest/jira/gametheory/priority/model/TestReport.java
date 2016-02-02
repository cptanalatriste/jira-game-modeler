package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.util.List;

public class TestReport {

  private long issuesReported = 0;
  private long possibleInflations = 0;
  private long severeIssuesFound = 0;
  private long nonSevereIssuesReported = 0;
  private long nonSevereIssuesFound = 0;
  private Double inflationRatio = 0.0;

  public static Predicate<ExtendedIssue> INFLATED = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isProbablyAnInflation();
    }
  };

  public static Predicate<ExtendedIssue> SEVERE_FOUND = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isReportedSevere() && !issue.isProbablyAnInflation();
    }
  };

  public static Predicate<ExtendedIssue> NON_SEVERE_REPORTED = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isReportedNonSevere() || issue.isReportedDefault();
    }
  };

  public static Predicate<ExtendedIssue> NON_SEVERE_FOUND = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isReportedNonSevere() || issue.isReportedDefault()
          || issue.isProbablyAnInflation();
    }
  };

  public TestReport() {
  }

  /**
   * Represents the behavior of a Tester for an specific release.
   * 
   * @param user
   *          User, corresponding to a Tester.
   * @param issuesByUser
   *          Issues reported previous to a release.
   */
  public TestReport(User user, List<ExtendedIssue> issuesByUser) {
    this.issuesReported = issuesByUser.size();
    this.severeIssuesFound = IterableUtils.countMatches(issuesByUser, SEVERE_FOUND);
    this.nonSevereIssuesFound = IterableUtils.countMatches(issuesByUser, NON_SEVERE_FOUND);
    this.nonSevereIssuesReported = IterableUtils.countMatches(issuesByUser, NON_SEVERE_REPORTED);
    this.possibleInflations = IterableUtils.countMatches(issuesByUser, INFLATED);

    if (nonSevereIssuesFound != 0) {
      this.inflationRatio = possibleInflations / (double) nonSevereIssuesFound;
    }
  }

  /**
   * Configures the metrics for a test report.
   * 
   * @param severeIssuesFound
   *          Severe Issues Found.
   * @param nonSevereIssuesFound
   *          Non-Severe Issues Found.
   * @param inflationRatio
   *          Inflation Ratio.
   */
  public void configureReport(double severeIssuesFound, double nonSevereIssuesFound,
      double inflationRatio) {
    double issuesReported = severeIssuesFound + nonSevereIssuesFound;
    double inflatedReports = nonSevereIssuesFound * inflationRatio;
    long nonSevereIssuesReported = (long) (nonSevereIssuesFound - inflatedReports);

    this.setSevereIssuesFound((long) severeIssuesFound);
    this.setNonSevereIssuesFound((long) nonSevereIssuesFound);
    this.setIssuesReported((long) issuesReported);
    this.setInflatedReports((long) inflatedReports);
    this.setNonSevereIssuesReported(nonSevereIssuesReported);

  }

  public long getIssuesReported() {
    return issuesReported;
  }

  public void setIssuesReported(long issuesReported) {
    this.issuesReported = issuesReported;
  }

  public long getSevereIssuesFound() {
    return severeIssuesFound;
  }

  public long getNonSevereIssuesFound() {
    return nonSevereIssuesFound;
  }

  public void setNonSevereIssuesFound(long nonSevereIssuesFound) {
    this.nonSevereIssuesFound = nonSevereIssuesFound;
  }

  public long getNonSevereIssuesReported() {
    return nonSevereIssuesReported;
  }

  public void setNonSevereIssuesReported(long nonSevereIssuesReported) {
    this.nonSevereIssuesReported = nonSevereIssuesReported;
  }

  public long getInflatedReports() {
    return possibleInflations;
  }

  public void setInflatedReports(long possibleInflations) {
    this.possibleInflations = possibleInflations;
  }

  public void setSevereIssuesFound(long severeIssuesFound) {
    this.severeIssuesFound = severeIssuesFound;
  }

  public Double getInflationRatio() {
    return this.inflationRatio;
  }

}
