package com.lowcode.collaboration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.collaboration.dto.CommentCreateDTO;
import com.lowcode.collaboration.dto.CommentQueryDTO;
import com.lowcode.collaboration.entity.Comment;
import com.lowcode.collaboration.service.CommentService;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "评论管理")
@RestController
@RequestMapping("/api/collaboration/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

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
}
