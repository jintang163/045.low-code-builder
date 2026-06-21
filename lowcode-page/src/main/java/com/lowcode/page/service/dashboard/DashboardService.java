package com.lowcode.page.service.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.dashboard.Dashboard;
import com.lowcode.page.entity.dashboard.DashboardComponent;
import com.lowcode.page.mapper.dashboard.DashboardMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DashboardService extends ServiceImpl<DashboardMapper, Dashboard> {

    @Autowired
    private DashboardComponentService componentService;

    public Dashboard getDashboardDetail(Long id) {
        Dashboard dashboard = getById(id);
        if (dashboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大屏不存在");
        }

        LambdaQueryWrapper<DashboardComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardComponent::getDashboardId, id);
        wrapper.orderByAsc(DashboardComponent::getSortOrder);
        List<DashboardComponent> components = componentService.list(wrapper);
        dashboard.setComponents(components);

        return dashboard;
    }

    public List<Dashboard> getDashboardList(Long appId) {
        LambdaQueryWrapper<Dashboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dashboard::getAppId, appId);
        wrapper.orderByDesc(Dashboard::getCreatedTime);
        return list(wrapper);
    }

    public Page<Dashboard> getDashboardPage(Integer current, Integer size, Long appId, String keyword) {
        LambdaQueryWrapper<Dashboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dashboard::getAppId, appId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Dashboard::getDashboardName, keyword)
                    .or().like(Dashboard::getDashboardCode, keyword);
        }
        wrapper.orderByDesc(Dashboard::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Dashboard saveDashboard(Dashboard dashboard) {
        LambdaQueryWrapper<Dashboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dashboard::getDashboardCode, dashboard.getDashboardCode());
        wrapper.eq(Dashboard::getAppId, dashboard.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("大屏编码已存在");
        }

        save(dashboard);

        if (dashboard.getComponents() != null) {
            for (DashboardComponent component : dashboard.getComponents()) {
                if (component.getComponentId() == null || component.getComponentId().isEmpty()) {
                    component.setComponentId(UUID.randomUUID().toString().replace("-", ""));
                }
                component.setDashboardId(dashboard.getId());
                componentService.save(component);
            }
        }

        return getDashboardDetail(dashboard.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Dashboard updateDashboard(Dashboard dashboard) {
        Dashboard existing = getById(dashboard.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大屏不存在");
        }

        updateById(dashboard);

        LambdaQueryWrapper<DashboardComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardComponent::getDashboardId, dashboard.getId());
        componentService.remove(wrapper);

        if (dashboard.getComponents() != null) {
            for (DashboardComponent component : dashboard.getComponents()) {
                if (component.getComponentId() == null || component.getComponentId().isEmpty()) {
                    component.setComponentId(UUID.randomUUID().toString().replace("-", ""));
                }
                component.setDashboardId(dashboard.getId());
                componentService.save(component);
            }
        }

        return getDashboardDetail(dashboard.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDashboard(Long id) {
        Dashboard dashboard = getById(id);
        if (dashboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大屏不存在");
        }

        LambdaQueryWrapper<DashboardComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardComponent::getDashboardId, id);
        componentService.remove(wrapper);

        removeById(id);
    }

    public Dashboard copyDashboard(Long sourceId, String newName, String newCode) {
        Dashboard source = getDashboardDetail(sourceId);
        if (source == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "源大屏不存在");
        }

        if (newCode == null || newCode.isEmpty()) {
            newCode = source.getDashboardCode() + "_copy";
        }
        if (newName == null || newName.isEmpty()) {
            newName = source.getDashboardName() + "_副本";
        }

        LambdaQueryWrapper<Dashboard> codeCheckWrapper = new LambdaQueryWrapper<>();
        codeCheckWrapper.eq(Dashboard::getDashboardCode, newCode);
        codeCheckWrapper.eq(Dashboard::getAppId, source.getAppId());
        Long count = count(codeCheckWrapper);
        if (count > 0) {
            throw new BusinessException("大屏编码已存在");
        }

        Dashboard newDashboard = new Dashboard();
        newDashboard.setAppId(source.getAppId());
        newDashboard.setDashboardName(newName);
        newDashboard.setDashboardCode(newCode);
        newDashboard.setDescription(source.getDescription());
        newDashboard.setLayoutType(source.getLayoutType());
        newDashboard.setWidth(source.getWidth());
        newDashboard.setHeight(source.getHeight());
        newDashboard.setBackgroundColor(source.getBackgroundColor());
        newDashboard.setBackgroundImage(source.getBackgroundImage());
        newDashboard.setTheme(source.getTheme());
        newDashboard.setStatus(0);
        newDashboard.setVersion("1.0.0");
        newDashboard.setAutoRefresh(source.getAutoRefresh());
        newDashboard.setRefreshInterval(source.getRefreshInterval());
        newDashboard.setCarouselConfig(source.getCarouselConfig());
        newDashboard.setDisplayConfig(source.getDisplayConfig());
        newDashboard.setShareConfig(source.getShareConfig());

        return saveDashboard(newDashboard);
    }

    public Dashboard publishDashboard(Long id) {
        Dashboard dashboard = getById(id);
        if (dashboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大屏不存在");
        }
        dashboard.setStatus(1);
        updateById(dashboard);
        return dashboard;
    }

    public String generateShareLink(Long id) {
        Dashboard dashboard = getById(id);
        if (dashboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大屏不存在");
        }
        return "/screen/display/" + id + "?share=true&code=" + System.currentTimeMillis();
    }
}
