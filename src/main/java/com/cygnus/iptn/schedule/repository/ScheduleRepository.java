package com.cygnus.iptn.schedule.repository;

import com.cygnus.iptn.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndAccount_Id(Long id, Long accountId);

    List<Schedule> findAllByAccount_IdOrderByStartAtAscIdAsc(Long accountId);

    List<Schedule> findAllByAccount_IdAndEndAtGreaterThanEqualAndStartAtLessThanEqualOrderByStartAtAscIdAsc(
            Long accountId,
            Instant from,
            Instant to
    );
}
