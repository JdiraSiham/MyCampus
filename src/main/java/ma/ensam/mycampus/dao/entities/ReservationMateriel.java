package ma.ensam.mycampus.dao.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ma.ensam.mycampus.dao.entities.enums.StatutReservation;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_materiel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La date de début est obligatoire")
    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    private String motif;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateDemande;

    private LocalDateTime dateDecision;

    @ManyToOne(optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @ManyToOne
    @JoinColumn(name = "decideur_id")
    private Utilisateur decideur;
}
