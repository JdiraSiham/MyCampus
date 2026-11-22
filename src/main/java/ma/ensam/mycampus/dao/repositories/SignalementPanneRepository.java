package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.SignalementPanne;
import ma.ensam.mycampus.dao.entities.enums.StatutSignalement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SignalementPanneRepository extends JpaRepository<SignalementPanne, Long> {

    List<SignalementPanne> findByMaterielId(Long materielId);

    List<SignalementPanne> findByResolu(boolean resolu);

    boolean existsByMaterielIdAndResoluFalse(Long materielId);

    // Signalements d'un étudiant (page « Mes signalements »)
    List<SignalementPanne> findByAuteurId(Long auteurId);

    // Filtre de la liste du gestionnaire
    List<SignalementPanne> findByStatut(StatutSignalement statut);

    // Une panne en attente ou validée et non résolue bloque un nouveau signalement
    // (un signalement refusé ne bloque pas)
    boolean existsByMaterielIdAndResoluFalseAndStatutIn(Long materielId,
                                                        Collection<StatutSignalement> statuts);
}