package com.lowcode.page.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.dto.CustomComponentUploadDTO;
import com.lowcode.page.dto.CustomComponentVersionUpdateDTO;
import com.lowcode.page.entity.CustomComponent;
import com.lowcode.page.entity.CustomComponentVersion;
import com.lowcode.page.mapper.CustomComponentMapper;
import com.lowcode.page.vo.CustomComponentVO;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class CustomComponentService extends ServiceImpl<CustomComponentMapper, CustomComponent> {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private CustomComponentVersionService versionService;

    @Value("${oss.minio.bucketName:lowcode}")
    private String bucketName;

    @Value("${oss.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String COMPONENT_PATH = "custom-components";

    @Transactional(rollbackFor = Exception.class)
    public CustomComponentVO uploadComponent(CustomComponentUploadDTO dto) throws Exception {
        LambdaQueryWrapper<CustomComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomComponent::getComponentType, dto.getComponentType());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("组件类型已存在");
        }

        validatePackage(dto.getFile());

        Map<String, Object> packageInfo = parsePackage(dto.getFile());

        String packagePath = uploadToMinio(dto.getFile(), dto.getComponentType(), dto.getVersion());

        CustomComponent component = new CustomComponent();
        component.setComponentType(dto.getComponentType());
        component.setComponentName(dto.getComponentName());
        component.setComponentCategory(dto.getComponentCategory());
        component.setIcon(dto.getIcon() != null ? dto.getIcon() : "📦");
        component.setDescription(dto.getDescription());
        component.setAuthor(dto.getAuthor());
        component.setCurrentVersion(dto.getVersion());
        component.setIsSystem(0);
        component.setStatus(1);
        save(component);

        CustomComponentVersion version = new CustomComponentVersion();
        version.setComponentId(component.getId());
        version.setVersion(dto.getVersion());
        version.setChangeLog(dto.getChangeLog());
        version.setPackagePath(packagePath);
        version.setPackageSize(dto.getFile().getSize());
        version.setMainFile((String) packageInfo.get("mainFile"));
        version.setPropSchema((String) packageInfo.get("propSchema"));
        version.setEventSchema((String) packageInfo.get("eventSchema"));
        version.setExposedEvents((String) packageInfo.get("exposedEvents"));
        version.setDefaultProps((String) packageInfo.get("defaultProps"));
        version.setDefaultStyle((String) packageInfo.get("defaultStyle"));
        version.setIsDeprecated(0);
        version.setStatus(1);
        versionService.save(version);

        component.setCurrentVersionInfo(version);
        return convertToVO(component);
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomComponentVO updateVersion(CustomComponentVersionUpdateDTO dto) throws Exception {
        CustomComponent component = getById(dto.getComponentId());
        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }

        LambdaQueryWrapper<CustomComponentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(CustomComponentVersion::getComponentId, dto.getComponentId());
        versionWrapper.eq(CustomComponentVersion::getVersion, dto.getVersion());
        Long versionCount = versionService.count(versionWrapper);
        if (versionCount > 0) {
            throw new BusinessException("版本号已存在");
        }

        validatePackage(dto.getFile());
        Map<String, Object> packageInfo = parsePackage(dto.getFile());

        String packagePath = uploadToMinio(dto.getFile(), component.getComponentType(), dto.getVersion());

        LambdaQueryWrapper<CustomComponentVersion> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(CustomComponentVersion::getComponentId, dto.getComponentId());
        updateWrapper.eq(CustomComponentVersion::getStatus, 1);
        CustomComponentVersion oldVersion = versionService.getOne(updateWrapper);
        if (oldVersion != null) {
            oldVersion.setStatus(0);
            versionService.updateById(oldVersion);
        }

        CustomComponentVersion newVersion = new CustomComponentVersion();
        newVersion.setComponentId(component.getId());
        newVersion.setVersion(dto.getVersion());
        newVersion.setChangeLog(dto.getChangeLog());
        newVersion.setPackagePath(packagePath);
        newVersion.setPackageSize(dto.getFile().getSize());
        newVersion.setMainFile((String) packageInfo.get("mainFile"));
        newVersion.setPropSchema((String) packageInfo.get("propSchema"));
        newVersion.setEventSchema((String) packageInfo.get("eventSchema"));
        newVersion.setExposedEvents((String) packageInfo.get("exposedEvents"));
        newVersion.setDefaultProps((String) packageInfo.get("defaultProps"));
        newVersion.setDefaultStyle((String) packageInfo.get("defaultStyle"));
        newVersion.setIsDeprecated(0);
        newVersion.setStatus(1);
        versionService.save(newVersion);

        component.setCurrentVersion(dto.getVersion());
        component.setCurrentVersionInfo(newVersion);
        updateById(component);

        return convertToVO(component);
    }

    public Page<CustomComponentVO> page(Integer current, Integer size, String category, String keyword) {
        LambdaQueryWrapper<CustomComponent> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(CustomComponent::getComponentCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(CustomComponent::getComponentName, keyword)
                    .or().like(CustomComponent::getComponentType, keyword)
                    .or().like(CustomComponent::getDescription, keyword));
        }
        wrapper.eq(CustomComponent::getStatus, 1);
        wrapper.orderByDesc(CustomComponent::getCreatedTime);

        Page<CustomComponent> page = page(new Page<>(current, size), wrapper);
        Page<CustomComponentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    public Map<String, List<CustomComponentVO>> getComponentTree() {
        LambdaQueryWrapper<CustomComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomComponent::getStatus, 1);
        wrapper.orderByAsc(CustomComponent::getComponentCategory, CustomComponent::getId);
        List<CustomComponent> components = list(wrapper);

        return components.stream()
                .map(this::convertToVO)
                .collect(Collectors.groupingBy(
                        CustomComponentVO::getComponentCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public CustomComponentVO getDetail(Long id) {
        CustomComponent component = getById(id);
        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }

        LambdaQueryWrapper<CustomComponentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(CustomComponentVersion::getComponentId, id);
        versionWrapper.orderByDesc(CustomComponentVersion::getCreatedTime);
        List<CustomComponentVersion> versions = versionService.list(versionWrapper);

        component.setVersions(versions);
        component.setCurrentVersionInfo(versions.stream()
                .filter(v -> v.getStatus() == 1)
                .findFirst()
                .orElse(null));

        return convertToVO(component);
    }

    public CustomComponentVO getByType(String componentType, String version) {
        LambdaQueryWrapper<CustomComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomComponent::getComponentType, componentType);
        wrapper.eq(CustomComponent::getStatus, 1);
        CustomComponent component = getOne(wrapper);
        if (component == null) {
            return null;
        }

        LambdaQueryWrapper<CustomComponentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(CustomComponentVersion::getComponentId, component.getId());
        if (version != null && !version.isEmpty()) {
            versionWrapper.eq(CustomComponentVersion::getVersion, version);
        } else {
            versionWrapper.eq(CustomComponentVersion::getStatus, 1);
        }
        CustomComponentVersion componentVersion = versionService.getOne(versionWrapper);
        if (componentVersion == null) {
            return null;
        }
        component.setCurrentVersionInfo(componentVersion);
        component.setCurrentVersion(componentVersion.getVersion());

        return convertToVO(component);
    }

    public CustomComponentVO getByType(String componentType) {
        return getByType(componentType, null);
    }

    public String getComponentBundleUrl(Long componentId, String version) throws Exception {
        LambdaQueryWrapper<CustomComponentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomComponentVersion::getComponentId, componentId);
        if (version != null && !version.isEmpty()) {
            wrapper.eq(CustomComponentVersion::getVersion, version);
        } else {
            wrapper.eq(CustomComponentVersion::getStatus, 1);
        }
        CustomComponentVersion componentVersion = versionService.getOne(wrapper);
        if (componentVersion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件版本不存在");
        }

        return getPresignedUrl(componentVersion.getPackagePath());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteComponent(Long id) {
        CustomComponent component = getById(id);
        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }
        if (component.getIsSystem() == 1) {
            throw new BusinessException("系统组件不能删除");
        }

        component.setStatus(0);
        updateById(component);

        LambdaQueryWrapper<CustomComponentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomComponentVersion::getComponentId, id);
        List<CustomComponentVersion> versions = versionService.list(wrapper);
        versions.forEach(v -> {
            v.setStatus(0);
            versionService.updateById(v);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void deprecateVersion(Long versionId) {
        CustomComponentVersion version = versionService.getById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        version.setIsDeprecated(1);
        versionService.updateById(version);
    }

    private void validatePackage(MultipartFile file) throws BusinessException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传组件包");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".zip")) {
            throw new BusinessException("组件包必须是zip格式");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new BusinessException("组件包大小不能超过50MB");
        }
    }

    private Map<String, Object> parsePackage(MultipartFile file) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("mainFile", "index.js");
        result.put("propSchema", "{}");
        result.put("eventSchema", "{}");
        result.put("exposedEvents", "[]");
        result.put("defaultProps", "{}");
        result.put("defaultStyle", "{}");

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith("component.json")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String jsonContent = baos.toString(StandardCharsets.UTF_8.name());
                    JsonNode rootNode = objectMapper.readTree(jsonContent);

                    if (rootNode.has("main")) {
                        result.put("mainFile", rootNode.get("main").asText("index.js"));
                    }
                    if (rootNode.has("propSchema")) {
                        result.put("propSchema", objectMapper.writeValueAsString(rootNode.get("propSchema")));
                    }
                    if (rootNode.has("eventSchema")) {
                        result.put("eventSchema", objectMapper.writeValueAsString(rootNode.get("eventSchema")));
                    }
                    if (rootNode.has("exposedEvents")) {
                        result.put("exposedEvents", objectMapper.writeValueAsString(rootNode.get("exposedEvents")));
                    }
                    if (rootNode.has("defaultProps")) {
                        result.put("defaultProps", objectMapper.writeValueAsString(rootNode.get("defaultProps")));
                    }
                    if (rootNode.has("defaultStyle")) {
                        result.put("defaultStyle", objectMapper.writeValueAsString(rootNode.get("defaultStyle")));
                    }
                    break;
                }
                zis.closeEntry();
            }
        }

        return result;
    }

    private String uploadToMinio(MultipartFile file, String componentType, String version) throws Exception {
        String objectName = COMPONENT_PATH + "/" + componentType + "/" + version + "/" + file.getOriginalFilename();

        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType("application/zip")
                            .build()
            );
        }

        return objectName;
    }

    private String getPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(7, java.util.concurrent.TimeUnit.DAYS)
                        .build()
        );
    }

    private CustomComponentVO convertToVO(CustomComponent component) {
        CustomComponentVO vo = new CustomComponentVO();
        BeanUtils.copyProperties(component, vo);
        if (component.getCurrentVersionInfo() != null) {
            try {
                vo.setPackageUrl(getPresignedUrl(component.getCurrentVersionInfo().getPackagePath()));
            } catch (Exception e) {
                log.warn("获取组件包URL失败", e);
            }
        }
        return vo;
    }

    public InputStream downloadComponent(String componentType, String version) throws Exception {
        CustomComponentVO component = getByType(componentType);
        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }

        LambdaQueryWrapper<CustomComponentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomComponentVersion::getComponentId, component.getId());
        if (version != null && !version.isEmpty()) {
            wrapper.eq(CustomComponentVersion::getVersion, version);
        } else {
            wrapper.eq(CustomComponentVersion::getStatus, 1);
        }
        CustomComponentVersion componentVersion = versionService.getOne(wrapper);
        if (componentVersion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件版本不存在");
        }

        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(componentVersion.getPackagePath())
                        .build()
        );
    }
}
