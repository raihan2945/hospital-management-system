package com.example.hms.repository;

import com.example.hms.domain.Bill;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {
    @Override
    @EntityGraph(attributePaths = {"patient", "appointment"})
    Page<Bill> findAll(Specification<Bill> specification, Pageable pageable);
    @Override
    @EntityGraph(attributePaths = {"patient", "appointment"})
    Optional<Bill> findById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findForUpdate(Long id);
    boolean existsByPatientId(Long id);
    boolean existsByAppointmentId(Long id);
    Optional<Bill> findByAppointmentId(Long id);
}
