package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.csv.CsvExportSupport;
import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;

import org.apache.commons.collections4.IterableUtils;

import java.util.ArrayList;
import java.util.List;

public class TesterBehaviour implements CsvExportSupport {

  private TestReport testReport;

  // TODO(cgavidia): Evaluate if this simple payoff needs to be modified.
  private Long nextReleaseFixes;
  private User user;
  private TestingEffortPerRelease testingEffort;

  /**
   * Stores the behavior of a Tester on an Specific release.
   * 
   * @param user
   *          Tester.
   * @param testingEffort
   *          Testing metrics for the release.
   * @param issuesByUser
   *          Issues reported.
   */
  public TesterBehaviour(User user, TestingEffortPerRelease testingEffort,
      List<ExtendedIssue> issuesByUser) {
    this.user = user;
    this.testingEffort = testingEffort;
    this.testReport = new TestReport(user, issuesByUser);
    this.nextReleaseFixes = IterableUtils.countMatches(issuesByUser,
        TestingEffortPerRelease.FIXED_NEXT_RELEASE);
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

  // TODO(cgavidia): Evaluate if this aggregation method is appropriate. The
  // other implies an explosion in dimensionality.
  public TestingEffortPerRelease getRelease() {
    return testingEffort;
  }

  public Long getExternalInflation() {
    return this.testingEffort.getReleaseInflation() - this.testReport.getPossibleInflations();
  }

  @Override
  public List<Object> getCsvRecord() {
    List<Object> recordAsList = new ArrayList<>();
    recordAsList.add(this.testingEffort.getRelease().getName());
    recordAsList.add(this.user.getName());

    recordAsList.add(this.testingEffort.getDeveloperProductivity());
    recordAsList.add(this.testingEffort.getTesterProductivity());
    recordAsList.add(this.testingEffort.getReleaseInflation());

    recordAsList.add(this.testReport.getIssuesReported());
    recordAsList.add(this.testReport.getPossibleInflations());
    recordAsList.add(this.testReport.getSevereIssues());
    recordAsList.add(this.testReport.getDefaultIssues());
    recordAsList.add(this.testReport.getNonSevereIssues());

    recordAsList.add(this.getExternalInflation());

    recordAsList.add(this.getNextReleaseFixes());
    return recordAsList;
  }

  @Override
  public String[] getCsvHeader() {
    List<String> headerAsList = new ArrayList<String>();

    headerAsList.add(TestingCsvConfiguration.RELEASE);
    headerAsList.add(TestingCsvConfiguration.TESTER);
    headerAsList.add(TestingCsvConfiguration.DEVELOPER_PRODUCTIVITY);
    headerAsList.add(TestingCsvConfiguration.TESTER_PRODUCTIVITY);
    headerAsList.add(TestingCsvConfiguration.RELEASE_INFLATION);

    headerAsList.add(TestingCsvConfiguration.ISSUES_REPORTED);
    headerAsList.add(TestingCsvConfiguration.POSSIBLE_INFLATIONS);
    headerAsList.add(TestingCsvConfiguration.SEVERE_ISSUES);
    headerAsList.add(TestingCsvConfiguration.DEFAULT_ISSUES);
    headerAsList.add(TestingCsvConfiguration.NON_SEVERE_ISSUES);

    headerAsList.add(TestingCsvConfiguration.EXTERNAL_INFLATION);

    headerAsList.add(TestingCsvConfiguration.NEXT_RELEASE_FIXES);
    return headerAsList.toArray(new String[headerAsList.size()]);
  }

}
