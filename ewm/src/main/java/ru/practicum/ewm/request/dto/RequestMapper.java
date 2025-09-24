package ru.practicum.ewm.request.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.request.model.ParticipationRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestMapper {

    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ParticipationRequestDto toRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .status(request.getStatus().name())
                .requester(request.getRequester().getId())
                .event(request.getEvent().getId())
                .build();
    }

    public EventRequestStatusUpdateResult toUpdateResultDto(
            List<ParticipationRequest> confirmedRequests,
            List<ParticipationRequest> rejectedRequests) {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream()
                        .map(this::toRequestDto)
                        .collect(Collectors.toList()))
                .rejectedRequests(rejectedRequests.stream()
                        .map(this::toRequestDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public List<ParticipationRequestDto> toRequestDtoList(List<ParticipationRequest> requests) {
        return requests.stream()
                .map(this::toRequestDto)
                .collect(Collectors.toList());
    }

    public static ParticipationRequestDto toDto(ParticipationRequest r) {
        return ParticipationRequestDto.builder()
                .id(r.getId())
                .created(r.getCreated())
                .event(r.getEvent().getId())
                .requester(r.getRequester().getId())
                .status(r.getStatus().name())
                .build();
    }
}
