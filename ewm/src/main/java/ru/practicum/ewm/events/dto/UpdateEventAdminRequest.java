package ru.practicum.ewm.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequest {
    @Size(min = 20, max = 2000, message = "Annotation must be between 20 and 2000 characters")
    private String annotation;  // Новая аннотация

    private Long category;  // Новая категория (ID)

    @Size(min = 20, max = 7000, message = "Description must be between 20 and 7000 characters")
    private String description;  // Новое описание

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String eventDate;  // Новая дата события

    @Valid
    private LocationDto location;  // Новое местоположение

    private Boolean paid;  // Новая платность

    private Integer participantLimit;  // Новый лимит участников

    private Boolean requestModeration;  // Новая пре-модерация

    private String stateAction;  // Действие со статусом (PUBLISH_EVENT, REJECT_EVENT)

    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    private String title;  // Новый заголовок
}
