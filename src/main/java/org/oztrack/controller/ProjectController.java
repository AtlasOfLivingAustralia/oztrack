package org.oztrack.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.oztrack.app.OzTrackConfiguration;
import org.oztrack.data.access.AnimalDao;
import org.oztrack.data.access.DataFileDao;
import org.oztrack.data.access.DataLicenceDao;
import org.oztrack.data.access.InstitutionDao;
import org.oztrack.data.access.OaiPmhRecordDao;
import org.oztrack.data.access.PersonDao;
import org.oztrack.data.access.PositionFixDao;
import org.oztrack.data.access.ProjectDao;
import org.oztrack.data.access.ProjectVisitDao;
import org.oztrack.data.access.SrsDao;
import org.oztrack.data.access.UserDao;
import org.oztrack.data.model.Animal;
import org.oztrack.data.model.DataFile;
import org.oztrack.data.model.Institution;
import org.oztrack.data.model.Person;
import org.oztrack.data.model.Project;
import org.oztrack.data.model.ProjectContribution;
import org.oztrack.data.model.ProjectUser;
import org.oztrack.data.model.ProjectVisit;
import org.oztrack.data.model.Publication;
import org.oztrack.data.model.User;
import org.oztrack.data.model.types.ProjectAccess;
import org.oztrack.data.model.types.ProjectVisitType;
import org.oztrack.data.model.types.Role;
import org.oztrack.util.EmailBuilder;
import org.oztrack.util.EmailBuilderFactory;
import org.oztrack.util.EmbargoUtils;
import org.oztrack.validator.ProjectFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProjectController {
    private final Logger logger = Logger.getLogger(getClass());

    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private OzTrackConfiguration configuration;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private ProjectVisitDao projectVisitDao;

    @Autowired
    private PositionFixDao positionFixDao;

    @Autowired
    private DataFileDao dataFileDao;

    @Autowired
    private AnimalDao animalDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private InstitutionDao institutionDao;

    @Autowired
    private SrsDao srsDao;

    @Autowired
    private DataLicenceDao dataLicenceDao;

    @Autowired
    private OaiPmhRecordDao oaiPmhRecordDao;

    @Autowired
    private EmailBuilderFactory emailBuilderFactory;

    @Autowired
    private OzTrackPermissionEvaluator permissionEvaluator;

    @InitBinder("project")
    public void initProjectBinder(WebDataBinder binder) {
        binder.setAllowedFields(
            "title",
            "description",
            "spatialCoverageDescr",
            "speciesCommonName",
            "speciesScientificName",
            "srsIdentifier",
            "access",
            "rightsStatement",
            "dataManipulation",
            "locationAccuracyComments",
            "licencingAndEthics"
        );
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }

    @ModelAttribute("project")
    public Project getProject(@PathVariable(value="id") Long projectId) {
        return projectDao.getProjectById(projectId);
    }

    @RequestMapping(value="/projects/{id}", method=RequestMethod.GET)
    @PreAuthorize("permitAll")
    public String getSummaryView(Model model, @ModelAttribute(value="project") Project project) {
        projectVisitDao.save(new ProjectVisit(project, ProjectVisitType.SUMMARY, new Date()));
        Role[] roles = Role.values();
        HashMap<Role, List<ProjectUser>> projectUsersByRole = new HashMap<Role, List<ProjectUser>>();
        for (Role role : roles) {
            projectUsersByRole.put(role, projectDao.getProjectUsersWithRole(project, role));
        }
        model.addAttribute("roles", roles);
        model.addAttribute("projectUsersByRole", projectUsersByRole);
        model.addAttribute("projectBoundingBox", projectDao.getBoundingBox(project, false));
        model.addAttribute("projectDetectionDateRange", projectDao.getDetectionDateRange(project, false));
        model.addAttribute("projectDetectionCount", projectDao.getDetectionCount(project, false));
        return getView(model, project, "project");
    }

    @RequestMapping(value="/projects/{id}/animals", method=RequestMethod.GET)
    @PreAuthorize("hasPermission(#project, 'read')")
    public String getAnimalsView(Model model, @ModelAttribute(value="project") Project project) {
        return getView(model, project, "project-animals");
    }

    @RequestMapping(value="/projects/{id}/edit", method=RequestMethod.GET)
    @PreAuthorize("hasPermission(#project, 'write')")
    public String getEditView(Model model, @ModelAttribute(value="project") Project project) {
        addFormAttributes(model, project);
        return "project-form";
    }

    @RequestMapping(value="/projects/{id}", method=RequestMethod.PUT)
    @PreAuthorize("hasPermission(#project, 'write')")
    public String processUpdate(
        Authentication authentication,
        Model model,
        @ModelAttribute(value="project") Project project,
        BindingResult bindingResult,
        @RequestParam(value="embargoDate", required=false) String embargoDateString,
        @RequestParam(value="crosses180", required=false, defaultValue="false") Boolean crosses180,
        @RequestParam(value="dataLicenceIdentifier", required=false) String dataLicenceIdentifier,
        HttpServletRequest request
    ) throws Exception {
        // Using @RequestParam for these fails when only one value provided:
        // Spring decides a single value containing commas should be expanded to a list
        // (e.g. ["a, b, c"] becomes ["a", "b", "c"] instead of being interpreted as ["a, b, c"]).
        // Note that two or more values is handled correctly (e.g. ["a, b", "c"]).
        String[] publicationReferenceParam = request.getParameterValues("publicationReference");
        String[] publicationUrlParam = request.getParameterValues("publicationUrl");
        String[] contributorIdParam = request.getParameterValues("contributor");

        Date prevEmbargoDate = project.getEmbargoDate();
        if (project.getAccess().equals(ProjectAccess.EMBARGO) && StringUtils.isNotBlank(embargoDateString)) {
            Date embargoDate = isoDateFormat.parse(embargoDateString);
            if (!embargoDate.equals(project.getEmbargoDate())) {
                project.setEmbargoDate(embargoDate);
                project.setEmbargoNotificationDate(null);
            }
        }
        else {
            project.setEmbargoDate(null);
            project.setEmbargoNotificationDate(null);
        }

        if ((project.getAccess() != ProjectAccess.CLOSED) && StringUtils.isNotBlank(dataLicenceIdentifier)) {
            project.setDataLicence(dataLicenceDao.getByIdentifier(dataLicenceIdentifier));
        }
        else {
            project.setDataLicence(null);
        }

        boolean shouldRenumberPositionFixes = !project.getCrosses180().equals(crosses180);
        project.setCrosses180(crosses180);

        setProjectPublications(project, bindingResult, publicationReferenceParam, publicationUrlParam);

        List<ProjectContribution> previousContributions = new ArrayList<ProjectContribution>(project.getProjectContributions());
        setProjectContributions(project, bindingResult, contributorIdParam, personDao);

        new ProjectFormValidator(projectDao, prevEmbargoDate).validate(project, bindingResult);

        if (bindingResult.hasErrors()) {
            project.setEmbargoDate(prevEmbargoDate);
            addFormAttributes(model, project);
            return "project-form";
        }

        User currentUser = permissionEvaluator.getAuthenticatedUser(authentication);
        Date currentDate = new Date();
        project.setUpdateUser(currentUser);
        project.setUpdateDate(currentDate);
        project.setUpdateDateForOaiPmh(currentDate);

        projectDao.update(project);

        projectDao.setIncludeInOaiPmh(project);

        oaiPmhRecordDao.updateOaiPmhSets();

        if (shouldRenumberPositionFixes) {
            ArrayList<Long> animalIds = new ArrayList<Long>();
            for (Animal animal : project.getAnimals()) {
                animalIds.add(animal.getId());
            }
            positionFixDao.renumberPositionFixes(project, animalIds);
        }

        respondToContributionsChange(
            configuration,
            emailBuilderFactory,
            logger,
            currentUser,
            project,
            previousContributions,
            project.getProjectContributions()
        );

        return "redirect:/projects/" + project.getId();
    }

    @SuppressWarnings("unchecked")
    public static void respondToContributionsChange(
        OzTrackConfiguration configuration,
        EmailBuilderFactory emailBuilderFactory,
        Logger logger,
        User currentUser,
        Project project,
        List<ProjectContribution> previousContributions,
        List<ProjectContribution> currentContributions
    ) {
        Transformer contributionToContributorTransformer = new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((ProjectContribution) input).getContributor();
            }
        };
        Collection<Person> previousContributors = CollectionUtils.collect(previousContributions, contributionToContributorTransformer);
        Collection<Person> currentContributors = CollectionUtils.collect(currentContributions, contributionToContributorTransformer);
        if (previousContributors.equals(currentContributors)) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("Project contributors list changed.\n");

        Transformer contributorToFullNameTransformer = new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((Person) input).getFullName();
            }
        };

        Collection<Person> addedContributors = CollectionUtils.subtract(currentContributors, previousContributors);
        if (!addedContributors.isEmpty()) {
            Collection<String> addedContributorNames = CollectionUtils.collect(addedContributors, contributorToFullNameTransformer);
            message.append(StringUtils.join(addedContributorNames, ", ") + " added to list of project contributors.\n");
        }
        Collection<Person> removedContributors = CollectionUtils.subtract(previousContributors, currentContributors);
        if (!removedContributors.isEmpty()) {
            Collection<String> removedContributorNames = CollectionUtils.collect(removedContributors, contributorToFullNameTransformer);
            message.append(StringUtils.join(removedContributorNames, ", ") + " removed from list of project contributors.\n");
        }

        Collection<String> previousContributorNames = CollectionUtils.collect(previousContributors, contributorToFullNameTransformer);
        message.append("Previous contributors: " + StringUtils.join(previousContributorNames, ", ") + "\n");
        Collection<String> currentContributorNames = CollectionUtils.collect(currentContributors, contributorToFullNameTransformer);
        message.append("Current contributors: " + StringUtils.join(currentContributorNames, ", "));
        logger.info(message.toString());

        Collection<Person> notifiedContributors = CollectionUtils.union(previousContributors, currentContributors);
        notifiedContributors.remove(currentUser.getPerson());
        for (Person notifiedContributor : notifiedContributors) {
            try {
                EmailBuilder emailBuilder = emailBuilderFactory.getObject();
                emailBuilder.to(notifiedContributor);
                emailBuilder.subject("ZoaTrack project contributor change");

                StringBuilder htmlMsgContent = new StringBuilder();

                if (addedContributors.contains(notifiedContributor)) {
                    htmlMsgContent.append("<p>\n");
                    htmlMsgContent.append("    You have been listed as a contributor to ZoaTrack project\n");
                    htmlMsgContent.append("    <i>" + project.getTitle() + "</i>.\n");
                    htmlMsgContent.append("</p>\n");
                    htmlMsgContent.append("<p>The full list of contributors is:</p>\n");
                    appendContributorsList(currentContributors, htmlMsgContent);
                }
                else {
                    if (removedContributors.contains(notifiedContributor)) {
                        htmlMsgContent.append("<p>\n");
                        htmlMsgContent.append("    You have been removed as a contributor to ZoaTrack project\n");
                        htmlMsgContent.append("    <i>" + project.getTitle() + "</i>.\n");
                        htmlMsgContent.append("</p>\n");
                    }
                    else {
                        htmlMsgContent.append("<p>\n");
                        htmlMsgContent.append("    The list of contributors to ZoaTrack project\n");
                        htmlMsgContent.append("    <i>" + project.getTitle() + "</i>\n");
                        htmlMsgContent.append("    has been updated.\n");
                        htmlMsgContent.append("</p>\n");
                    }
                    htmlMsgContent.append("<p>The previous list of contributors was:</p>\n");
                    appendContributorsList(previousContributors, htmlMsgContent);
                    htmlMsgContent.append("<p>The current list of contributors is:</p>\n");
                    appendContributorsList(currentContributors, htmlMsgContent);
                }

                {
                    String projectLink = configuration.getBaseUrl() + "/projects/" + project.getId();
                    htmlMsgContent.append("<p>\n");
                    htmlMsgContent.append("    To view the project, click here:\n");
                    htmlMsgContent.append("    <a href=\"" + projectLink + "\">" + projectLink + "</a>\n");
                    htmlMsgContent.append("</p>\n");
                }

                if (notifiedContributor.getUser() == null) {
                    htmlMsgContent.append("<p style=\"color: #333333;\">\n");
                    htmlMsgContent.append("    <b>What is ZoaTrack?</b>\n");
                    htmlMsgContent.append("</p>\n");
                    String websiteLink = configuration.getBaseUrl() + "/";
                    htmlMsgContent.append("<p>\n");
                    htmlMsgContent.append("    ZoaTrack is a free-to-use web-based platform for analysing and visualising\n");
                    htmlMsgContent.append("    individual-based animal location data. It was primarily developed for the\n");
                    htmlMsgContent.append("    Australian animal telemetry community but can be used to assess animal\n");
                    htmlMsgContent.append("    movement and estimate space-use for individually-marked animals anywhere\n");
                    htmlMsgContent.append("    in the world. To find out more, visit \n");
                    htmlMsgContent.append("    <a href=\"" + websiteLink + "\">" + websiteLink + "</a>.");
                    htmlMsgContent.append("</p>\n");
                    htmlMsgContent.append("<p style=\"color: #333333;\">\n");
                    htmlMsgContent.append("    <b>Register an ZoaTrack account</b>\n");
                    htmlMsgContent.append("</p>\n");
                    htmlMsgContent.append("<p>\n");
                    htmlMsgContent.append("    The project owner entered your details into ZoaTrack,\n");
                    htmlMsgContent.append("    creating a record consisting of your name and email address.\n");
                    htmlMsgContent.append("    To register an ZoaTrack user account based on this record, click the following link:\n");
                    htmlMsgContent.append("</p>\n");
                    String registrationLink = configuration.getBaseUrl() + "/users/new?person=" + notifiedContributor.getUuid();
                    htmlMsgContent.append("</p>\n");
                    htmlMsgContent.append("    <a href=\"" + registrationLink + "\">" + registrationLink + "</a>\n");
                    htmlMsgContent.append("<p>\n");
                    htmlMsgContent.append("<p>\n");
                    htmlMsgContent.append("    <span style=\"color: #ff9900;\"><b>Why register?</b></span>\n");
                    htmlMsgContent.append("    Having an account in ZoaTrack allows you to update your profile and create new projects.\n");
                    htmlMsgContent.append("</p>\n");
                }
                if (addedContributors.contains(notifiedContributor)) {
                    htmlMsgContent.append("<p style=\"color: #333333;\">\n");
                    htmlMsgContent.append("    <b>Remove your listing as a contributor</b>\n");
                    htmlMsgContent.append("</p>\n");
                    htmlMsgContent.append("<p>\n");
                    htmlMsgContent.append("    If you believe you were added in error or would prefer not to be listed in ZoaTrack,\n");
                    htmlMsgContent.append("    you can automatically remove your record by clicking the following link:\n");
                    htmlMsgContent.append("</p>\n");
                    String rejectionLink = configuration.getBaseUrl() + "/projects/" + project.getId() + "/reject?person=" + notifiedContributor.getUuid();
                    htmlMsgContent.append("</p>\n");
                    htmlMsgContent.append("    <a href=\"" + rejectionLink + "\">" + rejectionLink + "</a>\n");
                    htmlMsgContent.append("<p>\n");
                    emailBuilder.htmlMsgContent(htmlMsgContent.toString());
                }

                emailBuilder.build().send();
            }
            catch (Exception e) {
                logger.error("Error sending notification to " + notifiedContributor.getFullName() + " " + notifiedContributor.getEmail(), e);
            }
        }
    }

    private static void appendContributorsList(Collection<Person> previousContributors, StringBuilder htmlMsgContent) {
        htmlMsgContent.append("<ul>\n");
        for (Person previousContributor : previousContributors) {
            htmlMsgContent.append("    <li>\n");
            htmlMsgContent.append("        <span style=\"color: #777777;\"><b>" + previousContributor.getFullName() + "</b></span>");
            if (previousContributor.getInstitutions().isEmpty()) {
                htmlMsgContent.append("\n");
            }
            else {
                htmlMsgContent.append(",\n");
                for (Iterator<Institution> iterator = previousContributor.getInstitutions().iterator(); iterator.hasNext();) {
                    Institution institution = iterator.next();
                    htmlMsgContent.append("        " + institution.getTitle());
                    if (iterator.hasNext()) {
                        htmlMsgContent.append(" /");
                    }
                    htmlMsgContent.append("\n");
                }
            }
            htmlMsgContent.append("    </li>\n");
        }
        htmlMsgContent.append("</ul>\n");
    }

    public static void setProjectPublications(
        Project project,
        BindingResult bindingResult,
        String[] publicationReferenceParam,
        String[] publicationUrlParam
    ) {
        List<String> publicationReferences = (publicationReferenceParam != null)
            ? Arrays.asList(publicationReferenceParam)
            : Collections.<String>emptyList();
        List<String> publicationUrls = (publicationUrlParam != null)
            ? Arrays.asList(publicationUrlParam)
            : Collections.<String>emptyList();
        project.getPublications().clear();
        int numPublicationReferences = (publicationReferences != null) ? publicationReferences.size() : 0;
        int numPublicationUrls = (publicationUrls != null) ? publicationUrls.size() : 0;
        int numPublications = Math.max(numPublicationReferences, numPublicationUrls);
        for (int i = 0; i < numPublications; i++) {
            String reference = (i < numPublicationReferences) ? publicationReferences.get(i) : null;
            String url = (i < numPublicationUrls) ? publicationUrls.get(i) : null;
            if (StringUtils.isBlank(reference) && StringUtils.isBlank(url)) {
                continue;
            }
            if (StringUtils.isBlank(reference)) {
                bindingResult.rejectValue("publications", "publications", "All publications must have a Reference.");
            }
            if (StringUtils.isNotBlank(url)) {
                try {
                    // Prepend "http://" to URLs that look like they need it.
                    URI uri = new URI(url);
                    String prefix = "";
                    if (uri.getScheme() == null) {
                        prefix += "http:";
                    }
                    if (uri.getAuthority() == null) {
                        prefix += "//";
                    }
                    url = prefix + url;
                    new URI(url).toURL();
                }
                catch (Exception e) {
                    bindingResult.rejectValue("publications", "publications", "Invalid URL provided: \"" + url + "\".");
                }
            }
            Publication publication = new Publication();
            publication.setProject(project);
            publication.setOrdinal(project.getPublications().size());
            publication.setReference(reference);
            publication.setUrl(url);
            project.getPublications().add(publication);
        }
    }

    public static void setProjectContributions(
        Project project,
        BindingResult bindingResult,
        String[] conbtributorIdParam,
        PersonDao personDao
    ) {
        List<String> contributorIds = (conbtributorIdParam != null) ? Arrays.asList(conbtributorIdParam) : Collections.<String>emptyList();
        project.getProjectContributions().clear();
        for (String contributorId : contributorIds) {
            if (StringUtils.isBlank(contributorId)) {
                continue;
            }
            Person contributor = personDao.getById(Long.valueOf(contributorId));
            if (contributor == null) {
                bindingResult.rejectValue("projectContributions", "error.projectContributions", "Person not found with supplied ID.");
            }
            ProjectContribution projectContribution = new ProjectContribution();
            projectContribution.setProject(project);
            projectContribution.setContributor(contributor);
            projectContribution.setOrdinal(project.getProjectContributions().size());
            project.getProjectContributions().add(projectContribution);
        }
    }

    private void addFormAttributes(Model model, Project project) {
        GregorianCalendar currentCalendar = new GregorianCalendar();
        model.addAttribute("people", personDao.getAllOrderedByName());
        model.addAttribute("dataLicences", dataLicenceDao.getAll());
        model.addAttribute("srsList", srsDao.getAllOrderedByBoundsAreaDesc());
        model.addAttribute("currentYear", currentCalendar.get(Calendar.YEAR));
        model.addAttribute("currentDate", currentCalendar.getTime());
        boolean beforeClosedAccessDisableDate =
            (configuration.getClosedAccessDisableDate() == null) ||
            (project.getCreateDate().before(configuration.getClosedAccessDisableDate()));
        model.addAttribute("beforeClosedAccessDisableDate", beforeClosedAccessDisableDate);
        addEmbargoDateFormAttributes(model, project, currentCalendar.getTime());
    }

    private void addEmbargoDateFormAttributes(Model model, Project project, Date currentDate) {
        final Date truncatedCurrentDate = DateUtils.truncate(currentDate, Calendar.DATE);
        final Date truncatedCreateDate = DateUtils.truncate(project.getCreateDate(), Calendar.DATE);

        EmbargoUtils.EmbargoInfo embargoInfo = EmbargoUtils.getEmbargoInfo(project.getCreateDate(), project.getEmbargoDate());

        boolean beforeNonIncrementalEmbargoDisableDate =
            (configuration.getNonIncrementalEmbargoDisableDate() == null) ||
            (project.getCreateDate().before(configuration.getNonIncrementalEmbargoDisableDate()));
        model.addAttribute("beforeNonIncrementalEmbargoDisableDate", beforeNonIncrementalEmbargoDisableDate);

        model.addAttribute("minEmbargoDate", truncatedCurrentDate);
        model.addAttribute("maxEmbargoDate", embargoInfo.getMaxEmbargoDate());
        model.addAttribute("maxEmbargoYears", embargoInfo.getMaxEmbargoYears());
        model.addAttribute("maxIncrementalEmbargoDate", embargoInfo.getMaxIncrementalEmbargoDate());

        LinkedHashMap<String, Date> presetEmbargoDates = new LinkedHashMap<String, Date>();
        Date otherEmbargoDate = null;
        if (beforeNonIncrementalEmbargoDisableDate) {
            for (int years = 1; years <= embargoInfo.getMaxEmbargoYears(); years++) {
                String key = years + " " + ((years == 1) ? "year" : "years");
                Date value = DateUtils.addYears(truncatedCreateDate, years);
                presetEmbargoDates.put(key, value);
            }
            // Set otherEmbargoDate field if it doesn't match any of the presets
            if (project.getEmbargoDate() != null) {
                otherEmbargoDate = project.getEmbargoDate();
                DateUtils.truncate(project.getEmbargoDate(), Calendar.DATE);
                for (Date presetEmbargoDate : presetEmbargoDates.values()) {
                    if (otherEmbargoDate.getTime() == presetEmbargoDate.getTime()) {
                        otherEmbargoDate = null;
                        break;
                    }
                }
            }
        }
        else {
            if (project.getEmbargoDate() != null) {
                presetEmbargoDates.put("Current embargo", project.getEmbargoDate());
            }
            if ((project.getEmbargoDate() == null) || project.getEmbargoDate().before(embargoInfo.getMaxIncrementalEmbargoDate())) {
                if (embargoInfo.getMaxIncrementalEmbargoDate().before(embargoInfo.getMaxEmbargoDate())) {
                    presetEmbargoDates.put("Extend by 1 year", embargoInfo.getMaxIncrementalEmbargoDate());
                }
                else {
                    presetEmbargoDates.put("Extend to " + embargoInfo.getMaxEmbargoYears() + " year limit", embargoInfo.getMaxEmbargoDate());
                }
            }
        }
        model.addAttribute("presetEmbargoDates", presetEmbargoDates);
        model.addAttribute("otherEmbargoDate", otherEmbargoDate);
    }

    @RequestMapping(value="/projects/{id}", method=RequestMethod.DELETE)
    @PreAuthorize("hasPermission(#project, 'delete')")
    public void processDelete(@ModelAttribute(value="project") Project project, HttpServletResponse response) {
        ArrayList<Long> animalIds = new ArrayList<Long>();
        for (Animal animal : project.getAnimals()) {
            animalIds.add(animal.getId());
        }
        projectDao.delete(project);
        positionFixDao.renumberPositionFixes(project, animalIds);
        response.setStatus(204);
    }

    private String getView(Model model, Project project, String viewName) {
        List<Animal> projectAnimalsList = animalDao.getAnimalsByProjectId(project.getId());
        List<DataFile> dataFileList = dataFileDao.getDataFilesByProject(project);
        model.addAttribute("project", project);
        model.addAttribute("projectAnimalsList", projectAnimalsList);
        model.addAttribute("dataFileList", dataFileList);
        return viewName;
    }

    @RequestMapping(value="/projects/{id}/users", method=RequestMethod.POST, produces="application/xml")
    @PreAuthorize("hasPermission(#project, 'manage')")
    public void processAddUser(
        Authentication authentication,
        Model model,
        @ModelAttribute(value="project") Project project,
        @RequestParam(value="user-id") Long userId,
        @RequestParam(value="role") String role,
        HttpServletResponse response
    ) throws IOException {
        User currentUser = permissionEvaluator.getAuthenticatedUser(authentication);
        if (userId == null) {
            writeAddUserResponse(response.getWriter(), "No user selected");
            response.setStatus(400);
            return;
        }
        User user = userDao.getById(userId);
        if (user == null) {
            writeAddUserResponse(response.getWriter(), "Invalid user ID supplied");
            response.setStatus(400);
            return;
        }
        for (ProjectUser projectUser : project.getProjectUsers()) {
            if (projectUser.getUser().equals(user)) {
                writeAddUserResponse(response.getWriter(), "Already assigned to project");
                response.setStatus(400);
                return;
            }
        }
        ProjectUser projectUser = new ProjectUser();
        projectUser.setProject(project);
        projectUser.setUser(user);
        projectUser.setRole(Role.fromIdentifier(role));
        project.getProjectUsers().add(projectUser);
        project.setUpdateDate(new Date());
        project.setUpdateUser(currentUser);
        projectDao.update(project);
        writeAddUserResponse(response.getWriter(), null);
        response.setStatus(204);
    }

    private static void writeAddUserResponse(PrintWriter out, String error) {
        out.append("<?xml version=\"1.0\"?>\n");
        out.append("<add-project-user-response xmlns=\"http://oztrack.org/xmlns#\">\n");
        if (error != null) {
            out.append("    <error>" + error + "</error>\n");
        }
        out.append("</add-project-user-response>\n");
    }

    @RequestMapping(value="/projects/{id}/users/{userId}", method=RequestMethod.DELETE)
    @PreAuthorize("hasPermission(#project, 'manage')")
    public void processUserDelete(
        Authentication authentication,
        @ModelAttribute(value="project") Project project,
        @PathVariable(value="userId") Long userId,
        HttpServletResponse response
    ) throws IOException {
        User currentUser = permissionEvaluator.getAuthenticatedUser(authentication);
        if (userId == null) {
            writeDeleteUserResponse(response.getWriter(), "No user selected");
            response.setStatus(400);
            return;
        }
        User user = userDao.getById(userId);
        if (user == null) {
            writeDeleteUserResponse(response.getWriter(), "Invalid user ID supplied");
            response.setStatus(400);
            return;
        }
        ProjectUser foundProjectUser = null;
        boolean foundOtherManager = false;
        for (ProjectUser projectUser : project.getProjectUsers()) {
            if (projectUser.getUser().equals(user)) {
                foundProjectUser = projectUser;
            }
            else if (projectUser.getRole() == Role.MANAGER) {
                foundOtherManager = true;
            }
        }
        if (foundProjectUser == null) {
            writeDeleteUserResponse(response.getWriter(), "User not assigned to project");
            response.setStatus(400);
            return;
        }
        if (!foundOtherManager) {
            writeDeleteUserResponse(response.getWriter(), "There must be at least one manager remaining on a project");
            response.setStatus(400);
            return;
        }
        project.getProjectUsers().remove(foundProjectUser);
        project.setUpdateDate(new Date());
        project.setUpdateUser(currentUser);
        projectDao.update(project);
        writeDeleteUserResponse(response.getWriter(), null);
        response.setStatus(204);
    }

    private static void writeDeleteUserResponse(PrintWriter out, String error) {
        out.append("<?xml version=\"1.0\"?>\n");
        out.append("<delete-project-user-response xmlns=\"http://oztrack.org/xmlns#\">\n");
        if (error != null) {
            out.append("    <error>" + error + "</error>\n");
        }
        out.append("</delete-project-user-response>\n");
    }
}
