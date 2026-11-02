package org.yituliu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yituliu.common.enums.RecordType;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.IpUtil;
import org.yituliu.common.utils.LogUtils;
import org.yituliu.common.utils.UserAgentUtil;
import org.yituliu.entity.dto.AccessLogDTO;
import org.yituliu.entity.dto.UrlCountDTO;
import org.yituliu.entity.po.AccessLog;
import org.yituliu.entity.po.AccessLogHourlyStats;
import org.yituliu.entity.po.AccessLogHourlyStatsTask;
import org.yituliu.entity.po.AccessLogUrlDailyStats;
import org.yituliu.entity.po.AccessLogUrlDailyStatsTask;
import org.yituliu.entity.po.TrafficStats;
import org.yituliu.entity.vo.UrlPeriodDataVO;
import org.yituliu.entity.vo.UrlTotalVisitVO;
import org.yituliu.entity.vo.UrlVisitGroupVO;
import org.yituliu.mapper.AccessLogHourlyStatsMapper;
import org.yituliu.mapper.AccessLogHourlyStatsTaskMapper;
import org.yituliu.mapper.AccessLogMapper;
import org.yituliu.mapper.AccessLogUrlDailyStatsMapper;
import org.yituliu.mapper.AccessLogUrlDailyStatsTaskMapper;
import org.yituliu.mapper.TrafficStatsMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    /**
     * 查询接口返回的 Top URL 数量
     */
    private static final int TOP_URL_COUNT = 30;

    /**
     * 查询允许的最大时间范围（40 天）
     */
    private static final long MAX_RANGE_MILLIS = 40L * 24 * 60 * 60 * 1000;

    /**
     * 单日统计最多落库的 URL 条数（按访问量取 Top N）
     * url 由客户端上报，基数不受控；全量落库会让日统计表和区间查询的内存占用不可控。
     * 取 200 而不是接口展示的 30：接口取的是区间内累计 Top30，
     * 只留 30 会让"每天都排 31 名、但每天榜单都换人"的 URL 在跨区间累计时被误删。
     */
    private static final int URL_DAILY_STATS_MAX_ROWS = 200;

    /**
     * 小时维度格式 yyyy-MM-dd HH:00
     * 使用不可变、线程安全的 DateTimeFormatter，避免 SimpleDateFormat 并发格式化错乱
     */
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");

    /**
     * 天维度格式 yyyy-MM-dd
     */
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AccessLogMapper accessLogMapper;
    private final TrafficStatsMapper trafficStatsMapper;
    private final AccessLogHourlyStatsMapper accessLogHourlyStatsMapper;
    private final AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper;
    private final AccessLogUrlDailyStatsMapper accessLogUrlDailyStatsMapper;
    private final AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper;
    private final IdGenerator idGenerator;

    public AdminService(AccessLogMapper accessLogMapper,
                        TrafficStatsMapper trafficStatsMapper,
                        AccessLogHourlyStatsMapper accessLogHourlyStatsMapper,
                        AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper,
                        AccessLogUrlDailyStatsMapper accessLogUrlDailyStatsMapper,
                        AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper) {
        this.accessLogMapper = accessLogMapper;
        this.trafficStatsMapper = trafficStatsMapper;
        this.accessLogHourlyStatsMapper = accessLogHourlyStatsMapper;
        this.accessLogHourlyStatsTaskMapper = accessLogHourlyStatsTaskMapper;
        this.accessLogUrlDailyStatsMapper = accessLogUrlDailyStatsMapper;
        this.accessLogUrlDailyStatsTaskMapper = accessLogUrlDailyStatsTaskMapper;
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
     * 直接读取 access_log_url_daily_stats 预聚合表中 recordCode=DISPLAY 的记录，避免实时全表扫描
     * 按URL总访问量降序返回Top 30 URL，零值补齐
     * 注意：当天数据需等定时任务统计后才完整，可能缺失或偏低
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return URL分组访问量列表，每个URL包含其所有天数的数据
     */
    public List<UrlVisitGroupVO> getUrlDailyVisits(Date startTime, Date endTime) {
        validateRange(startTime, endTime);

        // 查询区间内已统计的每日URL数据，stat_day 从起始日00:00到结束日00:00（闭区间），仅取展示数据
        Date startDay = truncateToDay(startTime);
        Date endDay = truncateToDay(endTime);

        List<AccessLogUrlDailyStats> stats = accessLogUrlDailyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .ge(AccessLogUrlDailyStats::getStatDay, startDay)
                        .le(AccessLogUrlDailyStats::getStatDay, endDay)
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // url → (day → count)
        Map<String, Map<String, Long>> urlDayCount = new HashMap<>();
        for (AccessLogUrlDailyStats stat : stats) {
            String day = formatDay(stat.getStatDay());
            urlDayCount
                    .computeIfAbsent(stat.getUrl(), k -> new HashMap<>())
                    .merge(day, stat.getVisitCount(), Long::sum);
        }

        // 按总访问量降序排序，取前30个URL
        List<String> topUrls = urlDayCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue().values().stream().mapToLong(Long::longValue).sum(),
                        a.getValue().values().stream().mapToLong(Long::longValue).sum()))
                .limit(TOP_URL_COUNT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 生成时间范围内全部日期列表（yyyy-MM-dd）
        List<String> allDays = generateAllDays(startTime, endTime);

        // 展开：每个Top URL → 一个分组对象，包含每日的数据，无数据的填0
        List<UrlVisitGroupVO> result = new ArrayList<>(topUrls.size());
        for (String url : topUrls) {
            Map<String, Long> dayCount = urlDayCount.getOrDefault(url, Collections.emptyMap());
            List<UrlPeriodDataVO> dataList = new ArrayList<>(allDays.size());
            for (String day : allDays) {
                dataList.add(new UrlPeriodDataVO(day, dayCount.getOrDefault(day, 0L)));
            }
            result.add(new UrlVisitGroupVO(url, dataList));
        }

        return result;
    }

    /**
     * 统计指定时间范围内每个URL的总访问次数
     * 直接读取 access_log_url_daily_stats 预聚合表中 recordCode=DISPLAY 的记录聚合得出，避免实时全表扫描
     * Top 15 单独列出，其余合并为"其他"
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return URL总访问量列表
     */
    public List<UrlTotalVisitVO> getUrlTotalVisits(Date startTime, Date endTime) {
        validateRange(startTime, endTime);

        // 查询区间内已统计的每日URL数据
        Date startDay = truncateToDay(startTime);
        Date endDay = truncateToDay(endTime);

        List<AccessLogUrlDailyStats> stats = accessLogUrlDailyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .ge(AccessLogUrlDailyStats::getStatDay, startDay)
                        .le(AccessLogUrlDailyStats::getStatDay, endDay)
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // 按URL聚合区间内总访问量
        Map<String, Long> urlCount = new HashMap<>();
        for (AccessLogUrlDailyStats stat : stats) {
            urlCount.merge(stat.getUrl(), stat.getVisitCount(), Long::sum);
        }

        // 按总访问量降序排序全部URL
        List<Map.Entry<String, Long>> sortedEntries = urlCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<UrlTotalVisitVO> result = new ArrayList<>();

        // Top 15 单独列出
        for (int i = 0; i < Math.min(15, sortedEntries.size()); i++) {
            Map.Entry<String, Long> e = sortedEntries.get(i);
            result.add(new UrlTotalVisitVO(e.getKey(), e.getValue()));
        }

        // 剩余URL合并为"其他"
        if (sortedEntries.size() > 15) {
            long otherCount = 0;
            for (int i = 15; i < sortedEntries.size(); i++) {
                otherCount += sortedEntries.get(i).getValue();
            }
            result.add(new UrlTotalVisitVO("其他", otherCount));
        }

        return result;
    }

    /**
     * 统计指定时间范围内每小时的总访问量（所有URL聚合）
     * 直接读取 access_log_hourly_stats 预聚合表中 recordCode=DISPLAY 的记录，避免实时全表扫描，零值补齐
     * 注意：当前进行中的小时尚未被定时任务统计，可能不在结果中或为0
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 每小时总访问量列表
     */
    public List<UrlPeriodDataVO> getHourlyTotalVisits(Date startTime, Date endTime) {
        validateRange(startTime, endTime);

        // 查询区间内已统计的小时数据，stat_hour >= 起始整点 且 <= 结束整点，仅取展示数据
        Date startHour = truncateToHour(startTime);
        Date endHour = truncateToHour(endTime);

        List<AccessLogHourlyStats> stats = accessLogHourlyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogHourlyStats>()
                        .ge(AccessLogHourlyStats::getStatHour, startHour)
                        .le(AccessLogHourlyStats::getStatHour, endHour)
                        .eq(AccessLogHourlyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // stat_hour → visit_count
        Map<String, Long> hourCount = new HashMap<>();
        for (AccessLogHourlyStats stat : stats) {
            hourCount.put(formatHour(stat.getStatHour()), stat.getVisitCount());
        }

        // 生成时间范围内全部整点小时列表，无数据的小时填0
        List<String> allHours = generateAllHours(startTime, endTime);
        List<UrlPeriodDataVO> result = new ArrayList<>(allHours.size());
        for (String hour : allHours) {
            result.add(new UrlPeriodDataVO(hour, hourCount.getOrDefault(hour, 0L)));
        }

        return result;
    }

    /**
     * 统计指定时间范围内每日的总访问量（所有URL聚合）
     * 直接读取 access_log_url_daily_stats 预聚合表按天聚合，避免实时全表扫描，零值补齐
     * 与按URL的总访问量接口使用同一张日统计表，保证两个接口口径一致
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 每日总访问量列表
     */
    public List<UrlPeriodDataVO> getDailyTotalVisits(Date startTime, Date endTime) {
        validateRange(startTime, endTime);

        // 查询区间内已统计的每日URL数据
        Date startDay = truncateToDay(startTime);
        Date endDay = truncateToDay(endTime);

        List<AccessLogUrlDailyStats> stats = accessLogUrlDailyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .ge(AccessLogUrlDailyStats::getStatDay, startDay)
                        .le(AccessLogUrlDailyStats::getStatDay, endDay)
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // 按天汇总各URL访问量
        Map<String, Long> dayCount = new HashMap<>();
        for (AccessLogUrlDailyStats stat : stats) {
            String day = formatDay(stat.getStatDay());
            dayCount.merge(day, stat.getVisitCount(), Long::sum);
        }

        // 生成时间范围内全部日期列表，无数据的日期填0
        List<String> allDays = generateAllDays(startTime, endTime);
        List<UrlPeriodDataVO> result = new ArrayList<>(allDays.size());
        for (String day : allDays) {
            result.add(new UrlPeriodDataVO(day, dayCount.getOrDefault(day, 0L)));
        }

        return result;
    }

    /**
     * 定时统计上一完整小时的访问量并写入 access_log_hourly_stats 表
     * 计算上一完整小时的起止时间后委托给核心统计方法执行
     *
     * @return 本次统计写入的访问量
     */
    @Transactional(rollbackFor = Exception.class)
    public long statisticsLastHour() {
        // 计算上一完整小时的起止时间：[上一小时整点, 当前整点)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endHour = cal.getTime();          // 当前整点 = 上一小时的结束边界
        cal.add(Calendar.HOUR_OF_DAY, -1);
        Date startHour = cal.getTime();        // 上一小时起始

        return statisticsLastHour(startHour, endHour);
    }

    /**
     * 统计指定时间区间 [startHour, endHour) 的访问量并写入 access_log_hourly_stats 表
     * startHour 作为统计小时的整点标识写入 stat_hour 字段
     * 流程：
     * 1. 查询任务记录表 access_log_hourly_stats_task 当前小时是否执行过
     * 2. 若执行过：将 access_log_hourly_stats 中对应旧 task_id 的记录 recordCode 置为 EXPIRE
     * 3. 生成新 task_id，写入新的统计数据（recordCode=DISPLAY）
     * 4. 更新/插入任务记录表，使其 task_id 指向新值
     * 该流程保证可重跑、可补偿，接口仅读到最新 DISPLAY 数据
     *
     * @param startHour 统计起始时间（整点，含），同时作为 stat_hour 标识
     * @param endHour   统计结束时间（不含）
     * @return 本次统计写入的访问量
     */
    @Transactional(rollbackFor = Exception.class)
    public long statisticsLastHour(Date startHour, Date endHour) {
        // 强制将 startHour 对齐到整点，保证 stat_hour 标识的唯一性与匹配一致性
        startHour = truncateToHour(startHour);

        // 复用现成的 PV 统计 SQL
        Long count = accessLogMapper.countPageViews(startHour, endHour);
        long visitCount = count != null ? count : 0L;

        // 1. 查询任务记录表：当前小时是否已执行过
        AccessLogHourlyStatsTask oldTask = accessLogHourlyStatsTaskMapper.selectOne(
                new LambdaQueryWrapper<AccessLogHourlyStatsTask>()
                        .eq(AccessLogHourlyStatsTask::getStatHour, startHour)
        );

        // 2. 若执行过，将旧 task_id 对应的统计数据标记为过期
        if (oldTask != null) {
            AccessLogHourlyStats expireUpdate = new AccessLogHourlyStats();
            expireUpdate.setRecordCode(RecordType.EXPIRE.code());
            int expiredRows = accessLogHourlyStatsMapper.update(
                    expireUpdate,
                    new LambdaQueryWrapper<AccessLogHourlyStats>()
                            .eq(AccessLogHourlyStats::getTaskId, oldTask.getTaskId())
                            .eq(AccessLogHourlyStats::getRecordCode, RecordType.DISPLAY.code())
            );
            if (expiredRows > 0) {
                LogUtils.info("小时访问量统计：{} 旧数据 {} 条已标记为过期", formatHour(startHour), expiredRows);
            }
        }

        // 3. 生成新 task_id，写入新的统计数据
        long newTaskId = idGenerator.nextId();
        AccessLogHourlyStats newStats = new AccessLogHourlyStats();
        newStats.setId(idGenerator.nextId());
        newStats.setStatHour(startHour);
        newStats.setVisitCount(visitCount);
        newStats.setTaskId(newTaskId);
        newStats.setRecordCode(RecordType.DISPLAY.code());
        accessLogHourlyStatsMapper.insert(newStats);

        // 4. 更新/插入任务记录表，task_id 指向新值
        if (oldTask != null) {
            oldTask.setTaskId(newTaskId);
            oldTask.setDataCount(visitCount);
            oldTask.setUpdateTime(new Date());
            accessLogHourlyStatsTaskMapper.updateById(oldTask);
        } else {
            AccessLogHourlyStatsTask newTask = new AccessLogHourlyStatsTask();
            newTask.setId(idGenerator.nextId());
            newTask.setStatHour(startHour);
            newTask.setTaskId(newTaskId);
            newTask.setDataCount(visitCount);
            accessLogHourlyStatsTaskMapper.insert(newTask);
        }

        LogUtils.info("每小时访问量统计完成：{} 访问量 {} task_id {}", formatHour(startHour), visitCount, newTaskId);
        return visitCount;
    }

    /**
     * 定时统计昨天的每个URL访问量并写入 access_log_url_daily_stats 表
     * 计算昨天的起止时间后委托给核心统计方法执行
     *
     * @return 本次统计写入的URL数量
     */
    @Transactional(rollbackFor = Exception.class)
    public long statisticsYesterdayUrlDailyVisits() {
        // 计算昨天的起止时间：[昨天00:00, 今天00:00)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endDay = cal.getTime();          // 今天00:00 = 昨天的结束边界
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date startDay = cal.getTime();        // 昨天00:00

        return statisticsUrlDailyVisits(startDay, endDay);
    }

    /**
     * 定时统计今天的每个URL访问量并写入 access_log_url_daily_stats 表
     * 今天的数据仍在持续产生，定时任务会反复重跑刷新今天的最新数据
     *
     * @return 本次统计写入的URL数量
     */
    @Transactional(rollbackFor = Exception.class)
    public long statisticsTodayUrlDailyVisits() {
        // 计算今天的起止时间：[今天00:00, 明天00:00)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDay = cal.getTime();        // 今天00:00
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date endDay = cal.getTime();          // 明天00:00 = 今天的结束边界

        return statisticsUrlDailyVisits(startDay, endDay);
    }

    /**
     * 统计指定日期区间 [startDay, endDay) 内每个URL的访问量并写入 access_log_url_daily_stats 表
     * startDay 作为统计日期的标识写入 stat_day 字段（强制对齐到当天零点）
     * 流程（与小时统计一致）：
     * 1. 查询任务记录表 access_log_url_daily_stats_task 该日是否执行过
     * 2. 若执行过：将 access_log_url_daily_stats 中对应旧 task_id 的记录 recordCode 置为 EXPIRE
     * 3. 生成新 task_id，按URL分组统计访问量并写入（recordCode=DISPLAY）
     * 4. 更新/插入任务记录表，使其 task_id 指向新值
     *
     * @param startDay 统计起始日期（当天00:00，含），同时作为 stat_day 标识
     * @param endDay   统计结束日期（当天00:00，不含）
     * @return 本次统计写入的URL数量
     */
    @Transactional(rollbackFor = Exception.class)
    public long statisticsUrlDailyVisits(Date startDay, Date endDay) {
        // 强制将 startDay 对齐到当天零点，保证 stat_day 标识的唯一性与匹配一致性
        startDay = truncateToDay(startDay);

        // 按URL分组统计区间内的访问量，取访问量最高的前 N 个URL
        // 这里仍走一次 normalizeUrl 是为了兼容改造前的历史行（带结尾斜杠）
        List<UrlCountDTO> urlCounts = accessLogMapper.countByUrl(startDay, endDay, URL_DAILY_STATS_MAX_ROWS);
        Map<String, Long> urlCountMap = new HashMap<>();
        for (UrlCountDTO dto : urlCounts) {
            String url = normalizeUrl(dto.getUrl());
            urlCountMap.merge(url, dto.getCount(), Long::sum);
        }

        // 1. 查询任务记录表：该日是否已执行过
        AccessLogUrlDailyStatsTask oldTask = accessLogUrlDailyStatsTaskMapper.selectOne(
                new LambdaQueryWrapper<AccessLogUrlDailyStatsTask>()
                        .eq(AccessLogUrlDailyStatsTask::getStatDay, startDay)
        );

        // 2. 若执行过，将旧 task_id 对应的统计数据标记为过期
        if (oldTask != null) {
            AccessLogUrlDailyStats expireUpdate = new AccessLogUrlDailyStats();
            expireUpdate.setRecordCode(RecordType.EXPIRE.code());
            int expiredRows = accessLogUrlDailyStatsMapper.update(
                    expireUpdate,
                    new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                            .eq(AccessLogUrlDailyStats::getTaskId, oldTask.getTaskId())
                            .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
            );
            if (expiredRows > 0) {
                LogUtils.info("URL每日访问量统计：{} 旧数据 {} 条已标记为过期", formatDay(startDay), expiredRows);
            }
        }

        // 3. 生成新 task_id，批量写入新的统计数据
        long newTaskId = idGenerator.nextId();
        if (!urlCountMap.isEmpty()) {
            List<AccessLogUrlDailyStats> batch = new ArrayList<>(urlCountMap.size());
            for (Map.Entry<String, Long> entry : urlCountMap.entrySet()) {
                AccessLogUrlDailyStats stats = new AccessLogUrlDailyStats();
                stats.setId(idGenerator.nextId());
                stats.setUrl(entry.getKey());
                stats.setStatDay(startDay);
                stats.setVisitCount(entry.getValue());
                stats.setTaskId(newTaskId);
                stats.setRecordCode(RecordType.DISPLAY.code());
                batch.add(stats);
            }
            accessLogUrlDailyStatsMapper.insertBatch(batch);
        }

        // 4. 更新/插入任务记录表，task_id 指向新值
        if (oldTask != null) {
            oldTask.setTaskId(newTaskId);
            oldTask.setDataCount((long) urlCountMap.size());
            oldTask.setUpdateTime(new Date());
            accessLogUrlDailyStatsTaskMapper.updateById(oldTask);
        } else {
            AccessLogUrlDailyStatsTask newTask = new AccessLogUrlDailyStatsTask();
            newTask.setId(idGenerator.nextId());
            newTask.setStatDay(startDay);
            newTask.setTaskId(newTaskId);
            newTask.setDataCount((long) urlCountMap.size());
            accessLogUrlDailyStatsTaskMapper.insert(newTask);
        }

        LogUtils.info("URL每日访问量统计完成：{} 共 {} 个URL task_id {}", formatDay(startDay), urlCountMap.size(), newTaskId);
        return urlCountMap.size();
    }

    /**
     * 查询区间 [startHour, endHour) 内已完成小时统计的整点集合
     * 以任务记录表为准，供回填任务跳过已统计的小时，避免重复统计
     *
     * @param startHour 起始整点（含）
     * @param endHour   结束整点（不含）
     * @return 已统计整点的时间戳集合（epoch 毫秒）
     */
    public Set<Long> getStatisticHourlyCoverage(Date startHour, Date endHour) {
        List<AccessLogHourlyStatsTask> tasks = accessLogHourlyStatsTaskMapper.selectList(
                new LambdaQueryWrapper<AccessLogHourlyStatsTask>()
                        .ge(AccessLogHourlyStatsTask::getStatHour, truncateToHour(startHour))
                        .lt(AccessLogHourlyStatsTask::getStatHour, truncateToHour(endHour))
                        .select(AccessLogHourlyStatsTask::getStatHour)
        );
        return tasks.stream()
                .map(task -> task.getStatHour().getTime())
                .collect(Collectors.toSet());
    }

    /**
     * 查询区间 [startDay, endDay) 内已完成URL每日统计的日期集合
     * 以任务记录表为准，供回填任务跳过已统计的日期，避免重复统计
     *
     * @param startDay 起始日期（当天00:00，含）
     * @param endDay   结束日期（当天00:00，不含）
     * @return 已统计日期的时间戳集合（epoch 毫秒）
     */
    public Set<Long> getUrlDailyStatCoverage(Date startDay, Date endDay) {
        List<AccessLogUrlDailyStatsTask> tasks = accessLogUrlDailyStatsTaskMapper.selectList(
                new LambdaQueryWrapper<AccessLogUrlDailyStatsTask>()
                        .ge(AccessLogUrlDailyStatsTask::getStatDay, truncateToDay(startDay))
                        .lt(AccessLogUrlDailyStatsTask::getStatDay, truncateToDay(endDay))
                        .select(AccessLogUrlDailyStatsTask::getStatDay)
        );
        return tasks.stream()
                .map(task -> task.getStatDay().getTime())
                .collect(Collectors.toSet());
    }

    /**
     * 删除 access_log_hourly_stats 表中 recordCode=EXPIRE 的过期数据
     * 由定时任务定期调用，避免重跑历史数据堆积
     *
     * @return 删除的记录数
     */
    public int deleteExpireHourlyStats() {
        int deleted = accessLogHourlyStatsMapper.delete(
                new LambdaQueryWrapper<AccessLogHourlyStats>()
                        .eq(AccessLogHourlyStats::getRecordCode, RecordType.EXPIRE.code())
        );
        LogUtils.info("删除过期小时访问量统计数据 {} 条", deleted);
        return deleted;
    }

    /**
     * 删除 access_log_url_daily_stats 表中 recordCode=EXPIRE 的过期数据
     * 由定时任务定期调用，避免重跑历史数据堆积
     *
     * @return 删除的记录数
     */
    public int deleteExpireUrlDailyStats() {
        int deleted = accessLogUrlDailyStatsMapper.delete(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.EXPIRE.code())
        );
        LogUtils.info("删除过期URL每日访问量统计数据 {} 条", deleted);
        return deleted;
    }

    /**
     * 校验查询时间范围：结束时间必须大于开始时间，且跨度不超过40天
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    private void validateRange(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();
        if (diffMillis <= 0) {
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > MAX_RANGE_MILLIS) {
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }
    }

    /**
     * 将时间截断到当天零点（时分秒毫秒置零）
     *
     * @param date 原始时间
     * @return 当天零点
     */
    private Date truncateToDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 将时间截断到整点小时（分秒毫秒置零）
     *
     * @param date 原始时间
     * @return 整点时间
     */
    private Date truncateToHour(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 生成时间范围内所有整点小时的时间字符串列表（包含结束整点）
     * 例如 2026-05-12 10:00 → 2026-05-12 12:00，共3个小时
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
        while (!cal.after(end)) {                                             // 遍历每个整点直到结束时间（含）
            hours.add(formatHour(cal.getTime()));
            cal.add(Calendar.HOUR_OF_DAY, 1);                                 // 下移1小时
        }
        return hours;
    }

    /**
     * 生成时间范围内所有日期的字符串列表（包含结束日期）
     * 例如 2026-05-12 → 2026-05-14，共3个日期
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
        while (!cal.after(end)) {                                             // 遍历每个日期直到结束时间（含）
            days.add(formatDay(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);                                 // 下移1天
        }
        return days;
    }

    /**
     * 按天格式化（yyyy-MM-dd）
     *
     * @param date 时间
     * @return 格式化结果，按 JVM 默认时区换算
     */
    private static String formatDay(Date date) {
        return DAY_FORMAT.format(toLocalDateTime(date));
    }

    /**
     * 按小时格式化（yyyy-MM-dd HH:00）
     *
     * @param date 时间
     * @return 格式化结果，按 JVM 默认时区换算
     */
    private static String formatHour(Date date) {
        return HOUR_FORMAT.format(toLocalDateTime(date));
    }

    /**
     * java.util.Date 转 LocalDateTime，时区与 SimpleDateFormat 默认行为保持一致
     *
     * @param date 时间
     * @return 本地时间
     */
    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
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
