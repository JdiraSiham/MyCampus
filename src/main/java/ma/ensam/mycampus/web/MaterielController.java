package ma.ensam.mycampus.web;

import jakarta.validation.Valid;
import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.MaterielService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/materiels")
public class MaterielController {

    private final MaterielService materielService;

    public MaterielController(MaterielService materielService) {
        this.materielService = materielService;
    }

    // Un formulaire falsifié ne peut pas imposer l'id ni la valeur de "actif".
    @InitBinder("materiel")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id", "actif");
    }

    @GetMapping
    public String lister(Model model) {
        model.addAttribute("materiels", materielService.lister());
        return "materiels/liste";
    }

    @GetMapping("/nouveau")
    public String afficherFormulaireCreation(Model model) {
        model.addAttribute("materiel", new Materiel());
        model.addAttribute("etats", etatsManuels());
        return "materiels/formulaire";
    }

    @PostMapping("/enregistrer")
    public String creer(@Valid @ModelAttribute("materiel") Materiel materiel,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("etats", etatsManuels());
            return "materiels/formulaire";
        }
        try {
            materielService.creer(materiel);
            redirect.addFlashAttribute("succes", "Matériel ajouté avec succès.");
            return "redirect:/materiels";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("etats", etatsManuels());
            return "materiels/formulaire";
        }
    }

    @GetMapping("/{id}/modifier")
    public String afficherFormulaireModification(@PathVariable Long id, Model model,
                                                 RedirectAttributes redirect) {
        try {
            model.addAttribute("materiel", materielService.trouverParId(id));
            model.addAttribute("materielId", id);
            model.addAttribute("etats", etatsManuels());
            return "materiels/formulaire";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/materiels";
        }
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id,
                           @Valid @ModelAttribute("materiel") Materiel materiel,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("materielId", id);
            model.addAttribute("etats", etatsManuels());
            return "materiels/formulaire";
        }
        try {
            materielService.modifier(id, materiel);
            redirect.addFlashAttribute("succes", "Matériel modifié avec succès.");
            return "redirect:/materiels";
        } catch (BusinessException e) {
            model.addAttribute("materielId", id);
            model.addAttribute("etats", etatsManuels());
            model.addAttribute("erreur", e.getMessage());
            return "materiels/formulaire";
        }
    }

    @PostMapping("/{id}/etat")
    public String changerEtat(@PathVariable Long id, @RequestParam EtatMateriel etat,
                              RedirectAttributes redirect) {
        try {
            materielService.changerEtat(id, etat);
            redirect.addFlashAttribute("succes", "État du matériel mis à jour.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/materiels";
    }

    @PostMapping("/{id}/desactiver")
    public String desactiver(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            materielService.desactiver(id);
            redirect.addFlashAttribute("succes", "Matériel désactivé.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/materiels";
    }

    // Le gestionnaire ne choisit jamais EN_PANNE : seuls les signalements de panne le font.
    private List<EtatMateriel> etatsManuels() {
        return List.of(EtatMateriel.DISPONIBLE, EtatMateriel.INDISPONIBLE);
    }
}