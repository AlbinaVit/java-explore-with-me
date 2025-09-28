package ru.practicum.ewm.comments.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comments.dto.CommentDto;
import ru.practicum.ewm.comments.service.CommentService;

@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
@Slf4j
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping
    public Page<CommentDto> getComments(@PathVariable Long eventId,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "10") int size) {
        log.info("Получение комментариев для события eventId: {} с параметрами: from={}, size={}", eventId, from, size);
        return commentService.getCommentsByEvent(eventId, from, size);
    }
}
