package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.SignalementPanne;

import java.util.List;

public interface SignalementPanneService {

    SignalementPanne signaler(Long auteurId, Long materielId, String description);

    SignalementPanne cloturer(Long signalementId, Long gestionnaireId);

    SignalementPanne trouverParId(Long id);

    List<SignalementPanne> listerTout();

    List<SignalementPanne> listerNonResolus();

    List<SignalementPanne> listerParMateriel(Long materielId);
}