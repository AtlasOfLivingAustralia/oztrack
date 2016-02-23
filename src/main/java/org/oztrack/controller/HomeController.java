package org.oztrack.controller;

import org.oztrack.data.access.SettingsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.TreeMap;

@Controller
public class HomeController {
    @Autowired
    private SettingsDao settingsDao;

    @InitBinder("text")
    public void initTextBinder(WebDataBinder binder) {
        binder.setAllowedFields();
    }

    @ModelAttribute("text")
    public String getText() throws Exception {
        return settingsDao.getSettings().getHomeText();
    }

    @RequestMapping(value="/", method=RequestMethod.GET)
    @PreAuthorize("permitAll")
    public String getHomeView(Model model) {
        model.addAttribute("summaryStats", settingsDao.getSummaryStatistics());
        return "home";
    }
}