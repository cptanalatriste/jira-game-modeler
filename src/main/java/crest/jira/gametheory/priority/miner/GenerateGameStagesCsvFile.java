package crest.jira.gametheory.priority.miner;

import com.j256.ormlite.support.ConnectionSource;

import crest.jira.data.miner.GenerateConsolidatedCsvFiles;
import crest.jira.data.miner.config.ConfigurationProvider;
import crest.jira.data.miner.csv.BaseCsvGenerator;
import crest.jira.data.miner.db.JiraIssueListDao;
import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.miner.report.model.ExtendedUser;
import crest.jira.gametheory.priority.model.TesterBehaviour;
import crest.jira.gametheory.priority.model.TestingEffortPerTimeFrame;

import org.apache.commons.collections4.MultiValuedMap;

import java.awt.Toolkit;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class GenerateGameStagesCsvFile extends BaseCsvGenerator {

  private static final String TIME_FRAME_FOR_2015 = "2015";

  private static Logger logger = Logger.getLogger(GenerateGameStagesCsvFile.class.getName());

  private static final String BOARD_ID = "2";
  private static final boolean ONLY_BUGS = true;
  private static final String TESTER_BEHAVIOUR = "Tester_Behaviour_Board_" + BOARD_ID;

  public GenerateGameStagesCsvFile() {
    // TODO(cgavidia): This is wrong. Fix later.
    super(GenerateConsolidatedCsvFiles.FOLDER_NAME);
  }

  /**
   * Explores the JIRA Issue's database to generate a CSV file of Priority
   * Inflation game instances.
   * 
   * @param args
   *          Not used at all.
   */
  public static void main(String[] args) {
    try {
      ConfigurationProvider configProvider = new ConfigurationProvider();
      ConnectionSource connectionSource = configProvider.getConnectionSource();

      GenerateGameStagesCsvFile generator = new GenerateGameStagesCsvFile();
      generator.processBoard(connectionSource, BOARD_ID);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Toolkit.getDefaultToolkit().beep();
  }

  private void processBoard(ConnectionSource connectionSource, String boardId)
      throws SQLException, IOException {
    JiraIssueListDao issueListDao = new JiraIssueListDao(connectionSource);
    issueListDao.loadBoardIssues(boardId, ONLY_BUGS);

    MultiValuedMap<String, ExtendedIssue> issuesPerMonth = issueListDao.organizeInTimeFrames();
    Set<ExtendedUser> reportersPerBoard = issueListDao.getReporterCatalogPerBoard(boardId);
    logger.info("reportersPerBoard.size() " + reportersPerBoard.size());

    List<TesterBehaviour> testerPlays = getStageGames(issuesPerMonth, reportersPerBoard);
    logger.info(" " + testerPlays.size());

    generateCsvFile(TESTER_BEHAVIOUR, testerPlays);
  }

  private static List<TesterBehaviour> getStageGames(
      MultiValuedMap<String, ExtendedIssue> issuesPerMonth, Set<ExtendedUser> reportersPerBoard) {

    List<TesterBehaviour> testerBehaviours = new ArrayList<TesterBehaviour>();
    List<String> months = new ArrayList<>(issuesPerMonth.keySet());
    Collections.sort(months);

    for (int monthIndex = 0; monthIndex < months.size(); monthIndex += 1) {
      String month = months.get(monthIndex);

      if (includeTimeFrame(month)) {
        List<ExtendedIssue> issuesPerRelease = (List<ExtendedIssue>) issuesPerMonth.get(month);
        TestingEffortPerTimeFrame stageGame = new TestingEffortPerTimeFrame(month,
            reportersPerBoard, issuesPerRelease);
        testerBehaviours.addAll(stageGame.getTesterBehaviours());
        stageGame.calculateInflationRatioMetrics();
      }

    }
    return testerBehaviours;
  }

  private static boolean includeTimeFrame(String month) {
    return !month.startsWith(TIME_FRAME_FOR_2015);
  }

}
