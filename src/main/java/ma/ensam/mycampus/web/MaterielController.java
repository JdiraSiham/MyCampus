package ma.ensam.mycampus.web;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/materiels")
public class MaterielController {

    // EN_PANNE n'est jamais choisi à la main : il vient des signalements
    private static final List<EtatMateriel> ETATS_CHOISIS =
            List.of(EtatMateriel.DISPONIBLE, EtatMateriel.INDISPONIBLE);

    private final MaterielService materielService;

    public MaterielController(MaterielService materielService) {
        this.materielService = materielService;
    }

    // Le formulaire ne peut pas modifier l'id ni l'état actif
    @InitBinder("materiel")
    public void interdireChamps(WebDataBinder binder) {
        binder.setDisallowedFields("id", "actif");
    }

    // Étudiant : matériel actif ; gestionnaire : tout le matériel
    @GetMapping
    public String liste(Model model, HttpServletRequest request) {
        boolean gestion = request.isUserInRole("GESTIONNAIRE");
        model.addAttribute("gestion", gestion);
        model.addAttribute("materiels", gestion ? materielService.lister() : materielService.listerActifs());
        return "materiels/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        Materiel materiel = new Materiel();
        materiel.setEtat(EtatMateriel.DISPONIBLE);
        materiel.setQuantite(1);
        preparerFormulaire(model, materiel, null);
        return "materiels/formulaire";
    }

    @GetMapping("/modifier")
    public String modifier(@RequestParam Long id, Model model, RedirectAttributes redirect) {
        try {
            preparerFormulaire(model, materielService.trouverParId(id), id);
            return "materiels/formulaire";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/materiels";
        }
    }

    // id absent = création ; id présent = modification
    @PostMapping("/enregistrer")
    public String enregistrer(@RequestParam(required = false) Long id,
                              @Valid @ModelAttribute("materiel") Materiel materiel,
                              BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("materielId", id);
            model.addAttribute("etats", ETATS_CHOISIS);
            return "materiels/formulaire";
        }
        try {
            if (id == null) {
                materielService.creer(materiel);
                redirect.addFlashAttribute("succes", "Matériel ajouté.");
            } else {
                materielService.modifier(id, materiel);
                redirect.addFlashAttribute("succes", "Matériel modifié.");
            }
            return "redirect:/materiels";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("materielId", id);
            model.addAttribute("etats", ETATS_CHOISIS);
            return "materiels/formulaire";
        }
    }

    @PostMapping("/etat")
    public String changerEtat(@RequestParam Long id, @RequestParam EtatMateriel etat,
                              RedirectAttributes redirect) {
        try {
            materielService.changerEtat(id, etat);
            redirect.addFlashAttribute("succes", "État du matériel mis à jour.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/materiels";
    }

    @PostMapping("/desactiver")
    public String desactiver(@RequestParam Long id, RedirectAttributes redirect) {
        try {
            materielService.desactiver(id);
            redirect.addFlashAttribute("succes", "Matériel désactivé.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/materiels";
    }

    private void preparerFormulaire(Model model, Materiel materiel, Long id) {
        model.addAttribute("materiel", materiel);
        model.addAttribute("materielId", id);
        model.addAttribute("etats", ETATS_CHOISIS);
    }
}