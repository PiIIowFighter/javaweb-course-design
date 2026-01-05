package edu.bjfu.onlinesm.service;

import edu.bjfu.onlinesm.util.RawHtmlToPdfConverter;
import edu.bjfu.onlinesm.util.UploadPathUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 模拟查重服务（不对接真实 Turnitin）。
 *
 * 需求（v2026-01）：
 * - 查重率：随机生成 < 20% 的数；
 * - 查重报告：生成一个“Turnitin 风格”的 PDF（数据合理随机）。
 *
 * 说明：本查重仅用于课程/演示，不代表真实 Turnitin 结果。
 */
public class PlagiarismCheckService {

    private static final double HIGH_SIMILARITY_THRESHOLD = 20.0;
    private static final Random random = new Random();

    /**
     * 缓存：同一 manuscriptId 多次点击“查重”返回同一份报告（避免反复生成）。
     */
    private static final Map<Integer, PlagiarismReport> reportCache = new HashMap<>();

    public PlagiarismReport checkPlagiarism(int manuscriptId, String title, String abstractText, String bodyText) {
        if (reportCache.containsKey(manuscriptId)) {
            return reportCache.get(manuscriptId);
        }

        int extractedBodyCount = computeBodyCount(bodyText);
        PlagiarismReport report = generateSimulatedReport(manuscriptId, safe(title), extractedBodyCount);
        reportCache.put(manuscriptId, report);
        return report;
    }

    public void clearCache(int manuscriptId) {
        reportCache.remove(manuscriptId);
    }

    public void clearAllCache() {
        reportCache.clear();
    }

    private PlagiarismReport generateSimulatedReport(int manuscriptId, String title, int extractedBodyCount) {
        PlagiarismReport report = new PlagiarismReport();

        String reportId = "SIM-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + "-" + String.format("%04d", random.nextInt(10000));

        // similarity: [0.5, 19.9)
        double similarity = Math.round((0.5 + random.nextDouble() * 19.4) * 10.0) / 10.0;

        report.setReportId(reportId);
        report.setManuscriptId(manuscriptId);
        report.setSimilarityScore(similarity);
        report.setHighSimilarity(similarity >= HIGH_SIMILARITY_THRESHOLD);
        report.setCheckTime(System.currentTimeMillis());

        // 生成 PDF（存盘）
        String relativeUrl = "/files/plagiarismReport?manuscriptId=" + manuscriptId + "&reportId=" + reportId;
        try {
            File pdf = buildReportPdf(manuscriptId, reportId, title, similarity, extractedBodyCount);
            // 只返回 servlet 路径（由前端/servlet 加 ctx）
            report.setReportUrl(relativeUrl);
        } catch (Exception e) {
            // 即便生成失败，也返回可访问链接（servlet 会提示不存在）
            report.setReportUrl(relativeUrl);
        }

        return report;
    }

    /**
     * 生成“Turnitin 风格”的两页查重报告 PDF。
     *
     * @param extractedBodyCount 若能从 PDF 提取到正文，则用于展示“字数”；否则使用合理随机值。
     */
    private File buildReportPdf(int manuscriptId, String reportId, String title, double similarity, int extractedBodyCount)
            throws Exception {

        File baseDir = UploadPathUtil.getBaseDirFile();
        File dir = new File(baseDir, "plagiarism_reports");
        if (!dir.exists()) dir.mkdirs();

        String safeId = reportId.replaceAll("[^A-Za-z0-9\\-]", "_");
        File out = new File(dir, "turnitin_" + manuscriptId + "_" + safeId + ".pdf");

        // —— 合理随机数据（尽量与“字数”匹配）
        int bodyCount = extractedBodyCount > 0 ? extractedBodyCount : (3000 + random.nextInt(5001)); // 3000-8000
        int pages = Math.max(3, (int) Math.ceil(bodyCount / 520.0));
        int characters = bodyCount + 1200 + random.nextInt(2500);
        int excludedQuotes = random.nextInt(4);      // 0-3
        int excludedBibliography = random.nextInt(2);// 0-1
        int excludedSmallMatches = 5 + random.nextInt(10); // 5-14

        int sourceCount = 5; // 例图里是 5 条
        List<SourceItem> sources = generateSources(sourceCount, similarity);

        List<String> highlights = generateHighlights(sources);

        String xhtml = buildTurnitinStyleXhtml(reportId, manuscriptId, title, similarity,
                pages, bodyCount, characters,
                excludedQuotes, excludedBibliography, excludedSmallMatches,
                sources, highlights);

        RawHtmlToPdfConverter.convert(xhtml, out);
        return out;
    }

