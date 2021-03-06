package org.oztrack.view;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oztrack.app.OzTrackConfiguration;
import org.oztrack.data.model.Animal;
import org.oztrack.data.model.PositionFix;
import org.springframework.web.servlet.view.AbstractView;

public class DetectionsKMLView extends AbstractView{
    private final SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final OzTrackConfiguration configuration;
    private final List<Animal> animals;
    private final List<PositionFix> positionFixList;

    public DetectionsKMLView(
        OzTrackConfiguration configuration,
        List<Animal> animals,
        List<PositionFix> positionFixList
    ) {
        this.configuration = configuration;
        this.animals = animals;
        this.positionFixList = positionFixList;
    }

    @Override
    protected void renderMergedOutputModel(
        @SuppressWarnings("rawtypes") Map model,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {
        if (animals.isEmpty() || positionFixList.isEmpty()) {
            response.setStatus(404);
            return;
        }

        String fileName = "detections.kml";
        response.setHeader("Content-Disposition", "attachment; filename=\""+ fileName + "\"");
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");

        Date fromDate = positionFixList.get(0).getDetectionTime();
        Date toDate = positionFixList.get(positionFixList.size() - 1).getDetectionTime();

        PrintWriter writer = response.getWriter();
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        writer.append("<Document>\n");
        writer.append("  <description>\n");
        writer.append("    <p>Detections</p>\n");
        writer.append("    <p>" + dateFormat.format(fromDate) + " - " + dateFormat.format(toDate) + "</p>\n");
        writer.append("    <p>Generated by ZoaTrack\n" + (configuration.getBaseUrl() + "/") + "</p>\n");
        writer.append("  </description>\n");
        for (Animal animal : animals) {
            Matcher matcher = Pattern.compile("^#(..)(..)(..)$").matcher(animal.getColour());
            if (matcher.matches()) {
                String kmlBaseColour = matcher.group(3) + matcher.group(2) + matcher.group(1);
                String kmlIconColour = "cc" + kmlBaseColour; // 80% opacity
                writer.append("<Style id=\"animal-" + animal.getId() + "\">\n");
                writer.append("  <IconStyle>\n");
                writer.append("    <color>" + kmlIconColour + "</color>\n");
                writer.append("    <scale>0.8</scale>\n");
                writer.append("    <Icon>\n");
                writer.append("      <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n");
                writer.append("    </Icon>\n");
                writer.append("  </IconStyle>\n");
                writer.append("</Style>\n");
            }
            writer.append("<Folder>\n");
            writer.append("<name>" + animal.getAnimalName() + "</name>\n");
            for (PositionFix positionFix : positionFixList) {
                if (!positionFix.getAnimal().equals(animal)) {
                    continue;
                }
                writer.append("<Placemark>\n");
                writer.append("  <styleUrl>#animal-" + animal.getId() + "</styleUrl>\n");
                writer.append("  <description>" + dateTimeFormat.format(positionFix.getDetectionTime()) + "</description>\n");
                writer.append("  <Point>\n");
                writer.append("    <coordinates>" + positionFix.getLocationGeometry().getX() + "," + positionFix.getLocationGeometry().getY() + "</coordinates>\n");
                writer.append("  </Point>\n");
                writer.append("  <TimeStamp>\n");
                writer.append("    <when>" + isoDateTimeFormat.format(positionFix.getDetectionTime()) + "</when>\n");
                writer.append("  </TimeStamp>\n");
                writer.append("</Placemark>\n");
            }
            writer.append("</Folder>\n");
        }
        writer.append("</Document>\n");
        writer.append("</kml>");
    }
}
