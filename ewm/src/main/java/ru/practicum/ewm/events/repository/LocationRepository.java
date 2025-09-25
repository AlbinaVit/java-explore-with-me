package ru.practicum.ewm.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.events.model.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
