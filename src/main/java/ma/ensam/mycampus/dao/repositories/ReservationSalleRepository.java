package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.ReservationSalle;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationSalleRepository extends JpaRepository<ReservationSalle, Long> {

    List<ReservationSalle> findByDemandeurId(Long demandeurId);

    List<ReservationSalle> findBySalleId(Long salleId);

    List<ReservationSalle> findByStatut(StatutReservation statut);
}