package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.csv.CsvExportSupport;
import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.miner.report.model.ExtendedUser;
import crest.jira.gametheory.priority.regression.DataEntry;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TesterBehaviour implements CsvExportSupport, DataEntry {

  private static Logger logger = Logger.getLogger(TesterBehaviour.class.getName());

  private TestReport testReport;

  // TODO(cgavidia): Evaluate if this simple payoff needs to be modified.
  private Long nextReleaseFixes;
  private ExtendedUser extendedUser;
  private TestingEffortPerRelease testingEffort;

  /**
   * Percentage of non-severe issues that will be inflated.
   */
  private double inflationRatio;
  private double expectedSevereFixes;
  private double expectedInflatedFixes;
  private double expectedNonSevereFixes;

  private double expectedFixes;

  public TesterBehaviour(TestingEffortPerRelease releaseTestingEffort) {
    this.testReport = new TestReport();
    this.testingEffort = releaseTestingEffort;
  }

  /**
   * Stores the behavior of a Tester on an Specific release.
   * 
   * @param extendedUser
   *          Tester.
   * @param testingEffort
   *          Testing metrics for the release.
   * @param issuesByUser
   *          Issues reported.
   */
  public TesterBehaviour(ExtendedUser extendedUser, TestingEffortPerRelease testingEffort,
      List<ExtendedIssue> issuesByUser) {
    this.extendedUser = extendedUser;
    this.testingEffort = testingEffort;
    this.testReport = new TestReport(extendedUser.getUser(), issuesByUser,
        testingEffort.getRelease());

    this.nextReleaseFixes = IterableUtils.countMatches(issuesByUser,
        TestingEffortPerRelease.FIXED_NEXT_RELEASE);

    // this.calculateRegressionFields();
  }

  /**
   * Get's an instance from a CSV Record.
   * 
   * @param csvRecord
   *          CSV Record.
   */
  public TesterBehaviour(CSVRecord csvRecord) {
    this.expectedInflatedFixes = Double
        .parseDouble(csvRecord.get(TestingCsvConfiguration.EXPECTED_INFLATED_FIXES));
    this.expectedSevereFixes = Double
        .parseDouble(csvRecord.get(TestingCsvConfiguration.EXPECTED_SEVERE_FIXES));
    this.expectedNonSevereFixes = Double
        .parseDouble(csvRecord.get(TestingCsvConfiguration.EXPECTED_NON_SEVERE_FIXES));
    this.nextReleaseFixes = Long
        .parseLong(csvRecord.get(TestingCsvConfiguration.NEXT_RELEASE_FIXES));
    this.expectedFixes = Double.parseDouble(csvRecord.get(TestingCsvConfiguration.EXPECTED_FIXES));
  }

  /**
   * The number of inflated issues must be less or equal than the number of
   * non-severe issues found. In case this situation is detected, this logic
   * will enforce the invariant.
   */
  public void enforceInvariants() {
    if (this.testReport.getInflatedReports() > this.testReport.getNonSevereIssuesFound()) {
      logger.fine("INVARINT VIOLATED! this.testReport.getInflatedReports() "
          + this.testReport.getInflatedReports() + " this.testReport.getNonSevereIssuesFound() "
          + this.testReport.getNonSevereIssuesFound());
      this.testReport.setInflatedReports(this.testReport.getNonSevereIssuesFound());
    }
  }

  /**
   * After all the proper values are being set, the expected number of fixes per
   * release is estimated.
   * 
   * @throws NullPointerException
   *           If this method is called when the TestingEffort reference hasn't
   *           calculated release metrics through a calculateReleaseMetrics()
   *           call.
   */
  public void calculateRegressionFields() throws NullPointerException {
    this.expectedSevereFixes = this.testReport.getSevereIssuesFound()
        * this.getFixProbabilityForSevere();
    this.expectedInflatedFixes = this.testReport.getInflatedReports()
        * this.getFixProbabilityForSevere();
    this.expectedNonSevereFixes = this.testReport.getNonSevereIssuesReported()
        * this.getFixProbabilityForNonSevere();
    this.expectedFixes = this.getExpectedSevereFixes() + this.getExpectedNonSevereFixes()
        + this.getExpectedInflatedFixes();
  }

  public TestReport getTestReport() {
    return testReport;
  }

  public Long getNextReleaseFixes() {
    return nextReleaseFixes;
  }

  public ExtendedUser getUser() {
    return extendedUser;
  }

  public TestingEffortPerRelease getRelease() {
    return testingEffort;
  }

  // TODO(cgavidia): Evaluate if this aggregation method is appropriate. The
  // other implies an explosion in dimensionality.
  public Long getExternalInflation() {
    return this.testingEffort.getReleaseInflation() - this.testReport.getInflatedReports();
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

    if (probability > 1 || releaseReportedSeverity == 0) {
      probability = 1;
    }

    return probability;
  }

  private Double getFixProbabilityForNonSevere() {
    // TODO(cgavidia): Evaluate numerator.
    double productivityRemaining = this.testingEffort.getDeveloperProductivity()
        - this.testingEffort.getReleaseInflation()
        - this.testingEffort.getReleaseNonInflatedSeverity();

    long releaseReportedNonSeverity = this.testingEffort.getReleaseReportedNonSeverity();
    double probability = productivityRemaining / releaseReportedNonSeverity;

    if (probability < 0) {
      return 0.0;
    }

    if (probability > 1 || releaseReportedNonSeverity == 0) {
      return 1.0;
    }

    return probability;
  }

  private Double getExpectedSevereFixes() {
    return expectedSevereFixes;
  }

  private Double getExpectedInflatedFixes() {
    return expectedInflatedFixes;
  }

  private Double getExpectedNonSevereFixes() {
    return expectedNonSevereFixes;
  }

  public Double getExpectedFixes() {
    return expectedFixes;
  }

  // TODO(cgavidia): We need to refactor this cumbersome two-method way of doing
  // this.
  @Override
  public List<Object> getCsvRecord() {
    List<Object> recordAsList = new ArrayList<>();
    recordAsList.add(this.testingEffort.getRelease().getName());
    recordAsList.add(this.extendedUser.getUser().getName());
    recordAsList.add(this.extendedUser.getReleaseParticipation());

    recordAsList.add(this.testingEffort.getDeveloperProductivity());
    recordAsList.add(this.testingEffort.getTestTeamProductivity());
    recordAsList.add(this.testingEffort.getDeveloperProductivityRatio());
    recordAsList.add(this.testingEffort.getReleaseInflation());
    recordAsList.add(this.testingEffort.getAverageForInflationRatio());
    recordAsList.add(this.testingEffort.getMedianForInflationRatio());
    recordAsList.add(this.testingEffort.getVarianceForInflationRatio());

    recordAsList.add(this.testReport.getIssuesReported());
    recordAsList.add(this.testReport.getInflatedReports());
    recordAsList.add(this.testReport.getInflationRatio());
    recordAsList.add(this.testReport.getSevereRationInReport());
    recordAsList.add(this.testReport.getNonSevereRationInReport());

    recordAsList.add(this.testReport.getSevereIssuesFound());
    recordAsList.add(this.testReport.getSevereIssuesReported());
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
    headerAsList.add(TestingCsvConfiguration.TESTER_PARTICIPATION);
    headerAsList.add(TestingCsvConfiguration.DEVELOPER_PRODUCTIVITY);
    headerAsList.add(TestingCsvConfiguration.TESTER_PRODUCTIVITY);
    headerAsList.add(TestingCsvConfiguration.DEVELOPER_PRODUCTIVITY_RATIO);
    headerAsList.add(TestingCsvConfiguration.RELEASE_INFLATION);
    headerAsList.add(TestingCsvConfiguration.AVG_INFLATION_RATIO);
    headerAsList.add(TestingCsvConfiguration.MED_INFLATION_RATIO);
    headerAsList.add(TestingCsvConfiguration.VAR_INFLATION_RATIO);

    headerAsList.add(TestingCsvConfiguration.ISSUES_REPORTED);
    headerAsList.add(TestingCsvConfiguration.POSSIBLE_INFLATIONS);
    headerAsList.add(TestingCsvConfiguration.INFLATION_RATIO);
    headerAsList.add(TestingCsvConfiguration.SEVERE_RATIO_REPORTED);
    headerAsList.add(TestingCsvConfiguration.NON_SEVERE_RATIO_REPORTED);

    headerAsList.add(TestingCsvConfiguration.SEVERE_ISSUES_FOUND);
    headerAsList.add(TestingCsvConfiguration.SEVERE_ISSUES_REPORTED);
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

  public double getInflationRatio() {
    return inflationRatio;
  }

  /**
   * Calculates the inflation ratio, only if non-severe issues were found.
   * 
   */
  public void calculateInflatioRatio() {
    this.inflationRatio = 0.0;

    if (this.testReport.getNonSevereIssuesFound() > 0) {
      this.inflationRatio = ((double) this.testReport.getInflatedReports())
          / this.testReport.getNonSevereIssuesFound();
    }
  }

  public void setInflationRatio(double inflationRatio) {
    this.inflationRatio = inflationRatio;
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

  @Override
  public double getExpectedRegresandValue() {
    return this.expectedFixes;
  }

}
