package edu.asu.diging.citesphere.exporter.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.asu.diging.citesphere.exporter.core.data.AppRepository;

@Controller
public class AppListController {

    @Autowired
    private AppRepository repo;
    
    @RequestMapping("/admin/app/list")
    public String list(Model model) {
        model.addAttribute("apps", repo.findAll());
        return "admin/app/list";
    }
}
