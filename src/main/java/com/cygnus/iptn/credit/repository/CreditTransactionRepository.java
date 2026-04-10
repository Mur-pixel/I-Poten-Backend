package com.cygnus.iptn.credit.repository;

import com.cygnus.iptn.credit.entity.CreditTransaction;
import org.springframework.data.repository.CrudRepository;

public interface CreditTransactionRepository extends CrudRepository<CreditTransaction, Long> {
}
