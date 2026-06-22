package com.lowcode.collaboration.controller;

import com.lowcode.collaboration.dto.CommentCreateDTO;
import com.lowcode.collaboration.dto.CommentQueryDTO;
import com.lowcode.collaboration.entity.Comment;
import com.lowcode.collaboration.feign.OssFeignClient;
import com.lowcode.collaboration.feign.OssFileVO;
import com.lowcode.collaboration.service.CommentService;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "评论管理")
@RestController
@RequestMapping("/api/collaboration/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired(required = false)
    private OssFeignClient ossFeignClient;

    @ApiOperation("创建评论")
    @PostMapping
    public Result<Comment> create(@Validated @RequestBody CommentCreateDTO dto) {
        return Result.success(commentService.createComment(dto));
    }

    @ApiOperation("获取评论详情")
    @GetMapping("/{id}")
    public Result<Comment> getById(@PathVariable Long id) {
        return Result.success(commentService.getCommentById(id));
    }

    @ApiOperation("获取目标的评论列表")
    @GetMapping("/list")
    public Result<List<Comment>> list(CommentQueryDTO query) {
        return Result.success(commentService.getCommentsByTarget(query));
    }

    @ApiOperation("删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }

    @ApiOperation("标记评论为已解决")
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        commentService.resolveComment(id, userId);
        return Result.success();
    }

    @ApiOperation("取消评论解决状态")
    @PostMapping("/{id}/unresolve")
    public Result<Void> unresolve(@PathVariable Long id) {
        commentService.unresolveComment(id);
        return Result.success();
    }

    @ApiOperation("点赞评论")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.success();
    }

    @ApiOperation("统计未读@我的数量")
    @GetMapping("/mentions/unread/count")
    public Result<Integer> countUnreadMentions() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(commentService.countUnreadMentions(userId));
    }

    @ApiOperation("获取@我的未读评论")
    @GetMapping("/mentions/unread")
    public Result<List<Comment>> getUnreadMentions() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(commentService.getUnreadMentionComments(userId));
    }

    @ApiOperation("标记@我的评论为已读")
    @PostMapping("/mentions/{commentId}/read")
    public Result<Void> markMentionAsRead(@PathVariable Long commentId) {
        Long userId = UserContext.getCurrentUserId();
        commentService.markMentionAsRead(commentId, userId);
        return Result.success();
    }

    @ApiOperation("标记所有@我的评论为已读")
    @PostMapping("/mentions/read-all")
    public Result<Void> markAllMentionsAsRead() {
        Long userId = UserContext.getCurrentUserId();
        commentService.markAllMentionsAsRead(userId);
        return Result.success();
    }

    @ApiOperation("上传评论附件（图片/文件）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        try {
            OssFileVO ossFile;
            if (ossFeignClient != null) {
                Result<OssFileVO> uploadResult = ossFeignClient.upload(file, "collaboration/comments", null);
                if (uploadResult == null || uploadResult.getCode() != 0 && uploadResult.getCode() != 200) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                            uploadResult != null ? uploadResult.getMessage() : "文件上传失败");
                }
                ossFile = uploadResult.getData();
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OSS服务未连接");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", ossFile.getId());
            result.put("fileName", ossFile.getOriginalName());
            result.put("fileUrl", ossFile.getUrl());
            result.put("fileType", ossFile.getContentType());
            result.put("fileSize", ossFile.getFileSize());

            if (ossFile.getContentType() != null && ossFile.getContentType().startsWith("image/")) {
                try (InputStream is = file.getInputStream()) {
                    BufferedImage image = ImageIO.read(is);
                    if (image != null) {
                        result.put("width", image.getWidth());
                        result.put("height", image.getHeight());
                    }
                } catch (Exception e) {
                    log.warn("读取图片尺寸失败: {}", e.getMessage());
                }
            }

            return Result.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传评论附件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }
}
