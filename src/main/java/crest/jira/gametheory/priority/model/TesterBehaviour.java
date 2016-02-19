package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.csv.BaseCsvRecord;
import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.miner.report.model.ExtendedUser;
import crest.jira.gametheory.priority.regression.DataEntry;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.logging.Logger;

public class TesterBehaviour extends BaseCsvRecord implements DataEntry {

  private static Logger logger = Logger.getLogger(TesterBehaviour.class.getName());

  private TestReport testReport;

  // TODO(cgavidia): Evaluate if this simple payoff needs to be modified.
  private Long nextReleaseFixes;
  private Long rejectedIssues;
  private ExtendedUser extendedUser;
  private TestingEffortPerTimeFrame testingEffort;

  /**
   * Percentage of non-severe issues that will be inflated.
   */
  private double inflationRatio;
  private double successRatio;

  private double expectedSevereFixes;
  private double expectedInflatedFixes;
  private double expectedNonSevereFixes;

  private double expectedFixes;

  public TesterBehaviour(TestingEffortPerTimeFrame releaseTestingEffort) {
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
  public TesterBehaviour(ExtendedUser extendedUser, TestingEffortPerTimeFrame testingEffort,
      List<ExtendedIssue> issuesByUser) {
    this.extendedUser = extendedUser;
    this.testingEffort = testingEffort;
    this.testReport = new TestReport(extendedUser.getUser(), issuesByUser,
        testingEffort.getTimeFrame());

    this.nextReleaseFixes = IterableUtils.countMatches(issuesByUser,
        TestingEffortPerTimeFrame.FIXED_NEXT_RELEASE);
    this.rejectedIssues = IterableUtils.countMatches(issuesByUser,
        TestingEffortPerTimeFrame.RELEASE_REJECTIONS);

    this.successRatio = 0.0;
    if (this.testReport.getIssuesReported() != 0) {
      this.successRatio = this.nextReleaseFixes / (double) this.testReport.getIssuesReported();
    }

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

  public Long getRejectedIssues() {
    return rejectedIssues;
  }

  public Long getNextReleaseFixes() {
    return nextReleaseFixes;
  }

  public ExtendedUser getExtendedUser() {
    return extendedUser;
  }

  public TestingEffortPerTimeFrame getRelease() {
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
  public void configureCsvRecord() {

    this.addDataItem(TestingCsvConfiguration.RELEASE, this.testingEffort.getTimeFrame());
    this.addDataItem(TestingCsvConfiguration.TESTER, this.extendedUser.getUser().getName());
    this.addDataItem(TestingCsvConfiguration.TESTER_PARTICIPATION,
        this.extendedUser.getReleaseParticipation());
    this.addDataItem(TestingCsvConfiguration.TESTER_INFLATION_SLOPE,
        this.extendedUser.getRegressionForInflation().getSlope());

    this.addDataItem(TestingCsvConfiguration.DEVELOPER_PRODUCTIVITY,
        this.testingEffort.getDeveloperProductivity());
    this.addDataItem(TestingCsvConfiguration.RELEASE_REJECTION,
        this.testingEffort.getTimeFrameRejection());

    this.addDataItem(TestingCsvConfiguration.TESTER_PRODUCTIVITY,
        this.testingEffort.getTestTeamProductivity());
    this.addDataItem(TestingCsvConfiguration.NUMBER_OF_TESTERS,
        this.testingEffort.getNumberOfTesters());
    this.addDataItem(TestingCsvConfiguration.DEVELOPER_PRODUCTIVITY_RATIO,
        this.testingEffort.getDeveloperProductivityRatio());
    this.addDataItem(TestingCsvConfiguration.RELEASE_INFLATION,
        this.testingEffort.getReleaseInflation());
    this.addDataItem(TestingCsvConfiguration.RELEASE_SEVERITY_RATIO,
        this.testingEffort.getTimeFrameSeverityRatio());
    this.addDataItem(TestingCsvConfiguration.AVG_INFLATION_RATIO,
        this.testingEffort.getAverageForInflationRatio());
    this.addDataItem(TestingCsvConfiguration.MED_INFLATION_RATIO,
        this.testingEffort.getMedianForInflationRatio());
    this.addDataItem(TestingCsvConfiguration.VAR_INFLATION_RATIO,
        this.testingEffort.getVarianceForInflationRatio());

    this.addDataItem(TestingCsvConfiguration.ISSUES_REPORTED, this.testReport.getIssuesReported());

    this.addDataItem(TestingCsvConfiguration.INFLATION_RATIO, this.testReport.getInflationRatio());
    this.addDataItem(TestingCsvConfiguration.SEVERE_RATIO_REPORTED,
        this.testReport.getSevereRatioInReport());
    this.addDataItem(TestingCsvConfiguration.NON_SEVERE_RATIO_REPORTED,
        this.testReport.getNonSevereRationInReport());
    this.addDataItem(TestingCsvConfiguration.SEVERE_RATIO_REPORTED,
        this.testReport.getSevereRatioInReport());

    this.addDataItem(TestingCsvConfiguration.SEVERE_ISSUES_FOUND,
        this.testReport.getSevereIssuesFound());
    this.addDataItem(TestingCsvConfiguration.DEFAULT_ISSUES_FOUND,
        this.testReport.getDefaultIssuesFound());
    this.addDataItem(TestingCsvConfiguration.NON_SEVERE_ISSUES_FOUND,
        this.testReport.getNonSevereIssuesFound());

    this.addDataItem(TestingCsvConfiguration.POSSIBLE_INFLATIONS,
        this.testReport.getInflatedReports());

    this.addDataItem(TestingCsvConfiguration.SEVERE_ISSUES_REPORTED,
        this.testReport.getSevereIssuesReported());
    this.addDataItem(TestingCsvConfiguration.DEFAULT_ISSUES_REPORTED,
        this.testReport.getDefaultIssuesReported());
    this.addDataItem(TestingCsvConfiguration.NON_SEVERE_ISSUES_REPORTED,
        this.testReport.getNonSevereIssuesReported());

    this.addDataItem(TestingCsvConfiguration.EXTERNAL_INFLATION, this.getExternalInflation());
    this.addDataItem(TestingCsvConfiguration.EXTERNAL_SEVERITY, this.getExternalSeverity());

    this.addDataItem(TestingCsvConfiguration.NEXT_RELEASE_FIXES, this.getNextReleaseFixes());
    this.addDataItem(TestingCsvConfiguration.REJECTED_ISSUES, this.getRejectedIssues());
    this.addDataItem(TestingCsvConfiguration.SUCCESS_RATIO, this.getSuccessRatio());
    this.addDataItem(TestingCsvConfiguration.SEVERE_FIX_PROBABILITY,
        this.getFixProbabilityForSevere());
    this.addDataItem(TestingCsvConfiguration.NON_SEVERE_FIX_PROBABILITY,
        this.getFixProbabilityForNonSevere());
    this.addDataItem(TestingCsvConfiguration.EXPECTED_SEVERE_FIXES, this.getExpectedSevereFixes());
    this.addDataItem(TestingCsvConfiguration.EXPECTED_INFLATED_FIXES,
        this.getExpectedInflatedFixes());
    this.addDataItem(TestingCsvConfiguration.EXPECTED_NON_SEVERE_FIXES,
        this.getExpectedNonSevereFixes());
    this.addDataItem(TestingCsvConfiguration.EXPECTED_FIXES, this.getExpectedFixes());
  }

  public double getInflationRatio() {
    return inflationRatio;
  }

  /**
   * Calculates the inflation ratio, only if non-severe issues were found. Also,
   * stores this value in the corresponding User.
   * 
   */
  public void calculateInflatioRatio() {
    this.inflationRatio = 0.0;

    if (this.testReport.getNonSevereIssuesFound() > 0) {
      this.inflationRatio = ((double) this.testReport.getInflatedReports())
          / this.testReport.getNonSevereIssuesFound();
    }

    this.extendedUser.getInflationRatios().put(this.testingEffort.getTimeFrame(),
        this.inflationRatio);
  }

  public void setInflationRatio(double inflationRatio) {
    this.inflationRatio = inflationRatio;
  }

  public double getSuccessRatio() {
    return successRatio;
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
