package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.StoredFile;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * dbo.Files DAO：用于稿件的多附件（如 Cover Letter 附件）。
 */
public class FileDAO {

    public static final String TYPE_COVER_ATTACHMENT = "COVER_ATTACHMENT";

    public void insert(Connection conn, StoredFile f) throws SQLException {
        String sql = "INSERT INTO dbo.Files (FileName, FilePath, FileType, FileSize, UploaderId, ManuscriptId, VersionId) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getFileName());
            ps.setString(2, f.getFilePath());
            ps.setString(3, f.getFileType());
            if (f.getFileSize() == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, f.getFileSize());
            }
            if (f.getUploaderId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, f.getUploaderId());
            }
            ps.setInt(6, f.getManuscriptId());
            ps.setInt(7, f.getVersionId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    f.setFileId(rs.getInt(1));
                }
            }
        }
    }

    public StoredFile findById(int fileId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            return findById(conn, fileId);
        }
    }

    public StoredFile findById(Connection conn, int fileId) throws SQLException {
        String sql = "SELECT FileId, FileName, FilePath, FileType, FileSize, UploadTime, UploaderId, ManuscriptId, VersionId " +
                "FROM dbo.Files WHERE FileId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<StoredFile> findByManuscriptVersionAndType(int manuscriptId, int versionId, String fileType) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            return findByManuscriptVersionAndType(conn, manuscriptId, versionId, fileType);
        }
    }

    public List<StoredFile> findByManuscriptVersionAndType(Connection conn, int manuscriptId, int versionId, String fileType) throws SQLException {
        String sql = "SELECT FileId, FileName, FilePath, FileType, FileSize, UploadTime, UploaderId, ManuscriptId, VersionId " +
                "FROM dbo.Files WHERE ManuscriptId=? AND VersionId=? AND FileType=? ORDER BY FileId ASC";
        List<StoredFile> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setInt(2, versionId);
            ps.setString(3, fileType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 将某个版本的附件复制到新版本（用于“生成新版本但未重新上传附件”时仍能沿用）。
     */
    public void copyByVersionAndType(Connection conn, int manuscriptId, int fromVersionId, int toVersionId, String fileType) throws SQLException {
        String sql = "INSERT INTO dbo.Files (FileName, FilePath, FileType, FileSize, UploaderId, ManuscriptId, VersionId) " +
                "SELECT FileName, FilePath, FileType, FileSize, UploaderId, ManuscriptId, ? " +
                "FROM dbo.Files WHERE ManuscriptId=? AND VersionId=? AND FileType=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, toVersionId);
            ps.setInt(2, manuscriptId);
            ps.setInt(3, fromVersionId);
            ps.setString(4, fileType);
            ps.executeUpdate();
        }
    }

    private StoredFile mapRow(ResultSet rs) throws SQLException {
        StoredFile f = new StoredFile();
        f.setFileId(rs.getInt("FileId"));
        f.setFileName(rs.getString("FileName"));
        f.setFilePath(rs.getString("FilePath"));
        f.setFileType(rs.getString("FileType"));
        long size = rs.getLong("FileSize");
        if (!rs.wasNull()) {
            f.setFileSize(size);
        }
        Timestamp ts = rs.getTimestamp("UploadTime");
        if (ts != null) {
            f.setUploadTime(ts.toLocalDateTime());
        }
        int uploaderId = rs.getInt("UploaderId");
        if (!rs.wasNull()) {
            f.setUploaderId(uploaderId);
        }
        f.setManuscriptId(rs.getInt("ManuscriptId"));
        f.setVersionId(rs.getInt("VersionId"));
        return f;
    }
}
