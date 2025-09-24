package ru.practicum.ewm.request.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.request.model.ParticipationRequest;


@Component
@RequiredArgsConstructor
public class RequestMapper {

    public ParticipationRequestDto toDto(ParticipationRequest r) {
        return ParticipationRequestDto.builder()
                .id(r.getId())
                .created(r.getCreated())
                .event(r.getEvent().getId())
                .requester(r.getRequester().getId())
                .status(r.getStatus().name())
                .build();
    }
}
