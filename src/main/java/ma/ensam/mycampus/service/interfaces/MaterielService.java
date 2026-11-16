package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;

import java.util.List;

public interface MaterielService {
    List<Materiel> lister();
    List<Materiel> listerActifs();
    Materiel trouverParId(Long id);
    Materiel creer(Materiel materiel);
    Materiel modifier(Long id, Materiel donnees);
    Materiel changerEtat(Long id, EtatMateriel nouvelEtat);
    void desactiver(Long id);
}