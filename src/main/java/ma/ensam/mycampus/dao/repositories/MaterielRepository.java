package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.Materiel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterielRepository extends JpaRepository<Materiel, Long> {

    boolean existsByReference(String reference);

    List<Materiel> findByActif(boolean actif);
}