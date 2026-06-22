package com.lowcode.generator.service;

import com.alibaba.fastjson2.JSON;
import com.lowcode.flow.entity.AppExposedApi;
import com.lowcode.flow.mapper.AppExposedApiMapper;
import com.lowcode.generator.entity.AppGenerateConfig;
import com.lowcode.generator.entity.GeneratedCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SdkGeneratorService {

    @Autowired
    private AppExposedApiMapper appExposedApiMapper;

    public List<GeneratedCode> generateSdk(AppGenerateConfig config) {
        List<GeneratedCode> codes = new ArrayList<>();
        String sdkLanguage = config.getSdkLanguage() != null ? config.getSdkLanguage() : "java";

        List<AppExposedApi> apis = getApis(config);

        switch (sdkLanguage.toLowerCase()) {
            case "java":
                codes.addAll(generateJavaSdk(config, apis));
                break;
            case "javascript":
            case "js":
                codes.addAll(generateJavaScriptSdk(config, apis));
                break;
            case "typescript":
            case "ts":
                codes.addAll(generateTypeScriptSdk(config, apis));
                break;
            case "python":
                codes.addAll(generatePythonSdk(config, apis));
                break;
            default:
                codes.addAll(generateJavaSdk(config, apis));
                break;
        }

        return codes;
    }

    private List<AppExposedApi> getApis(AppGenerateConfig config) {
        List<AppExposedApi> apis = new ArrayList<>();
        try {
            apis = appExposedApiMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppExposedApi>()
                            .eq(AppExposedApi::getAppCode, config.getAppCode())
                            .eq(AppExposedApi::getStatus, 1)
            );
        } catch (Exception e) {
            apis = getDefaultApis(config);
        }
        if (apis == null || apis.isEmpty()) {
            apis = getDefaultApis(config);
        }
        return apis;
    }

    private List<AppExposedApi> getDefaultApis(AppGenerateConfig config) {
        List<AppExposedApi> apis = new ArrayList<>();

        apis.add(AppExposedApi.builder()
                .apiName("获取列表")
                .apiCode(config.getAppCode() + ".list")
                .apiType("HTTP")
                .httpMethod("GET")
                .apiPath("/api/" + config.getAppCode() + "/list")
                .description("分页查询数据列表")
                .authType("TOKEN")
                .build());

        apis.add(AppExposedApi.builder()
                .apiName("获取详情")
                .apiCode(config.getAppCode() + ".get")
                .apiType("HTTP")
                .httpMethod("GET")
                .apiPath("/api/" + config.getAppCode() + "/{id}")
                .description("根据ID获取详情")
                .authType("TOKEN")
                .build());

        apis.add(AppExposedApi.builder()
                .apiName("创建")
                .apiCode(config.getAppCode() + ".create")
                .apiType("HTTP")
                .httpMethod("POST")
                .apiPath("/api/" + config.getAppCode())
                .description("创建新记录")
                .authType("TOKEN")
                .build());

        apis.add(AppExposedApi.builder()
                .apiName("更新")
                .apiCode(config.getAppCode() + ".update")
                .apiType("HTTP")
                .httpMethod("PUT")
                .apiPath("/api/" + config.getAppCode())
                .description("更新记录")
                .authType("TOKEN")
                .build());

        apis.add(AppExposedApi.builder()
                .apiName("删除")
                .apiCode(config.getAppCode() + ".delete")
                .apiType("HTTP")
                .httpMethod("DELETE")
                .apiPath("/api/" + config.getAppCode() + "/{id}")
                .description("根据ID删除记录")
                .authType("TOKEN")
                .build());

        return apis;
    }

    private List<GeneratedCode> generateJavaSdk(AppGenerateConfig config, List<AppExposedApi> apis) {
        List<GeneratedCode> codes = new ArrayList<>();
        String basePackage = config.getBasePackage() != null ? config.getBasePackage() : "com.lowcode.sdk";
        String packagePath = basePackage.replace(".", "/");
        String className = toCamelCase(config.getAppCode()) + "Client";

        codes.add(generateJavaPom(config));
        codes.add(generateJavaClient(config, basePackage, packagePath, className, apis));
        codes.add(generateJavaConfig(config, basePackage, packagePath));
        codes.add(generateJavaApiResponse(config, basePackage, packagePath, apis));
        codes.add(generateSdkReadme(config, "Java"));

        return codes;
    }

    private GeneratedCode generateJavaPom(AppGenerateConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("    <modelVersion>4.0.0</modelVersion>\n\n");
        sb.append("    <groupId>").append(config.getBasePackage() != null ? config.getBasePackage() : "com.lowcode").append("</groupId>\n");
        sb.append("    <artifactId>").append(config.getAppCode()).append("-sdk</artifactId>\n");
        sb.append("    <version>").append(config.getVersion()).append("</version>\n");
        sb.append("    <packaging>jar</packaging>\n");
        sb.append("    <name>").append(config.getAppName()).append(" SDK</name>\n\n");
        sb.append("    <dependencies>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>com.squareup.okhttp3</groupId>\n");
        sb.append("            <artifactId>okhttp</artifactId>\n");
        sb.append("            <version>4.12.0</version>\n");
        sb.append("        </dependency>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>com.alibaba</groupId>\n");
        sb.append("            <artifactId>fastjson2</artifactId>\n");
        sb.append("            <version>2.0.43</version>\n");
        sb.append("        </dependency>\n");
        sb.append("        <dependency>\n");
        sb.append("            <groupId>org.projectlombok</groupId>\n");
        sb.append("            <artifactId>lombok</artifactId>\n");
        sb.append("            <version>1.18.30</version>\n");
        sb.append("            <scope>provided</scope>\n");
        sb.append("        </dependency>\n");
        sb.append("    </dependencies>\n");
        sb.append("</project>\n");
        return new GeneratedCode("SDK_JAVA_POM", "pom.xml", "sdk/java/pom.xml", sb.toString());
    }

    private GeneratedCode generateJavaClient(AppGenerateConfig config, String basePackage, String packagePath, String className, List<AppExposedApi> apis) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".sdk;\n\n");
        sb.append("import com.alibaba.fastjson2.JSON;\n");
        sb.append("import okhttp3.*;\n\n");
        sb.append("import java.io.IOException;\n");
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.concurrent.TimeUnit;\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(config.getAppName()).append(" SDK 客户端\n");
        sb.append(" * \n");
        sb.append(" * @author ").append(config.getAuthor() != null ? config.getAuthor() : "lowcode").append("\n");
        sb.append(" * @since ").append(config.getVersion()).append("\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("    private static final MediaType JSON_TYPE = MediaType.parse(\"application/json; charset=utf-8\");\n\n");
        sb.append("    private final String baseUrl;\n");
        sb.append("    private final String apiKey;\n");
        sb.append("    private final OkHttpClient client;\n\n");
        sb.append("    public ").append(className).append("(String baseUrl, String apiKey) {\n");
        sb.append("        this.baseUrl = baseUrl;\n");
        sb.append("        this.apiKey = apiKey;\n");
        sb.append("        this.client = new OkHttpClient.Builder()\n");
        sb.append("                .connectTimeout(30, TimeUnit.SECONDS)\n");
        sb.append("                .readTimeout(30, TimeUnit.SECONDS)\n");
        sb.append("                .writeTimeout(30, TimeUnit.SECONDS)\n");
        sb.append("                .build();\n");
        sb.append("    }\n\n");
        sb.append("    private String get(String path) throws IOException {\n");
        sb.append("        Request request = new Request.Builder()\n");
        sb.append("                .url(baseUrl + path)\n");
        sb.append("                .addHeader(\"Authorization\", \"Bearer \" + apiKey)\n");
        sb.append("                .get()\n");
        sb.append("                .build();\n\n");
        sb.append("        try (Response response = client.newCall(request).execute()) {\n");
        sb.append("            if (!response.isSuccessful()) throw new IOException(\"Unexpected code \" + response);\n");
        sb.append("            return response.body().string();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private String post(String path, Object body) throws IOException {\n");
        sb.append("        RequestBody requestBody = RequestBody.create(JSON.toJSONString(body), JSON_TYPE);\n");
        sb.append("        Request request = new Request.Builder()\n");
        sb.append("                .url(baseUrl + path)\n");
        sb.append("                .addHeader(\"Authorization\", \"Bearer \" + apiKey)\n");
        sb.append("                .post(requestBody)\n");
        sb.append("                .build();\n\n");
        sb.append("        try (Response response = client.newCall(request).execute()) {\n");
        sb.append("            if (!response.isSuccessful()) throw new IOException(\"Unexpected code \" + response);\n");
        sb.append("            return response.body().string();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private String put(String path, Object body) throws IOException {\n");
        sb.append("        RequestBody requestBody = RequestBody.create(JSON.toJSONString(body), JSON_TYPE);\n");
        sb.append("        Request request = new Request.Builder()\n");
        sb.append("                .url(baseUrl + path)\n");
        sb.append("                .addHeader(\"Authorization\", \"Bearer \" + apiKey)\n");
        sb.append("                .put(requestBody)\n");
        sb.append("                .build();\n\n");
        sb.append("        try (Response response = client.newCall(request).execute()) {\n");
        sb.append("            if (!response.isSuccessful()) throw new IOException(\"Unexpected code \" + response);\n");
        sb.append("            return response.body().string();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private String delete(String path) throws IOException {\n");
        sb.append("        Request request = new Request.Builder()\n");
        sb.append("                .url(baseUrl + path)\n");
        sb.append("                .addHeader(\"Authorization\", \"Bearer \" + apiKey)\n");
        sb.append("                .delete()\n");
        sb.append("                .build();\n\n");
        sb.append("        try (Response response = client.newCall(request).execute()) {\n");
        sb.append("            if (!response.isSuccessful()) throw new IOException(\"Unexpected code \" + response);\n");
        sb.append("            return response.body().string();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        for (AppExposedApi api : apis) {
            String methodName = toMethodName(api.getApiCode());
            sb.append("    /**\n");
            sb.append("     * ").append(api.getApiName()).append("\n");
            sb.append("     * ").append(api.getDescription() != null ? api.getDescription() : "").append("\n");
            sb.append("     */\n");
            sb.append("    public String ").append(methodName).append("(");
            if ("POST".equals(api.getHttpMethod()) || "PUT".equals(api.getHttpMethod())) {
                sb.append("Object data");
            } else if (api.getApiPath().contains("{id}")) {
                sb.append("Long id");
            }
            sb.append(") throws IOException {\n");
            String path = api.getApiPath();
            if (path.contains("{id}")) {
                String javaPath = path.replace("{id}", "\" + id + \"");
                if ("GET".equals(api.getHttpMethod())) {
                    sb.append("        return get(\"").append(javaPath).append("\");\n");
                } else if ("DELETE".equals(api.getHttpMethod())) {
                    sb.append("        return delete(\"").append(javaPath).append("\");\n");
                }
            } else if ("GET".equals(api.getHttpMethod())) {
                sb.append("        return get(\"").append(path).append("\");\n");
            } else if ("POST".equals(api.getHttpMethod())) {
                sb.append("        return post(\"").append(path).append("\", data);\n");
            } else if ("PUT".equals(api.getHttpMethod())) {
                sb.append("        return put(\"").append(path).append("\", data);\n");
            } else if ("DELETE".equals(api.getHttpMethod())) {
                sb.append("        return delete(\"").append(path).append("\");\n");
            }
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return new GeneratedCode("SDK_JAVA_CLIENT", className + ".java",
                "sdk/java/src/main/java/" + packagePath + "/sdk/" + className + ".java", sb.toString());
    }

    private GeneratedCode generateJavaConfig(AppGenerateConfig config, String basePackage, String packagePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".sdk;\n\n");
        sb.append("/**\n");
        sb.append(" * SDK 配置类\n");
        sb.append(" */\n");
        sb.append("public class SdkConfig {\n\n");
        sb.append("    private String baseUrl;\n");
        sb.append("    private String apiKey;\n");
        sb.append("    private int connectTimeout = 30;\n");
        sb.append("    private int readTimeout = 30;\n\n");
        sb.append("    public SdkConfig() {\n");
        sb.append("    }\n\n");
        sb.append("    public SdkConfig(String baseUrl, String apiKey) {\n");
        sb.append("        this.baseUrl = baseUrl;\n");
        sb.append("        this.apiKey = apiKey;\n");
        sb.append("    }\n\n");
        sb.append("    public String getBaseUrl() {\n");
        sb.append("        return baseUrl;\n");
        sb.append("    }\n\n");
        sb.append("    public void setBaseUrl(String baseUrl) {\n");
        sb.append("        this.baseUrl = baseUrl;\n");
        sb.append("    }\n\n");
        sb.append("    public String getApiKey() {\n");
        sb.append("        return apiKey;\n");
        sb.append("    }\n\n");
        sb.append("    public void setApiKey(String apiKey) {\n");
        sb.append("        this.apiKey = apiKey;\n");
        sb.append("    }\n\n");
        sb.append("    public int getConnectTimeout() {\n");
        sb.append("        return connectTimeout;\n");
        sb.append("    }\n\n");
        sb.append("    public void setConnectTimeout(int connectTimeout) {\n");
        sb.append("        this.connectTimeout = connectTimeout;\n");
        sb.append("    }\n\n");
        sb.append("    public int getReadTimeout() {\n");
        sb.append("        return readTimeout;\n");
        sb.append("    }\n\n");
        sb.append("    public void setReadTimeout(int readTimeout) {\n");
        sb.append("        this.readTimeout = readTimeout;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return new GeneratedCode("SDK_JAVA_CONFIG", "SdkConfig.java",
                "sdk/java/src/main/java/" + packagePath + "/sdk/SdkConfig.java", sb.toString());
    }

    private GeneratedCode generateJavaApiResponse(AppGenerateConfig config, String basePackage, String packagePath, List<AppExposedApi> apis) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".sdk;\n\n");
        sb.append("import lombok.Data;\n\n");
        sb.append("/**\n");
        sb.append(" * API 响应结果\n");
        sb.append(" *\n");
        sb.append(" * @param <T> 数据类型\n");
        sb.append(" */\n");
        sb.append("@Data\n");
        sb.append("public class ApiResponse<T> {\n\n");
        sb.append("    private Integer code;\n");
        sb.append("    private String message;\n");
        sb.append("    private T data;\n\n");
        sb.append("    public boolean isSuccess() {\n");
        sb.append("        return code != null && (code == 0 || code == 200);\n");
        sb.append("    }\n");
        sb.append("}\n");
        return new GeneratedCode("SDK_JAVA_RESPONSE", "ApiResponse.java",
                "sdk/java/src/main/java/" + packagePath + "/sdk/ApiResponse.java", sb.toString());
    }

    private List<GeneratedCode> generateJavaScriptSdk(AppGenerateConfig config, List<AppExposedApi> apis) {
        List<GeneratedCode> codes = new ArrayList<>();
        String className = toCamelCase(config.getAppCode()) + "Client";

        codes.add(generateJsPackageJson(config));
        codes.add(generateJsClient(config, className, apis));
        codes.add(generateSdkReadme(config, "JavaScript"));

        return codes;
    }

    private GeneratedCode generateJsPackageJson(AppGenerateConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(config.getAppCode()).append("-sdk\",\n");
        sb.append("  \"version\": \"").append(config.getVersion()).append("\",\n");
        sb.append("  \"description\": \"").append(config.getAppName()).append(" SDK\",\n");
        sb.append("  \"main\": \"index.js\",\n");
        sb.append("  \"dependencies\": {\n");
        sb.append("    \"axios\": \"^1.6.0\"\n");
        sb.append("  }\n");
        sb.append("}\n");
        return new GeneratedCode("SDK_JS_PACKAGE", "package.json", "sdk/javascript/package.json", sb.toString());
    }

    private GeneratedCode generateJsClient(AppGenerateConfig config, String className, List<AppExposedApi> apis) {
        StringBuilder sb = new StringBuilder();
        sb.append("const axios = require('axios');\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(config.getAppName()).append(" SDK 客户端\n");
        sb.append(" */\n");
        sb.append("class ").append(className).append(" {\n\n");
        sb.append("  constructor(baseUrl, apiKey) {\n");
        sb.append("    this.baseUrl = baseUrl;\n");
        sb.append("    this.apiKey = apiKey;\n");
        sb.append("    this.client = axios.create({\n");
        sb.append("      baseURL: baseUrl,\n");
        sb.append("      timeout: 30000,\n");
        sb.append("      headers: {\n");
        sb.append("        'Authorization': 'Bearer ' + apiKey,\n");
        sb.append("        'Content-Type': 'application/json'\n");
        sb.append("      }\n");
        sb.append("    });\n");
        sb.append("  }\n\n");

        for (AppExposedApi api : apis) {
            String methodName = toMethodName(api.getApiCode());
            sb.append("  /**\n");
            sb.append("   * ").append(api.getApiName()).append("\n");
            sb.append("   * ").append(api.getDescription() != null ? api.getDescription() : "").append("\n");
            sb.append("   */\n");
            sb.append("  async ").append(methodName).append("(");
            if ("POST".equals(api.getHttpMethod()) || "PUT".equals(api.getHttpMethod())) {
                sb.append("data");
            } else if (api.getApiPath().contains("{id}")) {
                sb.append("id");
            }
            sb.append(") {\n");
            String path = api.getApiPath();
            if (path.contains("{id}")) {
                path = path.replace("{id}", "${id}");
                if ("GET".equals(api.getHttpMethod())) {
                    sb.append("    const response = await this.client.get(`").append(path).append("`);\n");
                } else if ("DELETE".equals(api.getHttpMethod())) {
                    sb.append("    const response = await this.client.delete(`").append(path).append("`);\n");
                }
            } else if ("GET".equals(api.getHttpMethod())) {
                sb.append("    const response = await this.client.get('").append(path).append("');\n");
            } else if ("POST".equals(api.getHttpMethod())) {
                sb.append("    const response = await this.client.post('").append(path).append("', data);\n");
            } else if ("PUT".equals(api.getHttpMethod())) {
                sb.append("    const response = await this.client.put('").append(path).append("', data);\n");
            }
            sb.append("    return response.data;\n");
            sb.append("  }\n\n");
        }

        sb.append("}\n\n");
        sb.append("module.exports = ").append(className).append(";\n");
        return new GeneratedCode("SDK_JS_CLIENT", "index.js", "sdk/javascript/index.js", sb.toString());
    }

    private List<GeneratedCode> generateTypeScriptSdk(AppGenerateConfig config, List<AppExposedApi> apis) {
        List<GeneratedCode> codes = new ArrayList<>();
        String className = toCamelCase(config.getAppCode()) + "Client";

        codes.add(generateTsPackageJson(config));
        codes.add(generateTsClient(config, className, apis));
        codes.add(generateSdkReadme(config, "TypeScript"));

        return codes;
    }

    private GeneratedCode generateTsPackageJson(AppGenerateConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(config.getAppCode()).append("-sdk\",\n");
        sb.append("  \"version\": \"").append(config.getVersion()).append("\",\n");
        sb.append("  \"description\": \"").append(config.getAppName()).append(" SDK\",\n");
        sb.append("  \"main\": \"dist/index.js\",\n");
        sb.append("  \"types\": \"dist/index.d.ts\",\n");
        sb.append("  \"scripts\": {\n");
        sb.append("    \"build\": \"tsc\"\n");
        sb.append("  },\n");
        sb.append("  \"dependencies\": {\n");
        sb.append("    \"axios\": \"^1.6.0\"\n");
        sb.append("  },\n");
        sb.append("  \"devDependencies\": {\n");
        sb.append("    \"typescript\": \"^5.0.0\"\n");
        sb.append("  }\n");
        sb.append("}\n");
        return new GeneratedCode("SDK_TS_PACKAGE", "package.json", "sdk/typescript/package.json", sb.toString());
    }

    private GeneratedCode generateTsClient(AppGenerateConfig config, String className, List<AppExposedApi> apis) {
        StringBuilder sb = new StringBuilder();
        sb.append("import axios, { AxiosInstance, AxiosResponse } from 'axios';\n\n");
        sb.append("/**\n");
        sb.append(" * API 响应接口\n");
        sb.append(" */\n");
        sb.append("export interface ApiResponse<T = any> {\n");
        sb.append("  code: number;\n");
        sb.append("  message: string;\n");
        sb.append("  data: T;\n");
        sb.append("}\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(config.getAppName()).append(" SDK 配置\n");
        sb.append(" */\n");
        sb.append("export class ").append(className).append(" {\n\n");
        sb.append("  private client: AxiosInstance;\n\n");
        sb.append("  constructor(baseUrl: string, apiKey: string) {\n");
        sb.append("    this.client = axios.create({\n");
        sb.append("      baseURL: baseUrl,\n");
        sb.append("      timeout: 30000,\n");
        sb.append("      headers: {\n");
        sb.append("        'Authorization': 'Bearer ' + apiKey,\n");
        sb.append("        'Content-Type': 'application/json'\n");
        sb.append("      }\n");
        sb.append("    });\n");
        sb.append("  }\n\n");

        for (AppExposedApi api : apis) {
            String methodName = toMethodName(api.getApiCode());
            sb.append("  /**\n");
            sb.append("   * ").append(api.getApiName()).append("\n");
            sb.append("   * ").append(api.getDescription() != null ? api.getDescription() : "").append("\n");
            sb.append("   */\n");
            sb.append("  async ").append(methodName).append("(");
            if ("POST".equals(api.getHttpMethod()) || "PUT".equals(api.getHttpMethod())) {
                sb.append("data: any");
            } else if (api.getApiPath().contains("{id}")) {
                sb.append("id: number | string");
            }
            sb.append("): Promise<ApiResponse> {\n");
            String path = api.getApiPath();
            if (path.contains("{id}")) {
                path = path.replace("{id}", "${id}");
                if ("GET".equals(api.getHttpMethod())) {
                    sb.append("    const response = await this.client.get<ApiResponse>(`").append(path).append("`);\n");
                } else if ("DELETE".equals(api.getHttpMethod())) {
                    sb.append("    const response = await this.client.delete<ApiResponse>(`").append(path).append("`);\n");
                }
            } else if ("GET".equals(api.getHttpMethod())) {
                sb.append("    const response = await this.client.get<ApiResponse>('").append(path).append("');\n");
            } else if ("POST".equals(api.getHttpMethod())) {
                sb.append("    const response = await this.client.post<ApiResponse>('").append(path).append("', data);\n");
            } else if ("PUT".equals(api.getHttpMethod())) {
                sb.append("    const response = await this.client.put<ApiResponse>('").append(path).append("', data);\n");
            }
            sb.append("    return response.data;\n");
            sb.append("  }\n\n");
        }

        sb.append("}\n\n");
        sb.append("export default ").append(className).append(";\n");
        return new GeneratedCode("SDK_TS_CLIENT", "index.ts", "sdk/typescript/index.ts", sb.toString());
    }

    private List<GeneratedCode> generatePythonSdk(AppGenerateConfig config, List<AppExposedApi> apis) {
        List<GeneratedCode> codes = new ArrayList<>();
        String className = toCamelCase(config.getAppCode()) + "Client";

        codes.add(generatePythonSetup(config));
        codes.add(generatePythonClient(config, className, apis));
        codes.add(generateSdkReadme(config, "Python"));

        return codes;
    }

    private GeneratedCode generatePythonSetup(AppGenerateConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("from setuptools import setup, find_packages\n\n");
        sb.append("setup(\n");
        sb.append("    name='").append(config.getAppCode()).append("_sdk',\n");
        sb.append("    version='").append(config.getVersion()).append("',\n");
        sb.append("    description='").append(config.getAppName()).append(" SDK',\n");
        sb.append("    packages=find_packages(),\n");
        sb.append("    install_requires=[\n");
        sb.append("        'requests>=2.28.0',\n");
        sb.append("    ],\n");
        sb.append("    python_requires='>=3.7',\n");
        sb.append(")\n");
        return new GeneratedCode("SDK_PY_SETUP", "setup.py", "sdk/python/setup.py", sb.toString());
    }

    private GeneratedCode generatePythonClient(AppGenerateConfig config, String className, List<AppExposedApi> apis) {
        StringBuilder sb = new StringBuilder();
        sb.append("import requests\n");
        sb.append("import json\n\n");
        sb.append("class ").append(className).append(":\n");
        sb.append("    \"\"\"").append(config.getAppName()).append(" SDK 客户端\"\"\"\n\n");
        sb.append("    def __init__(self, base_url, api_key):\n");
        sb.append("        self.base_url = base_url.rstrip('/')\n");
        sb.append("        self.api_key = api_key\n");
        sb.append("        self.headers = {\n");
        sb.append("            'Authorization': f'Bearer {api_key}',\n");
        sb.append("            'Content-Type': 'application/json'\n");
        sb.append("        }\n\n");
        sb.append("        self.session = requests.Session()\n");
        sb.append("        self.session.headers.update(self.headers)\n\n");

        for (AppExposedApi api : apis) {
            String methodName = toSnakeCase(api.getApiCode());
            sb.append("    def ").append(methodName).append("(");
            if ("POST".equals(api.getHttpMethod()) || "PUT".equals(api.getHttpMethod())) {
                sb.append("self, data");
            } else if (api.getApiPath().contains("{id}")) {
                sb.append("self, item_id");
            } else {
                sb.append("self");
            }
            sb.append("):\n");
            sb.append("        \"\"\"").append(api.getApiName()).append("\n\n");
            sb.append("        ").append(api.getDescription() != null ? api.getDescription() : "").append("\n");
            sb.append("        \"\"\"\n");
            String path = api.getApiPath();
            if (path.contains("{id}")) {
                path = path.replace("{id}", "{item_id}");
                if ("GET".equals(api.getHttpMethod())) {
                    sb.append("        url = f'{self.base_url}").append(path).append("'\n");
                    sb.append("        response = self.session.get(url)\n");
                } else if ("DELETE".equals(api.getHttpMethod())) {
                    sb.append("        url = f'{self.base_url}").append(path).append("'\n");
                    sb.append("        response = self.session.delete(url)\n");
                }
            } else if ("GET".equals(api.getHttpMethod())) {
                sb.append("        url = f'{self.base_url}").append(path).append("'\n");
                sb.append("        response = self.session.get(url)\n");
            } else if ("POST".equals(api.getHttpMethod())) {
                sb.append("        url = f'{self.base_url}").append(path).append("'\n");
                sb.append("        response = self.session.post(url, json=data)\n");
            } else if ("PUT".equals(api.getHttpMethod())) {
                sb.append("        url = f'{self.base_url}").append(path).append("'\n");
                sb.append("        response = self.session.put(url, json=data)\n");
            }
            sb.append("        response.raise_for_status()\n");
            sb.append("        return response.json()\n\n");
        }

        return new GeneratedCode("SDK_PY_CLIENT", "__init__.py", "sdk/python/" + config.getAppCode() + "_sdk/__init__.py", sb.toString());
    }

    private GeneratedCode generateSdkReadme(AppGenerateConfig config, String language) {
        List<AppExposedApi> apis = getDefaultApis(config);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(config.getAppName()).append(" SDK (").append(language).append(")\n\n");
        sb.append("## 简介\n\n");
        sb.append("本 SDK 提供了 ").append(config.getAppName()).append(" 应用的 ").append(language).append(" 语言 SDK，方便外部系统调用应用 API。\n\n");
        sb.append("## 安装\n\n");
        if ("Java".equals(language)) {
            sb.append("```xml\n");
            sb.append("<dependency>\n");
            sb.append("    <groupId>").append(config.getBasePackage() != null ? config.getBasePackage() : "com.lowcode").append("</groupId>\n");
            sb.append("    <artifactId>").append(config.getAppCode()).append("-sdk</artifactId>\n");
            sb.append("    <version>").append(config.getVersion()).append("</version>\n");
            sb.append("</dependency>\n");
            sb.append("```\n\n");
        } else if ("JavaScript".equals(language) || "TypeScript".equals(language)) {
            sb.append("```bash\n");
            sb.append("npm install ").append(config.getAppCode()).append("-sdk\n");
            sb.append("```\n\n");
        } else if ("Python".equals(language)) {
            sb.append("```bash\n");
            sb.append("pip install ").append(config.getAppCode()).append("-sdk\n");
            sb.append("```\n\n");
        }
        sb.append("## 使用示例\n\n");
        String className = toCamelCase(config.getAppCode()) + "Client";
        if ("Java".equals(language)) {
            sb.append("```java\n");
            sb.append(className).append(" client = new ").append(className).append("(\n");
            sb.append("    \"https://api.example.com\",\n");
            sb.append("    \"your-api-key\"\n");
            sb.append(");\n\n");
            sb.append("// 调用API\n");
            sb.append("String result = client.list();\n");
            sb.append("System.out.println(result);\n");
            sb.append("```\n");
        } else if ("JavaScript".equals(language)) {
            sb.append("```javascript\n");
            sb.append("const ").append(className).append(" = require('").append(config.getAppCode()).append("-sdk');\n\n");
            sb.append("const client = new ").append(className).append("(\n");
            sb.append("  'https://api.example.com',\n");
            sb.append("  'your-api-key'\n");
            sb.append(");\n\n");
            sb.append("// 调用API\n");
            sb.append("client.list().then(result => {\n");
            sb.append("  console.log(result);\n");
            sb.append("});\n");
            sb.append("```\n");
        } else if ("TypeScript".equals(language)) {
            sb.append("```typescript\n");
            sb.append("import ").append(className).append(" from '").append(config.getAppCode()).append("-sdk';\n\n");
            sb.append("const client = new ").append(className).append("(\n");
            sb.append("  'https://api.example.com',\n");
            sb.append("  'your-api-key'\n");
            sb.append(");\n\n");
            sb.append("// 调用API\n");
            sb.append("const result = await client.list();\n");
            sb.append("console.log(result);\n");
            sb.append("```\n");
        } else if ("Python".equals(language)) {
            sb.append("```python\n");
            sb.append("from ").append(config.getAppCode()).append("_sdk import ").append(className).append("\n\n");
            sb.append("client = ").append(className).append("(\n");
            sb.append("    'https://api.example.com',\n");
            sb.append("    'your-api-key'\n");
            sb.append(")\n\n");
            sb.append("# 调用API\n");
            sb.append("result = client.list()\n");
            sb.append("print(result)\n");
            sb.append("```\n");
        }
        sb.append("\n## API 列表\n\n");
        for (AppExposedApi api : apis) {
            sb.append("- **").append(api.getApiName()).append("** (").append(api.getApiCode()).append(")\n");
            sb.append("  - 方法：").append(api.getHttpMethod()).append(" ").append(api.getApiPath()).append("\n");
            sb.append("  - 描述：").append(api.getDescription() != null ? api.getDescription() : "").append("\n\n");
        }
        return new GeneratedCode("SDK_README", "README.md", "sdk/" + language.toLowerCase() + "/README.md", sb.toString());
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : str.toCharArray()) {
            if (c == '-' || c == '_' || c == '.') {
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

    private String toMethodName(String apiCode) {
        if (apiCode == null || apiCode.isEmpty()) {
            return "execute";
        }
        String[] parts = apiCode.split("\\.");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        boolean first = true;
        for (char c : apiCode.toCharArray()) {
            if (c == '.' || c == '-' || c == '_') {
                nextUpper = true;
            } else {
                if (first) {
                    result.append(Character.toLowerCase(c));
                    first = false;
                } else if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    private String toSnakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '.' || c == '-') {
                sb.append('_');
            } else if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
