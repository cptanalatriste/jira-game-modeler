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

  private List<TesterPlay> testerPlays = null;

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
    this.testerPlays = new ArrayList<>();

    for (final User user : reportersPerBoard) {
      Predicate<ExtendedIssue> equalsPredicate = getEqualsPredicate(user);
      List<List<ExtendedIssue>> issuesByUser = IterableUtils.partition(issuesPerRelease,
          equalsPredicate);

      TesterPlay testerPlay = new TesterPlay(user, issuesByUser.get(0));

      if (testerPlay.getIssuesReported() > 0) {
        this.testerPlays.add(testerPlay);
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

}
