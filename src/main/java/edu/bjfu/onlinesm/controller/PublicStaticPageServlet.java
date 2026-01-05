package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.dao.JournalPageDAO;
import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.model.JournalPage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 前台页面：Publish 与 Guide。
 */
@WebServlet(name = "PublicStaticPageServlet", urlPatterns = {"/publish", "/guide"})
public class PublicStaticPageServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final JournalPageDAO journalPageDAO = new JournalPageDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sp = req.getServletPath();
        try {
            // 期刊信息（主期刊）
            Journal journal = null;
            try {
                journal = journalDAO.findPrimary();
            } catch (SQLException ignored) {
            }
            req.setAttribute("journal", journal);

            Integer journalId = (journal == null) ? null : journal.getJournalId();

            if ("/publish".equals(sp)) {
                // 如果数据库未初始化 publish 页面，则自动补齐一份默认内容，避免前台空白
                ensureDefaultPage(journalId,
                        "publish",
                        "期刊出版信息（Publish）",
                        defaultPublishHtml());
                loadPage(req, journalId, "publish");
                req.getRequestDispatcher("/WEB-INF/jsp/public/publish.jsp").forward(req, resp);
                return;
            }

            if ("/guide".equals(sp)) {
                // 如果数据库未初始化 guide 页面，则自动补齐一份默认内容，避免前台空白
                ensureDefaultPage(journalId,
                        "guide",
                        "用户指南（Guide for authors）",
                        defaultGuideHtml());
                loadPage(req, journalId, "guide");
                req.getRequestDispatcher("/WEB-INF/jsp/public/guide.jsp").forward(req, resp);
                return;
            }

            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException("加载静态页面失败", e);
        }
    }

    private void loadPage(HttpServletRequest req, Integer journalId, String pageKey) throws SQLException {
        JournalPage page;
        if (journalId != null) {
            page = journalPageDAO.findByJournalAndKey(journalId, pageKey);
            // 兼容：若主期刊没有配置该页面，尝试按 key 找“任意期刊”的同名页面做兜底展示
            if (page == null) {
                page = journalPageDAO.findFirstJournalByKey(pageKey);
            }
        } else {
            page = journalPageDAO.findFirstJournalByKey(pageKey);
        }
        req.setAttribute("page", page);
        if (page == null) {
            req.setAttribute("pageLoadError", "暂无页面内容。");
        }
    }

    /**
     * 课程设计场景：很多同学只跑了建表脚本但没有初始化 JournalPages，导致 /publish /guide 为空。
     * 这里在首次访问时自动插入默认页面（如已存在则不动），方便“开箱即用”。
     */
    private void ensureDefaultPage(Integer journalId, String pageKey, String title, String html) throws SQLException {
        if (journalId == null) return;
        JournalPage exists = journalPageDAO.findByJournalAndKey(journalId, pageKey);
        if (exists != null) return;

        JournalPage p = new JournalPage();
        p.setJournalId(journalId);
        p.setPageKey(pageKey);
        p.setTitle(title);
        p.setContent(html);
        p.setUpdatedAt(LocalDateTime.now());
        journalPageDAO.insert(p);
    }

    private String defaultGuideHtml() {
        return "" +
                "<p>本页用于向作者提供投稿指南与写作/格式要求。编辑部可在后台 <b>期刊管理 → 关于期刊页面</b> 中随时修改本页内容。</p>" +
                "<h3>一、投稿准备</h3>" +
                "<ul>" +
                "  <li>确认研究主题符合期刊范围（Aims &amp; Scope）。</li>" +
                "  <li>准备作者信息、单位、基金与通讯作者邮箱。</li>" +
                "  <li>整理正文、图表、补充材料与数据/代码链接（如有）。</li>" +
                "</ul>" +
                "<h3>二、写作与格式</h3>" +
                "<ul>" +
                "  <li>摘要建议包含：背景 / 方法 / 结果 / 结论；关键词 3–6 个。</li>" +
                "  <li>图表清晰，图题与注释完整；引用数据需注明来源。</li>" +
                "  <li>参考文献格式统一（作者-年份或顺序编码均可，按期刊要求）。</li>" +
                "</ul>" +
                "<h3>三、提交与后续流程</h3>" +
                "<ul>" +
                "  <li>在线提交后，稿件进入编辑部形式审查；通过后进入外审流程。</li>" +
                "  <li>外审意见返回后，作者可按要求提交修改稿。</li>" +
                "  <li>最终由主编做出录用/退稿决定。</li>" +
                "</ul>";
    }

    private String defaultPublishHtml() {
        return "" +
                "<p>本页用于展示期刊出版与投稿相关信息。编辑部可在后台 <b>期刊管理 → 关于期刊页面</b> 中随时修改本页内容。</p>" +
                "<h3>出版信息</h3>" +
                "<ul>" +
                "  <li>出版周期：以期刊基本信息为准（可在后台编辑）。</li>" +
                "  <li>在线优先：录用后可优先在线发表（如启用）。</li>" +
                "</ul>" +
                "<h3>投稿入口</h3>" +
                "<p>点击页面右上角 <b>提交论文</b> 或使用本页按钮进入投稿流程。</p>";
    }
}
