package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.ReservationMateriel;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.dao.repositories.MaterielRepository;
import ma.ensam.mycampus.dao.repositories.ReservationMaterielRepository;
import ma.ensam.mycampus.dao.repositories.UtilisateurRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.ReservationMaterielService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ReservationMaterielManager implements ReservationMaterielService {

    private static final List<StatutReservation> STATUTS_BLOQUANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.VALIDEE);

    private final ReservationMaterielRepository reservationRepository;
    private final MaterielRepository materielRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ReservationMaterielManager(ReservationMaterielRepository reservationRepository,
                                      MaterielRepository materielRepository,
                                      UtilisateurRepository utilisateurRepository) {
        this.reservationRepository = reservationRepository;
        this.materielRepository = materielRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public ReservationMateriel creer(Long demandeurId, Long materielId,
                                     LocalDateTime dateDebut, LocalDateTime dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new BusinessException("Les dates de début et de fin sont obligatoires.");
        }
        if (!dateDebut.isBefore(dateFin)) {
            throw new BusinessException("La date de début doit être antérieure à la date de fin.");
        }
        if (!dateDebut.isAfter(LocalDateTime.now())) {
            throw new BusinessException("La date de début doit être dans le futur.");
        }

        Utilisateur demandeur = utilisateurRepository.findById(demandeurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!demandeur.isActif()) {
            throw new BusinessException("Ce compte est désactivé.");
        }

        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new BusinessException("Matériel introuvable."));
        if (!materiel.isActif()) {
            throw new BusinessException("Ce matériel est inactif.");
        }
        if (materiel.getEtat() != EtatMateriel.DISPONIBLE) {
            throw new BusinessException("Ce matériel n'est pas disponible (état : "
                    + materiel.getEtat() + ").");
        }

        verifierQuantite(materiel, dateDebut, dateFin);

        ReservationMateriel reservation = new ReservationMateriel();
        reservation.setDemandeur(demandeur);
        reservation.setMateriel(materiel);
        reservation.setDateDebut(dateDebut);
        reservation.setDateFin(dateFin);
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        return reservationRepository.save(reservation);
    }

    @Override
    public ReservationMateriel trouverParId(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation introuvable."));
    }

    @Override
    public List<ReservationMateriel> listerTout() {
        return reservationRepository.findAll();
    }

    @Override
    public List<ReservationMateriel> listerParDemandeur(Long demandeurId) {
        return reservationRepository.findByDemandeurId(demandeurId);
    }

    @Override
    public List<ReservationMateriel> listerParStatut(StatutReservation statut) {
        return reservationRepository.findByStatut(statut);
    }

    @Override
    public ReservationMateriel valider(Long id, Long decideurId) {
        Utilisateur decideur = chargerGestionnaire(decideurId);
        ReservationMateriel reservation = chargerEnAttente(id);
        reservation.setStatut(StatutReservation.VALIDEE);
        reservation.setDecideur(decideur);
        reservation.setDateDecision(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Override
    public ReservationMateriel refuser(Long id, Long decideurId, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Un motif est obligatoire pour refuser une réservation.");
        }
        Utilisateur decideur = chargerGestionnaire(decideurId);
        ReservationMateriel reservation = chargerEnAttente(id);
        reservation.setStatut(StatutReservation.REFUSEE);
        reservation.setMotif(motif);
        reservation.setDecideur(decideur);
        reservation.setDateDecision(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Override
    public ReservationMateriel annuler(Long id, Long utilisateurId) {
        ReservationMateriel reservation = trouverParId(id);
        if (!reservation.getDemandeur().getId().equals(utilisateurId)) {
            throw new BusinessException("Vous ne pouvez annuler que vos propres réservations.");
        }
        if (!STATUTS_BLOQUANTS.contains(reservation.getStatut())) {
            throw new BusinessException("Cette réservation ne peut plus être annulée.");
        }
        if (!reservation.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Impossible d'annuler une réservation déjà commencée.");
        }
        reservation.setStatut(StatutReservation.ANNULEE);
        return reservationRepository.save(reservation);
    }

    // ----- méthodes privées -----

    private void verifierQuantite(Materiel materiel, LocalDateTime debut, LocalDateTime fin) {
        List<ReservationMateriel> chevauchantes = reservationRepository
                .findByMaterielIdAndStatutInAndDateDebutBeforeAndDateFinAfter(
                        materiel.getId(), STATUTS_BLOQUANTS, fin, debut);

        // Le nombre d'exemplaires utilisés ne change qu'au début d'une réservation :
        // on teste donc le début demandé et chaque début de réservation existante.
        List<LocalDateTime> instants = new ArrayList<>();
        instants.add(debut);
        for (ReservationMateriel r : chevauchantes) {
            if (r.getDateDebut().isAfter(debut)) {
                instants.add(r.getDateDebut());
            }
        }

        for (LocalDateTime instant : instants) {
            long utilises = chevauchantes.stream()
                    .filter(r -> !r.getDateDebut().isAfter(instant)
                            && r.getDateFin().isAfter(instant))
                    .count();
            if (utilises + 1 > materiel.getQuantite()) {
                throw new BusinessException(
                        "Aucun exemplaire n'est disponible sur cette période.");
            }
        }
    }

    private Utilisateur chargerGestionnaire(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!utilisateur.isActif() || utilisateur.getRole() != Role.GESTIONNAIRE) {
            throw new BusinessException("Seul un gestionnaire actif peut effectuer cette action.");
        }
        return utilisateur;
    }

    private ReservationMateriel chargerEnAttente(Long id) {
        ReservationMateriel reservation = trouverParId(id);
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seule une réservation EN_ATTENTE peut être traitée.");
        }
        return reservation;
    }
}