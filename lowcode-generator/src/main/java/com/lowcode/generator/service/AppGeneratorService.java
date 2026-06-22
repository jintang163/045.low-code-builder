package com.lowcode.generator.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson2.JSON;
import com.lowcode.common.enums.DbTypeEnum;
import com.lowcode.generator.config.GeneratorConfig;
import com.lowcode.generator.entity.AppGenerateConfig;
import com.lowcode.generator.entity.GeneratedApp;
import com.lowcode.generator.entity.GeneratedCode;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.mapper.DataModelMapper;
import com.lowcode.model.mapper.ModelFieldMapper;
import com.lowcode.page.entity.Page;
import com.lowcode.page.mapper.PageMapper;
import com.lowcode.flow.entity.BusinessLogic;
import com.lowcode.flow.mapper.BusinessLogicMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AppGeneratorService {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private DataModelMapper dataModelMapper;

    @Autowired
    private ModelFieldMapper modelFieldMapper;

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private BusinessLogicMapper businessLogicMapper;

    @Autowired
    private K8sGeneratorService k8sGeneratorService;

    @Autowired
    private SdkGeneratorService sdkGeneratorService;

    public GeneratedApp generateApp(AppGenerateConfig config) throws Exception {
        GeneratedApp app = new GeneratedApp();
        app.setAppName(config.getAppName());
        app.setAppCode(config.getAppCode());
        app.setVersion(config.getVersion());

        loadAppBoundResources(config);

        String tempDir = System.getProperty("java.io.tmpdir") + "/lowcode/" + config.getAppCode() + "_" + System.currentTimeMillis();
        Files.createDirectories(Paths.get(tempDir));

        List<GeneratedCode> backendCodes = new ArrayList<>();
        List<GeneratedCode> frontendCodes = new ArrayList<>();
        List<GeneratedCode> configFiles = new ArrayList<>();
        List<GeneratedCode> k8sFiles = new ArrayList<>();
        List<GeneratedCode> sdkCodes = new ArrayList<>();

        if (config.isIncludeBackend()) {
            backendCodes.addAll(generateBackend(config, tempDir));
        }

        if (config.isIncludeFrontend()) {
            frontendCodes.addAll(generateFrontend(config, tempDir));
        }

        if (config.isGenerateDocker()) {
            configFiles.addAll(generateDockerConfig(config, tempDir));
        }

        if (config.isGenerateK8s()) {
            k8sFiles.addAll(generateK8sConfig(config, tempDir));
        }

        if (config.isGenerateSdk()) {
            sdkCodes.addAll(generateSdk(config, tempDir));
        }

        if (config.isGenerateReadme()) {
            configFiles.add(generateReadme(config, tempDir));
        }

        String zipPath = tempDir + "/" + config.getAppCode() + "-" + config.getVersion() + ".zip";
        ZipUtil.zip(tempDir + "/src", zipPath);

        File zipFile = new File(zipPath);
        app.setFileSize(zipFile.length());
        app.setDownloadUrl("/api/app/download/" + config.getAppCode());
        app.setBackendCodes(backendCodes);
        app.setFrontendCodes(frontendCodes);
        app.setConfigFiles(configFiles);
        app.setK8sFiles(k8sFiles);
        app.setSdkCodes(sdkCodes);

        return app;
    }

    private List<GeneratedCode> generateBackend(AppGenerateConfig config, String tempDir) throws Exception {
        List<GeneratedCode> codes = new ArrayList<>();
        String baseDir = tempDir + "/src/backend/" + config.getAppCode();

        codes.add(generatePomXml(config, baseDir));
        codes.add(generateBootstrapYml(config, baseDir));
        codes.add(generateApplicationYml(config, baseDir));
        codes.add(generateMainApplication(config, baseDir));

        GeneratorConfig genConfig = new GeneratorConfig();
        genConfig.setAuthor(config.getAuthor());
        genConfig.setVersion(config.getVersion());
        genConfig.setBasePackage(config.getBasePackage());
        genConfig.setModuleName(config.getModuleName());
        genConfig.setGenerateEntity(true);
        genConfig.setGenerateMapper(true);
        genConfig.setGenerateService(true);
        genConfig.setGenerateController(true);
        genConfig.setGenerateVo(true);
        genConfig.setGenerateDto(true);

        if (config.getDataModelIds() != null) {
            for (Long modelId : config.getDataModelIds()) {
                DataModel model = dataModelMapper.selectById(modelId);
                if (model != null) {
                    List<ModelField> fields = modelFieldMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ModelField>()
                                    .eq(ModelField::getModelId, modelId)
                                    .orderByAsc(ModelField::getSortOrder)
                    );
                    model.setFields(fields);
                    codes.addAll(codeGeneratorService.generateAll(model, genConfig));
                }
            }
        }

        if (config.getLogicIds() != null) {
            for (Long logicId : config.getLogicIds()) {
                BusinessLogic logic = businessLogicMapper.selectById(logicId);
                if (logic != null && logic.getGeneratedCode() != null && !logic.getGeneratedCode().isEmpty()) {
                    String packagePath = config.getBasePackage().replace(".", "/");
                    String fileName = toCamelCase(logic.getLogicCode()) + "Logic.java";
                    String filePath = "src/main/java/" + packagePath + "/" + config.getModuleName() + "/logic/" + fileName;
                    codes.add(new GeneratedCode("BUSINESS_LOGIC", fileName, filePath, logic.getGeneratedCode()));
                }
            }
        }

        for (GeneratedCode code : codes) {
            writeCodeToFile(baseDir, code);
        }

        return codes;
    }

    private GeneratedCode generatePomXml(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("    <modelVersion>4.0.0</modelVersion>\n\n");
        sb.append("    <groupId>").append(config.getBasePackage()).append("</groupId>\n");
        sb.append("    <artifactId>").append(config.getAppCode()).append("</artifactId>\n");
        sb.append("    <version>").append(config.getVersion()).append("</version>\n");
        sb.append("    <packaging>jar</packaging>\n");
        sb.append("    <name>").append(config.getAppName()).append("</name>\n");
        sb.append("    <description>").append(config.getAppDesc()).append("</description>\n\n");
        sb.append("    <parent>\n");
        sb.append("        <groupId>org.springframework.boot</groupId>\n");
        sb.append("        <artifactId>spring-boot-starter-parent</artifactId>\n");
        sb.append("        <version>2.7.18</version>\n");
        sb.append("        <relativePath/>\n");
        sb.append("    </parent>\n\n");
        sb.append("    <properties>\n");
        sb.append("        <java.version>1.8</java.version>\n");
        sb.append("        <mybatis-plus.version>3.5.5</mybatis-plus.version>\n");
        sb.append("    </properties>\n\n");
        sb.append("    <dependencies>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>org.springframework.boot</groupId>\n");
        sb.append("            <artifactId>spring-boot-starter-web</artifactId>\n");
        sb.append("        </dependency>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>com.baomidou</groupId>\n");
        sb.append("            <artifactId>mybatis-plus-boot-starter</artifactId>\n");
        sb.append("            <version>${mybatis-plus.version}</version>\n");
        sb.append("        </dependency>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>mysql</groupId>\n");
        sb.append("            <artifactId>mysql-connector-java</artifactId>\n");
        sb.append("        </dependency>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>org.projectlombok</groupId>\n");
        sb.append("            <artifactId>lombok</artifactId>\n");
        sb.append("        </dependency>\n");
        sb.append("    </dependencies>\n\n");
        sb.append("    <build>\n");
        sb.append("        <finalName>${project.artifactId}</finalName>\n");
        sb.append("        <plugins>\n");
        sb.append("            <plugin>\n");
        sb.append("                <groupId>org.springframework.boot</groupId>\n");
        sb.append("                <artifactId>spring-boot-maven-plugin</artifactId>\n");
        sb.append("            </plugin>\n");
        sb.append("        </plugins>\n");
        sb.append("    </build>\n");
        sb.append("</project>\n");

        return new GeneratedCode("POM", "pom.xml", "pom.xml", sb.toString());
    }

    private GeneratedCode generateBootstrapYml(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("spring:\n");
        sb.append("  application:\n");
        sb.append("    name: ").append(config.getAppCode()).append("\n");
        sb.append("  profiles:\n");
        sb.append("    active: dev\n\n");
        sb.append("  cloud:\n");
        sb.append("    nacos:\n");
        sb.append("      discovery:\n");
        sb.append("        enabled: ${NACOS_ENABLED:false}\n");
        sb.append("        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}\n");
        sb.append("      config:\n");
        sb.append("        enabled: ${NACOS_ENABLED:false}\n");
        sb.append("        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}\n");
        sb.append("        file-extension: yml\n");
        return new GeneratedCode("CONFIG", "bootstrap.yml", "src/main/resources/bootstrap.yml", sb.toString());
    }

    private GeneratedCode generateApplicationYml(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("server:\n");
        sb.append("  port: 8080\n\n");
        sb.append("spring:\n");
        sb.append("  datasource:\n");
        sb.append("    driver-class-name: ").append(getDriverClassName(config.getDbType())).append("\n");
        sb.append("    url: jdbc:mysql://").append(config.getDbHost()).append(":").append(config.getDbPort())
                .append("/").append(config.getDbName()).append("?useUnicode=true&characterEncoding=utf8\n");
        sb.append("    username: ").append(config.getDbUsername()).append("\n");
        sb.append("    password: ").append(config.getDbPassword()).append("\n\n");
        sb.append("  redis:\n");
        sb.append("    host: ").append(config.getRedisHost()).append("\n");
        sb.append("    port: ").append(config.getRedisPort()).append("\n");
        sb.append("    password: ").append(config.getRedisPassword() != null ? config.getRedisPassword() : "").append("\n");
        sb.append("    database: ").append(config.getRedisDatabase()).append("\n\n");
        sb.append("mybatis-plus:\n");
        sb.append("  mapper-locations: classpath*:/mapper/**/*.xml\n");
        sb.append("  configuration:\n");
        sb.append("    map-underscore-to-camel-case: true\n");
        sb.append("  global-config:\n");
        sb.append("    db-config:\n");
        sb.append("      id-type: AUTO\n");
        sb.append("      logic-delete-field: deleted\n");
        sb.append("      logic-delete-value: 1\n");
        sb.append("      logic-not-delete-value: 0\n");
        return new GeneratedCode("CONFIG", "application.yml", "src/main/resources/application.yml", sb.toString());
    }

    private GeneratedCode generateMainApplication(AppGenerateConfig config, String baseDir) {
        String packagePath = config.getBasePackage().replace(".", "/");
        String className = toCamelCase(config.getAppCode()) + "Application";
        String filePath = "src/main/java/" + packagePath + "/" + className + ".java";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(config.getBasePackage()).append(";\n\n");
        sb.append("import org.mybatis.spring.annotation.MapperScan;\n");
        sb.append("import org.springframework.boot.SpringApplication;\n");
        sb.append("import org.springframework.boot.autoconfigure.SpringBootApplication;\n");
        sb.append("import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;\n");
        sb.append("import org.springframework.cloud.client.discovery.EnableDiscoveryClient;\n");
        sb.append("import org.springframework.context.annotation.Bean;\n");
        sb.append("import org.springframework.context.annotation.Configuration;\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(config.getAppName()).append(" 启动类\n");
        sb.append(" * @author ").append(config.getAuthor()).append("\n");
        sb.append(" * @since ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append(" */\n");
        sb.append("@SpringBootApplication\n");
        sb.append("@MapperScan(\"").append(config.getBasePackage()).append(".").append(config.getModuleName()).append(".mapper\")\n");
        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        SpringApplication.run(").append(className).append(".class, args);\n");
        sb.append("    }\n\n");
        sb.append("    @Configuration\n");
        sb.append("    @ConditionalOnProperty(name = \"spring.cloud.nacos.discovery.enabled\", havingValue = \"true\", matchIfMissing = false)\n");
        sb.append("    @EnableDiscoveryClient\n");
        sb.append("    static class DiscoveryClientConfig {\n");
        sb.append("    }\n");
        sb.append("}\n");

        return new GeneratedCode("MAIN", className + ".java", filePath, sb.toString());
    }

    private List<GeneratedCode> generateFrontend(AppGenerateConfig config, String tempDir) throws Exception {
        List<GeneratedCode> codes = new ArrayList<>();
        String baseDir = tempDir + "/src/frontend/" + config.getAppCode() + "-web";

        List<Page> pages = new ArrayList<>();
        if (config.getPageIds() != null && !config.getPageIds().isEmpty()) {
            for (Long pageId : config.getPageIds()) {
                Page page = pageMapper.selectById(pageId);
                if (page != null) {
                    pages.add(page);
                }
            }
        }

        codes.add(generateFrontendPackageJson(config, baseDir));
        codes.add(generateFrontendViteConfig(config, baseDir));
        codes.add(generateFrontendMainTs(config, baseDir));
        codes.add(generateFrontendAppTsx(config, baseDir, pages));
        codes.addAll(generateFrontendPages(config, baseDir, pages));
        codes.add(generateFrontendRouter(config, baseDir, pages));
        codes.add(generateFrontendServices(config, baseDir));

        for (GeneratedCode code : codes) {
            writeCodeToFile(baseDir, code);
        }

        return codes;
    }

    private GeneratedCode generateFrontendPackageJson(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(config.getAppCode()).append("-web\",\n");
        sb.append("  \"version\": \"").append(config.getVersion()).append("\",\n");
        sb.append("  \"description\": \"").append(config.getAppDesc()).append("\",\n");
        sb.append("  \"scripts\": {\n");
        sb.append("    \"dev\": \"vite\",\n");
        sb.append("    \"build\": \"vite build\",\n");
        sb.append("    \"preview\": \"vite preview\"\n");
        sb.append("  },\n");
        sb.append("  \"dependencies\": {\n");
        sb.append("    \"react\": \"^18.2.0\",\n");
        sb.append("    \"react-dom\": \"^18.2.0\",\n");
        sb.append("    \"antd\": \"^5.12.0\",\n");
        sb.append("    \"@ant-design/icons\": \"^5.2.6\",\n");
        sb.append("    \"axios\": \"^1.6.2\",\n");
        sb.append("    \"react-router-dom\": \"^6.20.0\",\n");
        sb.append("    \"zustand\": \"^4.4.7\"\n");
        sb.append("  },\n");
        sb.append("  \"devDependencies\": {\n");
        sb.append("    \"@types/react\": \"^18.2.0\",\n");
        sb.append("    \"@types/react-dom\": \"^18.2.0\",\n");
        sb.append("    \"@vitejs/plugin-react\": \"^4.2.0\",\n");
        sb.append("    \"typescript\": \"^5.3.0\",\n");
        sb.append("    \"vite\": \"^5.0.0\"\n");
        sb.append("  }\n");
        sb.append("}\n");
        return new GeneratedCode("PACKAGE_JSON", "package.json", "package.json", sb.toString());
    }

    private GeneratedCode generateFrontendViteConfig(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("import { defineConfig } from 'vite'\n");
        sb.append("import react from '@vitejs/plugin-react'\n\n");
        sb.append("export default defineConfig({\n");
        sb.append("  plugins: [react()],\n");
        sb.append("  server: {\n");
        sb.append("    port: 3000,\n");
        sb.append("    proxy: {\n");
        sb.append("      '/api': {\n");
        sb.append("        target: 'http://localhost:8080',\n");
        sb.append("        changeOrigin: true\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("})\n");
        return new GeneratedCode("VITE_CONFIG", "vite.config.ts", "vite.config.ts", sb.toString());
    }

    private GeneratedCode generateFrontendMainTs(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("import React from 'react'\n");
        sb.append("import ReactDOM from 'react-dom/client'\n");
        sb.append("import { ConfigProvider } from 'antd'\n");
        sb.append("import zhCN from 'antd/locale/zh_CN'\n");
        sb.append("import App from './App'\n");
        sb.append("import './index.css'\n\n");
        sb.append("ReactDOM.createRoot(document.getElementById('root')!).render(\n");
        sb.append("  <React.StrictMode>\n");
        sb.append("    <ConfigProvider locale={zhCN}>\n");
        sb.append("      <App />\n");
        sb.append("    </ConfigProvider>\n");
        sb.append("  </React.StrictMode>\n");
        sb.append(")\n");
        return new GeneratedCode("MAIN_TS", "main.tsx", "src/main.tsx", sb.toString());
    }

    private GeneratedCode generateFrontendAppTsx(AppGenerateConfig config, String baseDir, List<Page> pages) {
        StringBuilder sb = new StringBuilder();
        sb.append("import React from 'react'\n");
        sb.append("import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'\n");
        sb.append("import { Layout, Menu } from 'antd'\n\n");

        if (pages != null && !pages.isEmpty()) {
            for (Page page : pages) {
                String componentName = toCamelCase(page.getPageCode()) + "Page";
                sb.append("import ").append(componentName).append(" from './pages/").append(componentName).append("'\n");
            }
        }

        sb.append("\nconst { Header, Content, Sider } = Layout\n\n");
        sb.append("const App: React.FC = () => {\n");
        sb.append("  const menuItems = [\n");
        if (pages != null && !pages.isEmpty()) {
            for (Page page : pages) {
                String pagePath = page.getPagePath() != null ? page.getPagePath() : "/" + page.getPageCode();
                sb.append("    { key: '").append(page.getPageCode()).append("', label: <Link to='").append(pagePath).append("'>").append(page.getPageName()).append("</Link> },\n");
            }
        }
        sb.append("  ]\n\n");
        sb.append("  return (\n");
        sb.append("    <BrowserRouter>\n");
        sb.append("      <Layout style={{ minHeight: '100vh' }}>\n");
        sb.append("        <Sider theme='dark' width={220}>\n");
        sb.append("          <div style={{ height: 60, margin: 16, background: 'rgba(255, 255, 255, 0.2)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontWeight: 'bold' }}>\n");
        sb.append("            ").append(config.getAppName()).append("\n");
        sb.append("          </div>\n");
        sb.append("          <Menu theme='dark' mode='inline' items={menuItems} />\n");
        sb.append("        </Sider>\n");
        sb.append("        <Layout>\n");
        sb.append("          <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>\n");
        sb.append("            <h2 style={{ margin: 0 }}>").append(config.getAppName()).append("</h2>\n");
        sb.append("          </Header>\n");
        sb.append("          <Content style={{ padding: '24px', background: '#f5f5f5' }}>\n");
        sb.append("            <div style={{ background: '#fff', padding: 24, minHeight: 360, borderRadius: 8 }}>\n");
        sb.append("              <Routes>\n");
        if (pages != null && !pages.isEmpty()) {
            for (Page page : pages) {
                String pagePath = page.getPagePath() != null ? page.getPagePath() : "/" + page.getPageCode();
                String componentName = toCamelCase(page.getPageCode()) + "Page";
                String isHome = page.getIsHome() != null && page.getIsHome() == 1 ? "path='/' " : "";
                sb.append("                <Route ").append(isHome).append("path='").append(pagePath).append("' element={<").append(componentName).append(" />} />\n");
            }
        }
        sb.append("                <Route path='/' element={<div>Welcome to ").append(config.getAppName()).append("</div>} />\n");
        sb.append("              </Routes>\n");
        sb.append("            </div>\n");
        sb.append("          </Content>\n");
        sb.append("        </Layout>\n");
        sb.append("      </Layout>\n");
        sb.append("    </BrowserRouter>\n");
        sb.append("  )\n");
        sb.append("}\n\n");
        sb.append("export default App\n");
        return new GeneratedCode("APP_TSX", "App.tsx", "src/App.tsx", sb.toString());
    }

    private List<GeneratedCode> generateFrontendPages(AppGenerateConfig config, String baseDir, List<Page> pages) {
        List<GeneratedCode> codes = new ArrayList<>();
        if (pages == null || pages.isEmpty()) {
            return codes;
        }

        for (Page page : pages) {
            String componentName = toCamelCase(page.getPageCode()) + "Page";
            String fileName = componentName + ".tsx";
            String filePath = "src/pages/" + fileName;

            StringBuilder sb = new StringBuilder();
            sb.append("import React, { useState, useEffect } from 'react'\n");
            sb.append("import { Table, Button, Space, Modal, Form, Input, message, Popconfirm, Card } from 'antd'\n");
            sb.append("import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'\n\n");
            sb.append("const ").append(componentName).append(": React.FC = () => {\n");
            sb.append("  const [data, setData] = useState<any[]>([])\n");
            sb.append("  const [loading, setLoading] = useState(false)\n");
            sb.append("  const [modalVisible, setModalVisible] = useState(false)\n");
            sb.append("  const [editingId, setEditingId] = useState<number | null>(null)\n");
            sb.append("  const [form] = Form.useForm()\n\n");
            sb.append("  const fetchData = async () => {\n");
            sb.append("    setLoading(true)\n");
            sb.append("    try {\n");
            sb.append("      // TODO: 调用实际的API获取数据\n");
            sb.append("      setData([])\n");
            sb.append("    } catch (error) {\n");
            sb.append("      message.error('获取数据失败')\n");
            sb.append("    } finally {\n");
            sb.append("      setLoading(false)\n");
            sb.append("    }\n");
            sb.append("  }\n\n");
            sb.append("  useEffect(() => {\n");
            sb.append("    fetchData()\n");
            sb.append("  }, [])\n\n");
            sb.append("  const handleAdd = () => {\n");
            sb.append("    setEditingId(null)\n");
            sb.append("    form.resetFields()\n");
            sb.append("    setModalVisible(true)\n");
            sb.append("  }\n\n");
            sb.append("  const handleEdit = (record: any) => {\n");
            sb.append("    setEditingId(record.id)\n");
            sb.append("    form.setFieldsValue(record)\n");
            sb.append("    setModalVisible(true)\n");
            sb.append("  }\n\n");
            sb.append("  const handleDelete = (id: number) => {\n");
            sb.append("    // TODO: 调用删除API\n");
            sb.append("    message.success('删除成功')\n");
            sb.append("    fetchData()\n");
            sb.append("  }\n\n");
            sb.append("  const handleSubmit = async () => {\n");
            sb.append("    try {\n");
            sb.append("      const values = await form.validateFields()\n");
            sb.append("      // TODO: 调用保存API\n");
            sb.append("      message.success(editingId ? '更新成功' : '创建成功')\n");
            sb.append("      setModalVisible(false)\n");
            sb.append("      fetchData()\n");
            sb.append("    } catch (error) {\n");
            sb.append("      message.error('操作失败')\n");
            sb.append("    }\n");
            sb.append("  }\n\n");
            sb.append("  const columns = [\n");
            sb.append("    { title: 'ID', dataIndex: 'id', key: 'id' },\n");
            sb.append("    { title: '名称', dataIndex: 'name', key: 'name' },\n");
            sb.append("    { title: '创建时间', dataIndex: 'createdTime', key: 'createdTime' },\n");
            sb.append("    {\n");
            sb.append("      title: '操作',\n");
            sb.append("      key: 'action',\n");
            sb.append("      render: (_: any, record: any) => (\n");
            sb.append("        <Space>\n");
            sb.append("          <Button type='link' icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>\n");
            sb.append("          <Popconfirm title='确定删除?' onConfirm={() => handleDelete(record.id)}>\n");
            sb.append("            <Button type='link' danger icon={<DeleteOutlined />}>删除</Button>\n");
            sb.append("          </Popconfirm>\n");
            sb.append("        </Space>\n");
            sb.append("      ),\n");
            sb.append("    },\n");
            sb.append("  ]\n\n");
            sb.append("  return (\n");
            sb.append("    <div>\n");
            sb.append("      <Card\n");
            sb.append("        title='").append(page.getPageName()).append("'\n");
            sb.append("        extra={\n");
            sb.append("          <Button type='primary' icon={<PlusOutlined />} onClick={handleAdd}>\n");
            sb.append("            新增\n");
            sb.append("          </Button>\n");
            sb.append("        }\n");
            sb.append("      >\n");
            sb.append("        <Table\n");
            sb.append("          columns={columns}\n");
            sb.append("          dataSource={data}\n");
            sb.append("          loading={loading}\n");
            sb.append("          rowKey='id'\n");
            sb.append("        />\n");
            sb.append("      </Card>\n\n");
            sb.append("      <Modal\n");
            sb.append("        title={editingId ? '编辑' : '新增'}\n");
            sb.append("        open={modalVisible}\n");
            sb.append("        onOk={handleSubmit}\n");
            sb.append("        onCancel={() => setModalVisible(false)}\n");
            sb.append("      >\n");
            sb.append("        <Form form={form} layout='vertical'>\n");
            sb.append("          <Form.Item name='name' label='名称' rules={[{ required: true }]}>\n");
            sb.append("            <Input placeholder='请输入名称' />\n");
            sb.append("          </Form.Item>\n");
            sb.append("        </Form>\n");
            sb.append("      </Modal>\n");
            sb.append("    </div>\n");
            sb.append("  )\n");
            sb.append("}\n\n");
            sb.append("export default ").append(componentName).append("\n");

            codes.add(new GeneratedCode("FRONTEND_PAGE", fileName, filePath, sb.toString()));
        }

        return codes;
    }

    private GeneratedCode generateFrontendRouter(AppGenerateConfig config, String baseDir, List<Page> pages) {
        StringBuilder sb = new StringBuilder();
        sb.append("export interface MenuItem {\n");
        sb.append("  key: string\n");
        sb.append("  label: string\n");
        sb.append("  path: string\n");
        sb.append("  icon?: string\n");
        sb.append("}\n\n");
        sb.append("export const menuConfig: MenuItem[] = [\n");
        if (pages != null && !pages.isEmpty()) {
            for (Page page : pages) {
                String pagePath = page.getPagePath() != null ? page.getPagePath() : "/" + page.getPageCode();
                sb.append("  {\n");
                sb.append("    key: '").append(page.getPageCode()).append("',\n");
                sb.append("    label: '").append(page.getPageName()).append("',\n");
                sb.append("    path: '").append(pagePath).append("',\n");
                sb.append("  },\n");
            }
        }
        sb.append("]\n");
        return new GeneratedCode("ROUTER_CONFIG", "menuConfig.ts", "src/config/menuConfig.ts", sb.toString());
    }

    private GeneratedCode generateFrontendServices(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("import request from '../utils/request'\n\n");
        sb.append("export interface ApiResponse<T = any> {\n");
        sb.append("  code: number\n");
        sb.append("  message: string\n");
        sb.append("  data: T\n");
        sb.append("}\n\n");
        sb.append("export interface PageResult<T> {\n");
        sb.append("  list: T[]\n");
        sb.append("  total: number\n");
        sb.append("  page: number\n");
        sb.append("  pageSize: number\n");
        sb.append("}\n\n");
        sb.append("export const api = {\n");
        if (config.getDataModelIds() != null && !config.getDataModelIds().isEmpty()) {
            for (Long modelId : config.getDataModelIds()) {
                DataModel model = dataModelMapper.selectById(modelId);
                if (model != null) {
                    String modelName = model.getModelName() != null ? model.getModelName() : model.getTableName();
                    String varName = toCamelCase(model.getTableName());
                    sb.append("  ").append(varName).append(": {\n");
                    sb.append("    list: (params: any) => request.get<ApiResponse<PageResult>>('/api/").append(model.getTableName()).append("/list', { params }),\n");
                    sb.append("    get: (id: number) => request.get<ApiResponse>(`/api/").append(model.getTableName()).append("/${id}`),\n");
                    sb.append("    create: (data: any) => request.post<ApiResponse>('/api/").append(model.getTableName()).append("', data),\n");
                    sb.append("    update: (data: any) => request.put<ApiResponse>('/api/").append(model.getTableName()).append("', data),\n");
                    sb.append("    delete: (id: number) => request.delete<ApiResponse>(`/api/").append(model.getTableName()).append("/${id}`),\n");
                    sb.append("  },\n");
                }
            }
        }
        sb.append("}\n");
        return new GeneratedCode("API_SERVICES", "api.ts", "src/services/api.ts", sb.toString());
    }

    private List<GeneratedCode> generateDockerConfig(AppGenerateConfig config, String tempDir) throws Exception {
        List<GeneratedCode> codes = new ArrayList<>();
        String baseDir = tempDir + "/src/docker";

        codes.add(generateBackendDockerfile(config, baseDir));
        if (config.isIncludeFrontend()) {
            codes.add(generateFrontendDockerfile(config, baseDir));
        }
        codes.add(generateDockerCompose(config, baseDir));
        codes.add(generateNginxConfig(config, baseDir));

        for (GeneratedCode code : codes) {
            writeCodeToFile(tempDir + "/src", code);
        }

        return codes;
    }

    private GeneratedCode generateBackendDockerfile(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("FROM openjdk:8-jdk-alpine\n");
        sb.append("LABEL maintainer=\"").append(config.getAuthor()).append("\"\n");
        sb.append("LABEL app=\"").append(config.getAppCode()).append("-backend\"\n");
        sb.append("VOLUME /tmp\n");
        sb.append("WORKDIR /app\n");
        sb.append("COPY target/").append(config.getAppCode()).append(".jar app.jar\n");
        sb.append("RUN apk add --no-cache tzdata \\\n");
        sb.append("    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \\\n");
        sb.append("    && echo \"Asia/Shanghai\" > /etc/timezone\n");
        sb.append("ENV TZ Asia/Shanghai\n");
        sb.append("ENV JAVA_OPTS=\"-Xms256m -Xmx512m -XX:+UseG1GC\"\n");
        sb.append("ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar\"]\n");
        sb.append("EXPOSE 8080\n");
        sb.append("HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \\\n");
        sb.append("  CMD wget -q -O - http://localhost:8080/actuator/health || exit 1\n");
        return new GeneratedCode("BACKEND_DOCKERFILE", "Dockerfile.backend", "docker/Dockerfile.backend", sb.toString());
    }

    private GeneratedCode generateFrontendDockerfile(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("FROM node:18-alpine AS builder\n");
        sb.append("LABEL maintainer=\"").append(config.getAuthor()).append("\"\n");
        sb.append("LABEL app=\"").append(config.getAppCode()).append("-frontend\"\n");
        sb.append("WORKDIR /app\n");
        sb.append("COPY package*.json ./\n");
        sb.append("RUN npm install --registry=https://registry.npmmirror.com\n");
        sb.append("COPY . .\n");
        sb.append("RUN npm run build\n\n");
        sb.append("FROM nginx:alpine AS production\n");
        sb.append("LABEL maintainer=\"").append(config.getAuthor()).append("\"\n");
        sb.append("LABEL app=\"").append(config.getAppCode()).append("-frontend\"\n");
        sb.append("RUN apk add --no-cache tzdata \\\n");
        sb.append("    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \\\n");
        sb.append("    && echo \"Asia/Shanghai\" > /etc/timezone\n");
        sb.append("ENV TZ Asia/Shanghai\n");
        sb.append("COPY --from=builder /app/dist /usr/share/nginx/html\n");
        sb.append("COPY nginx.conf /etc/nginx/conf.d/default.conf\n");
        sb.append("EXPOSE 80\n");
        sb.append("HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \\\n");
        sb.append("  CMD wget -q -O - http://localhost:80/ || exit 1\n");
        sb.append("CMD [\"nginx\", \"-g\", \"daemon off;\"]\n");
        return new GeneratedCode("FRONTEND_DOCKERFILE", "Dockerfile.frontend", "docker/Dockerfile.frontend", sb.toString());
    }

    private GeneratedCode generateDockerCompose(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("version: '3.8'\n");
        sb.append("services:\n");
        sb.append("  ").append(config.getAppCode()).append("-backend:\n");
        sb.append("    build:\n");
        sb.append("      context: ../backend/").append(config.getAppCode()).append("\n");
        sb.append("      dockerfile: ../../docker/Dockerfile.backend\n");
        sb.append("    image: ").append(config.getAppCode()).append("-backend:").append(config.getVersion()).append("\n");
        sb.append("    container_name: ").append(config.getAppCode()).append("-backend\n");
        sb.append("    restart: unless-stopped\n");
        sb.append("    ports:\n");
        sb.append("      - \"8080:8080\"\n");
        sb.append("    environment:\n");
        sb.append("      - SPRING_PROFILES_ACTIVE=prod\n");
        sb.append("      - DB_HOST=mysql\n");
        sb.append("      - DB_PORT=3306\n");
        sb.append("      - DB_NAME=").append(config.getDbName()).append("\n");
        sb.append("      - DB_USERNAME=").append(config.getDbUsername()).append("\n");
        sb.append("      - DB_PASSWORD=").append(config.getDbPassword()).append("\n");
        sb.append("      - REDIS_HOST=redis\n");
        sb.append("      - REDIS_PORT=6379\n");
        sb.append("    depends_on:\n");
        sb.append("      mysql:\n");
        sb.append("        condition: service_healthy\n");
        sb.append("      redis:\n");
        sb.append("        condition: service_healthy\n");
        sb.append("    networks:\n");
        sb.append("      - ").append(config.getAppCode()).append("-network\n");
        sb.append("    healthcheck:\n");
        sb.append("      test: [\"CMD\", \"wget\", \"-q\", \"-O\", \"-\", \"http://localhost:8080/actuator/health\"]\n");
        sb.append("      interval: 30s\n");
        sb.append("      timeout: 3s\n");
        sb.append("      retries: 3\n");
        sb.append("      start_period: 60s\n\n");

        if (config.isIncludeFrontend()) {
            sb.append("  ").append(config.getAppCode()).append("-frontend:\n");
            sb.append("    build:\n");
            sb.append("      context: ../frontend/").append(config.getAppCode()).append("-web\n");
            sb.append("      dockerfile: ../../docker/Dockerfile.frontend\n");
            sb.append("    image: ").append(config.getAppCode()).append("-frontend:").append(config.getVersion()).append("\n");
            sb.append("    container_name: ").append(config.getAppCode()).append("-frontend\n");
            sb.append("    restart: unless-stopped\n");
            sb.append("    ports:\n");
            sb.append("      - \"80:80\"\n");
            sb.append("    depends_on:\n");
            sb.append("      - ").append(config.getAppCode()).append("-backend\n");
            sb.append("    networks:\n");
            sb.append("      - ").append(config.getAppCode()).append("-network\n");
            sb.append("    healthcheck:\n");
            sb.append("      test: [\"CMD\", \"wget\", \"-q\", \"-O\", \"-\", \"http://localhost:80/\"]\n");
            sb.append("      interval: 30s\n");
            sb.append("      timeout: 3s\n");
            sb.append("      retries: 3\n");
            sb.append("      start_period: 10s\n\n");
        }

        sb.append("  mysql:\n");
        sb.append("    image: mysql:8.0\n");
        sb.append("    container_name: ").append(config.getAppCode()).append("-mysql\n");
        sb.append("    restart: unless-stopped\n");
        sb.append("    environment:\n");
        sb.append("      MYSQL_ROOT_PASSWORD: ").append(config.getDbPassword()).append("\n");
        sb.append("      MYSQL_DATABASE: ").append(config.getDbName()).append("\n");
        sb.append("      TZ: Asia/Shanghai\n");
        sb.append("    ports:\n");
        sb.append("      - \"3306:3306\"\n");
        sb.append("    volumes:\n");
        sb.append("      - mysql-data:/var/lib/mysql\n");
        sb.append("      - ./mysql/init:/docker-entrypoint-initdb.d\n");
        sb.append("    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci\n");
        sb.append("    networks:\n");
        sb.append("      - ").append(config.getAppCode()).append("-network\n");
        sb.append("    healthcheck:\n");
        sb.append("      test: [\"CMD\", \"mysqladmin\", \"ping\", \"-h\", \"localhost\", \"-u").append(config.getDbUsername()).append("\", \"-p").append(config.getDbPassword()).append("\"]\n");
        sb.append("      interval: 10s\n");
        sb.append("      timeout: 5s\n");
        sb.append("      retries: 5\n");
        sb.append("      start_period: 30s\n\n");
        sb.append("  redis:\n");
        sb.append("    image: redis:7-alpine\n");
        sb.append("    container_name: ").append(config.getAppCode()).append("-redis\n");
        sb.append("    restart: unless-stopped\n");
        sb.append("    ports:\n");
        sb.append("      - \"6379:6379\"\n");
        sb.append("    volumes:\n");
        sb.append("      - redis-data:/data\n");
        sb.append("    command: redis-server --appendonly yes --requirepass ").append(config.getRedisPassword() != null && !config.getRedisPassword().isEmpty() ? config.getRedisPassword() : "\"\"").append("\n");
        sb.append("    networks:\n");
        sb.append("      - ").append(config.getAppCode()).append("-network\n");
        sb.append("    healthcheck:\n");
        sb.append("      test: [\"CMD\", \"redis-cli\", \"ping\"]\n");
        sb.append("      interval: 10s\n");
        sb.append("      timeout: 3s\n");
        sb.append("      retries: 3\n");
        sb.append("      start_period: 5s\n\n");
        sb.append("volumes:\n");
        sb.append("  mysql-data:\n");
        sb.append("  redis-data:\n\n");
        sb.append("networks:\n");
        sb.append("  ").append(config.getAppCode()).append("-network:\n");
        sb.append("    driver: bridge\n");
        return new GeneratedCode("DOCKER_COMPOSE", "docker-compose.yml", "docker/docker-compose.yml", sb.toString());
    }

    private GeneratedCode generateNginxConfig(AppGenerateConfig config, String baseDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("server {\n");
        sb.append("    listen 80;\n");
        sb.append("    server_name localhost;\n\n");
        sb.append("    location / {\n");
        sb.append("        root /usr/share/nginx/html;\n");
        sb.append("        index index.html index.htm;\n");
        sb.append("        try_files $uri $uri/ /index.html;\n");
        sb.append("    }\n\n");
        sb.append("    location /api {\n");
        sb.append("        proxy_pass http://").append(config.getAppCode()).append(":8080;\n");
        sb.append("        proxy_set_header Host $host;\n");
        sb.append("        proxy_set_header X-Real-IP $remote_addr;\n");
        sb.append("        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return new GeneratedCode("NGINX_CONFIG", "nginx.conf", "docker/nginx.conf", sb.toString());
    }

    private List<GeneratedCode> generateK8sConfig(AppGenerateConfig config, String tempDir) throws Exception {
        List<GeneratedCode> codes = k8sGeneratorService.generateK8sFiles(config);
        for (GeneratedCode code : codes) {
            writeCodeToFile(tempDir + "/src", code);
        }
        return codes;
    }

    private List<GeneratedCode> generateSdk(AppGenerateConfig config, String tempDir) throws Exception {
        List<GeneratedCode> codes = sdkGeneratorService.generateSdk(config);
        for (GeneratedCode code : codes) {
            writeCodeToFile(tempDir + "/src", code);
        }
        return codes;
    }

    private GeneratedCode generateReadme(AppGenerateConfig config, String tempDir) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(config.getAppName()).append("\n\n");
        sb.append(config.getAppDesc()).append("\n\n");
        sb.append("## 技术栈\n\n");
        sb.append("- 后端：Spring Boot 2.7 + MyBatis Plus + MySQL\n");
        sb.append("- 前端：React 18 + Ant Design 5 + Vite\n");
        sb.append("- 部署：Docker + Docker Compose\n\n");
        sb.append("## 快速开始\n\n");
        sb.append("### 方式一：Docker Compose 一键部署\n\n");
        sb.append("```bash\n");
        sb.append("cd docker\n");
        sb.append("docker-compose up -d\n");
        sb.append("```\n\n");
        sb.append("### 方式二：本地运行\n\n");
        sb.append("#### 启动后端\n\n");
        sb.append("```bash\n");
        sb.append("cd backend/").append(config.getAppCode()).append("\n");
        sb.append("mvn clean package\n");
        sb.append("java -jar target/").append(config.getAppCode()).append(".jar\n");
        sb.append("```\n\n");
        sb.append("#### 启动前端\n\n");
        sb.append("```bash\n");
        sb.append("cd frontend/").append(config.getAppCode()).append("-web\n");
        sb.append("npm install\n");
        sb.append("npm run dev\n");
        sb.append("```\n\n");
        sb.append("## 数据库配置\n\n");
        sb.append("- 主机：").append(config.getDbHost()).append("\n");
        sb.append("- 端口：").append(config.getDbPort()).append("\n");
        sb.append("- 数据库名：").append(config.getDbName()).append("\n");
        sb.append("- 用户名：").append(config.getDbUsername()).append("\n\n");
        sb.append("## 版本信息\n\n");
        sb.append("- 版本号：").append(config.getVersion()).append("\n");
        sb.append("- 生成时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("- 作者：").append(config.getAuthor()).append("\n");

        String filePath = "README.md";
        GeneratedCode code = new GeneratedCode("README", "README.md", filePath, sb.toString());
        writeCodeToFile(tempDir + "/src", code);
        return code;
    }

    private void writeCodeToFile(String baseDir, GeneratedCode code) throws Exception {
        Path filePath = Paths.get(baseDir, code.getFilePath());
        Files.createDirectories(filePath.getParent());
        try (FileWriter writer = new FileWriter(filePath.toFile(), StandardCharsets.UTF_8)) {
            writer.write(code.getCodeContent());
        }
    }

    private void loadAppBoundResources(AppGenerateConfig config) {
        if (config.getAppId() == null) {
            return;
        }

        if (config.getDataModelIds() == null || config.getDataModelIds().isEmpty()) {
            List<DataModel> models = dataModelMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DataModel>()
                            .eq(DataModel::getAppId, config.getAppId())
                            .eq(DataModel::getStatus, 1)
            );
            if (models != null && !models.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                for (DataModel model : models) {
                    ids.add(model.getId());
                }
                config.setDataModelIds(ids);
            }
        }

        if (config.getPageIds() == null || config.getPageIds().isEmpty()) {
            List<Page> pages = pageMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Page>()
                            .eq(Page::getAppId, config.getAppId())
                            .eq(Page::getStatus, 1)
            );
            if (pages != null && !pages.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                for (Page page : pages) {
                    ids.add(page.getId());
                }
                config.setPageIds(ids);
            }
        }

        if (config.getLogicIds() == null || config.getLogicIds().isEmpty()) {
            List<BusinessLogic> logics = businessLogicMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BusinessLogic>()
                            .eq(BusinessLogic::getAppId, config.getAppId())
                            .eq(BusinessLogic::getStatus, "1")
            );
            if (logics != null && !logics.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                for (BusinessLogic logic : logics) {
                    ids.add(logic.getId());
                }
                config.setLogicIds(ids);
            }
        }
    }

    private String getDriverClassName(String dbType) {
        DbTypeEnum type = DbTypeEnum.valueOf(dbType.toUpperCase());
        switch (type) {
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case DM:
                return "dm.jdbc.driver.DmDriver";
            case MYSQL:
            default:
                return "com.mysql.cj.jdbc.Driver";
        }
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : str.toCharArray()) {
            if (c == '-' || c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public byte[] downloadApp(String appCode) throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir") + "/lowcode/";
        Path dirPath = Paths.get(tempDir);
        if (!Files.exists(dirPath)) {
            throw new Exception("应用不存在或已过期");
        }

        File[] dirs = dirPath.toFile().listFiles((dir, name) -> name.startsWith(appCode + "_"));
        if (dirs == null || dirs.length == 0) {
            throw new Exception("应用不存在或已过期");
        }

        File latestDir = dirs[0];
        for (File dir : dirs) {
            if (dir.lastModified() > latestDir.lastModified()) {
                latestDir = dir;
            }
        }

        File zipFile = new File(latestDir, appCode + "-*.zip");
        File[] zipFiles = latestDir.listFiles((dir, name) -> name.startsWith(appCode + "-") && name.endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            throw new Exception("应用包不存在");
        }

        return Files.readAllBytes(zipFiles[0].toPath());
    }
}
