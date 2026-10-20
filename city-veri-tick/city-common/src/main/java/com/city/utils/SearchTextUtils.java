package com.city.utils;

import java.util.Locale;

/**
 * 文本搜索工具类，提供文本搜索相关的辅助方法
 * 该类包含用于标准化搜索关键词和检查文本是否包含关键词的静态方法
 */
public final class SearchTextUtils {
    // 私有构造函数，防止实例化该工具类
    private SearchTextUtils() { }

    /**
     * 标准化搜索关键词
     * 将输入的关键词转换为小写并去除前后空格
     * @param keyword 要标准化的关键词字符串
     * @return 标准化后的关键词，如果输入为null则返回空字符串
     */
    public static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 检查文本是否包含指定的标准化关键词
     * @param value 要搜索的文本内容
     * @param normalizedKeyword 已标准化的关键词
     * @return 如果文本不为null且包含关键词则返回true，否则返回false
     */
    public static boolean containsKeyword(String value, String normalizedKeyword) {
        return value != null && normalizedKeyword != null
                && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }
}
