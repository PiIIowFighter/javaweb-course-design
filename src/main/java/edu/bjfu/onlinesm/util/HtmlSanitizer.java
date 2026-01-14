package edu.bjfu.onlinesm.util;


public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    public static String sanitizeBasic(String html) {
        if (html == null) return null;
        String out = html;

        
        out = out.replaceAll("(?is)<\\s*(script|style)[^>]*>.*?<\\s*/\\s*\\1\\s*>", "");

        
        out = out.replaceAll("(?is)<\\s*(iframe|object|embed)[^>]*>.*?<\\s*/\\s*\\1\\s*>", "");
        out = out.replaceAll("(?is)<\\s*(iframe|object|embed)[^>]*/\\s*>", "");

        
        out = out.replaceAll("(?i)\\s+on\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", "");

        
        out = out.replaceAll("(?i)(href|src)\\s*=\\s*(\"|')\\s*javascript:[^\"']*(\"|')", "$1=$2#$3");
        out = out.replaceAll("(?i)(href|src)\\s*=\\s*(\"|')\\s*data:[^\"']*(\"|')", "$1=$2#$3");

        return out;
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

