package crest.jira.gametheory.priority.miner;

import com.j256.ormlite.support.ConnectionSource;

import crest.jira.data.miner.config.ConfigurationProvider;
import crest.jira.data.miner.db.JiraIssueListDao;
import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.miner.report.model.ReleaseDateComparator;
import crest.jira.data.retriever.model.User;
import crest.jira.data.retriever.model.Version;
import crest.jira.gametheory.priority.model.PriorityInflationStageGame;

import org.apache.commons.collections4.MultiValuedMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class GenerateGameCsvFile {

  private static Logger logger = Logger.getLogger(GenerateGameCsvFile.class.getName());

  private static final String BOARD_ID = "2";
  private static final boolean ONLY_BUGS = true;

  /**
   * Explores the JIRA Issue's database to generate a CSV file of Priority
   * Inflation game instances.
   * 
   * @param args
   *          Not used at all.
   * @throws SQLException
   *           In case of DB problems.
   */
  public static void main(String[] args) throws SQLException {
    ConfigurationProvider configProvider = new ConfigurationProvider();
    ConnectionSource connectionSource = configProvider.getConnectionSource();

    processBoard(connectionSource, BOARD_ID);
  }

  private static void processBoard(ConnectionSource connectionSource, String boardId)
      throws SQLException {
    JiraIssueListDao issueListDao = new JiraIssueListDao(connectionSource);
    issueListDao.loadBoardIssues(boardId, ONLY_BUGS);

    MultiValuedMap<Version, ExtendedIssue> issuesInReleases = issueListDao.organizeInReleases();
    Set<User> reportersPerBoard = issueListDao.getReporterCatalogPerBoard(boardId);
    logger.info("reportersPerBoard.size()" + reportersPerBoard.size());

    List<PriorityInflationStageGame> stageGames = getStageGames(issuesInReleases,
        reportersPerBoard);

    logger.info("stageGames.size()" + stageGames.size());
  }

  private static List<PriorityInflationStageGame> getStageGames(
      MultiValuedMap<Version, ExtendedIssue> issuesInReleases, Set<User> reportersPerBoard) {

    List<PriorityInflationStageGame> stageGames = new ArrayList<PriorityInflationStageGame>();
    List<Version> releases = new ArrayList<>(issuesInReleases.keySet());
    Collections.sort(releases, new ReleaseDateComparator());

    for (Version release : releases) {
      List<ExtendedIssue> issuesPerRelease = (List<ExtendedIssue>) issuesInReleases.get(release);
      PriorityInflationStageGame stageGame = new PriorityInflationStageGame(release,
          reportersPerBoard, issuesPerRelease);
      stageGames.add(stageGame);
    }

    return stageGames;
  }

}
