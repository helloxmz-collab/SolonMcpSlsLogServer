package com.anker.sls.util;

import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;

public class DateUtil {
   /**
     * 将日期字符串（如yyyy-MM-dd HH:mm:ss）转换为时间戳（秒）
     */
    public static long parseDateToTimestamp(String dateStr) {
        // 如果是时间戳（全为数字，长度为10或13），直接返回
        if (dateStr != null && dateStr.matches("^\\d{10}$")) {
            return Long.parseLong(dateStr);
        }
        // 如果是时间戳（全为数字，长度为13），除以1000
        if (dateStr != null && dateStr.matches("^\\d{13}$")) {
            return Long.parseLong(dateStr) / 1000;
        }
        if (dateStr == null || dateStr.isEmpty()) return 0L;
        List<String> formats = Arrays.asList(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        );
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                return sdf.parse(dateStr).getTime() / 1000;
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("时间格式错误，需为yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss 或 yyyy-MM-dd");
    }

    /**
     * 获取当前时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String getNowStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date());
    }

    /**
     * 获取当前时间减去days天的时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String getBeforeDaysStr(int days) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -days);
        return sdf.format(cal.getTime());
    }

    /**
     * 获取当前时间减去一个月的时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String getBeforeOneMonthStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, -1);
        return sdf.format(cal.getTime());
    }
}
