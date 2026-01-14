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
        
        Runtime rt = Runtime.getRuntime();
        req.setAttribute("now", LocalDateTime.now());
        req.setAttribute("javaVersion", System.getProperty("java.version"));
        req.setAttribute("osName", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        req.setAttribute("userTimeZone", System.getProperty("user.timezone"));
        req.setAttribute("maxMemory", rt.maxMemory());
        req.setAttribute("totalMemory", rt.totalMemory());
        req.setAttribute("freeMemory", rt.freeMemory());
        req.setAttribute("processors", rt.availableProcessors());

        
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

        
        try {
            List<OperationLog> recent = logDAO.findRecent(30, null);
            req.setAttribute("recentLogs", recent);
        } catch (SQLException e) {
            
        }

        req.getRequestDispatcher("/WEB-INF/jsp/admin/system/system_status.jsp").forward(req, resp);
    }

        
    private void handleDbMaintenance(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        
        boolean dbOk = false;
        String dbError = null;

        
        java.util.List<String> tableNames = new java.util.ArrayList<>();
        String selectedTable = req.getParameter("table");
        java.util.List<String> columnNames = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        java.util.List<String> pkColumns = new java.util.ArrayList<>();
        java.util.List<String> autoIncColumns = new java.util.ArrayList<>();
        String message = null;
        String error = null;

        try (Connection conn = DbUtil.getConnection()) {
            dbOk = conn != null && !conn.isClosed();

            if (dbOk) {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                String catalog = conn.getCatalog();
                
                String schema = "dbo";

                
                try (java.sql.ResultSet rs = meta.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        String tName = rs.getString("TABLE_NAME");
                        if (tName != null && !tName.trim().isEmpty()) {
                            tableNames.add(tName);
                        }
                    }
                }

                
                
                if ("POST".equalsIgnoreCase(req.getMethod())) {
                    String action = req.getParameter("action");
                    selectedTable = req.getParameter("table");

                    if (action != null && (selectedTable == null || !tableNames.contains(selectedTable))) {
                        error = "非法的表名或表不存在。";
                    } else if (action != null) {

                        
                        pkColumns.clear();
                        try (java.sql.ResultSet pkRs = meta.getPrimaryKeys(catalog, schema, selectedTable)) {
                            while (pkRs.next()) {
                                String col = pkRs.getString("COLUMN_NAME");
                                if (col != null && !col.trim().isEmpty()) {
                                    pkColumns.add(col);
                                }
                            }
                        }
                        
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

                        
                        columnNames.clear();
                        java.util.Set<String> autoIncSet = new java.util.HashSet<>();
                        try (java.sql.ResultSet colRs = meta.getColumns(catalog, schema, selectedTable, "%")) {
                            while (colRs.next()) {
                                String col = colRs.getString("COLUMN_NAME");
                                if (col == null || col.trim().isEmpty()) continue;
                                columnNames.add(col);

                                String isAuto = null;
                                try {
                                    isAuto = colRs.getString("IS_AUTOINCREMENT");
                                } catch (Exception ignore) {
                                    
                                }
                                if (isAuto != null && "YES".equalsIgnoreCase(isAuto)) {
                                    autoIncSet.add(col);
                                }
                            }
                        }

                        if (!pkColumns.isEmpty()) {
                            if ("updateRow".equals(action)) {
                                
                                java.util.List<String> updatableCols = new java.util.ArrayList<>();
                                for (String col : columnNames) {
                                    if (pkColumns.contains(col)) continue;
                                    if (autoIncSet.contains(col)) continue;
                                    updatableCols.add(col);
                                }

                                if (updatableCols.isEmpty()) {
                                    message = "该表没有可更新字段（主键/自增列不可直接更新）。";
                                } else {
                                    StringBuilder sql = new StringBuilder();
                                    sql.append("UPDATE ").append("[").append(selectedTable).append("]").append(" SET ");
                                    java.util.List<Object> params = new java.util.ArrayList<>();

                                    boolean first = true;
                                    for (String col : updatableCols) {
                                        String paramName = "col_" + col;
                                        String value = req.getParameter(paramName);

                                        if (!first) {
                                            sql.append(", ");
                                        }
                                        sql.append("[").append(col).append("] = ?");

                                        
                                        if (value != null && value.trim().isEmpty()) {
                                            value = null;
                                        }
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
                                    } catch (SQLException e) {
                                        error = "更新失败： " + e.getMessage();
                                    }
                                }
                            } else if ("deleteRow".equals(action)) {
                                
                                StringBuilder sql = new StringBuilder();
                                sql.append("DELETE FROM ").append("[").append(selectedTable).append("]").append(" WHERE ");
                                java.util.List<Object> params = new java.util.ArrayList<>();

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
                                    int deleted = ps.executeUpdate();
                                    message = "已删除 " + deleted + " 行记录（表 " + selectedTable + "）。";
                                    edu.bjfu.onlinesm.util.OperationLogger.log(
                                            req,
                                            "SYSTEM_DB",
                                            "DELETE_ROW",
                                            "删除表 " + selectedTable + " 中的一行数据"
                                    );
                                } catch (SQLException e) {
                                    error = "删除失败： " + e.getMessage();
                                }
                            } else if ("insertRow".equals(action)) {
                                
                                java.util.List<String> insertCols = new java.util.ArrayList<>();
                                for (String col : columnNames) {
                                    if (autoIncSet.contains(col)) continue;
                                    insertCols.add(col);
                                }

                                if (insertCols.isEmpty()) {
                                    error = "该表不存在可插入的列（可能全部为自增/生成列）。";
                                } else {
                                    StringBuilder sql = new StringBuilder();
                                    sql.append("INSERT INTO ").append("[").append(selectedTable).append("] (");
                                    boolean first = true;
                                    for (String col : insertCols) {
                                        if (!first) sql.append(", ");
                                        sql.append("[").append(col).append("]");
                                        first = false;
                                    }
                                    sql.append(") VALUES (");
                                    for (int i = 0; i < insertCols.size(); i++) {
                                        if (i > 0) sql.append(", ");
                                        sql.append("?");
                                    }
                                    sql.append(")");

                                    java.util.List<Object> params = new java.util.ArrayList<>();
                                    for (String col : insertCols) {
                                        String paramName = "new_" + col;
                                        String value = req.getParameter(paramName);
                                        if (value != null && value.trim().isEmpty()) {
                                            value = null;
                                        }
                                        params.add(value);
                                    }

                                    try (java.sql.PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                                        for (int i = 0; i < params.size(); i++) {
                                            ps.setObject(i + 1, params.get(i));
                                        }
                                        int inserted = ps.executeUpdate();
                                        message = "已新增 " + inserted + " 行记录（表 " + selectedTable + "）。";
                                        edu.bjfu.onlinesm.util.OperationLogger.log(
                                                req,
                                                "SYSTEM_DB",
                                                "INSERT_ROW",
                                                "向表 " + selectedTable + " 新增一行数据"
                                        );
                                    } catch (SQLException e) {
                                        error = "新增失败： " + e.getMessage();
                                    }
                                }
                            }
                        } else {
                            error = "未能识别表 " + selectedTable + " 的主键或列信息，无法执行操作。";
                        }
                    }
                }


                if (selectedTable != null && tableNames.contains(selectedTable)) {
                    
                    
                    columnNames.clear();
                    autoIncColumns.clear();
                    try (java.sql.ResultSet colRs = meta.getColumns(catalog, schema, selectedTable, "%")) {
                        while (colRs.next()) {
                            String col = colRs.getString("COLUMN_NAME");
                            if (col == null || col.trim().isEmpty()) {
                                continue;
                            }
                            columnNames.add(col);

                            String isAuto = null;
                            try {
                                isAuto = colRs.getString("IS_AUTOINCREMENT");
                            } catch (Exception ignore) {
                                
                            }
                            if (isAuto != null && "YES".equalsIgnoreCase(isAuto)) {
                                autoIncColumns.add(col);
                            }
                        }
                    }


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
        req.setAttribute("autoIncColumns", autoIncColumns);
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

