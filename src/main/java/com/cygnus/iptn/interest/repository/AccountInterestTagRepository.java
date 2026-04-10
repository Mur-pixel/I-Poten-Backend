package com.cygnus.iptn.interest.repository;

import com.cygnus.iptn.interest.entity.AccountInterestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountInterestTagRepository extends JpaRepository<AccountInterestTag, Long> {

    List<AccountInterestTag> findByAccountId(Long accountId);

    void deleteByAccountId(Long accountId);
}
