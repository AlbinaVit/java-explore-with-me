package ru.practicum.ewm.comments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.comments.dto.CommentDto;
import ru.practicum.ewm.comments.dto.CommentMapper;
import ru.practicum.ewm.comments.dto.NewCommentDto;
import ru.practicum.ewm.comments.dto.UpdateCommentDto;
import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.State;
import ru.practicum.ewm.events.service.EventService;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.users.model.User;
import ru.practicum.ewm.users.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserService userService;
    private final EventService eventService;
    private final CommentMapper commentMapper;

    public CommentDto create(Long userId, Long eventId, NewCommentDto dto) {
        User author = userService.getUserById(userId);
        Event event = eventService.getEventById(eventId);

        if (!State.PUBLISHED.equals(event.getState())) {
            throw new ValidationException("Комментарии можно оставлять только к опубликованным событиям");
        }

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setCreated(LocalDateTime.now());

        comment = commentRepository.save(comment);
        return commentMapper.toCommentDto(comment);
    }

    public Page<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        return comments.map(commentMapper::toCommentDto);
    }

    public CommentDto update(Long userId, Long commentId, UpdateCommentDto dto) {
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            log.info("");
            throw new ConflictException("Только автор может редактировать комментарий");
        }

        comment.setText(dto.getText());
        comment.setUpdated(LocalDateTime.now());

        comment = commentRepository.save(comment);
        return commentMapper.toCommentDto(comment);
    }

    public void deleteByUser(Long userId, Long commentId) {
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ValidationException("Только автор может удалить комментарий");
        }

        commentRepository.delete(comment);
    }

    public void deleteByAdmin(Long commentId) {
        Comment comment = getCommentById(commentId);
        commentRepository.delete(comment);
    }

    public List<CommentDto> getCommentsByUser(Long userId) {
        List<Comment> comments = commentRepository.findAllByAuthorId(userId);
        return comments.stream().map(commentMapper::toCommentDto).collect(Collectors.toList());
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID=" + commentId + " не найден"));
    }
}
