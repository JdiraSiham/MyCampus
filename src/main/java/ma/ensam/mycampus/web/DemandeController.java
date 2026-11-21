package ma.ensam.mycampus.web;

import ma.ensam.mycampus.dao.entities.DemandeAdministrative;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.entities.enums.StatutDemande;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.DemandeService;
import ma.ensam.mycampus.service.interfaces.TypeDocumentService;
import ma.ensam.mycampus.service.interfaces.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;
import java.util.List;

@Controller
public class DemandeController {

    private final DemandeService demandeService;
    private final TypeDocumentService typeDocumentService;
    private final UtilisateurService utilisateurService;

    @Autowired
    public DemandeController(DemandeService demandeService, TypeDocumentService typeDocumentService, UtilisateurService utilisateurService) {
        this.demandeService = demandeService;
        this.typeDocumentService = typeDocumentService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/demandes")
    public String mesDemandes(@RequestParam(required = false) StatutDemande statut, @RequestParam(required = false) Long typeId, Principal principal, Model model) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            if (connecte.getRole() == Role.GESTIONNAIRE) {
                return "redirect:/demandes/gestion";
            }
            model.addAttribute("demandes", demandeService.rechercherPourEtudiant(connecte.getId(), statut, typeId));
            model.addAttribute("gestion", false);
            model.addAttribute("statuts", StatutDemande.values());
            model.addAttribute("types", typeDocumentService.listerTous());
            model.addAttribute("statutChoisi", statut);
            model.addAttribute("typeChoisi", typeId);
            return "demandes/liste";
        } catch (BusinessException e) {
            return afficherErreur(model, e.getMessage());
        }
    }

    @GetMapping("/demandes/nouvelle")
    public String nouvelle(Model model) {
        model.addAttribute("types", typeDocumentService.listerActifs());
        return "demandes/formulaire";
    }

    @PostMapping("/demandes/deposer")
    public String deposer(@RequestParam Long typeDocumentId, Principal principal, Model model) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            demandeService.deposer(connecte.getId(), typeDocumentId);
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("types", typeDocumentService.listerActifs());
            return "demandes/formulaire";
        }
        return "redirect:/demandes";
    }

    @GetMapping("/demandes/detail")
    public String detail(@RequestParam Long id, Principal principal, Model model) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            remplirDetail(id, connecte, model);
            return "demandes/detail";
        } catch (BusinessException e) {
            return afficherErreur(model, e.getMessage());
        }
    }

    @GetMapping("/demandes/gestion")
    public String gestion(@RequestParam(required = false) String q, @RequestParam(required = false) StatutDemande statut, @RequestParam(required = false) Long typeId, Principal principal, Model model) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            model.addAttribute("demandes", demandeService.rechercherPourGestionnaire(connecte.getId(), q, statut, typeId));
            model.addAttribute("gestion", true);
            model.addAttribute("statuts", StatutDemande.values());
            model.addAttribute("types", typeDocumentService.listerTous());
            model.addAttribute("statutChoisi", statut);
            model.addAttribute("typeChoisi", typeId);
            model.addAttribute("recherche", q);
            return "demandes/liste";
        } catch (BusinessException e) {
            return afficherErreur(model, e.getMessage());
        }
    }

    @PostMapping("/demandes/prendre-en-charge")
    public String prendreEnCharge(@RequestParam Long id, Principal principal, Model model) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            demandeService.prendreEnCharge(id, connecte.getId());
        } catch (BusinessException e) {
            return reafficherDetail(id, principal, model, e.getMessage());
        }
        return "redirect:/demandes/detail?id=" + id;
    }

    @PostMapping("/demandes/prete")
    public String marquerPrete(@RequestParam Long id, Principal principal, Model model) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            demandeService.marquerPrete(id, connecte.getId());
        } catch (BusinessException e) {
            return reafficherDetail(id, principal, model, e.getMessage());
        }
        return "redirect:/demandes/detail?id=" + id;
    }

    @PostMapping("/demandes/rejeter")
    public String rejeter(@RequestParam Long id, @RequestParam String motif, Principal principal, Model model) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            demandeService.rejeter(id, connecte.getId(), motif);
        } catch (BusinessException e) {
            return reafficherDetail(id, principal, model, e.getMessage());
        }
        return "redirect:/demandes/detail?id=" + id;
    }

    private Utilisateur utilisateurConnecte(Principal principal) {
        return utilisateurService.trouverParEmail(principal.getName());
    }

    private void remplirDetail(Long id, Utilisateur connecte, Model model) {
        DemandeAdministrative demande = demandeService.consulter(id, connecte.getId());
        model.addAttribute("demande", demande);
        model.addAttribute("peutTraiter", connecte.getRole() == Role.GESTIONNAIRE);
    }

    private String reafficherDetail(Long id, Principal principal, Model model, String erreur) {
        try {
            Utilisateur connecte = utilisateurConnecte(principal);
            remplirDetail(id, connecte, model);
            model.addAttribute("erreur", erreur);
            return "demandes/detail";
        } catch (BusinessException e) {
            return afficherErreur(model, e.getMessage());
        }
    }

    private String afficherErreur(Model model, String message) {
        model.addAttribute("erreur", message);
        model.addAttribute("demandes", List.of());
        model.addAttribute("gestion", false);
        return "demandes/liste";
    }
}