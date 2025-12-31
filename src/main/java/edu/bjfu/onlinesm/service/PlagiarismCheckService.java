package edu.bjfu.onlinesm.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PlagiarismCheckService {

    private static final double HIGH_SIMILARITY_THRESHOLD = 20.0;
    private static final Random random = new Random();

    private static final Map<Integer, PlagiarismReport> reportCache = new HashMap<>();

    public PlagiarismReport checkPlagiarism(int manuscriptId, String title, String abstractText, String bodyText) {
        if (reportCache.containsKey(manuscriptId)) {
            return reportCache.get(manuscriptId);
        }

        PlagiarismReport report = generateSimulatedReport(manuscriptId, title, abstractText, bodyText);
        reportCache.put(manuscriptId, report);
        
        return report;
    }

    private PlagiarismReport generateSimulatedReport(int manuscriptId, String title, String abstractText, String bodyText) {
        double similarityScore = calculateSimulatedSimilarity(title, abstractText, bodyText);
        boolean highSimilarity = similarityScore > HIGH_SIMILARITY_THRESHOLD;
        String reportUrl = generateReportUrl(manuscriptId);
        String reportId = "TURNITIN-" + System.currentTimeMillis() + "-" + manuscriptId;

        PlagiarismReport report = new PlagiarismReport();
        report.setReportId(reportId);
        report.setManuscriptId(manuscriptId);
        report.setSimilarityScore(similarityScore);
        report.setHighSimilarity(highSimilarity);
        report.setReportUrl(reportUrl);
        report.setCheckTime(System.currentTimeMillis());

        return report;
    }

    private double calculateSimulatedSimilarity(String title, String abstractText, String bodyText) {
        int totalLength = 0;
        
        if (title != null) {
            totalLength += title.length();
        }
        if (abstractText != null) {
            totalLength += abstractText.length();
        }
        if (bodyText != null) {
            totalLength += bodyText.length();
        }

        double baseSimilarity = 5.0 + (random.nextDouble() * 15.0);
        
        if (totalLength > 0) {
            double lengthFactor = Math.min(totalLength / 10000.0, 10.0);
            baseSimilarity += lengthFactor;
        }

        baseSimilarity += (random.nextDouble() * 10.0) - 5.0;
        
        return Math.round(baseSimilarity * 100.0) / 100.0;
    }

    private String generateReportUrl(int manuscriptId) {
        return "/reports/plagiarism/turnitin_report_" + manuscriptId + ".pdf";
    }

    public void clearCache(int manuscriptId) {
        reportCache.remove(manuscriptId);
    }

    public void clearAllCache() {
        reportCache.clear();
    }

    public static class PlagiarismReport {
        private String reportId;
        private Integer manuscriptId;
        private double similarityScore;
        private boolean highSimilarity;
        private String reportUrl;
        private long checkTime;

        public String getReportId() {
            return reportId;
        }

        public void setReportId(String reportId) {
            this.reportId = reportId;
        }

        public Integer getManuscriptId() {
            return manuscriptId;
        }

        public void setManuscriptId(Integer manuscriptId) {
            this.manuscriptId = manuscriptId;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public void setSimilarityScore(double similarityScore) {
            this.similarityScore = similarityScore;
        }

        public boolean isHighSimilarity() {
            return highSimilarity;
        }

        public void setHighSimilarity(boolean highSimilarity) {
            this.highSimilarity = highSimilarity;
        }

        public String getReportUrl() {
            return reportUrl;
        }

        public void setReportUrl(String reportUrl) {
            this.reportUrl = reportUrl;
        }

        public long getCheckTime() {
            return checkTime;
        }

        public void setCheckTime(long checkTime) {
            this.checkTime = checkTime;
        }

        @Override
        public String toString() {
            return "PlagiarismReport{" +
                    "reportId='" + reportId + '\'' +
                    ", manuscriptId=" + manuscriptId +
                    ", similarityScore=" + similarityScore +
                    ", highSimilarity=" + highSimilarity +
                    ", reportUrl='" + reportUrl + '\'' +
                    ", checkTime=" + checkTime +
                    '}';
        }
    }
}
