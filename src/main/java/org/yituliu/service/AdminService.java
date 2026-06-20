package org.yituliu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.IpUtil;
import org.yituliu.common.utils.UserAgentUtil;
import org.yituliu.entity.dto.AccessLogDTO;
import org.yituliu.entity.po.AccessLog;
import org.yituliu.entity.po.TrafficStats;
import org.yituliu.entity.vo.UrlPeriodDataVO;
import org.yituliu.entity.vo.UrlTotalVisitVO;
import org.yituliu.entity.vo.UrlVisitGroupVO;
import org.yituliu.mapper.AccessLogMapper;
import org.yituliu.mapper.TrafficStatsMapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final int BATCH_SIZE = 100_000;
    private static final int TOP_URL_COUNT = 30;
    private static final SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:00");
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final AccessLogMapper accessLogMapper;
    private final TrafficStatsMapper trafficStatsMapper;
    private final IdGenerator idGenerator;

    public AdminService(AccessLogMapper accessLogMapper, TrafficStatsMapper trafficStatsMapper) {
        this.accessLogMapper = accessLogMapper;
        this.trafficStatsMapper = trafficStatsMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    /**
     * 保存访问日志
     * 
     * @param request   HTTP请求对象
     * @param accessLog 访问日志对象
     */
    public void saveAccessLog(HttpServletRequest request, AccessLogDTO accessLogDTO) {
        AccessLog accessLog = new AccessLog();

        if (accessLog.getId() == null) {
            accessLog.setId(idGenerator.nextId());
        }

        accessLog.setReferer(request.getHeader("Referer"));
        if (accessLog.getReferer() == null) {
            accessLog.setReferer("Unknown");
        }

        accessLog.setUrl(accessLogDTO.getUrl());
        if(accessLogDTO.getUrl() == null){
            accessLogDTO.setUrl("Empty");
        }

        if (accessLog.getAccessTime() == null) {
            accessLog.setAccessTime(new Date());
        }

        accessLog.setIp(IpUtil.getIpAddress(request));

        accessLog.setBrowser(UserAgentUtil.getBrowser(request));

        accessLog.setOs(UserAgentUtil.getOs(request));

        accessLog.setDevice(UserAgentUtil.getDevice(request));

        if("Unknown".equals(accessLog.getBrowser())||"Unknown".equals(accessLog.getOs())||"Unknown".equals(accessLog.getDevice())){
            accessLog.setUserAgent(UserAgentUtil.getUserAgent(request));
        }

        if (accessLog.getRegion() == null) {
            accessLog.setRegion("Unknown");
        }
        accessLogMapper.insert(accessLog);
    }

    /**
     * 统计1小时内的页面浏览量和独立访客数
     * 
     * @param startTime 统计开始时间
     * @return TrafficStats 流量统计结果
     */
    public TrafficStats calculateHourTrafficStats(Date startTime) {
        // 计算结束时间（开始时间 + 1小时）
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date endTime = calendar.getTime();
        
        // 统计页面浏览量(PV)
        Long pageViews = accessLogMapper.countPageViews(startTime, endTime);
        
        // 统计独立访客数(UV)
        Long uniqueVisitors = accessLogMapper.countUniqueVisitors(startTime, endTime);
        
        // 创建流量统计对象
        TrafficStats trafficStats = new TrafficStats();
        trafficStats.setId(idGenerator.nextId());
        trafficStats.setPageViews(pageViews != null ? pageViews : 0L);
        trafficStats.setUniqueVisitors(uniqueVisitors != null ? uniqueVisitors : 0L);
        trafficStats.setStatStartTime(startTime);
        trafficStats.setStatEndTime(endTime);
        trafficStats.setCreateTime(new Date());
        
        // 保存统计结果到数据库
        trafficStatsMapper.insert(trafficStats);
        
        return trafficStats;
    }

    /**
     * 统计指定时间范围内每个URL每天的访问次数
     * 分批查询数据（每批10万条），Java代码聚合，零值补齐，返回Top 30URL（按URL分组）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return URL分组访问量列表，每个URL包含其所有天数的数据
     */
    public List<UrlVisitGroupVO> getUrlDailyVisits(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();            // 计算时间范围的毫秒差
        if (diffMillis <= 0) {                                                 // 结束时间必须大于开始时间
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > 30L * 24 * 60 * 60 * 1000) {                         // 范围最大不超过30天
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }

        // url → (day → count)
        Map<String, Map<String, Long>> urlDayCount = new HashMap<>();

        long offset = 0;
        while (true) {
            List<AccessLog> batch = accessLogMapper.selectList(               // 每批查询10万条记录
                    new LambdaQueryWrapper<AccessLog>()
                            .select(AccessLog::getUrl, AccessLog::getAccessTime) // 只查url和access_time两列
                            .ge(AccessLog::getAccessTime, startTime)          // access_time >= 开始时间
                            .lt(AccessLog::getAccessTime, endTime)            // access_time < 结束时间
                            .last("LIMIT " + offset + "," + BATCH_SIZE)       // 分页偏移 + 批次大小
            );

            if (batch.isEmpty()) {                                            // 无更多数据时跳出循环
                break;
            }

            for (AccessLog log : batch) {                                     // 逐条聚合到内存Map
                String url = normalizeUrl(log.getUrl());                     // 标准化URL，去除结尾斜杠
                String day = DAY_FORMAT.format(log.getAccessTime());          // 格式化到天级别，如 2026-05-12
                urlDayCount
                        .computeIfAbsent(url, k -> new HashMap<>())           // 该URL首次出现时创建内部Map
                        .merge(day, 1L, Long::sum);                          // 累计该天的访问次数
            }

            offset += BATCH_SIZE;                                             // 偏移量递增，进入下一批
        }

        // 按总访问量降序排序，取前30个URL
        List<String> topUrls = urlDayCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(                               // 按总访问量降序
                        b.getValue().values().stream().mapToLong(Long::longValue).sum(),
                        a.getValue().values().stream().mapToLong(Long::longValue).sum()))
                .limit(TOP_URL_COUNT)                                         // 只取前30个
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 生成时间范围内全部日期列表（yyyy-MM-dd）
        List<String> allDays = generateAllDays(startTime, endTime);

        // 展开：每个Top URL → 一个分组对象，包含所有小时段和每日的数据，无数据的填0
        List<UrlVisitGroupVO> result = new ArrayList<>(topUrls.size());
        for (String url : topUrls) {
            Map<String, Long> dayCount = urlDayCount.getOrDefault(url, Collections.emptyMap());
            List<UrlPeriodDataVO> dataList = new ArrayList<>(allDays.size());
            for (String day : allDays) {
                dataList.add(new UrlPeriodDataVO(day, dayCount.getOrDefault(day, 0L))); // 时间段数据
            }
            result.add(new UrlVisitGroupVO(url, dataList));                  // 每个URL一个分组
        }

        return result;
    }

    /**
     * 统计指定时间范围内每个URL的总访问次数
     * 分批查询（每批10万条），Java代码聚合，返回Top 50 URL
     * 第一项固定为"访问总和"，时间范围最大不超过30天
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return URL总访问量列表
     */
    public List<UrlTotalVisitVO> getUrlTotalVisits(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();            // 计算时间范围的毫秒差
        if (diffMillis <= 0) {                                                 // 结束时间必须大于开始时间
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > 30L * 24 * 60 * 60 * 1000) {                        // 范围最大不超过30天
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }

        Map<String, Long> urlCount = new HashMap<>();                         // url → 总访问次数

        long offset = 0;
        while (true) {
            List<AccessLog> batch = accessLogMapper.selectList(               // 每批查询10万条记录
                    new LambdaQueryWrapper<AccessLog>()
                            .select(AccessLog::getUrl)                         // 只查url一列
                            .ge(AccessLog::getAccessTime, startTime)          // access_time >= 开始时间
                            .lt(AccessLog::getAccessTime, endTime)            // access_time < 结束时间
                            .last("LIMIT " + offset + "," + BATCH_SIZE)       // 分页偏移 + 批次大小
            );

            if (batch.isEmpty()) {                                            // 无更多数据时跳出循环
                break;
            }

            for (AccessLog log : batch) {                                     // 逐条聚合
                String url = normalizeUrl(log.getUrl());                     // 标准化URL，去除结尾斜杠
                urlCount.merge(url, 1L, Long::sum);                          // 累计访问次数
            }

            offset += BATCH_SIZE;                                             // 偏移量递增
        }

        // 按总访问量降序排序，返回Top 50 URL，第一项插入访问总和
        List<UrlTotalVisitVO> result = urlCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // 按总访问量降序
                .limit(50)                                                     // 只取前50个
                .map(e -> new UrlTotalVisitVO(e.getKey(), e.getValue()))       // 转换为VO
                .collect(Collectors.toList());

        long total = urlCount.values().stream().mapToLong(Long::longValue).sum(); // 所有URL访问量总和
        result.add(0, new UrlTotalVisitVO("访问总和", total));               // 插入到列表第一项

        return result;
    }

    /**
     * 统计指定时间范围内每小时的总访问量（所有URL聚合）
     * 分批查询（每批10万条），Java代码聚合，零值补齐
     * 时间范围最大不超过30天
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 每小时总访问量列表
     */
    public List<UrlPeriodDataVO> getHourlyTotalVisits(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();            // 计算时间范围的毫秒差
        if (diffMillis <= 0) {                                                 // 结束时间必须大于开始时间
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > 30L * 24 * 60 * 60 * 1000) {                        // 范围最大不超过30天
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }

        Map<String, Long> hourCount = new HashMap<>();                        // hour → 总访问次数

        long offset = 0;
        while (true) {
            List<AccessLog> batch = accessLogMapper.selectList(               // 每批查询10万条记录
                    new LambdaQueryWrapper<AccessLog>()
                            .select(AccessLog::getAccessTime)                  // 只查access_time一列
                            .ge(AccessLog::getAccessTime, startTime)          // access_time >= 开始时间
                            .lt(AccessLog::getAccessTime, endTime)            // access_time < 结束时间
                            .last("LIMIT " + offset + "," + BATCH_SIZE)       // 分页偏移 + 批次大小
            );

            if (batch.isEmpty()) {                                            // 无更多数据时跳出循环
                break;
            }

            for (AccessLog log : batch) {                                     // 逐条聚合，按小时累计
                String hour = HOUR_FORMAT.format(log.getAccessTime());        // 格式化到小时级别
                hourCount.merge(hour, 1L, Long::sum);                        // 累计该小时的访问次数
            }

            offset += BATCH_SIZE;                                             // 偏移量递增
        }

        // 生成时间范围内全部整点小时列表，无数据的小时填0
        List<String> allHours = generateAllHours(startTime, endTime);
        List<UrlPeriodDataVO> result = new ArrayList<>(allHours.size());
        for (String hour : allHours) {
            result.add(new UrlPeriodDataVO(hour, hourCount.getOrDefault(hour, 0L))); // 填充真实值或0
        }

        return result;
    }

    /**
     * 生成时间范围内所有整点小时的时间字符串列表
     * 例如 2026-05-12 10:00 → 2026-05-14 12:00，共50个小时
     */
    private List<String> generateAllHours(Date startTime, Date endTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        cal.set(Calendar.MINUTE, 0);                                          // 对齐到整点
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        List<String> hours = new ArrayList<>();
        while (cal.before(end)) {                                             // 遍历每个整点直到结束时间
            hours.add(HOUR_FORMAT.format(cal.getTime()));
            cal.add(Calendar.HOUR_OF_DAY, 1);                                 // 下移1小时
        }
        return hours;
    }

    /**
     * 生成时间范围内所有日期的字符串列表
     * 例如 2026-05-12 → 2026-05-15，共4个日期
     */
    private List<String> generateAllDays(Date startTime, Date endTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        cal.set(Calendar.HOUR_OF_DAY, 0);                                     // 对齐到当天0点
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        List<String> days = new ArrayList<>();
        while (cal.before(end)) {                                             // 遍历每个日期直到结束时间
            days.add(DAY_FORMAT.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);                                 // 下移1天
        }
        return days;
    }

    /**
     * 标准化URL，去除结尾斜杠，统一同一路径的带/不带/写法
     * 例如 "/tools/essence-calculator/" → "/tools/essence-calculator"
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return "Unknown";                                                  // URL为空时记为Unknown
        }
        if (url.length() > 1 && url.endsWith("/")) {                         // 长度>1 避免把根路径"/"也去掉
            return url.substring(0, url.length() - 1);                       // 去掉结尾斜杠
        }
        return url;
    }

}
