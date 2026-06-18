package com.lowcode.generator.service;

import cn.hutool.core.util.StrUtil;
import com.lowcode.common.enums.FieldTypeEnum;
import com.lowcode.generator.config.GeneratorConfig;
import com.lowcode.generator.entity.GeneratedCode;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.ModelField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CodeGeneratorService {

    public List<GeneratedCode> generateAll(DataModel model, GeneratorConfig config) {
        List<GeneratedCode> codes = new ArrayList<>();

        if (config.getGenerateEntity()) {
            codes.add(generateEntity(model, config));
        }
        if (config.getGenerateMapper()) {
            codes.add(generateMapper(model, config));
            codes.add(generateMapperXml(model, config));
        }
        if (config.getGenerateService()) {
            codes.add(generateService(model, config));
            codes.add(generateServiceImpl(model, config));
        }
        if (config.getGenerateController()) {
            codes.add(generateController(model, config));
        }
        if (config.getGenerateVo()) {
            codes.add(generateVO(model, config));
        }
        if (config.getGenerateDto()) {
            codes.add(generateDTO(model, config));
        }
        if (config.getGenerateApiJs()) {
            codes.add(generateApiJs(model, config));
        }

        return codes;
    }

    public GeneratedCode generateEntity(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String packageName = buildPackage(config, config.getEntityPackage());
        String filePath = packageToPath(packageName) + "/" + entityName + ".java";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        sb.append("import com.baomidou.mybatisplus.annotation.*;\n");
        sb.append("import lombok.Data;\n");
        sb.append("import lombok.EqualsAndHashCode;\n");

        Set<String> imports = new HashSet<>();
        for (ModelField field : model.getFields()) {
            String javaType = field.getJavaType();
            if (javaType.contains("LocalDate")) {
                imports.add("import java.time.LocalDate;");
            } else if (javaType.contains("LocalDateTime")) {
                imports.add("import java.time.LocalDateTime;");
            } else if (javaType.contains("LocalTime")) {
                imports.add("import java.time.LocalTime;");
            } else if (javaType.contains("BigDecimal")) {
                imports.add("import java.math.BigDecimal;");
            }
        }

        for (String imp : imports) {
            sb.append(imp).append("\n");
        }

        sb.append("import java.io.Serializable;\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(model.getModelDesc() != null ? model.getModelDesc() : model.getModelName()).append("\n");
        sb.append(" * @author ").append(config.getAuthor()).append("\n");
        sb.append(" * @since ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append(" * @version ").append(config.getVersion()).append("\n");
        sb.append(" */\n");

        sb.append("@Data\n");
        sb.append("@EqualsAndHashCode(callSuper = false)\n");
        sb.append("@TableName(\"").append(model.getTableName()).append("\")\n");
        sb.append("public class ").append(entityName).append(" implements Serializable {\n\n");
        sb.append("    private static final long serialVersionUID = 1L;\n\n");

        for (ModelField field : model.getFields()) {
            sb.append("    /**\n");
            sb.append("     * ").append(field.getFieldComment() != null ? field.getFieldComment() : field.getFieldName()).append("\n");
            sb.append("     */\n");

            if (field.getIsPrimary() != null && field.getIsPrimary() == 1) {
                String strategy = model.getPrimaryKeyStrategy() != null ? model.getPrimaryKeyStrategy() : "AUTO";
                sb.append("    @TableId(value = \"").append(field.getColumnName()).append("\", type = IdType.").append(strategy).append(")\n");
            } else {
                if (field.getIsLogicDelete() != null && field.getIsLogicDelete() == 1) {
                    sb.append("    @TableLogic\n");
                }
                if (field.getIsVersion() != null && field.getIsVersion() == 1) {
                    sb.append("    @Version\n");
                }
                if (field.getIsAutoIncrement() == null || field.getIsAutoIncrement() == 0) {
                    sb.append("    @TableField(\"").append(field.getColumnName()).append("\")\n");
                }
            }

            String fieldType = field.getJavaType();
            String fieldName = StrUtil.toCamelCase(field.getFieldName());

            sb.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n\n");
        }

        sb.append("}\n");

        return new GeneratedCode("ENTITY", entityName + ".java", filePath, sb.toString());
    }

    public GeneratedCode generateMapper(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String mapperName = entityName + "Mapper";
        String packageName = buildPackage(config, config.getMapperPackage());
        String filePath = packageToPath(packageName) + "/" + mapperName + ".java";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        String entityPackage = buildPackage(config, config.getEntityPackage());
        sb.append("import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n");
        sb.append("import ").append(entityPackage).append(".").append(entityName).append(";\n");
        sb.append("import org.apache.ibatis.annotations.Mapper;\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(model.getModelName()).append(" Mapper接口\n");
        sb.append(" * @author ").append(config.getAuthor()).append("\n");
        sb.append(" * @since ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append(" */\n");

        sb.append("@Mapper\n");
        sb.append("public interface ").append(mapperName).append(" extends BaseMapper<").append(entityName).append("> {\n\n");
        sb.append("}\n");

        return new GeneratedCode("MAPPER", mapperName + ".java", filePath, sb.toString());
    }

    public GeneratedCode generateMapperXml(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String mapperName = entityName + "Mapper";
        String packageName = buildPackage(config, config.getMapperPackage()).replace(".", "/");
        String filePath = "mapper/" + mapperName + ".xml";

        String entityPackage = buildPackage(config, config.getEntityPackage());

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        sb.append("<mapper namespace=\"").append(packageName.replace("/", ".")).append(".").append(mapperName).append("\">\n\n");

        List<String> columns = model.getFields().stream()
                .map(f -> "`" + f.getColumnName() + "`")
                .collect(Collectors.toList());

        sb.append("    <resultMap id=\"BaseResultMap\" type=\"").append(entityPackage).append(".").append(entityName).append("\">\n");
        for (ModelField field : model.getFields()) {
            String fieldName = StrUtil.toCamelCase(field.getFieldName());
            if (field.getIsPrimary() != null && field.getIsPrimary() == 1) {
                sb.append("        <id column=\"").append(field.getColumnName()).append("\" property=\"").append(fieldName).append("\" />\n");
            } else {
                sb.append("        <result column=\"").append(field.getColumnName()).append("\" property=\"").append(fieldName).append("\" />\n");
            }
        }
        sb.append("    </resultMap>\n\n");

        sb.append("    <sql id=\"Base_Column_List\">\n");
        sb.append("        ").append(String.join(", ", columns)).append("\n");
        sb.append("    </sql>\n\n");

        sb.append("</mapper>\n");

        return new GeneratedCode("MAPPER_XML", mapperName + ".xml", filePath, sb.toString());
    }

    public GeneratedCode generateService(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String serviceName = "I" + entityName + "Service";
        String packageName = buildPackage(config, config.getServicePackage());
        String filePath = packageToPath(packageName) + "/" + serviceName + ".java";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        String entityPackage = buildPackage(config, config.getEntityPackage());
        sb.append("import com.baomidou.mybatisplus.extension.service.IService;\n");
        sb.append("import ").append(entityPackage).append(".").append(entityName).append(";\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(model.getModelName()).append(" Service接口\n");
        sb.append(" * @author ").append(config.getAuthor()).append("\n");
        sb.append(" * @since ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append(" */\n");

        sb.append("public interface ").append(serviceName).append(" extends IService<").append(entityName).append("> {\n\n");
        sb.append("}\n");

        return new GeneratedCode("SERVICE", serviceName + ".java", filePath, sb.toString());
    }

    public GeneratedCode generateServiceImpl(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String serviceName = "I" + entityName + "Service";
        String implName = entityName + "ServiceImpl";
        String packageName = buildPackage(config, config.getServicePackage()) + ".impl";
        String filePath = packageToPath(packageName) + "/" + implName + ".java";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        String entityPackage = buildPackage(config, config.getEntityPackage());
        String mapperPackage = buildPackage(config, config.getMapperPackage());
        String servicePackage = buildPackage(config, config.getServicePackage());

        sb.append("import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n");
        sb.append("import ").append(entityPackage).append(".").append(entityName).append(";\n");
        sb.append("import ").append(mapperPackage).append(".").append(entityName).append("Mapper;\n");
        sb.append("import ").append(servicePackage).append(".").append(serviceName).append(";\n");
        sb.append("import org.springframework.stereotype.Service;\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(model.getModelName()).append(" Service实现类\n");
        sb.append(" * @author ").append(config.getAuthor()).append("\n");
        sb.append(" * @since ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append(" */\n");

        sb.append("@Service\n");
        sb.append("public class ").append(implName).append(" extends ServiceImpl<").append(entityName).append("Mapper, ")
                .append(entityName).append("> implements ").append(serviceName).append(" {\n\n");
        sb.append("}\n");

        return new GeneratedCode("SERVICE_IMPL", implName + ".java", filePath, sb.toString());
    }

    public GeneratedCode generateController(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String controllerName = entityName + "Controller";
        String packageName = buildPackage(config, config.getControllerPackage());
        String filePath = packageToPath(packageName) + "/" + controllerName + ".java";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        String entityPackage = buildPackage(config, config.getEntityPackage());
        String servicePackage = buildPackage(config, config.getServicePackage());
        String voPackage = buildPackage(config, config.getVoPackage());
        String dtoPackage = buildPackage(config, config.getDtoPackage());

        sb.append("import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;\n");
        sb.append("import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n");
        sb.append("import ").append(entityPackage).append(".").append(entityName).append(";\n");
        sb.append("import ").append(servicePackage).append(".I").append(entityName).append("Service;\n");
        sb.append("import ").append(voPackage).append(".").append(entityName).append("VO;\n");
        sb.append("import ").append(dtoPackage).append(".").append(entityName).append("DTO;\n");
        sb.append("import com.lowcode.common.result.Result;\n");
        sb.append("import io.swagger.annotations.Api;\n");
        sb.append("import io.swagger.annotations.ApiOperation;\n");
        sb.append("import org.springframework.beans.BeanUtils;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.stream.Collectors;\n\n");

        String requestMapping = "/" + StrUtil.toSymbolCase(entityName, '-');

        sb.append("/**\n");
        sb.append(" * ").append(model.getModelName()).append(" Controller\n");
        sb.append(" * @author ").append(config.getAuthor()).append("\n");
        sb.append(" * @since ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append(" */\n");

        sb.append("@Api(tags = \"").append(model.getModelName()).append("管理\")\n");
        sb.append("@RestController\n");
        sb.append("@RequestMapping(\"").append(requestMapping).append("\")\n");
        sb.append("public class ").append(controllerName).append(" {\n\n");

        String serviceField = StrUtil.lowerFirst(entityName) + "Service";

        sb.append("    @Autowired\n");
        sb.append("    private I").append(entityName).append("Service ").append(serviceField).append(";\n\n");

        sb.append("    @ApiOperation(\"新增\")\n");
        sb.append("    @PostMapping\n");
        sb.append("    public Result<").append(entityName).append("VO> save(@RequestBody ").append(entityName).append("DTO dto) {\n");
        sb.append("        ").append(entityName).append(" entity = new ").append(entityName).append("();\n");
        sb.append("        BeanUtils.copyProperties(dto, entity);\n");
        sb.append("        ").append(serviceField).append(".save(entity);\n");
        sb.append("        return Result.success(convertToVO(entity));\n");
        sb.append("    }\n\n");

        sb.append("    @ApiOperation(\"修改\")\n");
        sb.append("    @PutMapping\n");
        sb.append("    public Result<").append(entityName).append("VO> update(@RequestBody ").append(entityName).append("DTO dto) {\n");
        sb.append("        ").append(entityName).append(" entity = new ").append(entityName).append("();\n");
        sb.append("        BeanUtils.copyProperties(dto, entity);\n");
        sb.append("        ").append(serviceField).append(".updateById(entity);\n");
        sb.append("        return Result.success(convertToVO(entity));\n");
        sb.append("    }\n\n");

        sb.append("    @ApiOperation(\"删除\")\n");
        sb.append("    @DeleteMapping(\"/{id}\")\n");
        sb.append("    public Result<Void> delete(@PathVariable Long id) {\n");
        sb.append("        ").append(serviceField).append(".removeById(id);\n");
        sb.append("        return Result.success();\n");
        sb.append("    }\n\n");

        sb.append("    @ApiOperation(\"根据ID查询\")\n");
        sb.append("    @GetMapping(\"/{id}\")\n");
        sb.append("    public Result<").append(entityName).append("VO> getById(@PathVariable Long id) {\n");
        sb.append("        ").append(entityName).append(" entity = ").append(serviceField).append(".getById(id);\n");
        sb.append("        return Result.success(convertToVO(entity));\n");
        sb.append("    }\n\n");

        sb.append("    @ApiOperation(\"查询列表\")\n");
        sb.append("    @GetMapping(\"/list\")\n");
        sb.append("    public Result<List<").append(entityName).append("VO>> list() {\n");
        sb.append("        List<").append(entityName).append("> list = ").append(serviceField).append(".list();\n");
        sb.append("        List<").append(entityName).append("VO> voList = list.stream()\n");
        sb.append("                .map(this::convertToVO)\n");
        sb.append("                .collect(Collectors.toList());\n");
        sb.append("        return Result.success(voList);\n");
        sb.append("    }\n\n");

        sb.append("    @ApiOperation(\"分页查询\")\n");
        sb.append("    @GetMapping(\"/page\")\n");
        sb.append("    public Result<Page<").append(entityName).append("VO>> page(\n");
        sb.append("            @RequestParam(defaultValue = \"1\") Integer current,\n");
        sb.append("            @RequestParam(defaultValue = \"10\") Integer size) {\n");
        sb.append("        Page<").append(entityName).append("> page = ").append(serviceField)
                .append(".page(new Page<>(current, size), new LambdaQueryWrapper<>());\n");
        sb.append("        Page<").append(entityName).append("VO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());\n");
        sb.append("        voPage.setRecords(page.getRecords().stream()\n");
        sb.append("                .map(this::convertToVO)\n");
        sb.append("                .collect(Collectors.toList()));\n");
        sb.append("        return Result.success(voPage);\n");
        sb.append("    }\n\n");

        sb.append("    private ").append(entityName).append("VO convertToVO(").append(entityName).append(" entity) {\n");
        sb.append("        if (entity == null) return null;\n");
        sb.append("        ").append(entityName).append("VO vo = new ").append(entityName).append("VO();\n");
        sb.append("        BeanUtils.copyProperties(entity, vo);\n");
        sb.append("        return vo;\n");
        sb.append("    }\n");

        sb.append("}\n");

        return new GeneratedCode("CONTROLLER", controllerName + ".java", filePath, sb.toString());
    }

    public GeneratedCode generateVO(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String voName = entityName + "VO";
        String packageName = buildPackage(config, config.getVoPackage());
        String filePath = packageToPath(packageName) + "/" + voName + ".java";

        return generateDTOOrVO(model, config, voName, packageName, filePath, "VO", "视图对象");
    }

    public GeneratedCode generateDTO(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String dtoName = entityName + "DTO";
        String packageName = buildPackage(config, config.getDtoPackage());
        String filePath = packageToPath(packageName) + "/" + dtoName + ".java";

        return generateDTOOrVO(model, config, dtoName, packageName, filePath, "DTO", "数据传输对象");
    }

    private GeneratedCode generateDTOOrVO(DataModel model, GeneratorConfig config, String className,
                                           String packageName, String filePath, String type, String desc) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        sb.append("import lombok.Data;\n");

        Set<String> imports = new HashSet<>();
        for (ModelField field : model.getFields()) {
            String javaType = field.getJavaType();
            if (javaType.contains("LocalDate")) {
                imports.add("import java.time.LocalDate;");
            } else if (javaType.contains("LocalDateTime")) {
                imports.add("import java.time.LocalDateTime;");
            } else if (javaType.contains("LocalTime")) {
                imports.add("import java.time.LocalTime;");
            } else if (javaType.contains("BigDecimal")) {
                imports.add("import java.math.BigDecimal;");
            }
        }

        for (String imp : imports) {
            sb.append(imp).append("\n");
        }

        sb.append("import java.io.Serializable;\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(model.getModelName()).append(" ").append(desc).append("\n");
        sb.append(" * @author ").append(config.getAuthor()).append("\n");
        sb.append(" * @since ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append(" */\n");

        sb.append("@Data\n");
        sb.append("public class ").append(className).append(" implements Serializable {\n\n");
        sb.append("    private static final long serialVersionUID = 1L;\n\n");

        for (ModelField field : model.getFields()) {
            sb.append("    /**\n");
            sb.append("     * ").append(field.getFieldComment() != null ? field.getFieldComment() : field.getFieldName()).append("\n");
            sb.append("     */\n");

            String fieldType = field.getJavaType();
            String fieldName = StrUtil.toCamelCase(field.getFieldName());

            sb.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n\n");
        }

        sb.append("}\n");

        return new GeneratedCode(type, className + ".java", filePath, sb.toString());
    }

    public GeneratedCode generateApiJs(DataModel model, GeneratorConfig config) {
        String entityName = model.getEntityName();
        String apiName = StrUtil.toSymbolCase(entityName, '-') + ".js";
        String filePath = "api/" + apiName;

        String basePath = "/" + StrUtil.toSymbolCase(entityName, '-');
        String apiNameCamel = StrUtil.toCamelCase(entityName);

        StringBuilder sb = new StringBuilder();
        sb.append("import request from '@/utils/request'\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(model.getModelName()).append(" API接口\n");
        sb.append(" */\n\n");

        sb.append("const ").append(apiNameCamel).append("Api = {\n\n");

        sb.append("  /** 新增 */\n");
        sb.append("  save(data) {\n");
        sb.append("    return request({\n");
        sb.append("      url: '").append(basePath).append("',\n");
        sb.append("      method: 'post',\n");
        sb.append("      data\n");
        sb.append("    })\n");
        sb.append("  },\n\n");

        sb.append("  /** 修改 */\n");
        sb.append("  update(data) {\n");
        sb.append("    return request({\n");
        sb.append("      url: '").append(basePath).append("',\n");
        sb.append("      method: 'put',\n");
        sb.append("      data\n");
        sb.append("    })\n");
        sb.append("  },\n\n");

        sb.append("  /** 删除 */\n");
        sb.append("  delete(id) {\n");
        sb.append("    return request({\n");
        sb.append("      url: `").append(basePath).append("/${id}`,\n");
        sb.append("      method: 'delete'\n");
        sb.append("    })\n");
        sb.append("  },\n\n");

        sb.append("  /** 根据ID查询 */\n");
        sb.append("  getById(id) {\n");
        sb.append("    return request({\n");
        sb.append("      url: `").append(basePath).append("/${id}`,\n");
        sb.append("      method: 'get'\n");
        sb.append("    })\n");
        sb.append("  },\n\n");

        sb.append("  /** 查询列表 */\n");
        sb.append("  list(params) {\n");
        sb.append("    return request({\n");
        sb.append("      url: '").append(basePath).append("/list',\n");
        sb.append("      method: 'get',\n");
        sb.append("      params\n");
        sb.append("    })\n");
        sb.append("  },\n\n");

        sb.append("  /** 分页查询 */\n");
        sb.append("  page(params) {\n");
        sb.append("    return request({\n");
        sb.append("      url: '").append(basePath).append("/page',\n");
        sb.append("      method: 'get',\n");
        sb.append("      params\n");
        sb.append("    })\n");
        sb.append("  }\n\n");

        sb.append("}\n\n");
        sb.append("export default ").append(apiNameCamel).append("Api\n");

        return new GeneratedCode("API_JS", apiName, filePath, sb.toString());
    }

    private String buildPackage(GeneratorConfig config, String subPackage) {
        String base = config.getBasePackage();
        if (config.getModuleName() != null && !config.getModuleName().isEmpty()) {
            base += "." + config.getModuleName();
        }
        return base + "." + subPackage;
    }

    private String packageToPath(String packageName) {
        return packageName.replace(".", "/");
    }
}
