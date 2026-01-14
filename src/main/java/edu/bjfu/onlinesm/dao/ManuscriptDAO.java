package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


import edu.bjfu.onlinesm.dao.ManuscriptStageTimestampsDAO;


public class ManuscriptDAO {

    
    private final ManuscriptStageTimestampsDAO stageTimestampsDAO = new ManuscriptStageTimestampsDAO();

    
    public List<Manuscript> findLatestAccepted(int limit) throws SQLException {
        String top = limit > 0 ? "TOP " + limit + " " : "";

        
        String sqlNew = "SELECT " + top +
                " m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime, " +
                " j.Name AS JournalName, j.ISSN AS JournalIssn, " +
                " m.Doi, m.PublishYear, m.[Volume] AS Volume, m.[Issue] AS Issue, m.PageRange, m.[Language] AS Language, m.ArticleType, m.ClassificationNo, m.CnkiUrl, m.PublishedAt " +
                " FROM dbo.Manuscripts m " +
                " LEFT JOIN dbo.Journals j ON j.JournalId = m.JournalId " +
                " WHERE m.IsArchived = 0 AND m.IsWithdrawn = 0 AND m.Status IN ('ACCEPTED') " +
                " ORDER BY ISNULL(m.PublishedAt, m.FinalDecisionTime) DESC, m.ManuscriptId DESC";

        String sqlOld = "SELECT " + top + " ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                "FROM dbo.Manuscripts " +
                "WHERE IsArchived = 0 AND IsWithdrawn = 0 AND Status IN ('ACCEPTED') " +
                "ORDER BY FinalDecisionTime DESC, ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection()) {
            
            try (PreparedStatement ps = conn.prepareStatement(sqlNew);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowPublic(rs));
                }
                return list;
            } catch (SQLException e) {
                
                try (PreparedStatement ps = conn.prepareStatement(sqlOld);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        }
        return list;
    }

    
    public Manuscript findAcceptedById(int manuscriptId) throws SQLException {
        
        
        String sqlNew = "SELECT m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime, " +
                "j.Name AS JournalName, j.ISSN AS JournalIssn, " +
                "m.Doi, m.PublishYear, m.[Volume] AS Volume, m.[Issue] AS Issue, m.PageRange, m.[Language] AS Language, m.ArticleType, m.ClassificationNo, m.CnkiUrl, m.PublishedAt, " +
                "am.ViewCount, am.DownloadCount, am.CitationCount, am.PopularityScore " +
                "FROM dbo.Manuscripts m " +
                "LEFT JOIN dbo.Journals j ON j.JournalId = m.JournalId " +
                "LEFT JOIN dbo.ArticleMetrics am ON am.ManuscriptId = m.ManuscriptId " +
                "WHERE m.ManuscriptId = ? AND m.Status IN ('ACCEPTED')";

        String sqlOld = "SELECT m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime, " +
                "am.ViewCount, am.DownloadCount, am.CitationCount, am.PopularityScore " +
                "FROM dbo.Manuscripts m " +
                "LEFT JOIN dbo.ArticleMetrics am ON am.ManuscriptId = m.ManuscriptId " +
                "WHERE m.ManuscriptId = ? AND m.Status IN ('ACCEPTED')";

        try (Connection conn = DbUtil.getConnection()) {
            try {
                Manuscript m = queryOneAccepted(conn, sqlNew, manuscriptId);
                if (m == null) return null;
                
                if (isPublicationMetaMissing(m)) {
                    ensurePublicationMetaIfMissing(conn, manuscriptId, m.getTitle(), m.getFinalDecisionTime());
                    m = queryOneAccepted(conn, sqlNew, manuscriptId);
                }
                return m;
            } catch (SQLException e) {
                
                return queryOneAccepted(conn, sqlOld, manuscriptId);
            }
        }
    }

    private Manuscript queryOneAccepted(Connection conn, String sql, int manuscriptId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowPublic(rs);
                }
            }
        }
        return null;
    }

    private boolean isPublicationMetaMissing(Manuscript m) {
        if (m == null) return false;
        
        boolean missingCore = (m.getDoi() == null || m.getDoi().trim().isEmpty())
                || (m.getPublishYear() == null)
                || (m.getVolume() == null || m.getVolume().trim().isEmpty())
                || (m.getIssue() == null || m.getIssue().trim().isEmpty())
                || (m.getPageRange() == null || m.getPageRange().trim().isEmpty());
        return missingCore;
    }

    
    private void ensurePublicationMetaIfMissing(Connection conn,
                                                int manuscriptId,
                                                String titleHint,
                                                java.time.LocalDateTime finalDecisionTimeHint) throws SQLException {
        
        try (PreparedStatement probe = conn.prepareStatement(
                "SELECT TOP 1 Doi, PublishYear, [Volume], [Issue], PageRange, [Language], ArticleType, ClassificationNo, CnkiUrl, PublishedAt, FinalDecisionTime, Title " +
                        "FROM dbo.Manuscripts WHERE ManuscriptId=?")) {
            probe.setInt(1, manuscriptId);
            try (ResultSet rs = probe.executeQuery()) {
                if (!rs.next()) return;

                String title = titleHint != null ? titleHint : rs.getString("Title");
                Timestamp fdt = rs.getTimestamp("FinalDecisionTime");
                java.time.LocalDateTime finalDecisionTime = finalDecisionTimeHint != null ? finalDecisionTimeHint : (fdt == null ? null : fdt.toLocalDateTime());

                String doi = rs.getString("Doi");
                Object pyObj = rs.getObject("PublishYear");
                Integer publishYear = pyObj == null ? null : ((Number) pyObj).intValue();
                String volume = rs.getString("Volume");
                String issue = rs.getString("Issue");
                String pageRange = rs.getString("PageRange");
                String language = rs.getString("Language");
                String articleType = rs.getString("ArticleType");
                String classificationNo = rs.getString("ClassificationNo");
                String cnkiUrl = rs.getString("CnkiUrl");
                Timestamp publishedAtTs = rs.getTimestamp("PublishedAt");

                
                java.util.Random rnd = new java.util.Random(System.nanoTime() ^ (((long) manuscriptId) << 32));
                int year = (finalDecisionTime != null ? finalDecisionTime.getYear() : java.time.LocalDate.now().getYear());
                if (publishYear == null) publishYear = year;
                if (volume == null || volume.trim().isEmpty()) {
                    
                    volume = String.valueOf(Math.max(1, (publishYear - 2000) + 1));
                }
                if (issue == null || issue.trim().isEmpty()) {
                    issue = String.valueOf(1 + rnd.nextInt(12));
                }
                if (pageRange == null || pageRange.trim().isEmpty()) {
                    int start = 1 + rnd.nextInt(220);
                    int end = start + 4 + rnd.nextInt(12);
                    pageRange = start + "-" + end;
                }

                boolean hasCjk = title != null && title.chars().anyMatch(ch -> (ch >= 0x4E00 && ch <= 0x9FFF));
                if (language == null || language.trim().isEmpty()) {
                    language = hasCjk ? "中文" : "English";
                }
                if (articleType == null || articleType.trim().isEmpty()) {
                    String[] zh = {"研究论文", "综述", "方法", "短报", "观点"};
                    String[] en = {"Research Article", "Review", "Methods", "Brief Report", "Perspective"};
                    articleType = (hasCjk ? zh : en)[rnd.nextInt(5)];
                }
                if (classificationNo == null || classificationNo.trim().isEmpty()) {
                    String[] cls = {"TP391.41", "TP391.9", "TP18", "TP301.6", "O211"};
                    classificationNo = cls[rnd.nextInt(cls.length)];
                }
                if (doi == null || doi.trim().isEmpty()) {
                    doi = "10." + (1000 + rnd.nextInt(9000)) + "/onlinesm." + publishYear + "." + manuscriptId + (char) ('a' + rnd.nextInt(26));
                }

                java.time.LocalDateTime publishedAt = (publishedAtTs == null ? null : publishedAtTs.toLocalDateTime());
                if (publishedAt == null) {
                    java.time.LocalDateTime base = finalDecisionTime != null ? finalDecisionTime : java.time.LocalDateTime.now();
                    publishedAt = base.plusDays(rnd.nextInt(21)).withHour(9 + rnd.nextInt(8)).withMinute(rnd.nextInt(60)).withSecond(0).withNano(0);
                }

                
                if ((cnkiUrl == null || cnkiUrl.trim().isEmpty()) && manuscriptId == 15) {
                    cnkiUrl = "https://kns.cnki.net/kcms2/article/abstract?v=hyKDWyHWvTt9Oni1P6Lkq-5VqdV4b3UcgbOsmUcT1puL3W-6PsLlSDKHZ6gpEdPY4SfsGv3ZFS_c1MgyFn7GndnipDZeRu41wg_RxX5lHkaNEyCpeOnvM_KGe1fyQLkLDb9lgKT7TbAziCs8J_nvE2sOSYypoM57QybF9fXycU8=&uniplatform=NZKPT";
                }

                String upd = "UPDATE dbo.Manuscripts SET " +
                        "Doi = COALESCE(NULLIF(Doi,''), ?), " +
                        "PublishYear = COALESCE(PublishYear, ?), " +
                        "[Volume] = COALESCE(NULLIF([Volume],''), ?), " +
                        "[Issue] = COALESCE(NULLIF([Issue],''), ?), " +
                        "PageRange = COALESCE(NULLIF(PageRange,''), ?), " +
                        "[Language] = COALESCE(NULLIF([Language],''), ?), " +
                        "ArticleType = COALESCE(NULLIF(ArticleType,''), ?), " +
                        "ClassificationNo = COALESCE(NULLIF(ClassificationNo,''), ?), " +
                        "CnkiUrl = COALESCE(NULLIF(CnkiUrl,''), ?), " +
                        "PublishedAt = COALESCE(PublishedAt, ?) " +
                        "WHERE ManuscriptId = ?";

                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    int idx = 1;
                    ps.setString(idx++, doi);
                    ps.setInt(idx++, publishYear);
                    ps.setString(idx++, volume);
                    ps.setString(idx++, issue);
                    ps.setString(idx++, pageRange);
                    ps.setString(idx++, language);
                    ps.setString(idx++, articleType);
                    ps.setString(idx++, classificationNo);
                    ps.setString(idx++, cnkiUrl);
                    ps.setTimestamp(idx++, Timestamp.valueOf(publishedAt));
                    ps.setInt(idx, manuscriptId);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("invalid column")) {
                return;
            }
            throw e;
        }
    }

    
    public Manuscript insertWithStatus(Connection conn, Manuscript m, String status, boolean setSubmitTime) throws SQLException {
        String sql = "INSERT INTO dbo.Manuscripts " +
                "(JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime) " +
                "VALUES (?,?,?,?,?,?,?,?,?, " + (setSubmitTime ? "DATEADD(HOUR, 8, SYSUTCDATETIME())" : "NULL") + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            if (m.getJournalId() != null) {
                ps.setInt(idx++, m.getJournalId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }
            ps.setInt(idx++, m.getSubmitterId());
            ps.setString(idx++, m.getTitle());
            ps.setString(idx++, m.getAbstractText());
            ps.setString(idx++, m.getKeywords());
            ps.setString(idx++, m.getSubjectArea());
            ps.setString(idx++, m.getFundingInfo());
            ps.setString(idx++, m.getAuthorList());
            ps.setString(idx, status);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    m.setManuscriptId(rs.getInt(1));
                }
            }
        }

        m.setCurrentStatus(status);
        
        
        stageTimestampsDAO.create(conn, m.getManuscriptId());
        
        return m;
    }

    
    public void updateMetadataAndStatus(Connection conn, Manuscript m, String status, boolean setSubmitTime) throws SQLException {
        
        String oldStatus = null;
        String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setInt(1, m.getManuscriptId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    oldStatus = rs.getString("Status");
                }
            }
        }
        
        String sql = "UPDATE dbo.Manuscripts SET " +
                "JournalId = ?, Title = ?, Abstract = ?, Keywords = ?, SubjectArea = ?, FundingInfo = ?, AuthorList = ?, " +
                "Status = ?, " +
                (setSubmitTime ? "SubmitTime = ISNULL(SubmitTime, DATEADD(HOUR, 8, SYSUTCDATETIME())), " : "") +
                "LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                "WHERE ManuscriptId = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (m.getJournalId() != null) {
                ps.setInt(idx++, m.getJournalId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }
            ps.setString(idx++, m.getTitle());
            ps.setString(idx++, m.getAbstractText());
            ps.setString(idx++, m.getKeywords());
            ps.setString(idx++, m.getSubjectArea());
            ps.setString(idx++, m.getFundingInfo());
            ps.setString(idx++, m.getAuthorList());
            ps.setString(idx++, status);
            ps.setInt(idx, m.getManuscriptId());
            ps.executeUpdate();
        }

        m.setCurrentStatus(status);
        
        
        if (oldStatus != null && !oldStatus.equals(status)) {
            stageTimestampsDAO.ensureAndUpdateStage(conn, m.getManuscriptId(), oldStatus);
        }
    }

    
    public Manuscript insert(Manuscript m) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(true);
            return insertWithStatus(conn, m, "SUBMITTED", true);
        }
    }

    
    public List<Manuscript> findBySubmitter(int submitterId) throws SQLException {
        String sql = "SELECT m.ManuscriptId, m.JournalId, " +
                     "m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, " +
                     "m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime " +
                     "FROM dbo.Manuscripts m " +
                     "WHERE m.SubmitterId = ? ORDER BY m.ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, submitterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    
    public Manuscript findById(int manuscriptId) throws SQLException {
        String sql = "SELECT m.ManuscriptId, m.JournalId, " +
                     "m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, " +
                     "m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime " +
                     "FROM dbo.Manuscripts m " +
                     "WHERE m.ManuscriptId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    public Integer findCurrentEditorId(int manuscriptId) throws SQLException {
        
        
        String sql1 = "SELECT CurrentEditorId FROM dbo.Manuscripts WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql1)) {

            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int editorId = rs.getInt(1);
                    return rs.wasNull() ? null : editorId;
                }
            }
            return null;

        } catch (SQLException ex) {

            
            String msg = ex.getMessage();
            String low = (msg == null) ? "" : msg.toLowerCase();
            boolean columnMissing =
                    (msg != null && msg.contains("CurrentEditorId"))
                            || low.contains("invalid column")
                            || low.contains("unknown column");

            if (!columnMissing) {
                throw ex;
            }

            
            String sql2 = "SELECT TOP 1 EditorId " +
                          "FROM dbo.ManuscriptAssignments " +
                          "WHERE ManuscriptId = ? " +
                          "ORDER BY AssignedTime DESC, AssignmentId DESC";
            try (Connection conn2 = DbUtil.getConnection();
                 PreparedStatement ps2 = conn2.prepareStatement(sql2)) {

                ps2.setInt(1, manuscriptId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        int editorId = rs2.getInt(1);
                        return rs2.wasNull() ? null : editorId;
                    }
                }
            } catch (SQLException ignore) {
                
            }

            return null;
        }
    }

    public List<Manuscript> findByStatus(String status) throws SQLException {
        return findByStatuses(status);
    }

    
    public List<Manuscript> findByStatuses(String... statuses) throws SQLException {
        if (statuses == null || statuses.length == 0) {
            throw new IllegalArgumentException("statuses 不能为空");
        }

        StringBuilder sql = new StringBuilder(
                "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                "FROM dbo.Manuscripts WHERE IsArchived = 0 AND IsWithdrawn = 0 AND Status IN ("
        );
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
        }
        sql.append(") ORDER BY SubmitTime DESC, ManuscriptId DESC");

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < statuses.length; i++) {
                ps.setString(i + 1, statuses[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    
    public List<Manuscript> findByStatusesForEditor(int editorUserId, String... statuses) throws SQLException {
        if (statuses == null || statuses.length == 0) {
            throw new IllegalArgumentException("statuses 不能为空");
        }

        
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, " +
                    "       SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                    "FROM dbo.Manuscripts WHERE IsArchived = 0 AND IsWithdrawn = 0 " +
                    "  AND CurrentEditorId = ? AND Status IN ("
            );
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
            }
            sql.append(") ORDER BY ISNULL(SubmitTime, LastStatusTime) DESC, ManuscriptId DESC");

            List<Manuscript> list = new ArrayList<>();
            try (Connection conn = DbUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                int idx = 1;
                ps.setInt(idx++, editorUserId);
                for (String s : statuses) {
                    ps.setString(idx++, s);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            
            String msg = (e.getMessage() == null) ? "" : e.getMessage();
            if (!msg.contains("CurrentEditorId")) {
                throw e; 
            }
        }

        
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("WITH latest AS (\n");
            sql.append("    SELECT ManuscriptId, EditorId,\n");
            sql.append("           ROW_NUMBER() OVER (PARTITION BY ManuscriptId ORDER BY AssignedTime DESC, AssignmentId DESC) AS rn\n");
            sql.append("    FROM dbo.ManuscriptAssignments\n");
            sql.append(")\n");
            sql.append("SELECT m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords,\n");
            sql.append("       m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime\n");
            sql.append("FROM dbo.Manuscripts m\n");
            sql.append("JOIN latest la ON la.ManuscriptId = m.ManuscriptId AND la.rn = 1\n");
            sql.append("WHERE m.IsArchived = 0 AND m.IsWithdrawn = 0\n");
            sql.append("  AND la.EditorId = ? AND m.Status IN (");
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
            }
            sql.append(") ORDER BY ISNULL(m.SubmitTime, m.LastStatusTime) DESC, m.ManuscriptId DESC");

            List<Manuscript> list = new ArrayList<>();
            try (Connection conn = DbUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                int idx = 1;
                ps.setInt(idx++, editorUserId);
                for (String s : statuses) {
                    ps.setString(idx++, s);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return list;
        } catch (SQLException ignore) {
            
            List<Manuscript> raw = findByStatuses(statuses);
            List<Manuscript> filtered = new ArrayList<>();
            for (Manuscript m : raw) {
                Integer ce = findCurrentEditorId(m.getManuscriptId());
                if (java.util.Objects.equals(ce, editorUserId)) {
                    filtered.add(m);
                }
            }
            return filtered;
        }
    }

    
    public List<Manuscript> findAllForChief() throws SQLException {
        String sql = "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime "
                   + "FROM dbo.Manuscripts "
                   + "ORDER BY ISNULL(SubmitTime, LastStatusTime) DESC, ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    
    public void rescindDecision(int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts "
                   + "SET Status = 'FINAL_DECISION_PENDING', "
                   + "    Decision = NULL, "
                   + "    FinalDecisionTime = NULL, "
                   + "    LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) "
                   + "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    
    public void retractManuscript(int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts "
                   + "SET IsWithdrawn = 1, "
                   + "    IsArchived = 1, "
                   + "    Status = 'ARCHIVED', "
                   + "    LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) "
                   + "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    
    public void deskReject(int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts " +
                "SET Status = 'REJECTED', " +
                "    Decision = 'REJECT', " +
                "    FinalDecisionTime = NULL, " +
                "    CurrentEditorId = NULL, " +
                "    LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                "WHERE ManuscriptId = ? AND Status = 'DESK_REVIEW_INITIAL'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    
    public void deskRejectWithReason(int manuscriptId, int changedBy, String rejectReason) throws SQLException {
        if (rejectReason != null) rejectReason = rejectReason.trim();
        if (rejectReason == null || rejectReason.isEmpty()) {
            throw new SQLException("退稿理由不能为空。");
        }

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) oldStatus = rs.getString("Status");
                    }
                }

                
                String updateSql = "UPDATE dbo.Manuscripts " +
                        "SET Status='REJECTED', Decision='REJECT', FinalDecisionTime=DATEADD(HOUR, 8, SYSUTCDATETIME()), " +
                        "    CurrentEditorId=NULL, LastStatusTime=DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                        "WHERE ManuscriptId=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, manuscriptId);
                    ps.executeUpdate();
                }

                
                insertStatusHistory(conn, manuscriptId, oldStatus, "REJECTED", "DESK_REJECT", changedBy, rejectReason);

                
                if (oldStatus != null) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    

    
    public void changeDeskDecision(int manuscriptId, String deskOp, int changedBy, String reason) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ManuscriptSnapshot snap = lockAndLoadSnapshot(conn, manuscriptId);
                if (snap == null) {
                    throw new IllegalStateException("未找到该稿件。");
                }
                if (snap.isArchived || snap.isWithdrawn) {
                    throw new IllegalStateException("该稿件已归档/撤稿，无法更改决定。");
                }
                if (snap.finalDecisionTime != null) {
                    throw new IllegalStateException("该稿件已做出终审决定（FinalDecisionTime 非空），不能按“初审决定”改判。");
                }

                String fromStatus = snap.status;
                String toStatus;
                String decision;

                if ("deskAccept".equalsIgnoreCase(deskOp)) {
                    toStatus = "TO_ASSIGN";
                    decision = null;
                } else if ("deskReject".equalsIgnoreCase(deskOp)) {
                    toStatus = "REJECTED";
                    decision = "REJECT";
                } else {
                    throw new IllegalStateException("不支持的 deskOp：" + deskOp);
                }

                String updateSql = "UPDATE dbo.Manuscripts SET Status=?, Decision=?, FinalDecisionTime=NULL, CurrentEditorId=NULL, LastStatusTime=DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                        "WHERE ManuscriptId=? AND IsArchived=0 AND IsWithdrawn=0";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, toStatus);
                    if (decision == null) ps.setNull(2, java.sql.Types.NVARCHAR);
                    else ps.setString(2, decision);
                    ps.setInt(3, manuscriptId);
                    ps.executeUpdate();
                }

                expireActiveReviews(conn, manuscriptId);
                insertStatusHistory(conn, manuscriptId, fromStatus, toStatus, "CHANGE_DESK_DECISION", changedBy, reason);
                
                
                stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, fromStatus);

                
                if ("ACCEPTED".equalsIgnoreCase(toStatus)) {
                    ensurePublicationMetaIfMissing(conn, manuscriptId, null, null);
                }

                conn.commit();
            } catch (RuntimeException ex) {
                conn.rollback();
                throw ex;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void changeFinalDecision(int manuscriptId, String finalOp, int changedBy, String reason) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ManuscriptSnapshot snap = lockAndLoadSnapshot(conn, manuscriptId);
                if (snap == null) {
                    throw new IllegalStateException("未找到该稿件。");
                }
                if (snap.isArchived || snap.isWithdrawn) {
                    throw new IllegalStateException("该稿件已归档/撤稿，无法更改决定。");
                }
                if (snap.finalDecisionTime == null || !("ACCEPTED".equals(snap.status) || "REJECTED".equals(snap.status) || "REVISION".equals(snap.status))) {
                    throw new IllegalStateException("该稿件尚未形成终审决定，不能使用“更改终审决定”。");
                }

                String fromStatus = snap.status;
                String toStatus;
                String decision;

                if ("accept".equalsIgnoreCase(finalOp)) {
                    toStatus = "ACCEPTED";
                    decision = "ACCEPT";
                } else if ("reject".equalsIgnoreCase(finalOp)) {
                    toStatus = "REJECTED";
                    decision = "REJECT";
                } else if ("revision".equalsIgnoreCase(finalOp)) {
                    toStatus = "REVISION";
                    decision = "REVISION";
                } else {
                    throw new IllegalStateException("不支持的 finalOp：" + finalOp);
                }

                String updateSql = "UPDATE dbo.Manuscripts SET Status=?, Decision=?, FinalDecisionTime=DATEADD(HOUR, 8, SYSUTCDATETIME()), CurrentEditorId=NULL, LastStatusTime=DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                        "WHERE ManuscriptId=? AND IsArchived=0 AND IsWithdrawn=0";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, toStatus);
                    ps.setString(2, decision);
                    ps.setInt(3, manuscriptId);
                    ps.executeUpdate();
                }

                expireActiveReviews(conn, manuscriptId);
                insertStatusHistory(conn, manuscriptId, fromStatus, toStatus, "CHANGE_FINAL_DECISION", changedBy, reason);
                
                
                stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, fromStatus);

                
                if ("ACCEPTED".equalsIgnoreCase(toStatus)) {
                    ensurePublicationMetaIfMissing(conn, manuscriptId, null, null);
                }

                conn.commit();
            } catch (RuntimeException ex) {
                conn.rollback();
                throw ex;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void retractPublished(int manuscriptId, int changedBy, String reason) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ManuscriptSnapshot snap = lockAndLoadSnapshot(conn, manuscriptId);
                if (snap == null) {
                    throw new IllegalStateException("未找到该稿件。");
                }
                if (snap.isArchived || snap.isWithdrawn) {
                    throw new IllegalStateException("该稿件已归档/撤稿，不能重复撤稿。");
                }
                if (!"ACCEPTED".equalsIgnoreCase(snap.status)) {
                    throw new IllegalStateException("仅已发表（ACCEPTED）稿件允许撤稿。");
                }

                String fromStatus = snap.status;
                String toStatus = "ARCHIVED";

                String updateSql = "UPDATE dbo.Manuscripts SET IsWithdrawn=1, IsArchived=1, Status='ARCHIVED', LastStatusTime=DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE ManuscriptId=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, manuscriptId);
                    ps.executeUpdate();
                }

                expireActiveReviews(conn, manuscriptId);
                insertStatusHistory(conn, manuscriptId, fromStatus, toStatus, "RETRACT_PUBLISHED", changedBy, reason);
                
                
                stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, fromStatus);

                conn.commit();
            } catch (RuntimeException ex) {
                conn.rollback();
                throw ex;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    

    private static class ManuscriptSnapshot {
        final String status;
        final boolean isArchived;
        final boolean isWithdrawn;
        final Timestamp finalDecisionTime;

        ManuscriptSnapshot(String status, boolean isArchived, boolean isWithdrawn, Timestamp finalDecisionTime) {
            this.status = status;
            this.isArchived = isArchived;
            this.isWithdrawn = isWithdrawn;
            this.finalDecisionTime = finalDecisionTime;
        }
    }

    private ManuscriptSnapshot lockAndLoadSnapshot(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT Status, IsArchived, IsWithdrawn, FinalDecisionTime FROM dbo.Manuscripts WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ManuscriptSnapshot(
                        rs.getString("Status"),
                        rs.getBoolean("IsArchived"),
                        rs.getBoolean("IsWithdrawn"),
                        rs.getTimestamp("FinalDecisionTime")
                );
            }
        }
    }

    private void expireActiveReviews(Connection conn, int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Reviews SET Status='EXPIRED' WHERE ManuscriptId=? AND Status IN ('INVITED','ACCEPTED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }


    private void insertStatusHistory(Connection conn, int manuscriptId, String fromStatus, String toStatus, String event, int changedBy, String remark) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStatusHistory (ManuscriptId, FromStatus, ToStatus, Event, ChangedBy, Remark) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setString(2, fromStatus);
            ps.setString(3, toStatus);
            ps.setString(4, event);
            ps.setInt(5, changedBy);
            ps.setString(6, remark);
            ps.executeUpdate();
        }
    }

    
    public void updateStatus(int manuscriptId, String newStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }
                
                
                String sql = "UPDATE dbo.Manuscripts " +
                        "SET Status = ?, LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                        "WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }
                
                
                if (oldStatus != null && !oldStatus.equals(newStatus)) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void updateStatusWithHistory(int manuscriptId, String newStatus, String event, int changedBy, String remark) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }

                
                String updateSql = "UPDATE dbo.Manuscripts SET Status = ?, LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }

                
                insertStatusHistory(conn, manuscriptId, oldStatus, newStatus, event, changedBy, remark);
                
                
                if (oldStatus != null) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void assignEditor(int manuscriptId, int editorUserId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }
                
                
                String sql = "UPDATE dbo.Manuscripts " +
                        "SET CurrentEditorId = ?, Status = 'WITH_EDITOR', LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                        "WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, editorUserId);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }
                
                
                if (oldStatus != null && !oldStatus.equals("WITH_EDITOR")) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void assignEditorWithHistory(int manuscriptId, int editorUserId, int changedBy, String remark) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }

                
                String updateSql = "UPDATE dbo.Manuscripts SET CurrentEditorId = ?, Status = 'WITH_EDITOR', LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, editorUserId);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }

                
                insertStatusHistory(conn, manuscriptId, oldStatus, "WITH_EDITOR", "ASSIGN_EDITOR", changedBy, remark);
                
                
                if (oldStatus != null) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void updateFinalDecision(int manuscriptId, String decision, String newStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }
                
                
                String sql = "UPDATE dbo.Manuscripts " +
                        "SET Status = ?, Decision = ?, FinalDecisionTime = DATEADD(HOUR, 8, SYSUTCDATETIME()), LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                        "WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, decision);
                    ps.setInt(3, manuscriptId);
                    ps.executeUpdate();
                }
                
                
                if (oldStatus != null && !oldStatus.equals(newStatus)) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }

                
                if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
                    ensurePublicationMetaIfMissing(conn, manuscriptId, null, null);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void updateAndResubmit(Connection conn, Manuscript m, String fromStatus) throws SQLException {
        if (!"RETURNED".equals(fromStatus) && !"REVISION".equals(fromStatus)) {
            throw new IllegalArgumentException("不支持的 Resubmit 来源状态: " + fromStatus);
        }

        StringBuilder sql = new StringBuilder("UPDATE dbo.Manuscripts SET ");
        sql.append("Title = ?, ");
        sql.append("Abstract = ?, ");
        sql.append("Keywords = ?, ");
        sql.append("SubjectArea = ?, ");
        sql.append("FundingInfo = ?, ");
        sql.append("AuthorList = ?, ");
        sql.append("JournalId = ?, ");

        if ("RETURNED".equals(fromStatus)) {
            
            sql.append("Status = 'SUBMITTED', ");
        } else {
            
            sql.append("Status = 'WITH_EDITOR', ");
            sql.append("CurrentRound = ISNULL(CurrentRound, 0) + 1, ");
            sql.append("Decision = NULL, ");
            sql.append("FinalDecisionTime = NULL, ");
        }

        sql.append("LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) ");
        sql.append("WHERE ManuscriptId = ?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, m.getTitle());
            ps.setString(idx++, m.getAbstractText());
            ps.setString(idx++, m.getKeywords());
            ps.setString(idx++, m.getSubjectArea());
            ps.setString(idx++, m.getFundingInfo());
            ps.setString(idx++, m.getAuthorList());

            if (m.getJournalId() != null) {
                ps.setInt(idx++, m.getJournalId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }

            ps.setInt(idx, m.getManuscriptId());
            ps.executeUpdate();
        }
        
        
        stageTimestampsDAO.ensureAndUpdateStage(conn, m.getManuscriptId(), fromStatus);
    }


public void updateResubmitDraft(Connection conn, Manuscript m) throws SQLException {
    String sql = "UPDATE dbo.Manuscripts SET " +
            "Title = ?, Abstract = ?, Keywords = ?, SubjectArea = ?, FundingInfo = ?, AuthorList = ?, JournalId = ?, " +
            "LastStatusTime = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
            "WHERE ManuscriptId = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int idx = 1;
        ps.setString(idx++, m.getTitle());
        ps.setString(idx++, m.getAbstractText());
        ps.setString(idx++, m.getKeywords());
        ps.setString(idx++, m.getSubjectArea());
        ps.setString(idx++, m.getFundingInfo());
        ps.setString(idx++, m.getAuthorList());

        if (m.getJournalId() != null) {
            ps.setInt(idx++, m.getJournalId());
        } else {
            ps.setNull(idx++, Types.INTEGER);
        }

        ps.setInt(idx, m.getManuscriptId());
        ps.executeUpdate();
    }
}

    
    public void updateAndResubmit(Manuscript m, String fromStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            updateAndResubmit(conn, m, fromStatus);
        }
    }

    
    public Manuscript mapRowPublic(ResultSet rs) throws SQLException {
        Manuscript m = new Manuscript();
        m.setManuscriptId(rs.getInt("ManuscriptId"));
        int journalId = rs.getInt("JournalId");
        if (!rs.wasNull()) {
            m.setJournalId(journalId);
        }
        m.setSubmitterId(rs.getInt("SubmitterId"));
        m.setTitle(rs.getString("Title"));
        m.setAbstractText(rs.getString("Abstract"));
        m.setKeywords(rs.getString("Keywords"));
        try { m.setSubjectArea(rs.getString("SubjectArea")); } catch (SQLException ignored) {}
        try { m.setFundingInfo(rs.getString("FundingInfo")); } catch (SQLException ignored) {}
        try { m.setAuthorList(rs.getString("AuthorList")); } catch (SQLException ignored) {}

        m.setCurrentStatus(rs.getString("Status"));
        Timestamp ts = rs.getTimestamp("SubmitTime");
        if (ts != null) {
            m.setSubmitTime(ts.toLocalDateTime());
        }

        
        try {
            String decision = rs.getString("Decision");
            m.setDecision(decision);
        } catch (SQLException ignored) {
        }
        try {
            Timestamp finalTs = rs.getTimestamp("FinalDecisionTime");
            if (finalTs != null) {
                m.setFinalDecisionTime(finalTs.toLocalDateTime());
            }
        } catch (SQLException ignored) {
        }

        
        try {
            if (hasColumn(rs, "ViewCount")) {
                m.setViewCount((Integer) rs.getObject("ViewCount"));
            }
            if (hasColumn(rs, "DownloadCount")) {
                m.setDownloadCount((Integer) rs.getObject("DownloadCount"));
            }
            if (hasColumn(rs, "CitationCount")) {
                m.setCitationCount((Integer) rs.getObject("CitationCount"));
            }
            if (hasColumn(rs, "PopularityScore")) {
                Object v = rs.getObject("PopularityScore");
                if (v != null) m.setPopularityScore(((Number) v).doubleValue());
            }
        } catch (SQLException ignored) {
            
        }

        
        try {
            if (hasColumn(rs, "JournalName")) {
                m.setJournalName(rs.getString("JournalName"));
            }
            if (hasColumn(rs, "JournalIssn")) {
                m.setJournalIssn(rs.getString("JournalIssn"));
            }
            if (hasColumn(rs, "Doi")) {
                m.setDoi(rs.getString("Doi"));
            }
            if (hasColumn(rs, "PublishYear")) {
                Object y = rs.getObject("PublishYear");
                if (y != null) m.setPublishYear(((Number) y).intValue());
            }
            if (hasColumn(rs, "Volume")) {
                m.setVolume(rs.getString("Volume"));
            }
            if (hasColumn(rs, "Issue")) {
                m.setIssue(rs.getString("Issue"));
            }
            if (hasColumn(rs, "PageRange")) {
                m.setPageRange(rs.getString("PageRange"));
            }
            if (hasColumn(rs, "Language")) {
                m.setLanguage(rs.getString("Language"));
            }
            if (hasColumn(rs, "ArticleType")) {
                m.setArticleType(rs.getString("ArticleType"));
            }
            if (hasColumn(rs, "ClassificationNo")) {
                m.setClassificationNo(rs.getString("ClassificationNo"));
            }
            if (hasColumn(rs, "CnkiUrl")) {
                m.setCnkiUrl(rs.getString("CnkiUrl"));
            }
            if (hasColumn(rs, "PublishedAt")) {
                Timestamp pt = rs.getTimestamp("PublishedAt");
                if (pt != null) m.setPublishedAt(pt.toLocalDateTime());
            }
        } catch (SQLException ignored) {
            
        }

        return m;
    }

    private Manuscript mapRow(ResultSet rs) throws SQLException {
        return mapRowPublic(rs);
    }

    private boolean hasColumn(ResultSet rs, String col) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int count = md.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = md.getColumnLabel(i);
            if (label == null || label.isEmpty()) label = md.getColumnName(i);
            if (col.equalsIgnoreCase(label)) return true;
        }
        return false;
    }

    
    public List<Manuscript> findAcceptedByMetric(String type, int limit) throws SQLException {
        if (type == null) type = "popular";
        String metricCol;
        if ("topcited".equalsIgnoreCase(type)) {
            metricCol = "ISNULL(am.CitationCount,0)";
        } else if ("downloaded".equalsIgnoreCase(type)) {
            metricCol = "ISNULL(am.DownloadCount,0)";
        } else {
            metricCol = "ISNULL(am.PopularityScore,0)";
        }

        String sqlNew = "SELECT TOP " + limit + " " +
                "m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime, " +
                "j.Name AS JournalName, j.ISSN AS JournalIssn, " +
                "m.Doi, m.PublishYear, m.[Volume] AS Volume, m.[Issue] AS Issue, m.PageRange, m.[Language] AS Language, m.ArticleType, m.ClassificationNo, m.CnkiUrl, m.PublishedAt, " +
                "am.ViewCount, am.DownloadCount, am.CitationCount, am.PopularityScore " +
                "FROM dbo.Manuscripts m " +
                "LEFT JOIN dbo.Journals j ON j.JournalId = m.JournalId " +
                "LEFT JOIN dbo.ArticleMetrics am ON am.ManuscriptId = m.ManuscriptId " +
                "WHERE m.IsArchived=0 AND m.IsWithdrawn=0 AND m.Status='ACCEPTED' " +
                "ORDER BY " + metricCol + " DESC, ISNULL(m.PublishedAt, m.FinalDecisionTime) DESC, m.ManuscriptId DESC";

        String sqlOld = "SELECT TOP " + limit + " " +
                "m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime, " +
                "am.ViewCount, am.DownloadCount, am.CitationCount, am.PopularityScore " +
                "FROM dbo.Manuscripts m " +
                "LEFT JOIN dbo.ArticleMetrics am ON am.ManuscriptId = m.ManuscriptId " +
                "WHERE m.IsArchived=0 AND m.IsWithdrawn=0 AND m.Status='ACCEPTED' " +
                "ORDER BY " + metricCol + " DESC, m.FinalDecisionTime DESC, m.ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlNew);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowPublic(rs));
                }
                return list;
            } catch (SQLException e) {
                try (PreparedStatement ps = conn.prepareStatement(sqlOld);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRowPublic(rs));
                    }
                }
            }
        }
        return list;
    }

    
    public void incrementViewCount(int manuscriptId) throws SQLException {
        String ensure = "IF NOT EXISTS(SELECT 1 FROM dbo.ArticleMetrics WHERE ManuscriptId=?) " +
                "INSERT INTO dbo.ArticleMetrics(ManuscriptId) VALUES (?)";
        String upd = "UPDATE dbo.ArticleMetrics SET ViewCount = ViewCount + 1, UpdatedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE ManuscriptId=?";

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(ensure);
                 PreparedStatement ps2 = conn.prepareStatement(upd)) {
                ps1.setInt(1, manuscriptId);
                ps1.setInt(2, manuscriptId);
                ps1.executeUpdate();

                ps2.setInt(1, manuscriptId);
                ps2.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void incrementDownloadCount(int manuscriptId) throws SQLException {
        String ensure = "IF NOT EXISTS(SELECT 1 FROM dbo.ArticleMetrics WHERE ManuscriptId=?) " +
                "INSERT INTO dbo.ArticleMetrics(ManuscriptId) VALUES (?)";
        String upd = "UPDATE dbo.ArticleMetrics SET DownloadCount = DownloadCount + 1, UpdatedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE ManuscriptId=?";
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(ensure);
                 PreparedStatement ps2 = conn.prepareStatement(upd)) {
                ps1.setInt(1, manuscriptId);
                ps1.setInt(2, manuscriptId);
                ps1.executeUpdate();
                ps2.setInt(1, manuscriptId);
                ps2.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

}

/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

