package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.ReservationSalle;
import ma.ensam.mycampus.dao.entities.Salle;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.dao.repositories.ReservationSalleRepository;
import ma.ensam.mycampus.dao.repositories.SalleRepository;
import ma.ensam.mycampus.dao.repositories.UtilisateurRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.ReservationSalleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationSalleManager implements ReservationSalleService {

    private static final List<StatutReservation> STATUTS_BLOQUANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.VALIDEE);

    private final ReservationSalleRepository reservationRepository;
    private final SalleRepository salleRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ReservationSalleManager(ReservationSalleRepository reservationRepository,
                                   SalleRepository salleRepository,
                                   UtilisateurRepository utilisateurRepository) {
        this.reservationRepository = reservationRepository;
        this.salleRepository = salleRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public ReservationSalle creer(Long demandeurId, Long salleId,
                                  LocalDateTime dateDebut, LocalDateTime dateFin, String objet) {
        if (dateDebut == null || dateFin == null) {
            throw new BusinessException("Les dates de début et de fin sont obligatoires.");
        }
        if (!dateDebut.isBefore(dateFin)) {
            throw new BusinessException("La date de début doit être antérieure à la date de fin.");
        }
        if (!dateDebut.isAfter(LocalDateTime.now())) {
            throw new BusinessException("La date de début doit être dans le futur.");
        }
        if (objet == null || objet.isBlank()) {
            throw new BusinessException("L'objet de la réservation est obligatoire.");
        }

        Utilisateur demandeur = utilisateurRepository.findById(demandeurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!demandeur.isActif()) {
            throw new BusinessException("Ce compte est désactivé.");
        }

        Salle salle = salleRepository.findById(salleId)
                .orElseThrow(() -> new BusinessException("Salle introuvable."));
        if (!salle.isActif()) {
            throw new BusinessException("Cette salle est inactive.");
        }

        if (estOccupee(salle.getId(), dateDebut, dateFin)) {
            throw new BusinessException("Cette salle est déjà réservée sur cette période.");
        }

        ReservationSalle reservation = new ReservationSalle();
        reservation.setDemandeur(demandeur);
        reservation.setSalle(salle);
        reservation.setDateDebut(dateDebut);
        reservation.setDateFin(dateFin);
        reservation.setObjet(objet);
        // Validation automatique : statut VALIDEE et aucun décideur humain.
        reservation.setStatut(salle.isNecessiteValidation()
                ? StatutReservation.EN_ATTENTE
                : StatutReservation.VALIDEE);
        return reservationRepository.save(reservation);
    }

    @Override
    public ReservationSalle trouverParId(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation introuvable."));
    }

    @Override
    public List<ReservationSalle> listerTout() {
        return reservationRepository.findAll();
    }

    @Override
    public List<ReservationSalle> listerParDemandeur(Long demandeurId) {
        return reservationRepository.findByDemandeurId(demandeurId);
    }

    @Override
    public List<ReservationSalle> listerParStatut(StatutReservation statut) {
        return reservationRepository.findByStatut(statut);
    }

    @Override
    public List<Salle> rechercherSallesDisponibles(LocalDateTime dateDebut, LocalDateTime dateFin) {
        if (dateDebut == null || dateFin == null || !dateDebut.isBefore(dateFin)) {
            throw new BusinessException("La période de recherche est invalide.");
        }
        return salleRepository.findByActif(true).stream()
                .filter(salle -> !estOccupee(salle.getId(), dateDebut, dateFin))
                .toList();
    }

    @Override
    public ReservationSalle valider(Long id, Long decideurId) {
        Utilisateur decideur = chargerGestionnaire(decideurId);
        ReservationSalle reservation = chargerEnAttente(id);
        reservation.setStatut(StatutReservation.VALIDEE);
        reservation.setDecideur(decideur);
        reservation.setDateDecision(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Override
    public ReservationSalle refuser(Long id, Long decideurId, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Un motif est obligatoire pour refuser une réservation.");
        }
        Utilisateur decideur = chargerGestionnaire(decideurId);
        ReservationSalle reservation = chargerEnAttente(id);
        reservation.setStatut(StatutReservation.REFUSEE);
        reservation.setMotif(motif);
        reservation.setDecideur(decideur);
        reservation.setDateDecision(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Override
    public ReservationSalle annuler(Long id, Long utilisateurId) {
        ReservationSalle reservation = trouverParId(id);
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
    @Override
    public void annulerReservationsBloquantes(Long etudiantId) {
        List<ReservationSalle> aAnnuler = reservationRepository
                .findByDemandeurIdAndStatutInAndDateFinAfter(
                        etudiantId, STATUTS_BLOQUANTS, LocalDateTime.now());
        for (ReservationSalle reservation : aAnnuler) {
            reservation.setStatut(StatutReservation.ANNULEE);
            reservation.setMotif("Annulée automatiquement : compte de l'étudiant désactivé");
        }
        reservationRepository.saveAll(aAnnuler);
    }

    // ----- méthodes privées -----

    private boolean estOccupee(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        return reservationRepository
                .existsBySalleIdAndStatutInAndDateDebutBeforeAndDateFinAfter(
                        salleId, STATUTS_BLOQUANTS, fin, debut);
    }

    private Utilisateur chargerGestionnaire(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!utilisateur.isActif() || utilisateur.getRole() != Role.GESTIONNAIRE) {
            throw new BusinessException("Seul un gestionnaire actif peut effectuer cette action.");
        }
        return utilisateur;
    }

    private ReservationSalle chargerEnAttente(Long id) {
        ReservationSalle reservation = trouverParId(id);
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seule une réservation EN_ATTENTE peut être traitée.");
        }
        return reservation;
    }
}