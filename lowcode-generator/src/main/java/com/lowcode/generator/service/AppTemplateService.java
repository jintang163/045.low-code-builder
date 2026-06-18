package com.lowcode.generator.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AppTemplateService extends ServiceImpl<AppTemplateMapper, AppTemplate> {

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
    private AppInstallMapper appInstallMapper;

    @Autowired
    private TemplateUpgradeService upgradeService;

    @Autowired
    private TemplateDataProvider dataProvider;

    public Page<AppTemplate> getTemplatePage(int pageNum, int pageSize, String category, String keyword, Integer templateType) {
        LambdaQueryWrapper<AppTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppTemplate::getStatus, 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(AppTemplate::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AppTemplate::getTemplateName, keyword)
                    .or().like(AppTemplate::getTemplateDesc, keyword);
        }
        if (templateType != null) {
            wrapper.eq(AppTemplate::getTemplateType, templateType);
        }
        wrapper.orderByDesc(AppTemplate::getInstallCount, AppTemplate::getCreatedTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<AppTemplate> getTemplateList(String category, String keyword, Integer templateType) {
        LambdaQueryWrapper<AppTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppTemplate::getStatus, 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(AppTemplate::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AppTemplate::getTemplateName, keyword)
                    .or().like(AppTemplate::getTemplateDesc, keyword);
        }
        if (templateType != null) {
            wrapper.eq(AppTemplate::getTemplateType, templateType);
        }
        wrapper.orderByDesc(AppTemplate::getInstallCount, AppTemplate::getCreatedTime);
        return this.list(wrapper);
    }

    public AppTemplate getTemplateDetail(Long id) {
        return this.getById(id);
    }

    public TemplateData getTemplateData(Long id) {
        AppTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        return JSON.parseObject(template.getTemplateData(), TemplateData.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishAsTemplate(Map<String, Object> params) {
        Long appId = Long.valueOf(params.get("appId").toString());
        String templateName = (String) params.get("templateName");
        String templateCode = (String) params.get("templateCode");
        String templateDesc = (String) params.get("templateDesc");
        String category = (String) params.getOrDefault("category", "other");
        String version = (String) params.getOrDefault("version", "1.0.0");
        String changeLog = (String) params.getOrDefault("changeLog", "发布新版本");
        String tags = (String) params.get("tags");

        if (StrUtil.isBlank(templateName) || StrUtil.isBlank(templateCode)) {
            throw new BusinessException("模板名称和编码不能为空");
        }

        Long userId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<AppTemplate> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(AppTemplate::getTemplateCode, templateCode);
        AppTemplate existing = this.getOne(codeWrapper);

        TemplateData templateData = buildTemplateData(appId);
        String templateDataJson = JSON.toJSONString(templateData);

        AppTemplate template;
        if (existing != null && existing.getPublisherId() != null && existing.getPublisherId().equals(userId)) {
            template = existing;
            String newVersion = version;
            if (newVersion == null || newVersion.equals(template.getVersion())) {
                newVersion = nextVersion(template.getVersion());
            }
            template.setVersion(newVersion);
            template.setTemplateDesc(templateDesc);
            template.setCategory(category);
            template.setTags(tags);
            template.setTemplateData(templateDataJson);
            template.setUpdatedBy(userId);
            template.setUpdatedTime(LocalDateTime.now());
            this.updateById(template);

            upgradeService.createTemplateVersion(template.getId(), newVersion, changeLog, templateDataJson, userId);

            log.info("更新模板: templateId={}, version={}, userId={}", template.getId(), newVersion, userId);
        } else {
            template = new AppTemplate();
            template.setTemplateName(templateName);
            template.setTemplateCode(templateCode);
            template.setTemplateDesc(templateDesc);
            template.setCategory(category);
            template.setTags(tags);
            template.setVersion(version);
            template.setInstallCount(0);
            template.setStarCount(0);
            template.setTemplateData(templateDataJson);
            template.setTemplateType(1);
            template.setPublisher(UserContext.getCurrentUserName());
            template.setPublisherId(userId);
            template.setStatus(1);
            template.setCreatedBy(userId);
            template.setCreatedTime(LocalDateTime.now());
            template.setUpdatedBy(userId);
            template.setUpdatedTime(LocalDateTime.now());
            this.save(template);

            upgradeService.createTemplateVersion(template.getId(), version, "初始版本发布", templateDataJson, userId);

            log.info("发布新模板: templateId={}, templateCode={}, userId={}", template.getId(), templateCode, userId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("version", template.getVersion());
        result.put("message", existing != null ? "模板更新成功，版本：" + template.getVersion() : "模板发布成功");
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> installTemplate(Long templateId, Long userId) {
        AppTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        TemplateData templateData;
        try {
            templateData = JSON.parseObject(template.getTemplateData(), TemplateData.class);
        } catch (Exception e) {
            throw new BusinessException("模板数据解析失败");
        }

        String appName = (String) template.getTemplateName();
        AppInfo appInfo = new AppInfo();
        appInfo.setAppName(appName);
        appInfo.setAppCode(template.getTemplateCode() + "_" + IdUtil.nanoId(6));
        appInfo.setAppDesc(template.getTemplateDesc());
        appInfo.setStatus(1);
        appInfo.setTemplateId(templateId);
        appInfo.setTemplateVersion(template.getVersion());
        appInfo.setCreatedBy(userId);
        appInfo.setCreatedTime(LocalDateTime.now());
        appInfo.setUpdatedBy(userId);
        appInfo.setUpdatedTime(LocalDateTime.now());
        appInfoMapper.insert(appInfo);

        Map<Long, Long> idMapping = new HashMap<>();

        if (templateData.getDataSources() != null && !templateData.getDataSources().isEmpty()) {
            for (DataSource ds : templateData.getDataSources()) {
                Long oldId = ds.getId();
                ds.setAppId(appInfo.getId());
                ds.setStatus(1);
                ds.setCreatedBy(userId);
                ds.setCreatedTime(LocalDateTime.now());
                ds.setUpdatedBy(userId);
                ds.setUpdatedTime(LocalDateTime.now());
                dataSourceMapper.insert(ds);
                idMapping.put(oldId, ds.getId());
            }
        }

        if (templateData.getDataModels() != null && !templateData.getDataModels().isEmpty()) {
            for (Map<String, Object> modelMap : templateData.getDataModels()) {
                DataModel model = JSON.parseObject(JSON.toJSONString(modelMap.get("model")), DataModel.class);
                @SuppressWarnings("unchecked")
                List<ModelField> fields = JSON.parseArray(JSON.toJSONString(modelMap.get("fields")), ModelField.class);
                Long oldId = model.getId();
                Long oldDsId = model.getDataSourceId();
                model.setAppId(appInfo.getId());
                if (oldDsId != null && idMapping.containsKey(oldDsId)) {
                    model.setDataSourceId(idMapping.get(oldDsId));
                }
                model.setStatus(0);
                model.setCreatedBy(userId);
                model.setCreatedTime(LocalDateTime.now());
                model.setUpdatedBy(userId);
                model.setUpdatedTime(LocalDateTime.now());
                dataModelMapper.insert(model);
                idMapping.put(oldId, model.getId());

                if (fields != null && !fields.isEmpty()) {
                    for (ModelField field : fields) {
                        field.setModelId(model.getId());
                        field.setCreatedBy(userId);
                        field.setCreatedTime(LocalDateTime.now());
                        field.setUpdatedBy(userId);
                        field.setUpdatedTime(LocalDateTime.now());
                        modelFieldMapper.insert(field);
                    }
                }
            }
        }

        if (templateData.getPages() != null && !templateData.getPages().isEmpty()) {
            for (Map<String, Object> pageMap : templateData.getPages()) {
                Page page = JSON.parseObject(JSON.toJSONString(pageMap.get("page")), Page.class);
                @SuppressWarnings("unchecked")
                List<PageComponent> components = JSON.parseArray(JSON.toJSONString(pageMap.get("components")), PageComponent.class);
                Long oldId = page.getId();
                Long oldModelId = page.getModelId();
                page.setAppId(appInfo.getId());
                if (oldModelId != null && idMapping.containsKey(oldModelId)) {
                    page.setModelId(idMapping.get(oldModelId));
                }
                page.setStatus(1);
                page.setCreatedBy(userId);
                page.setCreatedTime(LocalDateTime.now());
                page.setUpdatedBy(userId);
                page.setUpdatedTime(LocalDateTime.now());
                pageMapper.insert(page);
                idMapping.put(oldId, page.getId());

                if (components != null && !components.isEmpty()) {
                    for (PageComponent component : components) {
                        component.setPageId(page.getId());
                        component.setCreatedBy(userId);
                        component.setCreatedTime(LocalDateTime.now());
                        component.setUpdatedBy(userId);
                        component.setUpdatedTime(LocalDateTime.now());
                        pageComponentMapper.insert(component);
                    }
                }
            }
        }

        if (templateData.getBusinessLogics() != null && !templateData.getBusinessLogics().isEmpty()) {
            for (Map<String, Object> logicMap : templateData.getBusinessLogics()) {
                BusinessLogic logic = JSON.parseObject(JSON.toJSONString(logicMap.get("logic")), BusinessLogic.class);
                @SuppressWarnings("unchecked")
                List<LogicNode> nodes = JSON.parseArray(JSON.toJSONString(logicMap.get("nodes")), LogicNode.class);
                @SuppressWarnings("unchecked")
                List<LogicEdge> edges = JSON.parseArray(JSON.toJSONString(logicMap.get("edges")), LogicEdge.class);
                Long oldId = logic.getId();
                Long oldModelId = logic.getModelId();
                logic.setAppId(appInfo.getId());
                if (oldModelId != null && idMapping.containsKey(oldModelId)) {
                    logic.setModelId(idMapping.get(oldModelId));
                }
                logic.setStatus("0");
                logic.setCreatedBy(userId);
                logic.setCreatedTime(LocalDateTime.now());
                logic.setUpdatedBy(userId);
                logic.setUpdatedTime(LocalDateTime.now());
                businessLogicMapper.insert(logic);
                idMapping.put(oldId, logic.getId());

                if (nodes != null && !nodes.isEmpty()) {
                    for (LogicNode node : nodes) {
                        node.setLogicId(logic.getId());
                        node.setCreatedBy(userId);
                        node.setCreatedTime(LocalDateTime.now());
                        node.setUpdatedBy(userId);
                        node.setUpdatedTime(LocalDateTime.now());
                        logicNodeMapper.insert(node);
                    }
                }
                if (edges != null && !edges.isEmpty()) {
                    for (LogicEdge edge : edges) {
                        edge.setLogicId(logic.getId());
                        edge.setCreatedBy(userId);
                        edge.setCreatedTime(LocalDateTime.now());
                        edge.setUpdatedBy(userId);
                        edge.setUpdatedTime(LocalDateTime.now());
                        logicEdgeMapper.insert(edge);
                    }
                }
            }
        }

        if (templateData.getWorkflows() != null && !templateData.getWorkflows().isEmpty()) {
            for (WorkflowDefinition wf : templateData.getWorkflows()) {
                Long oldId = wf.getId();
                Long oldModelId = wf.getModelId();
                wf.setAppId(appInfo.getId());
                if (oldModelId != null && idMapping.containsKey(oldModelId)) {
                    wf.setModelId(idMapping.get(oldModelId));
                }
                wf.setStatus(0);
                wf.setCreatedBy(userId);
                wf.setCreatedTime(LocalDateTime.now());
                wf.setUpdatedBy(userId);
                wf.setUpdatedTime(LocalDateTime.now());
                workflowDefinitionMapper.insert(wf);
                idMapping.put(oldId, wf.getId());
            }
        }

        AppInstall appInstall = new AppInstall();
        appInstall.setTemplateId(templateId);
        appInstall.setTemplateVersion(template.getVersion());
        appInstall.setAppId(appInfo.getId());
        appInstall.setUserId(userId);
        appInstall.setInstallTime(LocalDateTime.now());
        appInstall.setLastUpdateTime(LocalDateTime.now());
        appInstall.setCurrentVersion(template.getVersion());
        appInstall.setLatestVersion(template.getVersion());
        appInstall.setHasUpdate(0);
        appInstall.setCreatedBy(userId);
        appInstall.setCreatedTime(LocalDateTime.now());
        appInstallMapper.insert(appInstall);

        LambdaUpdateWrapper<AppTemplate> templateWrapper = new LambdaUpdateWrapper<>();
        templateWrapper.eq(AppTemplate::getId, templateId);
        templateWrapper.setSql("install_count = install_count + 1");
        this.update(templateWrapper);

        log.info("模板安装完成: appId={}, templateId={}, templateCode={}, userId={}",
                appInfo.getId(), templateId, template.getTemplateCode(), userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appId", appInfo.getId());
        result.put("appName", appInfo.getAppName());
        result.put("templateVersion", template.getVersion());
        result.put("message", "模板安装成功！");
        return result;
    }

    public String exportTemplate(Long id) {
        AppTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        AppTemplate export = new AppTemplate();
        BeanUtils.copyProperties(template, export);
        export.setId(null);
        export.setInstallCount(0);
        export.setStarCount(0);
        export.setCreatedBy(null);
        export.setCreatedTime(null);
        export.setUpdatedBy(null);
        export.setUpdatedTime(null);
        return JSON.toJSONString(export);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importTemplate(String templateJson, Long userId) {
        AppTemplate template;
        try {
            template = JSON.parseObject(templateJson, AppTemplate.class);
        } catch (Exception e) {
            throw new BusinessException("模板文件格式错误");
        }
        if (StrUtil.isBlank(template.getTemplateName()) || StrUtil.isBlank(template.getTemplateCode())) {
            throw new BusinessException("模板名称或编码不能为空");
        }

        LambdaQueryWrapper<AppTemplate> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(AppTemplate::getTemplateCode, template.getTemplateCode());
        if (this.count(codeWrapper) > 0) {
            template.setTemplateCode(template.getTemplateCode() + "_" + IdUtil.nanoId(4));
        }

        template.setId(null);
        template.setInstallCount(0);
        template.setStarCount(0);
        template.setTemplateType(1);
        template.setStatus(1);
        template.setPublisher(UserContext.getCurrentUserName());
        template.setPublisherId(userId);
        template.setCreatedBy(userId);
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedBy(userId);
        template.setUpdatedTime(LocalDateTime.now());
        this.save(template);

        upgradeService.createTemplateVersion(template.getId(), template.getVersion(),
                "模板导入创建", template.getTemplateData(), userId);

        log.info("模板导入成功: templateId={}, templateCode={}", template.getId(), template.getTemplateCode());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("version", template.getVersion());
        result.put("message", "模板导入成功，已自动上架");
        return result;
    }

    public Map<String, Object> getTemplateStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        LambdaQueryWrapper<AppTemplate> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(AppTemplate::getStatus, 1);
        stats.put("templateCount", this.count(templateWrapper));

        LambdaQueryWrapper<AppInstall> installWrapper = new LambdaQueryWrapper<>();
        stats.put("installCount", appInstallMapper.selectCount(installWrapper));

        LambdaQueryWrapper<AppInfo> appWrapper = new LambdaQueryWrapper<>();
        stats.put("appCount", appInfoMapper.selectCount(appWrapper));

        LambdaQueryWrapper<AppTemplate> builtinWrapper = new LambdaQueryWrapper<>();
        builtinWrapper.eq(AppTemplate::getTemplateType, 0);
        stats.put("builtinCount", this.count(builtinWrapper));

        return stats;
    }

    public List<String> getCategoryList() {
        LambdaQueryWrapper<AppTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppTemplate::getStatus, 1);
        wrapper.select(AppTemplate::getCategory);
        wrapper.groupBy(AppTemplate::getCategory);
        List<AppTemplate> templates = this.list(wrapper);
        List<String> categories = new ArrayList<>();
        categories.add("全部");
        for (AppTemplate t : templates) {
            if (t.getCategory() != null && !categories.contains(t.getCategory())) {
                categories.add(t.getCategory());
            }
        }
        return categories;
    }

    @Transactional(rollbackFor = Exception.class)
    public void initBuiltinTemplates() {
        String[][] builtins = {
                {"OA办公系统", "oa-system", "内置OA系统，包含部门、员工、请假审批等核心功能", "enterprise", "OA,办公,审批"},
                {"CRM客户管理系统", "crm-system", "内置CRM系统，包含客户、商机、合同管理等功能", "business", "CRM,客户,销售"},
                {"进销存管理系统", "inventory-system", "内置进销存系统，包含商品、采购、销售、库存管理", "retail", "进销存,库存,采购,销售"}
        };

        for (String[] builtin : builtins) {
            LambdaQueryWrapper<AppTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AppTemplate::getTemplateCode, builtin[1]);
            if (this.count(wrapper) == 0) {
                TemplateData data = dataProvider.createBuiltinTemplateData(builtin[1]);
                AppTemplate template = new AppTemplate();
                template.setTemplateName(builtin[0]);
                template.setTemplateCode(builtin[1]);
                template.setTemplateDesc(builtin[2]);
                template.setCategory(builtin[3]);
                template.setTags(builtin[4]);
                template.setVersion("1.0.0");
                template.setInstallCount(0);
                template.setStarCount(0);
                template.setTemplateData(JSON.toJSONString(data));
                template.setTemplateType(0);
                template.setPublisher("系统");
                template.setPublisherId(1L);
                template.setStatus(1);
                template.setCreatedBy(1L);
                template.setCreatedTime(LocalDateTime.now());
                template.setUpdatedBy(1L);
                template.setUpdatedTime(LocalDateTime.now());
                this.save(template);

                upgradeService.createTemplateVersion(template.getId(), "1.0.0",
                        "内置模板初始版本", template.getTemplateData(), 1L);

                log.info("内置模板初始化: {}", builtin[0]);
            }
        }
    }

    private TemplateData buildTemplateData(Long appId) {
        TemplateData data = new TemplateData();

        LambdaQueryWrapper<DataSource> dsWrapper = new LambdaQueryWrapper<>();
        dsWrapper.eq(DataSource::getAppId, appId);
        List<DataSource> dataSources = dataSourceMapper.selectList(dsWrapper);
        if (dataSources != null) {
            for (DataSource ds : dataSources) {
                ds.setPassword("***");
                ds.setId(null);
                ds.setCreatedBy(null);
                ds.setCreatedTime(null);
                ds.setUpdatedBy(null);
                ds.setUpdatedTime(null);
            }
        }
        data.setDataSources(dataSources);

        LambdaQueryWrapper<DataModel> modelWrapper = new LambdaQueryWrapper<>();
        modelWrapper.eq(DataModel::getAppId, appId);
        List<DataModel> models = dataModelMapper.selectList(modelWrapper);
        List<Map<String, Object>> modelList = new ArrayList<>();
        if (models != null) {
            for (DataModel model : models) {
                Long oldId = model.getId();
                model.setId(null);
                model.setCreatedBy(null);
                model.setCreatedTime(null);
                model.setUpdatedBy(null);
                model.setUpdatedTime(null);

                LambdaQueryWrapper<ModelField> fieldWrapper = new LambdaQueryWrapper<>();
                fieldWrapper.eq(ModelField::getModelId, oldId);
                List<ModelField> fields = modelFieldMapper.selectList(fieldWrapper);
                if (fields != null) {
                    for (ModelField f : fields) {
                        f.setId(null);
                        f.setModelId(null);
                        f.setCreatedBy(null);
                        f.setCreatedTime(null);
                        f.setUpdatedBy(null);
                        f.setUpdatedTime(null);
                    }
                }

                Map<String, Object> modelMap = new LinkedHashMap<>();
                modelMap.put("model", model);
                modelMap.put("fields", fields);
                modelList.add(modelMap);
            }
        }
        data.setDataModels(modelList);

        LambdaQueryWrapper<Page> pageWrapper = new LambdaQueryWrapper<>();
        pageWrapper.eq(Page::getAppId, appId);
        List<Page> pages = pageMapper.selectList(pageWrapper);
        List<Map<String, Object>> pageList = new ArrayList<>();
        if (pages != null) {
            for (Page page : pages) {
                Long oldId = page.getId();
                page.setId(null);
                page.setCreatedBy(null);
                page.setCreatedTime(null);
                page.setUpdatedBy(null);
                page.setUpdatedTime(null);

                LambdaQueryWrapper<PageComponent> compWrapper = new LambdaQueryWrapper<>();
                compWrapper.eq(PageComponent::getPageId, oldId);
                List<PageComponent> components = pageComponentMapper.selectList(compWrapper);
                if (components != null) {
                    for (PageComponent c : components) {
                        c.setId(null);
                        c.setPageId(null);
                        c.setCreatedBy(null);
                        c.setCreatedTime(null);
                        c.setUpdatedBy(null);
                        c.setUpdatedTime(null);
                    }
                }

                Map<String, Object> pageMap = new LinkedHashMap<>();
                pageMap.put("page", page);
                pageMap.put("components", components);
                pageList.add(pageMap);
            }
        }
        data.setPages(pageList);

        LambdaQueryWrapper<BusinessLogic> logicWrapper = new LambdaQueryWrapper<>();
        logicWrapper.eq(BusinessLogic::getAppId, appId);
        List<BusinessLogic> logics = businessLogicMapper.selectList(logicWrapper);
        List<Map<String, Object>> logicList = new ArrayList<>();
        if (logics != null) {
            for (BusinessLogic logic : logics) {
                Long oldId = logic.getId();
                logic.setId(null);
                logic.setCreatedBy(null);
                logic.setCreatedTime(null);
                logic.setUpdatedBy(null);
                logic.setUpdatedTime(null);

                LambdaQueryWrapper<LogicNode> nodeWrapper = new LambdaQueryWrapper<>();
                nodeWrapper.eq(LogicNode::getLogicId, oldId);
                List<LogicNode> nodes = logicNodeMapper.selectList(nodeWrapper);
                if (nodes != null) {
                    for (LogicNode n : nodes) {
                        n.setId(null);
                        n.setLogicId(null);
                        n.setCreatedBy(null);
                        n.setCreatedTime(null);
                        n.setUpdatedBy(null);
                        n.setUpdatedTime(null);
                    }
                }

                LambdaQueryWrapper<LogicEdge> edgeWrapper = new LambdaQueryWrapper<>();
                edgeWrapper.eq(LogicEdge::getLogicId, oldId);
                List<LogicEdge> edges = logicEdgeMapper.selectList(edgeWrapper);
                if (edges != null) {
                    for (LogicEdge e : edges) {
                        e.setId(null);
                        e.setLogicId(null);
                        e.setCreatedBy(null);
                        e.setCreatedTime(null);
                        e.setUpdatedBy(null);
                        e.setUpdatedTime(null);
                    }
                }

                Map<String, Object> logicMap = new LinkedHashMap<>();
                logicMap.put("logic", logic);
                logicMap.put("nodes", nodes);
                logicMap.put("edges", edges);
                logicList.add(logicMap);
            }
        }
        data.setBusinessLogics(logicList);

        LambdaQueryWrapper<WorkflowDefinition> wfWrapper = new LambdaQueryWrapper<>();
        wfWrapper.eq(WorkflowDefinition::getAppId, appId);
        List<WorkflowDefinition> workflows = workflowDefinitionMapper.selectList(wfWrapper);
        if (workflows != null) {
            for (WorkflowDefinition wf : workflows) {
                wf.setId(null);
                wf.setDeploymentId(null);
                wf.setProcessDefinitionId(null);
                wf.setCreatedBy(null);
                wf.setCreatedTime(null);
                wf.setUpdatedBy(null);
                wf.setUpdatedTime(null);
            }
        }
        data.setWorkflows(workflows);

        return data;
    }

    private String nextVersion(String version) {
        try {
            String[] parts = version.replaceAll("[^0-9.]", "").split("\\.");
            while (parts.length < 3) {
                parts = Arrays.copyOf(parts, parts.length + 1);
                parts[parts.length - 1] = "0";
            }
            int patch = Integer.parseInt(parts[2]) + 1;
            return parts[0] + "." + parts[1] + "." + patch;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    public boolean updateTemplate(AppTemplate template) {
        Long userId = UserContext.getCurrentUserId();
        template.setUpdatedBy(userId);
        template.setUpdatedTime(LocalDateTime.now());
        return this.updateById(template);
    }

    public boolean deleteTemplate(Long id) {
        return this.removeById(id);
    }

    public boolean starTemplate(Long id) {
        LambdaUpdateWrapper<AppTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppTemplate::getId, id);
        wrapper.setSql("star_count = star_count + 1");
        return this.update(wrapper);
    }
}
