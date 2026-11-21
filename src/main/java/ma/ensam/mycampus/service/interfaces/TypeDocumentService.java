package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.TypeDocument;
import java.util.List;

public interface TypeDocumentService {
    List<TypeDocument> listerTous();
    List<TypeDocument> listerActifs();
    TypeDocument trouverParId(Long id);
    TypeDocument creer(TypeDocument typeDocument);
    TypeDocument modifier(Long id, TypeDocument donnees);
    void desactiver(Long id);
    List<TypeDocument> rechercher(String recherche);
}