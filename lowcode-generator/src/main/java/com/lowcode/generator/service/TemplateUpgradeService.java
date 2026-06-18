package com.lowcode.generator.service;

import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.util.UserContext;
import com.lowcode.flow.entity.BusinessLogic;
import com.lowcode.flow.entity.LogicEdge;
import com.lowcode.flow.entity.LogicNode;
import com.lowcode.flow.entity.WorkflowDefinition;
import com.lowcode.flow.mapper.BusinessLogicMapper;
import com.lowcode.flow.mapper.LogicEdgeMapper;
import com.lowcode.flow.mapper.LogicNodeMapper;
import com.lowcode.flow.mapper.WorkflowDefinitionMapper;
import com.lowcode.generator.entity.*;
import com.lowcode.generator.mapper.*;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.mapper.DataModelMapper;
import com.lowcode.model.mapper.DataSourceMapper;
import com.lowcode.model.mapper.ModelFieldMapper;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.PageComponent;
import com.lowcode.page.mapper.PageComponentMapper;
import com.lowcode.page.mapper.PageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TemplateUpgradeService {

    @Autowired
    private AppInstallMapper appInstallMapper;

    @Autowired
    private AppTemplateMapper appTemplateMapper;

    @Autowired
    private AppInfoMapper appInfoMapper;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private DataModelMapper dataModelMapper;

    @Autowired
    private ModelFieldMapper modelFieldMapper;

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private PageComponentMapper pageComponentMapper;

    @Autowired
    private BusinessLogicMapper businessLogicMapper;

    @Autowired
    private LogicNodeMapper logicNodeMapper;

    @Autowired
    private LogicEdgeMapper logicEdgeMapper;

    @Autowired
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Autowired
    private TemplateVersionMapper templateVersionMapper;

    @Autowired
    private TemplateDataProvider dataProvider;

    public Map<String, Object> checkUpdate(Long appId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasUpdate", 0);

        LambdaQueryWrapper<AppInstall> installWrapper = new LambdaQueryWrapper<>();
        installWrapper.eq(AppInstall::getAppId, appId);
        AppInstall install = appInstallMapper.selectOne(installWrapper);

        if (install == null) {
            result.put("message", "该应用不是从模板安装的");
            return result;
        }

        AppTemplate template = appTemplateMapper.selectById(install.getTemplateId());
        if (template == null) {
            result.put("message", "模板不存在，可能已被删除");
            return result;
        }

        String currentVersion = install.getCurrentVersion();
        String latestVersion = template.getVersion();

        result.put("currentVersion", currentVersion);
        result.put("latestVersion", latestVersion);
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());

        if (!Objects.equals(currentVersion, latestVersion)) {
            result.put("hasUpdate", 1);
            result.put("message", "有新版本可更新");

            List<TemplateVersion> versions = getTemplateVersions(template.getId());
            List<TemplateVersion> newerVersions = versions.stream()
                    .filter(v -> isNewerVersion(v.getVersion(), currentVersion))
                    .collect(Collectors.toList());
            result.put("newerVersions", newerVersions);

            List<String> changeLogs = newerVersions.stream()
                    .map(v -> "v" + v.getVersion() + ": " + v.getChangeLog())
                    .collect(Collectors.toList());
            result.put("changeLogs", changeLogs);

            try {
                TemplateData oldData = getTemplateDataFromVersion(template.getId(), currentVersion);
                TemplateData newData = JSON.parseObject(template.getTemplateData(), TemplateData.class);
                result.put("diff", compareTemplateDiff(oldData, newData));
            } catch (Exception e) {
                log.warn("比较差异失败: {}", e.getMessage());
            }
        } else {
            result.put("message", "当前已是最新版本");
        }

        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateAppFromTemplate(Long appId, String updateMode) {
        Map<String, Object> result = new LinkedHashMap<>();

        LambdaQueryWrapper<AppInstall> installWrapper = new LambdaQueryWrapper<>();
        installWrapper.eq(AppInstall::getAppId, appId);
        AppInstall install = appInstallMapper.selectOne(installWrapper);

        if (install == null) {
            throw new BusinessException("该应用不是从模板安装的");
        }

        AppTemplate template = appTemplateMapper.selectById(install.getTemplateId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        if (Objects.equals(install.getCurrentVersion(), template.getVersion())) {
            result.put("message", "当前已是最新版本");
            return result;
        }

        TemplateData newData;
        try {
            newData = JSON.parseObject(template.getTemplateData(), TemplateData.class);
        } catch (Exception e) {
            throw new BusinessException("模板数据解析失败");
        }

        Long userId = UserContext.getCurrentUserId();
        int added = 0, updated = 0, skipped = 0;

        if ("full".equals(updateMode)) {
            added += restoreDataSourcesFull(appId, newData.getDataSources(), userId);
            added += restoreDataModelsFull(appId, newData.getDataModels(), userId);
            added += restorePagesFull(appId, newData.getPages(), userId);
            added += restoreLogicsFull(appId, newData.getBusinessLogics(), userId);
            added += restoreWorkflowsFull(appId, newData.getWorkflows(), userId);
            result.put("updateMode", "full");
            result.put("message", "全量覆盖更新完成，所有资源已重置为最新模板版本");
        } else {
            result.put("updateMode", "incremental");

            if (newData.getDataSources() != null && !newData.getDataSources().isEmpty()) {
                int[] r = restoreDataSourcesIncremental(appId, newData.getDataSources(), userId);
                added += r[0]; updated += r[1]; skipped += r[2];
            }
            if (newData.getDataModels() != null && !newData.getDataModels().isEmpty()) {
                int[] r = restoreDataModelsIncremental(appId, newData.getDataModels(), userId);
                added += r[0]; updated += r[1]; skipped += r[2];
            }
            if (newData.getPages() != null && !newData.getPages().isEmpty()) {
                int[] r = restorePagesIncremental(appId, newData.getPages(), userId);
                added += r[0]; updated += r[1]; skipped += r[2];
            }
            if (newData.getBusinessLogics() != null && !newData.getBusinessLogics().isEmpty()) {
                int[] r = restoreLogicsIncremental(appId, newData.getBusinessLogics(), userId);
                added += r[0]; updated += r[1]; skipped += r[2];
            }
            if (newData.getWorkflows() != null && !newData.getWorkflows().isEmpty()) {
                int[] r = restoreWorkflowsIncremental(appId, newData.getWorkflows(), userId);
                added += r[0]; updated += r[1]; skipped += r[2];
            }

            result.put("message", "增量更新完成（已跳过用户自定义修改的资源）");
        }

        install.setCurrentVersion(template.getVersion());
        install.setLastUpdateTime(LocalDateTime.now());
        install.setHasUpdate(0);
        appInstallMapper.updateById(install);

        LambdaUpdateWrapper<AppInfo> appWrapper = new LambdaUpdateWrapper<>();
        appWrapper.eq(AppInfo::getId, appId);
        appWrapper.set(AppInfo::getTemplateVersion, template.getVersion());
        appWrapper.set(AppInfo::getUpdatedBy, userId);
        appWrapper.set(AppInfo::getUpdatedTime, LocalDateTime.now());
        appInfoMapper.update(null, appWrapper);

        result.put("added", added);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("newVersion", template.getVersion());

        log.info("应用模板更新: appId={}, mode={}, added={}, updated={}, skipped={}", appId, updateMode, added, updated, skipped);
        return result;
    }

    public List<TemplateVersion> getTemplateVersions(Long templateId) {
        LambdaQueryWrapper<TemplateVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateVersion::getTemplateId, templateId);
        wrapper.orderByDesc(TemplateVersion::getPublishTime);
        return templateVersionMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createTemplateVersion(Long templateId, String version, String changeLog, String templateData, Long publishedBy) {
        TemplateVersion tv = new TemplateVersion();
        tv.setTemplateId(templateId);
        tv.setVersion(version);
        tv.setChangeLog(changeLog);
        tv.setTemplateData(templateData);
        tv.setMd5(MD5.create().digestHex(templateData));
        tv.setPublishedBy(publishedBy);
        tv.setPublishTime(LocalDateTime.now());
        tv.setStatus(1);
        tv.setCreatedBy(publishedBy);
        tv.setCreatedTime(LocalDateTime.now());
        templateVersionMapper.insert(tv);

        LambdaUpdateWrapper<AppInstall> installWrapper = new LambdaUpdateWrapper<>();
        installWrapper.eq(AppInstall::getTemplateId, templateId);
        installWrapper.set(AppInstall::getLatestVersion, version);
        installWrapper.set(AppInstall::getHasUpdate, 1);
        appInstallMapper.update(null, installWrapper);
    }

    private Map<String, Object> compareTemplateDiff(TemplateData oldData, TemplateData newData) {
        Map<String, Object> diff = new LinkedHashMap<>();

        diff.put("dataSources", compareSimpleList(
                oldData.getDataSources(), newData.getDataSources(), "sourceCode"));
        diff.put("dataModels", compareModelList(
                oldData.getDataModels(), newData.getDataModels()));
        diff.put("pages", comparePageList(
                oldData.getPages(), newData.getPages()));
        diff.put("businessLogics", compareLogicList(
                oldData.getBusinessLogics(), newData.getBusinessLogics()));
        diff.put("workflows", compareSimpleList(
                oldData.getWorkflows(), newData.getWorkflows(), "processKey"));

        return diff;
    }

    private Map<String, Object> compareSimpleList(List<?> oldList, List<?> newList, String codeField) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> oldCodes = extractCodes(oldList, codeField);
        Set<String> newCodes = extractCodes(newList, codeField);
        result.put("added", newCodes.stream().filter(c -> !oldCodes.contains(c)).count());
        result.put("removed", oldCodes.stream().filter(c -> !newCodes.contains(c)).count());
        result.put("modified", newCodes.stream().filter(oldCodes::contains).count());
        return result;
    }

    private Map<String, Object> compareModelList(List<Map<String, Object>> oldList, List<Map<String, Object>> newList) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> oldCodes = new HashSet<>();
        Set<String> newCodes = new HashSet<>();
        if (oldList != null) {
            for (Map<String, Object> m : oldList) {
                oldCodes.add(extractField(m, "model", "tableName"));
            }
        }
        if (newList != null) {
            for (Map<String, Object> m : newList) {
                newCodes.add(extractField(m, "model", "tableName"));
            }
        }
        result.put("added", newCodes.stream().filter(c -> !oldCodes.contains(c)).count());
        result.put("removed", oldCodes.stream().filter(c -> !newCodes.contains(c)).count());
        result.put("modified", newCodes.stream().filter(oldCodes::contains).count());
        return result;
    }

    private Map<String, Object> comparePageList(List<Map<String, Object>> oldList, List<Map<String, Object>> newList) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> oldCodes = new HashSet<>();
        Set<String> newCodes = new HashSet<>();
        if (oldList != null) {
            for (Map<String, Object> m : oldList) {
                oldCodes.add(extractField(m, "page", "pageCode"));
            }
        }
        if (newList != null) {
            for (Map<String, Object> m : newList) {
                newCodes.add(extractField(m, "page", "pageCode"));
            }
        }
        result.put("added", newCodes.stream().filter(c -> !oldCodes.contains(c)).count());
        result.put("removed", oldCodes.stream().filter(c -> !newCodes.contains(c)).count());
        result.put("modified", newCodes.stream().filter(oldCodes::contains).count());
        return result;
    }

    private Map<String, Object> compareLogicList(List<Map<String, Object>> oldList, List<Map<String, Object>> newList) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> oldCodes = new HashSet<>();
        Set<String> newCodes = new HashSet<>();
        if (oldList != null) {
            for (Map<String, Object> m : oldList) {
                oldCodes.add(extractField(m, "logic", "logicCode"));
            }
        }
        if (newList != null) {
            for (Map<String, Object> m : newList) {
                newCodes.add(extractField(m, "logic", "logicCode"));
            }
        }
        result.put("added", newCodes.stream().filter(c -> !oldCodes.contains(c)).count());
        result.put("removed", oldCodes.stream().filter(c -> !newCodes.contains(c)).count());
        result.put("modified", newCodes.stream().filter(oldCodes::contains).count());
        return result;
    }

    private Set<String> extractCodes(List<?> list, String codeField) {
        Set<String> codes = new HashSet<>();
        if (list == null) return codes;
        for (Object item : list) {
            try {
                com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(com.alibaba.fastjson2.JSON.toJSONString(item));
                String code = obj.getString(codeField);
                if (code != null) codes.add(code);
            } catch (Exception ignored) {}
        }
        return codes;
    }

    private String extractField(Map<String, Object> map, String nestedKey, String fieldKey) {
        try {
            Object nested = map.get(nestedKey);
            com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(com.alibaba.fastjson2.JSON.toJSONString(nested));
            return obj.getString(fieldKey);
        } catch (Exception e) {
            return null;
        }
    }

    private TemplateData getTemplateDataFromVersion(Long templateId, String version) {
        LambdaQueryWrapper<TemplateVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateVersion::getTemplateId, templateId);
        wrapper.eq(TemplateVersion::getVersion, version);
        wrapper.orderByDesc(TemplateVersion::getPublishTime);
        wrapper.last("LIMIT 1");
        TemplateVersion tv = templateVersionMapper.selectOne(wrapper);
        if (tv != null && tv.getTemplateData() != null) {
            return JSON.parseObject(tv.getTemplateData(), TemplateData.class);
        }
        AppTemplate template = appTemplateMapper.selectById(templateId);
        if (template != null && template.getTemplateData() != null) {
            return JSON.parseObject(template.getTemplateData(), TemplateData.class);
        }
        return new TemplateData();
    }

    private int restoreDataSourcesFull(Long appId, List<DataSource> sources, Long userId) {
        int count = 0;
        if (sources == null) return 0;
        LambdaQueryWrapper<DataSource> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(DataSource::getAppId, appId);
        dataSourceMapper.delete(delWrapper);
        for (DataSource ds : sources) {
            ds.setAppId(appId);
            ds.setStatus(1);
            ds.setCreatedBy(userId);
            ds.setCreatedTime(LocalDateTime.now());
            ds.setUpdatedBy(userId);
            ds.setUpdatedTime(LocalDateTime.now());
            dataSourceMapper.insert(ds);
            count++;
        }
        return count;
    }

    private int[] restoreDataSourcesIncremental(Long appId, List<DataSource> sources, Long userId) {
        int[] result = {0, 0, 0};
        if (sources == null) return result;
        for (DataSource ds : sources) {
            LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataSource::getAppId, appId);
            wrapper.eq(DataSource::getSourceCode, ds.getSourceCode());
            DataSource existing = dataSourceMapper.selectOne(wrapper);
            if (existing == null) {
                ds.setAppId(appId);
                ds.setStatus(1);
                ds.setCreatedBy(userId);
                ds.setCreatedTime(LocalDateTime.now());
                ds.setUpdatedBy(userId);
                ds.setUpdatedTime(LocalDateTime.now());
                dataSourceMapper.insert(ds);
                result[0]++;
            } else {
                result[2]++;
            }
        }
        return result;
    }

    private int restoreDataModelsFull(Long appId, List<Map<String, Object>> models, Long userId) {
        int count = 0;
        if (models == null) return 0;
        LambdaQueryWrapper<DataModel> modelWrapper = new LambdaQueryWrapper<>();
        modelWrapper.eq(DataModel::getAppId, appId);
        List<DataModel> oldModels = dataModelMapper.selectList(modelWrapper);
        for (DataModel m : oldModels) {
            LambdaQueryWrapper<ModelField> fieldWrapper = new LambdaQueryWrapper<>();
            fieldWrapper.eq(ModelField::getModelId, m.getId());
            modelFieldMapper.delete(fieldWrapper);
        }
        dataModelMapper.delete(modelWrapper);

        for (Map<String, Object> modelMap : models) {
            DataModel model = JSON.parseObject(JSON.toJSONString(modelMap.get("model")), DataModel.class);
            @SuppressWarnings("unchecked")
            List<ModelField> fields = JSON.parseArray(JSON.toJSONString(modelMap.get("fields")), ModelField.class);
            model.setAppId(appId);
            model.setStatus(0);
            model.setCreatedBy(userId);
            model.setCreatedTime(LocalDateTime.now());
            model.setUpdatedBy(userId);
            model.setUpdatedTime(LocalDateTime.now());
            dataModelMapper.insert(model);
            count++;
            if (fields != null) {
                for (ModelField f : fields) {
                    f.setModelId(model.getId());
                    f.setCreatedBy(userId);
                    f.setCreatedTime(LocalDateTime.now());
                    f.setUpdatedBy(userId);
                    f.setUpdatedTime(LocalDateTime.now());
                    modelFieldMapper.insert(f);
                }
            }
        }
        return count;
    }

    private int[] restoreDataModelsIncremental(Long appId, List<Map<String, Object>> models, Long userId) {
        int[] result = {0, 0, 0};
        if (models == null) return result;
        for (Map<String, Object> modelMap : models) {
            DataModel model = JSON.parseObject(JSON.toJSONString(modelMap.get("model")), DataModel.class);
            LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataModel::getAppId, appId);
            wrapper.eq(DataModel::getTableName, model.getTableName());
            DataModel existing = dataModelMapper.selectOne(wrapper);
            if (existing == null) {
                @SuppressWarnings("unchecked")
                List<ModelField> fields = JSON.parseArray(JSON.toJSONString(modelMap.get("fields")), ModelField.class);
                model.setAppId(appId);
                model.setStatus(0);
                model.setCreatedBy(userId);
                model.setCreatedTime(LocalDateTime.now());
                model.setUpdatedBy(userId);
                model.setUpdatedTime(LocalDateTime.now());
                dataModelMapper.insert(model);
                if (fields != null) {
                    for (ModelField f : fields) {
                        f.setModelId(model.getId());
                        f.setCreatedBy(userId);
                        f.setCreatedTime(LocalDateTime.now());
                        f.setUpdatedBy(userId);
                        f.setUpdatedTime(LocalDateTime.now());
                        modelFieldMapper.insert(f);
                    }
                }
                result[0]++;
            } else {
                result[2]++;
            }
        }
        return result;
    }

    private int restorePagesFull(Long appId, List<Map<String, Object>> pages, Long userId) {
        int count = 0;
        if (pages == null) return 0;
        LambdaQueryWrapper<Page> pageWrapper = new LambdaQueryWrapper<>();
        pageWrapper.eq(Page::getAppId, appId);
        List<Page> oldPages = pageMapper.selectList(pageWrapper);
        for (Page p : oldPages) {
            LambdaQueryWrapper<PageComponent> compWrapper = new LambdaQueryWrapper<>();
            compWrapper.eq(PageComponent::getPageId, p.getId());
            pageComponentMapper.delete(compWrapper);
        }
        pageMapper.delete(pageWrapper);

        for (Map<String, Object> pageMap : pages) {
            Page page = JSON.parseObject(JSON.toJSONString(pageMap.get("page")), Page.class);
            @SuppressWarnings("unchecked")
            List<PageComponent> components = JSON.parseArray(JSON.toJSONString(pageMap.get("components")), PageComponent.class);
            page.setAppId(appId);
            page.setStatus(1);
            page.setCreatedBy(userId);
            page.setCreatedTime(LocalDateTime.now());
            page.setUpdatedBy(userId);
            page.setUpdatedTime(LocalDateTime.now());
            pageMapper.insert(page);
            count++;
            if (components != null) {
                for (PageComponent c : components) {
                    c.setPageId(page.getId());
                    c.setCreatedBy(userId);
                    c.setCreatedTime(LocalDateTime.now());
                    c.setUpdatedBy(userId);
                    c.setUpdatedTime(LocalDateTime.now());
                    pageComponentMapper.insert(c);
                }
            }
        }
        return count;
    }

    private int[] restorePagesIncremental(Long appId, List<Map<String, Object>> pages, Long userId) {
        int[] result = {0, 0, 0};
        if (pages == null) return result;
        for (Map<String, Object> pageMap : pages) {
            Page page = JSON.parseObject(JSON.toJSONString(pageMap.get("page")), Page.class);
            LambdaQueryWrapper<Page> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Page::getAppId, appId);
            wrapper.eq(Page::getPageCode, page.getPageCode());
            Page existing = pageMapper.selectOne(wrapper);
            if (existing == null) {
                @SuppressWarnings("unchecked")
                List<PageComponent> components = JSON.parseArray(JSON.toJSONString(pageMap.get("components")), PageComponent.class);
                page.setAppId(appId);
                page.setStatus(1);
                page.setCreatedBy(userId);
                page.setCreatedTime(LocalDateTime.now());
                page.setUpdatedBy(userId);
                page.setUpdatedTime(LocalDateTime.now());
                pageMapper.insert(page);
                if (components != null) {
                    for (PageComponent c : components) {
                        c.setPageId(page.getId());
                        c.setCreatedBy(userId);
                        c.setCreatedTime(LocalDateTime.now());
                        c.setUpdatedBy(userId);
                        c.setUpdatedTime(LocalDateTime.now());
                        pageComponentMapper.insert(c);
                    }
                }
                result[0]++;
            } else {
                result[2]++;
            }
        }
        return result;
    }

    private int restoreLogicsFull(Long appId, List<Map<String, Object>> logics, Long userId) {
        int count = 0;
        if (logics == null) return 0;
        LambdaQueryWrapper<BusinessLogic> logicWrapper = new LambdaQueryWrapper<>();
        logicWrapper.eq(BusinessLogic::getAppId, appId);
        List<BusinessLogic> oldLogics = businessLogicMapper.selectList(logicWrapper);
        for (BusinessLogic l : oldLogics) {
            LambdaQueryWrapper<LogicNode> nodeWrapper = new LambdaQueryWrapper<>();
            nodeWrapper.eq(LogicNode::getLogicId, l.getId());
            logicNodeMapper.delete(nodeWrapper);
            LambdaQueryWrapper<LogicEdge> edgeWrapper = new LambdaQueryWrapper<>();
            edgeWrapper.eq(LogicEdge::getLogicId, l.getId());
            logicEdgeMapper.delete(edgeWrapper);
        }
        businessLogicMapper.delete(logicWrapper);

        for (Map<String, Object> logicMap : logics) {
            BusinessLogic logic = JSON.parseObject(JSON.toJSONString(logicMap.get("logic")), BusinessLogic.class);
            @SuppressWarnings("unchecked")
            List<LogicNode> nodes = JSON.parseArray(JSON.toJSONString(logicMap.get("nodes")), LogicNode.class);
            @SuppressWarnings("unchecked")
            List<LogicEdge> edges = JSON.parseArray(JSON.toJSONString(logicMap.get("edges")), LogicEdge.class);
            logic.setAppId(appId);
            logic.setStatus("0");
            logic.setCreatedBy(userId);
            logic.setCreatedTime(LocalDateTime.now());
            logic.setUpdatedBy(userId);
            logic.setUpdatedTime(LocalDateTime.now());
            businessLogicMapper.insert(logic);
            count++;
            if (nodes != null) {
                for (LogicNode n : nodes) {
                    n.setLogicId(logic.getId());
                    n.setCreatedBy(userId);
                    n.setCreatedTime(LocalDateTime.now());
                    n.setUpdatedBy(userId);
                    n.setUpdatedTime(LocalDateTime.now());
                    logicNodeMapper.insert(n);
                }
            }
            if (edges != null) {
                for (LogicEdge e : edges) {
                    e.setLogicId(logic.getId());
                    e.setCreatedBy(userId);
                    e.setCreatedTime(LocalDateTime.now());
                    e.setUpdatedBy(userId);
                    e.setUpdatedTime(LocalDateTime.now());
                    logicEdgeMapper.insert(e);
                }
            }
        }
        return count;
    }

    private int[] restoreLogicsIncremental(Long appId, List<Map<String, Object>> logics, Long userId) {
        int[] result = {0, 0, 0};
        if (logics == null) return result;
        for (Map<String, Object> logicMap : logics) {
            BusinessLogic logic = JSON.parseObject(JSON.toJSONString(logicMap.get("logic")), BusinessLogic.class);
            LambdaQueryWrapper<BusinessLogic> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BusinessLogic::getAppId, appId);
            wrapper.eq(BusinessLogic::getLogicCode, logic.getLogicCode());
            BusinessLogic existing = businessLogicMapper.selectOne(wrapper);
            if (existing == null) {
                @SuppressWarnings("unchecked")
                List<LogicNode> nodes = JSON.parseArray(JSON.toJSONString(logicMap.get("nodes")), LogicNode.class);
                @SuppressWarnings("unchecked")
                List<LogicEdge> edges = JSON.parseArray(JSON.toJSONString(logicMap.get("edges")), LogicEdge.class);
                logic.setAppId(appId);
                logic.setStatus("0");
                logic.setCreatedBy(userId);
                logic.setCreatedTime(LocalDateTime.now());
                logic.setUpdatedBy(userId);
                logic.setUpdatedTime(LocalDateTime.now());
                businessLogicMapper.insert(logic);
                if (nodes != null) {
                    for (LogicNode n : nodes) {
                        n.setLogicId(logic.getId());
                        n.setCreatedBy(userId);
                        n.setCreatedTime(LocalDateTime.now());
                        n.setUpdatedBy(userId);
                        n.setUpdatedTime(LocalDateTime.now());
                        logicNodeMapper.insert(n);
                    }
                }
                if (edges != null) {
                    for (LogicEdge e : edges) {
                        e.setLogicId(logic.getId());
                        e.setCreatedBy(userId);
                        e.setCreatedTime(LocalDateTime.now());
                        e.setUpdatedBy(userId);
                        e.setUpdatedTime(LocalDateTime.now());
                        logicEdgeMapper.insert(e);
                    }
                }
                result[0]++;
            } else {
                result[2]++;
            }
        }
        return result;
    }

    private int restoreWorkflowsFull(Long appId, List<WorkflowDefinition> workflows, Long userId) {
        int count = 0;
        if (workflows == null) return 0;
        LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowDefinition::getAppId, appId);
        workflowDefinitionMapper.delete(wrapper);

        for (WorkflowDefinition wf : workflows) {
            wf.setAppId(appId);
            wf.setStatus(0);
            wf.setCreatedBy(userId);
            wf.setCreatedTime(LocalDateTime.now());
            wf.setUpdatedBy(userId);
            wf.setUpdatedTime(LocalDateTime.now());
            workflowDefinitionMapper.insert(wf);
            count++;
        }
        return count;
    }

    private int[] restoreWorkflowsIncremental(Long appId, List<WorkflowDefinition> workflows, Long userId) {
        int[] result = {0, 0, 0};
        if (workflows == null) return result;
        for (WorkflowDefinition wf : workflows) {
            LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WorkflowDefinition::getAppId, appId);
            wrapper.eq(WorkflowDefinition::getProcessKey, wf.getProcessKey());
            WorkflowDefinition existing = workflowDefinitionMapper.selectOne(wrapper);
            if (existing == null) {
                wf.setAppId(appId);
                wf.setStatus(0);
                wf.setCreatedBy(userId);
                wf.setCreatedTime(LocalDateTime.now());
                wf.setUpdatedBy(userId);
                wf.setUpdatedTime(LocalDateTime.now());
                workflowDefinitionMapper.insert(wf);
                result[0]++;
            } else {
                result[2]++;
            }
        }
        return result;
    }

    private boolean isNewerVersion(String version1, String version2) {
        try {
            String[] v1 = version1.replaceAll("[^0-9.]", "").split("\\.");
            String[] v2 = version2.replaceAll("[^0-9.]", "").split("\\.");
            int len = Math.max(v1.length, v2.length);
            for (int i = 0; i < len; i++) {
                int n1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
                int n2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;
                if (n1 > n2) return true;
                if (n1 < n2) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
