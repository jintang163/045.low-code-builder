package com.lowcode.page.service.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.report.Report;
import com.lowcode.page.entity.report.ReportSchedule;
import com.lowcode.page.mapper.report.ReportScheduleMapper;
import com.lowcode.page.service.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ReportScheduleService extends ServiceImpl<ReportScheduleMapper, ReportSchedule> {

    @Autowired
    private ReportService reportService;

    @Autowired
    private EmailService emailService;

    public ReportSchedule getScheduleDetail(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "定时任务不存在");
        }
        return schedule;
    }

    public List<ReportSchedule> getScheduleList(Long reportId) {
        LambdaQueryWrapper<ReportSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSchedule::getReportId, reportId);
        wrapper.orderByDesc(ReportSchedule::getCreatedTime);
        return list(wrapper);
    }

    public Page<ReportSchedule> getSchedulePage(Integer current, Integer size, Long reportId, String keyword) {
        LambdaQueryWrapper<ReportSchedule> wrapper = new LambdaQueryWrapper<>();
        if (reportId != null) {
            wrapper.eq(ReportSchedule::getReportId, reportId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ReportSchedule::getScheduleName, keyword)
                    .or().like(ReportSchedule::getEmailSubject, keyword);
        }
        wrapper.orderByDesc(ReportSchedule::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportSchedule saveSchedule(ReportSchedule schedule) {
        Report report = reportService.getById(schedule.getReportId());
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表不存在");
        }

        if (schedule.getStatus() == null) {
            schedule.setStatus(0);
        }

        save(schedule);
        return getScheduleDetail(schedule.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportSchedule updateSchedule(ReportSchedule schedule) {
        ReportSchedule existing = getById(schedule.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "定时任务不存在");
        }

        updateById(schedule);
        return getScheduleDetail(schedule.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "定时任务不存在");
        }
        removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportSchedule enableSchedule(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "定时任务不存在");
        }
        schedule.setStatus(1);
        schedule.setNextExecuteTime(calculateNextExecuteTime(schedule.getCronExpression()));
        updateById(schedule);
        return schedule;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportSchedule disableSchedule(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "定时任务不存在");
        }
        schedule.setStatus(0);
        schedule.setNextExecuteTime(null);
        updateById(schedule);
        return schedule;
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeSchedule(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "定时任务不存在");
        }
        log.info("手动执行报表定时任务: {}", schedule.getScheduleName());
        doExecute(schedule);
    }

    @Scheduled(cron = "0 * * * * ?")
    public void scheduledTask() {
        log.debug("检查报表定时任务...");
        LambdaQueryWrapper<ReportSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSchedule::getStatus, 1);
        List<ReportSchedule> schedules = list(wrapper);

        LocalDateTime now = LocalDateTime.now();
        for (ReportSchedule schedule : schedules) {
            if (schedule.getNextExecuteTime() != null &&
                    !now.isBefore(schedule.getNextExecuteTime())) {
                log.info("定时触发报表任务: {}", schedule.getScheduleName());
                doExecute(schedule);
            }
        }
    }

    private void doExecute(ReportSchedule schedule) {
        try {
            File attachment = generateReportAttachment(schedule);
            sendScheduleEmail(schedule, attachment);

            schedule.setLastExecuteTime(LocalDateTime.now());
            schedule.setNextExecuteTime(calculateNextExecuteTime(schedule.getCronExpression()));
            updateById(schedule);

            log.info("报表定时任务执行成功: {}", schedule.getScheduleName());
        } catch (Exception e) {
            log.error("报表定时任务执行失败: {}", schedule.getScheduleName(), e);
        }
    }

    public File generateReportAttachment(ReportSchedule schedule) throws IOException {
        Report report = reportService.getById(schedule.getReportId());
        String reportName = report != null ? report.getReportName() : "report";
        String attachType = schedule.getAttachType() != null ? schedule.getAttachType() : "pdf";

        String fileName = reportName + "_" + System.currentTimeMillis() + "." + attachType;
        File tempFile = File.createTempFile("report_", "." + attachType);

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("报表名称: " + reportName + "\n");
            writer.write("生成时间: " + LocalDateTime.now() + "\n");
            writer.write("附件类型: " + attachType + "\n");
            writer.write("定时任务: " + schedule.getScheduleName() + "\n");
            writer.write("\n");
            writer.write("【模拟报表数据】\n");
            writer.write("这是一个模拟的报表附件文件。\n");
            writer.write("在实际项目中，这里会生成真实的 PDF / Excel / HTML 文件。\n");
        }

        log.info("生成报表附件: {}, 大小: {} bytes", fileName, tempFile.length());
        return tempFile;
    }

    public void sendScheduleEmail(ReportSchedule schedule, File attachment) {
        List<String> recipients = parseEmailList(schedule.getEmailRecipients());
        List<String> ccList = parseEmailList(schedule.getEmailCc());

        if (recipients.isEmpty()) {
            log.warn("邮件收件人为空，跳过发送: {}", schedule.getScheduleName());
            return;
        }

        String subject = schedule.getEmailSubject();
        String content = schedule.getEmailContent();

        emailService.sendEmailWithAttachment(recipients, ccList, subject, content, attachment);
    }

    private List<String> parseEmailList(String emailStr) {
        if (emailStr == null || emailStr.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return Arrays.asList(emailStr.split("[,;，；]"));
    }

    private LocalDateTime calculateNextExecuteTime(String cronExpression) {
        if (cronExpression == null || cronExpression.isEmpty()) {
            return null;
        }
        return LocalDateTime.now().plusMinutes(1);
    }
}
