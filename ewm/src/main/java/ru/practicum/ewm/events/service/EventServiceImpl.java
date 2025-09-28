package ru.practicum.ewm.events.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.categories.dto.CategoriesMapper;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoriesRepository;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.events.dto.EventFullDto;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.events.dto.NewEventDto;
import ru.practicum.ewm.events.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.events.dto.UpdateEventUserRequest;
import ru.practicum.ewm.events.dto.mapper.EventMapper;
import ru.practicum.ewm.events.dto.mapper.LocationMapper;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.Location;
import ru.practicum.ewm.events.model.State;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.events.repository.LocationRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.users.dto.UserMapper;
import ru.practicum.ewm.users.model.User;
import ru.practicum.ewm.users.repository.UserRepository;
import ru.practicum.ewm.users.service.UserService;
import ru.practicum.statsclient.client.StatsClient;
import ru.practicum.statsdto.dto.EndpointHitDTO;
import ru.practicum.statsdto.dto.ViewStatsDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoriesRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final CategoriesMapper categoriesMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;
    private final StatsClient statsClient;
    private final RequestMapper requestMapper;
    private final UserService userService;
    private final CommentRepository commentRepository;

    @PersistenceContext
    private EntityManager em;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        User user = userService.getUserById(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();

        // подсчёт commentCount
        Map<Long, Long> commentCounts = batchCommentCounts(events);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event,
                        categoriesMapper.toCategoryDto(event.getCategory()),
                        userMapper.toUserShortDto(user),
                        commentCounts.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userService.getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        // Проверка времени события
        LocalDateTime eventDateTime = LocalDateTime.parse(newEventDto.getEventDate(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (eventDateTime.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        // Проверка participantLimit
        if (newEventDto.getParticipantLimit() != null && newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }

        // Сохранение локации
        Location location = locationMapper.toLocation(newEventDto.getLocation());
        Location savedLocation = locationRepository.save(location);

        Event event = eventMapper.toEntity(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setLocation(savedLocation);

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toFullDto(savedEvent,
                categoriesMapper.toCategoryDto(category),
                userMapper.toUserShortDto(user),
                locationMapper.toLocationDto(savedLocation),
                0L);
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        User user = userService.getUserById(userId);
        Event event = getEventById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }
        Long commentCount = commentRepository.countByEventId(eventId);

        return eventMapper.toFullDto(event,
                categoriesMapper.toCategoryDto(event.getCategory()),
                userMapper.toUserShortDto(user),
                locationMapper.toLocationDto(event.getLocation()),
                commentCount);
    }

    @Override
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        User user = userService.getUserById(userId);
        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        if (event.getState() != State.PENDING && event.getState() != State.CANCELED) {
            throw new ConflictException("Можно изменять только ожидающие или отмененные события");
        }

        // Проверка времени события при обновлении
        if (updateEventUserRequest.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(updateEventUserRequest.getEventDate(),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
        }

        // Проверка participantLimit на отрицательное значение
        if (updateEventUserRequest.getParticipantLimit() != null && updateEventUserRequest.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }

        // Обновление категории
        if (updateEventUserRequest.getCategory() != null) {
            Category category = getCategoryById(updateEventUserRequest.getCategory());
            event.setCategory(category);
        }

        // Обновление локации
        if (updateEventUserRequest.getLocation() != null) {
            Location location = locationMapper.toLocation(updateEventUserRequest.getLocation());
            Location savedLocation = locationRepository.save(location);
            event.setLocation(savedLocation);
        }

        // Обновление остальных полей
        eventMapper.updateFromUserRequest(updateEventUserRequest, event);

        // Обработка stateAction
        if (updateEventUserRequest.getStateAction() != null) {
            switch (updateEventUserRequest.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(State.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(State.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Long commentCount = commentRepository.countByEventId(eventId);

        return eventMapper.toFullDto(updatedEvent,
                categoriesMapper.toCategoryDto(updatedEvent.getCategory()),
                userMapper.toUserShortDto(updatedEvent.getInitiator()),
                locationMapper.toLocationDto(updatedEvent.getLocation()),
                commentCount);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        userService.getUserById(userId);
        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateEventRequests(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        if (request == null || request.getRequestIds() == null || request.getRequestIds().isEmpty() ||
                request.getStatus() == null || request.getStatus().isEmpty()) {
            throw new ConflictException("Некорректное тело запроса или отсутствующие данные");
        }

        userService.getUserById(userId);
        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        // Проверка лимита участников (только если статус CONFIRMED)
        if ("CONFIRMED".equals(request.getStatus()) && event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(request.getRequestIds());

        // Проверка, что все запросы найдены и в состоянии PENDING
        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Один или несколько запросов не найдены");
        }
        boolean allPending = requests.stream()
                .allMatch(req -> req.getStatus() == RequestStatus.PENDING);
        if (!allPending) {
            throw new ConflictException("Все запросы должны быть в состоянии ожидания");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult(new ArrayList<>(), new ArrayList<>());
        List<ParticipationRequest> toSave = new ArrayList<>();

        if ("CONFIRMED".equals(request.getStatus())) {
            // Подтверждение запросов
            int availableSlots = event.getParticipantLimit() - event.getConfirmedRequests().intValue();
            int toConfirm = Math.min(availableSlots, requests.size());

            for (int i = 0; i < toConfirm; i++) {
                ParticipationRequest req = requests.get(i);
                req.setStatus(RequestStatus.CONFIRMED);
                toSave.add(req);
                result.getConfirmedRequests().add(requestMapper.toDto(req));
            }

            // Отклонение оставшихся если лимит исчерпан
            for (int i = toConfirm; i < requests.size(); i++) {
                ParticipationRequest req = requests.get(i);
                req.setStatus(RequestStatus.REJECTED);
                toSave.add(req);
                result.getRejectedRequests().add(requestMapper.toDto(req));
            }

            requestRepository.saveAll(toSave);

            // Обновление счетчика подтвержденных запросов
            event.setConfirmedRequests(event.getConfirmedRequests() + toConfirm);
            eventRepository.save(event);

        } else if ("REJECTED".equals(request.getStatus())) {
            // Отклонение запросов
            for (ParticipationRequest req : requests) {
                req.setStatus(RequestStatus.REJECTED);
                toSave.add(req);
                result.getRejectedRequests().add(requestMapper.toDto(req));
            }
            requestRepository.saveAll(toSave);
        }

        return result;
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<State> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);
        users = users == null ? Collections.emptyList() : users;
        List<String> strStates = states == null ? Collections.emptyList() : states.stream().map(State::name).toList();

        categories = categories == null ? Collections.emptyList() : categories;

        List<Event> events = eventRepository.findEventsForAdmin(users, strStates, categories, rangeStart, rangeEnd, pageable).getContent();
        // подсчёт commentCount
        Map<Long, Long> commentCounts = batchCommentCounts(events);

        return events.stream()
                .map(event -> eventMapper.toFullDto(
                        event,
                        categoriesMapper.toCategoryDto(event.getCategory()),
                        userMapper.toUserShortDto(event.getInitiator()),
                        locationMapper.toLocationDto(event.getLocation()),
                        commentCounts.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event event = getEventById(eventId);

        // Проверка времени публикации
        if (updateEventAdminRequest.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(updateEventAdminRequest.getEventDate(),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ValidationException("Дата начала события должна быть не ранее чем за час от публикации");
            }
        }

        // Обновление категории если нужно
        if (updateEventAdminRequest.getCategory() != null) {
            Category category = getCategoryById(updateEventAdminRequest.getCategory());
            event.setCategory(category);
        }

        // Обновление локации если нужно
        if (updateEventAdminRequest.getLocation() != null) {
            Location location = locationMapper.toLocation(updateEventAdminRequest.getLocation());
            Location savedLocation = locationRepository.save(location);
            event.setLocation(savedLocation);
        }

        // Обновление остальных полей
        eventMapper.updateFromAdminRequest(updateEventAdminRequest, event);

        // Обработка stateAction
        if (updateEventAdminRequest.getStateAction() != null) {
            switch (updateEventAdminRequest.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != State.PENDING) {
                        throw new ConflictException("Можно публиковать только события в состоянии ожидания");
                    }
                    event.setState(State.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case "REJECT_EVENT":
                    if (event.getState() == State.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(State.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Long commentCount = commentRepository.countByEventId(eventId);

        return eventMapper.toFullDto(updatedEvent,
                categoriesMapper.toCategoryDto(updatedEvent.getCategory()),
                userMapper.toUserShortDto(updatedEvent.getInitiator()),
                locationMapper.toLocationDto(updatedEvent.getLocation()),
                commentCount);
    }

    // Вспомогательный метод для сохранения статистики
    private void saveHitStatistic(String endpoint, String clientIp) {
        String timestampString = LocalDateTime.now().format(formatter);
        log.info("Получаем отформатированную дату: {}", timestampString);

        EndpointHitDTO hit = new EndpointHitDTO("ewm-main-service", endpoint, clientIp, timestampString);
        log.info("EndpointHitDTO hit: {}", hit);
        statsClient.saveHit(hit);
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               String sort, Integer from, Integer size, String clientIp, String endpoint) {

        log.info("Сохраняем статистику для основного запроса списка событий");
        saveHitStatistic(endpoint, clientIp);

        Pageable pageable = PageRequest.of(from / size, size);
        log.info("Страничный доступ для номера страницы: {} и количества страниц: {}", from / size, size);

        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);

        if (categories != null) {
            categories = categories.stream()
                    .filter(id -> id > 0)
                    .collect(Collectors.toList());
        } else {
            categories = Collections.emptyList();
        }

        if (rangeEnd.isBefore(rangeStart)) {
            throw new ValidationException("rangeEnd не может быть раньше rangeStart");
        }

        log.info("Данные для запроса в репозиторий text: {}, categories: {}, paid: {}, rangeStart: {}, rangeEnd: {}, onlyAvailable: {}, pageable: {}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);

        List<Event> events = eventRepository.findPublicEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable
        ).getContent();

        // Пакетное получение статистики для всех событий
        updateViewsForEvents(events);

        log.info("Данные events: {}", events);

        log.info("Сортировка");
        if ("EVENT_DATE".equals(sort)) {
            List<Event> sortedEvents = new ArrayList<>(events);
            sortedEvents.sort((e1, e2) -> e2.getEventDate().compareTo(e1.getEventDate()));
            events = sortedEvents;
        } else if ("VIEWS".equals(sort)) {
            List<Event> sortedEvents = new ArrayList<>(events);
            sortedEvents.sort((e1, e2) -> e2.getViews().compareTo(e1.getViews()));
            events = sortedEvents;
        }

        // Batch-подсчёт commentCount
        Map<Long, Long> commentCounts = batchCommentCounts(events);

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        categoriesMapper.toCategoryDto(event.getCategory()),
                        userMapper.toUserShortDto(event.getInitiator()),
                        commentCounts.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventPublic(Long id, String clientIp, String endpoint) {
        Event event = getPublishEventById(id);
        log.info("метод getEventPublic пришло в параметрах id: {}, clientIp: {}, endpoint: {}", id, clientIp, endpoint);

        updateViewsForSingleEvent(event);
        saveHitStatistic(endpoint, clientIp);

        Long commentCount = commentRepository.countByEventId(id);

        return eventMapper.toFullDto(event,
                categoriesMapper.toCategoryDto(event.getCategory()),
                userMapper.toUserShortDto(event.getInitiator()),
                locationMapper.toLocationDto(event.getLocation()),
                commentCount);
    }

    // Пакетное обновление просмотров для списка событий
    private void updateViewsForEvents(List<Event> events) {

        if (events.isEmpty()) return;

        try {
            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now();

            // Формируем список URI для всех событий
            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            List<ViewStatsDTO> stats = statsClient.getStats(start, end, uris, false);

            Map<Long, Long> viewsMap = new HashMap<>();
            for (ViewStatsDTO stat : stats) {
                try {
                    Long eventId = Long.parseLong(stat.getUri().substring("/events/".length()));
                    viewsMap.put(eventId, stat.getHits());
                } catch (NumberFormatException e) {
                    log.warn("Невозможно распарсить ID события из URI: {}", stat.getUri());
                }
            }

            // Обновляем просмотры для каждого события
            for (Event event : events) {
                Long currentViews = viewsMap.getOrDefault(event.getId(), event.getViews());
                event.setViews(currentViews);
            }

        } catch (Exception e) {
            log.error("Ошибка при пакетном обновлении просмотров: {}", e.getMessage());
        }
    }

    // Метод для обновления просмотров одного события
    private void updateViewsForSingleEvent(Event event) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now();
            String eventUri = "/events/" + event.getId();

            List<ViewStatsDTO> stats = statsClient.getStats(start, end, List.of(eventUri), true);

            if (!stats.isEmpty()) {
                event.setViews((long) stats.size());
            }

        } catch (Exception e) {
            log.error("Ошибка при обновлении просмотров для события {}: {}", event.getId(), e.getMessage());
        }
    }

    // Вспомогательный метод для подсчёта commentCount
    private Map<Long, Long> batchCommentCounts(List<Event> events) {
        Map<Long, Long> commentCounts = new HashMap<>();
        if (!events.isEmpty()) {
            List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
            List<Object[]> counts = commentRepository.findCommentCountsByEventIds(eventIds);
            for (Object[] count : counts) {
                commentCounts.put((Long) count[0], (Long) count[1]);
            }
        }
        return commentCounts;
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + categoryId + " не найдена"));
    }

    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));
    }

    private Event getPublishEventById(Long eventId) {
        return eventRepository.findPublishedById(eventId)
                .orElseThrow(() -> new NotFoundException("Опубликованное событие с ID " + eventId + " не найдено"));
    }
}
