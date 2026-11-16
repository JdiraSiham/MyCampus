package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.Salle;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.dao.repositories.ReservationSalleRepository;
import ma.ensam.mycampus.dao.repositories.SalleRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.SalleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SalleManager implements SalleService {

    private static final List<StatutReservation> STATUTS_BLOQUANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.VALIDEE);

    private final SalleRepository salleRepository;
    private final ReservationSalleRepository reservationSalleRepository;

    public SalleManager(SalleRepository salleRepository,
                        ReservationSalleRepository reservationSalleRepository) {
        this.salleRepository = salleRepository;
        this.reservationSalleRepository = reservationSalleRepository;
    }

    @Override
    public List<Salle> lister() {
        return salleRepository.findAll();
    }

    @Override
    public List<Salle> listerActives() {
        return salleRepository.findByActif(true);
    }

    @Override
    public Salle trouverParId(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Salle introuvable."));
    }

    @Override
    public Salle creer(Salle salle) {
        if (salleRepository.existsByNom(salle.getNom())) {
            throw new BusinessException("Une salle porte déjà ce nom.");
        }
        salle.setActif(true);
        return salleRepository.save(salle);
    }

    @Override
    public Salle modifier(Long id, Salle donnees) {
        Salle existante = trouverParId(id);
        if (salleRepository.existsByNomAndIdNot(donnees.getNom(), id)) {
            throw new BusinessException("Une salle porte déjà ce nom.");
        }
        existante.setNom(donnees.getNom());
        existante.setCapacite(donnees.getCapacite());
        existante.setLocalisation(donnees.getLocalisation());
        existante.setEquipements(donnees.getEquipements());
        existante.setNecessiteValidation(donnees.isNecessiteValidation());
        return salleRepository.save(existante);
    }

    @Override
    public void desactiver(Long id) {
        Salle salle = trouverParId(id);
        boolean reservationsBloquantes = reservationSalleRepository
                .existsBySalleIdAndStatutInAndDateFinAfter(
                        id, STATUTS_BLOQUANTS, LocalDateTime.now());
        if (reservationsBloquantes) {
            throw new BusinessException(
                    "Impossible de désactiver cette salle : des réservations sont en cours ou à venir.");
        }
        salle.setActif(false);
        salleRepository.save(salle);
    }
}