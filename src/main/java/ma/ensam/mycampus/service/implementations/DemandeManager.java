package ma.ensam.mycampus.service.implementations;

import ma.ensam.mycampus.dao.entities.DemandeAdministrative;
import ma.ensam.mycampus.dao.entities.TypeDocument;
import ma.ensam.mycampus.dao.entities.Utilisateur;
import ma.ensam.mycampus.dao.entities.enums.Role;
import ma.ensam.mycampus.dao.entities.enums.StatutDemande;
import ma.ensam.mycampus.dao.repositories.DemandeRepository;
import ma.ensam.mycampus.dao.repositories.TypeDocumentRepository;
import ma.ensam.mycampus.dao.repositories.UtilisateurRepository;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.DemandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class DemandeManager implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TypeDocumentRepository typeDocumentRepository;

    @Autowired
    public DemandeManager(DemandeRepository demandeRepository, UtilisateurRepository utilisateurRepository, TypeDocumentRepository typeDocumentRepository) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.typeDocumentRepository = typeDocumentRepository;
    }

    @Override
    public DemandeAdministrative deposer(Long etudiantId, Long typeDocumentId) {
        Utilisateur etudiant = chargerAvecRole(etudiantId, Role.ETUDIANT);
        TypeDocument type = typeDocumentRepository.findById(typeDocumentId)
                .orElseThrow(() -> new BusinessException("Type de document introuvable."));
        if (!type.isActif()) {
            throw new BusinessException("Ce type de document n'est plus disponible.");
        }
        DemandeAdministrative demande = new DemandeAdministrative();
        demande.setEtudiant(etudiant);
        demande.setTypeDocument(type);
        demande.setStatut(StatutDemande.SOUMISE);
        return demandeRepository.save(demande);
    }

    @Override
    public List<DemandeAdministrative> listerPourEtudiant(Long etudiantId) {
        Utilisateur etudiant = chargerAvecRole(etudiantId, Role.ETUDIANT);
        return demandeRepository.findByEtudiant(etudiant);
    }

    @Override
    public DemandeAdministrative consulter(Long demandeId, Long acteurId) {
        Utilisateur acteur = chargerUtilisateurActif(acteurId);
        DemandeAdministrative demande = chargerDemande(demandeId);
        boolean estProprietaire = demande.getEtudiant().getId().equals(acteur.getId());
        boolean estGestionnaire = acteur.getRole() == Role.GESTIONNAIRE;
        if (!estProprietaire && !estGestionnaire) {
            throw new BusinessException("Accès refusé à cette demande.");
        }
        return demande;
    }

    @Override
    public List<DemandeAdministrative> listerToutes(Long gestionnaireId) {
        chargerAvecRole(gestionnaireId, Role.GESTIONNAIRE);
        return demandeRepository.findAll();
    }

    @Override
    public List<DemandeAdministrative> listerParStatut(Long gestionnaireId, StatutDemande statut) {
        chargerAvecRole(gestionnaireId, Role.GESTIONNAIRE);
        return demandeRepository.findByStatut(statut);
    }

    @Override
    public DemandeAdministrative prendreEnCharge(Long demandeId, Long gestionnaireId) {
        Utilisateur gestionnaire = chargerAvecRole(gestionnaireId, Role.GESTIONNAIRE);
        DemandeAdministrative demande = chargerDemande(demandeId);
        if (demande.getStatut() != StatutDemande.SOUMISE) {
            throw new BusinessException("Seule une demande soumise peut être prise en charge.");
        }
        demande.setStatut(StatutDemande.EN_COURS);
        demande.setAgentTraitant(gestionnaire);
        return demandeRepository.save(demande);
    }

    @Override
    public DemandeAdministrative marquerPrete(Long demandeId, Long gestionnaireId) {
        chargerAvecRole(gestionnaireId, Role.GESTIONNAIRE);
        DemandeAdministrative demande = chargerDemande(demandeId);
        if (demande.getStatut() != StatutDemande.EN_COURS) {
            throw new BusinessException("Seule une demande en cours peut être marquée prête.");
        }
        demande.setStatut(StatutDemande.PRETE);
        demande.setDateTraitement(LocalDateTime.now());
        return demandeRepository.save(demande);
    }

    @Override
    public DemandeAdministrative rejeter(Long demandeId, Long gestionnaireId, String motif) {
        chargerAvecRole(gestionnaireId, Role.GESTIONNAIRE);
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Un motif est obligatoire pour rejeter une demande.");
        }
        DemandeAdministrative demande = chargerDemande(demandeId);
        if (demande.getStatut() != StatutDemande.EN_COURS) {
            throw new BusinessException("Seule une demande en cours peut être rejetée.");
        }
        demande.setStatut(StatutDemande.REJETEE);
        demande.setMotif(motif.trim());
        demande.setDateTraitement(LocalDateTime.now());
        return demandeRepository.save(demande);
    }


    private DemandeAdministrative chargerDemande(Long id) {
        return demandeRepository.findById(id).orElseThrow(() -> new BusinessException("Demande introuvable."));
    }

    private Utilisateur chargerUtilisateurActif(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id).orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        if (!utilisateur.isActif()) {
            throw new BusinessException("Ce compte est désactivé.");
        }
        return utilisateur;
    }

    private Utilisateur chargerAvecRole(Long id, Role roleAttendu) {
        Utilisateur utilisateur = chargerUtilisateurActif(id);
        if (utilisateur.getRole() != roleAttendu) {
            throw new BusinessException("Action non autorisée pour ce rôle.");
        }
        return utilisateur;
    }

    @Override
    public List<DemandeAdministrative> rechercherPourEtudiant(Long etudiantId, StatutDemande statut, Long typeDocumentId) {
        Utilisateur etudiant = chargerAvecRole(etudiantId, Role.ETUDIANT);
        List<DemandeAdministrative> demandes = demandeRepository.findByEtudiant(etudiant, Sort.by(Sort.Direction.DESC, "dateDepot"));
        return filtrer(demandes, null, statut, typeDocumentId);
    }

    @Override
    public List<DemandeAdministrative> rechercherPourGestionnaire(Long gestionnaireId, String recherche, StatutDemande statut, Long typeDocumentId) {
        chargerAvecRole(gestionnaireId, Role.GESTIONNAIRE);
        List<DemandeAdministrative> demandes = demandeRepository.findAll(Sort.by(Sort.Direction.DESC, "dateDepot"));
        return filtrer(demandes, normaliser(recherche), statut, typeDocumentId);
    }

    private String normaliser(String texte) {
        return (texte == null || texte.isBlank()) ? null : texte.trim();
    }

    private List<DemandeAdministrative> filtrer(List<DemandeAdministrative> demandes, String texte, StatutDemande statut, Long typeDocumentId) {
        List<DemandeAdministrative> resultat = new ArrayList<>();
        for (DemandeAdministrative d : demandes) {
            boolean statutOk = (statut == null) || d.getStatut() == statut;
            boolean typeOk = (typeDocumentId == null) || d.getTypeDocument().getId().equals(typeDocumentId);
            boolean nomOk = nomCorrespond(d.getEtudiant(), texte);
            if (statutOk && typeOk && nomOk) {
                resultat.add(d);
            }
        }
        return resultat;
    }

    private boolean nomCorrespond(Utilisateur etudiant, String texte) {
        if (texte == null) {
            return true;
        }
        String recherche = texte.toLowerCase();
        String prenomNom = (etudiant.getPrenom() + " " + etudiant.getNom()).toLowerCase();
        String nomPrenom = (etudiant.getNom() + " " + etudiant.getPrenom()).toLowerCase();
        return prenomNom.contains(recherche) || nomPrenom.contains(recherche);
    }
}