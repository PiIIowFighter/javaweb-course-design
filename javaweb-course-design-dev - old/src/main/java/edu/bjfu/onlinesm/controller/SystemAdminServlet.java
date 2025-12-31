package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.OperationLogDAO;
import edu.bjfu.onlinesm.model.OperationLog;
import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.util.SchemaUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统维护模块：
 *  - 查看系统运行状态（JVM、内存、数据库连通性等）；
 *  - 方便系统管理员快速排查。
 */
@WebServlet(name = "SystemAdminServlet", urlPatterns = {"/admin/system/*"})
public class SystemAdminServlet extends HttpServlet {

    private final OperationLogDAO logDAO = new OperationLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/status".equals(path)) {
            handleStatus(req, resp);
            return;
        }
        if ("/db".equals(path)) {
            handleDbMaintenance(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleStatus(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // JVM/系统状态
        Runtime rt = Runtime.getRuntime();
        req.setAttribute("now", LocalDateTime.now());
        req.setAttribute("javaVersion", System.getProperty("java.version"));
        req.setAttribute("osName", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        req.setAttribute("userTimeZone", System.getProperty("user.timezone"));
        req.setAttribute("maxMemory", rt.maxMemory());
        req.setAttribute("totalMemory", rt.totalMemory());
        req.setAttribute("freeMemory", rt.freeMemory());
        req.setAttribute("processors", rt.availableProcessors());

        // DB 连通性
        boolean dbOk = false;
        String dbError = null;
        try (Connection conn = DbUtil.getConnection()) {
            dbOk = conn != null && !conn.isClosed();
        } catch (SQLException e) {
            dbOk = false;
            dbError = e.getMessage();
        }
        req.setAttribute("dbOk", dbOk);
        req.setAttribute("dbError", dbError);

        // 最近操作日志（用于快速排查）
        try {
            List<OperationLog> recent = logDAO.findRecent(30, null);
            req.setAttribute("recentLogs", recent);
        } catch (SQLException e) {
            // ignore
        }

        req.getRequestDispatcher("/WEB-INF/jsp/admin/system/system_status.jsp").forward(req, resp);
    }

        /**
     * 数据库维护：简单的数据浏览 / 修改工具。
     * 需求：可以看到所有表的名称，点击进入展示所有数据，并且可以对数据进行修改。
     * 仅系统管理员/超级管理员可通过权限点访问。
     */
    private void handleDbMaintenance(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 基本的 DB 连通性检查
        boolean dbOk = false;
        String dbError = null;

        // 数据浏览所需的属性
        java.util.List<String> tableNames = new java.util.ArrayList<>();
        String selectedTable = req.getParameter("table");
        java.util.List<String> columnNames = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        java.util.List<String> pkColumns = new java.util.ArrayList<>();
        String message = null;
        String error = null;

        try (Connection conn = DbUtil.getConnection()) {
            dbOk = conn != null && !conn.isClosed();

            if (dbOk) {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                String catalog = conn.getCatalog();
                // SQL Server 中常用的 schema 为 dbo
                String schema = "dbo";

                // 读取当前数据库下的所有用户表名称
                try (java.sql.ResultSet rs = meta.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        String tName = rs.getString("TABLE_NAME");
                        if (tName != null && !tName.trim().isEmpty()) {
                            tableNames.add(tName);
                        }
                    }
                }

                // 处理更新操作（POST + action=updateRow）
                if ("POST".equalsIgnoreCase(req.getMethod())) {
                    String action = req.getParameter("action");
                    if ("updateRow".equals(action)) {
                        selectedTable = req.getParameter("table");
                        if (selectedTable != null && tableNames.contains(selectedTable)) {
                            // 获取主键列
                            try (java.sql.ResultSet pkRs = meta.getPrimaryKeys(catalog, schema, selectedTable)) {
                                while (pkRs.next()) {
                                    String col = pkRs.getString("COLUMN_NAME");
                                    if (col != null && !col.trim().isEmpty()) {
                                        pkColumns.add(col);
                                    }
                                }
                            }
                            // 若没有配置主键，则退化为使用第一列作为“主键”
                            if (pkColumns.isEmpty()) {
                                try (java.sql.ResultSet colRs = meta.getColumns(catalog, schema, selectedTable, "%")) {
                                    if (colRs.next()) {
                                        String firstCol = colRs.getString("COLUMN_NAME");
                                        if (firstCol != null && !firstCol.trim().isEmpty()) {
                                            pkColumns.add(firstCol);
                                        }
                                    }
                                }
                            }

                            // 获取所有列名，用于构造 UPDATE 语句
                            try (java.sql.ResultSet colRs = meta.getColumns(catalog, schema, selectedTable, "%")) {
                                while (colRs.next()) {
                                    String col = colRs.getString("COLUMN_NAME");
                                    if (col != null && !col.trim().isEmpty()) {
                                        columnNames.add(col);
                                    }
                                }
                            }

                            if (!columnNames.isEmpty() && !pkColumns.isEmpty()) {
                                StringBuilder sql = new StringBuilder();
                                sql.append("UPDATE ").append("[").append(selectedTable).append("]").append(" SET ");
                                java.util.List<Object> params = new java.util.ArrayList<>();

                                boolean first = true;
                                for (String col : columnNames) {
                                    String paramName = "col_" + col;
                                    String value = req.getParameter(paramName);
                                    if (!first) {
                                        sql.append(", ");
                                    }
                                    sql.append("[").append(col).append("] = ?");
                                    params.add(value);
                                    first = false;
                                }

                                sql.append(" WHERE ");
                                boolean firstPk = true;
                                for (String pk : pkColumns) {
                                    String pkParamName = "pk_" + pk;
                                    String pkVal = req.getParameter(pkParamName);
                                    if (!firstPk) {
                                        sql.append(" AND ");
                                    }
                                    sql.append("[").append(pk).append("] = ?");
                                    params.add(pkVal);
                                    firstPk = false;
                                }

                                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                                    for (int i = 0; i < params.size(); i++) {
                                        ps.setObject(i + 1, params.get(i));
                                    }
                                    int updated = ps.executeUpdate();
                                    message = "已更新 " + updated + " 行记录（表 " + selectedTable + "）。";
                                    edu.bjfu.onlinesm.util.OperationLogger.log(
                                            req,
                                            "SYSTEM_DB",
                                            "UPDATE_ROW",
                                            "更新表 " + selectedTable + " 中的一行数据"
                                    );
                                }
                            } else {
                                error = "未能识别表 " + selectedTable + " 的主键或列信息，无法执行更新。";
                            }
                        } else {
                            error = "非法的表名或表不存在。";
                        }
                    }
                }

                // 重新读取选中表的数据（GET 或 更新之后）
                if (selectedTable != null && tableNames.contains(selectedTable)) {
                    // 列名
                    columnNames.clear();
                    try (java.sql.ResultSet colRs = meta.getColumns(catalog, schema, selectedTable, "%")) {
                        while (colRs.next()) {
                            String col = colRs.getString("COLUMN_NAME");
                            if (col != null && !col.trim().isEmpty()) {
                                columnNames.add(col);
                            }
                        }
                    }

                    // 主键列
                    pkColumns.clear();
                    try (java.sql.ResultSet pkRs = meta.getPrimaryKeys(catalog, schema, selectedTable)) {
                        while (pkRs.next()) {
                            String col = pkRs.getString("COLUMN_NAME");
                            if (col != null && !col.trim().isEmpty()) {
                                pkColumns.add(col);
                            }
                        }
                    }
                    if (pkColumns.isEmpty() && !columnNames.isEmpty()) {
                        pkColumns.add(columnNames.get(0));
                    }

                    // 实际数据
                    try (java.sql.Statement st = conn.createStatement();
                         java.sql.ResultSet rs = st.executeQuery("SELECT * FROM [" + selectedTable + "]")) {
                        while (rs.next()) {
                            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                            for (String col : columnNames) {
                                row.put(col, rs.getObject(col));
                            }
                            rows.add(row);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            dbOk = false;
            dbError = e.getMessage();
        }

        req.setAttribute("dbOk", dbOk);
        req.setAttribute("dbError", dbError);
        req.setAttribute("tableNames", tableNames);
        req.setAttribute("selectedTable", selectedTable);
        req.setAttribute("columnNames", columnNames);
        req.setAttribute("rows", rows);
        req.setAttribute("pkColumns", pkColumns);
        req.setAttribute("message", message);
        req.setAttribute("error", error);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/system/db_maintenance.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/db".equals(path)) {
            handleDbMaintenance(req, resp);
        } else {
            doGet(req, resp);
        }
    }

}