package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.ReservationMateriel;
import ma.ensam.mycampus.dao.entities.SignalementPanne;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.dao.entities.enums.StatutSignalement;
import ma.ensam.mycampus.dao.repositories.MaterielRepository;
import ma.ensam.mycampus.dao.repositories.ReservationMaterielRepository;
import ma.ensam.mycampus.dao.repositories.SignalementPanneRepository;
import ma.ensam.mycampus.dao.repositories.UtilisateurRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.SignalementPanneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SignalementPanneManager implements SignalementPanneService {

    private static final List<StatutReservation> STATUTS_BLOQUANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.VALIDEE);

    // Un signalement dans l'un de ces statuts (et non résolu) bloque un nouveau signalement
    private static final List<StatutSignalement> STATUTS_EN_COURS =
            List.of(StatutSignalement.EN_ATTENTE, StatutSignalement.VALIDEE);

    private final SignalementPanneRepository signalementRepository;
    private final MaterielRepository materielRepository;
    private final ReservationMaterielRepository reservationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public SignalementPanneManager(SignalementPanneRepository signalementRepository,
                                   MaterielRepository materielRepository,
                                   ReservationMaterielRepository reservationRepository,
                                   UtilisateurRepository utilisateurRepository) {
        this.signalementRepository = signalementRepository;
        this.materielRepository = materielRepository;
        this.reservationRepository = reservationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public SignalementPanne signaler(Long auteurId, Long materielId, String description) {
        if (description == null || description.isBlank()) {
            throw new BusinessException("La description de la panne est obligatoire.");
        }

        Utilisateur auteur = utilisateurRepository.findById(auteurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!auteur.isActif()) {
            throw new BusinessException("Ce compte est désactivé.");
        }

        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> new BusinessException("Matériel introuvable."));
        if (!materiel.isActif()) {
            throw new BusinessException("Ce matériel est inactif.");
        }

        if (auteur.getRole() == Role.ETUDIANT) {
            if (materiel.getEtat() != EtatMateriel.DISPONIBLE) {
                throw new BusinessException(
                        "Vous ne pouvez signaler une panne que sur un matériel disponible.");
            }
            boolean aReserve = reservationRepository.existsByDemandeurIdAndMaterielIdAndStatut(
                    auteurId, materielId, StatutReservation.VALIDEE);
            if (!aReserve) {
                throw new BusinessException(
                        "Vous ne pouvez signaler une panne que sur un matériel que vous avez réservé (réservation validée).");
            }
        } else if (auteur.getRole() != Role.GESTIONNAIRE) {
            throw new BusinessException("Seul un étudiant ou un gestionnaire peut signaler une panne.");
        }

        if (signalementRepository.existsByMaterielIdAndResoluFalseAndStatutIn(materielId, STATUTS_EN_COURS)) {
            throw new BusinessException(
                    "Une panne est déjà signalée pour ce matériel (en attente de décision ou validée).");
        }

        SignalementPanne signalement = new SignalementPanne();
        signalement.setMateriel(materiel);
        signalement.setAuteur(auteur);
        signalement.setDescription(description);
        signalement.setResolu(false);

        if (auteur.getRole() == Role.GESTIONNAIRE) {
            // Le gestionnaire décide lui-même : la panne est validée tout de suite
            signalement.setStatut(StatutSignalement.VALIDEE);
            signalement.setDecideur(auteur);
            signalement.setDateDecision(LocalDateTime.now());
            SignalementPanne enregistre = signalementRepository.save(signalement);
            appliquerPanne(materiel);
            return enregistre;
        }

        // Étudiant : simple demande, aucun effet tant qu'un gestionnaire n'a pas validé
        signalement.setStatut(StatutSignalement.EN_ATTENTE);
        return signalementRepository.save(signalement);
    }

    @Override
    public SignalementPanne valider(Long signalementId, Long gestionnaireId) {
        Utilisateur gestionnaire = verifierGestionnaire(gestionnaireId,
                "Seul un gestionnaire actif peut valider un signalement.");

        SignalementPanne signalement = trouverParId(signalementId);
        if (signalement.getStatut() != StatutSignalement.EN_ATTENTE) {
            throw new BusinessException("Ce signalement a déjà été traité.");
        }
        Materiel materiel = signalement.getMateriel();
        if (!materiel.isActif()) {
            throw new BusinessException("Ce matériel est inactif.");
        }

        signalement.setStatut(StatutSignalement.VALIDEE);
        signalement.setDecideur(gestionnaire);
        signalement.setDateDecision(LocalDateTime.now());
        SignalementPanne enregistre = signalementRepository.save(signalement);

        // Les effets de la panne ne sont appliqués qu'ici, dans la même transaction
        appliquerPanne(materiel);
        return enregistre;
    }

    @Override
    public SignalementPanne refuser(Long signalementId, Long gestionnaireId, String motif) {
        Utilisateur gestionnaire = verifierGestionnaire(gestionnaireId,
                "Seul un gestionnaire actif peut refuser un signalement.");
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif du refus est obligatoire.");
        }

        SignalementPanne signalement = trouverParId(signalementId);
        if (signalement.getStatut() != StatutSignalement.EN_ATTENTE) {
            throw new BusinessException("Ce signalement a déjà été traité.");
        }

        signalement.setStatut(StatutSignalement.REFUSEE);
        signalement.setDecideur(gestionnaire);
        signalement.setDateDecision(LocalDateTime.now());
        signalement.setMotifRefus(motif);
        return signalementRepository.save(signalement);
    }

    @Override
    public SignalementPanne cloturer(Long signalementId, Long gestionnaireId) {
        Utilisateur gestionnaire = verifierGestionnaire(gestionnaireId,
                "Seul un gestionnaire actif peut clôturer un signalement.");

        SignalementPanne signalement = trouverParId(signalementId);
        if (signalement.getStatut() != StatutSignalement.VALIDEE) {
            throw new BusinessException("Seul un signalement validé peut être clôturé.");
        }
        if (signalement.isResolu()) {
            throw new BusinessException("Ce signalement est déjà résolu.");
        }

        signalement.setResolu(true);
        signalement.setResoluPar(gestionnaire);
        signalement.setDateResolution(LocalDateTime.now());
        SignalementPanne enregistre = signalementRepository.save(signalement);

        Materiel materiel = signalement.getMateriel();
        materiel.setEtat(EtatMateriel.DISPONIBLE);
        materielRepository.save(materiel);

        return enregistre;
    }

    @Override
    public SignalementPanne trouverParId(Long id) {
        return signalementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Signalement introuvable."));
    }

    @Override
    public List<SignalementPanne> listerTout() {
        return signalementRepository.findAll();
    }

    @Override
    public List<SignalementPanne> listerNonResolus() {
        return signalementRepository.findByResolu(false);
    }

    @Override
    public List<SignalementPanne> listerParMateriel(Long materielId) {
        return signalementRepository.findByMaterielId(materielId);
    }

    @Override
    public List<SignalementPanne> listerParStatut(StatutSignalement statut) {
        return signalementRepository.findByStatut(statut);
    }

    @Override
    public List<SignalementPanne> listerParAuteur(Long auteurId) {
        return signalementRepository.findByAuteurId(auteurId);
    }

    // ----- méthodes privées -----

    private Utilisateur verifierGestionnaire(Long gestionnaireId, String messageRefus) {
        Utilisateur gestionnaire = utilisateurRepository.findById(gestionnaireId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!gestionnaire.isActif() || gestionnaire.getRole() != Role.GESTIONNAIRE) {
            throw new BusinessException(messageRefus);
        }
        return gestionnaire;
    }

    // Effets d'une panne confirmée : matériel EN_PANNE + réservations futures annulées
    private void appliquerPanne(Materiel materiel) {
        materiel.setEtat(EtatMateriel.EN_PANNE);
        materielRepository.save(materiel);
        annulerReservationsFutures(materiel);
    }

    private void annulerReservationsFutures(Materiel materiel) {
        List<ReservationMateriel> futures = reservationRepository
                .findByMaterielIdAndStatutInAndDateDebutAfter(
                        materiel.getId(), STATUTS_BLOQUANTS, LocalDateTime.now());
        for (ReservationMateriel reservation : futures) {
            reservation.setStatut(StatutReservation.ANNULEE);
            reservation.setMotif("Annulée automatiquement : panne du matériel " + materiel.getNom());
        }
        reservationRepository.saveAll(futures);
    }
}