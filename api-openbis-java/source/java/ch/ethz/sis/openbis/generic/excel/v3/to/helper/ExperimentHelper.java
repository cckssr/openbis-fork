package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.helper.longvals.CellWriter;
import ch.ethz.sis.openbis.generic.excel.v3.to.helper.longvals.RowWriteResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.ethz.sis.openbis.generic.excel.v3.to.Constants.COLLECTION_TYPE;

public class ExperimentHelper
{
    private static final String EXPERIMENT_TYPE_FIELD = "Experiment type";
    private static final String EXPERIMENT = "EXPERIMENT";

    private enum Attribute { //implements IAttribute {
        Identifier("Identifier", false),
        Code("Code", true),
        Project("Project", true),
        Name("Name", true);

        private final String headerName;

        private final boolean mandatory;

        Attribute(String headerName, boolean mandatory) {
            this.headerName = headerName;
            this.mandatory = mandatory;
        }

        public String getHeaderName() {
            return headerName;
        }

    }

    public int addExperimentSection(Sheet sheet, int rowNum, CellStyle headerStyle,
            Experiment experiment)
    {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(EXPERIMENT);
        row.getCell(0).setCellStyle(headerStyle);

        Row rowType = sheet.createRow(rowNum++);
        rowType.createCell(0).setCellValue(EXPERIMENT_TYPE_FIELD);
        rowType.getCell(0).setCellStyle(headerStyle);

        Row rowTypeValues = sheet.createRow(rowNum++);

        rowTypeValues.createCell(0).setCellValue(COLLECTION_TYPE);

        Row rowHeaders = sheet.createRow(rowNum++);

        // Populate header row with enum values
        Attribute[] fields = Attribute.values();
        for (int i = 0; i < fields.length; i++)
        {
            Cell cell = rowHeaders.createCell(i);
            cell.setCellValue(fields[i].getHeaderName());
            cell.setCellStyle(headerStyle);
        }

        autosizeColumns(sheet, fields.length);

        // add empty row
        sheet.createRow(rowNum++);

        return rowNum;

    }

    public static String extractLabel(String uri)
    {
        int hashIndex = uri.indexOf('#');
        if (hashIndex != -1)
        {
            return uri.substring(hashIndex + 1);
        } else
        {
            return uri;
        }
    }

    public static void autosizeColumns(Sheet sheet, int numCols)
    {
        for (int i = 0; i < numCols; i++)
        {
            sheet.autoSizeColumn(i);
        }
    }

    public record PropertyTypeIndexInfo(List<String> labels, List<String> codes,
                                        List<String> defaultCols)
    {
        int findIndex(String query)
        {
            int idx = labels.indexOf(query);

            if (idx >= 0)
            {
                return idx;
            }
            return defaultCols.size() + codes.indexOf(query);
        }

    }

    public static ExperimentHelper.PropertyTypeIndexInfo getPropertyTypeIndexInfo(
            ExperimentType experimentType)
    {
        List<String> typeLabels =
                experimentType.getPropertyAssignments().stream().map(
                                PropertyAssignment::getPropertyType)
                        .map(PropertyType::getLabel)
                        .toList();
        List<String> defaultCols = Stream.of(ExperimentHelper.Attribute.values())
                .map(ExperimentHelper.Attribute::name)
                .collect(Collectors.toList());
        List<String> labels = new ArrayList<>(defaultCols);
        labels.addAll(typeLabels);

        List<String> codes =
                experimentType.getPropertyAssignments().stream().map(
                                PropertyAssignment::getPropertyType)
                        .map(PropertyType::getCode)
                        .toList();
        return new ExperimentHelper.PropertyTypeIndexInfo(labels, codes, defaultCols);

    }

    public int createExperimentHeaders(Sheet sheet, int rowNum, CellStyle headerStyle,
            String experimentTypeKey, ExperimentHelper.PropertyTypeIndexInfo propertyTypeIndexInfo)
    {

        // Create header row for SAMPLE
        Row headerSampleRow = sheet.createRow(rowNum++);
        Cell cellSample = headerSampleRow.createCell(0);
        cellSample.setCellValue("EXPERIMENT");
        cellSample.setCellStyle(headerStyle);

        // Create header row for Sample Type
        Row headerExperimentType = sheet.createRow(rowNum++);
        Cell cellExperimentType = headerExperimentType.createCell(0);
        cellExperimentType.setCellValue("Experiment type");
        cellExperimentType.setCellStyle(headerStyle);

        // Add Sample Type Value
        Row sampleTypeRow = sheet.createRow(rowNum++);
        sampleTypeRow.createCell(0).setCellValue(experimentTypeKey.toUpperCase(Locale.ROOT));

        // Create header row for Sample Type columns
        Row sampleTypeRowHeaders = sheet.createRow(rowNum++);

        for (int i = 0; i < propertyTypeIndexInfo.labels.size(); i++)
        {
            Cell cell = sampleTypeRowHeaders.createCell(i);
            cell.setCellValue(propertyTypeIndexInfo.labels.get(i));
            cell.setCellStyle(headerStyle);
        }

        return rowNum;
    }

    public RowWriteResult createResourceRows(Sheet sheet, int rowNum, Experiment experiment,
            OpenBisModel openBisModel, ExperimentHelper.PropertyTypeIndexInfo propertyTypeIndexInfo)
    {

        String projectId =
                Optional.ofNullable(experiment.getProject()).map(Project::getIdentifier)
                        .map(ObjectIdentifier::getIdentifier).orElse(null);
        Row propertyRowValues = sheet.createRow(rowNum);
        //propertyRowValues.createCell(0).setCellValue(""); // $
        propertyRowValues.createCell(0)
                .setCellValue(experiment.getIdentifier().getIdentifier()); // Identifier
        propertyRowValues.createCell(1).setCellValue(experiment.getCode()); // Code
        propertyRowValues.createCell(2).setCellValue(
                Optional.ofNullable(experiment.getProject()).map(Project::getIdentifier).map(
                                ProjectIdentifier::getIdentifier)
                        .orElse(null)); // Space
        propertyRowValues.createCell(3).setCellValue(projectId); // Project
        int idxName = propertyTypeIndexInfo.findIndex("Name");
        if (idxName != -1)
        {
            String val = Optional.ofNullable(experiment.getProperties().get("NAME"))
                    .map(Object::toString)
                    .orElse("");
            propertyRowValues.createCell(idxName).setCellValue(val);
        }

        experiment.getType().getPropertyAssignments();
        List<RowWriteResult.LongCell> longCells = new ArrayList<>();

        for (Map.Entry<String, Serializable> property : experiment.getProperties().entrySet())
        {

            //propertyRowValues.createCell(1).setCellValue(projectId + "/" + sampleObject.code); // Identifier
            //propertyRowValues.createCell(5).setCellValue(projectId + "/" + sampleObject.type.toUpperCase(Locale.ROOT) + "_COLLECTION"); // Experiment
            int idx = propertyTypeIndexInfo.findIndex(property.getKey());
            if (idx != -1)
            {
                String val = property.getValue().toString();
                if (property.getValue() instanceof Serializable[] || property.getValue() instanceof String[])
                {
                    Serializable[] array = (Serializable[]) property.getValue();
                    val = Arrays.stream(array).map(x -> x.toString())
                            .collect(Collectors.joining(","));

                }

                CellWriter.writeCell(propertyRowValues.createCell(idx),
                        val).ifPresent(longCells::add);
            }

        }
        return new RowWriteResult(rowNum, longCells);  // Move to the next row for future entries
    }

}
