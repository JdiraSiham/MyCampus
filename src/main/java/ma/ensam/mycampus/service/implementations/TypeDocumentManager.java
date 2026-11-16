package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.TypeDocument;
import ma.ensam.mycampus.dao.repositories.TypeDocumentRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.TypeDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TypeDocumentManager implements TypeDocumentService {

    private final TypeDocumentRepository typeDocumentRepository;

    @Autowired
    public TypeDocumentManager(TypeDocumentRepository typeDocumentRepository) {
        this.typeDocumentRepository = typeDocumentRepository;
    }

    @Override
    public List<TypeDocument> listerTous() {
        return typeDocumentRepository.findAll();
    }

    @Override
    public List<TypeDocument> listerActifs() {
        return typeDocumentRepository.findByActif(true);
    }

    @Override
    public TypeDocument trouverParId(Long id) {
        return typeDocumentRepository.findById(id).orElseThrow(() -> new BusinessException("Type de document introuvable."));
    }

    @Override
    public TypeDocument creer(TypeDocument typeDocument) {
        String libelle = nettoyer(typeDocument.getLibelle());
        if (typeDocumentRepository.existsByLibelle(libelle)) {
            throw new BusinessException("Ce libellé existe déjà.");
        }
        typeDocument.setId(null);
        typeDocument.setLibelle(libelle);
        typeDocument.setActif(true);
        return typeDocumentRepository.save(typeDocument);
    }

    @Override
    public TypeDocument modifier(Long id, TypeDocument donnees) {
        TypeDocument existant = trouverParId(id);
        String nouveauLibelle = nettoyer(donnees.getLibelle());
        if (!nouveauLibelle.equals(existant.getLibelle())&& typeDocumentRepository.existsByLibelle(nouveauLibelle)) {
            throw new BusinessException("Ce libellé existe déjà.");
        }
        existant.setLibelle(nouveauLibelle);
        existant.setDescription(donnees.getDescription());
        return typeDocumentRepository.save(existant);
    }

    @Override
    public void desactiver(Long id) {
        TypeDocument existant = trouverParId(id);
        existant.setActif(false);
        typeDocumentRepository.save(existant);
    }

    private String nettoyer(String libelle) {
        if (libelle == null || libelle.isBlank()) {
            throw new BusinessException("Le libellé est obligatoire.");
        }
        return libelle.trim();
    }
}