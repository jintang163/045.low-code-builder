package com.lowcode.page.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.PageComponent;
import com.lowcode.page.entity.VersionSnapshot;
import com.lowcode.page.mapper.PageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PageService extends ServiceImpl<PageMapper, Page> {

    @Autowired
    private PageComponentService componentService;

    @Autowired
    @Lazy
    private VersionSnapshotService versionSnapshotService;

    public Page getPageDetail(Long id) {
        Page page = getById(id);
        if (page == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
        }

        LambdaQueryWrapper<PageComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PageComponent::getPageId, id);
        wrapper.orderByAsc(PageComponent::getSortOrder);
        List<PageComponent> components = componentService.list(wrapper);
        page.setComponents(components);

        return page;
    }

    public List<Page> getPageList(Long appId) {
        LambdaQueryWrapper<Page> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Page::getAppId, appId);
        wrapper.orderByDesc(Page::getCreatedTime);
        List<Page> pages = list(wrapper);
        return pages;
    }

    @Transactional(rollbackFor = Exception.class)
    public Page savePage(Page page) {
        LambdaQueryWrapper<Page> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Page::getPageCode, page.getPageCode());
        wrapper.eq(Page::getAppId, page.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PAGE_EXISTS);
        }

        if (page.getIsHome() != null && page.getIsHome() == 1) {
            clearHomePage(page.getAppId());
        }

        save(page);

        if (page.getComponents() != null) {
            for (PageComponent component : page.getComponents()) {
                component.setPageId(page.getId());
                componentService.save(component);
            }
        }

        versionSnapshotService.createAutoSnapshot("PAGE", page.getId(), page.getAppId());

        return getPageDetail(page.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Page updatePage(Page page) {
        Page existing = getById(page.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
        }

        if (page.getIsHome() != null && page.getIsHome() == 1) {
            clearHomePage(page.getAppId());
        }

        updateById(page);

        LambdaQueryWrapper<PageComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PageComponent::getPageId, page.getId());
        componentService.remove(wrapper);

        if (page.getComponents() != null) {
            for (PageComponent component : page.getComponents()) {
                component.setPageId(page.getId());
                componentService.save(component);
            }
        }

        versionSnapshotService.createAutoSnapshot("PAGE", page.getId(), page.getAppId());

        return getPageDetail(page.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePage(Long id) {
        Page page = getById(id);
        if (page == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
        }

        LambdaQueryWrapper<PageComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PageComponent::getPageId, id);
        componentService.remove(wrapper);

        removeById(id);
    }

    public Page publishPage(Long id) {
        Page page = getById(id);
        if (page == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
        }

        page.setStatus(1);
        updateById(page);
        return page;
    }

    public String generatePageCode(Long id) {
        Page page = getPageDetail(id);
        StringBuilder sb = new StringBuilder();

        sb.append("import React, { useState, useEffect } from 'react';\n");
        sb.append("import { Form, Input, Button, Table, Card, message } from 'antd';\n\n");
        sb.append("const ").append(page.getPageCode()).append(" = () => {\n");
        sb.append("  const [form] = Form.useForm();\n");
        sb.append("  const [data, setData] = useState([]);\n\n");
        sb.append("  useEffect(() => {\n");
        sb.append("    // 页面加载时执行的逻辑\n");
        sb.append("  }, []);\n\n");
        sb.append("  const handleSubmit = (values) => {\n");
        sb.append("    console.log('Form values:', values);\n");
        sb.append("    message.success('提交成功');\n");
        sb.append("  };\n\n");
        sb.append("  return (\n");
        sb.append("    <div style={{ padding: '24px' }}>\n");
        sb.append("      <Card title=\"").append(page.getPageName()).append("\">\n");
        sb.append("        <Form form={form} onFinish={handleSubmit} layout=\"vertical\">\n");

        for (PageComponent component : page.getComponents()) {
            if ("Input".equals(component.getComponentType())) {
                sb.append("          <Form.Item\n");
                sb.append("            label=\"").append(component.getComponentName()).append("\"\n");
                sb.append("            name=\"").append(component.getComponentId()).append("\"\n");
                sb.append("          >\n");
                sb.append("            <Input placeholder=\"请输入\" />\n");
                sb.append("          </Form.Item>\n");
            } else if ("Button".equals(component.getComponentType())) {
                sb.append("          <Form.Item>\n");
                sb.append("            <Button type=\"primary\" htmlType=\"submit\">\n");
                sb.append("              ").append(component.getComponentName()).append("\n");
                sb.append("            </Button>\n");
                sb.append("          </Form.Item>\n");
            } else if ("Table".equals(component.getComponentType())) {
                sb.append("          <Table\n");
                sb.append("            dataSource={data}\n");
                sb.append("            columns={[]}\n");
                sb.append("            rowKey=\"id\"\n");
                sb.append("          />\n");
            }
        }

        sb.append("        </Form>\n");
        sb.append("      </Card>\n");
        sb.append("    </div>\n");
        sb.append("  );\n");
        sb.append("};\n\n");
        sb.append("export default ").append(page.getPageCode()).append(";\n");

        return sb.toString();
    }

    public Map<String, Object> getPagePreviewData(Long id) {
        return getPagePreviewData(id, null);
    }

    public Map<String, Object> getPagePreviewData(Long pageId, Long snapshotId) {
        log.info("获取页面预览数据，pageId: {}, snapshotId: {}", pageId, snapshotId);

        Page page;
        if (snapshotId != null) {
            VersionSnapshot snapshot = versionSnapshotService.getSnapshotDetail(snapshotId);
            if (snapshot == null || snapshot.getPageSnapshot() == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "快照数据不存在");
            }
            page = JSON.parseObject(snapshot.getPageSnapshot(), Page.class);
            log.info("从快照获取页面数据成功，snapshotId: {}, version: {}", snapshotId, snapshot.getVersion());
        } else {
            page = getPageDetail(pageId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);

        List<Map<String, Object>> componentTree = buildComponentTree(page.getComponents());
        result.put("componentTree", componentTree);

        return result;
    }

    private List<Map<String, Object>> buildComponentTree(List<PageComponent> components) {
        if (components == null || components.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Map<String, Object>> componentMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (PageComponent component : components) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", component.getComponentId());
            node.put("type", component.getComponentType());
            node.put("name", component.getComponentName());
            node.put("props", component.getPropsConfig() != null ? JSON.parse(component.getPropsConfig()) : new HashMap<>());
            node.put("style", component.getStyleConfig() != null ? JSON.parse(component.getStyleConfig()) : new HashMap<>());
            node.put("events", component.getEventConfig() != null ? JSON.parse(component.getEventConfig()) : new HashMap<>());
            node.put("dataSource", component.getDataSourceConfig() != null ? JSON.parse(component.getDataSourceConfig()) : new HashMap<>());
            node.put("validation", component.getValidationConfig() != null ? JSON.parse(component.getValidationConfig()) : new HashMap<>());
            node.put("position", new HashMap<String, Object>() {{
                put("x", component.getPositionX());
                put("y", component.getPositionY());
                put("width", component.getWidth());
                put("height", component.getHeight());
            }});
            node.put("children", new ArrayList<Map<String, Object>>());

            componentMap.put(component.getComponentId(), node);
        }

        for (PageComponent component : components) {
            Map<String, Object> node = componentMap.get(component.getComponentId());
            String parentId = component.getParentId();

            if (parentId == null || parentId.isEmpty() || !componentMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                Map<String, Object> parent = componentMap.get(parentId);
                ((List<Map<String, Object>>) parent.get("children")).add(node);
            }
        }

        return roots;
    }

    public List<Page> getMobilePages(Long appId) {
        LambdaQueryWrapper<Page> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Page::getAppId, appId);
        wrapper.and(w -> w.like(Page::getLayoutType, "MOBILE_")
                .or().eq(Page::getPageType, "MOBILE"));
        wrapper.eq(Page::getStatus, 1);
        wrapper.orderByDesc(Page::getCreatedTime);
        return list(wrapper);
    }

    public String generateMobilePageSchema(Long pageId) {
        Page page = getPageDetail(pageId);
        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("pageId", page.getId());
        schema.put("pageName", page.getPageName());
        schema.put("pageCode", page.getPageCode());
        schema.put("layoutType", page.getLayoutType());
        schema.put("version", page.getVersion());

        if (page.getMobileConfig() != null && !page.getMobileConfig().isEmpty()) {
            schema.put("mobileConfig", JSON.parse(page.getMobileConfig()));
        } else {
            Map<String, Object> defaultMobileConfig = new LinkedHashMap<>();
            defaultMobileConfig.put("safeArea", true);
            defaultMobileConfig.put("orientation", "portrait");
            defaultMobileConfig.put("statusBarColor", "#ffffff");
            defaultMobileConfig.put("navigationBarColor", "#ffffff");
            schema.put("mobileConfig", defaultMobileConfig);
        }

        List<Map<String, Object>> componentSchemas = new ArrayList<>();
        if (page.getComponents() != null) {
            for (PageComponent component : page.getComponents()) {
                Map<String, Object> componentSchema = new LinkedHashMap<>();
                componentSchema.put("id", component.getComponentId());
                componentSchema.put("type", component.getComponentType());
                componentSchema.put("name", component.getComponentName());
                componentSchema.put("parentId", component.getParentId());

                if (component.getPropsConfig() != null && !component.getPropsConfig().isEmpty()) {
                    componentSchema.put("props", JSON.parse(component.getPropsConfig()));
                }
                if (component.getStyleConfig() != null && !component.getStyleConfig().isEmpty()) {
                    componentSchema.put("style", JSON.parse(component.getStyleConfig()));
                }
                if (component.getEventConfig() != null && !component.getEventConfig().isEmpty()) {
                    componentSchema.put("events", JSON.parse(component.getEventConfig()));
                }

                componentSchemas.add(componentSchema);
            }
        }
        schema.put("components", componentSchemas);

        return JSON.toJSONString(schema, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
    }

    @Transactional(rollbackFor = Exception.class)
    public Page copyPage(Long sourcePageId, String newPageName, String newPageCode, String copyMode) {
        Page sourcePage = getPageDetail(sourcePageId);
        if (sourcePage == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "源页面不存在");
        }

        if (newPageCode == null || newPageCode.isEmpty()) {
            newPageCode = sourcePage.getPageCode() + "_copy";
        }
        if (newPageName == null || newPageName.isEmpty()) {
            newPageName = sourcePage.getPageName() + "_副本";
        }

        LambdaQueryWrapper<Page> codeCheckWrapper = new LambdaQueryWrapper<>();
        codeCheckWrapper.eq(Page::getPageCode, newPageCode);
        codeCheckWrapper.eq(Page::getAppId, sourcePage.getAppId());
        Long count = count(codeCheckWrapper);
        if (count > 0) {
            throw new BusinessException("页面编码已存在");
        }

        Page newPage = new Page();
        newPage.setAppId(sourcePage.getAppId());
        newPage.setPageName(newPageName);
        newPage.setPageCode(newPageCode);
        newPage.setPageType(sourcePage.getPageType());
        newPage.setPagePath(sourcePage.getPagePath());
        newPage.setLayoutType(sourcePage.getLayoutType());
        newPage.setPageConfig(sourcePage.getPageConfig());
        newPage.setMobileConfig(sourcePage.getMobileConfig());
        newPage.setPageSchema(sourcePage.getPageSchema());
        newPage.setIsHome(0);
        newPage.setStatus(0);
        newPage.setVersion("1.0.0");

        save(newPage);

        if (!"structure".equals(copyMode) && sourcePage.getComponents() != null) {
            Map<String, String> idMapping = new HashMap<>();
            List<PageComponent> newComponents = new ArrayList<>();

            for (PageComponent sourceComponent : sourcePage.getComponents()) {
                PageComponent newComponent = new PageComponent();
                String newComponentId = UUID.randomUUID().toString().replace("-", "");
                idMapping.put(sourceComponent.getComponentId(), newComponentId);

                newComponent.setPageId(newPage.getId());
                newComponent.setComponentId(newComponentId);
                newComponent.setComponentType(sourceComponent.getComponentType());
                newComponent.setComponentName(sourceComponent.getComponentName());
                newComponent.setParentId(sourceComponent.getParentId());
                newComponent.setSlotName(sourceComponent.getSlotName());
                newComponent.setPropsConfig(sourceComponent.getPropsConfig());
                newComponent.setStyleConfig(sourceComponent.getStyleConfig());
                newComponent.setEventConfig(sourceComponent.getEventConfig());
                newComponent.setDataSourceConfig(sourceComponent.getDataSourceConfig());
                newComponent.setValidationConfig(sourceComponent.getValidationConfig());
                newComponent.setPositionX(sourceComponent.getPositionX());
                newComponent.setPositionY(sourceComponent.getPositionY());
                newComponent.setWidth(sourceComponent.getWidth());
                newComponent.setHeight(sourceComponent.getHeight());
                newComponent.setSortOrder(sourceComponent.getSortOrder());

                newComponents.add(newComponent);
            }

            for (PageComponent component : newComponents) {
                if (component.getParentId() != null && idMapping.containsKey(component.getParentId())) {
                    component.setParentId(idMapping.get(component.getParentId()));
                }
                componentService.save(component);
            }
        }

        versionSnapshotService.createAutoSnapshot("PAGE", newPage.getId(), newPage.getAppId());

        return getPageDetail(newPage.getId());
    }

    private void clearHomePage(Long appId) {
        LambdaQueryWrapper<Page> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Page::getAppId, appId);
        wrapper.eq(Page::getIsHome, 1);
        List<Page> homePages = list(wrapper);
        for (Page homePage : homePages) {
            homePage.setIsHome(0);
            updateById(homePage);
        }
    }
}
