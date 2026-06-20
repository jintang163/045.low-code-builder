package com.lowcode.deploy.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.entity.AppService;
import com.lowcode.deploy.entity.CloudConfig;
import com.lowcode.deploy.entity.DeploySpec;
import com.lowcode.deploy.entity.DeployStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@Service
public class DockerBuildService {

    @Autowired(required = false)
    private DeployProgressTracker progressTracker;

    @Value("${deploy.docker.host:http://127.0.0.1:2375}")
    private String dockerHost;

    @Value("${deploy.build.work-dir:${java.io.tmpdir}/lowcode-build}")
    private String buildWorkDir;

    @Value("${deploy.build.maven-home:}")
    private String mavenHome;

    private static final DateTimeFormatter TAG_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    public String buildAndPushImage(AppService app, DeploySpec spec, CloudConfig cloudConfig,
                                    String taskId, Consumer<String> logConsumer) {
        log.info("[{}] 任务开始：构建并推送Docker镜像，服务名: {}", taskId, app.getServiceName());
        validateParams(app, cloudConfig, taskId);

        Path workDir = null;
        try {
            String imageTag = generateImageTag(app);
            String imageName = resolveImageName(app, cloudConfig, imageTag);

            updateProgress(taskId, DeployStatus.BUILDING, 5, "开始准备构建环境");
            logConsumer.accept("[INFO] 开始准备构建环境...");

            workDir = createWorkDir(taskId);
            log.info("[{}] 构建工作目录: {}", taskId, workDir);

            updateProgress(taskId, DeployStatus.BUILDING, 10, "准备构建上下文");
            logConsumer.accept("[INFO] 准备构建上下文...");
            prepareBuildContext(app, workDir, taskId, logConsumer);

            if (needMavenBuild(app)) {
                updateProgress(taskId, DeployStatus.BUILDING, 15, "执行Maven构建");
                logConsumer.accept("[INFO] 开始执行Maven构建...");
                runMavenBuild(app, taskId, logConsumer);
                copyJarToBuildContext(app, workDir, taskId, logConsumer);
            } else {
                updateProgress(taskId, DeployStatus.BUILDING, 20, "使用现有JAR包");
                logConsumer.accept("[INFO] 跳过Maven构建，使用现有JAR包...");
                copyJarToBuildContext(app, workDir, taskId, logConsumer);
            }

            updateProgress(taskId, DeployStatus.BUILDING, 25, "开始构建Docker镜像");
            logConsumer.accept("[INFO] 开始构建Docker镜像...");
            String localImageId = buildDockerImage(workDir, imageName, taskId, logConsumer);
            log.info("[{}] Docker镜像构建成功，镜像ID: {}", taskId, localImageId);

            updateProgress(taskId, DeployStatus.BUILDING, 35, "Docker镜像构建完成");
            logConsumer.accept("[INFO] Docker镜像构建完成");

            updateProgress(taskId, DeployStatus.PUSHING, 40, "开始推送镜像到仓库");
            logConsumer.accept("[INFO] 开始推送镜像到仓库: " + cloudConfig.getRegistryUrl());
            pushImage(imageName, cloudConfig, taskId, logConsumer);

            updateProgress(taskId, DeployStatus.PUSHING, 65, "镜像推送成功");
            logConsumer.accept("[INFO] 镜像推送成功: " + imageName);
            log.info("[{}] 任务完成：镜像构建推送成功，最终镜像: {}", taskId, imageName);

            return imageName;

        } catch (BusinessException e) {
            log.error("[{}] 构建推送镜像失败: {}", taskId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[{}] 构建推送镜像发生未知错误", taskId, e);
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "构建推送镜像失败: " + e.getMessage());
        } finally {
            cleanupWorkDir(workDir, taskId);
        }
    }

