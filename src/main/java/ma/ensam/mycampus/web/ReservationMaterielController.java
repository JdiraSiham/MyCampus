package ma.ensam.mycampus.web;

import ma.ensam.mycampus.dao.entities.ReservationMateriel;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.MaterielService;
import ma.ensam.mycampus.service.interfaces.ReservationMaterielService;
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
@RequestMapping("/reservations-materiel")
public class ReservationMaterielController {

    // TODO étape 5 : SUPPRIMER ces deux constantes et utiliser l'utilisateur authentifié.
    // Remplacer 1 et 2 par les id réels (table utilisateur) d'un étudiant et d'un gestionnaire.
    private static final Long ETUDIANT_TEST_ID = 11L;
    private static final Long GESTIONNAIRE_TEST_ID = 12L;

    private final ReservationMaterielService reservationService;
    private final MaterielService materielService;

    public ReservationMaterielController(ReservationMaterielService reservationService,
                                         MaterielService materielService) {
        this.reservationService = reservationService;
        this.materielService = materielService;
    }

    // ----- Côté étudiant -----

    @GetMapping("/mes-reservations")
    public String mesReservations(Model model) {
        model.addAttribute("reservations",
                reservationService.listerParDemandeur(ETUDIANT_TEST_ID));
        return "reservationsmateriel/mes-reservations";
    }

    @GetMapping("/nouvelle")
    public String afficherFormulaire(@RequestParam(required = false) Long materielId, Model model) {
        model.addAttribute("materiels", materielService.listerActifs());
        model.addAttribute("materielId", materielId);
        return "reservationsmateriel/formulaire";
    }

    @PostMapping("/reserver")
    public String reserver(@RequestParam Long materielId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                           LocalDateTime dateDebut,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                           LocalDateTime dateFin,
                           Model model, RedirectAttributes redirect) {
        try {
            reservationService.creer(ETUDIANT_TEST_ID, materielId, dateDebut, dateFin);
            redirect.addFlashAttribute("succes", "Votre demande de réservation a été enregistrée.");
            return "redirect:/reservations-materiel/mes-reservations";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("materiels", materielService.listerActifs());
            model.addAttribute("materielId", materielId);
            model.addAttribute("dateDebut", dateDebut);
            model.addAttribute("dateFin", dateFin);
            return "reservationsmateriel/formulaire";
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
        return "redirect:/reservations-materiel/mes-reservations";
    }

    // ----- Côté gestionnaire -----

    @GetMapping("/gestion")
    public String gestion(@RequestParam(required = false) StatutReservation statut, Model model) {
        List<ReservationMateriel> reservations = (statut == null)
                ? reservationService.listerTout()
                : reservationService.listerParStatut(statut);
        model.addAttribute("reservations", reservations);
        model.addAttribute("statuts", StatutReservation.values());
        model.addAttribute("statutChoisi", statut);
        return "reservationsmateriel/gestion";
    }

    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reservationService.valider(id, GESTIONNAIRE_TEST_ID);
            redirect.addFlashAttribute("succes", "Réservation validée.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/reservations-materiel/gestion";
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
        return "redirect:/reservations-materiel/gestion";
    }
}