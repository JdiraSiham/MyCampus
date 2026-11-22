package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.SignalementPanne;
import ma.ensam.mycampus.dao.entities.enums.StatutSignalement;

import java.util.List;

public interface SignalementPanneService {

    SignalementPanne signaler(Long auteurId, Long materielId, String description);

    // Le gestionnaire confirme la panne d'un étudiant : le matériel passe EN_PANNE
    SignalementPanne valider(Long signalementId, Long gestionnaireId);

    // Le gestionnaire ne confirme pas la panne (motif obligatoire)
    SignalementPanne refuser(Long signalementId, Long gestionnaireId, String motif);

    SignalementPanne cloturer(Long signalementId, Long gestionnaireId);

    SignalementPanne trouverParId(Long id);

    List<SignalementPanne> listerTout();

    List<SignalementPanne> listerNonResolus();

    List<SignalementPanne> listerParMateriel(Long materielId);

    List<SignalementPanne> listerParStatut(StatutSignalement statut);

    List<SignalementPanne> listerParAuteur(Long auteurId);
}