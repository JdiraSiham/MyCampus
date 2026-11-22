package ma.ensam.mycampus.web;

import jakarta.servlet.http.HttpServletRequest;
import ma.ensam.mycampus.dao.entities.ReservationSalle;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.ReservationSalleService;
import ma.ensam.mycampus.service.interfaces.SalleService;
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

@Controller
@RequestMapping("/reservations-salle")
public class ReservationSalleController {

    private final ReservationSalleService reservationService;
    private final SalleService salleService;
    private final UtilisateurService utilisateurService;

    public ReservationSalleController(ReservationSalleService reservationService,
                                      SalleService salleService,
                                      UtilisateurService utilisateurService) {
        this.reservationService = reservationService;
        this.salleService = salleService;
        this.utilisateurService = utilisateurService;
    }

    // Même URL pour les deux rôles : chacun est envoyé vers sa page
    @GetMapping
    public String accueil(HttpServletRequest request) {
        return request.isUserInRole("GESTIONNAIRE")
                ? "redirect:/reservations-salle/gestion"
                : "redirect:/reservations-salle/mes-reservations";
    }

    // ----- Étudiant -----

    @GetMapping("/mes-reservations")
    public String mesReservations(Principal principal, Model model) {
        model.addAttribute("reservations", reservationService.listerParDemandeur(idConnecte(principal)));
        return "reservationssalle/mes-reservations";
    }

    // Recherche des salles libres sur une période (rien n'est cherché tant que les deux dates manquent)
    @GetMapping("/disponibles")
    public String disponibles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            Model model) {
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);
        boolean recherche = dateDebut != null && dateFin != null;
        model.addAttribute("recherche", recherche);
        if (recherche) {
            try {
                model.addAttribute("salles", reservationService.rechercherSallesDisponibles(dateDebut, dateFin));
            } catch (BusinessException e) {
                model.addAttribute("erreur", e.getMessage());
                model.addAttribute("recherche", false);
            }
        }
        return "reservationssalle/disponibles";
    }

    @GetMapping("/nouvelle")
    public String nouvelle(
            @RequestParam(required = false) Long salleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            Model model) {
        preparerFormulaire(model, salleId, dateDebut, dateFin, null);
        return "reservationssalle/formulaire";
    }

    @PostMapping("/reserver")
    public String reserver(@RequestParam Long salleId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
                           @RequestParam(required = false) String objet,
                           Principal principal, Model model, RedirectAttributes redirect) {
        try {
            ReservationSalle reservation = reservationService.creer(
                    idConnecte(principal), salleId, dateDebut, dateFin, objet);
            String message = (reservation.getStatut() == StatutReservation.VALIDEE)
                    ? "Réservation confirmée (validation automatique)."
                    : "Demande enregistrée : elle doit être validée par un gestionnaire.";
            redirect.addFlashAttribute("succes", message);
            return "redirect:/reservations-salle/mes-reservations";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            preparerFormulaire(model, salleId, dateDebut, dateFin, objet);
            return "reservationssalle/formulaire";
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
        return "redirect:/reservations-salle/mes-reservations";
    }

    // ----- Gestionnaire -----

    @GetMapping("/gestion")
    public String gestion(@RequestParam(required = false) StatutReservation statut, Model model) {
        model.addAttribute("reservations", statut == null
                ? reservationService.listerTout()
                : reservationService.listerParStatut(statut));
        model.addAttribute("statutChoisi", statut);
        return "reservationssalle/gestion";
    }

    @PostMapping("/valider")
    public String valider(@RequestParam Long id, Principal principal, RedirectAttributes redirect) {
        try {
            reservationService.valider(id, idConnecte(principal));
            redirect.addFlashAttribute("succes", "Réservation validée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-salle/gestion";
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
        return "redirect:/reservations-salle/gestion";
    }

    // ----- Outils internes -----

    private Long idConnecte(Principal principal) {
        return utilisateurService.trouverParEmail(principal.getName()).getId();
    }

    private void preparerFormulaire(Model model, Long salleId, LocalDateTime dateDebut,
                                    LocalDateTime dateFin, String objet) {
        model.addAttribute("salles", salleService.listerActives());
        model.addAttribute("salleId", salleId);
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);
        model.addAttribute("objet", objet);
    }
}