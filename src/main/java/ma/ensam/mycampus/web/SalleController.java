package ma.ensam.mycampus.web;

import jakarta.validation.Valid;
import ma.ensam.mycampus.dao.entities.Salle;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.SalleService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/salles")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    // Un formulaire falsifié ne peut pas imposer l'id ni la valeur de "actif".
    @InitBinder("salle")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id", "actif");
    }

    @GetMapping
    public String lister(Model model) {
        model.addAttribute("salles", salleService.lister());
        return "salles/liste";
    }

    @GetMapping("/nouveau")
    public String afficherFormulaireCreation(Model model) {
        model.addAttribute("salle", new Salle());
        return "salles/formulaire";
    }

    @PostMapping("/enregistrer")
    public String creer(@Valid @ModelAttribute("salle") Salle salle,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "salles/formulaire";
        }
        try {
            salleService.creer(salle);
            redirect.addFlashAttribute("succes", "Salle ajoutée avec succès.");
            return "redirect:/salles";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            return "salles/formulaire";
        }
    }

    @GetMapping("/{id}/modifier")
    public String afficherFormulaireModification(@PathVariable Long id, Model model,
                                                 RedirectAttributes redirect) {
        try {
            model.addAttribute("salle", salleService.trouverParId(id));
            model.addAttribute("salleId", id);
            return "salles/formulaire";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/salles";
        }
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id,
                           @Valid @ModelAttribute("salle") Salle salle,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("salleId", id);
            return "salles/formulaire";
        }
        try {
            salleService.modifier(id, salle);
            redirect.addFlashAttribute("succes", "Salle modifiée avec succès.");
            return "redirect:/salles";
        } catch (BusinessException e) {
            model.addAttribute("salleId", id);
            model.addAttribute("erreur", e.getMessage());
            return "salles/formulaire";
        }
    }

    @PostMapping("/{id}/desactiver")
    public String desactiver(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            salleService.desactiver(id);
            redirect.addFlashAttribute("succes", "Salle désactivée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/salles";
    }
}