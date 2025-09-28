package ru.practicum.ewm.comments.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.comments.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByEventId(Long eventId, Pageable pageable);

    List<Comment> findAllByAuthorId(Long authorId);

    long countByEventId(Long eventId);

    @Query("SELECT c.event.id, COUNT(c) FROM Comment c WHERE c.event.id IN :eventIds GROUP BY c.event.id")
    List<Object[]> findCommentCountsByEventIds(@Param("eventIds") List<Long> eventIds);
}
