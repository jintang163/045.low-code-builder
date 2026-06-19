package com.lowcode.model.controller;

import com.lowcode.common.result.Result;
import com.lowcode.model.expression.ExpressionExecuteRequest;
import com.lowcode.model.expression.ExpressionFunction;
import com.lowcode.model.expression.ExpressionResult;
import com.lowcode.model.expression.ExpressionSandboxExecutor;
import com.lowcode.model.expression.ExpressionValidator;
import com.lowcode.model.expression.ValidationResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "表达式引擎")
@RestController
@RequestMapping("/api/expression")
public class ExpressionController {

    @Autowired
    private ExpressionSandboxExecutor sandboxExecutor;

    @Autowired
    private ExpressionValidator expressionValidator;

    @ApiOperation("获取所有函数定义列表")
    @GetMapping("/functions")
    public Result<List<ExpressionFunction>> listFunctions() {
        return Result.success(Arrays.asList(ExpressionFunction.values()));
    }

    @ApiOperation("按分类获取函数")
    @GetMapping("/functions/category/{category}")
    public Result<List<ExpressionFunction>> listFunctionsByCategory(@PathVariable String category) {
        ExpressionFunction.Category cat = null;
        for (ExpressionFunction.Category c : ExpressionFunction.Category.values()) {
            if (c.name().equalsIgnoreCase(category) || c.getDisplayName().equals(category)) {
                cat = c;
                break;
            }
        }
        if (cat == null) {
            return Result.success(Arrays.asList());
        }
        Map<ExpressionFunction.Category, List<ExpressionFunction>> grouped = ExpressionFunction.getByCategory();
        return Result.success(grouped.getOrDefault(cat, Arrays.asList()));
    }

    @ApiOperation("获取函数分类列表")
    @GetMapping("/categories")
    public Result<List<Map<String, String>>> listCategories() {
        List<Map<String, String>> categories = Arrays.stream(ExpressionFunction.Category.values())
                .map(c -> {
                    Map<String, String> item = new java.util.LinkedHashMap<>();
                    item.put("name", c.name());
                    item.put("displayName", c.getDisplayName());
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(categories);
    }

    @ApiOperation("校验表达式")
    @PostMapping("/validate")
    public Result<ValidationResult> validate(@RequestBody String expression) {
        return Result.success(expressionValidator.validate(expression));
    }

    @ApiOperation("执行表达式")
    @PostMapping("/execute")
    public Result<ExpressionResult> execute(@RequestBody ExpressionExecuteRequest request) {
        return Result.success(sandboxExecutor.execute(request.getExpression(), request.getContext()));
    }
}
