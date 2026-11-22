package ma.ensam.mycampus.service.interfaces;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import java.util.List;

public interface UtilisateurService {
    List<Utilisateur> listerTous();
    Utilisateur trouverParId(Long id);
    Utilisateur creer(Utilisateur utilisateur, String motDePasseClair);
    Utilisateur modifier(Long id, Utilisateur donnees);
    void desactiver(Long id, Long adminId);
    void changerMotDePasse(Long id, String ancienMotDePasse, String nouveauMotDePasse);
    void initialiserPremierAdmin();
    Utilisateur trouverParEmail(String email);
    List<Utilisateur> rechercher(Role role, String recherche);
}