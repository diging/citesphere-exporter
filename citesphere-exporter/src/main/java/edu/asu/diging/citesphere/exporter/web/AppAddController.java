package edu.asu.diging.citesphere.exporter.web;

import java.security.Principal;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.asu.diging.citesphere.exporter.core.data.AppRepository;
import edu.asu.diging.citesphere.exporter.core.model.IApp;
import edu.asu.diging.citesphere.exporter.core.model.impl.App;
import edu.asu.diging.citesphere.exporter.core.service.ITokenManager;

@Controller
public class AppAddController {
    
    @Autowired
    private AppRepository repo;
    
    @Autowired
    private ITokenManager tokenManager;
    
    @RequestMapping(value="/admin/app/add", method=RequestMethod.GET)
    public String get(Model model) {
        model.addAttribute("app", new App());
        return "admin/app/add";
    }
    
    @RequestMapping(value="/admin/app/add", method=RequestMethod.POST)
    public String post(@ModelAttribute App app, Principal principal, RedirectAttributes redirectAttrs) {
        
        // the following needs to be moved into a manager class
        app.setCreatedBy(principal.getName());
        app.setCreatedOn(OffsetDateTime.now());
        IApp savedApp = repo.save(app);
        
        String jwt = tokenManager.createToken(savedApp, principal.getName());
        redirectAttrs.addFlashAttribute("token", jwt);
        
        return "redirect:/admin/app/" + savedApp.getId();
    }
}
