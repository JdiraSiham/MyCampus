package ma.ensam.mycampus.dao.repositories;

import ma.ensam.mycampus.dao.entities.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TypeDocumentRepository extends JpaRepository<TypeDocument, Long> {
    boolean existsByLibelle(String libelle);
    List<TypeDocument> findByActif(boolean actif);
}