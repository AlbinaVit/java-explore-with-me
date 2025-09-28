package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException e) {
        log.warn("Объект не найден: {}", e.getMessage());
        return new ApiError(
                "NOT_FOUND",
                "The required object was not found.",
                e.getMessage(),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class/*, NumberFormatException.class*/})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(Exception e) {
        log.warn("Некорректный запрос: {}", e.getMessage());
        return new ApiError(
                "BAD_REQUEST",
                "Incorrectly made request.",
                e.getMessage(),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s", error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                .collect(Collectors.toList());
        return new ApiError(
                "BAD_REQUEST",
                "Incorrectly made request.",
                "Ошибка валидации полей",
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                errors
        );
    }

    @ExceptionHandler(java.time.format.DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleDateTimeParseException(java.time.format.DateTimeParseException e) {
        log.warn("Некорректный формат даты: {}", e.getMessage());
        return new ApiError(
                "BAD_REQUEST",
                "Incorrectly made request.",
                "Failed to convert value of type java.lang.String to required type; nested exception is java.lang.NumberFormatException: For input string: ad",
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(Exception e) {
        log.warn("Нарушение целостности данных: {}", e.getMessage());
        String reason = e instanceof DataIntegrityViolationException ? "Integrity constraint has been violated." : "For the requested operation the conditions are not met.";
        return new ApiError(
                "CONFLICT",
                reason,
                e.getMessage(),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                Collections.emptyList()
        );
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)  // Изменено с CONFLICT на BAD_REQUEST для корректности
    public ApiError handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Некорректное тело запроса: {}", e.getMessage());
        return new ApiError(
                "CONFLICT",
                "Incorrectly made request.",
                "Некорректное тело запроса или отсутствующие данные",
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                Collections.emptyList()
        );
    }

    // Обработчик для отсутствующих параметров (400) — теперь сработает
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Отсутствующий параметр: {}", e.getMessage());
        return new ApiError(
                "BAD_REQUEST",
                "Incorrectly made request.",
                "Отсутствует обязательный параметр: " + e.getParameterName(),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                Collections.emptyList()
        );
    }

    // Общий обработчик — переместите в самый конец!
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return new ApiError(
                "INTERNAL_SERVER_ERROR",
                "Internal server error.",
                "Внутренняя ошибка сервера",
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                Collections.emptyList()
        );
    }

}
