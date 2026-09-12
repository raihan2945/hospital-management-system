package com.example.hms.repository;

import com.example.hms.domain.Appointment;
import com.example.hms.domain.enums.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    long countByAppointmentDate(LocalDate date);
    long countByStatus(AppointmentStatus status);
    long countByStatusIn(java.util.Collection<AppointmentStatus> statuses);

    @EntityGraph(attributePaths = {"patient", "doctor"})
    java.util.List<Appointment> findTop5ByOrderByCreatedAtDescIdDesc();

    @Query("select a from Appointment a join fetch a.patient join fetch a.doctor "
            + "where a.status <> com.example.hms.domain.enums.AppointmentStatus.CANCELLED "
            + "and not exists (select b.id from Bill b where b.appointment = a) "
            + "order by a.appointmentDate desc, a.appointmentTime desc, a.id desc")
    java.util.List<Appointment> findUnbilled();

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor"})
    Page<Appointment> findAll(Specification<Appointment> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor"})
    Optional<Appointment> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findForUpdate(Long id);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long doctorId, LocalDate date, LocalTime time, AppointmentStatus excludedStatus);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
            Long doctorId, LocalDate date, LocalTime time, AppointmentStatus excludedStatus, Long excludedId);

    boolean existsByPatientId(Long patientId);
    boolean existsByDoctorId(Long doctorId);
}
