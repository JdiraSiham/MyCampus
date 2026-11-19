package ma.ensam.mycampus.web;

import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.MaterielService;
import ma.ensam.mycampus.service.interfaces.SignalementPanneService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/signalements")
public class SignalementPanneController {

    // TODO étape 5 : SUPPRIMER ces constantes et utiliser l'utilisateur authentifié.
    private static final Long ETUDIANT_TEST_ID = 11L;
    private static final Long GESTIONNAIRE_TEST_ID = 12L;

    private final SignalementPanneService signalementService;
    private final MaterielService materielService;

    public SignalementPanneController(SignalementPanneService signalementService,
                                      MaterielService materielService) {
        this.signalementService = signalementService;
        this.materielService = materielService;
    }

    @GetMapping
    public String lister(@RequestParam(defaultValue = "false") boolean nonResolus, Model model) {
        model.addAttribute("signalements", nonResolus
                ? signalementService.listerNonResolus()
                : signalementService.listerTout());
        model.addAttribute("nonResolus", nonResolus);
        return "signalements/liste";
    }

    @GetMapping("/nouveau")
    public String afficherFormulaire(@RequestParam(required = false) Long materielId, Model model) {
        model.addAttribute("materiels", materielService.listerActifs());
        model.addAttribute("materielId", materielId);
        return "signalements/formulaire";
    }

    @PostMapping("/signaler")
    public String signaler(@RequestParam Long materielId,
                           @RequestParam(required = false) String description,
                           Model model, RedirectAttributes redirect) {
        try {
            signalementService.signaler(auteurTestId(), materielId, description);
            redirect.addFlashAttribute("succes",
                    "Panne signalée : le matériel est passé EN_PANNE et ses réservations à venir ont été annulées.");
            return "redirect:/signalements";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("materiels", materielService.listerActifs());
            model.addAttribute("materielId", materielId);
            model.addAttribute("description", description);
            return "signalements/formulaire";
        }
    }

    @PostMapping("/{id}/cloturer")
    public String cloturer(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            signalementService.cloturer(id, GESTIONNAIRE_TEST_ID);
            redirect.addFlashAttribute("succes", "Signalement clôturé : le matériel est de nouveau disponible.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/signalements";
    }

    // TODO étape 5 : l'auteur sera l'utilisateur connecté (étudiant ou gestionnaire).
    // Pour tester le signalement par un gestionnaire, renvoyer GESTIONNAIRE_TEST_ID ici.
    private Long auteurTestId() {
        return ETUDIANT_TEST_ID;
    }
}