package ma.ensam.mycampus.web;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/salles")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @InitBinder("salle")
    public void interdireChamps(WebDataBinder binder) {
        binder.setDisallowedFields("id", "actif");
    }

    // Étudiant : salles actives ; gestionnaire : toutes les salles
    @GetMapping
    public String liste(Model model, HttpServletRequest request) {
        boolean gestion = request.isUserInRole("GESTIONNAIRE");
        model.addAttribute("gestion", gestion);
        model.addAttribute("salles", gestion ? salleService.lister() : salleService.listerActives());
        return "salles/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("salle", new Salle());
        model.addAttribute("salleId", null);
        return "salles/formulaire";
    }

    @GetMapping("/modifier")
    public String modifier(@RequestParam Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("salle", salleService.trouverParId(id));
            model.addAttribute("salleId", id);
            return "salles/formulaire";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/salles";
        }
    }

    // id absent = création ; id présent = modification
    @PostMapping("/enregistrer")
    public String enregistrer(@RequestParam(required = false) Long id,
                              @Valid @ModelAttribute("salle") Salle salle,
                              BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("salleId", id);
            return "salles/formulaire";
        }
        try {
            if (id == null) {
                salleService.creer(salle);
                redirect.addFlashAttribute("succes", "Salle ajoutée.");
            } else {
                salleService.modifier(id, salle);
                redirect.addFlashAttribute("succes", "Salle modifiée.");
            }
            return "redirect:/salles";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("salleId", id);
            return "salles/formulaire";
        }
    }

    @PostMapping("/desactiver")
    public String desactiver(@RequestParam Long id, RedirectAttributes redirect) {
        try {
            salleService.desactiver(id);
            redirect.addFlashAttribute("succes", "Salle désactivée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/salles";
    }
}