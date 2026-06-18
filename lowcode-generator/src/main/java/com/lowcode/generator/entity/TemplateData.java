package com.lowcode.generator.entity;

import com.lowcode.model.entity.DataSource;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class TemplateData implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<DataSource> dataSources;

    private List<Map<String, Object>> dataModels;

    private List<Map<String, Object>> pages;

    private List<Map<String, Object>> businessLogics;

    private List<Map<String, Object>> workflows;

    private List<Map<String, Object>> components;
}
