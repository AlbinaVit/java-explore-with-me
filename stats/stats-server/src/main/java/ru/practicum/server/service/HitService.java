package ru.practicum.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.server.exception.ValidationException;
import ru.practicum.server.model.Hit;
import ru.practicum.server.repository.HitRepository;
import ru.practicum.statsdto.dto.EndpointHitDTO;
import ru.practicum.statsdto.dto.ViewStatsDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HitService {

    private final HitRepository hitRepository;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public void createHit(EndpointHitDTO endpointHitDTO) {
        Hit hit = Hit.builder()
                .app(endpointHitDTO.getApp())
                .uri(endpointHitDTO.getUri())
                .ip(endpointHitDTO.getIp())
                .timestamp(LocalDateTime.parse(endpointHitDTO.getTimestamp(), formatter))
                .build();

        hitRepository.save(hit);
        log.info("Hit saved: {}", hit);
    }

    @Transactional(readOnly = true)
    public List<ViewStatsDTO> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        validateDateRange(start, end);
        List<ViewStatsDTO> result;
        if (Boolean.TRUE.equals(unique)) {
             result =  hitRepository.getUniqueStats(start, end, uris);
        //    return result
        } else {
           result = hitRepository.getStats(start, end, uris);
        }
        log.info("getStats result: {}", result);
        return result;
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }
    }
}