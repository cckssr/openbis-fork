package ch.ethz.sis.openbis.generic.excel.v3.to;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.excel.v3.model.IFileInfo;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.helper.*;
import ch.ethz.sis.openbis.generic.excel.v3.to.helper.longvals.RowWriteResult;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ExcelWriter
{

    private static final int BUFFER_SIZE = 1024;

    Format format;
    private static final String XLSX_EXTENSION = "." + "xlsx";

    SampleTypeHelper sampleTypeHelper;

    PropertyTypeHelper propertyTypeHelper;

    SampleHelper sampleHelper;

    SpaceHelper spaceHelper;

    ProjectHelper projectHelper;

    ExperimentHelper experimentHelper;

    ExperimentTypeHelper rdfExperimentTypeHelper;

    VocabularyTypeHelper vocabularyTypeHelper;

    public static final String DATA_DIRECTORY = "data";


    public static enum Format { ZIP_EXPORT, EXCEL }

    public static byte[] convert(Format outputFormat, OpenBisModel model) {
        ExcelWriter ExcelWriter = new ExcelWriter();
        return ExcelWriter.write(model, outputFormat, true);
    }

    public static byte[] convert(Format outputFormat, OpenBisModel model, boolean writeSchema)
    {
        ExcelWriter ExcelWriter = new ExcelWriter();
        return ExcelWriter.write(model, outputFormat, writeSchema);
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
    private byte[] write(OpenBisModel openBisModel, Format format, boolean writeSchema)
    {
        if (openBisModel.getFiles().values().stream()
                .anyMatch(x -> !x.isEmpty()) && format == Format.EXCEL)
        {
            throw new UserFailureException(
                    "Attempting to write data to Excel sheet, this requires Zip format");
        }

        this.format = format;
        try (Workbook workbook = new XSSFWorkbook())
        {  // Create a new workbook
            // Define a style for headers
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            List<RowWriteResult> writeResults = new ArrayList<>();

            if (!openBisModel.getVocabularyTypes().isEmpty() && writeSchema)
            {
                writeResults.addAll(
                        createVocabularyTypesSheet(workbook, headerStyle, openBisModel));
            }
            if (writeSchema)
            {
            createObjectTypesSheet(workbook, headerStyle, openBisModel);
            createExperimentTypesSheet(workbook, headerStyle);
            }
            createSpaceProjExpSheet(workbook, headerStyle, openBisModel);
            if (!openBisModel.getSampleTypes().isEmpty())
            {
                writeResults.addAll(createObjectsSheet(workbook, headerStyle, openBisModel));
            }
            checkWriteResult(writeResults);

            final Map<String, String> valueFiles =
                    writeResults.stream().map(x -> x.getLongCells()).flatMap(Collection::stream)
                            .collect(Collectors.toMap(RowWriteResult.LongCell::getFileName,
                                    RowWriteResult.LongCell::getValue));

            switch (format)
            {
                case EXCEL:
                {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
                    {
                        workbook.write(outputStream);
                        return outputStream.toByteArray();
                    } catch (FileNotFoundException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                case ZIP_EXPORT:
                {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            final ZipOutputStream zos = new ZipOutputStream(outputStream);
                            final BufferedOutputStream bos = new BufferedOutputStream(zos);
                    )
                    {
                        for (Map.Entry<ObjectIdentifier, List<IFileInfo>> objectAndFileInfo : openBisModel.getFiles()
                                .entrySet())
                        {
                            for (IFileInfo file : objectAndFileInfo.getValue())
                            {
                                AbstractEntityPropertyHolder entity =
                                        openBisModel.getEntities().get(objectAndFileInfo.getKey());
                                String zipPath = DataPathHelper.getPath(file, entity);
                                ZipEntry zipEntry = new ZipEntry(zipPath);
                                zos.putNextEntry(zipEntry);

                                writeFile(file, bos);

                            }
                        }
                        for (Map.Entry<ObjectIdentifier, List<IFileInfo>> objectAndFileInfo : openBisModel.getImageFiles()
                                .entrySet())
                        {
                            for (IFileInfo file : objectAndFileInfo.getValue())
                            {
                                ZipEntry zipEntry = new ZipEntry(file.originalPath());
                                zos.putNextEntry(zipEntry);
                                writeFile(file, bos);
                            }
                        }


                        for (final Map.Entry<String, String> valueFile : valueFiles.entrySet())
                        {
                            zos.putNextEntry(new ZipEntry(
                                    String.format("%s/%s", DATA_DIRECTORY, valueFile.getKey())));
                            bos.write(valueFile.getValue().getBytes());
                            bos.flush();
                            zos.closeEntry();
                        }
                        zos.putNextEntry(new ZipEntry("metadata" + XLSX_EXTENSION));

                        workbook.write(bos);
                        return outputStream.toByteArray();
                    } catch (FileNotFoundException e)
                    {
                        throw new RuntimeException(e);
                    }

                }

            }


        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Unknown format, how did this happen?");
    }

    private static void writeFile(IFileInfo file, BufferedOutputStream bos) throws IOException
    {
        try (InputStream input = file.getInputStream()) {
            input.transferTo(bos);
        }
    }

    public void checkWriteResult(List<RowWriteResult> writeResults)
    {
        if (format == Format.EXCEL && writeResults.stream()
                .anyMatch(x -> !x.getLongCells().isEmpty()))
        {
            throw new IllegalArgumentException(
                    "Some values are too long for excel! Use a zipped format instead.");
        }
    }

    private List<RowWriteResult> createVocabularyTypesSheet(Workbook workbook,
            CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        List<RowWriteResult> res = new ArrayList<>();

        Sheet sheet = workbook.createSheet(Constants.SHEET_TITLE_VOCAB);
        int rowNum = 0;

        Collection<Vocabulary> vocabularyTypeList =
                openBisModel.getVocabularyTypes().values();

        for (Vocabulary vocabularyType : vocabularyTypeList)
        {
            List<RowWriteResult>
                    rowRes = vocabularyTypeHelper.addVocabularyTypes(sheet, rowNum, headerStyle,
                    vocabularyType);
            rowNum = rowRes.get(rowRes.size() - 1).getRowNum();
        }
        return res;

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

    private List<RowWriteResult> createObjectsSheet(Workbook workbook, CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        List<RowWriteResult> rowWriteResults = new ArrayList<>();

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
                rowWriteResults.add(sampleHelper.createResourceRows(sheet, rowNum, sampleObject,
                        openBisModel
                        , allColumnList));
                rowNum++;
                checkWriteResult(rowWriteResults);
            }
            sheet.createRow(rowNum++);
            rowWriteResults.add(new RowWriteResult(rowNum, new ArrayList<>()));
        }
        //Utils.autosizeColumns(sheet, 20);
        return rowWriteResults;
    }
}
