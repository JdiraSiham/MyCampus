package ma.ensam.mycampus.dao.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "signalement_panne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignalementPanne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La description de la panne est obligatoire")
    @Column(nullable = false)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateSignalement;

    @Column(nullable = false)
    private boolean resolu = false;

    private LocalDateTime dateResolution;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    @ManyToOne
    @JoinColumn(name = "resolu_par_id")
    private Utilisateur resoluPar;
}