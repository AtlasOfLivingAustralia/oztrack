package org.oztrack.controller;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.oztrack.data.access.AnimalDao;
import org.oztrack.data.access.Page;
import org.oztrack.data.access.PositionFixDao;
import org.oztrack.data.access.ProjectDao;
import org.oztrack.data.model.Animal;
import org.oztrack.data.model.PositionFix;
import org.oztrack.data.model.SearchQuery;
import org.oztrack.data.model.User;
import org.oztrack.validator.AnimalFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AnimalController {
    private final Logger logger = Logger.getLogger(getClass());
    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private AnimalDao animalDao;

    @Autowired
    private PositionFixDao positionFixDao;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private OzTrackPermissionEvaluator permissionEvaluator;

    @InitBinder("animal")
    public void initAnimalBinder(WebDataBinder binder) {
        binder.setAllowedFields(
             "projectAnimalId"
            ,"animalName"
            ,"animalDescription"
            ,"colour"
            ,"speciesScientificName"
            ,"speciesCommonName"
            ,"sex"
            ,"mass"
            ,"dimensions"
            ,"lifePhase"
            ,"experimentalContext "
            ,"captureLocation"
            ,"captureDate"
            ,"releaseDate"
            ,"tagDeployStartDate"
            ,"tagDeployEndDate"
            ,"tagManufacturerModel"
            ,"tagIdentifier"
            ,"tagDimensions"
            ,"tagAttachmentTechnique"
            ,"tagDeploymentComments"
            ,"dataRetrievalMethod"
        );
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }

    @ModelAttribute("animal")
    public Animal getAnimal(@PathVariable(value="id") Long animalId) throws Exception {
        return animalDao.getAnimalById(animalId);
    }

    @RequestMapping(value="/projects/{projectId}/animals/{id}", method=RequestMethod.GET)
    @PreAuthorize("hasPermission(#animal.project, 'read')")
    public String getView(Model model, @ModelAttribute("animal") Animal animal) {
        model.addAttribute("project", animal.getProject());
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setProject(animal.getProject());
        searchQuery.setAnimalIds(Arrays.asList(animal.getId()));
        searchQuery.setSortField("Detection Time");
        Page<PositionFix> positionFixPage = positionFixDao.getPage(searchQuery, 0, 15);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("positionFixPage", positionFixPage);
        return "animal";
    }

    @RequestMapping(value="/projects/{projectId}/animals/{id}/edit", method=RequestMethod.GET)
    @PreAuthorize("hasPermission(#animal.project, 'write')")
    public String getEditView(Model model, @ModelAttribute("animal") Animal animal) {
        model.addAttribute("project", animal.getProject());
        return "animal-form";
    }

    @RequestMapping(value="/projects/{projectId}/animals/{id}", method=RequestMethod.PUT)
    @PreAuthorize("hasPermission(#animal.project, 'write')")
    public String processUpdate(
        Authentication authentication,
        RedirectAttributes redirectAttributes,
        Model model,
        @ModelAttribute(value="animal") Animal animal,
        BindingResult bindingResult
    ) throws Exception {
        User currentUser = permissionEvaluator.getAuthenticatedUser(authentication);
        new AnimalFormValidator().validate(animal, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("project", animal.getProject());
            return "animal-form";
        }
        animal.setUpdateDate(new Date());
        animal.setUpdateUser(currentUser);
        animalDao.update(animal);
        positionFixDao.renumberPositionFixes(animal.getProject(), Arrays.asList(animal.getId()));
        return "redirect:/projects/" + animal.getProject().getId() + "/animals/" + animal.getId();
    }

    @RequestMapping(value="/projects/{projectId}/animals/{id}", method=RequestMethod.DELETE)
    @PreAuthorize("hasPermission(#animal.project, 'manage')")
    public void processDelete(@ModelAttribute(value="animal") Animal animal, HttpServletResponse response) {
        List<Long> animalIds = Arrays.asList(animal.getId());
        animalDao.delete(animal);
        positionFixDao.renumberPositionFixes(animal.getProject(), animalIds);
        animal.getProject().setUpdateDateForOaiPmh(new Date());
        projectDao.update(animal.getProject());
        response.setStatus(204);
    }
}
