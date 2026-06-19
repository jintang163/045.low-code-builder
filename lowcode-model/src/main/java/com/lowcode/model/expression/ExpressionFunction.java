package com.lowcode.model.expression;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum ExpressionFunction {

    SUM("SUM", "求和", "对一组数值求和", "SUM(field1, field2, ...)", "number", Category.AGGREGATE),
    AVG("AVG", "平均值", "对一组数值求平均值", "AVG(field1, field2, ...)", "number", Category.AGGREGATE),
    COUNT("COUNT", "计数", "统计非空值的数量", "COUNT(field1, field2, ...)", "number", Category.AGGREGATE),
    MAX("MAX", "最大值", "求一组数值的最大值", "MAX(field1, field2, ...)", "number", Category.AGGREGATE),
    MIN("MIN", "最小值", "求一组数值的最小值", "MIN(field1, field2, ...)", "number", Category.AGGREGATE),

    IF("IF", "条件判断", "根据条件返回不同的值", "IF(condition, trueValue, falseValue)", "any", Category.CONDITIONAL),
    IIF("IIF", "内联条件", "内联条件表达式", "IIF(condition, trueValue, falseValue)", "any", Category.CONDITIONAL),

    CONTAINS("CONTAINS", "包含", "判断字符串是否包含子串", "CONTAINS(str, substring)", "boolean", Category.STRING_MATCH),
    STARTSWITH("STARTSWITH", "开头匹配", "判断字符串是否以指定前缀开头", "STARTSWITH(str, prefix)", "boolean", Category.STRING_MATCH),
    ENDSWITH("ENDSWITH", "结尾匹配", "判断字符串是否以指定后缀结尾", "ENDSWITH(str, suffix)", "boolean", Category.STRING_MATCH),

    CONCAT("CONCAT", "字符串拼接", "将多个字符串拼接为一个", "CONCAT(str1, str2, ...)", "string", Category.STRING_PROCESS),
    LENGTH("LENGTH", "字符串长度", "获取字符串长度", "LENGTH(str)", "number", Category.STRING_PROCESS),
    TRIM("TRIM", "去除空格", "去除字符串首尾空格", "TRIM(str)", "string", Category.STRING_PROCESS),
    UPPER("UPPER", "转大写", "将字符串转换为大写", "UPPER(str)", "string", Category.STRING_PROCESS),
    LOWER("LOWER", "转小写", "将字符串转换为小写", "LOWER(str)", "string", Category.STRING_PROCESS),
    SUBSTRING("SUBSTRING", "截取子串", "截取字符串的子串", "SUBSTRING(str, start, length)", "string", Category.STRING_PROCESS),

    NOW("NOW", "当前时间", "获取当前日期时间", "NOW()", "string", Category.DATE),
    FORMAT_DATE("FORMAT_DATE", "格式化日期", "将日期按指定格式格式化", "FORMAT_DATE(date, pattern)", "string", Category.DATE),
    DATE_ADD("DATE_ADD", "日期加减", "对日期进行加减运算", "DATE_ADD(date, amount, unit)", "string", Category.DATE),
    DATE_DIFF("DATE_DIFF", "日期差值", "计算两个日期之间的差值", "DATE_DIFF(startDate, endDate, unit)", "number", Category.DATE),

    ABS("ABS", "绝对值", "求绝对值", "ABS(number)", "number", Category.MATH),
    ROUND("ROUND", "四舍五入", "对数值进行四舍五入", "ROUND(number, digits)", "number", Category.MATH),
    CEIL("CEIL", "向上取整", "向上取整", "CEIL(number)", "number", Category.MATH),
    FLOOR("FLOOR", "向下取整", "向下取整", "FLOOR(number)", "number", Category.MATH),
    MOD("MOD", "取模", "求模运算", "MOD(number, divisor)", "number", Category.MATH),

    IS_NULL("IS_NULL", "空值判断", "判断值是否为null", "IS_NULL(value)", "boolean", Category.NULL_HANDLER),
    IS_EMPTY("IS_EMPTY", "空值判断", "判断值是否为空(null或空字符串)", "IS_EMPTY(value)", "boolean", Category.NULL_HANDLER),
    COALESCE("COALESCE", "空值替换", "返回第一个非空值", "COALESCE(value1, value2, ...)", "any", Category.NULL_HANDLER);

    private final String name;
    private final String displayName;
    private final String description;
    private final String syntax;
    private final String returnType;
    private final Category category;

    ExpressionFunction(String name, String displayName, String description, String syntax, String returnType, Category category) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.syntax = syntax;
        this.returnType = returnType;
        this.category = category;
    }

    public static Map<Category, List<ExpressionFunction>> getByCategory() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(
                        ExpressionFunction::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public static List<String> getCategories() {
        return Arrays.stream(Category.values())
                .map(Category::getDisplayName)
                .collect(Collectors.toList());
    }

    public static ExpressionFunction getByName(String name) {
        for (ExpressionFunction fn : values()) {
            if (fn.getName().equalsIgnoreCase(name)) {
                return fn;
            }
        }
        return null;
    }

    public static List<String> getAllFunctionNames() {
        return Arrays.stream(values())
                .map(ExpressionFunction::getName)
                .collect(Collectors.toList());
    }

    @Getter
    public enum Category {
        AGGREGATE("聚合函数"),
        CONDITIONAL("条件函数"),
        STRING_MATCH("字符串匹配"),
        STRING_PROCESS("字符串处理"),
        DATE("日期函数"),
        MATH("数学函数"),
        NULL_HANDLER("空值处理");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }
    }
}
