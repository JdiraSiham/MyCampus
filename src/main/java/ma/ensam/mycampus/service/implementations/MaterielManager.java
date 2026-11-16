package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.dao.repositories.MaterielRepository;
import ma.ensam.mycampus.dao.repositories.ReservationMaterielRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.MaterielService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MaterielManager implements MaterielService {

    private static final List<StatutReservation> STATUTS_BLOQUANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.VALIDEE);

    private final MaterielRepository materielRepository;
    private final ReservationMaterielRepository reservationMaterielRepository;

    public MaterielManager(MaterielRepository materielRepository,
                           ReservationMaterielRepository reservationMaterielRepository) {
        this.materielRepository = materielRepository;
        this.reservationMaterielRepository = reservationMaterielRepository;
    }

    @Override
    public List<Materiel> lister() {
        return materielRepository.findAll();
    }

    @Override
    public List<Materiel> listerActifs() {
        return materielRepository.findByActif(true);
    }

    @Override
    public Materiel trouverParId(Long id) {
        return materielRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Matériel introuvable."));
    }

    @Override
    public Materiel creer(Materiel materiel) {
        if (materielRepository.existsByReference(materiel.getReference())) {
            throw new BusinessException("Cette référence est déjà utilisée.");
        }
        if (materiel.getEtat() == null) {
            materiel.setEtat(EtatMateriel.DISPONIBLE);
        }
        if (materiel.getEtat() == EtatMateriel.EN_PANNE) {
            throw new BusinessException("Un matériel ne peut pas être créé en panne.");
        }
        materiel.setActif(true);
        return materielRepository.save(materiel);
    }

    @Override
    public Materiel modifier(Long id, Materiel donnees) {
        Materiel existant = trouverParId(id);
        if (materielRepository.existsByReferenceAndIdNot(donnees.getReference(), id)) {
            throw new BusinessException("Cette référence est déjà utilisée.");
        }
        existant.setNom(donnees.getNom());
        existant.setReference(donnees.getReference());
        existant.setDescription(donnees.getDescription());
        existant.setQuantite(donnees.getQuantite());
        return materielRepository.save(existant);
    }

    @Override
    public Materiel changerEtat(Long id, EtatMateriel nouvelEtat) {
        Materiel materiel = trouverParId(id);
        if (nouvelEtat == EtatMateriel.EN_PANNE || materiel.getEtat() == EtatMateriel.EN_PANNE) {
            throw new BusinessException(
                    "L'état EN_PANNE est géré uniquement par les signalements de panne.");
        }
        materiel.setEtat(nouvelEtat);
        return materielRepository.save(materiel);
    }

    @Override
    public void desactiver(Long id) {
        Materiel materiel = trouverParId(id);
        boolean reservationsBloquantes = reservationMaterielRepository
                .existsByMaterielIdAndStatutInAndDateFinAfter(
                        id, STATUTS_BLOQUANTS, LocalDateTime.now());
        if (reservationsBloquantes) {
            throw new BusinessException(
                    "Impossible de désactiver ce matériel : des réservations sont en cours ou à venir.");
        }
        materiel.setActif(false);
        materielRepository.save(materiel);
    }
}