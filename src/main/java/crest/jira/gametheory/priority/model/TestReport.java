package crest.jira.gametheory.priority.model;

import crest.jira.data.miner.report.model.ExtendedIssue;
import crest.jira.data.retriever.model.User;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.util.List;
import java.util.logging.Logger;

public class TestReport {

    private static Logger logger = Logger.getLogger(TestReport.class.getName());

    public static Predicate<ExtendedIssue> INFLATED = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue issue) {
            return issue.isInflated();
        }
    };


    public static Predicate<ExtendedIssue> SEVERE_REPORTED = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue issue) {
            return issue.isReportedSevere();
        }
    };

    public static Predicate<ExtendedIssue> DEFAULT_REPORTED = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue issue) {
            return issue.isReportedDefault();
        }
    };

    public static Predicate<ExtendedIssue> NON_SEVERE_REPORTED = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue issue) {
            return issue.isReportedNonSevere();
        }
    };


    public static Predicate<ExtendedIssue> SEVERE_FOUND = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue issue) {
            return issue.isReportedSevere() &&
                    !issue.isInflated();
        }
    };


    public static Predicate<ExtendedIssue> DEFAULT_FOUND = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue extendedIssue) {
            boolean trueReport = extendedIssue.isReportedDefault() && !extendedIssue.isInflated();
            boolean inflatedReport = extendedIssue.isDefaultInflation();

            return trueReport || inflatedReport;
        }
    };

    public static Predicate<ExtendedIssue> DEFAULT_INFLATED = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue extendedIssue) {
            return extendedIssue.isDefaultInflation();
        }
    };

    public static Predicate<ExtendedIssue> NON_SEVERE_FOUND = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue extendedIssue) {
            boolean trueReports = extendedIssue.isReportedNonSevere();
            boolean inflatedReport = extendedIssue.isInflated() && !extendedIssue.isDefaultInflation();
            return trueReports || inflatedReport;
        }
    };

    public static Predicate<ExtendedIssue> NON_SEVERE_INFLATED = new Predicate<ExtendedIssue>() {
        @Override
        public boolean evaluate(ExtendedIssue extendedIssue) {
            return extendedIssue.isInflated() && !extendedIssue.isDefaultInflation();
        }
    };


    private long issuesReported = 0;
    private long inflatedReports = 0;
    private long defaultIssuesInflated = 0;
    private long nonSevereIssuesInflated = 0;

    private long severeIssuesFound = 0;
    private long nonSevereIssuesFound = 0;
    private long defaultIssuesFound = 0;
    private long nonSevereIssuesReported = 0;
    private long severeIssuesReported = 0;
    private long defaultIssuesReported = 0;
    private Double inflationRatio = 0.0;
    private Double severeRationInReport = 0.0;
    private Double nonSevereRationInReport = 0.0;
    private String timeFrame;

    public TestReport() {
    }

    /**
     * Represents the behavior of a Tester for an specific release.
     *
     * @param user         User, corresponding to a Tester.
     * @param issuesByUser Issues reported previous to a release.
     * @param timeFrame    Related release.
     */
    public TestReport(User user, List<ExtendedIssue> issuesByUser, String timeFrame) {
        this.timeFrame = timeFrame;
        this.issuesReported = issuesByUser.size();

        if (this.issuesReported > TestingEffortPerTimeFrame.MINIMUM_INVOLVEMENT) {
            logger.fine("User " + user.getName() + "is contributing release " + timeFrame + " with "
                    + this.issuesReported + " reports.");
        }

        this.severeIssuesFound = IterableUtils.countMatches(issuesByUser, SEVERE_FOUND);
        this.nonSevereIssuesFound = IterableUtils.countMatches(issuesByUser, NON_SEVERE_FOUND);
        this.defaultIssuesFound = IterableUtils.countMatches(issuesByUser, DEFAULT_FOUND);

        this.nonSevereIssuesReported = IterableUtils.countMatches(issuesByUser, NON_SEVERE_REPORTED);
        this.defaultIssuesReported = IterableUtils.countMatches(issuesByUser, DEFAULT_REPORTED);
        this.severeIssuesReported = IterableUtils.countMatches(issuesByUser, SEVERE_REPORTED);

        this.inflatedReports = IterableUtils.countMatches(issuesByUser, INFLATED);
        this.defaultIssuesInflated = IterableUtils.countMatches(issuesByUser, DEFAULT_INFLATED);
        this.nonSevereIssuesInflated = IterableUtils.countMatches(issuesByUser, NON_SEVERE_INFLATED);

        if (nonSevereIssuesFound != 0) {
            this.inflationRatio = inflatedReports / (double) (nonSevereIssuesFound + defaultIssuesFound);
        }

        if (this.issuesReported > 0) {
            this.severeRationInReport = this.severeIssuesReported / (double) this.issuesReported;
            this.nonSevereRationInReport = this.nonSevereIssuesReported / (double) this.issuesReported;
        }

        this.validateCalculations();
    }

    /**
     * Verifies that the test report is consistent.
     */
    private void validateCalculations() {
        long totalFound = this.severeIssuesFound + this.nonSevereIssuesFound + this.defaultIssuesFound;
        if (totalFound != this.issuesReported) {
            throw new RuntimeException("The number of issues found(" + totalFound + " doesn't match the issues reported (" +
                    this.issuesReported + ") for time frame " + this.timeFrame);
        }

        long totalReported = this.severeIssuesReported + this.nonSevereIssuesReported + this.defaultIssuesReported;
        if (totalReported != this.issuesReported) {
            throw new RuntimeException("The number of issues reported(" + totalReported + " doesn't match the original " +
                    "issues reported (" + this.issuesReported + ") for time frame " + this.timeFrame);

        }

        long totalInflated = this.defaultIssuesInflated + this.nonSevereIssuesInflated;
        if (totalInflated != this.inflatedReports) {
            throw new RuntimeException("The number of issues inflated(" + totalInflated + " doesn't match the original " +
                    "issues inflated (" + this.inflatedReports + ") for time frame " + this.timeFrame);

        }

    }

    /**
     * Configures the metrics for a test report.
     *
     * @param severeIssuesFound    Severe Issues Found.
     * @param nonSevereIssuesFound Non-Severe Issues Found.
     * @param inflationRatio       Inflation Ratio.
     */
    public void configureReport(double severeIssuesFound, double nonSevereIssuesFound,
                                double inflationRatio) {
        double issuesReported = severeIssuesFound + nonSevereIssuesFound;
        double inflatedReports = nonSevereIssuesFound * inflationRatio;
        long nonSevereIssuesReported = (long) (nonSevereIssuesFound - inflatedReports);

        this.setSevereIssuesFound((long) severeIssuesFound);
        this.setNonSevereIssuesFound((long) nonSevereIssuesFound);
        this.setIssuesReported((long) issuesReported);
        this.setInflatedReports((long) inflatedReports);
        this.setNonSevereIssuesReported(nonSevereIssuesReported);

    }

    public long getIssuesReported() {
        return issuesReported;
    }

    public void setIssuesReported(long issuesReported) {
        this.issuesReported = issuesReported;
    }

    public long getSevereIssuesFound() {
        return severeIssuesFound;
    }

    public long getNonSevereIssuesFound() {
        return nonSevereIssuesFound;
    }

    public void setNonSevereIssuesFound(long nonSevereIssuesFound) {
        this.nonSevereIssuesFound = nonSevereIssuesFound;
    }

    public long getNonSevereIssuesReported() {
        return nonSevereIssuesReported;
    }

    public void setNonSevereIssuesReported(long nonSevereIssuesReported) {
        this.nonSevereIssuesReported = nonSevereIssuesReported;
    }

    public long getInflatedReports() {
        return inflatedReports;
    }

    public void setInflatedReports(long possibleInflations) {
        this.inflatedReports = possibleInflations;
    }

    public void setSevereIssuesFound(long severeIssuesFound) {
        this.severeIssuesFound = severeIssuesFound;
    }

    public Double getInflationRatio() {
        return this.inflationRatio;
    }

    public long getSevereIssuesReported() {
        return severeIssuesReported;
    }

    public Double getSevereRatioInReport() {
        return severeRationInReport;
    }

    public Double getNonSevereRationInReport() {
        return nonSevereRationInReport;
    }

    public long getDefaultIssuesFound() {
        return defaultIssuesFound;
    }

    public long getDefaultIssuesReported() {
        return defaultIssuesReported;
    }

    public long getDefaultIssuesInflated() {
        return defaultIssuesInflated;
    }

    public long getNonSevereIssuesInflated() {
        return nonSevereIssuesInflated;
    }

    public String getTimeFrame() {
        return timeFrame;
    }

}
