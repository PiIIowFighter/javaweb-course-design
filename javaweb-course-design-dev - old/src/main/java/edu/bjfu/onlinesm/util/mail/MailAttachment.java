package edu.bjfu.onlinesm.util.mail;

/** 简单附件对象（内存字节）。 */
public class MailAttachment {
    private final String filename;
    private final String contentType;
    private final byte[] bytes;

    public MailAttachment(String filename, String contentType, byte[] bytes) {
        this.filename = filename;
        this.contentType = contentType == null || contentType.isEmpty() ? "application/octet-stream" : contentType;
        this.bytes = bytes;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
