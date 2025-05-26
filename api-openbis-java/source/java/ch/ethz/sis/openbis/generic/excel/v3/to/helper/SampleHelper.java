package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.helper.longvals.CellWriter;
import ch.ethz.sis.openbis.generic.excel.v3.to.helper.longvals.RowWriteResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    public static List<String> getAllColumnsList(List<String> sampleObjectPropertyLabelList)
    {
        List<String> defaultCols = new ArrayList<>(Stream.of(SampleHelper.Attribute.values())
                .map(SampleHelper.Attribute::name)
                .collect(Collectors.toList()));
        defaultCols.addAll(sampleObjectPropertyLabelList);
        return defaultCols;
    }

    public int createSampleHeaders(Sheet sheet, int rowNum, CellStyle headerStyle, String sampleTypeKey, List<String> allColumnList)
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

        for (int i = 0; i < allColumnList.size(); i++)
        {
            Cell cell = sampleTypeRowHeaders.createCell(i);
            cell.setCellValue(allColumnList.get(i));
            cell.setCellStyle(headerStyle);
        }

        return rowNum;
    }

    public RowWriteResult createResourceRows(Sheet sheet, int rowNum, Sample sampleObject,
            OpenBisModel openBisModel, List<String> allColumnList)
    {

        String projectId = sampleObject.getProject().getIdentifier().getIdentifier();
        Row propertyRowValues = sheet.createRow(rowNum);
        //propertyRowValues.createCell(0).setCellValue(""); // $
        propertyRowValues.createCell(1)
                .setCellValue(sampleObject.getProject().getIdentifier()
                        .getIdentifier() + "/" + sampleObject.getCode()); // Identifier
        propertyRowValues.createCell(2).setCellValue(sampleObject.getCode()); // Code
        propertyRowValues.createCell(3).setCellValue(sampleObject.getSpace().getCode()); // Space
        propertyRowValues.createCell(4).setCellValue(projectId); // Project
        propertyRowValues.createCell(5).setCellValue(
                projectId + "/" + sampleObject.getType().getCode()
                        .toUpperCase(Locale.ROOT) + "_COLLECTION"); // Experiment
        //propertyRowValues.createCell(6).setCellValue(""); // Parents
        //propertyRowValues.createCell(7).setCellValue(""); // Children

        int idxName = allColumnList.indexOf("Name");
        if (idxName != -1)
        {
            propertyRowValues.createCell(idxName).setCellValue(sampleObject.getCode());
        }

        List<String> vocabularyOptionList = openBisModel.getVocabularyTypes().values().stream()
                .flatMap(vocabularyType -> vocabularyType.getTerms().stream())
                .map(VocabularyTerm::getDescription)
                .collect(Collectors.toList());

        sampleObject.getType().getPropertyAssignments();
        List<RowWriteResult.LongCell> longCells = new ArrayList<>();

        for (var property : sampleObject.getProperties().entrySet())
        {

            //propertyRowValues.createCell(1).setCellValue(projectId + "/" + sampleObject.code); // Identifier
            //propertyRowValues.createCell(5).setCellValue(projectId + "/" + sampleObject.type.toUpperCase(Locale.ROOT) + "_COLLECTION"); // Experiment
            int idx = allColumnList.indexOf(property.getKey());
            if (idx != -1)
            {
                CellWriter.writeCell(propertyRowValues.createCell(idx),
                        property.getValue().toString()).ifPresent(longCells::add);
            }

        }
        return new RowWriteResult(rowNum, longCells);  // Move to the next row for future entries
    }


}
