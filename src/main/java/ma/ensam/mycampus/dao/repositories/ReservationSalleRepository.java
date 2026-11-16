package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.ReservationSalle;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ReservationSalleRepository extends JpaRepository<ReservationSalle, Long> {

    List<ReservationSalle> findByDemandeurId(Long demandeurId);

    List<ReservationSalle> findBySalleId(Long salleId);

    List<ReservationSalle> findByStatut(StatutReservation statut);

    boolean existsBySalleIdAndStatutInAndDateFinAfter(
            Long salleId, List<StatutReservation> statuts, LocalDateTime date);

    boolean existsBySalleIdAndStatutInAndDateDebutBeforeAndDateFinAfter(
            Long salleId, List<StatutReservation> statuts,
            LocalDateTime fin, LocalDateTime debut);
    List<ReservationSalle> findByDemandeurIdAndStatutInAndDateFinAfter(
            Long demandeurId, List<StatutReservation> statuts, LocalDateTime date);
}