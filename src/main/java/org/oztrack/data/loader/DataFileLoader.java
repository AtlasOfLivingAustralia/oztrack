package org.oztrack.data.loader;

import static org.oztrack.util.OzTrackUtils.removeDuplicateLinesFromFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.oztrack.data.access.AnimalDao;
import org.oztrack.data.access.DataFileDao;
import org.oztrack.data.access.JdbcAccess;
import org.oztrack.data.access.PositionFixDao;
import org.oztrack.data.access.ProjectDao;
import org.oztrack.data.model.Animal;
import org.oztrack.data.model.DataFile;
import org.oztrack.error.FileProcessingException;

public abstract class DataFileLoader {
    protected final Logger logger = Logger.getLogger(getClass());

    protected DataFile dataFile;
    protected DataFileDao dataFileDao;
    protected EntityManager entityManager;

    private AnimalDao animalDao;
    private PositionFixDao positionFixDao;
    private ProjectDao projectDao;
    private JdbcAccess jdbcAccess;

    public DataFileLoader(
        DataFile dataFile,
        DataFileDao dataFileDao,
        AnimalDao animalDao,
        PositionFixDao positionFixDao,
        ProjectDao projectDao,
        EntityManager entityManager,
        JdbcAccess jdbcAccess
    ) {
        this.dataFile = dataFileDao.getDataFileById(dataFile.getId());
        this.dataFileDao = dataFileDao;
        this.animalDao = animalDao;
        this.positionFixDao = positionFixDao;
        this.projectDao = projectDao;
        this.entityManager = entityManager;
        this.jdbcAccess = jdbcAccess;
    }

    public void process() throws Exception {
        int duplicates = removeDuplicateLinesFromFile(this.dataFile.getAbsoluteDataFilePath());
        this.dataFile.appendStatusMessage(duplicates +" duplicates were found in data files");
        processRawObservations();
        processFinalObservations();
    }

    private void processRawObservations() throws Exception {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            logger.info("Inserting raw observations for " + this.dataFile.getAbsoluteDataFilePath());
            createRawObservations();
            logger.info("Checking animals for " + this.dataFile.getAbsoluteDataFilePath());
            checkAnimals();
            transaction.commit();
        }
        catch (Exception e) {
            try {
                transaction.rollback();
            }
            catch (Exception e2) {
            }
            throw e;
        }
    }

    private void processFinalObservations() throws Exception {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            logger.info("Creating final observations for " + this.dataFile.getAbsoluteDataFilePath());
            createFinalObservations();
            transaction.commit();
        }
        catch (Exception e) {
            try {
                transaction.rollback();
            }
            catch (Exception e2) {
            }
            throw e;
        }
    }

    public abstract void createRawObservations() throws FileProcessingException;

    private final String[] colours = new String[] {
        "#8DD3C7",
        "#FFFFB3",
        "#BEBADA",
        "#FB8072",
        "#80B1D3",
        "#FDB462",
        "#B3DE69",
        "#FCCDE5",
        "#D9D9D9",
        "#BC80BD",
        "#CCEBC5",
        "#FFED6F"
    };

    private void checkAnimals() throws FileProcessingException {
        if (dataFile.getSingleAnimalInFile()) {
            // only one animal in the file being uploaded. Create it.
            Animal animal = new Animal();
            animal.setProject(dataFile.getProject());
            animal.setCreateDescription("Created from data file " + dataFile.getUserGivenFileName() + " on " + dataFile.getCreateDate());
            animal.setCreateDate(new Date());
            animal.setCreateUser(dataFile.getCreateUser());
            animalDao.save(animal);
            animal.setProjectAnimalId(animal.getId().toString());
            animal.setAnimalName(animal.getId().toString());
            animal.setColour(colours[(int) (animal.getId() % colours.length)]);
            animalDao.update(animal);
        }
        else {
            List<String> newAnimalIdList = this.dataFileDao.getRawProjectAnimalIds(this.dataFile);
            List<Animal> projectAnimalList = animalDao.getAnimalsByProjectId(this.dataFile.getProject().getId());
            forNewAnimalId: for (String newAnimalId  : newAnimalIdList) {
                 for (Animal projectAnimal : projectAnimalList) {
                     if (newAnimalId.equals(projectAnimal.getProjectAnimalId()))   {
                         continue forNewAnimalId;
                     }
                 }
                 Animal newAnimal = new Animal();
                 newAnimal.setProjectAnimalId(newAnimalId);
                 newAnimal.setAnimalName(newAnimalId);
                 newAnimal.setAnimalDescription(null);
                 newAnimal.setProject(dataFile.getProject());
                 newAnimal.setCreateDate(new Date());
                 newAnimal.setCreateUser(dataFile.getCreateUser());
                 animalDao.save(newAnimal);
                 newAnimal.setColour(colours[(int) (newAnimal.getId() % colours.length)]);
                 animalDao.update(newAnimal);
            }
        }
    }

    private void createFinalObservations() throws FileProcessingException {
        try {
            int[] stats = jdbcAccess.statObservations(dataFile);
            dataFile.appendStatusMessage(stats[0] +" records were found in the data file.");
            dataFile.appendStatusMessage((stats[0]-stats[1]) +" records have been loaded, ignore uploading.");

            int count = jdbcAccess.loadObservations(dataFile);


            dataFile.setUpdateDate(new Date());
            dataFile.setUpdateUser(dataFile.getCreateUser());
            dataFile.appendStatusMessage(count + " observations have been added");
            dataFileDao.update(dataFile);
            jdbcAccess.truncateRawObservations(dataFile);
            List<Animal> animals = dataFileDao.getAnimals(dataFile);
            ArrayList<Long> animalIds = new ArrayList<Long>();
            for (Animal animal : animals) {
                animalIds.add(animal.getId());
            }
            positionFixDao.renumberPositionFixes(dataFile.getProject(), animalIds);
            dataFile.getProject().setUpdateDateForOaiPmh(new Date());
            projectDao.update(dataFile.getProject());
        }
        catch (Exception e) {
            jdbcAccess.truncateRawObservations(dataFile);
            throw new FileProcessingException(e.getMessage(), e);
        }
    }
}