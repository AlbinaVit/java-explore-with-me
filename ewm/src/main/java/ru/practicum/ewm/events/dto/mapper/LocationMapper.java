package ru.practicum.ewm.events.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.events.dto.LocationDto;
import ru.practicum.ewm.events.model.Location;

@Component
@RequiredArgsConstructor
public class LocationMapper {
    public LocationDto toLocationDto(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationDto(location.getLat(), location.getLon());
    }

    public Location toLocation(LocationDto locationDto) {
        if (locationDto == null) {
            return null;
        }
        return new Location(null, locationDto.getLat(), locationDto.getLon());
    }
}
