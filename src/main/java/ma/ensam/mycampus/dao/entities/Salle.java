package ma.ensam.mycampus.dao.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "salle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false, unique = true)
    private String nom;

    @NotNull(message = "La capacité est obligatoire")
    @Positive(message = "La capacité doit être strictement positive")
    @Column(nullable = false)
    private Integer capacite;

    private String localisation;

    private String equipements;

    @Column(nullable = false)
    private boolean necessiteValidation;

    @Column(nullable = false)
    private boolean actif = true;
}