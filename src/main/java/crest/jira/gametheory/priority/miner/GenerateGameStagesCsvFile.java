package crest.jira.gametheory.priority.miner;

import com.j256.ormlite.support.ConnectionSource;

import crest.jira.data.miner.GenerateConsolidatedCsvFiles;
import crest.jira.data.miner.config.ConfigurationProvider;
import crest.jira.data.miner.csv.BaseCsvGenerator;
import crest.jira.data.miner.db.JiraIssueListDao;
import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.miner.report.model.ReleaseDateComparator;
import crest.jira.data.retriever.model.User;
import crest.jira.data.retriever.model.Version;
import crest.jira.gametheory.priority.model.TesterBehaviour;
import crest.jira.gametheory.priority.model.TestingEffortPerRelease;

import org.apache.commons.collections4.MultiValuedMap;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class GenerateGameStagesCsvFile extends BaseCsvGenerator {

  public GenerateGameStagesCsvFile() {
    // TODO(cgavidia): This is wrong. Fix later.
    super(GenerateConsolidatedCsvFiles.FOLDER_NAME);
  }

  private static Logger logger = Logger.getLogger(GenerateGameStagesCsvFile.class.getName());

  private static final String BOARD_ID = "2";
  private static final boolean ONLY_BUGS = true;
  private static final String TESTER_BEHAVIOUR = "Tester_Behaviour_Board_" + BOARD_ID;

  /**
   * Explores the JIRA Issue's database to generate a CSV file of Priority
   * Inflation game instances.
   * 
   * @param args
   *          Not used at all.
   * @throws SQLException
   *           In case of DB problems.
   * @throws IOException
   *           Problems might arise.
   */
  public static void main(String[] args) throws SQLException, IOException {
    ConfigurationProvider configProvider = new ConfigurationProvider();
    ConnectionSource connectionSource = configProvider.getConnectionSource();

    GenerateGameStagesCsvFile generator = new GenerateGameStagesCsvFile();

    generator.processBoard(connectionSource, BOARD_ID);
  }

  private void processBoard(ConnectionSource connectionSource, String boardId)
      throws SQLException, IOException {
    JiraIssueListDao issueListDao = new JiraIssueListDao(connectionSource);
    issueListDao.loadBoardIssues(boardId, ONLY_BUGS);

    MultiValuedMap<Version, ExtendedIssue> issuesInReleases = issueListDao.organizeInReleases();
    Set<User> reportersPerBoard = issueListDao.getReporterCatalogPerBoard(boardId);
    logger.info("reportersPerBoard.size()" + reportersPerBoard.size());

    List<TesterBehaviour> testerPlays = getStageGames(issuesInReleases, reportersPerBoard);

    logger.info("testerPlays.size()" + testerPlays.size());
    generateCsvFile(TESTER_BEHAVIOUR, testerPlays);
  }

  private static List<TesterBehaviour> getStageGames(
      MultiValuedMap<Version, ExtendedIssue> issuesInReleases, Set<User> reportersPerBoard) {

    List<TesterBehaviour> testerBehaviours = new ArrayList<TesterBehaviour>();
    List<Version> releases = new ArrayList<>(issuesInReleases.keySet());
    Collections.sort(releases, new ReleaseDateComparator());

    for (Version release : releases) {
      List<ExtendedIssue> issuesPerRelease = (List<ExtendedIssue>) issuesInReleases.get(release);
      TestingEffortPerRelease stageGame = new TestingEffortPerRelease(release, reportersPerBoard,
          issuesPerRelease);
      testerBehaviours.addAll(stageGame.getTesterBehaviours());
    }

    return testerBehaviours;
  }

}