package com.cygnus.ipoten.credit.repository;

import com.cygnus.ipoten.credit.entity.CreditTransaction;
import org.springframework.data.repository.CrudRepository;

public interface CreditTransactionRepository extends CrudRepository<CreditTransaction, Long> {
}
