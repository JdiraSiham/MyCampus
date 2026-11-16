package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.ReservationSalle;
import ma.ensam.mycampus.dao.entities.Salle;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationSalleService {

    ReservationSalle creer(Long demandeurId, Long salleId,
                           LocalDateTime dateDebut, LocalDateTime dateFin, String objet);

    ReservationSalle trouverParId(Long id);

    List<ReservationSalle> listerTout();

    List<ReservationSalle> listerParDemandeur(Long demandeurId);

    List<ReservationSalle> listerParStatut(StatutReservation statut);

    List<Salle> rechercherSallesDisponibles(LocalDateTime dateDebut, LocalDateTime dateFin);

    ReservationSalle valider(Long id, Long decideurId);

    ReservationSalle refuser(Long id, Long decideurId, String motif);

    ReservationSalle annuler(Long id, Long utilisateurId);
    void annulerReservationsBloquantes(Long etudiantId);
}