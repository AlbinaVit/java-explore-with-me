package ru.practicum.ewm.events.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.events.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e JOIN FETCH e.category JOIN FETCH e.initiator WHERE e.initiator.id = :initiatorId")
    Page<Event> findByInitiatorId(@Param("initiatorId") Long initiatorId, Pageable pageable);

    @Query(value = "SELECT e.* FROM events e WHERE " +
            "((:users) IS NULL OR e.initiator_id IN (:users)) AND " +
            "((:states) IS NULL OR e.state IN (:states)) AND " +
            "((:categories) IS NULL OR e.category_id IN (:categories)) AND " +
            "(cast(:rangeStart as text) is null or e.event_date >= cast(cast(:rangeStart as text) as timestamp)) AND " +
            "(cast(:rangeEnd as text) is null or e.event_date <= cast(cast(:rangeEnd as text) as timestamp))",
            nativeQuery = true)
    Page<Event> findEventsForAdmin(@Param("users") List<Long> users,
                                   @Param("states") List<String> states,
                                   @Param("categories") List<Long> categories,
                                   @Param("rangeStart") LocalDateTime rangeStart,
                                   @Param("rangeEnd") LocalDateTime rangeEnd,
                                   Pageable pageable);

    @Query(value = """
            SELECT e FROM Event e
            WHERE e.id = :id AND e.state = 'PUBLISHED'
            """)
    Optional<Event> findPublishedById(@Param("id") Long id);


    @Query(value = "SELECT e.* FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND ((:text) IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND ((:categories) IS NULL OR e.category_id IN (:categories)) " +
            "AND ((:paid) IS NULL OR e.paid = (:paid)) " +
            "AND (cast(:rangeStart as text) is null or e.event_date >= cast(cast(:rangeStart as text) as timestamp)) " +
            "AND (cast(:rangeEnd as text) is null or e.event_date <= cast(cast(:rangeEnd as text) as timestamp)) " +
            "AND (:onlyAvailable = false OR e.participant_limit = 0 " +
            "     OR (SELECT COUNT(pr.id) FROM requests pr " +
            "         WHERE pr.event_id = e.id AND pr.status = 'CONFIRMED') < e.participant_limit)",
            nativeQuery = true)
    Page<Event> findPublicEvents(@Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("onlyAvailable") Boolean onlyAvailable,
                                 Pageable pageable);

    // Для сортировки по просмотрам (добавьте поле views в Event, обновляйте из статистики)
    @Query("SELECT e FROM Event e WHERE e.id IN :ids ORDER BY CASE WHEN :sort = 'VIEWS' THEN e.views ELSE 0 END DESC, e.eventDate DESC")
    List<Event> findByIdsSorted(@Param("ids") List<Long> ids, @Param("sort") String sort);

    //для категорий
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e WHERE e.category.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);

}
