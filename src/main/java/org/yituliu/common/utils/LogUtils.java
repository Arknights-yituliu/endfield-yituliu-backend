package org.yituliu.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类，封装SLF4J日志接口
 * 提供便捷的info和error日志记录方法
 * 
 * @author AI Assistant
 */
public class LogUtils {
    
    /**
     * 获取Logger实例
     * @param clazz 调用日志的类
     * @return Logger实例
     */
    private static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * 获取调用当前方法的类
     * @return 调用类的Class对象
     */
    private static Class<?> getCallingClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 索引2是调用getCallingClass()的方法，索引3是调用LogUtils静态方法的类
        if (stackTrace.length > 3) {
            String className = stackTrace[3].getClassName();
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                // 如果获取失败，返回LogUtils类
                return LogUtils.class;
            }
        }
        return LogUtils.class;
    }
    
    /**
     * 记录INFO级别日志
     * @param message 日志消息
     */
    public static void info(String message) {
        getLogger(getCallingClass()).info(message);
    }
    
    /**
     * 记录INFO级别格式化日志
     * @param format 日志格式字符串
     * @param arguments 格式化参数
     */
    public static void info(String format, Object... arguments) {
        getLogger(getCallingClass()).info(format, arguments);
    }
    
    /**
     * 记录INFO级别日志，包含异常信息
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void info(String message, Throwable throwable) {
        getLogger(getCallingClass()).info(message, throwable);
    }
    
    /**
     * 记录ERROR级别日志
     * @param message 日志消息
     */
    public static void error(String message) {
        getLogger(getCallingClass()).error(message);
    }
    
    /**
     * 记录ERROR级别格式化日志
     * @param format 日志格式字符串
     * @param arguments 格式化参数
     */
    public static void error(String format, Object... arguments) {
        getLogger(getCallingClass()).error(format, arguments);
    }
    
    /**
     * 记录ERROR级别日志，包含异常信息
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void error(String message, Throwable throwable) {
        getLogger(getCallingClass()).error(message, throwable);
    }
    
    /**
     * 记录ERROR级别日志，支持异常和格式化参数
     * @param format 日志格式字符串
     * @param throwable 异常对象
     * @param arguments 格式化参数
     */
    public static void error(String format, Throwable throwable, Object... arguments) {
        getLogger(getCallingClass()).error(format, arguments, throwable);
    }
}