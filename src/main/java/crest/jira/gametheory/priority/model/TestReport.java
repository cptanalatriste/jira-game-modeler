package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.util.List;

public class TestReport {

  private long issuesReported = 0;
  private long possibleInflations = 0;
  private long severeIssues = 0;
  private long nonSevereIssues = 0;
  private long defaultIssues = 0;

  private static Predicate<ExtendedIssue> INFLATED = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isProbablyAnInflation();
    }
  };

  private static Predicate<ExtendedIssue> SEVERE = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isSevere();
    }
  };

  private static Predicate<ExtendedIssue> NON_SEVERE = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isNonSevere();
    }
  };

  private static Predicate<ExtendedIssue> DEFAULT = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue issue) {
      return issue.isDefault();
    }
  };

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
    this.severeIssues = IterableUtils.countMatches(issuesByUser, SEVERE);
    this.nonSevereIssues = IterableUtils.countMatches(issuesByUser, NON_SEVERE);
    this.defaultIssues = IterableUtils.countMatches(issuesByUser, DEFAULT);
    this.possibleInflations = IterableUtils.countMatches(issuesByUser, INFLATED);
  }

  public long getIssuesReported() {
    return issuesReported;
  }

  public long getSevereIssues() {
    return severeIssues;
  }

  public long getNonSevereIssues() {
    return nonSevereIssues;
  }

  public long getDefaultIssues() {
    return defaultIssues;
  }

  public long getPossibleInflations() {
    return possibleInflations;
  }
}
