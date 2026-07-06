package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
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

public class SampleHelper
{
    public enum Attribute { // implements IAttribute {
        $("$", false),
        Identifier("Identifier", false),
        Code("Code", false),
        Space("Space", false),
        Project("Project", false),
        Experiment("Experiment", false),
        Parents("Parents", false),
        Children("Children", false),
        Name("Name", false);

        private final String headerName;

        private final boolean mandatory;

        Attribute(String headerName, boolean mandatory) {
            this.headerName = headerName;
            this.mandatory = mandatory;
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

    public static List<String> getAllColumnsList(List<String> sampleObjectPropertyLabelList)
    {
        List<String> defaultCols = new ArrayList<>(Stream.of(SampleHelper.Attribute.values())
                .map(SampleHelper.Attribute::name)
                .collect(Collectors.toList()));
        defaultCols.addAll(sampleObjectPropertyLabelList);
        return defaultCols;
    }

    public static PropertyTypeIndexInfo getPropertyTypeIndexInfo(SampleType sampleType)
    {
        List<String> typeLabels =
                sampleType.getPropertyAssignments().stream().map(x -> x.getPropertyType())
                        .map(x -> x.getLabel())
                        .toList();
        List<String> defaultCols = Stream.of(Attribute.values())
                .map(Attribute::name)
                .collect(Collectors.toList());
        List<String> labels = new ArrayList<>(defaultCols);
        labels.addAll(typeLabels);

        List<String> codes =
                sampleType.getPropertyAssignments().stream().map(x -> x.getPropertyType())
                        .map(x -> x.getCode())
                        .toList();
        return new PropertyTypeIndexInfo(labels, codes, defaultCols);

    }

    public int createSampleHeaders(Sheet sheet, int rowNum, CellStyle headerStyle,
            String sampleTypeKey, PropertyTypeIndexInfo propertyTypeIndexInfo)
    {

        // Create header row for SAMPLE
        Row headerSampleRow = sheet.createRow(rowNum++);
        Cell cellSample = headerSampleRow.createCell(0);
        cellSample.setCellValue("SAMPLE");
        cellSample.setCellStyle(headerStyle);

        // Create header row for Sample Type
        Row headerSampleTypeRow = sheet.createRow(rowNum++);
        Cell cellSampleType = headerSampleTypeRow.createCell(0);
        cellSampleType.setCellValue("Sample type");
        cellSampleType.setCellStyle(headerStyle);

        // Add Sample Type Value
        Row sampleTypeRow = sheet.createRow(rowNum++);
        sampleTypeRow.createCell(0).setCellValue(sampleTypeKey.toUpperCase(Locale.ROOT));

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

    public RowWriteResult createResourceRows(Sheet sheet, int rowNum, Sample sampleObject,
            OpenBisModel openBisModel, PropertyTypeIndexInfo propertyTypeIndexInfo)
    {

        String projectId =
                Optional.ofNullable(sampleObject.getProject()).map(x -> x.getIdentifier())
                        .map(x -> x.getIdentifier()).orElse(null);
        Row propertyRowValues = sheet.createRow(rowNum);
        //propertyRowValues.createCell(0).setCellValue(""); // $
        propertyRowValues.createCell(1)
                .setCellValue(sampleObject.getIdentifier().getIdentifier()); // Identifier
        propertyRowValues.createCell(2).setCellValue(sampleObject.getCode()); // Code
        propertyRowValues.createCell(3).setCellValue(
                Optional.ofNullable(sampleObject.getSpace()).map(x -> x.getCode())
                        .orElse(null)); // Space
        propertyRowValues.createCell(4).setCellValue(projectId); // Project
        propertyRowValues.createCell(5).setCellValue(
                Optional.ofNullable(sampleObject.getExperiment()).map(x -> x.getIdentifier())
                        .map(x -> x.toString()).orElse(null)); // Experiment
        //propertyRowValues.createCell(6).setCellValue(""); // Parents
        //propertyRowValues.createCell(7).setCellValue(""); // Children

        int idxName = propertyTypeIndexInfo.findIndex("Name");
        if (idxName != -1)
        {
            String val = Optional.ofNullable(sampleObject.getProperties().get("NAME"))
                    .map(Object::toString)
                    .orElse("");
            propertyRowValues.createCell(idxName).setCellValue(val);
        }

        List<String> vocabularyOptionList = openBisModel.getVocabularyTypes().values().stream()
                .flatMap(vocabularyType -> vocabularyType.getTerms().stream())
                .map(VocabularyTerm::getDescription)
                .collect(Collectors.toList());

        sampleObject.getType().getPropertyAssignments();
        List<RowWriteResult.LongCell> longCells = new ArrayList<>();

        for (Map.Entry<String, Serializable> property : sampleObject.getProperties().entrySet())
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
