package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.repositories.UtilisateurRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.ReservationMaterielService;
import ma.ensam.mycampus.service.interfaces.ReservationSalleService;
import ma.ensam.mycampus.service.interfaces.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class UtilisateurManager implements UtilisateurService {

    private static final String EMAIL_ADMIN_INITIAL = "admin@mycampus.ma";
    private static final String MOT_DE_PASSE_ADMIN_INITIAL = "Admin@2026";

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReservationMaterielService reservationMaterielService;
    private final ReservationSalleService reservationSalleService;

    @Autowired
    public UtilisateurManager(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder, ReservationMaterielService reservationMaterielService, ReservationSalleService reservationSalleService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.reservationMaterielService = reservationMaterielService;
        this.reservationSalleService = reservationSalleService;
    }

    @Override
    public List<Utilisateur> listerTous() {
        return utilisateurRepository.findAll();
    }

    @Override
    public Utilisateur trouverParId(Long id) {
        return utilisateurRepository.findById(id).orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
    }

    @Override
    public Utilisateur creer(Utilisateur utilisateur, String motDePasseClair) {
        verifierChampsObligatoires(utilisateur);
        if (motDePasseClair == null || motDePasseClair.isBlank()) {
            throw new BusinessException("Le mot de passe est obligatoire.");
        }
        String email = utilisateur.getEmail().trim();
        if (utilisateurRepository.existsByEmail(email)) {
            throw new BusinessException("Cet email est déjà utilisé.");
        }
        utilisateur.setId(null);
        utilisateur.setEmail(email);
        utilisateur.setActif(true);
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseClair));
        return utilisateurRepository.save(utilisateur);
    }

    @Override
    public Utilisateur modifier(Long id, Utilisateur donnees) {
        verifierChampsObligatoires(donnees);
        Utilisateur existant = trouverParId(id);
        String nouvelEmail = donnees.getEmail().trim();
        if (!nouvelEmail.equals(existant.getEmail()) && utilisateurRepository.existsByEmail(nouvelEmail)) {
            throw new BusinessException("Cet email est déjà utilisé.");
        }
        existant.setNom(donnees.getNom().trim());
        existant.setPrenom(donnees.getPrenom().trim());
        existant.setEmail(nouvelEmail);
        existant.setRole(donnees.getRole());
        existant.setCne(donnees.getCne());
        return utilisateurRepository.save(existant);
    }

    @Override
    public void desactiver(Long id, Long adminId) {
        Utilisateur admin = trouverParId(adminId);
        if (!admin.isActif() || admin.getRole() != Role.ADMIN) {
            throw new BusinessException("Seul un administrateur actif peut désactiver un compte.");
        }
        if (id.equals(adminId)) {
            throw new BusinessException("Un administrateur ne peut pas désactiver son propre compte.");
        }
        Utilisateur utilisateur = trouverParId(id);
        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);

        if (utilisateur.getRole() == Role.ETUDIANT) {
            reservationMaterielService.annulerReservationsBloquantes(id);
            reservationSalleService.annulerReservationsBloquantes(id);
        }
    }

    @Override
    public void changerMotDePasse(Long id, String ancienMotDePasse, String nouveauMotDePasse) {
        Utilisateur utilisateur = trouverParId(id);
        if (ancienMotDePasse == null || !passwordEncoder.matches(ancienMotDePasse, utilisateur.getMotDePasse())) {
            throw new BusinessException("L'ancien mot de passe est incorrect.");
        }
        if (nouveauMotDePasse == null || nouveauMotDePasse.isBlank()) {
            throw new BusinessException("Le nouveau mot de passe est obligatoire.");
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);
    }

    @Override
    public void initialiserPremierAdmin() {
        if (utilisateurRepository.existsByRole(Role.ADMIN)){
            return;
        }
        Utilisateur admin = new Utilisateur();
        admin.setNom("Administrateur");
        admin.setPrenom("MyCampus");
        admin.setEmail(EMAIL_ADMIN_INITIAL);
        admin.setRole(Role.ADMIN);
        creer(admin, MOT_DE_PASSE_ADMIN_INITIAL);
    }

    private void verifierChampsObligatoires(Utilisateur u) {
        if (u.getNom() == null || u.getNom().isBlank()
                || u.getPrenom() == null || u.getPrenom().isBlank()
                || u.getEmail() == null || u.getEmail().isBlank()
                || u.getRole() == null) {
            throw new BusinessException("Nom, prénom, email et rôle sont obligatoires.");
        }
    }
}