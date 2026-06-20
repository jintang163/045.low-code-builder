package com.lowcode.generator.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.generator.entity.GeneratedApp;
import com.lowcode.generator.entity.GeneratedCode;
import com.lowcode.generator.entity.GeneratedUniAppCode;
import com.lowcode.generator.entity.MobileGenerateConfig;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.service.DataModelService;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.PageComponent;
import com.lowcode.page.service.PageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UniAppCodeGeneratorService {

    @Autowired
    private AppGeneratorService appGeneratorService;

    @Autowired
    private PageService pageService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private VelocityEngine velocityEngine;

    private static final String TEMPLATE_BASE = "templates/uniapp/";
    private static final String PLATFORM_H5 = "h5";
    private static final String PLATFORM_MP_WEIXIN = "mp-weixin";
    private static final String PLATFORM_MP_ALIPAY = "mp-alipay";
    private static final String PLATFORM_APP = "app";

    public List<GeneratedUniAppCode> generateUniAppProject(MobileGenerateConfig config) {
        log.info("开始生成 uni-app 项目: appName={}, appCode={}", config.getAppName(), config.getAppCode());
        List<GeneratedUniAppCode> codes = new ArrayList<>();

        try {
            Map<String, Object> context = buildBaseContext(config);

            codes.add(generatePackageJson(config, context));
            codes.add(generateManifestJson(config, context));
            codes.add(generateAppVue(config, context));
            codes.add(generateMainTs(config, context));
            codes.add(generateViteConfig(config, context));
            codes.add(generateTsConfig(config, context));
            codes.add(generateUniScss(config, context));
            codes.add(generateGitignore(config, context));
            codes.add(generateIndexHtml(config, context));

            List<Page> pages = loadPages(config);
            codes.add(generatePagesJsonFile(pages, config, context));

            for (Page page : pages) {
                codes.addAll(generatePage(page, config));
            }

            List<DataModel> models = loadDataModels(config);
            for (DataModel model : models) {
                codes.add(generateApiService(model));
                codes.add(generateStoreModule(model));
            }

            codes.addAll(generateUtils(config, context));

            codes.addAll(generateMobileComponents(config));

            log.info("uni-app 项目生成完成, 共生成 {} 个文件", codes.size());
        } catch (Exception e) {
            log.error("生成 uni-app 项目失败", e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "生成 uni-app 项目失败: " + e.getMessage());
        }

        return codes;
    }

    public List<GeneratedUniAppCode> generatePage(Page page, MobileGenerateConfig config) {
        log.info("开始生成页面代码: pageName={}, pageCode={}", page.getPageName(), page.getPageCode());
        List<GeneratedUniAppCode> codes = new ArrayList<>();

        try {
            Page pageDetail = pageService.getPageDetail(page.getId());

            String pagePath = "pages/" + page.getPageCode() + "/index";
            String filePath = pagePath + ".vue";

            StringBuilder template = new StringBuilder();
            template.append("<template>\n");
            template.append("  <view class=\"").append(page.getPageCode()).append("-page\">\n");

            if (pageDetail.getComponents() != null) {
                for (PageComponent component : pageDetail.getComponents()) {
                    template.append(componentToVueTemplate(component));
                }
            }

            template.append("  </view>\n");
            template.append("</template>\n\n");

            template.append("<script setup lang=\"ts\">\n");
            template.append("import { ref, reactive, onMounted } from 'vue'\n");
            template.append("import { onLoad, onShow } from '@dcloudio/uni-app'\n\n");

            template.append("const pageTitle = ref('").append(page.getPageName()).append("')\n\n");

            if (config.getEnableTouchEvents() != null && config.getEnableTouchEvents()) {
                template.append("import { useTouch } from '@/utils/touch'\n");
                template.append("const { touchStart, touchMove, touchEnd } = useTouch()\n\n");
            }

            if (config.getEnableGestures() != null && config.getEnableGestures()) {
                template.append("import { useGesture } from '@/utils/gesture'\n");
                template.append("const { onSwipe, onLongPress, onDoubleTap } = useGesture()\n\n");
            }

            template.append("onLoad((options) => {\n");
            template.append("  console.log('页面加载:', options)\n");
            template.append("})\n\n");

            template.append("onShow(() => {\n");
            template.append("  console.log('页面显示')\n");
            template.append("})\n\n");

            template.append("onMounted(() => {\n");
            template.append("  console.log('页面挂载完成')\n");
            template.append("})\n");
            template.append("</script>\n\n");

            template.append("<style lang=\"scss\" scoped>\n");
            template.append(".").append(page.getPageCode()).append("-page {\n");
            template.append("  min-height: 100vh;\n");
            template.append("  background-color: #f5f5f5;\n");
            template.append("  padding: 16px;\n");
            template.append("  box-sizing: border-box;\n");

            if (config.getResponsiveConfig() != null) {
                try {
                    JSONObject responsiveConfig = JSON.parseObject(config.getResponsiveConfig());
                    String designWidth = responsiveConfig.getString("designWidth");
                    if (designWidth != null) {
                        template.append("\n  /* 响应式设计基准宽度: ").append(designWidth).append("px */\n");
                    }
                } catch (Exception e) {
                    log.warn("解析响应式配置失败", e);
                }
            }

            template.append("}\n");
            template.append("</style>\n");

            List<String> platforms = config.getTargetPlatforms() != null ?
                    config.getTargetPlatforms() : Arrays.asList(PLATFORM_H5, PLATFORM_MP_WEIXIN);

            for (String platform : platforms) {
                codes.add(new GeneratedUniAppCode(
                        "PAGE",
                        page.getPageCode() + ".vue",
                        filePath,
                        template.toString(),
                        platform
                ));
            }

            log.info("页面代码生成完成: {}", page.getPageCode());
        } catch (Exception e) {
            log.error("生成页面代码失败: pageCode={}", page.getPageCode(), e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "生成页面代码失败: " + e.getMessage());
        }

        return codes;
    }

    public String generateComponent(PageComponent component) {
        log.info("开始生成组件代码: componentType={}, componentName={}",
                component.getComponentType(), component.getComponentName());

        try {
            StringBuilder template = new StringBuilder();
            String componentType = component.getComponentType();
            String componentName = StrUtil.toCamelCase(component.getComponentId());

            Map<String, Object> props = component.getPropsConfig() != null ?
                    JSON.parseObject(component.getPropsConfig()) : new HashMap<>();
            Map<String, Object> style = component.getStyleConfig() != null ?
                    JSON.parseObject(component.getStyleConfig()) : new HashMap<>();
            Map<String, Object> events = component.getEventsConfig() != null ?
                    JSON.parseObject(component.getEventsConfig()) : new HashMap<>();

            switch (componentType) {
                case "MobileGrid":
                    template.append(generateMobileGrid(componentName, props, style, events));
                    break;
                case "MobileCollapse":
                    template.append(generateMobileCollapse(componentName, props, style, events));
                    break;
                case "MobileTabBar":
                    template.append(generateMobileTabBar(componentName, props, style, events));
                    break;
                case "MobileSwiper":
                    template.append(generateMobileSwiper(componentName, props, style, events));
                    break;
                case "MobileSearchBar":
                    template.append(generateMobileSearchBar(componentName, props, style, events));
                    break;
                case "MobilePullRefresh":
                    template.append(generateMobilePullRefresh(componentName, props, style, events));
                    break;
                case "MobileSwipeCell":
                    template.append(generateMobileSwipeCell(componentName, props, style, events));
                    break;
                case "MobileWaterfall":
                    template.append(generateMobileWaterfall(componentName, props, style, events));
                    break;
                default:
                    template.append(generateDefaultComponent(componentName, componentType, props, style, events));
            }

            log.info("组件代码生成完成: {}", componentType);
            return template.toString();
        } catch (Exception e) {
            log.error("生成组件代码失败: componentType={}", component.getComponentType(), e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "生成组件代码失败: " + e.getMessage());
        }
    }

    public GeneratedUniAppCode generateApiService(DataModel model) {
        log.info("开始生成 API 服务: modelName={}", model.getModelName());

        try {
            String modelName = StrUtil.toCamelCase(model.getEntityName());
            String apiFileName = StrUtil.toSymbolCase(model.getEntityName(), '-') + ".ts";
            String filePath = "api/" + apiFileName;

            StringBuilder sb = new StringBuilder();
            sb.append("import request from '@/utils/request'\n\n");
            sb.append("/**\n");
            sb.append(" * ").append(model.getModelDesc() != null ? model.getModelDesc() : model.getModelName()).append(" API服务\n");
            sb.append(" */\n\n");

            String basePath = "/" + StrUtil.toSymbolCase(model.getEntityName(), '-');

            sb.append("const ").append(modelName).append("Api = {\n\n");

            sb.append("  /** 新增 */\n");
            sb.append("  save(data: any) {\n");
            sb.append("    return request({\n");
            sb.append("      url: '").append(basePath).append("',\n");
            sb.append("      method: 'POST',\n");
            sb.append("      data\n");
            sb.append("    })\n");
            sb.append("  },\n\n");

            sb.append("  /** 修改 */\n");
            sb.append("  update(data: any) {\n");
            sb.append("    return request({\n");
            sb.append("      url: '").append(basePath).append("',\n");
            sb.append("      method: 'PUT',\n");
            sb.append("      data\n");
            sb.append("    })\n");
            sb.append("  },\n\n");

            sb.append("  /** 删除 */\n");
            sb.append("  delete(id: number | string) {\n");
            sb.append("    return request({\n");
            sb.append("      url: `${basePath}/${id}`,\n");
            sb.append("      method: 'DELETE'\n");
            sb.append("    })\n");
            sb.append("  },\n\n");

            sb.append("  /** 根据ID查询 */\n");
            sb.append("  getById(id: number | string) {\n");
            sb.append("    return request({\n");
            sb.append("      url: `${basePath}/${id}`,\n");
            sb.append("      method: 'GET'\n");
            sb.append("    })\n");
            sb.append("  },\n\n");

            sb.append("  /** 查询列表 */\n");
            sb.append("  list(params?: any) {\n");
            sb.append("    return request({\n");
            sb.append("      url: '").append(basePath).append("/list',\n");
            sb.append("      method: 'GET',\n");
            sb.append("      params\n");
            sb.append("    })\n");
            sb.append("  },\n\n");

            sb.append("  /** 分页查询 */\n");
            sb.append("  page(params?: any) {\n");
            sb.append("    return request({\n");
            sb.append("      url: '").append(basePath).append("/page',\n");
            sb.append("      method: 'GET',\n");
            sb.append("      params\n");
            sb.append("    })\n");
            sb.append("  }\n\n");

            sb.append("}\n\n");
            sb.append("export default ").append(modelName).append("Api\n");

            return new GeneratedUniAppCode("API_SERVICE", apiFileName, filePath, sb.toString(), PLATFORM_H5);
        } catch (Exception e) {
            log.error("生成 API 服务失败: modelName={}", model.getModelName(), e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "生成 API 服务失败: " + e.getMessage());
        }
    }

    public GeneratedUniAppCode generateStoreModule(DataModel model) {
        log.info("开始生成 Pinia 状态管理模块: modelName={}", model.getModelName());

        try {
            String modelName = StrUtil.toCamelCase(model.getEntityName());
            String storeFileName = StrUtil.toSymbolCase(model.getEntityName(), '-') + ".ts";
            String filePath = "stores/" + storeFileName;
            String apiName = StrUtil.toSymbolCase(model.getEntityName(), '-');

            StringBuilder sb = new StringBuilder();
            sb.append("import { defineStore } from 'pinia'\n");
            sb.append("import { ref, reactive } from 'vue'\n");
            sb.append("import ").append(modelName).append("Api from '@/api/").append(apiName).append("'\n\n");
            sb.append("/**\n");
            sb.append(" * ").append(model.getModelDesc() != null ? model.getModelDesc() : model.getModelName()).append(" 状态管理\n");
            sb.append(" */\n");
            sb.append("export const use").append(model.getEntityName()).append("Store = defineStore('").append(modelName).append("', () => {\n\n");

            sb.append("  const list = ref<any[]>([])\n");
            sb.append("  const current = ref<any>(null)\n");
            sb.append("  const loading = ref(false)\n");
            sb.append("  const pagination = reactive({\n");
            sb.append("    current: 1,\n");
            sb.append("    pageSize: 10,\n");
            sb.append("    total: 0\n");
            sb.append("  })\n\n");

            sb.append("  async function fetchList(params?: any) {\n");
            sb.append("    loading.value = true\n");
            sb.append("    try {\n");
            sb.append("      const res = await ").append(modelName).append("Api.list(params)\n");
            sb.append("      list.value = res?.data || res || []\n");
            sb.append("      return list.value\n");
            sb.append("    } finally {\n");
            sb.append("      loading.value = false\n");
            sb.append("    }\n");
            sb.append("  }\n\n");

            sb.append("  async function fetchPage(params?: any) {\n");
            sb.append("    loading.value = true\n");
            sb.append("    try {\n");
            sb.append("      const res = await ").append(modelName).append("Api.page({\n");
            sb.append("        current: pagination.current,\n");
            sb.append("        size: pagination.pageSize,\n");
            sb.append("        ...params\n");
            sb.append("      })\n");
            sb.append("      list.value = res?.records || res?.data?.records || []\n");
            sb.append("      pagination.total = res?.total || res?.data?.total || 0\n");
            sb.append("      return list.value\n");
            sb.append("    } finally {\n");
            sb.append("      loading.value = false\n");
            sb.append("    }\n");
            sb.append("  }\n\n");

            sb.append("  async function fetchById(id: number | string) {\n");
            sb.append("    loading.value = true\n");
            sb.append("    try {\n");
            sb.append("      const res = await ").append(modelName).append("Api.getById(id)\n");
            sb.append("      current.value = res?.data || res\n");
            sb.append("      return current.value\n");
            sb.append("    } finally {\n");
            sb.append("      loading.value = false\n");
            sb.append("    }\n");
            sb.append("  }\n\n");

            sb.append("  async function save(data: any) {\n");
            sb.append("    loading.value = true\n");
            sb.append("    try {\n");
            sb.append("      const res = await ").append(modelName).append("Api.save(data)\n");
            sb.append("      return res?.data || res\n");
            sb.append("    } finally {\n");
            sb.append("      loading.value = false\n");
            sb.append("    }\n");
            sb.append("  }\n\n");

            sb.append("  async function update(data: any) {\n");
            sb.append("    loading.value = true\n");
            sb.append("    try {\n");
            sb.append("      const res = await ").append(modelName).append("Api.update(data)\n");
            sb.append("      return res?.data || res\n");
            sb.append("    } finally {\n");
            sb.append("      loading.value = false\n");
            sb.append("    }\n");
            sb.append("  }\n\n");

            sb.append("  async function remove(id: number | string) {\n");
            sb.append("    loading.value = true\n");
            sb.append("    try {\n");
            sb.append("      const res = await ").append(modelName).append("Api.delete(id)\n");
            sb.append("      return res?.data || res\n");
            sb.append("    } finally {\n");
            sb.append("      loading.value = false\n");
            sb.append("    }\n");
            sb.append("  }\n\n");

            sb.append("  function setCurrent(item: any) {\n");
            sb.append("    current.value = item\n");
            sb.append("  }\n\n");

            sb.append("  function resetPagination() {\n");
            sb.append("    pagination.current = 1\n");
            sb.append("    pagination.pageSize = 10\n");
            sb.append("    pagination.total = 0\n");
            sb.append("  }\n\n");

            sb.append("  return {\n");
            sb.append("    list,\n");
            sb.append("    current,\n");
            sb.append("    loading,\n");
            sb.append("    pagination,\n");
            sb.append("    fetchList,\n");
            sb.append("    fetchPage,\n");
            sb.append("    fetchById,\n");
            sb.append("    save,\n");
            sb.append("    update,\n");
            sb.append("    remove,\n");
            sb.append("    setCurrent,\n");
            sb.append("    resetPagination\n");
            sb.append("  }\n");
            sb.append("})\n");

            return new GeneratedUniAppCode("STORE_MODULE", storeFileName, filePath, sb.toString(), PLATFORM_H5);
        } catch (Exception e) {
            log.error("生成 Pinia 状态管理模块失败: modelName={}", model.getModelName(), e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "生成 Pinia 状态管理模块失败: " + e.getMessage());
        }
    }

    private String loadTemplate(String templatePath) {
        log.info("加载模板: {}", templatePath);
        try {
            String fullPath = TEMPLATE_BASE + templatePath;
            Template template = velocityEngine.getTemplate(fullPath, "UTF-8");
            StringWriter writer = new StringWriter();
            template.merge(new VelocityContext(), writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("加载模板失败: {}", templatePath, e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "加载模板失败: " + templatePath);
        }
    }

    private String processTemplate(String templateContent, Map<String, Object> context) {
        log.debug("处理模板, context keys: {}", context.keySet());
        try {
            VelocityContext velocityContext = new VelocityContext();
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                velocityContext.put(entry.getKey(), entry.getValue());
            }

            StringWriter writer = new StringWriter();
            velocityEngine.evaluate(velocityContext, writer, "template", templateContent);
            return writer.toString();
        } catch (Exception e) {
            log.error("处理模板失败", e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "处理模板失败: " + e.getMessage());
        }
    }

    public String componentToVueTemplate(PageComponent component) {
        log.debug("组件转 Vue 模板: componentType={}", component.getComponentType());

        try {
            StringBuilder sb = new StringBuilder();
            String indent = "    ";

            Map<String, Object> props = component.getPropsConfig() != null ?
                    JSON.parseObject(component.getPropsConfig()) : new HashMap<>();
            Map<String, Object> style = component.getStyleConfig() != null ?
                    JSON.parseObject(component.getStyleConfig()) : new HashMap<>();
            Map<String, Object> events = component.getEventsConfig() != null ?
                    JSON.parseObject(component.getEventsConfig()) : new HashMap<>();

            String tagName = mapComponentTypeToTag(component.getComponentType());
            String propsStr = buildPropsString(props);
            String styleStr = buildStyleString(style);
            String eventsStr = buildEventsString(events);

            sb.append(indent).append("<").append(tagName);
            if (!propsStr.isEmpty()) {
                sb.append(" ").append(propsStr);
            }
            if (!styleStr.isEmpty()) {
                sb.append(" :style=\"").append(styleStr).append("\"");
            }
            if (!eventsStr.isEmpty()) {
                sb.append(" ").append(eventsStr);
            }

            if (isSelfClosingTag(tagName)) {
                sb.append(" />\n");
            } else {
                sb.append(">\n");
                if (props.get("placeholder") != null) {
                    sb.append(indent).append("  ").append(props.get("placeholder")).append("\n");
                } else if (component.getComponentName() != null) {
                    sb.append(indent).append("  ").append(component.getComponentName()).append("\n");
                }
                sb.append(indent).append("</").append(tagName).append(">\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("组件转 Vue 模板失败", e);
            return "    <!-- 组件渲染错误: " + component.getComponentType() + " -->\n";
        }
    }

    public String buildPagesJson(List<Page> pages, MobileGenerateConfig config, Map<String, Object> context) {
        log.info("构建 pages.json 配置, 页面数量: {}", pages.size());

        try {
            String template = loadTemplate("pages.json.vm");

            List<Map<String, Object>> pageList = new ArrayList<>();
            String homePagePath = "pages/index/index";

            for (Page page : pages) {
                Map<String, Object> pageConfig = new LinkedHashMap<>();
                String pagePath = "pages/" + page.getPageCode() + "/index";

                if (page.getIsHome() != null && page.getIsHome() == 1) {
                    homePagePath = pagePath;
                }

                pageConfig.put("path", pagePath);
                Map<String, Object> style = new LinkedHashMap<>();
                style.put("navigationBarTitleText", page.getPageName());
                style.put("enablePullDownRefresh", true);
                pageConfig.put("style", style);
                pageList.add(pageConfig);
            }

            if (!pageList.isEmpty() && !homePagePath.equals(pageList.get(0).get("path"))) {
                for (int i = 0; i < pageList.size(); i++) {
                    if (homePagePath.equals(pageList.get(i).get("path"))) {
                        Map<String, Object> homePage = pageList.remove(i);
                        pageList.add(0, homePage);
                        break;
                    }
                }
            }

            context.put("pages", pageList);
            context.put("homePagePath", homePagePath);

            return processTemplate(template, context);
        } catch (Exception e) {
            log.error("构建 pages.json 配置失败", e);
            throw new BusinessException(ErrorCode.CODE_GENERATE_ERROR, "构建 pages.json 配置失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildBaseContext(MobileGenerateConfig config) {
        Map<String, Object> context = new HashMap<>();
        context.put("appName", config.getAppName());
        context.put("appCode", config.getAppCode());
        context.put("appDesc", config.getAppDesc() != null ? config.getAppDesc() : config.getAppName());
        context.put("version", config.getVersion() != null ? config.getVersion() : "1.0.0");
        context.put("author", config.getAuthor() != null ? config.getAuthor() : "lowcode-platform");
        context.put("appid", config.getAppid() != null ? config.getAppid() : "__UNI__LOWCODE");
        context.put("wechatAppid", config.getWechatAppid() != null ? config.getWechatAppid() : "");
        context.put("alipayAppid", config.getAlipayAppid() != null ? config.getAlipayAppid() : "");
        context.put("uniAppVersion", config.getUniAppVersion() != null ? config.getUniAppVersion() : "3.0.0-4020920240930001");
        context.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return context;
    }

    private GeneratedUniAppCode generatePackageJson(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("package.json.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("PACKAGE_JSON", "package.json", "package.json", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateManifestJson(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("manifest.json.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("MANIFEST_JSON", "manifest.json", "manifest.json", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateAppVue(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("App.vue.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("APP_VUE", "App.vue", "App.vue", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateMainTs(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("main.ts.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("MAIN_TS", "main.ts", "main.ts", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateViteConfig(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("vite.config.ts.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("VITE_CONFIG", "vite.config.ts", "vite.config.ts", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateTsConfig(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("tsconfig.json.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("TS_CONFIG", "tsconfig.json", "tsconfig.json", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateUniScss(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("uni.scss.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("UNI_SCSS", "uni.scss", "uni.scss", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateGitignore(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate(".gitignore.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("GITIGNORE", ".gitignore", ".gitignore", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generateIndexHtml(MobileGenerateConfig config, Map<String, Object> context) {
        String template = loadTemplate("index.html.vm");
        String content = processTemplate(template, context);
        return new GeneratedUniAppCode("INDEX_HTML", "index.html", "index.html", content, PLATFORM_H5);
    }

    private GeneratedUniAppCode generatePagesJsonFile(List<Page> pages, MobileGenerateConfig config, Map<String, Object> context) {
        String content = buildPagesJson(pages, config, context);
        return new GeneratedUniAppCode("PAGES_JSON", "pages.json", "pages.json", content, PLATFORM_H5);
    }

    private List<GeneratedUniAppCode> generateUtils(MobileGenerateConfig config, Map<String, Object> context) {
        List<GeneratedUniAppCode> codes = new ArrayList<>();

        String[] utils = {"touch.ts.vm", "gesture.ts.vm", "responsive.ts.vm", "eventBus.ts.vm", "nativeBridge.ts.vm"};
        for (String util : utils) {
            try {
                String template = loadTemplate("utils/" + util);
                String content = processTemplate(template, context);
                String fileName = util.replace(".vm", "");
                codes.add(new GeneratedUniAppCode("UTIL", fileName, "utils/" + fileName, content, PLATFORM_H5));
            } catch (Exception e) {
                log.warn("生成工具类失败: {}", util, e);
            }
        }

        return codes;
    }

    private List<GeneratedUniAppCode> generateMobileComponents(MobileGenerateConfig config) {
        List<GeneratedUniAppCode> codes = new ArrayList<>();

        String[] components = {
                "MobileGrid.vue", "MobileGridItem.vue", "MobileCollapse.vue",
                "MobileTabBar.vue", "MobileSwiper.vue", "MobileSearchBar.vue",
                "MobilePullRefresh.vue", "MobileSwipeCell.vue", "MobileWaterfall.vue"
        };

        for (String component : components) {
            try {
                String content = loadTemplate("components/" + component);
                codes.add(new GeneratedUniAppCode(
                        "MOBILE_COMPONENT",
                        component,
                        "components/" + component,
                        content,
                        PLATFORM_H5
                ));
            } catch (Exception e) {
                log.warn("生成移动端组件失败: {}", component, e);
            }
        }

        return codes;
    }

    private String generateMobileGrid(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-grid\n");
        sb.append("      :columns=\"").append(props.getOrDefault("columns", 3)).append("\"\n");
        sb.append("      :gap=\"").append(props.getOrDefault("gap", 10)).append("\"\n");
        sb.append("      :border=\"").append(props.getOrDefault("border", true)).append("\"\n");
        sb.append("      :square=\"").append(props.getOrDefault("square", false)).append("\"\n");
        sb.append("    >\n");
        sb.append("      <mobile-grid-item\n");
        sb.append("        v-for=\"(item, index) in gridItems\"\n");
        sb.append("        :key=\"index\"\n");
        sb.append("        :icon=\"item.icon\"\n");
        sb.append("        :text=\"item.text\"\n");
        sb.append("        @click=\"handleGridClick(item, index)\"\n");
        sb.append("      />\n");
        sb.append("    </mobile-grid>\n");
        return sb.toString();
    }

    private String generateMobileCollapse(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-collapse\n");
        sb.append("      v-model=\"activeNames\"\n");
        sb.append("      :accordion=\"").append(props.getOrDefault("accordion", false)).append("\"\n");
        sb.append("    >\n");
        sb.append("      <mobile-collapse-item\n");
        sb.append("        v-for=\"(item, index) in collapseItems\"\n");
        sb.append("        :key=\"item.id\"\n");
        sb.append("        :title=\"item.title\"\n");
        sb.append("        :name=\"item.id\"\n");
        sb.append("      >\n");
        sb.append("        {{ item.content }}\n");
        sb.append("      </mobile-collapse-item>\n");
        sb.append("    </mobile-collapse>\n");
        return sb.toString();
    }

    private String generateMobileTabBar(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-tab-bar\n");
        sb.append("      v-model=\"activeTab\"\n");
        sb.append("      :safe-area-inset-bottom=\"").append(props.getOrDefault("safeAreaInsetBottom", true)).append("\"\n");
        sb.append("      :fixed=\"").append(props.getOrDefault("fixed", true)).append("\"\n");
        sb.append("    >\n");
        sb.append("      <mobile-tab-bar-item\n");
        sb.append("        v-for=\"(item, index) in tabBarItems\"\n");
        sb.append("        :key=\"index\"\n");
        sb.append("        :icon=\"item.icon\"\n");
        sb.append("        :icon-active=\"item.iconActive\"\n");
        sb.append("        @change=\"handleTabChange(index)\"\n");
        sb.append("      >\n");
        sb.append("        {{ item.text }}\n");
        sb.append("      </mobile-tab-bar-item>\n");
        sb.append("    </mobile-tab-bar>\n");
        return sb.toString();
    }

    private String generateMobileSwiper(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-swiper\n");
        sb.append("      :autoplay=\"").append(props.getOrDefault("autoplay", true)).append("\"\n");
        sb.append("      :interval=\"").append(props.getOrDefault("interval", 3000)).append("\"\n");
        sb.append("      :circular=\"").append(props.getOrDefault("circular", true)).append("\"\n");
        sb.append("      :indicator-dots=\"").append(props.getOrDefault("indicatorDots", true)).append("\"\n");
        sb.append("      @change=\"handleSwiperChange\"\n");
        sb.append("    >\n");
        sb.append("      <swiper-item\n");
        sb.append("        v-for=\"(item, index) in swiperItems\"\n");
        sb.append("        :key=\"index\"\n");
        sb.append("      >\n");
        sb.append("        <image :src=\"item.image\" mode=\"aspectFill\" @click=\"handleSwiperClick(item)\" />\n");
        sb.append("      </swiper-item>\n");
        sb.append("    </mobile-swiper>\n");
        return sb.toString();
    }

    private String generateMobileSearchBar(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-search-bar\n");
        sb.append("      v-model=\"searchKeyword\"\n");
        sb.append("      :placeholder=\"'").append(props.getOrDefault("placeholder", "请输入搜索内容")).append("'\"\n");
        sb.append("      :show-action=\"").append(props.getOrDefault("showAction", true)).append("\"\n");
        sb.append("      action-text=\"搜索\"\n");
        sb.append("      @search=\"handleSearch\"\n");
        sb.append("      @cancel=\"handleSearchCancel\"\n");
        sb.append("      @confirm=\"handleSearchConfirm\"\n");
        sb.append("    />\n");
        return sb.toString();
    }

    private String generateMobilePullRefresh(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-pull-refresh\n");
        sb.append("      v-model=\"refreshing\"\n");
        sb.append("      :pull-distance=\"").append(props.getOrDefault("pullDistance", 80)).append("\"\n");
        sb.append("      @refresh=\"handleRefresh\"\n");
        sb.append("    >\n");
        sb.append("      <view class=\"pull-refresh-content\">\n");
        sb.append("        <slot />\n");
        sb.append("      </view>\n");
        sb.append("    </mobile-pull-refresh>\n");
        return sb.toString();
    }

    private String generateMobileSwipeCell(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-swipe-cell\n");
        sb.append("      :left-width=\"").append(props.getOrDefault("leftWidth", 0)).append("\"\n");
        sb.append("      :right-width=\"").append(props.getOrDefault("rightWidth", 150)).append("\"\n");
        sb.append("      :name=\"item.id\"\n");
        sb.append("      @open=\"handleSwipeOpen\"\n");
        sb.append("      @close=\"handleSwipeClose\"\n");
        sb.append("      @delete=\"handleSwipeDelete(item)\"\n");
        sb.append("    >\n");
        sb.append("      <template #left>\n");
        sb.append("        <view class=\"swipe-left\">选择</view>\n");
        sb.append("      </template>\n");
        sb.append("      <view class=\"swipe-content\">\n");
        sb.append("        <slot />\n");
        sb.append("      </view>\n");
        sb.append("      <template #right>\n");
        sb.append("        <view class=\"swipe-right-edit\">编辑</view>\n");
        sb.append("        <view class=\"swipe-right-delete\">删除</view>\n");
        sb.append("      </template>\n");
        sb.append("    </mobile-swipe-cell>\n");
        return sb.toString();
    }

    private String generateMobileWaterfall(String name, Map<String, Object> props, Map<String, Object> style, Map<String, Object> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <mobile-waterfall\n");
        sb.append("      :columns=\"").append(props.getOrDefault("columns", 2)).append("\"\n");
        sb.append("      :gap=\"").append(props.getOrDefault("gap", 10)).append("\"\n");
        sb.append("      :load-more=\"").append(props.getOrDefault("loadMore", true)).append("\"\n");
        sb.append("      @loadmore=\"handleLoadMore\"\n");
        sb.append("    >\n");
        sb.append("      <mobile-waterfall-item\n");
        sb.append("        v-for=\"(item, index) in waterfallItems\"\n");
        sb.append("        :key=\"item.id\"\n");
        sb.append("        :height=\"item.height\"\n");
        sb.append("        @click=\"handleWaterfallItemClick(item)\"\n");
        sb.append("      >\n");
        sb.append("        <image :src=\"item.image\" mode=\"widthFix\" />\n");
        sb.append("        <view class=\"waterfall-item-title\">{{ item.title }}</view>\n");
        sb.append("      </mobile-waterfall-item>\n");
        sb.append("    </mobile-waterfall>\n");
        return sb.toString();
    }

    private String generateDefaultComponent(String name, String type, Map<String, Object> props,
                                            Map<String, Object> style, Map<String, Object> events) {
        String tagName = mapComponentTypeToTag(type);
        StringBuilder sb = new StringBuilder();
        sb.append("    <").append(tagName);

        if (!props.isEmpty()) {
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                String key = StrUtil.toSymbolCase(entry.getKey(), '-');
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append(" ").append(key).append("=\"").append(value).append("\"");
                } else if (value instanceof Boolean) {
                    sb.append(" :").append(key).append("=\"").append(value).append("\"");
                } else if (value instanceof Number) {
                    sb.append(" :").append(key).append("=\"").append(value).append("\"");
                } else {
                    sb.append(" :").append(key).append("='").append(JSON.toJSONString(value)).append("'");
                }
            }
        }

        if (!events.isEmpty()) {
            for (Map.Entry<String, Object> entry : events.entrySet()) {
                String eventName = entry.getKey();
                String handlerName = "handle" + StrUtil.upperFirst(StrUtil.toCamelCase(eventName));
                sb.append(" @").append(StrUtil.toSymbolCase(eventName, '-')).append("=\"").append(handlerName).append("\"");
            }
        }

        sb.append(">\n");
        sb.append("      ").append(props.getOrDefault("placeholder", type)).append("\n");
        sb.append("    </").append(tagName).append(">\n");
        return sb.toString();
    }

    private String mapComponentTypeToTag(String componentType) {
        if (componentType == null) {
            return "view";
        }

        Map<String, String> typeMapping = new HashMap<>();
        typeMapping.put("Input", "input");
        typeMapping.put("Button", "button");
        typeMapping.put("Text", "text");
        typeMapping.put("Image", "image");
        typeMapping.put("ListView", "list");
        typeMapping.put("GridView", "grid");
        typeMapping.put("ScrollView", "scroll-view");
        typeMapping.put("Swiper", "swiper");
        typeMapping.put("Form", "form");
        typeMapping.put("Picker", "picker");
        typeMapping.put("DatePicker", "picker");
        typeMapping.put("TimePicker", "picker");
        typeMapping.put("Switch", "switch");
        typeMapping.put("Checkbox", "checkbox");
        typeMapping.put("Radio", "radio");
        typeMapping.put("Slider", "slider");
        typeMapping.put("Textarea", "textarea");
        typeMapping.put("Icon", "text");
        typeMapping.put("Link", "navigator");
        typeMapping.put("Video", "video");
        typeMapping.put("Audio", "audio");
        typeMapping.put("Map", "map");
        typeMapping.put("Canvas", "canvas");
        typeMapping.put("WebView", "web-view");
        typeMapping.put("MobileGrid", "mobile-grid");
        typeMapping.put("MobileCollapse", "mobile-collapse");
        typeMapping.put("MobileTabBar", "mobile-tab-bar");
        typeMapping.put("MobileSwiper", "mobile-swiper");
        typeMapping.put("MobileSearchBar", "mobile-search-bar");
        typeMapping.put("MobilePullRefresh", "mobile-pull-refresh");
        typeMapping.put("MobileSwipeCell", "mobile-swipe-cell");
        typeMapping.put("MobileWaterfall", "mobile-waterfall");

        return typeMapping.getOrDefault(componentType, StrUtil.toSymbolCase(componentType, '-'));
    }

    private String buildPropsString(Map<String, Object> props) {
        List<String> propList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = StrUtil.toSymbolCase(entry.getKey(), '-');
            Object value = entry.getValue();
            if (value instanceof String) {
                propList.add(key + "=\"" + value + "\"");
            } else if (value instanceof Boolean || value instanceof Number) {
                propList.add(":" + key + "=\"" + value + "\"");
            } else {
                propList.add(":" + key + "='" + JSON.toJSONString(value) + "'");
            }
        }
        return String.join(" ", propList);
    }

    private String buildStyleString(Map<String, Object> style) {
        if (style.isEmpty()) {
            return "";
        }
        return JSON.toJSONString(style);
    }

    private String buildEventsString(Map<String, Object> events) {
        List<String> eventList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : events.entrySet()) {
            String eventName = StrUtil.toSymbolCase(entry.getKey(), '-');
            String handlerName = "handle" + StrUtil.upperFirst(StrUtil.toCamelCase(entry.getKey()));
            eventList.add("@" + eventName + "=\"" + handlerName + "\"");
        }
        return String.join(" ", eventList);
    }

    private boolean isSelfClosingTag(String tagName) {
        return Arrays.asList("input", "image", "slider", "switch", "checkbox", "radio",
                "mobile-search-bar", "mobile-pull-refresh").contains(tagName);
    }

    private List<Page> loadPages(MobileGenerateConfig config) {
        List<Page> pages = new ArrayList<>();
        if (config.getPageIds() != null && !config.getPageIds().isEmpty()) {
            for (Long pageId : config.getPageIds()) {
                try {
                    Page page = pageService.getPageDetail(pageId);
                    if (page != null) {
                        pages.add(page);
                    }
                } catch (Exception e) {
                    log.warn("加载页面失败: pageId={}", pageId, e);
                }
            }
        }
        return pages;
    }

    private List<DataModel> loadDataModels(MobileGenerateConfig config) {
        List<DataModel> models = new ArrayList<>();
        if (config.getDataModelIds() != null && !config.getDataModelIds().isEmpty()) {
            for (Long modelId : config.getDataModelIds()) {
                try {
                    DataModel model = dataModelService.getModelDetail(modelId);
                    if (model != null) {
                        models.add(model);
                    }
                } catch (Exception e) {
                    log.warn("加载数据模型失败: modelId={}", modelId, e);
                }
            }
        }
        return models;
    }

    public GeneratedApp generateUniApp(MobileGenerateConfig config) throws Exception {
        log.info("开始生成 uni-app 完整项目包: appName={}, appCode={}", config.getAppName(), config.getAppCode());

        GeneratedApp app = new GeneratedApp();
        app.setAppName(config.getAppName());
        app.setAppCode(config.getAppCode());
        app.setVersion(config.getVersion() != null ? config.getVersion() : "1.0.0");

        String tempDir = System.getProperty("java.io.tmpdir") + "/lowcode/uniapp/" + config.getAppCode() + "_" + System.currentTimeMillis();
        Files.createDirectories(Paths.get(tempDir));

        List<GeneratedUniAppCode> uniAppCodes = generateUniAppProject(config);

        String srcDir = tempDir + "/src";
        for (GeneratedUniAppCode code : uniAppCodes) {
            writeCodeToFile(srcDir, code);
        }

        String zipPath = tempDir + "/" + config.getAppCode() + "-" + app.getVersion() + ".zip";
        ZipUtil.zip(srcDir, zipPath);

        File zipFile = new File(zipPath);
        app.setFileSize(zipFile.length());
        app.setDownloadUrl("/api/uniapp/download/" + config.getAppCode());

        List<GeneratedCode> frontendCodes = uniAppCodes.stream()
                .map(code -> (GeneratedCode) code)
                .collect(Collectors.toList());
        app.setFrontendCodes(frontendCodes);
        app.setBackendCodes(new ArrayList<>());
        app.setConfigFiles(new ArrayList<>());

        log.info("uni-app 项目包生成完成: appCode={}, fileSize={}", config.getAppCode(), app.getFileSize());
        return app;
    }

    public List<String> getSupportedPlatforms() {
        return Arrays.asList("wechat", "alipay", "h5", "app-plus");
    }

    public byte[] downloadUniApp(String appCode) throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir") + "/lowcode/uniapp/";
        Path dirPath = Paths.get(tempDir);
        if (!Files.exists(dirPath)) {
            throw new Exception("uni-app 项目不存在或已过期");
        }

        File[] dirs = dirPath.toFile().listFiles((dir, name) -> name.startsWith(appCode + "_"));
        if (dirs == null || dirs.length == 0) {
            throw new Exception("uni-app 项目不存在或已过期");
        }

        File latestDir = dirs[0];
        for (File dir : dirs) {
            if (dir.lastModified() > latestDir.lastModified()) {
                latestDir = dir;
            }
        }

        File[] zipFiles = latestDir.listFiles((dir, name) -> name.startsWith(appCode + "-") && name.endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            throw new Exception("uni-app 项目包不存在");
        }

        return Files.readAllBytes(zipFiles[0].toPath());
    }

    private void writeCodeToFile(String baseDir, GeneratedCode code) throws Exception {
        Path filePath = Paths.get(baseDir, code.getFilePath());
        Files.createDirectories(filePath.getParent());
        try (FileWriter writer = new FileWriter(filePath.toFile(), StandardCharsets.UTF_8)) {
            writer.write(code.getCodeContent());
        }
    }
}
