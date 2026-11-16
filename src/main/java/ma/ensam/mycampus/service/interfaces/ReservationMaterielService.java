package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.ReservationMateriel;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationMaterielService {

    ReservationMateriel creer(Long demandeurId, Long materielId,
                              LocalDateTime dateDebut, LocalDateTime dateFin);

    ReservationMateriel trouverParId(Long id);

    List<ReservationMateriel> listerTout();

    List<ReservationMateriel> listerParDemandeur(Long demandeurId);

    List<ReservationMateriel> listerParStatut(StatutReservation statut);

    ReservationMateriel valider(Long id, Long decideurId);

    ReservationMateriel refuser(Long id, Long decideurId, String motif);

    ReservationMateriel annuler(Long id, Long utilisateurId);
}