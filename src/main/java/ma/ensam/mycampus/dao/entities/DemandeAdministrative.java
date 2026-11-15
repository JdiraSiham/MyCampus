package ma.ensam.mycampus.dao.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.ensam.mycampus.dao.entities.enums.StatutDemande;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DemandeAdministrative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateDepot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemande statut = StatutDemande.SOUMISE;

    @Column(length = 500)
    private String motif;

    private LocalDateTime dateTraitement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Utilisateur etudiant;

    @ManyToOne
    @JoinColumn(name = "agent_traitant_id")
    private Utilisateur agentTraitant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_document_id", nullable = false)
    private TypeDocument typeDocument;
}