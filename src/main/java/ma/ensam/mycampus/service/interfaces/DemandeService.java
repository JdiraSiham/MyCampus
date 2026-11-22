package ma.ensam.mycampus.service.interfaces;

import ma.ensam.mycampus.dao.entities.DemandeAdministrative;
import ma.ensam.mycampus.dao.entities.enums.StatutDemande;

import java.util.List;

public interface DemandeService {
    DemandeAdministrative deposer(Long etudiantId, Long typeDocumentId);
    List<DemandeAdministrative> listerPourEtudiant(Long etudiantId);
    DemandeAdministrative consulter(Long demandeId, Long acteurId);
    List<DemandeAdministrative> listerToutes(Long gestionnaireId);
    List<DemandeAdministrative> listerParStatut(Long gestionnaireId, StatutDemande statut);
    DemandeAdministrative prendreEnCharge(Long demandeId, Long gestionnaireId);
    DemandeAdministrative marquerPrete(Long demandeId, Long gestionnaireId);
    DemandeAdministrative rejeter(Long demandeId, Long gestionnaireId, String motif);
    List<DemandeAdministrative> rechercherPourEtudiant(Long etudiantId, StatutDemande statut, Long typeDocumentId);
    List<DemandeAdministrative> rechercherPourGestionnaire(Long gestionnaireId, String recherche,                                                    StatutDemande statut, Long typeDocumentId);
}