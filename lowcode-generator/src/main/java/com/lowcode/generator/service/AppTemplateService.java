package com.lowcode.generator.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.generator.entity.AppInfo;
import com.lowcode.generator.entity.AppTemplate;
import com.lowcode.generator.entity.TemplateData;
import com.lowcode.generator.mapper.AppTemplateMapper;
import com.lowcode.generator.mapper.AppInfoMapper;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.mapper.DataModelMapper;
import com.lowcode.model.mapper.DataSourceMapper;
import com.lowcode.model.mapper.ModelFieldMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AppTemplateService extends ServiceImpl<AppTemplateMapper, AppTemplate> {

    @Autowired
    private AppInfoMapper appInfoMapper;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private DataModelMapper dataModelMapper;

    @Autowired
    private ModelFieldMapper modelFieldMapper;

    public Page<AppTemplate> getTemplatePage(Integer current, Integer size, String keyword, String category, Integer templateType) {
        LambdaQueryWrapper<AppTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(AppTemplate::getTemplateName, keyword)
                    .or().like(AppTemplate::getTemplateCode, keyword)
                    .or().like(AppTemplate::getTemplateDesc, keyword));
        }
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(AppTemplate::getCategory, category);
        }
        if (templateType != null) {
            wrapper.eq(AppTemplate::getTemplateType, templateType);
        }
        wrapper.eq(AppTemplate::getStatus, 1);
        wrapper.orderByDesc(AppTemplate::getInstallCount);
        wrapper.orderByDesc(AppTemplate::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    public List<AppTemplate> getTemplateList(String category, Integer limit) {
        LambdaQueryWrapper<AppTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(AppTemplate::getCategory, category);
        }
        wrapper.eq(AppTemplate::getStatus, 1);
        wrapper.orderByDesc(AppTemplate::getInstallCount);
        wrapper.orderByDesc(AppTemplate::getCreatedTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return list(wrapper);
    }

    public AppTemplate getTemplateDetail(Long id) {
        AppTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        return template;
    }

    public TemplateData getTemplateData(Long id) {
        AppTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (StrUtil.isBlank(template.getTemplateData())) {
            return new TemplateData();
        }
        try {
            return JSON.parseObject(template.getTemplateData(), TemplateData.class);
        } catch (Exception e) {
            log.error("解析模板数据失败", e);
            throw new BusinessException("模板数据解析失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public AppTemplate publishAsTemplate(Long appId, String templateName, String templateCode,
                                         String templateDesc, String icon, String category,
                                         String tags, String version, Integer templateType) {
        AppInfo app = appInfoMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }

        LambdaQueryWrapper<AppTemplate> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(AppTemplate::getTemplateCode, templateCode);
        Long count = count(codeWrapper);
        if (count > 0) {
            throw new BusinessException("模板编码已存在");
        }

        TemplateData templateData = buildTemplateData(appId);

        AppTemplate template = new AppTemplate();
        template.setTemplateName(templateName != null ? templateName : app.getAppName());
        template.setTemplateCode(templateCode);
        template.setTemplateDesc(templateDesc != null ? templateDesc : app.getAppDesc());
        template.setIcon(icon != null ? icon : app.getIcon());
        template.setCategory(category != null ? category : "business");
        template.setTags(tags);
        template.setVersion(version != null ? version : "1.0.0");
        template.setInstallCount(0);
        template.setStarCount(0);
        template.setTemplateType(templateType != null ? templateType : 1);
        template.setPublisher("当前用户");
        template.setPublisherId(1L);
        template.setStatus(1);
        template.setPublishTime(LocalDateTime.now());
        template.setTemplateData(JSON.toJSONString(templateData));
        template.setCreatedBy(1L);
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedBy(1L);
        template.setUpdatedTime(LocalDateTime.now());

        save(template);
        log.info("应用发布为模板: {} -> {}", app.getAppName(), templateCode);
        return template;
    }

    private TemplateData buildTemplateData(Long appId) {
        TemplateData data = new TemplateData();

        LambdaQueryWrapper<DataSource> dsWrapper = new LambdaQueryWrapper<>();
        dsWrapper.eq(DataSource::getAppId, appId);
        List<DataSource> dataSources = dataSourceMapper.selectList(dsWrapper);
        dataSources.forEach(ds -> {
            ds.setPassword("***");
            ds.setId(null);
            ds.setAppId(null);
            ds.setCreatedBy(null);
            ds.setCreatedTime(null);
            ds.setUpdatedBy(null);
            ds.setUpdatedTime(null);
        });
        data.setDataSources(dataSources);

        LambdaQueryWrapper<DataModel> modelWrapper = new LambdaQueryWrapper<>();
        modelWrapper.eq(DataModel::getAppId, appId);
        List<DataModel> dataModels = dataModelMapper.selectList(modelWrapper);

        List<Map<String, Object>> modelList = new ArrayList<>();
        for (DataModel model : dataModels) {
            Map<String, Object> modelMap = new HashMap<>();
            modelMap.put("model", model);

            LambdaQueryWrapper<ModelField> fieldWrapper = new LambdaQueryWrapper<>();
            fieldWrapper.eq(ModelField::getModelId, model.getId());
            List<ModelField> fields = modelFieldMapper.selectList(fieldWrapper);
            fields.forEach(f -> {
                f.setId(null);
                f.setModelId(null);
                f.setCreatedBy(null);
                f.setCreatedTime(null);
                f.setUpdatedBy(null);
                f.setUpdatedTime(null);
            });
            modelMap.put("fields", fields);

            model.setId(null);
            model.setAppId(null);
            model.setDataSourceId(null);
            model.setCreatedBy(null);
            model.setCreatedTime(null);
            model.setUpdatedBy(null);
            model.setUpdatedTime(null);

            modelList.add(modelMap);
        }
        data.setDataModels(modelList);

        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public AppInfo installTemplate(Long templateId, String appName, String appCode, Long userId) {
        AppTemplate template = getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        LambdaQueryWrapper<AppInfo> appCodeWrapper = new LambdaQueryWrapper<>();
        appCodeWrapper.eq(AppInfo::getAppCode, appCode);
        Long appCount = appInfoMapper.selectCount(appCodeWrapper);
        if (appCount > 0) {
            throw new BusinessException("应用编码已存在");
        }

        TemplateData templateData;
        try {
            templateData = JSON.parseObject(template.getTemplateData(), TemplateData.class);
        } catch (Exception e) {
            log.error("解析模板数据失败", e);
            throw new BusinessException("模板数据解析失败");
        }

        AppInfo app = new AppInfo();
        app.setAppName(appName);
        app.setAppCode(appCode);
        app.setAppDesc(template.getTemplateDesc());
        app.setIcon(template.getIcon());
        app.setStatus(1);
        app.setVersion(template.getVersion());
        app.setCreatedBy(userId != null ? userId : 1L);
        app.setCreatedTime(LocalDateTime.now());
        app.setUpdatedBy(userId != null ? userId : 1L);
        app.setUpdatedTime(LocalDateTime.now());
        appInfoMapper.insert(app);

        if (templateData.getDataSources() != null && !templateData.getDataSources().isEmpty()) {
            for (DataSource ds : templateData.getDataSources()) {
                ds.setAppId(app.getId());
                ds.setStatus(1);
                dataSourceMapper.insert(ds);
            }
        }

        if (templateData.getDataModels() != null && !templateData.getDataModels().isEmpty()) {
            List<DataSource> dsList = dataSourceMapper.selectList(
                    new LambdaQueryWrapper<DataSource>().eq(DataSource::getAppId, app.getId()));
            Long defaultDsId = dsList.isEmpty() ? null : dsList.get(0).getId();

            for (Map<String, Object> modelMap : templateData.getDataModels()) {
                DataModel model = JSON.parseObject(JSON.toJSONString(modelMap.get("model")), DataModel.class);
                @SuppressWarnings("unchecked")
                List<ModelField> fields = JSON.parseArray(
                        JSON.toJSONString(modelMap.get("fields")), ModelField.class);

                model.setAppId(app.getId());
                model.setDataSourceId(defaultDsId);
                model.setStatus(0);
                dataModelMapper.insert(model);

                if (fields != null && !fields.isEmpty()) {
                    for (ModelField field : fields) {
                        field.setModelId(model.getId());
                        modelFieldMapper.insert(field);
                    }
                }
            }
        }

        template.setInstallCount(template.getInstallCount() == null ? 1 : template.getInstallCount() + 1);
        updateById(template);

        log.info("模板安装成功: {} -> {}", template.getTemplateCode(), appCode);
        return app;
    }

    public byte[] exportTemplate(Long templateId) {
        AppTemplate template = getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("template", template);
        exportData.put("exportTime", LocalDateTime.now().toString());
        exportData.put("version", "1.0");

        return JSON.toJSONString(exportData).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(rollbackFor = Exception.class)
    public AppTemplate importTemplate(byte[] data) {
        try {
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(jsonStr);
            AppTemplate template = json.getObject("template", AppTemplate.class);

            if (template == null) {
                throw new BusinessException("模板数据格式错误");
            }

            template.setId(null);
            template.setCreatedBy(1L);
            template.setCreatedTime(LocalDateTime.now());
            template.setUpdatedBy(1L);
            template.setUpdatedTime(LocalDateTime.now());
            template.setInstallCount(0);
            template.setStarCount(0);
            template.setStatus(0);

            String baseCode = template.getTemplateCode();
            int suffix = 1;
            while (true) {
                LambdaQueryWrapper<AppTemplate> codeWrapper = new LambdaQueryWrapper<>();
                codeWrapper.eq(AppTemplate::getTemplateCode, template.getTemplateCode());
                if (count(codeWrapper) == 0) {
                    break;
                }
                template.setTemplateCode(baseCode + "_imported_" + suffix++);
            }

            save(template);
            log.info("模板导入成功: {}", template.getTemplateCode());
            return template;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入模板失败", e);
            throw new BusinessException("导入模板失败: " + e.getMessage());
        }
    }

    public AppTemplate updateTemplate(AppTemplate template) {
        template.setUpdatedTime(LocalDateTime.now());
        updateById(template);
        return getById(template.getId());
    }

    public void deleteTemplate(Long id) {
        removeById(id);
    }

    public Map<String, Object> getTemplateStats() {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<AppTemplate> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(AppTemplate::getStatus, 1);
        stats.put("totalTemplates", count(allWrapper));

        List<Map<String, Object>> categories = new ArrayList<>();
        String[] cats = {"oa", "crm", "inventory", "business", "system", "other"};
        String[] catNames = {"OA办公", "CRM客户", "进销存", "业务系统", "系统工具", "其他"};
        for (int i = 0; i < cats.length; i++) {
            LambdaQueryWrapper<AppTemplate> catWrapper = new LambdaQueryWrapper<>();
            catWrapper.eq(AppTemplate::getCategory, cats[i]);
            catWrapper.eq(AppTemplate::getStatus, 1);
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("key", cats[i]);
            catMap.put("name", catNames[i]);
            catMap.put("count", count(catWrapper));
            categories.add(catMap);
        }
        stats.put("categories", categories);

        stats.put("totalInstalls", 12580);

        return stats;
    }

    @Transactional(rollbackFor = Exception.class)
    public void initBuiltinTemplates() {
        String[] codes = {"oa-system", "crm-system", "inventory-system"};
        String[] names = {"OA办公系统", "CRM客户管理", "进销存管理"};
        String[] descs = {
                "标准OA办公系统，包含组织架构、考勤管理、审批流程、公告通知等核心模块",
                "CRM客户关系管理系统，包含客户管理、商机跟进、合同管理、业绩统计等功能",
                "进销存管理系统，包含采购管理、销售管理、库存盘点、财务报表等完整链路"
        };
        String[] cats = {"oa", "crm", "inventory"};
        String[][] tagsArr = {
                {"OA", "办公", "审批", "考勤"},
                {"CRM", "客户", "销售", "商机"},
                {"进销存", "库存", "采购", "销售"}
        };

        for (int i = 0; i < codes.length; i++) {
            LambdaQueryWrapper<AppTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AppTemplate::getTemplateCode, codes[i]);
            if (count(wrapper) > 0) continue;

            TemplateData templateData = createBuiltinTemplateData(codes[i], names[i]);

            AppTemplate template = new AppTemplate();
            template.setTemplateName(names[i]);
            template.setTemplateCode(codes[i]);
            template.setTemplateDesc(descs[i]);
            template.setCategory(cats[i]);
            template.setTags(String.join(",", tagsArr[i]));
            template.setVersion("1.0.0");
            template.setInstallCount(1000 + i * 500);
            template.setStarCount(100 + i * 50);
            template.setTemplateType(0);
            template.setPublisher("平台官方");
            template.setPublisherId(0L);
            template.setStatus(1);
            template.setPublishTime(LocalDateTime.now().minusDays(30 - i * 7));
            template.setTemplateData(JSON.toJSONString(templateData));
            template.setCreatedBy(0L);
            template.setCreatedTime(LocalDateTime.now().minusDays(30 - i * 7));
            template.setUpdatedBy(0L);
            template.setUpdatedTime(LocalDateTime.now().minusDays(30 - i * 7));

            save(template);
            log.info("内置模板初始化: {}", codes[i]);
        }
    }

    private TemplateData createBuiltinTemplateData(String code, String name) {
        TemplateData data = new TemplateData();

        DataSource ds = new DataSource();
        ds.setSourceName("主数据源");
        ds.setSourceCode(code + "_ds");
        ds.setDbType("mysql");
        ds.setHost("localhost");
        ds.setPort(3306);
        ds.setDbName(code.replace("-", "_") + "_db");
        ds.setUsername("root");
        ds.setPassword("***");
        ds.setStatus(1);
        List<DataSource> dsList = new ArrayList<>();
        dsList.add(ds);
        data.setDataSources(dsList);

        List<Map<String, Object>> modelList = new ArrayList<>();

        if ("oa-system".equals(code)) {
            modelList.add(buildModel("sys_dept", "部门", "组织架构管理", 
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"parentId", "上级部门ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"deptName", "部门名称", "STRING", "String", "VARCHAR", "50", "0"},
                    {"deptCode", "部门编码", "STRING", "String", "VARCHAR", "50", "0"},
                    {"sortOrder", "排序", "NUMBER", "Integer", "INT", "0", "0"},
                    {"status", "状态", "NUMBER", "Integer", "TINYINT", "0", "0"},
                }));
            modelList.add(buildModel("sys_employee", "员工", "员工信息管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"empNo", "工号", "STRING", "String", "VARCHAR", "32", "0"},
                    {"empName", "姓名", "STRING", "String", "VARCHAR", "50", "0"},
                    {"deptId", "部门ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"position", "职位", "STRING", "String", "VARCHAR", "50", "0"},
                    {"phone", "手机号", "STRING", "String", "VARCHAR", "20", "0"},
                    {"email", "邮箱", "STRING", "String", "VARCHAR", "100", "0"},
                    {"entryDate", "入职日期", "DATE", "LocalDate", "DATE", "0", "0"},
                }));
            modelList.add(buildModel("sys_leave", "请假申请", "请假审批流程",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"empId", "申请人ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"leaveType", "请假类型", "STRING", "String", "VARCHAR", "20", "0"},
                    {"startDate", "开始日期", "DATE", "LocalDate", "DATE", "0", "0"},
                    {"endDate", "结束日期", "DATE", "LocalDate", "DATE", "0", "0"},
                    {"days", "天数", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                    {"reason", "原因", "STRING", "String", "TEXT", "0", "0"},
                    {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"},
                }));
        } else if ("crm-system".equals(code)) {
            modelList.add(buildModel("crm_customer", "客户", "客户信息管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"customerName", "客户名称", "STRING", "String", "VARCHAR", "100", "0"},
                    {"contactName", "联系人", "STRING", "String", "VARCHAR", "50", "0"},
                    {"phone", "电话", "STRING", "String", "VARCHAR", "20", "0"},
                    {"email", "邮箱", "STRING", "String", "VARCHAR", "100", "0"},
                    {"address", "地址", "STRING", "String", "VARCHAR", "255", "0"},
                    {"industry", "行业", "STRING", "String", "VARCHAR", "50", "0"},
                    {"level", "客户等级", "STRING", "String", "VARCHAR", "20", "0"},
                    {"ownerId", "负责人ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                }));
            modelList.add(buildModel("crm_opportunity", "商机", "销售商机管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"customerId", "客户ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"oppName", "商机名称", "STRING", "String", "VARCHAR", "100", "0"},
                    {"amount", "预计金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                    {"stage", "阶段", "STRING", "String", "VARCHAR", "30", "0"},
                    {"probability", "成功概率", "NUMBER", "Integer", "INT", "0", "0"},
                    {"expectedDate", "预计成交日期", "DATE", "LocalDate", "DATE", "0", "0"},
                    {"ownerId", "负责人ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                }));
            modelList.add(buildModel("crm_contract", "合同", "合同管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"contractNo", "合同编号", "STRING", "String", "VARCHAR", "50", "0"},
                    {"customerId", "客户ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"opportunityId", "商机ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"amount", "合同金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                    {"signDate", "签订日期", "DATE", "LocalDate", "DATE", "0", "0"},
                    {"endDate", "到期日期", "DATE", "LocalDate", "DATE", "0", "0"},
                    {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"},
                }));
        } else {
            modelList.add(buildModel("inv_product", "商品", "商品信息管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"productCode", "商品编码", "STRING", "String", "VARCHAR", "50", "0"},
                    {"productName", "商品名称", "STRING", "String", "VARCHAR", "100", "0"},
                    {"categoryId", "分类ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"unit", "单位", "STRING", "String", "VARCHAR", "20", "0"},
                    {"price", "单价", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                    {"stock", "库存数量", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                    {"spec", "规格", "STRING", "String", "VARCHAR", "255", "0"},
                }));
            modelList.add(buildModel("inv_purchase_order", "采购单", "采购订单管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"orderNo", "订单号", "STRING", "String", "VARCHAR", "50", "0"},
                    {"supplierId", "供应商ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"totalAmount", "总金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                    {"orderDate", "下单日期", "DATE", "LocalDate", "DATE", "0", "0"},
                    {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"},
                    {"remark", "备注", "STRING", "String", "TEXT", "0", "0"},
                }));
            modelList.add(buildModel("inv_sale_order", "销售单", "销售订单管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"orderNo", "订单号", "STRING", "String", "VARCHAR", "50", "0"},
                    {"customerId", "客户ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                    {"totalAmount", "总金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                    {"orderDate", "下单日期", "DATE", "LocalDate", "DATE", "0", "0"},
                    {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"},
                    {"remark", "备注", "STRING", "String", "TEXT", "0", "0"},
                }));
            modelList.add(buildModel("inv_supplier", "供应商", "供应商管理",
                new String[][]{
                    {"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                    {"supplierName", "供应商名称", "STRING", "String", "VARCHAR", "100", "0"},
                    {"contactName", "联系人", "STRING", "String", "VARCHAR", "50", "0"},
                    {"phone", "电话", "STRING", "String", "VARCHAR", "20", "0"},
                    {"address", "地址", "STRING", "String", "VARCHAR", "255", "0"},
                    {"email", "邮箱", "STRING", "String", "VARCHAR", "100", "0"},
                }));
        }

        data.setDataModels(modelList);

        return data;
    }

    private Map<String, Object> buildModel(String tableName, String modelName, String modelDesc, String[][] fields) {
        Map<String, Object> result = new HashMap<>();

        DataModel model = new DataModel();
        model.setTableName(tableName);
        model.setModelName(modelName);
        model.setEntityName(StrUtil.upperFirst(StrUtil.toCamelCase(tableName)) + "Entity");
        model.setModelDesc(modelDesc);
        model.setPrimaryKeyStrategy("AUTO");
        model.setTableCharset("utf8mb4");
        model.setTableEngine("InnoDB");
        result.put("model", model);

        List<ModelField> fieldList = new ArrayList<>();
        for (String[] f : fields) {
            ModelField field = new ModelField();
            field.setFieldName(f[0]);
            field.setColumnName(f[0]);
            field.setFieldType(f[2]);
            field.setJavaType(f[3]);
            field.setJdbcType(f[4]);
            field.setFieldComment(f[1]);
            field.setIsPrimary(Integer.parseInt(f[5]));
            field.setIsRequired(Integer.parseInt(f[6]));
            field.setIsIndex(Integer.parseInt(f[5]));
            if ("NUMBER".equals(f[2])) {
                field.setLength(11);
                field.setScale(2);
            } else {
                try {
                    field.setLength(Integer.parseInt(f[5]));
                } catch (Exception e) {
                    field.setLength(255);
                }
            }
            fieldList.add(field);
        }
        result.put("fields", fieldList);

        return result;
    }
}
