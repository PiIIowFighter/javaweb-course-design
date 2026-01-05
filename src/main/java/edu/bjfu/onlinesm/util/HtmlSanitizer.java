package edu.bjfu.onlinesm.util;

/**
 * Very small HTML sanitizer for user-generated rich text (Quill/CKEditor/TinyMCE).
 *
 * Notes:
 * - We keep basic formatting tags (p/strong/em/ul/li/br/span/etc).
 * - We remove scripts/styles/iframes/objects/embeds and dangerous attributes/protocols.
 * - This is not a full OWASP-grade sanitizer, but is sufficient to prevent common XSS vectors
 *   in the context of this course project.
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    public static String sanitizeBasic(String html) {
        if (html == null) return null;
        String out = html;

        // Remove script/style blocks
        out = out.replaceAll("(?is)<\\s*(script|style)[^>]*>.*?<\\s*/\\s*\\1\\s*>", "");

        // Remove iframe/object/embed blocks (potentially dangerous)
        out = out.replaceAll("(?is)<\\s*(iframe|object|embed)[^>]*>.*?<\\s*/\\s*\\1\\s*>", "");
        out = out.replaceAll("(?is)<\\s*(iframe|object|embed)[^>]*/\\s*>", "");

        // Drop inline event handlers (onclick=, onload=, ...)
        out = out.replaceAll("(?i)\\s+on\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", "");

        // Neutralize javascript:/data: URLs in href/src
        out = out.replaceAll("(?i)(href|src)\\s*=\\s*(\"|')\\s*javascript:[^\"']*(\"|')", "$1=$2#$3");
        out = out.replaceAll("(?i)(href|src)\\s*=\\s*(\"|')\\s*data:[^\"']*(\"|')", "$1=$2#$3");

        return out;
    }
}
