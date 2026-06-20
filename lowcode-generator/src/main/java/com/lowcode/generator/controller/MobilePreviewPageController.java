package com.lowcode.generator.controller;

import com.alibaba.fastjson2.JSON;
import com.lowcode.generator.service.MobilePreviewService;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.PageComponent;
import com.lowcode.page.service.PageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Controller
@RequestMapping("/mobile")
public class MobilePreviewPageController {

    @Autowired
    private MobilePreviewService mobilePreviewService;

    @Autowired(required = false)
    private PageService pageService;

    private static final String THEME_COLOR = "#007AFF";
    private static final String NAV_BAR_HEIGHT = "44px";
    private static final String TAB_BAR_HEIGHT = "50px";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentHashMap<String, List<Map<String, Object>>> mockDataStore = new ConcurrentHashMap<>();
    private final AtomicLong mockIdGenerator = new AtomicLong(1000);

    @GetMapping(value = "/preview/{previewToken}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> previewPage(
            @PathVariable String previewToken,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) Long pageId,
            @RequestParam(required = false, defaultValue = "ios") String platform,
            HttpServletRequest request) {

        log.info("访问H5预览页面 - previewToken: {}, appId: {}, pageId: {}, platform: {}",
                previewToken, appId, pageId, platform);

        try {
            MobilePreviewService.MobilePreview preview = validatePreviewToken(previewToken);
            Long targetPageId = pageId != null ? pageId : preview.getPageId();
            Long targetAppId = appId != null ? appId : preview.getAppId();

            Page pageData = fetchPageData(targetAppId, targetPageId);
            String html = renderMobilePage(preview, pageData, platform, targetPageId, request);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (IllegalArgumentException e) {
            log.warn("预览token无效 - previewToken: {}, error: {}", previewToken, e.getMessage());
            return ResponseEntity.status(HttpStatus.GONE)
                    .contentType(MediaType.TEXT_HTML)
                    .body(renderExpiredPage(previewToken, e.getMessage()));
        } catch (Exception e) {
            log.error("渲染预览页面失败 - previewToken: {}", previewToken, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .body(renderErrorPage(previewToken, e.getMessage()));
        }
    }

    @GetMapping(value = "/preview/{previewToken}/page/{targetPageId}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> previewSpecificPage(
            @PathVariable String previewToken,
            @PathVariable Long targetPageId,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false, defaultValue = "ios") String platform,
            HttpServletRequest request) {

        log.info("访问指定页面预览 - previewToken: {}, targetPageId: {}", previewToken, targetPageId);

        try {
            MobilePreviewService.MobilePreview preview = validatePreviewToken(previewToken);
            Long targetAppId = appId != null ? appId : preview.getAppId();

            Page pageData = fetchPageData(targetAppId, targetPageId);
            String html = renderMobilePage(preview, pageData, platform, targetPageId, request);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .contentType(MediaType.TEXT_HTML)
                    .body(renderExpiredPage(previewToken, e.getMessage()));
        } catch (Exception e) {
            log.error("渲染指定页面失败 - previewToken: {}, pageId: {}", previewToken, targetPageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .body(renderErrorPage(previewToken, e.getMessage()));
        }
    }

    @GetMapping(value = "/preview/{previewToken}/api/mock/**", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> mockApi(
            @PathVariable String previewToken,
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        log.info("Mock API请求 - previewToken: {}, URI: {}", previewToken, request.getRequestURI());

        try {
            validatePreviewToken(previewToken);

            String path = extractMockPath(request, previewToken);
            Map<String, Object> result = handleMockRequest(path, page, pageSize, keyword);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 401);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            log.error("Mock API处理失败", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 500);
            error.put("message", "Mock API错误: " + e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping(value = "/preview/{previewToken}/api/mock/**", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> mockApiPost(
            @PathVariable String previewToken,
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {

        log.info("Mock API POST请求 - previewToken: {}, URI: {}", previewToken, request.getRequestURI());

        try {
            validatePreviewToken(previewToken);

            String path = extractMockPath(request, previewToken);
            Map<String, Object> result = handleMockPostRequest(path, body);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 401);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            log.error("Mock API POST处理失败", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 500);
            error.put("message", "Mock API错误: " + e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private MobilePreviewService.MobilePreview validatePreviewToken(String previewToken) {
        if (!StringUtils.hasText(previewToken)) {
            throw new IllegalArgumentException("预览令牌不能为空");
        }
        return mobilePreviewService.getPreview(previewToken);
    }

    private Page fetchPageData(Long appId, Long pageId) {
        if (pageService != null && pageId != null) {
            try {
                Page page = pageService.getPageDetail(pageId);
                if (page != null) {
                    return page;
                }
            } catch (Exception e) {
                log.warn("通过PageService获取页面数据失败，使用模拟数据 - pageId: {}, error: {}", pageId, e.getMessage());
            }
        }
        return createFallbackPage(appId, pageId);
    }

    private Page createFallbackPage(Long appId, Long pageId) {
        Page page = new Page();
        page.setId(pageId != null ? pageId : 1L);
        page.setAppId(appId != null ? appId : 1L);
        page.setPageName("示例页面");
        page.setPageCode("demo_page");
        page.setPageType("MOBILE");
        page.setLayoutType("MOBILE_LIST");
        page.setPageConfig(buildDefaultPageConfig());
        page.setMobileConfig(buildDefaultMobileConfig());
        page.setComponents(createDemoComponents());
        return page;
    }

    private String buildDefaultPageConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enablePullDownRefresh", true);
        config.put("enableReachBottom", true);
        config.put("backgroundColor", "#f5f5f5");
        return JSON.toJSONString(config);
    }

    private String buildDefaultMobileConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("safeArea", true);
        config.put("orientation", "portrait");
        config.put("statusBarColor", "#ffffff");
        config.put("navigationBarColor", "#ffffff");
        config.put("navigationBarTitleColor", "#000000");
        config.put("themeColor", THEME_COLOR);
        config.put("tabBar", buildDefaultTabBar());
        return JSON.toJSONString(config);
    }

    private Map<String, Object> buildDefaultTabBar() {
        Map<String, Object> tabBar = new LinkedHashMap<>();
        tabBar.put("color", "#7A7E83");
        tabBar.put("selectedColor", THEME_COLOR);
        tabBar.put("backgroundColor", "#ffffff");
        tabBar.put("borderStyle", "black");

        List<Map<String, Object>> list = new ArrayList<>();
        list.add(createTabBarItem("home", "首页", "📱"));
        list.add(createTabBarItem("list", "列表", "📋"));
        list.add(createTabBarItem("profile", "我的", "👤"));
        tabBar.put("list", list);
        return tabBar;
    }

    private Map<String, Object> createTabBarItem(String key, String text, String icon) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("text", text);
        item.put("icon", icon);
        return item;
    }

    private List<PageComponent> createDemoComponents() {
        List<PageComponent> components = new ArrayList<>();
        int sortOrder = 1;

        components.add(createComponent("searchBar1", "MobileSearchBar", "搜索",
                JSON.toJSONString(mapOf("placeholder", "搜索内容...", "showCancel", true)),
                null, sortOrder++));

        components.add(createComponent("swiper1", "MobileSwiper", "轮播图",
                JSON.toJSONString(mapOf(
                        "autoplay", true,
                        "interval", 3000,
                        "circular", true,
                        "items", listOf(
                                mapOf("image", "https://picsum.photos/seed/banner1/400/200", "title", "欢迎使用低代码平台"),
                                mapOf("image", "https://picsum.photos/seed/banner2/400/200", "title", "快速构建移动应用"),
                                mapOf("image", "https://picsum.photos/seed/banner3/400/200", "title", "可视化拖拽设计")
                        )
                )), null, sortOrder++));

        components.add(createComponent("grid1", "MobileGrid", "功能入口",
                JSON.toJSONString(mapOf(
                        "columns", 4,
                        "items", listOf(
                                mapOf("icon", "📋", "text", "订单管理", "badge", "5"),
                                mapOf("icon", "💰", "text", "财务中心", "badge", ""),
                                mapOf("icon", "📦", "text", "商品管理", "badge", "12"),
                                mapOf("icon", "👥", "text", "客户管理", "badge", ""),
                                mapOf("icon", "📊", "text", "数据报表", "badge", ""),
                                mapOf("icon", "⚙️", "text", "系统设置", "badge", ""),
                                mapOf("icon", "📝", "text", "工作流", "badge", "3"),
                                mapOf("icon", "🔔", "text", "消息通知", "badge", "99+")
                        )
                )), null, sortOrder++));

        components.add(createComponent("collapse1", "MobileCollapse", "快捷操作",
                JSON.toJSONString(mapOf(
                        "title", "展开更多操作",
                        "content", "这里可以放置更多的快捷操作入口，帮助用户快速完成常见任务。支持自定义配置每个操作的图标、文字和链接地址。"
                )), null, sortOrder++));

        components.add(createComponent("listTitle1", "Text", "最新动态",
                JSON.toJSONString(mapOf("text", "最新动态", "level", "h3")),
                JSON.toJSONString(mapOf("padding", "16px 16px 8px 16px", "fontWeight", "bold", "fontSize", "16px")),
                sortOrder++));

        components.add(createComponent("listContainer1", "Container", "列表容器",
                JSON.toJSONString(mapOf("dataSource", "mock://api/mock/list")),
                JSON.toJSONString(mapOf("backgroundColor", "#ffffff", "borderRadius", "8px", "margin", "0 12px 12px 12px")),
                sortOrder++));

        components.add(createComponent("input1", "Input", "用户名",
                JSON.toJSONString(mapOf("placeholder", "请输入用户名", "type", "text", "clearable", true)),
                JSON.toJSONString(mapOf("margin", "0 12px")),
                sortOrder++));

        components.add(createComponent("button1", "Button", "提交",
                JSON.toJSONString(mapOf("type", "primary", "block", true, "loading", false)),
                JSON.toJSONString(mapOf("margin", "16px 12px")),
                sortOrder++));

        return components;
    }

    private PageComponent createComponent(String id, String type, String name, String props, String style, int sortOrder) {
        PageComponent component = new PageComponent();
        component.setComponentId(id);
        component.setComponentType(type);
        component.setComponentName(name);
        component.setPropsConfig(props);
        component.setStyleConfig(style);
        component.setSortOrder(sortOrder);
        component.setParentId(null);
        return component;
    }

    private String renderMobilePage(MobilePreviewService.MobilePreview preview, Page page, String platform, Long currentPageId, HttpServletRequest request) {
        String pageName = page.getPageName() != null ? page.getPageName() : "预览页面";
        String appName = "低代码应用";
        Map<String, Object> mobileConfig = parseMobileConfig(page.getMobileConfig());
        Map<String, Object> pageConfig = parsePageConfig(page.getPageConfig());
        String themeColor = getConfigValue(mobileConfig, "themeColor", THEME_COLOR);
        boolean enablePullDownRefresh = getConfigValue(pageConfig, "enablePullDownRefresh", true);

        String contextPath = request.getContextPath();
        String mockApiBase = contextPath + "/mobile/preview/" + preview.getPreviewToken() + "/api/mock";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover\">\n");
        html.append("  <meta name=\"apple-mobile-web-app-capable\" content=\"yes\">\n");
        html.append("  <meta name=\"apple-mobile-web-app-status-bar-style\" content=\"default\">\n");
        html.append("  <meta name=\"format-detection\" content=\"telephone=no\">\n");
        html.append("  <title>").append(escapeHtml(pageName)).append("</title>\n");
        html.append("  <style>\n");
        html.append(renderGlobalStyles(themeColor, enablePullDownRefresh));
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body class=\"platform-").append(platform).append("\">\n");

        html.append(renderPullToRefreshIndicator(themeColor));

        html.append("  <div class=\"app-container\" id=\"appContainer\">\n");
        html.append("    <div class=\"status-bar\" id=\"statusBar\" style=\"background: ").append(themeColor).append(";\">\n");
        html.append("      <div class=\"status-bar-content\">\n");
        html.append("        <span class=\"status-time\" id=\"statusTime\">9:41</span>\n");
        html.append("        <span class=\"status-icons\">\n");
        html.append("          <span class=\"signal\">●●●●●</span>\n");
        html.append("          <span class=\"wifi\">📶</span>\n");
        html.append("          <span class=\"battery\">🔋 100%</span>\n");
        html.append("        </span>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        html.append("    <div class=\"nav-bar\" style=\"background: #ffffff; border-bottom: 1px solid #ebebeb;\">\n");
        html.append("      <div class=\"nav-bar-left\" onclick=\"goBack()\">\n");
        html.append("        <span class=\"nav-back-icon\">←</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"nav-bar-title\">").append(escapeHtml(pageName)).append("</div>\n");
        html.append("      <div class=\"nav-bar-right\" onclick=\"showMenu()\">\n");
        html.append("        <span class=\"nav-menu-icon\">⋯</span>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        html.append("    <div class=\"page-content\" id=\"pageContent\">\n");
        html.append(renderComponentTree(page.getComponents(), preview.getPreviewToken()));
        html.append("    </div>\n");

        html.append(renderTabBar(mobileConfig, currentPageId));

        html.append("  </div>\n");

        html.append("  <div class=\"toast\" id=\"toast\"></div>\n");

        html.append("  <script>\n");
        html.append(renderInlineJavaScript(preview.getPreviewToken(), mockApiBase, enablePullDownRefresh));
        html.append("  </script>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String renderGlobalStyles(String themeColor, boolean enablePullDownRefresh) {
        StringBuilder css = new StringBuilder();
        css.append("    :root {\n");
        css.append("      --theme-color: ").append(themeColor).append(";\n");
        css.append("      --theme-color-light: ").append(lightenColor(themeColor, 0.1)).append(";\n");
        css.append("      --theme-color-dark: ").append(darkenColor(themeColor, 0.1)).append(";\n");
        css.append("      --nav-bar-height: ").append(NAV_BAR_HEIGHT).append(";\n");
        css.append("      --tab-bar-height: ").append(TAB_BAR_HEIGHT).append(";\n");
        css.append("      --status-bar-height: env(safe-area-inset-top, 20px);\n");
        css.append("      --safe-area-bottom: env(safe-area-inset-bottom, 0px);\n");
        css.append("    }\n");
        css.append("    * { margin: 0; padding: 0; box-sizing: border-box; -webkit-tap-highlight-color: transparent; }\n");
        css.append("    html, body { width: 100%; height: 100%; overflow: hidden; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif; font-size: 14px; color: #333; background: #f5f5f5; -webkit-font-smoothing: antialiased; }\n");
        css.append("    body.platform-ios { font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, Arial, sans-serif; }\n");
        css.append("    body.platform-android { font-family: 'Roboto', 'Noto Sans SC', 'Microsoft YaHei', sans-serif; }\n");
        css.append("    .app-container { display: flex; flex-direction: column; height: 100vh; width: 100%; background: #f5f5f5; position: relative; }\n");
        css.append("    .status-bar { height: var(--status-bar-height); min-height: 20px; display: flex; align-items: center; justify-content: center; transition: all 0.3s ease; position: relative; z-index: 100; }\n");
        css.append("    .status-bar-content { display: flex; justify-content: space-between; align-items: center; width: 100%; padding: 0 20px; font-size: 12px; font-weight: 600; color: #000; }\n");
        css.append("    .status-icons { display: flex; gap: 6px; align-items: center; font-size: 11px; }\n");
        css.append("    .status-icons .signal { font-size: 10px; letter-spacing: -1px; }\n");
        css.append("    .nav-bar { height: var(--nav-bar-height); display: flex; align-items: center; justify-content: space-between; padding: 0 12px; position: relative; z-index: 99; flex-shrink: 0; }\n");
        css.append("    .nav-bar-left, .nav-bar-right { width: 60px; height: 44px; display: flex; align-items: center; justify-content: center; cursor: pointer; }\n");
        css.append("    .nav-bar-left:active, .nav-bar-right:active { opacity: 0.6; }\n");
        css.append("    .nav-back-icon { font-size: 24px; color: #333; }\n");
        css.append("    .nav-menu-icon { font-size: 20px; color: #333; font-weight: bold; }\n");
        css.append("    .nav-bar-title { flex: 1; text-align: center; font-size: 17px; font-weight: 600; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n");
        css.append("    .page-content { flex: 1; overflow-y: auto; overflow-x: hidden; -webkit-overflow-scrolling: touch; position: relative; padding-bottom: calc(var(--tab-bar-height) + var(--safe-area-bottom)); }\n");
        css.append("    .page-content::-webkit-scrollbar { width: 0; display: none; }\n");
        css.append("    .tab-bar { height: calc(var(--tab-bar-height) + var(--safe-area-bottom)); padding-bottom: var(--safe-area-bottom); background: #ffffff; border-top: 1px solid #ebebeb; display: flex; position: fixed; bottom: 0; left: 0; right: 0; z-index: 100; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); }\n");
        css.append("    .tab-bar-item { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; padding: 6px 0; transition: all 0.2s ease; }\n");
        css.append("    .tab-bar-item:active { transform: scale(0.95); }\n");
        css.append("    .tab-bar-icon { font-size: 22px; line-height: 1; margin-bottom: 2px; opacity: 0.7; transition: all 0.2s ease; }\n");
        css.append("    .tab-bar-item.active .tab-bar-icon { opacity: 1; transform: scale(1.1); }\n");
        css.append("    .tab-bar-label { font-size: 10px; color: #7A7E83; transition: color 0.2s ease; }\n");
        css.append("    .tab-bar-item.active .tab-bar-label { color: var(--theme-color); font-weight: 500; }\n");
        css.append("    .component-wrapper { margin: 12px; }\n");
        css.append("    .card { background: #ffffff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin: 12px; }\n");
        css.append("    .card-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; color: #333; }\n");

        css.append(renderSearchBarStyles());
        css.append(renderSwiperStyles());
        css.append(renderGridStyles());
        css.append(renderCollapseStyles());
        css.append(renderButtonStyles());
        css.append(renderInputStyles());
        css.append(renderListStyles());
        css.append(renderToastStyles());

        if (enablePullDownRefresh) {
            css.append("    .pull-refresh-indicator { position: absolute; top: 0; left: 0; right: 0; height: 60px; display: flex; align-items: center; justify-content: center; transform: translateY(-60px); transition: transform 0.3s ease; z-index: 50; pointer-events: none; }\n");
            css.append("    .pull-refresh-content { display: flex; align-items: center; gap: 8px; color: #999; font-size: 13px; }\n");
            css.append("    .pull-refresh-spinner { width: 18px; height: 18px; border: 2px solid #ddd; border-top-color: var(--theme-color); border-radius: 50%; animation: spin 0.8s linear infinite; }\n");
            css.append("    @keyframes spin { to { transform: rotate(360deg); } }\n");
            css.append("    .refreshing .pull-refresh-indicator { transform: translateY(0); }\n");
        }

        return css.toString();
    }

    private String renderSearchBarStyles() {
        return "    .mobile-search-bar { padding: 12px; background: #ffffff; border-bottom: 1px solid #f0f0f0; }\n" +
                "    .search-bar-inner { display: flex; align-items: center; background: #f5f5f5; border-radius: 20px; padding: 8px 14px; transition: all 0.2s ease; }\n" +
                "    .search-bar-inner:focus-within { background: #eef3ff; box-shadow: 0 0 0 2px var(--theme-color-light); }\n" +
                "    .search-icon { margin-right: 8px; color: #999; font-size: 14px; }\n" +
                "    .search-input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: #333; height: 20px; }\n" +
                "    .search-input::placeholder { color: #bfbfbf; }\n" +
                "    .search-cancel { margin-left: 10px; padding: 4px 8px; color: var(--theme-color); font-size: 14px; cursor: pointer; display: none; }\n" +
                "    .search-bar-inner.has-value .search-cancel { display: block; }\n";
    }

    private String renderSwiperStyles() {
        return "    .mobile-swiper { position: relative; width: 100%; overflow: hidden; border-radius: 12px; margin: 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.08); }\n" +
                "    .mobile-swiper-container { position: relative; height: 180px; }\n" +
                "    .swiper-slide { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0; transition: opacity 0.5s ease; background-size: cover; background-position: center; display: flex; align-items: flex-end; }\n" +
                "    .swiper-slide.active { opacity: 1; z-index: 1; }\n" +
                "    .swiper-slide-overlay { width: 100%; padding: 16px; background: linear-gradient(transparent, rgba(0,0,0,0.6)); color: #ffffff; font-size: 14px; font-weight: 500; }\n" +
                "    .swiper-dots { position: absolute; bottom: 10px; left: 50%; transform: translateX(-50%); display: flex; gap: 6px; z-index: 10; }\n" +
                "    .swiper-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.5); transition: all 0.3s ease; }\n" +
                "    .swiper-dot.active { width: 18px; border-radius: 3px; background: #ffffff; }\n";
    }

    private String renderGridStyles() {
        return "    .mobile-grid { background: #ffffff; border-radius: 12px; padding: 16px 8px; margin: 12px; display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }\n" +
                "    .grid-item { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 12px 4px; cursor: pointer; transition: all 0.2s ease; position: relative; border-radius: 8px; }\n" +
                "    .grid-item:active { background: #f5f5f5; transform: scale(0.95); }\n" +
                "    .grid-icon { width: 44px; height: 44px; border-radius: 12px; background: linear-gradient(135deg, var(--theme-color-light), var(--theme-color)); display: flex; align-items: center; justify-content: center; font-size: 22px; margin-bottom: 8px; box-shadow: 0 4px 8px rgba(0,122,255,0.2); }\n" +
                "    .grid-text { font-size: 12px; color: #333; text-align: center; line-height: 1.3; }\n" +
                "    .grid-badge { position: absolute; top: 6px; right: 8px; min-width: 16px; height: 16px; padding: 0 4px; background: #ff4d4f; color: #ffffff; font-size: 10px; font-weight: 500; border-radius: 8px; display: flex; align-items: center; justify-content: center; line-height: 1; }\n";
    }

    private String renderCollapseStyles() {
        return "    .mobile-collapse { background: #ffffff; border-radius: 12px; margin: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }\n" +
                "    .collapse-header { display: flex; align-items: center; justify-content: space-between; padding: 16px; cursor: pointer; transition: background 0.2s ease; user-select: none; }\n" +
                "    .collapse-header:active { background: #fafafa; }\n" +
                "    .collapse-title { font-size: 15px; font-weight: 500; color: #333; }\n" +
                "    .collapse-arrow { transition: transform 0.3s ease; color: #999; font-size: 12px; }\n" +
                "    .mobile-collapse.expanded .collapse-arrow { transform: rotate(180deg); }\n" +
                "    .collapse-content { max-height: 0; overflow: hidden; transition: max-height 0.3s ease, padding 0.3s ease; padding: 0 16px; line-height: 1.6; color: #666; font-size: 13px; }\n" +
                "    .mobile-collapse.expanded .collapse-content { max-height: 500px; padding: 0 16px 16px 16px; }\n";
    }

    private String renderButtonStyles() {
        return "    .mobile-btn { display: inline-flex; align-items: center; justify-content: center; padding: 10px 20px; border-radius: 24px; font-size: 14px; font-weight: 500; border: none; cursor: pointer; transition: all 0.2s ease; user-select: none; outline: none; min-height: 40px; }\n" +
                "    .mobile-btn:active { transform: scale(0.97); opacity: 0.85; }\n" +
                "    .mobile-btn.primary { background: linear-gradient(135deg, var(--theme-color), var(--theme-color-dark)); color: #ffffff; box-shadow: 0 4px 12px rgba(0,122,255,0.3); }\n" +
                "    .mobile-btn.primary:active { box-shadow: 0 2px 6px rgba(0,122,255,0.25); }\n" +
                "    .mobile-btn.default { background: #f5f5f5; color: #333; }\n" +
                "    .mobile-btn.default:active { background: #e8e8e8; }\n" +
                "    .mobile-btn.block { display: flex; width: 100%; margin: 16px 12px; }\n" +
                "    .mobile-btn:disabled { opacity: 0.5; cursor: not-allowed; }\n" +
                "    .btn-spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #ffffff; border-radius: 50%; animation: spin 0.6s linear infinite; margin-right: 6px; }\n";
    }

    private String renderInputStyles() {
        return "    .mobile-input-wrapper { padding: 12px; background: #ffffff; border-radius: 10px; margin: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }\n" +
                "    .mobile-input-label { font-size: 13px; color: #666; margin-bottom: 8px; display: block; }\n" +
                "    .mobile-input-field { display: flex; align-items: center; border: 1px solid #e8e8e8; border-radius: 8px; padding: 0 12px; height: 40px; transition: all 0.2s ease; }\n" +
                "    .mobile-input-field:focus-within { border-color: var(--theme-color); box-shadow: 0 0 0 2px var(--theme-color-light); }\n" +
                "    .mobile-input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: #333; height: 100%; }\n" +
                "    .mobile-input::placeholder { color: #bfbfbf; }\n" +
                "    .input-clear-btn { width: 18px; height: 18px; border-radius: 50%; background: #d9d9d9; color: #ffffff; display: none; align-items: center; justify-content: center; font-size: 12px; cursor: pointer; margin-left: 8px; flex-shrink: 0; }\n" +
                "    .mobile-input-field.has-value .input-clear-btn { display: flex; }\n";
    }

    private String renderListStyles() {
        return "    .mobile-list { background: #ffffff; border-radius: 12px; margin: 12px; overflow: hidden; }\n" +
                "    .list-item { display: flex; align-items: center; padding: 14px 16px; border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.2s ease; position: relative; }\n" +
                "    .list-item:last-child { border-bottom: none; }\n" +
                "    .list-item:active { background: #fafafa; }\n" +
                "    .list-item-avatar { width: 48px; height: 48px; border-radius: 10px; background: linear-gradient(135deg, #e3f2fd, #bbdefb); display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 12px; flex-shrink: 0; }\n" +
                "    .list-item-content { flex: 1; min-width: 0; }\n" +
                "    .list-item-title { font-size: 15px; font-weight: 500; color: #333; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n" +
                "    .list-item-desc { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n" +
                "    .list-item-right { display: flex; flex-direction: column; align-items: flex-end; margin-left: 8px; }\n" +
                "    .list-item-time { font-size: 11px; color: #bfbfbf; margin-bottom: 4px; }\n" +
                "    .list-item-arrow { color: #d9d9d9; font-size: 14px; }\n" +
                "    .list-item-tag { padding: 2px 8px; border-radius: 4px; font-size: 10px; font-weight: 500; margin-bottom: 4px; }\n" +
                "    .tag-success { background: #f6ffed; color: #52c41a; }\n" +
                "    .tag-warning { background: #fffbe6; color: #faad14; }\n" +
                "    .tag-error { background: #fff2f0; color: #ff4d4f; }\n" +
                "    .tag-info { background: #e6f7ff; color: #1890ff; }\n" +
                "    .section-title { padding: 16px 16px 8px 16px; font-size: 15px; font-weight: 600; color: #333; }\n" +
                "    .empty-state { padding: 60px 20px; text-align: center; color: #999; }\n" +
                "    .empty-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.5; }\n" +
                "    .empty-text { font-size: 14px; }\n";
    }

    private String renderToastStyles() {
        return "    .toast { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.9); padding: 12px 20px; background: rgba(0,0,0,0.75); color: #ffffff; border-radius: 8px; font-size: 14px; z-index: 9999; opacity: 0; transition: all 0.3s ease; pointer-events: none; max-width: 80%; text-align: center; }\n" +
                "    .toast.show { opacity: 1; transform: translate(-50%, -50%) scale(1); }\n" +
                "    .touch-feedback { position: fixed; border-radius: 50%; background: rgba(0,122,255,0.3); transform: scale(0); animation: touchRipple 0.6s ease-out; pointer-events: none; z-index: 9998; }\n" +
                "    @keyframes touchRipple { to { transform: scale(4); opacity: 0; } }\n";
    }

    private String renderPullToRefreshIndicator(String themeColor) {
        return "    <div class=\"pull-refresh-indicator\" id=\"pullRefreshIndicator\">\n" +
                "      <div class=\"pull-refresh-content\">\n" +
                "        <div class=\"pull-refresh-spinner\" id=\"pullRefreshSpinner\"></div>\n" +
                "        <span id=\"pullRefreshText\">下拉刷新</span>\n" +
                "      </div>\n" +
                "    </div>\n";
    }

    private String renderTabBar(Map<String, Object> mobileConfig, Long currentPageId) {
        Object tabBarObj = mobileConfig.get("tabBar");
        if (tabBarObj == null) {
            return "";
        }

        Map<String, Object> tabBarConfig;
        if (tabBarObj instanceof Map) {
            tabBarConfig = (Map<String, Object>) tabBarObj;
        } else {
            return "";
        }

        Object listObj = tabBarConfig.get("list");
        if (!(listObj instanceof List)) {
            return "";
        }

        List<?> items = (List<?>) listObj;
        StringBuilder html = new StringBuilder();
        html.append("    <div class=\"tab-bar\">\n");

        for (int i = 0; i < items.size(); i++) {
            Object itemObj = items.get(i);
            if (!(itemObj instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) itemObj;

            String text = String.valueOf(item.getOrDefault("text", "Tab" + (i + 1)));
            String icon = String.valueOf(item.getOrDefault("icon", "📄"));
            boolean isActive = i == 0;

            html.append("      <div class=\"tab-bar-item").append(isActive ? " active" : "").append("\" onclick=\"switchTab(").append(i).append(", '").append(text).append("')\">\n");
            html.append("        <div class=\"tab-bar-icon\">").append(icon).append("</div>\n");
            html.append("        <div class=\"tab-bar-label\">").append(escapeHtml(text)).append("</div>\n");
            html.append("      </div>\n");
        }

        html.append("    </div>\n");
        return html.toString();
    }

    private String renderComponentTree(List<PageComponent> components, String previewToken) {
        StringBuilder html = new StringBuilder();

        if (components == null || components.isEmpty()) {
            html.append(renderListItems(previewToken));
            return html.toString();
        }

        List<PageComponent> sortedComponents = components.stream()
                .sorted(Comparator.comparing(c -> c.getSortOrder() != null ? c.getSortOrder() : 0))
                .collect(java.util.stream.Collectors.toList());

        for (PageComponent component : sortedComponents) {
            if (component.getParentId() == null || component.getParentId().isEmpty()) {
                html.append(renderComponent(component, sortedComponents, previewToken));
            }
        }

        if (!hasListComponent(components)) {
            html.append(renderListItems(previewToken));
        }

        return html.toString();
    }

    private boolean hasListComponent(List<PageComponent> components) {
        return components.stream()
                .anyMatch(c -> "List".equalsIgnoreCase(c.getComponentType()) || "MobileList".equalsIgnoreCase(c.getComponentType()));
    }

    private String renderComponent(PageComponent component, List<PageComponent> allComponents, String previewToken) {
        String type = component.getComponentType();
        if (type == null) return "";

        Map<String, Object> props = parseJson(component.getPropsConfig());
        Map<String, Object> style = parseJson(component.getStyleConfig());
        String customStyle = buildStyleString(style);

        switch (type) {
            case "MobileSearchBar":
            case "SearchBar":
                return renderMobileSearchBar(props, customStyle);
            case "MobileSwiper":
            case "Swiper":
                return renderMobileSwiper(props, customStyle);
            case "MobileGrid":
            case "Grid":
                return renderMobileGrid(props, customStyle);
            case "MobileCollapse":
            case "Collapse":
                return renderMobileCollapse(props, customStyle);
            case "MobileTabBar":
                return "";
            case "Input":
            case "MobileInput":
                return renderMobileInput(component, props, customStyle);
            case "Button":
            case "MobileButton":
                return renderMobileButton(component, props, customStyle);
            case "Text":
            case "Label":
                return renderText(component, props, customStyle);
            case "Container":
            case "View":
                return renderContainer(component, props, customStyle);
            case "Card":
                return renderCard(component, props, customStyle, allComponents, previewToken);
            case "List":
            case "MobileList":
                return renderListItems(previewToken);
            default:
                return renderUnknownComponent(component);
        }
    }

    private String renderMobileSearchBar(Map<String, Object> props, String customStyle) {
        String placeholder = getConfigValue(props, "placeholder", "搜索...");
        boolean showCancel = getConfigValue(props, "showCancel", false);

        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"mobile-search-bar\"");
        if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
        html.append(">\n");
        html.append("    <div class=\"search-bar-inner\" id=\"searchBarInner\">\n");
        html.append("      <span class=\"search-icon\">🔍</span>\n");
        html.append("      <input type=\"text\" class=\"search-input\" id=\"searchInput\" placeholder=\"").append(escapeHtml(placeholder)).append("\" oninput=\"handleSearchInput(this)\" onkeypress=\"if(event.key==='Enter')handleSearch()\">\n");
        if (showCancel) {
            html.append("      <span class=\"search-cancel\" onclick=\"clearSearch()\">取消</span>\n");
        }
        html.append("    </div>\n");
        html.append("  </div>\n");
        return html.toString();
    }

    private String renderMobileSwiper(Map<String, Object> props, String customStyle) {
        boolean autoplay = getConfigValue(props, "autoplay", true);
        List<?> items = getConfigValue(props, "items", Collections.emptyList());

        if (items.isEmpty()) {
            items = listOf(
                    mapOf("image", "https://picsum.photos/seed/sw1/400/200", "title", "智能低代码平台"),
                    mapOf("image", "https://picsum.photos/seed/sw2/400/200", "title", "一键生成移动应用"),
                    mapOf("image", "https://picsum.photos/seed/sw3/400/200", "title", "所见即所得设计")
            );
        }

        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"mobile-swiper\" id=\"mobileSwiper\"");
        if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
        html.append(">\n");
        html.append("    <div class=\"mobile-swiper-container\">\n");

        for (int i = 0; i < items.size(); i++) {
            Object itemObj = items.get(i);
            if (!(itemObj instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) itemObj;

            String image = String.valueOf(item.getOrDefault("image", "https://picsum.photos/400/200"));
            String title = String.valueOf(item.getOrDefault("title", ""));
            boolean isFirst = i == 0;

            html.append("      <div class=\"swiper-slide").append(isFirst ? " active" : "").append("\" style=\"background-image: url('").append(escapeHtml(image)).append("')\">\n");
            if (!title.isEmpty()) {
                html.append("        <div class=\"swiper-slide-overlay\">").append(escapeHtml(title)).append("</div>\n");
            }
            html.append("      </div>\n");
        }

        html.append("    </div>\n");
        html.append("    <div class=\"swiper-dots\">\n");
        for (int i = 0; i < items.size(); i++) {
            html.append("      <div class=\"swiper-dot").append(i == 0 ? " active" : "").append("\" data-index=\"").append(i).append("\"></div>\n");
        }
        html.append("    </div>\n");
        html.append("  </div>\n");
        return html.toString();
    }

    private String renderMobileGrid(Map<String, Object> props, String customStyle) {
        int columns = getConfigValue(props, "columns", 4);
        List<?> items = getConfigValue(props, "items", Collections.emptyList());

        if (items.isEmpty()) {
            items = listOf(
                    mapOf("icon", "📋", "text", "订单管理"),
                    mapOf("icon", "💰", "text", "财务中心"),
                    mapOf("icon", "📦", "text", "商品管理"),
                    mapOf("icon", "👥", "text", "客户管理")
            );
        }

        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"mobile-grid\" style=\"grid-template-columns: repeat(").append(columns).append(", 1fr);");
        if (!customStyle.isEmpty()) html.append(customStyle);
        html.append("\">\n");

        for (int i = 0; i < items.size(); i++) {
            Object itemObj = items.get(i);
            if (!(itemObj instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) itemObj;

            String icon = String.valueOf(item.getOrDefault("icon", "📄"));
            String text = String.valueOf(item.getOrDefault("text", "功能" + (i + 1)));
            String badge = String.valueOf(item.getOrDefault("badge", ""));

            html.append("    <div class=\"grid-item\" onclick=\"handleGridItemClick(").append(i).append(", '").append(escapeHtml(text)).append("')\">\n");
            html.append("      <div class=\"grid-icon\">").append(icon).append("</div>\n");
            html.append("      <div class=\"grid-text\">").append(escapeHtml(text)).append("</div>\n");
            if (!badge.isEmpty() && !"null".equals(badge)) {
                html.append("      <div class=\"grid-badge\">").append(escapeHtml(badge)).append("</div>\n");
            }
            html.append("    </div>\n");
        }

        html.append("  </div>\n");
        return html.toString();
    }

    private String renderMobileCollapse(Map<String, Object> props, String customStyle) {
        String title = getConfigValue(props, "title", "展开/收起");
        String content = getConfigValue(props, "content", "这里是折叠面板的内容区域，可以放置更多信息。");

        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"mobile-collapse\" id=\"mobileCollapse\"");
        if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
        html.append(" onclick=\"toggleCollapse()\">\n");
        html.append("    <div class=\"collapse-header\">\n");
        html.append("      <span class=\"collapse-title\">").append(escapeHtml(title)).append("</span>\n");
        html.append("      <span class=\"collapse-arrow\">▼</span>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"collapse-content\">\n");
        html.append("      <p>").append(escapeHtml(content)).append("</p>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        return html.toString();
    }

    private String renderMobileInput(PageComponent component, Map<String, Object> props, String customStyle) {
        String label = component.getComponentName() != null ? component.getComponentName() : "";
        String placeholder = getConfigValue(props, "placeholder", "请输入...");
        String inputType = getConfigValue(props, "type", "text");
        boolean clearable = getConfigValue(props, "clearable", true);

        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"mobile-input-wrapper\"");
        if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
        html.append(">\n");
        if (!label.isEmpty()) {
            html.append("    <label class=\"mobile-input-label\">").append(escapeHtml(label)).append("</label>\n");
        }
        html.append("    <div class=\"mobile-input-field\" id=\"inputField_").append(component.getComponentId()).append("\">\n");
        html.append("      <input type=\"").append(escapeHtml(inputType)).append("\" class=\"mobile-input\" placeholder=\"").append(escapeHtml(placeholder)).append("\" oninput=\"handleInputChange(this, '").append(component.getComponentId()).append("')\">\n");
        if (clearable) {
            html.append("      <span class=\"input-clear-btn\" onclick=\"clearInput('").append(component.getComponentId()).append("')\">×</span>\n");
        }
        html.append("    </div>\n");
        html.append("  </div>\n");
        return html.toString();
    }

    private String renderMobileButton(PageComponent component, Map<String, Object> props, String customStyle) {
        String label = component.getComponentName() != null ? component.getComponentName() : "按钮";
        String btnType = getConfigValue(props, "type", "primary");
        boolean block = getConfigValue(props, "block", false);

        StringBuilder html = new StringBuilder();
        html.append("  <button class=\"mobile-btn ").append(btnType).append(block ? " block" : "").append("\"");
        if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
        html.append(" onclick=\"handleButtonClick('").append(component.getComponentId()).append("', '").append(escapeHtml(label)).append("')\">\n");
        html.append("    <span>").append(escapeHtml(label)).append("</span>\n");
        html.append("  </button>\n");
        return html.toString();
    }

    private String renderText(PageComponent component, Map<String, Object> props, String customStyle) {
        String text = getConfigValue(props, "text", component.getComponentName() != null ? component.getComponentName() : "");
        String level = getConfigValue(props, "level", "");

        StringBuilder html = new StringBuilder();
        if (!level.isEmpty()) {
            html.append("  <div class=\"section-title\"");
            if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
            html.append(">").append(escapeHtml(text)).append("</div>\n");
        } else {
            html.append("  <div class=\"component-wrapper\"");
            if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
            html.append(">").append(escapeHtml(text)).append("</div>\n");
        }
        return html.toString();
    }

    private String renderContainer(PageComponent component, Map<String, Object> props, String customStyle) {
        String dataSource = getConfigValue(props, "dataSource", "");

        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"component-wrapper\" id=\"container_").append(component.getComponentId()).append("\"");
        if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
        html.append(">\n");

        if (!dataSource.isEmpty() && dataSource.contains("mock")) {
            html.append("    <div id=\"listContainer_").append(component.getComponentId()).append("\"></div>\n");
            html.append("    <script>loadListData('listContainer_").append(component.getComponentId()).append("');</script>\n");
        }

        html.append("  </div>\n");
        return html.toString();
    }

    private String renderCard(PageComponent component, Map<String, Object> props, String customStyle, List<PageComponent> allComponents, String previewToken) {
        String title = component.getComponentName() != null ? component.getComponentName() : "";

        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"card\"");
        if (!customStyle.isEmpty()) html.append(" style=\"").append(customStyle).append("\"");
        html.append(">\n");
        if (!title.isEmpty()) {
            html.append("    <div class=\"card-title\">").append(escapeHtml(title)).append("</div>\n");
        }
        html.append("  </div>\n");
        return html.toString();
    }

    private String renderListItems(String previewToken) {
        StringBuilder html = new StringBuilder();
        html.append("  <div class=\"section-title\">最新动态</div>\n");
        html.append("  <div class=\"mobile-list\" id=\"mockList\">\n");
        html.append("    <div id=\"listLoading\">加载中...</div>\n");
        html.append("  </div>\n");
        return html.toString();
    }

    private String renderUnknownComponent(PageComponent component) {
        return "  <div class=\"component-wrapper\" style=\"padding: 16px; background: #f0f0f0; border-radius: 8px; margin: 12px; text-align: center; color: #999; font-size: 12px;\">".append("未知组件: ").append(escapeHtml(component.getComponentType())).append("</div>\n").toString();
    }

    private String renderInlineJavaScript(String previewToken, String mockApiBase, boolean enablePullDownRefresh) {
        StringBuilder js = new StringBuilder();

        js.append("    const MOCK_API_BASE = '").append(mockApiBase).append("';\n");
        js.append("    const PREVIEW_TOKEN = '").append(previewToken).append("';\n\n");

        js.append(renderStatusBarTimeScript());
        js.append(renderNavigationScript());
        js.append(renderSearchScript());
        js.append(renderSwiperScript());
        js.append(renderCollapseScript());
        js.append(renderInputScript());
        js.append(renderButtonScript());
        js.append(renderGridItemScript());
        js.append(renderTabBarScript());
        js.append(renderToastScript());
        js.append(renderTouchFeedbackScript());

        if (enablePullDownRefresh) {
            js.append(renderPullToRefreshScript());
        }

        js.append(renderListLoaderScript());
        js.append(renderTouchEventsScript());

        js.append("    document.addEventListener('DOMContentLoaded', function() {\n");
        js.append("      updateStatusTime();\n");
        js.append("      loadListData('mockList');\n");
        js.append("      console.log('[H5预览] 页面加载完成 - Token: ' + PREVIEW_TOKEN);\n");
        js.append("    });\n");

        return js.toString();
    }

    private String renderStatusBarTimeScript() {
        return "    function updateStatusTime() {\n" +
                "      const now = new Date();\n" +
                "      const hours = String(now.getHours()).padStart(2, '0');\n" +
                "      const minutes = String(now.getMinutes()).padStart(2, '0');\n" +
                "      const el = document.getElementById('statusTime');\n" +
                "      if (el) el.textContent = hours + ':' + minutes;\n" +
                "    }\n" +
                "    setInterval(updateStatusTime, 30000);\n\n";
    }

    private String renderNavigationScript() {
        return "    function goBack() {\n" +
                "      if (window.history.length > 1) {\n" +
                "        window.history.back();\n" +
                "      } else {\n" +
                "        showToast('已经是第一页了');\n" +
                "      }\n" +
                "    }\n\n" +
                "    function showMenu() {\n" +
                "      showToast('更多菜单功能开发中');\n" +
                "    }\n\n";
    }

    private String renderSearchScript() {
        return "    function handleSearchInput(input) {\n" +
                "      const inner = document.getElementById('searchBarInner');\n" +
                "      if (inner) {\n" +
                "        if (input.value.trim().length > 0) {\n" +
                "          inner.classList.add('has-value');\n" +
                "        } else {\n" +
                "          inner.classList.remove('has-value');\n" +
                "        }\n" +
                "      }\n" +
                "    }\n\n" +
                "    function clearSearch() {\n" +
                "      const input = document.getElementById('searchInput');\n" +
                "      if (input) {\n" +
                "        input.value = '';\n" +
                "        handleSearchInput(input);\n" +
                "        input.focus();\n" +
                "      }\n" +
                "    }\n\n" +
                "    function handleSearch() {\n" +
                "      const input = document.getElementById('searchInput');\n" +
                "      if (input && input.value.trim()) {\n" +
                "        showToast('搜索: ' + input.value);\n" +
                "      }\n" +
                "    }\n\n";
    }

    private String renderSwiperScript() {
        return "    let swiperIndex = 0;\n" +
                "    let swiperTimer = null;\n" +
                "    const SWIPER_ITEMS = document.querySelectorAll('.swiper-slide');\n" +
                "    const SWIPER_DOTS = document.querySelectorAll('.swiper-dot');\n\n" +
                "    function goToSlide(index) {\n" +
                "      if (SWIPER_ITEMS.length === 0) return;\n" +
                "      swiperIndex = (index + SWIPER_ITEMS.length) % SWIPER_ITEMS.length;\n" +
                "      SWIPER_ITEMS.forEach((el, i) => el.classList.toggle('active', i === swiperIndex));\n" +
                "      SWIPER_DOTS.forEach((el, i) => el.classList.toggle('active', i === swiperIndex));\n" +
                "    }\n\n" +
                "    function startSwiper() {\n" +
                "      if (SWIPER_ITEMS.length <= 1) return;\n" +
                "      swiperTimer = setInterval(() => goToSlide(swiperIndex + 1), 3500);\n" +
                "    }\n\n" +
                "    function stopSwiper() {\n" +
                "      if (swiperTimer) clearInterval(swiperTimer);\n" +
                "      swiperTimer = null;\n" +
                "    }\n\n" +
                "    SWIPER_DOTS.forEach(dot => {\n" +
                "      dot.addEventListener('click', () => {\n" +
                "        const idx = parseInt(dot.dataset.index);\n" +
                "        goToSlide(idx);\n" +
                "        stopSwiper();\n" +
                "        startSwiper();\n" +
                "      });\n" +
                "    });\n\n" +
                "    startSwiper();\n\n";
    }

    private String renderCollapseScript() {
        return "    function toggleCollapse() {\n" +
                "      const el = document.getElementById('mobileCollapse');\n" +
                "      if (el) el.classList.toggle('expanded');\n" +
                "    }\n\n";
    }

    private String renderInputScript() {
        return "    function handleInputChange(input, id) {\n" +
                "      const field = document.getElementById('inputField_' + id);\n" +
                "      if (field) {\n" +
                "        field.classList.toggle('has-value', input.value.trim().length > 0);\n" +
                "      }\n" +
                "    }\n\n" +
                "    function clearInput(id) {\n" +
                "      const field = document.getElementById('inputField_' + id);\n" +
                "      if (field) {\n" +
                "        const input = field.querySelector('.mobile-input');\n" +
                "        if (input) {\n" +
                "          input.value = '';\n" +
                "          handleInputChange(input, id);\n" +
                "          input.focus();\n" +
                "        }\n" +
                "      }\n" +
                "    }\n\n";
    }

    private String renderButtonScript() {
        return "    function handleButtonClick(id, label) {\n" +
                "      showToast('点击了: ' + label);\n" +
                "    }\n\n";
    }

    private String renderGridItemScript() {
        return "    function handleGridItemClick(index, text) {\n" +
                "      showToast('打开: ' + text);\n" +
                "    }\n\n";
    }

    private String renderTabBarScript() {
        return "    let currentTabIndex = 0;\n" +
                "    function switchTab(index, label) {\n" +
                "      if (currentTabIndex === index) return;\n" +
                "      const tabs = document.querySelectorAll('.tab-bar-item');\n" +
                "      tabs.forEach((tab, i) => tab.classList.toggle('active', i === index));\n" +
                "      currentTabIndex = index;\n" +
                "      showToast('切换到: ' + label);\n" +
                "    }\n\n";
    }

    private String renderToastScript() {
        return "    let toastTimer = null;\n" +
                "    function showToast(message, duration) {\n" +
                "      const toast = document.getElementById('toast');\n" +
                "      if (!toast) return;\n" +
                "      toast.textContent = message;\n" +
                "      toast.classList.add('show');\n" +
                "      if (toastTimer) clearTimeout(toastTimer);\n" +
                "      toastTimer = setTimeout(() => {\n" +
                "        toast.classList.remove('show');\n" +
                "      }, duration || 2000);\n" +
                "    }\n\n";
    }

    private String renderTouchFeedbackScript() {
        return "    function createTouchFeedback(x, y) {\n" +
                "      const ripple = document.createElement('div');\n" +
                "      ripple.className = 'touch-feedback';\n" +
                "      const size = 40;\n" +
                "      ripple.style.width = size + 'px';\n" +
                "      ripple.style.height = size + 'px';\n" +
                "      ripple.style.left = (x - size / 2) + 'px';\n" +
                "      ripple.style.top = (y - size / 2) + 'px';\n" +
                "      document.body.appendChild(ripple);\n" +
                "      setTimeout(() => ripple.remove(), 600);\n" +
                "    }\n\n";
    }

    private String renderPullToRefreshScript() {
        return "    let touchStartY = 0;\n" +
                "    let isPulling = false;\n" +
                "    let pullDistance = 0;\n" +
                "    const PULL_THRESHOLD = 80;\n" +
                "    const pageContent = document.getElementById('pageContent');\n" +
                "    const pullText = document.getElementById('pullRefreshText');\n" +
                "    const appContainer = document.getElementById('appContainer');\n\n" +
                "    if (pageContent) {\n" +
                "      pageContent.addEventListener('touchstart', function(e) {\n" +
                "        if (pageContent.scrollTop <= 0) {\n" +
                "          touchStartY = e.touches[0].clientY;\n" +
                "          isPulling = true;\n" +
                "        }\n" +
                "      }, { passive: true });\n\n" +
                "      pageContent.addEventListener('touchmove', function(e) {\n" +
                "        if (!isPulling) return;\n" +
                "        const currentY = e.touches[0].clientY;\n" +
                "        pullDistance = currentY - touchStartY;\n" +
                "        if (pullDistance > 0 && pageContent.scrollTop <= 0) {\n" +
                "          const dampened = Math.min(pullDistance * 0.5, PULL_THRESHOLD + 20);\n" +
                "          pageContent.style.transform = 'translateY(' + dampened + 'px)';\n" +
                "          if (pullText) {\n" +
                "            pullText.textContent = dampened >= PULL_THRESHOLD ? '释放刷新' : '下拉刷新';\n" +
                "          }\n" +
                "        }\n" +
                "      }, { passive: true });\n\n" +
                "      pageContent.addEventListener('touchend', function() {\n" +
                "        if (!isPulling) return;\n" +
                "        isPulling = false;\n" +
                "        const dampened = Math.min(pullDistance * 0.5, PULL_THRESHOLD + 20);\n" +
                "        if (dampened >= PULL_THRESHOLD) {\n" +
                "          if (pullText) pullText.textContent = '正在刷新...';\n" +
                "          if (appContainer) appContainer.classList.add('refreshing');\n" +
                "          pageContent.style.transform = 'translateY(60px)';\n" +
                "          setTimeout(() => {\n" +
                "            pageContent.style.transform = '';\n" +
                "            if (appContainer) appContainer.classList.remove('refreshing');\n" +
                "            if (pullText) pullText.textContent = '下拉刷新';\n" +
                "            loadListData('mockList');\n" +
                "            showToast('刷新成功');\n" +
                "          }, 1500);\n" +
                "        } else {\n" +
                "          pageContent.style.transform = '';\n" +
                "        }\n" +
                "        pullDistance = 0;\n" +
                "      });\n" +
                "    }\n\n";
    }

    private String renderListLoaderScript() {
        return "    let mockListCache = null;\n" +
                "    async function loadListData(containerId) {\n" +
                "      const container = document.getElementById(containerId);\n" +
                "      if (!container) return;\n" +
                "      try {\n" +
                "        if (!mockListCache) {\n" +
                "          const res = await fetch(MOCK_API_BASE + '/list?pageSize=10');\n" +
                "          const data = await res.json();\n" +
                "          mockListCache = data.data || [];\n" +
                "        }\n" +
                "        renderListItems(container, mockListCache);\n" +
                "      } catch (e) {\n" +
                "        container.innerHTML = '<div class=\"empty-state\"><div class=\"empty-icon\">📭</div><div class=\"empty-text\">加载失败，请下拉刷新重试</div></div>';\n" +
                "      }\n" +
                "    }\n\n" +
                "    function renderListItems(container, items) {\n" +
                "      if (!items || items.length === 0) {\n" +
                "        container.innerHTML = '<div class=\"empty-state\"><div class=\"empty-icon\">📭</div><div class=\"empty-text\">暂无数据</div></div>';\n" +
                "        return;\n" +
                "      }\n" +
                "      const icons = ['📄', '💼', '🎯', '🚀', '⭐', '💡', '🔥', '🎉', '📊', '🎨'];\n" +
                "      const tags = [\n" +
                "        { text: '已完成', cls: 'tag-success' },\n" +
                "        { text: '处理中', cls: 'tag-info' },\n" +
                "        { text: '待审核', cls: 'tag-warning' },\n" +
                "        { text: '已驳回', cls: 'tag-error' }\n" +
                "      ];\n" +
                "      container.innerHTML = items.map((item, idx) => {\n" +
                "        const icon = icons[idx % icons.length];\n" +
                "        const tag = tags[idx % tags.length];\n" +
                "        return '<div class=\"list-item\" onclick=\"showToast(\\'查看: ' + escapeHtml(item.title) + '\\')\">' +\n" +
                "          '<div class=\"list-item-avatar\">' + icon + '</div>' +\n" +
                "          '<div class=\"list-item-content\">' +\n" +
                "          '<div class=\"list-item-title\">' + escapeHtml(item.title) + '</div>' +\n" +
                "          '<div class=\"list-item-desc\">' + escapeHtml(item.description) + '</div>' +\n" +
                "          '</div>' +\n" +
                "          '<div class=\"list-item-right\">' +\n" +
                "          '<div class=\"list-item-time\">' + item.createTime + '</div>' +\n" +
                "          '<div style=\"display:flex;align-items:center;gap:6px;\">' +\n" +
                "          '<span class=\"list-item-tag ' + tag.cls + '\">' + tag.text + '</span>' +\n" +
                "          '<span class=\"list-item-arrow\">›</span>' +\n" +
                "          '</div>' +\n" +
                "          '</div>' +\n" +
                "          '</div>';\n" +
                "      }).join('');\n" +
                "    }\n\n" +
                "    function escapeHtml(str) {\n" +
                "      const div = document.createElement('div');\n" +
                "      div.textContent = str;\n" +
                "      return div.innerHTML;\n" +
                "    }\n\n";
    }

    private String renderTouchEventsScript() {
        return "    let touchStartTime = 0;\n" +
                "    let touchStartX = 0;\n" +
                "    let touchStartYGlobal = 0;\n\n" +
                "    document.addEventListener('touchstart', function(e) {\n" +
                "      const touch = e.touches[0];\n" +
                "      touchStartTime = Date.now();\n" +
                "      touchStartX = touch.clientX;\n" +
                "      touchStartYGlobal = touch.clientY;\n" +
                "    }, { passive: true });\n\n" +
                "    document.addEventListener('touchend', function(e) {\n" +
                "      const touch = e.changedTouches[0];\n" +
                "      const duration = Date.now() - touchStartTime;\n" +
                "      const deltaX = touch.clientX - touchStartX;\n" +
                "      const deltaY = touch.clientY - touchStartYGlobal;\n" +
                "      if (duration < 200 && Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {\n" +
                "        createTouchFeedback(touch.clientX, touch.clientY);\n" +
                "      }\n" +
                "    }, { passive: true });\n\n";
    }

    private String renderExpiredPage(String previewToken, String message) {
        String safeMessage = message != null ? escapeHtml(message) : "预览链接已过期";
        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "  <title>预览已过期</title>\n" +
                "  <style>\n" +
                "    * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }\n" +
                "    .expired-card { background: #ffffff; border-radius: 20px; padding: 48px 32px; max-width: 360px; width: 100%; text-align: center; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }\n" +
                "    .expired-icon { font-size: 72px; margin-bottom: 24px; animation: pulse 2s ease-in-out infinite; }\n" +
                "    @keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.05); } }\n" +
                "    .expired-title { font-size: 22px; font-weight: 600; color: #333; margin-bottom: 12px; }\n" +
                "    .expired-desc { font-size: 14px; color: #666; line-height: 1.6; margin-bottom: 32px; }\n" +
                "    .expired-tip { font-size: 12px; color: #999; padding: 12px; background: #f5f5f5; border-radius: 8px; word-break: break-all; }\n" +
                "    .back-btn { display: inline-block; padding: 12px 40px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; border-radius: 25px; font-size: 15px; font-weight: 500; text-decoration: none; margin-top: 24px; transition: transform 0.2s ease; border: none; cursor: pointer; }\n" +
                "    .back-btn:active { transform: scale(0.97); }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"expired-card\">\n" +
                "    <div class=\"expired-icon\">⏰</div>\n" +
                "    <h1 class=\"expired-title\">预览链接已失效</h1>\n" +
                "    <p class=\"expired-desc\">" + safeMessage + "</p>\n" +
                "    <p class=\"expired-tip\">Token: " + escapeHtml(previewToken) + "</p>\n" +
                "    <button class=\"back-btn\" onclick=\"window.close()\">关闭页面</button>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String renderErrorPage(String previewToken, String message) {
        String safeMessage = message != null ? escapeHtml(message) : "页面加载出错";
        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "  <title>加载出错</title>\n" +
                "  <style>\n" +
                "    * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f5f5f5; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }\n" +
                "    .error-card { background: #ffffff; border-radius: 16px; padding: 40px 28px; max-width: 340px; width: 100%; text-align: center; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }\n" +
                "    .error-icon { font-size: 64px; margin-bottom: 20px; }\n" +
                "    .error-title { font-size: 20px; font-weight: 600; color: #333; margin-bottom: 12px; }\n" +
                "    .error-desc { font-size: 14px; color: #ff4d4f; line-height: 1.6; margin-bottom: 20px; padding: 12px; background: #fff2f0; border-radius: 8px; word-break: break-all; text-align: left; }\n" +
                "    .retry-btn { display: inline-block; padding: 10px 32px; background: #007AFF; color: #ffffff; border-radius: 20px; font-size: 14px; font-weight: 500; text-decoration: none; border: none; cursor: pointer; transition: all 0.2s ease; }\n" +
                "    .retry-btn:active { transform: scale(0.97); opacity: 0.85; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"error-card\">\n" +
                "    <div class=\"error-icon\">😵</div>\n" +
                "    <h1 class=\"error-title\">页面加载出错</h1>\n" +
                "    <p class=\"error-desc\">" + safeMessage + "</p>\n" +
                "    <button class=\"retry-btn\" onclick=\"location.reload()\">重新加载</button>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String extractMockPath(HttpServletRequest request, String previewToken) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String basePattern = contextPath + "/mobile/preview/" + previewToken + "/api/mock";
        String path = uri.substring(basePattern.length());
        if (path.isEmpty()) path = "/";
        return path;
    }

    private Map<String, Object> handleMockRequest(String path, Integer page, Integer pageSize, String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (path.startsWith("/list")) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", generateMockList(page, pageSize, keyword));
            result.put("total", 87);
            result.put("page", page);
            result.put("pageSize", pageSize);
        } else if (path.startsWith("/detail")) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", generateMockDetail());
        } else if (path.startsWith("/user")) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", generateMockUser());
        } else if (path.startsWith("/stats")) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", generateMockStats());
        } else {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", generateDefaultMockData(path));
            result.put("path", path);
        }

        return result;
    }

    private Map<String, Object> handleMockPostRequest(String path, Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long newId = mockIdGenerator.incrementAndGet();

        if (path.startsWith("/create") || path.startsWith("/add")) {
            result.put("code", 200);
            result.put("message", "创建成功");
            result.put("data", new LinkedHashMap<String, Object>() {{
                put("id", newId);
                put("createdAt", LocalDateTime.now().format(DATE_FORMATTER));
                putAll(body != null ? body : Collections.emptyMap());
            }});
        } else if (path.startsWith("/update")) {
            result.put("code", 200);
            result.put("message", "更新成功");
            result.put("data", new LinkedHashMap<String, Object>() {{
                put("updatedAt", LocalDateTime.now().format(DATE_FORMATTER));
                putAll(body != null ? body : Collections.emptyMap());
            }});
        } else if (path.startsWith("/delete")) {
            result.put("code", 200);
            result.put("message", "删除成功");
            result.put("data", null);
        } else {
            result.put("code", 200);
            result.put("message", "操作成功");
            result.put("data", body);
            result.put("path", path);
        }

        return result;
    }

    private List<Map<String, Object>> generateMockList(Integer page, Integer pageSize, String keyword) {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] titles = {
                "低代码平台用户需求文档评审会议纪要",
                "移动端H5预览功能开发进度周报",
                "数据模型设计方案讨论与确认",
                "工作流引擎性能优化测试报告",
                "组件库迭代计划与roadmap公示",
                "权限管理模块重构方案评审",
                "生产环境部署操作手册更新通知",
                "系统安全审计发现问题整改",
                "API接口文档自动生成工具发布",
                "新员工入职培训安排"
        };
        String[] descriptions = {
                "完成了核心功能需求的梳理和优先级排序",
                "已完成80%的功能开发，预计下周提测",
                "包含ER图、字段定义、索引策略等",
                "优化后TPS提升了45%，响应时间缩短60%",
                "新增15个业务组件，优化32个现有组件",
                "解决了权限粒度不够精细的问题",
                "更新了滚动发布和蓝绿部署的流程",
                "涉及3个高危漏洞和5个中危问题",
                "支持OpenAPI 3.0规范，一键导出文档",
                "包含系统架构、开发规范、安全意识等"
        };
        String[] times = generateRandomTimes(pageSize);

        int startIdx = (page - 1) * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = (startIdx + i) % titles.length;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", Long.valueOf(startIdx + i + 1));
            item.put("title", titles[idx] + (keyword != null && !keyword.isEmpty() ? " (含:" + keyword + ")" : ""));
            item.put("description", descriptions[idx]);
            item.put("createTime", times[i]);
            item.put("status", idx % 4);
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> generateMockDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", 1);
        detail.put("title", "低代码平台用户需求文档评审会议纪要");
        detail.put("description", "本次会议主要讨论了低代码平台的核心功能需求，包括可视化拖拽设计器、组件库建设、数据模型管理、工作流引擎、权限管理、移动端适配等关键模块。经过充分讨论，确定了各模块的优先级和交付时间表。");
        detail.put("content", "<h3>一、会议背景</h3><p>为了更好地推进低代码平台建设...</p>");
        detail.put("createTime", "2024-01-15 14:30:00");
        detail.put("updateTime", "2024-01-15 18:20:00");
        detail.put("author", "张三");
        detail.put("status", 1);
        detail.put("attachments", listOf(
                mapOf("name", "需求文档v1.0.pdf", "size", "2.3MB"),
                mapOf("name", "评审意见汇总.xlsx", "size", "85KB")
        ));
        return detail;
    }

    private Map<String, Object> generateMockUser() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", 10086);
        user.put("username", "admin");
        user.put("nickname", "系统管理员");
        user.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=admin");
        user.put("email", "admin@lowcode.com");
        user.put("phone", "138****8888");
        user.put("department", "技术中台部");
        user.put("role", "超级管理员");
        user.put("permissions", listOf("*:*:*"));
        return user;
    }

    private Map<String, Object> generateMockStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayVisits", 1256);
        stats.put("totalUsers", 8754);
        stats.put("totalApps", 326);
        stats.put("totalPages", 2148);
        stats.put("weeklyGrowth", 12.5);
        stats.put("chartData", listOf(120, 200, 150, 80, 70, 110, 130));
        return stats;
    }

    private Map<String, Object> generateDefaultMockData(String path) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("path", path);
        data.put("timestamp", System.currentTimeMillis());
        data.put("message", "这是Mock数据，请根据实际需求配置真实API");
        data.put("items", listOf(
                mapOf("id", 1, "name", "示例数据1"),
                mapOf("id", 2, "name", "示例数据2"),
                mapOf("id", 3, "name", "示例数据3")
        ));
        return data;
    }

    private String[] generateRandomTimes(int count) {
        String[] times = new String[count];
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            LocalDateTime time = now.minusMinutes((long) (Math.random() * 1440 * 7));
            times[i] = time.format(DATE_FORMATTER);
        }
        return times;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            Object obj = JSON.parse(jsonStr);
            if (obj instanceof Map) {
                return (Map<String, Object>) obj;
            }
            return new LinkedHashMap<>();
        } catch (Exception e) {
            log.warn("解析JSON失败: {}", jsonStr, e);
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> parseMobileConfig(String configStr) {
        Map<String, Object> config = parseJson(configStr);
        if (!config.containsKey("themeColor")) {
            config.put("themeColor", THEME_COLOR);
        }
        return config;
    }

    private Map<String, Object> parsePageConfig(String configStr) {
        Map<String, Object> config = parseJson(configStr);
        if (!config.containsKey("enablePullDownRefresh")) {
            config.put("enablePullDownRefresh", true);
        }
        if (!config.containsKey("enableReachBottom")) {
            config.put("enableReachBottom", true);
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(Map<String, Object> config, String key, T defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(String.valueOf(value));
            } else if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(String.valueOf(value));
            } else if (defaultValue instanceof Long) {
                return (T) Long.valueOf(String.valueOf(value));
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(String.valueOf(value));
            } else if (defaultValue instanceof List) {
                if (value instanceof List) {
                    return (T) value;
                }
            } else if (defaultValue instanceof String) {
                return (T) String.valueOf(value);
            }
            return (T) value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String buildStyleString(Map<String, Object> styleMap) {
        if (styleMap == null || styleMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : styleMap.entrySet()) {
            String key = camelToKebab(entry.getKey());
            Object value = entry.getValue();
            if (value != null) {
                sb.append(key).append(": ").append(value);
                if (isNumericCssProperty(entry.getKey()) && isNumericValue(value.toString())) {
                    sb.append("px");
                }
                sb.append("; ");
            }
        }
        return sb.toString().trim();
    }

    private boolean isNumericCssProperty(String property) {
        return property.matches("(?i)(width|height|top|left|right|bottom|margin|padding|fontSize|borderRadius|borderWidth|gap|rowGap|columnGap)$");
    }

    private boolean isNumericValue(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String camelToKebab(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    private String lightenColor(String color, double amount) {
        return adjustColor(color, amount);
    }

    private String darkenColor(String color, double amount) {
        return adjustColor(color, -amount);
    }

    private String adjustColor(String color, double amount) {
        try {
            String hex = color.replace("#", "");
            if (hex.length() == 3) {
                hex = hex.replaceAll("(.)", "$1$1");
            }
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            if (amount >= 0) {
                r = (int) Math.round(r + (255 - r) * amount);
                g = (int) Math.round(g + (255 - g) * amount);
                b = (int) Math.round(b + (255 - b) * amount);
            } else {
                r = (int) Math.round(r * (1 + amount));
                g = (int) Math.round(g * (1 + amount));
                b = (int) Math.round(b * (1 + amount));
            }

            return String.format("#%02x%02x%02x", Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
        } catch (Exception e) {
            return color;
        }
    }

    @SafeVarargs
    private final <T> List<T> listOf(T... elements) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, elements);
        return list;
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = String.valueOf(keyValues[i]);
            Object value = keyValues[i + 1];
            map.put(key, value);
        }
        return map;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                case '&': sb.append("&amp;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}