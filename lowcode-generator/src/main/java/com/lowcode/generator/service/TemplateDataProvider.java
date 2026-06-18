package com.lowcode.generator.service;

import com.alibaba.fastjson2.JSON;
import com.lowcode.generator.entity.TemplateData;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.entity.ModelField;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.PageComponent;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TemplateDataProvider {

    public Map<String, Object> buildModel(String tableName, String modelName, String modelDesc, String[][] fields) {
        Map<String, Object> result = new LinkedHashMap<>();

        DataModel model = new DataModel();
        model.setTableName(tableName);
        model.setModelName(modelName);
        model.setEntityName(toPascalCase(toCamelCase(tableName)) + "Entity");
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

    public Map<String, Object> buildPage(String pageName, String pageCode, String pageType,
                                        String pagePath, String layoutType, boolean isHome,
                                        List<Map<String, Object>> components) {
        Map<String, Object> result = new LinkedHashMap<>();

        Page page = new Page();
        page.setPageName(pageName);
        page.setPageCode(pageCode);
        page.setPageType(pageType);
        page.setPagePath(pagePath);
        page.setLayoutType(layoutType);
        page.setIsHome(isHome ? 1 : 0);
        page.setStatus(1);
        page.setVersion("1.0.0");
        result.put("page", page);

        List<PageComponent> compList = new ArrayList<>();
        if (components != null) {
            for (int i = 0; i < components.size(); i++) {
                Map<String, Object> compConfig = components.get(i);
                PageComponent comp = new PageComponent();
                comp.setComponentId("comp_" + System.currentTimeMillis() + "_" + i);
                comp.setComponentType((String) compConfig.get("type"));
                comp.setComponentName((String) compConfig.get("name"));
                comp.setParentId((String) compConfig.getOrDefault("parentId", "root"));
                comp.setSlotName((String) compConfig.getOrDefault("slotName", "default"));
                comp.setPropsConfig(JSON.toJSONString(compConfig.get("props")));
                comp.setStyleConfig(JSON.toJSONString(compConfig.get("style")));
                comp.setPositionX((Integer) compConfig.getOrDefault("positionX", 0));
                comp.setPositionY((Integer) compConfig.getOrDefault("positionY", 0));
                comp.setSortOrder(i);
                compList.add(comp);
            }
        }
        result.put("components", compList);

        return result;
    }

    public List<Map<String, Object>> createOAPages() {
        List<Map<String, Object>> pages = new ArrayList<>();

        pages.add(buildPage("首页", "home", "DASHBOARD", "/home", "default", true,
                createHomeComponents("OA办公系统")));

        pages.add(buildPage("部门管理", "dept_list", "LIST", "/dept", "table", false,
                createListPageComponents("sys_dept", "部门",
                        new String[]{"deptName", "部门名称"},
                        new String[]{"deptCode", "部门编码"},
                        new String[]{"leader", "负责人"},
                        new String[]{"phone", "联系电话"},
                        new String[]{"status", "状态"})));

        pages.add(buildPage("员工管理", "employee_list", "LIST", "/employee", "table", false,
                createListPageComponents("sys_employee", "员工",
                        new String[]{"empNo", "工号"},
                        new String[]{"empName", "姓名"},
                        new String[]{"deptId", "部门"},
                        new String[]{"position", "职位"},
                        new String[]{"phone", "手机号"},
                        new String[]{"entryDate", "入职日期"},
                        new String[]{"status", "状态"})));

        pages.add(buildPage("请假申请", "leave_list", "LIST", "/leave", "table", false,
                createListPageComponents("sys_leave", "请假申请",
                        new String[]{"empId", "申请人"},
                        new String[]{"leaveType", "请假类型"},
                        new String[]{"startDate", "开始日期"},
                        new String[]{"endDate", "结束日期"},
                        new String[]{"days", "天数"},
                        new String[]{"status", "状态"})));

        return pages;
    }

    public List<Map<String, Object>> createCRMPages() {
        List<Map<String, Object>> pages = new ArrayList<>();

        pages.add(buildPage("销售仪表盘", "crm_dashboard", "DASHBOARD", "/dashboard", "default", true,
                createHomeComponents("CRM客户管理系统")));

        pages.add(buildPage("客户管理", "customer_list", "LIST", "/customer", "table", false,
                createListPageComponents("crm_customer", "客户",
                        new String[]{"customerName", "客户名称"},
                        new String[]{"contactName", "联系人"},
                        new String[]{"phone", "电话"},
                        new String[]{"industry", "行业"},
                        new String[]{"level", "客户等级"},
                        new String[]{"source", "客户来源"})));

        pages.add(buildPage("商机管理", "opportunity_list", "LIST", "/opportunity", "table", false,
                createListPageComponents("crm_opportunity", "商机",
                        new String[]{"oppName", "商机名称"},
                        new String[]{"customerId", "客户"},
                        new String[]{"amount", "预计金额"},
                        new String[]{"stage", "阶段"},
                        new String[]{"probability", "成功概率"},
                        new String[]{"expectedDate", "预计成交日期"})));

        pages.add(buildPage("合同管理", "contract_list", "LIST", "/contract", "table", false,
                createListPageComponents("crm_contract", "合同",
                        new String[]{"contractNo", "合同编号"},
                        new String[]{"customerId", "客户"},
                        new String[]{"amount", "合同金额"},
                        new String[]{"signDate", "签订日期"},
                        new String[]{"endDate", "到期日期"},
                        new String[]{"status", "状态"})));

        return pages;
    }

    public List<Map<String, Object>> createInventoryPages() {
        List<Map<String, Object>> pages = new ArrayList<>();

        pages.add(buildPage("库存仪表盘", "inv_dashboard", "DASHBOARD", "/dashboard", "default", true,
                createHomeComponents("进销存管理系统")));

        pages.add(buildPage("商品管理", "product_list", "LIST", "/product", "table", false,
                createListPageComponents("inv_product", "商品",
                        new String[]{"productCode", "商品编码"},
                        new String[]{"productName", "商品名称"},
                        new String[]{"price", "单价"},
                        new String[]{"cost", "成本价"},
                        new String[]{"stock", "库存数量"},
                        new String[]{"warnStock", "预警库存"},
                        new String[]{"unit", "单位"})));

        pages.add(buildPage("采购管理", "purchase_list", "LIST", "/purchase", "table", false,
                createListPageComponents("inv_purchase_order", "采购单",
                        new String[]{"orderNo", "订单号"},
                        new String[]{"supplierId", "供应商"},
                        new String[]{"totalAmount", "总金额"},
                        new String[]{"orderDate", "下单日期"},
                        new String[]{"status", "状态"})));

        pages.add(buildPage("销售管理", "sale_list", "LIST", "/sale", "table", false,
                createListPageComponents("inv_sale_order", "销售单",
                        new String[]{"orderNo", "订单号"},
                        new String[]{"customerId", "客户"},
                        new String[]{"totalAmount", "总金额"},
                        new String[]{"orderDate", "下单日期"},
                        new String[]{"status", "状态"})));

        pages.add(buildPage("供应商管理", "supplier_list", "LIST", "/supplier", "table", false,
                createListPageComponents("inv_supplier", "供应商",
                        new String[]{"supplierName", "供应商名称"},
                        new String[]{"contactName", "联系人"},
                        new String[]{"phone", "电话"},
                        new String[]{"email", "邮箱"},
                        new String[]{"address", "地址"})));

        return pages;
    }

    private List<Map<String, Object>> createHomeComponents(String title) {
        List<Map<String, Object>> components = new ArrayList<>();

        Map<String, Object> titleComp = new LinkedHashMap<>();
        titleComp.put("type", "Title");
        titleComp.put("name", "欢迎标题");
        titleComp.put("parentId", "root");
        titleComp.put("props", Map.of("text", "欢迎使用" + title, "level", 3));
        titleComp.put("style", Map.of("margin", "24px 0 16px 0"));
        components.add(titleComp);

        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("type", "Row");
        row1.put("name", "统计行");
        row1.put("parentId", "root");
        row1.put("props", Map.of("gutter", 16));
        row1.put("style", Map.of("marginBottom", "16px"));
        components.add(row1);

        String[] stats = {"总记录数", "今日新增", "本月新增", "待处理"};
        for (int i = 0; i < 4; i++) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("type", "Col");
            col.put("name", "统计列" + (i + 1));
            col.put("parentId", "root");
            col.put("props", Map.of("span", 6));
            col.put("style", Map.of());
            components.add(col);

            Map<String, Object> card = new LinkedHashMap<>();
            card.put("type", "Card");
            card.put("name", stats[i] + "卡片");
            card.put("parentId", "root");
            card.put("props", Map.of("title", stats[i]));
            card.put("style", Map.of("textAlign", "center"));
            components.add(card);

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("type", "Statistic");
            stat.put("name", stats[i]);
            stat.put("parentId", "root");
            stat.put("props", Map.of("value", 0, "valueStyle", Map.of("fontSize", "28px", "color", "#1677ff")));
            stat.put("style", Map.of());
            components.add(stat);
        }

        return components;
    }

    private List<Map<String, Object>> createListPageComponents(String tableName, String label, String[]... columns) {
        List<Map<String, Object>> components = new ArrayList<>();

        Map<String, Object> pageComp = new LinkedHashMap<>();
        pageComp.put("type", "PageHeader");
        pageComp.put("name", "页面标题");
        pageComp.put("parentId", "root");
        pageComp.put("props", Map.of("title", label + "管理", "subTitle", "管理系统所有" + label + "数据"));
        pageComp.put("style", Map.of("marginBottom", "16px"));
        components.add(pageComp);

        Map<String, Object> toolbar = new LinkedHashMap<>();
        toolbar.put("type", "Toolbar");
        toolbar.put("name", "工具栏");
        toolbar.put("parentId", "root");
        toolbar.put("props", Map.of());
        toolbar.put("style", Map.of("marginBottom", "16px", "display", "flex", "gap", "8px"));
        components.add(toolbar);

        Map<String, Object> addBtn = new LinkedHashMap<>();
        addBtn.put("type", "Button");
        addBtn.put("name", "新增按钮");
        addBtn.put("parentId", "root");
        addBtn.put("props", Map.of("text", "新增" + label, "type", "primary"));
        addBtn.put("style", Map.of());
        components.add(addBtn);

        Map<String, Object> table = new LinkedHashMap<>();
        table.put("type", "Table");
        table.put("name", label + "表格");
        table.put("parentId", "root");
        table.put("props", Map.of(
                "columns", buildTableColumns(columns),
                "dataSource", "${tableData}",
                "rowKey", "id",
                "pagination", true
        ));
        table.put("style", Map.of("width", "100%"));
        components.add(table);

        return components;
    }

    private List<Map<String, Object>> buildTableColumns(String[]... columns) {
        List<Map<String, Object>> colList = new ArrayList<>();
        for (String[] col : columns) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("title", col[1]);
            c.put("dataIndex", col[0]);
            c.put("key", col[0]);
            colList.add(c);
        }
        Map<String, Object> actionCol = new LinkedHashMap<>();
        actionCol.put("title", "操作");
        actionCol.put("key", "action");
        actionCol.put("fixed", "right");
        colList.add(actionCol);
        return colList;
    }

    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    private String toPascalCase(String name) {
        if (name == null || name.isEmpty()) return name;
        String camel = toCamelCase(name);
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    public TemplateData createBuiltinTemplateData(String code) {
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
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"parentId", "上级部门ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"deptName", "部门名称", "STRING", "String", "VARCHAR", "50", "0"},
                            {"deptCode", "部门编码", "STRING", "String", "VARCHAR", "50", "0"},
                            {"leader", "部门负责人", "STRING", "String", "VARCHAR", "50", "0"},
                            {"phone", "联系电话", "STRING", "String", "VARCHAR", "20", "0"},
                            {"sortOrder", "排序", "NUMBER", "Integer", "INT", "0", "0"},
                            {"status", "状态", "NUMBER", "Integer", "TINYINT", "0", "0"}}));
            modelList.add(buildModel("sys_employee", "员工", "员工信息管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"empNo", "工号", "STRING", "String", "VARCHAR", "32", "1"},
                            {"empName", "姓名", "STRING", "String", "VARCHAR", "50", "0"},
                            {"gender", "性别", "STRING", "String", "VARCHAR", "10", "0"},
                            {"deptId", "部门ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"position", "职位", "STRING", "String", "VARCHAR", "50", "0"},
                            {"phone", "手机号", "STRING", "String", "VARCHAR", "20", "0"},
                            {"email", "邮箱", "STRING", "String", "VARCHAR", "100", "0"},
                            {"entryDate", "入职日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"status", "状态", "NUMBER", "Integer", "TINYINT", "0", "0"}}));
            modelList.add(buildModel("sys_leave", "请假申请", "请假审批流程",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"empId", "申请人ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"leaveType", "请假类型", "STRING", "String", "VARCHAR", "20", "0"},
                            {"startDate", "开始日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"endDate", "结束日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"days", "天数", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"reason", "原因", "STRING", "String", "TEXT", "0", "0"},
                            {"approverId", "审批人ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"}}));
            data.setPages(createOAPages());
        } else if ("crm-system".equals(code)) {
            modelList.add(buildModel("crm_customer", "客户", "客户信息管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"customerName", "客户名称", "STRING", "String", "VARCHAR", "100", "0"},
                            {"contactName", "联系人", "STRING", "String", "VARCHAR", "50", "0"},
                            {"phone", "电话", "STRING", "String", "VARCHAR", "20", "0"},
                            {"email", "邮箱", "STRING", "String", "VARCHAR", "100", "0"},
                            {"address", "地址", "STRING", "String", "VARCHAR", "255", "0"},
                            {"industry", "行业", "STRING", "String", "VARCHAR", "50", "0"},
                            {"level", "客户等级", "STRING", "String", "VARCHAR", "20", "0"},
                            {"source", "客户来源", "STRING", "String", "VARCHAR", "50", "0"},
                            {"ownerId", "负责人ID", "BIGINT", "Long", "BIGINT", "0", "0"}}));
            modelList.add(buildModel("crm_opportunity", "商机", "销售商机管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"customerId", "客户ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"oppName", "商机名称", "STRING", "String", "VARCHAR", "100", "0"},
                            {"amount", "预计金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"stage", "阶段", "STRING", "String", "VARCHAR", "30", "0"},
                            {"probability", "成功概率", "NUMBER", "Integer", "INT", "0", "0"},
                            {"expectedDate", "预计成交日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"ownerId", "负责人ID", "BIGINT", "Long", "BIGINT", "0", "0"}}));
            modelList.add(buildModel("crm_contract", "合同", "合同管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"contractNo", "合同编号", "STRING", "String", "VARCHAR", "50", "1"},
                            {"customerId", "客户ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"opportunityId", "商机ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"amount", "合同金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"signDate", "签订日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"endDate", "到期日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"}}));
            data.setPages(createCRMPages());
        } else {
            modelList.add(buildModel("inv_product", "商品", "商品信息管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"productCode", "商品编码", "STRING", "String", "VARCHAR", "50", "1"},
                            {"productName", "商品名称", "STRING", "String", "VARCHAR", "100", "0"},
                            {"categoryId", "分类ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"unit", "单位", "STRING", "String", "VARCHAR", "20", "0"},
                            {"price", "单价", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"cost", "成本价", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"stock", "库存数量", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"warnStock", "预警库存", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"spec", "规格", "STRING", "String", "VARCHAR", "255", "0"}}));
            modelList.add(buildModel("inv_purchase_order", "采购单", "采购订单管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"orderNo", "订单号", "STRING", "String", "VARCHAR", "50", "1"},
                            {"supplierId", "供应商ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"totalAmount", "总金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"orderDate", "下单日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"},
                            {"remark", "备注", "STRING", "String", "TEXT", "0", "0"}}));
            modelList.add(buildModel("inv_sale_order", "销售单", "销售订单管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"orderNo", "订单号", "STRING", "String", "VARCHAR", "50", "1"},
                            {"customerId", "客户ID", "BIGINT", "Long", "BIGINT", "0", "0"},
                            {"totalAmount", "总金额", "NUMBER", "BigDecimal", "DECIMAL", "0", "0"},
                            {"orderDate", "下单日期", "DATE", "LocalDate", "DATE", "0", "0"},
                            {"status", "状态", "STRING", "String", "VARCHAR", "20", "0"},
                            {"remark", "备注", "STRING", "String", "TEXT", "0", "0"}}));
            modelList.add(buildModel("inv_supplier", "供应商", "供应商管理",
                    new String[][]{{"id", "主键", "BIGINT", "Long", "BIGINT", "1", "1"},
                            {"supplierName", "供应商名称", "STRING", "String", "VARCHAR", "100", "0"},
                            {"contactName", "联系人", "STRING", "String", "VARCHAR", "50", "0"},
                            {"phone", "电话", "STRING", "String", "VARCHAR", "20", "0"},
                            {"address", "地址", "STRING", "String", "VARCHAR", "255", "0"},
                            {"email", "邮箱", "STRING", "String", "VARCHAR", "100", "0"}}));
            data.setPages(createInventoryPages());
        }

        return data;
    }
}
