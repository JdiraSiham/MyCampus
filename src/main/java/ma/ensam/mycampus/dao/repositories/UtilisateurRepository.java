package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.ensam.mycampus.dao.entities.enums.Role;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    List<Utilisateur> findByRole(Role role, Sort sort);
}