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

                // ----- Imane : ajouter ici les règles de ses URL (matériel, salles, réservations, signalements) -----

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