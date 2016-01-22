package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;
import crest.jira.data.retriever.model.Version;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PriorityInflationStageGame {

  protected static final Integer NEXT_RELEASE = 0;

  private Long developerProductivity;
  private Long testerProductivity;

  private List<TesterBehaviour> testingResults = null;

  public static Predicate<ExtendedIssue> FIXED_NEXT_RELEASE = new Predicate<ExtendedIssue>() {
    @Override
    public boolean evaluate(ExtendedIssue extendedIssue) {
      return NEXT_RELEASE.equals(extendedIssue.getReleasesToBeFixed());
    }
  };

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
  public PriorityInflationStageGame(Version release, Set<User> reportersPerBoard,
      List<ExtendedIssue> issuesPerRelease) {
    this.testingResults = new ArrayList<>();
    this.developerProductivity = IterableUtils.countMatches(issuesPerRelease, FIXED_NEXT_RELEASE);
    this.testerProductivity = (long) issuesPerRelease.size();

    for (final User user : reportersPerBoard) {
      Predicate<ExtendedIssue> equalsPredicate = getEqualsPredicate(user);
      List<List<ExtendedIssue>> issuesByUser = IterableUtils.partition(issuesPerRelease,
          equalsPredicate);

      TesterBehaviour testerPlay = new TesterBehaviour(user, release, issuesByUser.get(0));

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

  public Long getDeveloperProductivity() {
    return developerProductivity;
  }

  public Long getTesterProductivity() {
    return testerProductivity;
  }

  public List<TesterBehaviour> getTestingResults() {
    return testingResults;
  }
}
