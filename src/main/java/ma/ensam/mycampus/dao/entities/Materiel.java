package ma.ensam.mycampus.dao.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.ensam.mycampus.dao.entities.enums.EtatMateriel;

@Entity
@Table(name = "materiel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Materiel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "La référence est obligatoire")
    @Column(nullable = false, unique = true)
    private String reference;

    private String description;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être strictement positive")
    @Column(nullable = false)
    private Integer quantite;

    @NotNull(message = "L'état est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatMateriel etat;

    @Column(nullable = false)
    private boolean actif = true;
}