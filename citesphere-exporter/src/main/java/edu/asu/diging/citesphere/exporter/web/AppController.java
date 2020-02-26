package edu.asu.diging.citesphere.exporter.web;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.asu.diging.citesphere.exporter.core.data.AppRepository;
import edu.asu.diging.citesphere.exporter.core.model.impl.App;

@Controller
public class AppController {

    @Autowired
    private AppRepository repo;
    
    @RequestMapping("/admin/app/{id}")
    public String get(@PathVariable String id, Model model) {
        Optional<App> optional = repo.findById(id);
        if (optional.isPresent()) {
            model.addAttribute("app", optional.get());
        }
        
        return "admin/app/app";
    }
}
