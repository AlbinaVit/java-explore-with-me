package ru.practicum.ewm.comments.dto;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.users.dto.UserShortDto;

@Component
public class CommentMapper {

    public CommentDto toCommentDto(Comment comment) {
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                new UserShortDto(comment.getAuthor().getId(), comment.getAuthor().getName()),
                comment.getEvent().getId(),
                comment.getCreated(),
                comment.getUpdated()
        );
    }
}
