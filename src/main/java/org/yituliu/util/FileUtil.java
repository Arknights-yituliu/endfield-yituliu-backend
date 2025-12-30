package org.yituliu.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * 文件操作工具类
 * 提供文件保存、读取、删除、复制等常用文件操作功能
 * 
 * @author yituliu
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    
    /**
     * 默认缓冲区大小：8KB
     */
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * 默认字符编码：UTF-8
     */
    private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

    /**
     * 私有构造函数，防止实例化
     */
    private FileUtil() {
        throw new IllegalStateException("工具类不允许实例化");
    }

    /**
     * 保存字符串内容到文件
     * 如果文件不存在会自动创建，如果文件存在会覆盖原有内容
     *
     * @param content 要保存的字符串内容
     * @param filePath 文件路径，可以是相对路径或绝对路径
     * @return boolean 保存成功返回true，失败返回false
     */
    public static boolean saveStringToFile(String content, String filePath) {
        return saveStringToFile(content, filePath, DEFAULT_CHARSET);
    }

    /**
     * 保存字符串内容到文件（指定字符编码）
     *
     * @param content 要保存的字符串内容
     * @param filePath 文件路径
     * @param charset 字符编码，如"UTF-8"、"GBK"等
     * @return boolean 保存成功返回true，失败返回false
     */
    public static boolean saveStringToFile(String content, String filePath, String charset) {
        if (content == null || filePath == null || filePath.trim().isEmpty()) {
            logger.warn("保存文件失败：参数不能为空");
            return false;
        }
        
        try {
            // 创建父目录（如果不存在）
            createParentDirs(filePath);
            
            // 使用Files.write方法保存文件，自动处理编码和文件创建
            Path path = Paths.get(filePath);
            Files.write(path, content.getBytes(charset));
            
            logger.info("文件保存成功：{}，文件大小：{}字节", filePath, content.getBytes(charset).length);
            return true;
            
        } catch (IOException e) {
            logger.error("保存文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 保存字节数组到文件
     *
     * @param bytes 要保存的字节数组
     * @param filePath 文件路径
     * @return boolean 保存成功返回true，失败返回false
     */
    public static boolean saveBytesToFile(byte[] bytes, String filePath) {
        if (bytes == null || filePath == null || filePath.trim().isEmpty()) {
            logger.warn("保存字节数组到文件失败：参数不能为空");
            return false;
        }
        
        try {
            // 创建父目录（如果不存在）
            createParentDirs(filePath);
            
            // 使用Files.write方法保存字节数组
            Path path = Paths.get(filePath);
            Files.write(path, bytes);
            
            logger.info("字节数组保存成功：{}，文件大小：{}字节", filePath, bytes.length);
            return true;
            
        } catch (IOException e) {
            logger.error("保存字节数组到文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 保存文本行列表到文件
     * 每行文本会单独写入文件，自动添加换行符
     *
     * @param lines 文本行列表
     * @param filePath 文件路径
     * @return boolean 保存成功返回true，失败返回false
     */
    public static boolean saveLinesToFile(List<String> lines, String filePath) {
        return saveLinesToFile(lines, filePath, DEFAULT_CHARSET);
    }

    /**
     * 保存文本行列表到文件（指定字符编码）
     *
     * @param lines 文本行列表
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return boolean 保存成功返回true，失败返回false
     */
    public static boolean saveLinesToFile(List<String> lines, String filePath, String charset) {
        if (lines == null || filePath == null || filePath.trim().isEmpty()) {
            logger.warn("保存文本行到文件失败：参数不能为空");
            return false;
        }
        
        try {
            // 创建父目录（如果不存在）
            createParentDirs(filePath);
            
            // 使用Files.write方法保存文本行
            Path path = Paths.get(filePath);
            Files.write(path, lines, StandardCharsets.UTF_8);
            
            logger.info("文本行保存成功：{}，行数：{}", filePath, lines.size());
            return true;
            
        } catch (IOException e) {
            logger.error("保存文本行到文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 追加内容到文件末尾
     * 如果文件不存在会自动创建
     *
     * @param content 要追加的字符串内容
     * @param filePath 文件路径
     * @return boolean 追加成功返回true，失败返回false
     */
    public static boolean appendToFile(String content, String filePath) {
        return appendToFile(content, filePath, DEFAULT_CHARSET);
    }

    /**
     * 追加内容到文件末尾（指定字符编码）
     *
     * @param content 要追加的字符串内容
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return boolean 追加成功返回true，失败返回false
     */
    public static boolean appendToFile(String content, String filePath, String charset) {
        if (content == null || filePath == null || filePath.trim().isEmpty()) {
            logger.warn("追加内容到文件失败：参数不能为空");
            return false;
        }
        
        try {
            // 创建父目录（如果不存在）
            createParentDirs(filePath);
            
            // 使用Files.write方法追加内容，设置StandardOpenOption.APPEND
            Path path = Paths.get(filePath);
            Files.write(path, content.getBytes(charset), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            logger.debug("内容追加成功：{}", filePath);
            return true;
            
        } catch (IOException e) {
            logger.error("追加内容到文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 从文件读取字符串内容
     *
     * @param filePath 文件路径
     * @return String 文件内容，如果读取失败返回null
     */
    public static String readFileToString(String filePath) {
        return readFileToString(filePath, DEFAULT_CHARSET);
    }

    /**
     * 从文件读取字符串内容（指定字符编码）
     *
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return String 文件内容，如果读取失败返回null
     */
    public static String readFileToString(String filePath, String charset) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("读取文件失败：文件路径不能为空");
            return null;
        }
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("文件不存在：{}", filePath);
                return null;
            }
            
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, charset);
            
        } catch (IOException e) {
            logger.error("读取文件失败：{}", filePath, e);
            return null;
        }
    }

    /**
     * 从文件读取字节数组
     *
     * @param filePath 文件路径
     * @return byte[] 文件字节数组，如果读取失败返回null
     */
    public static byte[] readFileToBytes(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("读取文件字节数组失败：文件路径不能为空");
            return null;
        }
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("文件不存在：{}", filePath);
                return null;
            }
            
            return Files.readAllBytes(path);
            
        } catch (IOException e) {
            logger.error("读取文件字节数组失败：{}", filePath, e);
            return null;
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return boolean 文件存在返回true，否则返回false
     */
    public static boolean fileExists(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return boolean 删除成功返回true，失败返回false
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("删除文件失败：文件路径不能为空");
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            
            if (deleted) {
                logger.info("文件删除成功：{}", filePath);
            } else {
                logger.debug("文件不存在，无需删除：{}", filePath);
            }
            
            return deleted;
            
        } catch (IOException e) {
            logger.error("删除文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return boolean 复制成功返回true，失败返回false
     */
    public static boolean copyFile(String sourcePath, String targetPath) {
        if (sourcePath == null || targetPath == null) {
            logger.warn("复制文件失败：源文件路径或目标文件路径不能为空");
            return false;
        }
        
        try {
            // 创建目标文件的父目录
            createParentDirs(targetPath);
            
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);
            
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("文件复制成功：{} -> {}", sourcePath, targetPath);
            return true;
            
        } catch (IOException e) {
            logger.error("复制文件失败：{} -> {}", sourcePath, targetPath, e);
            return false;
        }
    }

    /**
     * 创建目录（包括所有不存在的父目录）
     *
     * @param dirPath 目录路径
     * @return boolean 创建成功返回true，失败返回false
     */
    public static boolean createDirectories(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            logger.warn("创建目录失败：目录路径不能为空");
            return false;
        }
        
        try {
            Path path = Paths.get(dirPath);
            Files.createDirectories(path);
            
            logger.debug("目录创建成功：{}", dirPath);
            return true;
            
        } catch (IOException e) {
            logger.error("创建目录失败：{}", dirPath, e);
            return false;
        }
    }

    /**
     * 获取文件大小（字节）
     *
     * @param filePath 文件路径
     * @return long 文件大小（字节），如果文件不存在返回-1
     */
    public static long getFileSize(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return -1;
        }
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return -1;
            }
            
            return Files.size(path);
            
        } catch (IOException e) {
            logger.error("获取文件大小失败：{}", filePath, e);
            return -1;
        }
    }

    /**
     * 创建文件的父目录（如果不存在）
     * 私有方法，供内部使用
     *
     * @param filePath 文件路径
     */
    private static void createParentDirs(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}