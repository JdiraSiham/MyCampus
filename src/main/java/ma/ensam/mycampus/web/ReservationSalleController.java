package ma.ensam.mycampus.web;

import ma.ensam.mycampus.dao.entities.ReservationSalle;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.ReservationSalleService;
import ma.ensam.mycampus.service.interfaces.SalleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/reservations-salle")
public class ReservationSalleController {

    // TODO étape 5 : SUPPRIMER ces deux constantes et utiliser l'utilisateur authentifié.
    private static final Long ETUDIANT_TEST_ID = 11L;
    private static final Long GESTIONNAIRE_TEST_ID = 12L;

    private final ReservationSalleService reservationService;
    private final SalleService salleService;

    public ReservationSalleController(ReservationSalleService reservationService,
                                      SalleService salleService) {
        this.reservationService = reservationService;
        this.salleService = salleService;
    }

    // ----- Côté étudiant -----

    @GetMapping("/mes-reservations")
    public String mesReservations(Model model) {
        model.addAttribute("reservations",
                reservationService.listerParDemandeur(ETUDIANT_TEST_ID));
        return "reservationssalle/mes-reservations";
    }

    @GetMapping("/disponibles")
    public String disponibles(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            Model model) {
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);
        if (dateDebut != null && dateFin != null) {
            try {
                model.addAttribute("salles",
                        reservationService.rechercherSallesDisponibles(dateDebut, dateFin));
            } catch (BusinessException e) {
                model.addAttribute("erreur", e.getMessage());
            }
        }
        return "reservationssalle/disponibles";
    }

    @GetMapping("/nouvelle")
    public String afficherFormulaire(
            @RequestParam(required = false) Long salleId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            Model model) {
        model.addAttribute("salles", salleService.listerActives());
        model.addAttribute("salleId", salleId);
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);
        return "reservationssalle/formulaire";
    }

    @PostMapping("/reserver")
    public String reserver(
            @RequestParam Long salleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) String objet,
            Model model, RedirectAttributes redirect) {
        try {
            ReservationSalle reservation = reservationService.creer(
                    ETUDIANT_TEST_ID, salleId, dateDebut, dateFin, objet);
            String message = (reservation.getStatut() == StatutReservation.VALIDEE)
                    ? "Réservation confirmée (validation automatique)."
                    : "Demande enregistrée : elle doit être validée par un gestionnaire.";
            redirect.addFlashAttribute("succes", message);
            return "redirect:/reservations-salle/mes-reservations";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("salles", salleService.listerActives());
            model.addAttribute("salleId", salleId);
            model.addAttribute("dateDebut", dateDebut);
            model.addAttribute("dateFin", dateFin);
            model.addAttribute("objet", objet);
            return "reservationssalle/formulaire";
        }
    }

    @PostMapping("/{id}/annuler")
    public String annuler(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reservationService.annuler(id, ETUDIANT_TEST_ID);
            redirect.addFlashAttribute("succes", "Réservation annulée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-salle/mes-reservations";
    }

    // ----- Côté gestionnaire -----

    @GetMapping("/gestion")
    public String gestion(@RequestParam(required = false) StatutReservation statut, Model model) {
        List<ReservationSalle> reservations = (statut == null)
                ? reservationService.listerTout()
                : reservationService.listerParStatut(statut);
        model.addAttribute("reservations", reservations);
        model.addAttribute("statuts", StatutReservation.values());
        model.addAttribute("statutChoisi", statut);
        return "reservationssalle/gestion";
    }

    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reservationService.valider(id, GESTIONNAIRE_TEST_ID);
            redirect.addFlashAttribute("succes", "Réservation validée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-salle/gestion";
    }

    @PostMapping("/{id}/refuser")
    public String refuser(@PathVariable Long id,
                          @RequestParam(required = false) String motif,
                          RedirectAttributes redirect) {
        try {
            reservationService.refuser(id, GESTIONNAIRE_TEST_ID, motif);
            redirect.addFlashAttribute("succes", "Réservation refusée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-salle/gestion";
    }
}