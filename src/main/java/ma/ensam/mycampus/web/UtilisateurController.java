package ma.ensam.mycampus.web;

import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;

@Controller
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @Autowired
    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/utilisateurs")
    public String lister(@RequestParam(required = false) Role role, @RequestParam(required = false) String q, Model model) {
        remplirListe(model, role, q);
        return "utilisateurs/liste";
    }

    @GetMapping("/utilisateurs/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        model.addAttribute("roles", Role.values());
        return "utilisateurs/formulaire";
    }

    @GetMapping("/utilisateurs/modifier")
    public String modifier(@RequestParam Long id, Model model) {
        model.addAttribute("utilisateur", utilisateurService.trouverParId(id));
        model.addAttribute("roles", Role.values());
        return "utilisateurs/formulaire";
    }

    @PostMapping("/utilisateurs/enregistrer")
    public String enregistrer(@ModelAttribute Utilisateur utilisateur, @RequestParam(required = false) String motDePasseClair, Model model) {
        try {
            if (utilisateur.getId() == null) {
                utilisateurService.creer(utilisateur, motDePasseClair);
            } else {
                utilisateurService.modifier(utilisateur.getId(), utilisateur);
            }
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "utilisateurs/formulaire";
        }
        return "redirect:/utilisateurs";
    }

    @PostMapping("/utilisateurs/desactiver")
    public String desactiver(@RequestParam Long id, Principal principal, Model model) {
        try {
            Utilisateur admin = utilisateurService.trouverParEmail(principal.getName());
            utilisateurService.desactiver(id, admin.getId());
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            remplirListe(model, null, null);
            return "utilisateurs/liste";
        }
        return "redirect:/utilisateurs";
    }

    @GetMapping("/mot-de-passe")
    public String formulaireMotDePasse() {
        return "mot-de-passe";
    }

    @PostMapping("/mot-de-passe")
    public String changerMotDePasse(@RequestParam String ancienMotDePasse, @RequestParam String nouveauMotDePasse, @RequestParam String confirmation, Principal principal, Model model) {
        if (!nouveauMotDePasse.equals(confirmation)) {
            model.addAttribute("erreur", "La confirmation ne correspond pas au nouveau mot de passe.");
            return "mot-de-passe";
        }
        try {
            Utilisateur connecte = utilisateurService.trouverParEmail(principal.getName());
            utilisateurService.changerMotDePasse(connecte.getId(), ancienMotDePasse, nouveauMotDePasse);
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            return "mot-de-passe";
        }
        model.addAttribute("succes", "Mot de passe modifié avec succès.");
        return "mot-de-passe";
    }
    
    private void remplirListe(Model model, Role role, String q) {
        model.addAttribute("utilisateurs", utilisateurService.rechercher(role, q));
        model.addAttribute("roleChoisi", role);
        model.addAttribute("recherche", q);
    }
}