    // =========================
    // 报告内容生成
    // =========================

    private String buildTurnitinStyleXhtml(String reportId,
                                          int manuscriptId,
                                          String title,
                                          double similarity,
                                          int pages,
                                          int bodyCount,
                                          int characters,
                                          int excludedQuotes,
                                          int excludedBibliography,
                                          int excludedSmallMatches,
                                          List<SourceItem> sources,
                                          List<String> highlights) {

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now());
        int simInt = (int) Math.round(similarity);
        String simText = String.format(Locale.US, "%.1f", similarity);

        // 进度条宽度（0-100）
        int bar = Math.max(0, Math.min(100, simInt));

        StringBuilder sourceRows = new StringBuilder();
        for (SourceItem s : sources) {
            sourceRows.append("<tr>")
                    .append("<td class='td num'>").append(s.index).append("</td>")
                    .append("<td class='td src'>").append(escapeHtml(s.source)).append("</td>")
                    .append("<td class='td pct'>").append(String.format(Locale.US, "%d%%", (int) Math.round(s.percent))).append("</td>")
                    .append("</tr>");
        }

        StringBuilder highlightHtml = new StringBuilder();
        for (String h : highlights) {
            highlightHtml.append("<p class='para'>").append(escapeHtml(h)).append("</p>");
        }

        // XHTML 1.0 Strict（Flying Saucer 更稳）
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
                "<head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" +
                "<style type=\"text/css\">" +
                "@page { size: A4; margin: 40pt 42pt 40pt 42pt; }" +
                "body { font-family: Arial, 'Microsoft YaHei', sans-serif; font-size: 11pt; color: #1b1f2a; }" +
                ".page { page-break-after: always; }" +
                ".page:last-child { page-break-after: auto; }" +
                ".topbar { width: 100%; background: #1f3b65; color: #fff; padding: 16pt 18pt; }" +
                // Flying Saucer 的表格宽度分配有时会导致右侧标题被挤出页面；固定布局并显式分配宽度
                ".topbar table{ width:100%; border-collapse:collapse; table-layout:fixed; }" +
                ".topbar td{ vertical-align:middle; }" +
                ".topbar .left{ font-size: 17pt; font-weight: bold; }" +
                ".topbar .right{ font-size: 10pt; text-align:right; opacity: 0.95; word-wrap:break-word; }" +
                ".card { border: 1pt solid #d9dee7; border-radius: 10pt; padding: 14pt 14pt; margin-top: 14pt; }" +
                ".cardTitle { font-size: 13pt; font-weight: bold; margin: 0 0 10pt 0; }" +
                ".muted { color: #5c677d; }" +
                ".mono { font-family: 'Courier New', monospace; }" +
                ".pill { display:inline-block; padding: 4pt 8pt; border: 1pt solid #cfd6e3; border-radius: 999pt; font-size: 10pt; color:#344055; background:#f6f8fb; }" +
                ".gridTable{ width:100%; border-collapse:separate; border-spacing: 10pt 0pt; }" +
                ".metric { border: 1pt solid #d9dee7; border-radius: 10pt; padding: 10pt 10pt; }" +
                ".metric .k{ font-size: 10pt; color:#5c677d; }" +
                ".metric .v{ font-size: 16pt; font-weight:bold; margin-top: 4pt; }" +
                ".simBox{ border-radius: 10pt; background:#f59e0b; color:#fff; text-align:center; padding: 10pt 0; }" +
                ".simBox .big{ font-size: 28pt; font-weight:bold; line-height: 1.0; }" +
                ".simBox .small{ font-size: 10pt; opacity:0.95; }" +
                ".table{ width:100%; border-collapse: collapse; margin-top: 8pt; }" +
                ".th{ background:#f3f5f9; border: 1pt solid #e6eaf2; padding: 8pt 8pt; font-weight:bold; }" +
                ".td{ border: 1pt solid #e6eaf2; padding: 8pt 8pt; }" +
                ".td.num{ width: 34pt; text-align:left; }" +
                ".td.pct{ width: 60pt; text-align:left; }" +
                ".progressWrap{ width:100%; border-radius: 10pt; background:#e5e7eb; border:1pt solid #e6eaf2; }" +
                ".progressFill{ height: 26pt; background:#f59e0b; border-radius: 10pt; }" +
                ".hr{ border-bottom: 1pt solid #d9dee7; margin: 10pt 0 10pt 0; }" +
                ".footer{ margin-top: 18pt; font-size: 9.5pt; color:#6b7280; }" +
                ".footerTable{ width:100%; border-collapse:collapse; }" +
                ".rightAlign{ text-align:right; }" +
	                // Flying Saucer 对 position:absolute 的支持不稳定，易造成文字重叠；改为普通块级“水印”避免覆盖正文
	                ".wm{ width:100%; text-align:center; opacity:0.08; font-size: 48pt; font-weight: bold; color:#6b7280; margin: 18pt 0 6pt 0; }" +
                ".para{ margin: 6pt 0; line-height: 1.45; }" +
                "</style>" +
                "</head>" +
                "<body>" +

