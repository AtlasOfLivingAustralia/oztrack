package org.oztrack.validator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.geotools.referencing.CRS;
import org.oztrack.app.OzTrackApplication;
import org.oztrack.data.access.InstitutionDao;
import org.oztrack.data.access.ProjectDao;
import org.oztrack.data.model.Institution;
import org.oztrack.data.model.Project;
import org.oztrack.data.model.types.ProjectAccess;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class ProjectFormValidator implements Validator {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final ProjectDao projectDao;
    private final Date prevEmbargoDate;
    private final InstitutionDao institutionDao;

    public ProjectFormValidator(ProjectDao projectDao, Date prevEmbargoDate, InstitutionDao institutionDao) {
        this.projectDao = projectDao;
        this.prevEmbargoDate = prevEmbargoDate;
        this.institutionDao = institutionDao;
    }

    @Override
    public boolean supports(@SuppressWarnings("rawtypes") Class clazz) {
        return Project.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object obj, Errors errors) {
        Project project = (Project) obj;
        Date currentDate = new Date();
        Date createDate = (project.getCreateDate() != null) ? project.getCreateDate() : currentDate;
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "title", "error.empty.field", "Please enter a short project title.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "description", "error.empty.field", "Please enter a description for the project.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "spatialCoverageDescr", "error.empty.field", "Please enter a location description.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "speciesScientificName", "error.empty.field", "Please enter at least the Scientific Name of the species.");

        if (project.getAccess() == ProjectAccess.OPEN) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dataLicence", "error.empty.field", "A Data Licence must be selected for Open Access projects.");
        }
        else if (project.getAccess() == ProjectAccess.EMBARGO) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dataLicence", "error.empty.field", "A Data Licence must be selected for Delayed Open Access projects.");
        }

        if (project.getAccess() == ProjectAccess.EMBARGO) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "embargoDate", "error.empty.field", "An embargo date must be selected for Delayed Open Access projects.");
            if (project.getEmbargoDate() != null) {
                if (project.getEmbargoDate().before(DateUtils.truncate(currentDate, Calendar.DATE))) {
                    errors.rejectValue("embargoDate", "error.embargoDate", "Embargo date must be today's date or later.");
                }
                else {
                    if (project.getEmbargoDate().after(DateUtils.addYears(currentDate,1))) {
                        errors.rejectValue("embargoDate", "error.embargoDate", "Embargo period can only be extended up to 1 year at a time.");
                    }
                }
            }
        }

        if (project.getAccess() == ProjectAccess.CLOSED) {
                errors.rejectValue("access", "error.access", "Creation of Closed Access projects has been disabled.");
        }

        try {
            CRS.decode(project.getSrsIdentifier());
        }
        catch (Exception e) {
            errors.rejectValue("srsIdentifier", "error.srsidentifier", "Please enter a valid SRS code.");
        }

        Project projectWithSameTitle = projectDao.getProjectByTitle(project.getTitle());
        if ((projectWithSameTitle != null) && (projectWithSameTitle.getId() != project.getId())) {
            errors.rejectValue("title", "error.duplicateTitle", "Project with same title already exists.");
        }

        if (project.getInstitution() != null && institutionDao.getById(project.getInstitution().getId()) == null) {
            errors.rejectValue("institution", "error.institution", "Problem with the institution");
        }
    }
}