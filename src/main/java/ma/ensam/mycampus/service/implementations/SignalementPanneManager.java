package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.ReservationMateriel;
import ma.ensam.mycampus.dao.entities.SignalementPanne;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
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

        if (signalementRepository.existsByMaterielIdAndResoluFalse(materielId)) {
            throw new BusinessException("Une panne non résolue est déjà signalée pour ce matériel.");
        }

        // 1. Création du signalement
        SignalementPanne signalement = new SignalementPanne();
        signalement.setMateriel(materiel);
        signalement.setAuteur(auteur);
        signalement.setDescription(description);
        signalement.setResolu(false);
        SignalementPanne enregistre = signalementRepository.save(signalement);

        // 2. Le matériel passe EN_PANNE
        materiel.setEtat(EtatMateriel.EN_PANNE);
        materielRepository.save(materiel);

        // 3. Annulation des réservations futures (avec un motif)
        annulerReservationsFutures(materiel);

        return enregistre;
    }

    @Override
    public SignalementPanne cloturer(Long signalementId, Long gestionnaireId) {
        Utilisateur gestionnaire = utilisateurRepository.findById(gestionnaireId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!gestionnaire.isActif() || gestionnaire.getRole() != Role.GESTIONNAIRE) {
            throw new BusinessException("Seul un gestionnaire actif peut clôturer un signalement.");
        }

        SignalementPanne signalement = trouverParId(signalementId);
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

    // ----- méthode privée -----

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