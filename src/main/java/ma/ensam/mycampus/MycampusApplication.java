package ma.ensam.mycampus;

import ma.ensam.mycampus.service.interfaces.UtilisateurService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MycampusApplication {

    public static void main(String[] args) {
        SpringApplication.run(MycampusApplication.class, args);
    }

    @Bean
    public CommandLineRunner initialiserDonnees(UtilisateurService utilisateurService) {
        return args -> utilisateurService.initialiserPremierAdmin();
    }
}