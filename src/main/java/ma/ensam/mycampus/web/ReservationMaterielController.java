package ma.ensam.mycampus.web;

import jakarta.servlet.http.HttpServletRequest;
import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.MaterielService;
import ma.ensam.mycampus.service.interfaces.ReservationMaterielService;
import ma.ensam.mycampus.service.interfaces.UtilisateurService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/reservations-materiel")
public class ReservationMaterielController {

    private final ReservationMaterielService reservationService;
    private final MaterielService materielService;
    private final UtilisateurService utilisateurService;

    public ReservationMaterielController(ReservationMaterielService reservationService,
                                         MaterielService materielService,
                                         UtilisateurService utilisateurService) {
        this.reservationService = reservationService;
        this.materielService = materielService;
        this.utilisateurService = utilisateurService;
    }

    // Même URL pour les deux rôles : chacun est envoyé vers sa page
    @GetMapping
    public String accueil(HttpServletRequest request) {
        return request.isUserInRole("GESTIONNAIRE")
                ? "redirect:/reservations-materiel/gestion"
                : "redirect:/reservations-materiel/mes-reservations";
    }

    // ----- Étudiant -----

    @GetMapping("/mes-reservations")
    public String mesReservations(Principal principal, Model model) {
        model.addAttribute("reservations", reservationService.listerParDemandeur(idConnecte(principal)));
        return "reservationsmateriel/mes-reservations";
    }

    @GetMapping("/nouvelle")
    public String nouvelle(@RequestParam(required = false) Long materielId, Model model) {
        preparerFormulaire(model, materielId, null, null);
        return "reservationsmateriel/formulaire";
    }

    @PostMapping("/reserver")
    public String reserver(@RequestParam Long materielId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
                           Principal principal, Model model, RedirectAttributes redirect) {
        try {
            reservationService.creer(idConnecte(principal), materielId, dateDebut, dateFin);
            redirect.addFlashAttribute("succes", "Votre demande de réservation a été enregistrée.");
            return "redirect:/reservations-materiel/mes-reservations";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            preparerFormulaire(model, materielId, dateDebut, dateFin);
            return "reservationsmateriel/formulaire";
        }
    }

    @PostMapping("/annuler")
    public String annuler(@RequestParam Long id, Principal principal, RedirectAttributes redirect) {
        try {
            reservationService.annuler(id, idConnecte(principal));
            redirect.addFlashAttribute("succes", "Réservation annulée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-materiel/mes-reservations";
    }

    // ----- Gestionnaire -----

    @GetMapping("/gestion")
    public String gestion(@RequestParam(required = false) StatutReservation statut, Model model) {
        model.addAttribute("reservations", statut == null
                ? reservationService.listerTout()
                : reservationService.listerParStatut(statut));
        model.addAttribute("statutChoisi", statut);
        return "reservationsmateriel/gestion";
    }

    @PostMapping("/valider")
    public String valider(@RequestParam Long id, Principal principal, RedirectAttributes redirect) {
        try {
            reservationService.valider(id, idConnecte(principal));
            redirect.addFlashAttribute("succes", "Réservation validée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-materiel/gestion";
    }

    @PostMapping("/refuser")
    public String refuser(@RequestParam Long id,
                          @RequestParam(required = false) String motif,
                          Principal principal, RedirectAttributes redirect) {
        try {
            reservationService.refuser(id, idConnecte(principal), motif);
            redirect.addFlashAttribute("succes", "Réservation refusée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-materiel/gestion";
    }

    // ----- Outils internes -----

    private Long idConnecte(Principal principal) {
        return utilisateurService.trouverParEmail(principal.getName()).getId();
    }

    // Matériel proposé : actif et disponible (le Manager revérifie tout à l'enregistrement)
    private void preparerFormulaire(Model model, Long materielId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Materiel> materiels = materielService.listerActifs().stream()
                .filter(m -> m.getEtat() == EtatMateriel.DISPONIBLE)
                .toList();
        model.addAttribute("materiels", materiels);
        model.addAttribute("materielId", materielId);
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);
    }
}