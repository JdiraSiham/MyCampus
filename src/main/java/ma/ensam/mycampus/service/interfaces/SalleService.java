package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.Salle;

import java.util.List;

public interface SalleService {
    List<Salle> lister();
    List<Salle> listerActives();
    Salle trouverParId(Long id);
    Salle creer(Salle salle);
    Salle modifier(Long id, Salle donnees);
    void desactiver(Long id);
}