package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.ReservationMateriel;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ReservationMaterielRepository extends JpaRepository<ReservationMateriel, Long> {

    List<ReservationMateriel> findByDemandeurId(Long demandeurId);

    List<ReservationMateriel> findByMaterielId(Long materielId);

    List<ReservationMateriel> findByStatut(StatutReservation statut);

    boolean existsByMaterielIdAndStatutInAndDateFinAfter(
            Long materielId, List<StatutReservation> statuts, LocalDateTime date);
    List<ReservationMateriel> findByMaterielIdAndStatutInAndDateDebutBeforeAndDateFinAfter(
            Long materielId, List<StatutReservation> statuts,
            LocalDateTime fin, LocalDateTime debut);
}