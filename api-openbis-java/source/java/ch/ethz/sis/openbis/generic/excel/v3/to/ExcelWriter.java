package ch.ethz.sis.openbis.generic.excel.v3.to;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.helper.*;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ExcelWriter
{
    SampleTypeHelper sampleTypeHelper;

    PropertyTypeHelper propertyTypeHelper;

    SampleHelper sampleHelper;

    SpaceHelper spaceHelper;

    ProjectHelper projectHelper;

    ExperimentHelper experimentHelper;

    ExperimentTypeHelper rdfExperimentTypeHelper;

    VocabularyTypeHelper vocabularyTypeHelper;

    public static enum Format { ZIP_EXPORT, EXCEL }

    public static byte[] convert(Format outputFormat, OpenBisModel model) {
        if (outputFormat != Format.EXCEL) {
            throw new IllegalArgumentException("Argument with name outputFormat and value: " + outputFormat +  " not supported.");
        }
        ExcelWriter ExcelWriter = new ExcelWriter();
        return ExcelWriter.write(model);
    }

    private ExcelWriter()
    {
        this.sampleTypeHelper = new SampleTypeHelper();
        this.propertyTypeHelper = new PropertyTypeHelper();
        this.sampleHelper = new SampleHelper();
        this.spaceHelper = new SpaceHelper();
        this.projectHelper = new ProjectHelper();
        this.experimentHelper = new ExperimentHelper();
        this.rdfExperimentTypeHelper = new ExperimentTypeHelper();
        this.vocabularyTypeHelper = new VocabularyTypeHelper();
    }

    //TODO Remove projectIdentifier from write method
    private byte[] write(OpenBisModel openBisModel)
    {
        try (Workbook workbook = new XSSFWorkbook())
        {  // Create a new workbook
            // Define a style for headers
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            if (!openBisModel.getVocabularyTypes().isEmpty())
            {
                createVocabularyTypesSheet(workbook, headerStyle, openBisModel);
            }
            createObjectTypesSheet(workbook, headerStyle, openBisModel);
            createExperimentTypesSheet(workbook, headerStyle);
            createSpaceProjExpSheet(workbook, headerStyle, openBisModel);
            if (!openBisModel.getSampleTypes().isEmpty())
                createObjectsSheet(workbook, headerStyle, openBisModel);

            // Write the output to a file
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
            {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            } catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void createVocabularyTypesSheet(Workbook workbook, CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        Sheet sheet = workbook.createSheet(Constants.SHEET_TITLE_VOCAB);
        int rowNum = 0;

        Collection<Vocabulary> vocabularyTypeList =
                openBisModel.getVocabularyTypes().values();

        for (Vocabulary vocabularyType : vocabularyTypeList)
        {
            rowNum = vocabularyTypeHelper.addVocabularyTypes(sheet, rowNum, headerStyle,
                    vocabularyType);
        }

    }

    private void createObjectTypesSheet(Workbook workbook, CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        Sheet sheetOT = workbook.createSheet(Constants.SHEET_TITLE_OBJ_TYPES);

        int rowNumOT = 0;

        for (SampleType sampleType : openBisModel.getSampleTypes())
        {
            // Add SAMPLE_TYPE header row for ClassDetails
            rowNumOT = sampleTypeHelper.addSampleTypeSection(sheetOT, rowNumOT, headerStyle,
                    sampleType);

            // Add object properties section
            rowNumOT = propertyTypeHelper.addObjectProperties(sheetOT, rowNumOT, headerStyle,
                    sampleType);
        }
    }

    private void createExperimentTypesSheet(Workbook workbook, CellStyle headerStyle)
    {
        Sheet sheet =
                workbook.createSheet(Constants.SHEET_TITLE_EXP);  // Create a sheet named "OBJ PROP"
        int rowNum = 0;

        rowNum = rdfExperimentTypeHelper.addExperimentTypeSection(sheet, rowNum, headerStyle);
        rdfExperimentTypeHelper.addExperimentSection(sheet, rowNum, headerStyle);

    }

    private void createSpaceProjExpSheet(Workbook workbook, CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        Sheet sheet = workbook.createSheet(
                Constants.SHEET_TITLE_SPACES);  // Create a sheet named "OBJ PROP"
        int rowNum = 0;

        rowNum = spaceHelper.addSpaceSection(sheet, rowNum, headerStyle, openBisModel);
        rowNum = projectHelper.addProjectSection(sheet, rowNum, headerStyle, openBisModel);
        experimentHelper.addExperimentSection(sheet, rowNum, headerStyle,
                openBisModel);

    }

    private void createObjectsSheet(Workbook workbook, CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        Sheet sheet = workbook.createSheet(Constants.SHEET_TITLE_OBJS);
        int rowNum = 0;

        for (Map.Entry<EntityTypePermId, List<Sample>> entry : openBisModel.getSamplesByType().entrySet())
        {
            List<String> sampleObjectPropertyLabelList = entry.getValue().stream()
                    .map(x -> x.getType())
                    .distinct()
                    .map(x -> x.getPropertyAssignments())
                    .flatMap(Collection::stream)
                    .map(x -> x.getPropertyType().getLabel())
                    .collect(Collectors.toSet()).stream()
                    .collect(Collectors.toList());

            List<String> allColumnList =
                    SampleHelper.getAllColumnsList(sampleObjectPropertyLabelList);

            rowNum = sampleHelper.createSampleHeaders(sheet, rowNum, headerStyle,
                    entry.getKey().getPermId(),
                    allColumnList);

            for (Sample sampleObject : entry.getValue())
            {
                rowNum = sampleHelper.createResourceRows(sheet, rowNum, sampleObject,
                        openBisModel
                        , allColumnList);
            }
            sheet.createRow(rowNum++);
        }
        //Utils.autosizeColumns(sheet, 20);
    }
}
