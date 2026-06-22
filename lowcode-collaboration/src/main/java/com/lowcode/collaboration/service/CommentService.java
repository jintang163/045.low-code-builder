package com.lowcode.collaboration.service;

import com.lowcode.collaboration.dto.CommentCreateDTO;
import com.lowcode.collaboration.dto.CommentQueryDTO;
import com.lowcode.collaboration.entity.Comment;

import java.util.List;

public interface CommentService {

    Comment createComment(CommentCreateDTO dto);

    Comment getCommentById(Long id);

    List<Comment> getCommentsByTarget(CommentQueryDTO query);

    boolean deleteComment(Long id);

    boolean resolveComment(Long id, Long userId);

    boolean unresolveComment(Long id);

    boolean likeComment(Long id);

    int countUnreadMentions(Long userId);

    List<Comment> getUnreadMentionComments(Long userId);

    boolean markMentionAsRead(Long commentId, Long userId);

    boolean markAllMentionsAsRead(Long userId);
}
