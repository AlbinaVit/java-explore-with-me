package ru.practicum.ewm.events.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewEventDto {
    @NotBlank(message = "Annotation must not be blank")
    @Size(min = 20, max = 2000, message = "Annotation must be between 20 and 2000 characters")
    private String annotation;  // Краткое описание

    @NotNull(message = "Category must not be null")
    private Long category;  // ID категории

    @NotBlank(message = "Description must not be blank")
    @Size(min = 20, max = 7000, message = "Description must be between 20 and 7000 characters")
    private String description;  // Полное описание

    @NotBlank(message = "Event date must not be blank")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String eventDate;  // Дата и время события

    @NotNull(message = "Location must not be null")
    @Valid
    private LocationDto location;  // Местоположение

    private Boolean paid;  // Платность (default false)

    @PositiveOrZero(message = "Лимит участников не может быть отрицательным")
    private Integer participantLimit;  // Лимит участников (default 0)

    private Boolean requestModeration;  // Пре-модерация (default true)

    @NotBlank(message = "Title must not be blank")
    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    private String title;  // Заголовок
}
