package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.DemandeAdministrative;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;

public interface DemandeRepository extends JpaRepository<DemandeAdministrative, Long> {
    List<DemandeAdministrative> findByEtudiant(Utilisateur etudiant);
    List<DemandeAdministrative> findByStatut(StatutDemande statut);
    List<DemandeAdministrative> findByEtudiant(Utilisateur etudiant, Sort sort);
}