                // ======= PAGE 1 =======
                "<div class='page'>" +
                "  <div class='topbar'>" +
                "    <table><tr>" +
                "      <td class='left' style='width:70%;'>Similarity Report (Mock)</td>" +
                "      <td class='right' style='width:30%;'>Academic Integrity Check</td>" +
                "    </tr></table>" +
                "  </div>" +

                "  <div class='wm'>SIMILARITY REPORT</div>" +

                "  <div class='card'>" +
                "    <div class='cardTitle'>Document Details</div>" +
                "    <table style='width:100%; border-collapse:collapse;'>" +
                "      <tr>" +
                "        <td style='vertical-align:top; width:72%;'>" +
                "          <p class='para'><span class='muted'>Title:</span> <b>" + escapeHtml(title) + "</b></p>" +
                "          <p class='para'><span class='muted'>Manuscript ID:</span> <span class='mono'>#" + manuscriptId + "</span></p>" +
                "          <p class='para'><span class='muted'>Report ID:</span> <span class='mono'>" + escapeHtml(reportId) + "</span> <span class='pill' style='margin-left:6pt;'>Mock</span></p>" +
                "          <p class='para'><span class='muted'>Generated:</span> " + escapeHtml(now) + "</p>" +
                "        </td>" +
                "        <td style='vertical-align:top; width:28%; padding-left:10pt;'>" +
                "          <div class='simBox'>" +
                "            <div class='big'>" + simInt + "%</div>" +
                "            <div class='small'>Similarity Index</div>" +
                "          </div>" +
                "        </td>" +
                "      </tr>" +
                "    </table>" +
                "  </div>" +

                "  <div class='card'>" +
                "    <div class='cardTitle'>Summary Metrics</div>" +
                "    <table class='gridTable'>" +
                "      <tr>" +
                "        <td class='metric' style='width:33%;'>" +
                "          <div class='k'>Pages</div><div class='v'>" + pages + "</div>" +
                "        </td>" +
                "        <td class='metric' style='width:33%;'>" +
                "          <div class='k'>Body Count</div><div class='v'>" + bodyCount + "</div>" +
                "        </td>" +
                "        <td class='metric' style='width:33%;'>" +
                "          <div class='k'>Characters</div><div class='v'>" + characters + "</div>" +
                "        </td>" +
                "      </tr>" +
                "    </table>" +
                "  </div>" +

