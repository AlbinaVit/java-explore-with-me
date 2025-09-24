package ru.practicum.statsclient.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.statsdto.dto.EndpointHitDTO;
import ru.practicum.statsdto.dto.ViewStatsDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class StatsClient {
    private final RestClient restClient;

    public StatsClient(String serverUrl) {
        restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void saveHit(EndpointHitDTO endpointHitDto) {
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(endpointHitDto)
                .retrieve()
                .toBodilessEntity();
    }

    public List<ViewStatsDTO> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("List<ViewStatsDTO> getStats start: {}, end: {}", start, end);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl("/stats")
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            uriBuilder.queryParam("uris", String.join(",", uris));
        }

        String url = uriBuilder.toUriString();
        log.info("url: {}", url);
        ViewStatsDTO[] response = restClient.get()
                .uri(url)
                .retrieve()
                .body(ViewStatsDTO[].class);
        log.info("iewStatsDTO[] response", Arrays.toString(response));
        return Arrays.asList(response);
    }

}