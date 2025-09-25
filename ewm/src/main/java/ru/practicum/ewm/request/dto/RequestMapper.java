package ru.practicum.ewm.request.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.users.model.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


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

    public ParticipationRequest toRequest(Event event,
                                          User user,
                                          RequestStatus status) {
        return ParticipationRequest.builder()
                .created(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .event(event)
                .requester(user)
                .status(status)
                .build();
    }
}