                "  <div class='card'>" +
                "    <div class='cardTitle'>Filters Applied</div>" +
                "    <table class='table'>" +
                "      <tr><th class='th' style='width:45%;'>Filter</th><th class='th'>Value</th></tr>" +
                "      <tr><td class='td'>Exclude Quotes</td><td class='td'>" + excludedQuotes + "</td></tr>" +
                "      <tr><td class='td'>Exclude Bibliography</td><td class='td'>" + excludedBibliography + "</td></tr>" +
                "      <tr><td class='td'>Exclude Small Matches</td><td class='td'>" + excludedSmallMatches + " words</td></tr>" +
                "      <tr><td class='td'>Similarity Threshold</td><td class='td'>&lt; 20% (" + (similarity < HIGH_SIMILARITY_THRESHOLD ? "OK" : "High") + ")</td></tr>" +
                "    </table>" +
                "  </div>" +

                "  <div class='footer'>" +
                "    <table class='footerTable'><tr>" +
                "      <td><i>Disclaimer: This is a simulated Turnitin-style report for coursework/demo only.</i></td>" +
                "      <td class='rightAlign'>Page 1 of 2</td>" +
                "    </tr></table>" +
                "  </div>" +
                "</div>" +

                // ======= PAGE 2 =======
                "<div class='page'>" +
                "  <div class='topbar'>" +
                "    <table><tr>" +
                "      <td class='left' style='width:70%;'>Match Overview (Mock)</td>" +
                "      <td class='right' style='width:30%;'>Similarity Sources &amp; Highlights</td>" +
                "    </tr></table>" +
                "  </div>" +

                "  <div class='wm'>SIMILARITY SOURCES</div>" +

                "  <div style='margin-top:16pt;'>" +
                "    <div class='cardTitle'>Overall Similarity Index</div>" +
                "    <div class='hr'></div>" +
                "    <div class='progressWrap'>" +
                "      <div class='progressFill' style='width:" + bar + "%'></div>" +
                "    </div>" +
                "    <p class='para' style='margin-top:10pt;'><b>Similarity:</b> " + simInt + "% &nbsp;&nbsp;(<span class='muted'>threshold example: 20% = high similarity</span>)</p>" +
                "  </div>" +

                "  <div style='margin-top:14pt;'>" +
                "    <div class='cardTitle'>Top Matching Sources (demo)</div>" +
                "    <div class='hr'></div>" +
                "    <table class='table'>" +
                "      <tr><th class='th' style='width:34pt;'>#</th><th class='th'>Source</th><th class='th' style='width:70pt;'>Match %</th></tr>" +
                sourceRows +
                "    </table>" +
                "  </div>" +

                "  <div style='margin-top:16pt;'>" +
                "    <div class='cardTitle'>Match Highlights (mock excerpts)</div>" +
                "    <div class='hr'></div>" +
                "    <p class='para muted'>This section typically shows where overlaps occur and which sources they map to. For coursework demo purposes, the content below is synthetic (non-copyright) guidance text.</p>" +
                highlightHtml +
                "  </div>" +

                "  <div class='footer'>" +
                "    <table class='footerTable'><tr>" +
                "      <td><i>Disclaimer: Similarity scores do not automatically imply plagiarism; interpretation depends on citations and context.</i></td>" +
                "      <td class='rightAlign'>Page 2 of 2</td>" +
                "    </tr></table>" +
                "  </div>" +
                "</div>" +

