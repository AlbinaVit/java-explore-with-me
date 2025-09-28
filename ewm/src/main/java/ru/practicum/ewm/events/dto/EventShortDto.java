package ru.practicum.ewm.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.ewm.users.dto.UserShortDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {

    @NotBlank(message = "Annotation must not be blank")
    private String annotation;

    @NotNull(message = "Category must not be null")
    private CategoryDto category;

    private Long confirmedRequests;

    @NotBlank(message = "Event date must not be blank")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String eventDate;

    private Long id;

    @NotNull(message = "Initiator must not be null")
    private UserShortDto initiator;

    @NotNull(message = "Paid must not be null")
    private Boolean paid;

    @NotBlank(message = "Title must not be blank")
    private String title;

    private Long views;

    private Long commentCount;
}
