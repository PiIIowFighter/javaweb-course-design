package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.OperationLog;
import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.util.SchemaUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志 DAO（dbo.OperationLogs）。
 */
public class OperationLogDAO {

    public void insert(OperationLog log) throws SQLException {
        SchemaUtil.ensureOperationLogsTable();
        String sql = "INSERT INTO dbo.OperationLogs(ActorUserId,ActorUsername,Module,Action,Detail,Ip) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (log.getActorUserId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, log.getActorUserId());
            }
            ps.setString(2, log.getActorUsername());
            ps.setString(3, log.getModule());
            ps.setString(4, log.getAction());
            ps.setString(5, log.getDetail());
            ps.setString(6, log.getIp());
            ps.executeUpdate();
        }
    }

    /**
     * 查询最近的操作日志。
     *
     * @param keyword 可为空，会在 ActorUsername/Module/Action/Detail 中做模糊查询
     */
    public List<OperationLog> findRecent(int limit, String keyword) throws SQLException {
        SchemaUtil.ensureOperationLogsTable();

        if (limit <= 0) {
            limit = 100;
        }

        String base = "SELECT TOP " + limit + " LogId, ActorUserId, ActorUsername, Module, Action, Detail, Ip, CreatedAt " +
                "FROM dbo.OperationLogs ";

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String where = hasKeyword ? "WHERE ActorUsername LIKE ? OR Module LIKE ? OR Action LIKE ? OR Detail LIKE ? " : "";
        String sql = base + where + "ORDER BY CreatedAt DESC, LogId DESC";

        List<OperationLog> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasKeyword) {
                String k = "%" + keyword.trim() + "%";
                ps.setString(1, k);
                ps.setString(2, k);
                ps.setString(3, k);
                ps.setString(4, k);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private OperationLog mapRow(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.setLogId(rs.getInt("LogId"));
        int uid = rs.getInt("ActorUserId");
        if (rs.wasNull()) {
            log.setActorUserId(null);
        } else {
            log.setActorUserId(uid);
        }
        log.setActorUsername(rs.getString("ActorUsername"));
        log.setModule(rs.getString("Module"));
        log.setAction(rs.getString("Action"));
        log.setDetail(rs.getString("Detail"));
        log.setIp(rs.getString("Ip"));

        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) {
            log.setCreatedAt(ts.toLocalDateTime());
        } else {
            log.setCreatedAt(LocalDateTime.now());
        }
        return log;
    }
}
