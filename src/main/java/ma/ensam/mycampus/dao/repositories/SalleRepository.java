package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.Salle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalleRepository extends JpaRepository<Salle, Long> {

    boolean existsByNom(String nom);

    List<Salle> findByActif(boolean actif);
}