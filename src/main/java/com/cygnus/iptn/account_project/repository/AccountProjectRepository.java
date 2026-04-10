package com.cygnus.iptn.account_project.repository;

import com.cygnus.iptn.account_project.entity.AccountProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountProjectRepository extends JpaRepository<AccountProject, Long> {

    List<AccountProject> findAllByAccount_IdAndIsActiveTrue(Long accountId);

}
