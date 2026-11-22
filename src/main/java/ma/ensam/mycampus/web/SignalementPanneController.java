package ma.ensam.mycampus.web;

import jakarta.servlet.http.HttpServletRequest;
import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
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

    // Gestionnaire : liste des signalements ; étudiant : directement le formulaire
    @GetMapping
    public String liste(@RequestParam(defaultValue = "false") boolean nonResolus,
                        Model model, HttpServletRequest request) {
        if (!request.isUserInRole("GESTIONNAIRE")) {
            return "redirect:/signalements/nouveau";
        }
        model.addAttribute("signalements",
                nonResolus ? signalementService.listerNonResolus() : signalementService.listerTout());
        model.addAttribute("nonResolus", nonResolus);
        return "signalements/liste";
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
            redirect.addFlashAttribute("succes",
                    "Panne signalée : le matériel est passé en panne et ses réservations à venir ont été annulées.");
            return request.isUserInRole("GESTIONNAIRE")
                    ? "redirect:/signalements"
                    : "redirect:/signalements/nouveau";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            preparerFormulaire(model, materielId, description);
            return "signalements/formulaire";
        }
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