package ma.ensam.mycampus.web;

import jakarta.servlet.http.HttpServletRequest;
import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
import ma.ensam.mycampus.dao.entities.enums.StatutSignalement;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.MaterielService;
import ma.ensam.mycampus.service.interfaces.SignalementPanneService;
import ma.ensam.mycampus.service.interfaces.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/signalements")
public class SignalementPanneController {

    private final SignalementPanneService signalementService;
    private final MaterielService materielService;
    private final UtilisateurService utilisateurService;

    public SignalementPanneController(SignalementPanneService signalementService,
                                      MaterielService materielService,
                                      UtilisateurService utilisateurService) {
        this.signalementService = signalementService;
        this.materielService = materielService;
        this.utilisateurService = utilisateurService;
    }

    // Gestionnaire : tous les signalements (filtre par statut) ; étudiant : ses propres signalements
    @GetMapping
    public String liste(@RequestParam(required = false) StatutSignalement statut,
                        Model model, HttpServletRequest request) {
        if (!request.isUserInRole("GESTIONNAIRE")) {
            return "redirect:/signalements/mes-signalements";
        }
        model.addAttribute("signalements", statut == null
                ? signalementService.listerTout()
                : signalementService.listerParStatut(statut));
        model.addAttribute("statutChoisi", statut);
        return "signalements/liste";
    }

    // ----- Étudiant -----

    @GetMapping("/mes-signalements")
    public String mesSignalements(Principal principal, Model model) {
        model.addAttribute("signalements", signalementService.listerParAuteur(idConnecte(principal)));
        return "signalements/mes-signalements";
    }

    @GetMapping("/nouveau")
    public String nouveau(@RequestParam(required = false) Long materielId, Model model) {
        preparerFormulaire(model, materielId, null);
        return "signalements/formulaire";
    }

    @PostMapping("/signaler")
    public String signaler(@RequestParam Long materielId,
                           @RequestParam(required = false) String description,
                           Principal principal, Model model, RedirectAttributes redirect,
                           HttpServletRequest request) {
        try {
            signalementService.signaler(idConnecte(principal), materielId, description);
            if (request.isUserInRole("GESTIONNAIRE")) {
                redirect.addFlashAttribute("succes",
                        "Panne signalée et validée : le matériel est en panne et ses réservations à venir sont annulées.");
                return "redirect:/signalements";
            }
            redirect.addFlashAttribute("succes",
                    "Votre signalement a été transmis à un gestionnaire pour validation.");
            return "redirect:/signalements/mes-signalements";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            preparerFormulaire(model, materielId, description);
            return "signalements/formulaire";
        }
    }

    // ----- Gestionnaire -----

    @PostMapping("/valider")
    public String valider(@RequestParam Long id, Principal principal, RedirectAttributes redirect) {
        try {
            signalementService.valider(id, idConnecte(principal));
            redirect.addFlashAttribute("succes",
                    "Panne validée : le matériel est en panne et ses réservations à venir sont annulées.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/signalements";
    }

    @PostMapping("/refuser")
    public String refuser(@RequestParam Long id,
                          @RequestParam(required = false) String motif,
                          Principal principal, RedirectAttributes redirect) {
        try {
            signalementService.refuser(id, idConnecte(principal), motif);
            redirect.addFlashAttribute("succes", "Signalement refusé : le matériel n'est pas modifié.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/signalements";
    }

    @PostMapping("/cloturer")
    public String cloturer(@RequestParam Long id, Principal principal, RedirectAttributes redirect) {
        try {
            signalementService.cloturer(id, idConnecte(principal));
            redirect.addFlashAttribute("succes", "Signalement clôturé : le matériel est de nouveau disponible.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/signalements";
    }

    // ----- Outils internes -----

    private Long idConnecte(Principal principal) {
        return utilisateurService.trouverParEmail(principal.getName()).getId();
    }

    // Matériel proposé : actif et pas déjà en panne
    private void preparerFormulaire(Model model, Long materielId, String description) {
        List<Materiel> materiels = materielService.listerActifs().stream()
                .filter(m -> m.getEtat() != EtatMateriel.EN_PANNE)
                .toList();
        model.addAttribute("materiels", materiels);
        model.addAttribute("materielId", materielId);
        model.addAttribute("description", description);
    }
}