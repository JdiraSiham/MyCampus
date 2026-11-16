package ma.ensam.mycampus;

import ma.ensam.mycampus.dao.entities.Materiel;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.MaterielService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestMaterielRunner implements CommandLineRunner {

    private final MaterielService materielService;

    public TestMaterielRunner(MaterielService materielService) {
        this.materielService = materielService;
    }

    @Override
    public void run(String... args) {
        try {
            Materiel m = new Materiel();
            m.setNom("PC portable");
            m.setReference("PC-TEST-01");
            m.setQuantite(3);
            materielService.creer(m);
            System.out.println(">>> Matériel créé");
        } catch (BusinessException e) {
            System.out.println(">>> Erreur métier : " + e.getMessage());
        }
        materielService.lister().forEach(x ->
                System.out.println(">>> " + x.getNom() + " / " + x.getEtat()));
    }
}