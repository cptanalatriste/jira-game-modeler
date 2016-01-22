package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;
import crest.jira.data.retriever.model.Version;

import org.apache.commons.collections4.IterableUtils;

import java.util.List;

public class TesterBehaviour {

  private TestReport testReport;
  private Long nextReleaseFixes;
  private User user;
  private Version release;

  /**
   * Stores the behavior of a Tester on an Specific release.
   * 
   * @param user
   *          Tester.
   * @param release
   *          Release.
   * @param issuesByUser
   *          Issues reported.
   */
  public TesterBehaviour(User user, Version release, List<ExtendedIssue> issuesByUser) {
    this.user = user;
    this.release = release;
    this.testReport = new TestReport(user, issuesByUser);
    this.nextReleaseFixes = IterableUtils.countMatches(issuesByUser,
        PriorityInflationStageGame.FIXED_NEXT_RELEASE);
  }

  public TestReport getTestReport() {
    return testReport;
  }

  public Long getNextReleaseFixes() {
    return nextReleaseFixes;
  }

  public User getUser() {
    return user;
  }

  public Version getRelease() {
    return release;
  }

}
