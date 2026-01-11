package edu.bjfu.onlinesm.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * Simple in-memory pagination helper for list pages.
 *
 * Conventions:
 *  - request param: page (1-based), pageSize
 *  - request attributes (for JSP):
 *      page, pageSize, totalCount, pageCount, paginationPrefix
 *
 * paginationPrefix example:
 *   /app/admin/users/list?roleCode=EDITOR&page=
 */
public class PaginationUtil {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private PaginationUtil() {}

    public static int getPage(HttpServletRequest req) {
        return parsePositiveInt(req.getParameter("page"), 1, Integer.MAX_VALUE, 1);
    }

    public static int getPageSize(HttpServletRequest req) {
        return parsePositiveInt(req.getParameter("pageSize"), 1, MAX_PAGE_SIZE, DEFAULT_PAGE_SIZE);
    }

    /**
     * Apply pagination to a full list and write both the paged list and metadata into request attributes.
     */
    public static <T> void apply(HttpServletRequest req, List<T> fullList, String listAttrName) {
        if (fullList == null) fullList = Collections.emptyList();

        int pageSize = getPageSize(req);
        int totalCount = fullList.size();
        int pageCount = (int) Math.ceil(totalCount / (double) pageSize);
        if (pageCount <= 0) pageCount = 1;

        int page = getPage(req);
        if (page > pageCount) page = pageCount;

        int from = Math.min((page - 1) * pageSize, totalCount);
        int to = Math.min(from + pageSize, totalCount);

        List<T> items = fullList.subList(from, to);

        req.setAttribute(listAttrName, items);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", pageSize);
        req.setAttribute("totalCount", totalCount);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("paginationPrefix", buildPaginationPrefix(req));
    }

    private static String buildPaginationPrefix(HttpServletRequest req) {
        String uri = req.getRequestURI(); // includes context path
        String qs = req.getQueryString();

        String cleaned = removeQueryParam(qs, "page");
        // keep pageSize and other filters

        if (cleaned == null || cleaned.isEmpty()) {
            return uri + "?page=";
        }
        return uri + "?" + cleaned + "&page=";
    }

    private static int parsePositiveInt(String s, int min, int max, int def) {
        if (s == null) return def;
        try {
            int v = Integer.parseInt(s.trim());
            if (v < min) return def;
            if (v > max) return max;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Remove a single query parameter from an existing query string.
     */
    private static String removeQueryParam(String queryString, String param) {
        if (queryString == null || queryString.isEmpty()) return queryString;
        // split and rebuild to avoid regex edge cases
        String[] parts = queryString.split("&");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            int eq = p.indexOf('=');
            String key = (eq >= 0) ? p.substring(0, eq) : p;
            if (param.equals(key)) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(p);
        }
        return sb.toString();
    }
}