    private void validateParams(AppService app, CloudConfig cloudConfig, String taskId) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "应用服务配置不能为空");
        }
        if (!StringUtils.hasText(app.getServiceName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务名不能为空");
        }
        if (!StringUtils.hasText(app.getDockerfilePath())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Dockerfile路径不能为空");
        }
        if (cloudConfig == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "云平台配置不能为空");
        }
        if (!StringUtils.hasText(cloudConfig.getRegistryUrl())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "镜像仓库地址不能为空");
        }
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
    }

    private String resolveImageName(AppService app, CloudConfig cloudConfig, String tag) {
        String registry = removeProtocol(cloudConfig.getRegistryUrl());
        String namespace = StringUtils.hasText(cloudConfig.getRegistryNamespace())
                ? cloudConfig.getRegistryNamespace() : "";
        String image = StringUtils.hasText(app.getImageName())
                ? app.getImageName() : app.getServiceName();

        StringBuilder sb = new StringBuilder();
        sb.append(registry);
        if (StringUtils.hasText(namespace)) {
            sb.append("/").append(namespace);
        }
        sb.append("/").append(image);
        sb.append(":").append(tag);
        return sb.toString();
    }

    private String removeProtocol(String url) {
        if (url == null) return "";
        return url.replaceAll("^https?://", "");
    }

    private Path createWorkDir(String taskId) {
        try {
            Path workDir = Paths.get(buildWorkDir, taskId);
            Files.createDirectories(workDir);
            return workDir;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "创建工作目录失败: " + e.getMessage());
        }
    }

    private boolean needMavenBuild(AppService app) {
        if (!StringUtils.hasText(app.getModulePath())) {
            return false;
        }
        String jarPath = resolveJarPath(app);
        return !Files.exists(Paths.get(jarPath));
    }

    private void prepareBuildContext(AppService app, Path workDir, String taskId, Consumer<String> logConsumer) {
        try {
            Path dockerfileSource = Paths.get(app.getDockerfilePath());
            if (!Files.exists(dockerfileSource)) {
                throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR,
                        "Dockerfile不存在: " + dockerfileSource.toAbsolutePath());
            }

            Path dockerfileTarget = workDir.resolve("Dockerfile");
            Files.copy(dockerfileSource, dockerfileTarget, StandardCopyOption.REPLACE_EXISTING);
            log.info("[{}] Dockerfile已复制到: {}", taskId, dockerfileTarget);
            logConsumer.accept("[INFO] Dockerfile已就绪");

            modifyDockerfileForBuildContext(dockerfileTarget);

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] 准备构建上下文失败", taskId, e);
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "准备构建上下文失败: " + e.getMessage());
        }
    }

    private void modifyDockerfileForBuildContext(Path dockerfile) throws IOException {
        List<String> lines = Files.readAllLines(dockerfile);
        List<String> modified = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("COPY") || trimmed.startsWith("ADD")) {
                String[] parts = line.split("\\s+");
                boolean modifiedLine = false;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].endsWith(".jar")) {
                        parts[i] = "app.jar";
                        modifiedLine = true;
                        break;
                    }
                }
                if (modifiedLine) {
                    modified.add(String.join(" ", parts));
                    continue;
                }
            }
            modified.add(line);
        }
        Files.write(dockerfile, modified);
    }

    private void runMavenBuild(AppService app, String taskId, Consumer<String> logConsumer) {
        String modulePath = app.getModulePath();
        Path moduleDir = Paths.get(modulePath).toAbsolutePath();

        if (!Files.exists(moduleDir) || !Files.isDirectory(moduleDir)) {
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            moduleDir = projectRoot.resolve(modulePath).toAbsolutePath();
        }

        if (!Files.exists(moduleDir.resolve("pom.xml"))) {
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR,
                    "Maven模块不存在pom.xml: " + moduleDir);
        }

        log.info("[{}] 开始Maven构建，模块目录: {}", taskId, moduleDir);

        try {
            List<String> command = new ArrayList<>();
            String mvnCmd = getMavenCommand();
            command.add(mvnCmd);
            command.add("clean");
            command.add("package");
            command.add("-DskipTests");
            command.add("-q");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(moduleDir.toFile());
            pb.redirectErrorStream(true);

            log.info("[{}] 执行Maven命令: {}", taskId, String.join(" ", command));

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (logConsumer != null) {
                        logConsumer.accept("[MVN] " + line);
                    }
                    log.debug("[{}] Maven输出: {}", taskId, line);
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "Maven构建超时(30分钟)");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR,
                        "Maven构建失败，退出码: " + exitCode);
            }

            log.info("[{}] Maven构建成功", taskId);
            logConsumer.accept("[INFO] Maven构建成功");

        } catch (BusinessException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            log.error("[{}] Maven构建异常", taskId, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "Maven构建失败: " + e.getMessage());
        }
    }

    private String getMavenCommand() {
        if (StringUtils.hasText(mavenHome)) {
            Path mvn = Paths.get(mavenHome, "bin", "mvn");
            if (Files.exists(mvn)) {
                return mvn.toAbsolutePath().toString();
            }
            Path mvnCmd = Paths.get(mavenHome, "bin", "mvn.cmd");
            if (Files.exists(mvnCmd)) {
                return mvnCmd.toAbsolutePath().toString();
            }
        }
        return isWindows() ? "mvn.cmd" : "mvn";
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String resolveJarPath(AppService app) {
        String jarPath = app.getJarPath();
        if (StringUtils.hasText(jarPath)) {
            Path p = Paths.get(jarPath);
            if (p.isAbsolute()) {
                return p.toString();
            }
            if (StringUtils.hasText(app.getModulePath())) {
                return Paths.get(app.getModulePath(), jarPath).toString();
            }
            return p.toString();
        }
        if (StringUtils.hasText(app.getModulePath())) {
            Path target = Paths.get(app.getModulePath(), "target");
            try (Stream<Path> files = Files.list(target)) {
                Optional<Path> jar = files
                        .filter(f -> f.toString().endsWith(".jar"))
                        .filter(f -> !f.toString().endsWith("-sources.jar"))
                        .filter(f -> !f.toString().endsWith("-javadoc.jar"))
                        .findFirst();
                if (jar.isPresent()) {
                    return jar.get().toString();
                }
            } catch (IOException ignored) {
            }
        }
        return "";
    }

    private void copyJarToBuildContext(AppService app, Path workDir, String taskId, Consumer<String> logConsumer) {
        try {
            String jarPathStr = resolveJarPath(app);
            if (!StringUtils.hasText(jarPathStr)) {
                throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "未找到JAR包路径");
            }

            Path jarPath = Paths.get(jarPathStr);
            if (!jarPath.isAbsolute()) {
                Path projectRoot = Paths.get(System.getProperty("user.dir"));
                Path resolved = projectRoot.resolve(jarPath);
                if (Files.exists(resolved)) {
                    jarPath = resolved;
                }
            }

            if (!Files.exists(jarPath)) {
                throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "JAR包不存在: " + jarPath);
            }

            Path targetJar = workDir.resolve("app.jar");
            Files.copy(jarPath, targetJar, StandardCopyOption.REPLACE_EXISTING);
            log.info("[{}] JAR包已复制: {} -> {}", taskId, jarPath, targetJar);
            logConsumer.accept("[INFO] JAR包已就绪: " + jarPath.getFileName());

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] 复制JAR包失败", taskId, e);
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "复制JAR包失败: " + e.getMessage());
        }
    }

    private String buildDockerImage(Path buildContextDir, String fullImageName,
                                    String taskId, Consumer<String> logConsumer) {
        DockerClient dockerClient = createDockerClient(taskId);
        try {
            Map<String, String> buildArgs = new HashMap<>();
            buildArgs.put("APP_NAME", fullImageName);

            BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    String stream = item.getStream();
                    if (stream != null && logConsumer != null) {
                        String trimmed = stream.trim();
                        if (!trimmed.isEmpty()) {
                            logConsumer.accept("[BUILD] " + trimmed);
                        }
                    }
                    if (item.getErrorDetail() != null) {
                        log.error("[{}] Docker构建错误: {}", taskId, item.getErrorDetail().getMessage());
                        if (logConsumer != null) {
                            logConsumer.accept("[ERROR] " + item.getErrorDetail().getMessage());
                        }
                    }
                    super.onNext(item);
                }
            };

            String imageId = dockerClient.buildImageCmd(buildContextDir.toFile())
                    .withTags(new HashSet<>(Collections.singletonList(fullImageName)))
                    .withBuildArgs(buildArgs)
                    .withNoCache(false)
                    .withPull(false)
                    .exec(callback)
                    .awaitImageId(30, TimeUnit.MINUTES);

            log.info("[{}] Docker镜像构建成功，Image ID: {}, Tag: {}", taskId, imageId, fullImageName);
            return imageId;

        } catch (com.github.dockerjava.api.exception.DockerException e) {
            log.error("[{}] Docker构建异常", taskId, e);
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "Docker构建失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "Docker构建被中断");
        } finally {
            closeDockerClient(dockerClient);
        }
    }

    private void pushImage(String fullImageName, CloudConfig cloudConfig,
                           String taskId, Consumer<String> logConsumer) {
        DockerClient dockerClient = createDockerClient(taskId);
        try {
            AuthConfig authConfig = new AuthConfig()
                    .withRegistryAddress(cloudConfig.getRegistryUrl())
                    .withUsername(cloudConfig.getRegistryUsername())
                    .withPassword(cloudConfig.getRegistryPassword());

            PushImageCmd pushCmd = dockerClient.pushImageCmd(fullImageName)
                    .withAuthConfig(authConfig);

            ResultCallback.Adapter<PushResponseItem> callback = new ResultCallback.Adapter<>() {
                @Override
                public void onNext(PushResponseItem item) {
                    String status = item.getStatus();
                    if (status != null && logConsumer != null) {
                        StringBuilder sb = new StringBuilder("[PUSH] ");
                        sb.append(status);
                        if (item.getProgress() != null) {
                            sb.append(" ").append(item.getProgress());
                        }
                        String id = item.getId();
                        if (id != null) {
                            sb.append(" [").append(id).append("]");
                        }
                        logConsumer.accept(sb.toString());
                    }
                    if (item.getErrorDetail() != null) {
                        log.error("[{}] Docker推送错误: {}", taskId, item.getErrorDetail().getMessage());
                        if (logConsumer != null) {
                            logConsumer.accept("[ERROR] " + item.getErrorDetail().getMessage());
                        }
                    }
                    super.onNext(item);
                }
            };

            pushCmd.exec(callback).awaitCompletion(60, TimeUnit.MINUTES);
            log.info("[{}] Docker镜像推送成功: {}", taskId, fullImageName);

        } catch (com.github.dockerjava.api.exception.DockerException e) {
            log.error("[{}] Docker推送异常", taskId, e);
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "Docker推送失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "Docker推送被中断");
        } finally {
            closeDockerClient(dockerClient);
        }
    }

    private DockerClient createDockerClient(String taskId) {
        try {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();

            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(300))
                    .build();

            DockerClient client = DockerClientBuilder.getInstance(config)
                    .withDockerHttpClient(httpClient)
                    .build();

            log.info("[{}] DockerClient已创建，连接: {}", taskId, dockerHost);
            return client;

        } catch (Exception e) {
            log.error("[{}] 创建DockerClient失败", taskId, e);
            throw new BusinessException(ErrorCode.CODE_DEPLOY_ERROR, "创建Docker客户端失败: " + e.getMessage());
        }
    }

    private void closeDockerClient(DockerClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                log.warn("关闭DockerClient异常: {}", e.getMessage());
            }
        }
    }

    public String generateImageTag(AppService app) {
        String timestamp = LocalDateTime.now().format(TAG_FORMATTER);
        String random = generateRandomString(6);
        return timestamp + "-" + random;
    }

    private String generateRandomString(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }

    private void updateProgress(String taskId, DeployStatus status, int progress, String message) {
        if (progressTracker != null) {
            progressTracker.updateProgress(taskId, progress, status.getName(), status, "INFO", message);
        }
    }

    private void cleanupWorkDir(Path workDir, String taskId) {
        if (workDir == null) return;
        try {
            if (Files.exists(workDir)) {
                Files.walk(workDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("[{}] 构建工作目录已清理: {}", taskId, workDir);
            }
        } catch (IOException e) {
            log.warn("[{}] 清理工作目录失败: {}", taskId, e.getMessage());
        }
    }
}
