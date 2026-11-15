package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.ReservationMateriel;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationMaterielRepository extends JpaRepository<ReservationMateriel, Long> {

    List<ReservationMateriel> findByDemandeurId(Long demandeurId);

    List<ReservationMateriel> findByMaterielId(Long materielId);

    List<ReservationMateriel> findByStatut(StatutReservation statut);
}