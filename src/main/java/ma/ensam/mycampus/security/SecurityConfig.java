package ma.ensam.mycampus.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Pages accessibles sans connexion
                        .requestMatchers("/login", "/error", "/css/**", "/webjars/**").permitAll()

                        // ----- ADMIN -----
                        .requestMatchers("/types-document/**").hasRole("ADMIN")

                        // ----- GESTIONNAIRE -----
                        .requestMatchers("/demandes/gestion", "/demandes/prendre-en-charge",
                                "/demandes/prete", "/demandes/rejeter")
                        .hasRole("GESTIONNAIRE")

                        // ----- ETUDIANT -----
                        .requestMatchers("/demandes/nouvelle", "/demandes/deposer").hasRole("ETUDIANT")

                        // Étudiant (ses demandes) et gestionnaire
                        .requestMatchers("/demandes", "/demandes/detail")
                        .hasAnyRole("ETUDIANT", "GESTIONNAIRE")

                        // ===== Imane : matériel, salles, réservations, signalements =====
                        // (les règles les plus précises d'abord, puis la règle générale ; l'ADMIN n'y figure jamais)

                        // --- GESTIONNAIRE seulement ---
                        .requestMatchers("/materiels/nouveau", "/materiels/modifier", "/materiels/enregistrer",
                                "/materiels/etat", "/materiels/desactiver",
                                "/salles/nouveau", "/salles/modifier", "/salles/enregistrer",
                                "/salles/desactiver",
                                "/reservations-materiel/gestion", "/reservations-materiel/valider",
                                "/reservations-materiel/refuser",
                                "/reservations-salle/gestion", "/reservations-salle/valider",
                                "/reservations-salle/refuser",
                                "/signalements/cloturer")
                        .hasRole("GESTIONNAIRE")

                        // --- ETUDIANT seulement (seul l'étudiant réserve et annule) ---
                        .requestMatchers("/reservations-materiel/mes-reservations", "/reservations-materiel/nouvelle",
                                "/reservations-materiel/reserver", "/reservations-materiel/annuler",
                                "/reservations-salle/mes-reservations", "/reservations-salle/disponibles",
                                "/reservations-salle/nouvelle", "/reservations-salle/reserver",
                                "/reservations-salle/annuler",
                                "/signalements/mes-signalements")
                        .hasRole("ETUDIANT")

                        // --- ETUDIANT et GESTIONNAIRE (consultation, signalement) ---
                        .requestMatchers("/materiels/**", "/salles/**",
                                "/reservations-materiel/**", "/reservations-salle/**",
                                "/signalements/**")
                        .hasAnyRole("ETUDIANT", "GESTIONNAIRE")

                        // Tout le reste (accueil, mot de passe...) : utilisateur connecté
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }
}