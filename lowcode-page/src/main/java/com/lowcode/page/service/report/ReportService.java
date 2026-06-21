package com.lowcode.page.service.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.report.Report;
import com.lowcode.page.entity.report.ReportComponent;
import com.lowcode.page.mapper.report.ReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReportService extends ServiceImpl<ReportMapper, Report> {

    @Autowired
    private ReportComponentService componentService;

    public Report getReportDetail(Long id) {
        Report report = getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表不存在");
        }

        LambdaQueryWrapper<ReportComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponent::getReportId, id);
        wrapper.orderByAsc(ReportComponent::getSortOrder);
        List<ReportComponent> components = componentService.list(wrapper);
        report.setComponents(components);

        return report;
    }

    public List<Report> getReportList(Long appId) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getAppId, appId);
        wrapper.orderByDesc(Report::getCreatedTime);
        return list(wrapper);
    }

    public Page<Report> getReportPage(Integer current, Integer size, Long appId, String keyword) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getAppId, appId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Report::getReportName, keyword)
                    .or().like(Report::getReportCode, keyword);
        }
        wrapper.orderByDesc(Report::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Report saveReport(Report report) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReportCode, report.getReportCode());
        wrapper.eq(Report::getAppId, report.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("报表编码已存在");
        }

        save(report);

        if (report.getComponents() != null) {
            for (ReportComponent component : report.getComponents()) {
                if (component.getComponentId() == null || component.getComponentId().isEmpty()) {
                    component.setComponentId(UUID.randomUUID().toString().replace("-", ""));
                }
                component.setReportId(report.getId());
                componentService.save(component);
            }
        }

        return getReportDetail(report.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Report updateReport(Report report) {
        Report existing = getById(report.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表不存在");
        }

        updateById(report);

        LambdaQueryWrapper<ReportComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponent::getReportId, report.getId());
        componentService.remove(wrapper);

        if (report.getComponents() != null) {
            for (ReportComponent component : report.getComponents()) {
                if (component.getComponentId() == null || component.getComponentId().isEmpty()) {
                    component.setComponentId(UUID.randomUUID().toString().replace("-", ""));
                }
                component.setReportId(report.getId());
                componentService.save(component);
            }
        }

        return getReportDetail(report.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long id) {
        Report report = getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表不存在");
        }

        LambdaQueryWrapper<ReportComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponent::getReportId, id);
        componentService.remove(wrapper);

        removeById(id);
    }

    public Report copyReport(Long sourceId, String newName, String newCode) {
        Report source = getReportDetail(sourceId);
        if (source == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "源报表不存在");
        }

        if (newCode == null || newCode.isEmpty()) {
            newCode = source.getReportCode() + "_copy";
        }
        if (newName == null || newName.isEmpty()) {
            newName = source.getReportName() + "_副本";
        }

        LambdaQueryWrapper<Report> codeCheckWrapper = new LambdaQueryWrapper<>();
        codeCheckWrapper.eq(Report::getReportCode, newCode);
        codeCheckWrapper.eq(Report::getAppId, source.getAppId());
        Long count = count(codeCheckWrapper);
        if (count > 0) {
            throw new BusinessException("报表编码已存在");
        }

        Report newReport = new Report();
        newReport.setAppId(source.getAppId());
        newReport.setReportName(newName);
        newReport.setReportCode(newCode);
        newReport.setReportType(source.getReportType());
        newReport.setDescription(source.getDescription());
        newReport.setLayoutType(source.getLayoutType());
        newReport.setPageConfig(source.getPageConfig());
        newReport.setReportConfig(source.getReportConfig());
        newReport.setStatus(0);
        newReport.setVersion("1.0.0");
        newReport.setScheduleConfig(source.getScheduleConfig());
        newReport.setEmailConfig(source.getEmailConfig());
        newReport.setAutoRefresh(source.getAutoRefresh());
        newReport.setRefreshInterval(source.getRefreshInterval());

        return saveReport(newReport);
    }

    public Report publishReport(Long id) {
        Report report = getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表不存在");
        }
        report.setStatus(1);
        updateById(report);
        return report;
    }
}
