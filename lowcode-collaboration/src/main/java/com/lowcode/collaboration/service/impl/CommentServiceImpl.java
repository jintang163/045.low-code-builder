package com.lowcode.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.collaboration.dto.CommentAttachmentDTO;
import com.lowcode.collaboration.dto.CommentCreateDTO;
import com.lowcode.collaboration.dto.CommentQueryDTO;
import com.lowcode.collaboration.dto.TaskAssignmentDTO;
import com.lowcode.collaboration.entity.Comment;
import com.lowcode.collaboration.entity.CommentAttachment;
import com.lowcode.collaboration.entity.CommentMention;
import com.lowcode.collaboration.mapper.CommentAttachmentMapper;
import com.lowcode.collaboration.mapper.CommentMapper;
import com.lowcode.collaboration.mapper.CommentMentionMapper;
import com.lowcode.collaboration.service.CommentService;
import com.lowcode.collaboration.service.TaskAssignmentService;
import com.lowcode.collaboration.websocket.CollaborationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentAttachmentMapper attachmentMapper;

    @Autowired
    private CommentMentionMapper mentionMapper;

    @Autowired
    private TaskAssignmentService taskAssignmentService;

    @Autowired(required = false)
    private CollaborationWebSocketHandler webSocketHandler;

    @Override
    @Transactional
    public Comment createComment(CommentCreateDTO dto) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(dto, comment);
        comment.setStatus(1);
        comment.setLikeCount(0);
        commentMapper.insert(comment);

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            for (CommentAttachmentDTO attDTO : dto.getAttachments()) {
                CommentAttachment att = new CommentAttachment();
                BeanUtils.copyProperties(attDTO, att);
                att.setCommentId(comment.getId());
                attachmentMapper.insert(att);
            }
        }

        if (dto.getMentionUserIds() != null && !dto.getMentionUserIds().isEmpty()) {
            for (Long userId : dto.getMentionUserIds()) {
                CommentMention mention = new CommentMention();
                mention.setCommentId(comment.getId());
                mention.setUserId(userId);
                mention.setIsRead(0);
                mentionMapper.insert(mention);
            }
        }

        if (dto.getTaskAssignment() != null) {
            TaskAssignmentDTO taskDTO = dto.getTaskAssignment();
            taskAssignmentService.createTask(taskDTO, dto.getAppId(), dto.getTargetType(),
                    dto.getTargetId(), dto.getTargetName(), comment.getId());
        }

        Comment result = getCommentWithDetails(comment.getId());

        if (webSocketHandler != null) {
            try {
                webSocketHandler.broadcastComment(dto.getAppId(), dto.getTargetType(), dto.getTargetId(), result);
            } catch (Exception e) {
                log.warn("WebSocket推送评论消息失败", e);
            }
        }

        return result;
    }

    @Override
    public Comment getCommentById(Long id) {
        return getCommentWithDetails(id);
    }

    @Override
    public List<Comment> getCommentsByTarget(CommentQueryDTO query) {
        List<Comment> roots = commentMapper.selectRootComments(query.getAppId(), query.getTargetType(), query.getTargetId());
        for (Comment root : roots) {
            enrichComment(root);
            List<Comment> replies = commentMapper.selectReplies(root.getId());
            for (Comment reply : replies) {
                enrichComment(reply);
            }
            root.setReplies(replies);
        }
        return roots;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return false;
        }
        commentMapper.deleteById(id);
        return true;
    }

    @Override
    @Transactional
    public boolean resolveComment(Long id, Long userId) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return false;
        }
        comment.setStatus(2);
        comment.setResolvedBy(userId);
        comment.setResolvedTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        return true;
    }

    @Override
    @Transactional
    public boolean unresolveComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return false;
        }
        comment.setStatus(1);
        comment.setResolvedBy(null);
        comment.setResolvedTime(null);
        commentMapper.updateById(comment);
        return true;
    }

    @Override
    @Transactional
    public boolean likeComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return false;
        }
        comment.setLikeCount(comment.getLikeCount() == null ? 1 : comment.getLikeCount() + 1);
        commentMapper.updateById(comment);
        return true;
    }

    @Override
    public int countUnreadMentions(Long userId) {
        return mentionMapper.selectUnreadByUserId(userId).size();
    }

    @Override
    public List<Comment> getUnreadMentionComments(Long userId) {
        List<CommentMention> mentions = mentionMapper.selectUnreadByUserId(userId);
        List<Long> commentIds = mentions.stream()
                .map(CommentMention::getCommentId)
                .collect(Collectors.toList());
        if (commentIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Comment> comments = commentMapper.selectBatchIds(commentIds);
        for (Comment comment : comments) {
            enrichComment(comment);
        }
        return comments;
    }

    @Override
    @Transactional
    public boolean markMentionAsRead(Long commentId, Long userId) {
        LambdaQueryWrapper<CommentMention> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentMention::getCommentId, commentId)
                .eq(CommentMention::getUserId, userId);
        CommentMention mention = mentionMapper.selectOne(wrapper);
        if (mention == null) {
            return false;
        }
        mention.setIsRead(1);
        mention.setReadTime(LocalDateTime.now());
        mentionMapper.updateById(mention);
        return true;
    }

    @Override
    @Transactional
    public boolean markAllMentionsAsRead(Long userId) {
        List<CommentMention> mentions = mentionMapper.selectUnreadByUserId(userId);
        for (CommentMention mention : mentions) {
            mention.setIsRead(1);
            mention.setReadTime(LocalDateTime.now());
            mentionMapper.updateById(mention);
        }
        return true;
    }

    private Comment getCommentWithDetails(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            enrichComment(comment);
        }
        return comment;
    }

    private void enrichComment(Comment comment) {
        comment.setAttachments(attachmentMapper.selectByCommentId(comment.getId()));
        comment.setMentions(mentionMapper.selectByCommentId(comment.getId()));
    }
}
