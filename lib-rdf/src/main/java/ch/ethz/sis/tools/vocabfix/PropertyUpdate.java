package ch.ethz.sis.tools.vocabfix;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class PropertyUpdate
{
    private static final String helperCommand =
            "java -jar stuff";

    public static void main(String[] args) throws SQLException
    {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        Options options = createOptions();
        CommandLine cmd = null;
        try
        {
            cmd = parser.parse(options, args);
        } catch (ParseException ex)
        {
            formatter.printHelp(helperCommand, options);
            ex.printStackTrace();
        }

        Connection connection = null; // Better to get this from a pool.

        String dbPath = "jdbc:sqlite:" + cmd.getOptionValue('d');
        System.out.println(dbPath);
        connection = DriverManager.getConnection(
                dbPath);
        Statement statement =
                connection.createStatement(); // https://stackoverflow.com/questions/4905579/read-sqlite-db-file-using-java
        String url = cmd.getOptionValue('u');
        OpenBIS openBIS = new OpenBIS(url, Integer.MAX_VALUE);
        String token = cmd.getOptionValue('t');
        openBIS.setSessionToken(token);

        IApplicationServerApi v3 =
                HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
                        url + "/openbis/openbis"
                                + IApplicationServerApi.SERVICE_URL, Integer.MAX_VALUE);

        List<UpdateData> updateData = new ArrayList<>();

        ResultSet rs = statement.executeQuery("select * from vocab_fix WHERE imported IS NULL;");
        while (rs.next())
        {

            String vocab_fix_id = rs.getString("vocab_fix_id");
            String objectName = rs.getString("object_name");
            String propertyCode = rs.getString("property_code");
            String value = rs.getString("value");
            updateData.add(new UpdateData(objectName, propertyCode, value, vocab_fix_id));

        }

        System.out.println("Found " + updateData.size() + " statements");
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();

        int CHUNK_SIZE =
                Optional.ofNullable(cmd.getOptionValue('n')).map(Integer::parseInt).orElse(1000);
        int processed = 0;
        List<List<UpdateData>> chunks = nPartition(updateData, CHUNK_SIZE);
        for (List<UpdateData> chunk : chunks)
        {
            SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
            sampleSearchCriteria.withOrOperator();
            sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();

            Set<String> objectNames = new HashSet<>();
            List<String> codes = new ArrayList<>();
            for (UpdateData val : chunk)
            {
                String code = makeOpenBisCodeCompliant(val.objectName)
                        .toUpperCase(Locale.ROOT);
                objectNames.add(code);
                codes.add(code);
            }
            sampleSearchCriteria.withCodes().thatIn(codes);
            SearchResult<Sample> sampleSearchResult =
                    v3.searchSamples(token, sampleSearchCriteria, sampleFetchOptions);

            Set<String> foundNames =
                    sampleSearchResult.getObjects().stream().map(x -> x.getCode())
                            .map(Object::toString).collect(
                                    Collectors.toSet());
            Set<String> difference1 = new HashSet<>(objectNames);
            difference1.removeAll(foundNames);
            if (!difference1.isEmpty())
            {
                throw new RuntimeException("Objects not found: " + difference1.stream().collect(
                        Collectors.joining(", ")));

            }

            Map<String, Sample> nameToObject = sampleSearchResult.getObjects().stream().collect(
                    Collectors.toMap(x -> x.getCode(), x -> x, (s1, s2) -> {
                        return s1;
                    }));
            List<SampleUpdate> updates = new ArrayList<>();
            Map<Pair<Sample, PropertyType>, Set<String>> updatesPerObjectAndProperty =
                    new LinkedHashMap<>();


            for (UpdateData val : chunk)
            {
                Sample sample = nameToObject.get(makeOpenBisCodeCompliant(val.objectName()));

                Optional<PropertyType> maybePropertyType =
                        sample.getType().getPropertyAssignments().stream()
                                .map(x -> x.getPropertyType())
                                .filter(x -> x.getCode().equals(val.propertyCode))
                                .findFirst();

                Set<String> propertyValues = updatesPerObjectAndProperty.getOrDefault(
                        new ImmutablePair<>(sample, maybePropertyType.orElseThrow()),
                        new LinkedHashSet<>());
                Serializable existingValues =
                        sample.getProperties().get(maybePropertyType.get().getCode());
                if (existingValues != null)
                {
                    if (existingValues instanceof String[] array)
                    {
                        propertyValues.addAll(Arrays.asList(array));
                    } else if (existingValues instanceof String)
                    {
                        propertyValues.add(existingValues.toString());

                    }
                }

                propertyValues.add(val.value);
                updatesPerObjectAndProperty.put(
                        new ImmutablePair<>(sample, maybePropertyType.get()), propertyValues);

            }

            for (Map.Entry<Pair<Sample, PropertyType>, Set<String>> keyVal : updatesPerObjectAndProperty.entrySet())
            {
                SampleUpdate sampleUpdate = new SampleUpdate();
                sampleUpdate.setSampleId(keyVal.getKey().getKey().getPermId());
                Set<String> value = keyVal.getValue();

                if (value.size() > 1 && !keyVal.getKey().getRight().isMultiValue())
                {

                    value = value.stream().sorted(Comparator.comparing(x -> -x.length()))
                            .findFirst().map(x -> {
                                HashSet<String> set = new HashSet();
                                set.add(x);
                                return set;

                            }).orElseThrow();
                    System.out.println(value);
                    System.out.println(
                            "Single-valued property " + keyVal.getKey().getRight()
                                    .getCode() + " has multiple values for object " + keyVal.getKey()
                                    .getLeft().getCode());
                }

                Serializable serialValue =
                        keyVal.getKey().getValue().isMultiValue() ?
                                value.toArray(String[]::new) :
                                value.stream().findFirst().orElseThrow();

                sampleUpdate.setProperty(keyVal.getKey().getRight().getCode(), serialValue);
                updates.add(sampleUpdate);
            }
            connection.createStatement()
                    .execute(getTimeStampUpdates(chunk.stream().map(UpdateData::rowId).collect(
                            Collectors.toList())));

            v3.updateSamples(token, updates);

            processed += CHUNK_SIZE;
            System.out.println(
                    new Date().toString() + ": Processed " + processed + " of " + updateData.size());


        }

        rs.close();
        connection.close();

    }

    static String getTimeStampUpdates(List<String> rowIds)
    {
        String ids = rowIds.stream().collect(Collectors.joining(","));
        return "UPDATE vocab_fix SET imported=current_timestamp WHERE vocab_fix_id IN (" + ids + ")";

    }

    record UpdateData(String objectName, String propertyCode, String value, String rowId)
    {
    }

    private static Options createOptions()
    {
        Options options = new Options();

        Option url = new Option("u", "URL of openBIS instance", true, "openBIS instance token");
        url.setRequired(true);
        options.addOption(url);

        Option token = new Option("t", "token", true, "openBIS instance token");
        token.setRequired(true);
        options.addOption(token);

        Option database = new Option("d", "database", true, "Database where the fixes are");
        database.setRequired(true);
        options.addOption(database);

        Option chunkSize = new Option("n", "chunksize", true, "Chunk size of updates");
        chunkSize.setRequired(false);
        options.addOption(chunkSize);

        return options;
    }

    public static final String CODE_SPECIAL_CHARACTER_REPLACEMENT = "_";

    public static String makeOpenBisCodeCompliant(String candiate)
    {
        return candiate.replaceAll("\\|", CODE_SPECIAL_CHARACTER_REPLACEMENT)
                .replaceAll("%[0-9A-Fa-f]{2}", CODE_SPECIAL_CHARACTER_REPLACEMENT)
                .replaceAll("\\\\u([0-9A-Fa-f]{2}){6}", CODE_SPECIAL_CHARACTER_REPLACEMENT)
                .replaceAll("\\\\u([0-9A-Fa-f]{2}){3}", CODE_SPECIAL_CHARACTER_REPLACEMENT)
                .toUpperCase(Locale.ROOT);
    }

    private static <T> List<List<T>> nPartition(List<T> objs, final int N)
    {
        return new ArrayList<>(IntStream.range(0, objs.size()).boxed().collect(
                Collectors.groupingBy(e -> e / N,
                        Collectors.mapping(e -> objs.get(e), Collectors.toList())
                )).values());
    }



}
