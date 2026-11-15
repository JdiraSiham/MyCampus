package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.SignalementPanne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalementPanneRepository extends JpaRepository<SignalementPanne, Long> {

    List<SignalementPanne> findByMaterielId(Long materielId);

    List<SignalementPanne> findByResolu(boolean resolu);

    boolean existsByMaterielIdAndResoluFalse(Long materielId);
}