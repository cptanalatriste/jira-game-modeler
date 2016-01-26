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

  /**
   * Returns the proportion of defects detected by this tester for a release.
   * 
   * @return Tester Productivity.
   */
  public Double getProductivityRatio() {
    double issuesReported = this.testReport.getIssuesReported();
    double testTeamProductivity = this.testingEffort.getTestTeamProductivity();
    return issuesReported / testTeamProductivity;
  }

  /**
   * Returns the proportion of inflated issues related to the entire release
   * report.
   * 
   * @return Inflation Ratio.
   */
  public Double getInflationRatio() {
    double possibleInflations = this.testReport.getPossibleInflations();
    double issuesReported = this.testReport.getIssuesReported();
    return possibleInflations / issuesReported;
  }

  /**
   * Returns the proportion of inflated issues by the team, excluding this
   * tester.
   * 
   * @return External Inflation Ratio.
   */
  public Double getExternalInflationRatio() {
    double externalInflation = this.getExternalInflation();
    double testTeamProductivity = this.testingEffort.getTestTeamProductivity()
        - this.testReport.getIssuesReported();
    return externalInflation / testTeamProductivity;
  }

  /**
   * Returns the proportion of real severe issues that were found by the user.
   * 
   * @return Severity Ratio.
   */
  public Double getSeverityRatio() {
    double severeIssues = this.testReport.getSevereIssues();
    double issuesReported = this.testReport.getIssuesReported();
    return severeIssues / issuesReported;
  }

  /**
   * Return the proportion of issues reported that got fixed on the next
   * release.
   * 
   * @return Success Ratio.
   */
  public Double getSuccessRatio() {
    double success = this.getNextReleaseFixes();
    double issuesReported = this.testReport.getIssuesReported();
    return success / issuesReported;
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
    return this.testingEffort.getReleaseSeverity() + this.testReport.getSevereIssues();
  }

  private Double getExpectedFixes() {
    return this.getExpectedSevereFixes() + this.getExpectedNonSevereFixes()
        + this.getExpectedInflatedFixes();
  }

  private Double getExpectedNonSevereFixes() {
    return this.testReport.getNonSevereIssues() * this.getFixProbabilityForNonSevere();
  }

  private Double getExpectedInflatedFixes() {
    return this.testReport.getPossibleInflations() * this.getFixProbabilityForSevere();
  }

  private Double getExpectedSevereFixes() {
    return this.testReport.getSevereIssues() * this.getFixProbabilityForSevere();
  }

  private Double getFixProbabilityForNonSevere() {

    double releaseNonSeverity = this.testingEffort.getReleaseNonSeverity();
    double probability = (this.testingEffort.getDeveloperProductivity()
        - this.testingEffort.getReleaseInflation() - this.testingEffort.getReleaseSeverity())
        / releaseNonSeverity;

    if (probability < 0) {
      return 0.0;
    }

    if (probability > 1) {
      return 1.1;
    }

    return probability;
  }

  private Double getFixProbabilityForSevere() {
    double developerProductivity = this.testingEffort.getDeveloperProductivity();
    double probability = developerProductivity
        / (this.testingEffort.getReleaseInflation() + this.testingEffort.getReleaseSeverity());

    if (probability > 1) {
      probability = 1;
    }

    return probability;
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
    recordAsList.add(this.testReport.getSevereIssues());
    recordAsList.add(this.testReport.getNonSevereIssues());

    recordAsList.add(this.getExternalInflation());
    recordAsList.add(this.getExternalSeverity());

    recordAsList.add(this.testReport.getSevereIssues() * this.testReport.getPossibleInflations());
    recordAsList.add(Math.pow(this.testReport.getPossibleInflations(), 2));
    recordAsList.add(this.getProductivityRatio());
    recordAsList.add(this.getInflationRatio());
    recordAsList.add(this.getExternalInflationRatio());
    recordAsList.add(this.getSeverityRatio());

    recordAsList.add(this.getNextReleaseFixes());
    recordAsList.add(this.getSuccessRatio());
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
    headerAsList.add(TestingCsvConfiguration.NON_SEVERE_ISSUES);

    headerAsList.add(TestingCsvConfiguration.EXTERNAL_INFLATION);
    headerAsList.add(TestingCsvConfiguration.EXTERNAL_SEVERITY);

    headerAsList.add(TestingCsvConfiguration.SEVERE_TIMES_INFLATION);
    headerAsList.add(TestingCsvConfiguration.INFLATION_SQUARED);
    headerAsList.add(TestingCsvConfiguration.PRODUCTIVITY_RATIO);
    headerAsList.add(TestingCsvConfiguration.INFLATION_RATIO);
    headerAsList.add(TestingCsvConfiguration.EXTERNAL_INFLATION_RATIO);
    headerAsList.add(TestingCsvConfiguration.SEVERITY_RATIO);

    headerAsList.add(TestingCsvConfiguration.NEXT_RELEASE_FIXES);
    headerAsList.add(TestingCsvConfiguration.SUCCESS_RATIO);
    headerAsList.add(TestingCsvConfiguration.SEVERE_FIX_PROBABILITY);
    headerAsList.add(TestingCsvConfiguration.NON_SEVERE_FIX_PROBABILITY);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_SEVERE_FIXES);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_INFLATED_FIXES);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_NON_SEVERE_FIXES);
    headerAsList.add(TestingCsvConfiguration.EXPECTED_FIXES);

    return headerAsList.toArray(new String[headerAsList.size()]);
  }

}
