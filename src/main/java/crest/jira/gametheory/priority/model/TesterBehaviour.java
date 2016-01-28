package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.csv.CsvExportSupport;
import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;
import crest.jira.gametheory.priority.regression.DataEntry;

import org.apache.commons.collections4.IterableUtils;

import java.util.ArrayList;
import java.util.List;

public class TesterBehaviour implements CsvExportSupport, DataEntry {

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

  public TestingEffortPerRelease getRelease() {
    return testingEffort;
  }

  // TODO(cgavidia): Evaluate if this aggregation method is appropriate. The
  // other implies an explosion in dimensionality.
  public Long getExternalInflation() {
    return this.testingEffort.getReleaseInflation() - this.testReport.getPossibleInflations();
  }

  public Long getExternalSeverity() {
    return this.testingEffort.getReleaseNonInflatedSeverity()
        + this.testReport.getSevereIssuesFound();
  }

  private Double getFixProbabilityForSevere() {
    double developerProductivity = this.testingEffort.getDeveloperProductivity();
    long releaseReportedSeverity = this.testingEffort.getReleaseInflation()
        + this.testingEffort.getReleaseNonInflatedSeverity();

    double probability = developerProductivity / releaseReportedSeverity;

    if (probability > 1) {
      probability = 1;
    }

    return probability;
  }

  private Double getFixProbabilityForNonSevere() {

    double productivityRemaining = this.testingEffort.getDeveloperProductivity()
        - this.testingEffort.getReleaseInflation()
        - this.testingEffort.getReleaseNonInflatedSeverity();

    double probability = productivityRemaining / this.testingEffort.getReleaseReportedNonSeverity();

    if (probability < 0) {
      return 0.0;
    }

    if (probability > 1) {
      return 1.1;
    }

    return probability;
  }

  private Double getExpectedSevereFixes() {
    return this.testReport.getSevereIssuesFound() * this.getFixProbabilityForSevere();
  }

  private Double getExpectedInflatedFixes() {
    return this.testReport.getPossibleInflations() * this.getFixProbabilityForSevere();
  }

  private Double getExpectedNonSevereFixes() {
    return this.testReport.getNonSevereIssuesReported() * this.getFixProbabilityForNonSevere();
  }

  private Double getExpectedFixes() {
    return this.getExpectedSevereFixes() + this.getExpectedNonSevereFixes()
        + this.getExpectedInflatedFixes();
  }

  @Override
  public List<Object> getCsvRecord() {
    List<Object> recordAsList = new ArrayList<>();
    recordAsList.add(this.testingEffort.getRelease().getName());
    recordAsList.add(this.user.getName());

    recordAsList.add(this.testingEffort.getDeveloperProductivity());
    recordAsList.add(this.testingEffort.getTestTeamProductivity());
    recordAsList.add(this.testingEffort.getReleaseInflation());

    recordAsList.add(this.testReport.getIssuesReported());
    recordAsList.add(this.testReport.getPossibleInflations());
    recordAsList.add(this.testReport.getSevereIssuesFound());
    recordAsList.add(this.testReport.getNonSevereIssuesReported());
    recordAsList.add(this.testReport.getNonSevereIssuesFound());

    recordAsList.add(this.getExternalInflation());
    recordAsList.add(this.getExternalSeverity());

    recordAsList.add(this.getNextReleaseFixes());
    recordAsList.add(this.getFixProbabilityForSevere());
    recordAsList.add(this.getFixProbabilityForNonSevere());
    recordAsList.add(this.getExpectedSevereFixes());
    recordAsList.add(this.getExpectedInflatedFixes());
    recordAsList.add(this.getExpectedNonSevereFixes());
    recordAsList.add(this.getExpectedFixes());

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
    headerAsList.add(TestingCsvConfiguration.NON_SEVERE_ISSUES_REPORTED);
    headerAsList.add(TestingCsvConfiguration.NON_SEVERE_ISSUES_FOUND);

    headerAsList.add(TestingCsvConfiguration.EXTERNAL_INFLATION);
    headerAsList.add(TestingCsvConfiguration.EXTERNAL_SEVERITY);

    headerAsList.add(TestingCsvConfiguration.NEXT_RELEASE_FIXES);
    headerAsList.add(TestingCsvConfiguration.SEVERE_FIX_PROBABILITY);
    headerAsList.add(TestingCsvConfiguration.NON_SEVERE_FIX_PROBABILITY);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_SEVERE_FIXES);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_INFLATED_FIXES);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_NON_SEVERE_FIXES);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_FIXES);

    return headerAsList.toArray(new String[headerAsList.size()]);
  }

  @Override
  public double getRegressandValue() {
    return this.nextReleaseFixes;
  }

  @Override
  public double[] getRegressorValue() {
    return new double[] { this.getExpectedInflatedFixes(), this.getExpectedSevereFixes(),
        this.getExpectedNonSevereFixes() };
  }
}
