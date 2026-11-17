package ma.ensam.mycampus.web;

import ma.ensam.mycampus.dao.entities.TypeDocument;
import ma.ensam.mycampus.service.BusinessException;
import ma.ensam.mycampus.service.interfaces.TypeDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TypeDocumentController {

    private final TypeDocumentService typeDocumentService;

    @Autowired
    public TypeDocumentController(TypeDocumentService typeDocumentService) {
        this.typeDocumentService = typeDocumentService;
    }

    @GetMapping("/types-document")
    public String lister(Model model) {
        model.addAttribute("types", typeDocumentService.listerTous());
        return "typesdocument/liste";
    }

    @GetMapping("/types-document/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("typeDocument", new TypeDocument());
        return "typesdocument/formulaire";
    }

    @GetMapping("/types-document/modifier")
    public String modifier(@RequestParam Long id, Model model) {
        model.addAttribute("typeDocument", typeDocumentService.trouverParId(id));
        return "typesdocument/formulaire";
    }

    @PostMapping("/types-document/enregistrer")
    public String enregistrer(@ModelAttribute TypeDocument typeDocument, Model model) {
        try {
            if (typeDocument.getId() == null) {
                typeDocumentService.creer(typeDocument);
            } else {
                typeDocumentService.modifier(typeDocument.getId(), typeDocument);
            }
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            return "typesdocument/formulaire";
        }
        return "redirect:/types-document";
    }

    @PostMapping("/types-document/desactiver")
    public String desactiver(@RequestParam Long id, Model model) {
        try {
            typeDocumentService.desactiver(id);
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("types", typeDocumentService.listerTous());
            return "typesdocument/liste";
        }
        return "redirect:/types-document";
    }
}