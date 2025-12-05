package com.cygnus.ipoten.userSchedule.repository;

import com.cygnus.ipoten.userSchedule.entity.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {

    List<UserSchedule> findAllByAccountId(Long accountId);
    void deleteAllByAccountId(Long accountId);
}
