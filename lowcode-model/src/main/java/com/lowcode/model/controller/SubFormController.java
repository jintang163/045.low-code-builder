package com.lowcode.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lowcode.common.result.Result;
import com.lowcode.model.service.SubFormService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "子表单数据服务")
@RestController
@RequestMapping("/api/subform")
public class SubFormController {

    @Autowired
    private SubFormService subFormService;

    @ApiOperation("查询子表单数据列表")
    @GetMapping("/list/{mainModelId}/{mainId}/{subModelId}")
    public Result<List<Map<String, Object>>> querySubFormList(
            @PathVariable Long mainModelId,
            @PathVariable Object mainId,
            @PathVariable Long subModelId,
            @RequestParam String foreignKeyField) {
        return Result.success(subFormService.querySubFormList(mainModelId, mainId, subModelId, foreignKeyField));
    }

    @ApiOperation("分页查询子表单数据")
    @GetMapping("/page/{mainModelId}/{mainId}/{subModelId}")
    public Result<IPage<Map<String, Object>>> querySubFormPage(
            @PathVariable Long mainModelId,
            @PathVariable Object mainId,
            @PathVariable Long subModelId,
            @RequestParam String foreignKeyField,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(subFormService.querySubFormPage(mainModelId, mainId, subModelId, foreignKeyField, current, size));
    }

    @ApiOperation("保存子表单单条数据")
    @PostMapping("/save/{mainModelId}/{mainId}/{subModelId}")
    public Result<Map<String, Object>> saveSubFormData(
            @PathVariable Long mainModelId,
            @PathVariable Object mainId,
            @PathVariable Long subModelId,
            @RequestParam String foreignKeyField,
            @RequestBody Map<String, Object> data) {
        return Result.success(subFormService.saveSubFormData(mainModelId, mainId, subModelId, foreignKeyField, data));
    }

    @ApiOperation("批量保存子表单数据")
    @PostMapping("/batchSave/{mainModelId}/{mainId}/{subModelId}")
    public Result<List<Map<String, Object>>> batchSaveSubFormData(
            @PathVariable Long mainModelId,
            @PathVariable Object mainId,
            @PathVariable Long subModelId,
            @RequestParam String foreignKeyField,
            @RequestBody List<Map<String, Object>> dataList) {
        return Result.success(subFormService.batchSaveSubFormData(mainModelId, mainId, subModelId, foreignKeyField, dataList));
    }

    @ApiOperation("删除子表单单条数据")
    @DeleteMapping("/{subModelId}/{id}")
    public Result<Void> deleteSubFormData(
            @PathVariable Long subModelId,
            @PathVariable Object id) {
        subFormService.deleteSubFormData(subModelId, id);
        return Result.success();
    }

    @ApiOperation("按外键删除子表单数据")
    @DeleteMapping("/byForeignKey/{subModelId}")
    public Result<Void> deleteSubFormByForeignKey(
            @PathVariable Long subModelId,
            @RequestParam String foreignKeyField,
            @RequestParam Object foreignKeyValue) {
        subFormService.deleteSubFormByForeignKey(subModelId, foreignKeyField, foreignKeyValue);
        return Result.success();
    }

    @ApiOperation("查询主表单及所有子表单数据")
    @PostMapping("/masterWithSubForms/{mainModelId}/{mainId}")
    public Result<Map<String, Object>> queryMasterWithSubForms(
            @PathVariable Long mainModelId,
            @PathVariable Object mainId,
            @RequestBody List<SubFormService.SubFormConfig> subFormConfigs) {
        return Result.success(subFormService.queryMasterWithSubForms(mainModelId, mainId, subFormConfigs));
    }

    @ApiOperation("保存主表单及所有子表单数据")
    @PostMapping("/saveMasterWithSubForms/{mainModelId}")
    public Result<Map<String, Object>> saveMasterWithSubForms(
            @PathVariable Long mainModelId,
            @RequestBody MasterWithSubFormsRequest request) {
        return Result.success(subFormService.saveMasterWithSubForms(
                mainModelId,
                request.getMasterData(),
                request.getSubFormDataList()
        ));
    }

    public static class MasterWithSubFormsRequest {
        private Map<String, Object> masterData;
        private List<SubFormService.SubFormData> subFormDataList;

        public Map<String, Object> getMasterData() {
            return masterData;
        }

        public void setMasterData(Map<String, Object> masterData) {
            this.masterData = masterData;
        }

        public List<SubFormService.SubFormData> getSubFormDataList() {
            return subFormDataList;
        }

        public void setSubFormDataList(List<SubFormService.SubFormData> subFormDataList) {
            this.subFormDataList = subFormDataList;
        }
    }
}