                "</body></html>";
    }

    private List<String> generateHighlights(List<SourceItem> sources) {
        List<String> hs = new ArrayList<>();
        if (sources == null || sources.isEmpty()) {
            hs.add("No significant overlaps detected in the sampled sections.");
            return hs;
        }
        // 只展示前两条，和示例图一致
        int limit = Math.min(2, sources.size());
        for (int i = 0; i < limit; i++) {
            SourceItem s = sources.get(i);
            int pct = (int) Math.round(s.percent);
            String msg;
            if (i == 0) {
                msg = "Example highlight: - Source #" + s.index + " (" + pct + "%): Overlap detected in the 'workflow automation' paragraph. Suggested action: paraphrase and cite.";
            } else {
                msg = "- Source #" + s.index + " (" + pct + "%): Overlap detected in the related work summary. Suggested action: add quotation marks or rewrite.";
            }
            hs.add(msg);
        }
        return hs;
    }

    private List<SourceItem> generateSources(int n, double similarity) {
        List<SourceItem> list = new ArrayList<>();
        if (n <= 0) return list;

        String[] pool = new String[]{
                "journal.example.org/article/1234",
                "conference.demo.edu/papers/2024-ml-systems.pdf",
                "preprint.example.com/abs/2201.01234",
                "thesis.archive.edu/handle/98765",
                "books.example.net/chapter/online-workflows",
                "repository.example.edu/item/5566",
                "openaccess.sample.org/paper/7788"
        };

        // 让前 5 条加起来大致 <= similarity
        double remaining = similarity;
        for (int i = 1; i <= n; i++) {
            double val;
            if (i == n) {
                val = Math.max(0.0, remaining);
            } else {
                // 每条 2-7 左右，最后一条兜底
                double maxThis = Math.max(2.0, Math.min(7.0, remaining - (n - i) * 2.0));
                val = 2.0 + random.nextDouble() * Math.max(0.1, (maxThis - 2.0));
                val = Math.min(val, remaining);
            }
            val = Math.max(0.5, Math.round(val * 10.0) / 10.0);
            remaining = Math.max(0.0, Math.round((remaining - val) * 10.0) / 10.0);

            String src = pool[(i - 1) % pool.length];
            list.add(new SourceItem(i, val, src));

            if (remaining <= 0.0) break;
        }

        // 若剩余为 0 但还没满 5 条，用 2/3% 的小来源补齐（不要求严格总和）
        while (list.size() < n) {
            int idx = list.size() + 1;
            String src = pool[(idx - 1) % pool.length];
            double val = 2 + random.nextInt(3); // 2-4
            list.add(new SourceItem(idx, val, src));
        }

        return list;
    }

    // =========================
    // “字数”统计（中文字符 + 英文单词）
    // =========================

    private int computeBodyCount(String bodyText) {
        if (bodyText == null || bodyText.trim().isEmpty()) return 0;
        String cleaned = bodyText.replaceAll("<[^>]+>", " ").trim();
        return countCjkChars(cleaned) + countEnglishWordsExcludingCjk(cleaned);
    }

    private int countCjkChars(String text) {
        if (text == null || text.isEmpty()) return 0;
        int c = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isCjk(ch)) c++;
        }
        return c;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(ch);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || b == Character.UnicodeBlock.HIRAGANA
                || b == Character.UnicodeBlock.KATAKANA
                || b == Character.UnicodeBlock.HANGUL_SYLLABLES
                || b == Character.UnicodeBlock.HANGUL_JAMO;
    }

    private int countEnglishWordsExcludingCjk(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        String noCjk = text.replaceAll("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]", " ");
        noCjk = noCjk.replaceAll("[^A-Za-z0-9]+", " ");
        String[] words = noCjk.trim().split("\\s+");
        int count = 0;
        for (String w : words) {
            if (w != null && !w.trim().isEmpty()) count++;
        }
        return count;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static class SourceItem {
        final int index;
        final double percent;
        final String source;

        SourceItem(int index, double percent, String source) {
            this.index = index;
            this.percent = percent;
            this.source = source;
        }
    }

    public static class PlagiarismReport {
        private String reportId;
        private Integer manuscriptId;
        private double similarityScore;
        private boolean highSimilarity;
        private String reportUrl;
        private long checkTime;

        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }

        public Integer getManuscriptId() { return manuscriptId; }
        public void setManuscriptId(Integer manuscriptId) { this.manuscriptId = manuscriptId; }

        public double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }

        public boolean isHighSimilarity() { return highSimilarity; }
        public void setHighSimilarity(boolean highSimilarity) { this.highSimilarity = highSimilarity; }

        public String getReportUrl() { return reportUrl; }
        public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }

        public long getCheckTime() { return checkTime; }
        public void setCheckTime(long checkTime) { this.checkTime = checkTime; }

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
