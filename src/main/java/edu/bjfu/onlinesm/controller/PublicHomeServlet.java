package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.EditorialBoardDAO;
import edu.bjfu.onlinesm.dao.CallForPaperDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.CallForPaper;
import edu.bjfu.onlinesm.model.EditorialBoardMember;
import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.News;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * 前台首页：展示期刊介绍、论文列表、新闻、征稿通知。
 */
@WebServlet(name = "PublicHomeServlet", urlPatterns = {"/home"})
public class PublicHomeServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final EditorialBoardDAO editorialBoardDAO = new EditorialBoardDAO();
    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final NewsDAO newsDAO = new NewsDAO();
    private final CallForPaperDAO callDAO = new CallForPaperDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Journal journal = journalDAO.findPrimary();
            req.setAttribute("journal", journal);

            List<EditorialBoardMember> boardMembers = Collections.emptyList();
if (journal != null && journal.getJournalId() != null) {
    // 首页只展示前 6 位编委
    boardMembers = editorialBoardDAO.findByJournal(journal.getJournalId(), 6);

    // 兼容：若按期刊查询为空，但数据库中实际存在编委（例如 JournalId 不一致/历史数据问题），
    // 则回退到全表查询后再尽量筛选当前期刊，确保首页能展示编委信息。
    if (boardMembers == null || boardMembers.isEmpty()) {
        List<EditorialBoardMember> all = editorialBoardDAO.findAll();
        if (all != null && !all.isEmpty()) {
            // 优先筛选当前期刊的编委
            int jid = journal.getJournalId();
            List<EditorialBoardMember> sameJournal = new java.util.ArrayList<>();
            for (EditorialBoardMember m : all) {
                if (m.getJournalId() == jid) sameJournal.add(m);
            }
            List<EditorialBoardMember> source = !sameJournal.isEmpty() ? sameJournal : all;
            int n = Math.min(6, source.size());
            boardMembers = source.subList(0, n);
        }
    }
}
req.setAttribute("boardMembers", boardMembers);

            List<Manuscript> latestAccepted = manuscriptDAO.findLatestAccepted(8);
            req.setAttribute("latestPublished", latestAccepted);

            List<News> newsList = newsDAO.findPublishedTopN(6);
            req.setAttribute("newsList", newsList);

            // 征稿通知（Call for papers）：读取 dbo.CallForPapers 已发布数据
            if (journal != null && journal.getJournalId() != null) {
                List<CallForPaper> calls = callDAO.listPublished(journal.getJournalId(), 6);
                req.setAttribute("callForPapers", calls);
            } else {
                req.setAttribute("callForPapers", Collections.emptyList());
            }

            req.getRequestDispatcher("/WEB-INF/jsp/public/home.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载首页数据失败", e);
        }
    }
}
