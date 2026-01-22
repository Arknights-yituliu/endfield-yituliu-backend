package org.yituliu.util;

import org.yituliu.common.utils.LogUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * URL编码工具类
 * 提供URL编码判断和智能编码功能
 * @author 山桜
 */
public class UrlEncodeUtil {

    /**
     * 判断字符串是否已经被URL编码过
     * 通过尝试解码并检查是否产生变化来判断编码状态
     * @param str 要判断的字符串
     * @return true表示已编码，false表示未编码
     */
    public static boolean isUrlEncoded(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }


            // 尝试解码字符串
            String decoded = URLDecoder.decode(str, StandardCharsets.UTF_8);

            // 检查是否包含需要编码但未编码的特殊字符
            // 如果包含这些字符但字符串没有变化，说明未编码
        return !containsSpecialCharacters(str);

            // 如果解码后的字符串与原字符串不同，说明原字符串是编码过的
            // 或者如果解码后包含特殊字符但原字符串不包含，也说明是编码过的
//            if (!decoded.equals(str)) {
//                System.out.println("解码后不同");
//                return true;
//            }
//
//            // 检查是否包含URL编码特征字符
//            // URL编码通常包含%后跟两个十六进制数字
//            if (str.matches(".*%[0-9A-Fa-f]{2}.*")) {
//                System.out.println("包含特征字符");
//                return true;
//            }
    }

    /**
     * 智能URL编码处理
     * 如果字符串已经编码过，直接返回原字符串；如果未编码，进行URL编码
     * @param str 要处理的字符串
     * @return 处理后的字符串
     */
    public static String smartUrlEncode(String str) {
        if (str == null || str.trim().isEmpty()) {
            return str;
        }
        if (isUrlEncoded(str)) {
//            LogUtils.info("已编码，返回原字符串");
            // 如果已经编码过，直接返回原字符串（不进行解码）
            return str;
        } else {
            // 如果未编码，进行URL编码
//            LogUtils.info("未编码，进行URL编码");
            return URLEncoder.encode(str, StandardCharsets.UTF_8);
        }
    }

    /**
     * 检查字符串是否包含需要URL编码的特殊字符
     * @param str 要检查的字符串
     * @return true表示包含特殊字符
     */
    private static boolean containsSpecialCharacters(String str) {
        // URL中需要编码的特殊字符（RFC 3986保留字符 + 不安全字符）
        // 保留字符: ! * ' ( ) ; : @ & = + $ , / ? % # [ ]
        // 不安全字符: 空格 " < > # % { } | \ ^ ~ [ ] `
        String specialChars = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
        for (char c : specialChars.toCharArray()) {
            if (str.indexOf(c) >= 0) {
                return true;
            }
        }

        // // 检查非ASCII字符（Unicode字符）
        // for (char c : str.toCharArray()) {
        //     if (c > 127) {
        //         return true;
        //     }
        // }

        // // 检查控制字符（ASCII 0-31和127）
        // for (char c : str.toCharArray()) {
        //     if (c <= 31 || c == 127) {
        //         return true;
        //     }
        // }

        return false;
    }

    /**
     * 通过特征判断字符串是否可能是URL编码的
     * @param str 要判断的字符串
     * @return true表示可能是URL编码的
     */
    private static boolean isLikelyUrlEncoded(String str) {
        // 检查是否包含URL编码模式：%后跟两个十六进制数字
        if (str.matches(".*%[0-9A-Fa-f]{2}.*")) {
            return true;
        }

        // 检查是否包含常见的编码字符
        String[] encodedPatterns = {"%20", "%2F", "%3F", "%3D", "%26", "%2B", "%25"};
        for (String pattern : encodedPatterns) {
            if (str.contains(pattern)) {
                return true;
            }
        }

        // 检查是否包含空格但未编码（空格在URL中应该被编码为%20或+）
        if (str.contains(" ") && !str.contains("%20") && !str.contains("+")) {
            return false;
        }

        return false;
    }

    /**
     * 强制URL编码（无论是否已编码都进行编码）
     * @param str 要编码的字符串
     * @return URL编码后的字符串
     */
    public static String forceUrlEncode(String str) {
        if (str == null || str.trim().isEmpty()) {
            return str;
        }

        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * 强制URL解码（无论是否已编码都尝试解码）
     * @param str 要解码的字符串
     * @return URL解码后的字符串
     */
    public static String forceUrlDecode(String str) {
        if (str == null || str.trim().isEmpty()) {
            return str;
        }

        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return str;
        }
    }
}