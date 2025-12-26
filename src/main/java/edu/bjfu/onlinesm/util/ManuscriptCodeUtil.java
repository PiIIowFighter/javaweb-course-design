package edu.bjfu.onlinesm.util;

import java.time.LocalDate;

/**
 * 统一生成“稿件编号”显示字符串。
 */
public class ManuscriptCodeUtil {

    private ManuscriptCodeUtil() {}

    public static String code(int manuscriptId) {
        int year = LocalDate.now().getYear();
        return String.format("MS-%d-%03d", year, manuscriptId);
    }
}
