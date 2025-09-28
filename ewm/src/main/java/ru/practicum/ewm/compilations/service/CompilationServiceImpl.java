package ru.practicum.ewm.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.categories.dto.CategoriesMapper;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.compilations.dto.CompilationDto;
import ru.practicum.ewm.compilations.dto.CompilationMapper;
import ru.practicum.ewm.compilations.dto.NewCompilationDto;
import ru.practicum.ewm.compilations.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilations.model.Compilation;
import ru.practicum.ewm.compilations.repository.CompilationRepository;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.events.dto.mapper.EventMapper;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.users.dto.UserMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final CategoriesMapper categoriesMapper;
    private final UserMapper userMapper;
    private final CommentRepository commentRepository;

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        // Проверка уникальности названия
        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConflictException("Подборка с названием '" + newCompilationDto.getTitle() + "' уже существует");
        }

        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

        // Добавляем события если они указаны
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
            compilation.setEvents(events);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Создана новая подборка: {}", savedCompilation.getTitle());

        return convertToDtoWithEvents(savedCompilation);
    }

    @Override
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationIfExists(compId);
        compilationRepository.delete(compilation);
        log.info("Удалена подборка с id: {}", compId);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = getCompilationIfExists(compId);

        // Проверка уникальности названия
        if (updateRequest.getTitle() != null &&
                compilationRepository.existsByTitleAndIdNot(updateRequest.getTitle(), compId)) {
            throw new ConflictException("Подборка с названием '" + updateRequest.getTitle() + "' уже существует");
        }

        // Обновление полей
        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }
        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateRequest.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Обновлена подборка с id: {}", compId);

        return convertToDtoWithEvents(updatedCompilation);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from / size, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, page).getContent();
        } else {
            compilations = compilationRepository.findAll(page).getContent();
        }

        log.info("Найдено {} подборок", compilations.size());
        return compilations.stream()
                .map(this::convertToDtoWithEvents)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = getCompilationIfExists(compId);
        log.info("Найдена подборка: {}", compilation.getTitle());
        return convertToDtoWithEvents(compilation);
    }

    private CompilationDto convertToDtoWithEvents(Compilation compilation) {
        Set<Event> events = compilation.getEvents();
        Map<Long, Long> commentCounts = new HashMap<>();
        if (!events.isEmpty()) {
            List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
            List<Object[]> counts = commentRepository.findCommentCountsByEventIds(eventIds);
            for (Object[] count : counts) {
                commentCounts.put((Long) count[0], (Long) count[1]);
            }
        }

        Set<EventShortDto> eventDtos = events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        categoriesMapper.toCategoryDto(event.getCategory()),
                        userMapper.toUserShortDto(event.getInitiator()),
                        commentCounts.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toSet());

        return compilationMapper.toCompilationDto(compilation, eventDtos);
    }

    private Compilation getCompilationIfExists(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
    }
}
