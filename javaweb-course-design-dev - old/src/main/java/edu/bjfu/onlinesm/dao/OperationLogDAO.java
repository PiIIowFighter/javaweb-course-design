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

    /**
     * 按条件查询操作日志（用于“系统管理 -> 日志审计/排查故障”）。
     *
     * @param limit    最大返回条数，<=0 默认 200
     * @param keyword  模糊关键字：会在 ActorUsername/Module/Action/Detail 中做 LIKE
     * @param actor    指定用户名（精确匹配），可为空
     * @param module   指定模块（精确匹配），可为空
     * @param from     起始时间（包含），可为空
     * @param to       结束时间（包含），可为空
     */
    public List<OperationLog> findByFilters(int limit,
                                           String keyword,
                                           String actor,
                                           String module,
                                           LocalDateTime from,
                                           LocalDateTime to) throws SQLException {
        SchemaUtil.ensureOperationLogsTable();

        if (limit <= 0) limit = 200;

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT TOP ").append(limit)
                .append(" LogId, ActorUserId, ActorUsername, Module, Action, Detail, Ip, CreatedAt ")
                .append("FROM dbo.OperationLogs ");

        List<Object> params = new ArrayList<>();
        List<Integer> types = new ArrayList<>();

        boolean hasWhere = false;

        // 时间范围
        if (from != null) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            hasWhere = true;
            sb.append("CreatedAt >= ? ");
            params.add(Timestamp.valueOf(from));
            types.add(Types.TIMESTAMP);
        }
        if (to != null) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            hasWhere = true;
            // 结束时间包含
            sb.append("CreatedAt <= ? ");
            params.add(Timestamp.valueOf(to));
            types.add(Types.TIMESTAMP);
        }

        // 精确用户名
        if (actor != null && !actor.trim().isEmpty()) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            hasWhere = true;
            sb.append("ActorUsername = ? ");
            params.add(actor.trim());
            types.add(Types.NVARCHAR);
        }

        // 精确模块
        if (module != null && !module.trim().isEmpty()) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            hasWhere = true;
            sb.append("Module = ? ");
            params.add(module.trim());
            types.add(Types.NVARCHAR);
        }

        // 模糊关键字
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            hasWhere = true;
            sb.append("(ActorUsername LIKE ? OR Module LIKE ? OR Action LIKE ? OR Detail LIKE ?) ");
            String k = "%" + keyword.trim() + "%";
            for (int i = 0; i < 4; i++) {
                params.add(k);
                types.add(Types.NVARCHAR);
            }
        }

        sb.append("ORDER BY CreatedAt DESC, LogId DESC");

        List<OperationLog> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                int t = types.get(i);
                if (v == null) {
                    ps.setNull(i + 1, t);
                } else if (v instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) v);
                } else {
                    ps.setObject(i + 1, v, t);
                }
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
