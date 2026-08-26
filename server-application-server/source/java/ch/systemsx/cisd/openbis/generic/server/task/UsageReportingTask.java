/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.generic.server.task;

import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.mail.EMailAddress;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.common.maintenance.INextTimestampProvider;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.util.PluginUtils;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maintenance task which report usage of openBIS by users.
 * 
 * @author Franz-Josef Elmer
 */
public class UsageReportingTask extends AbstractGroupMaintenanceTask
{
    public static interface IUsageInfoHandler
    {
        public void handleUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo, boolean groupAction);

        public void handleGroupUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo);
    }

    enum UserReportingType
    {
        NONE(), OUTSIDE_GROUP_ONLY()
        {
            @Override
            void handleUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo, boolean groupAction)
            {
                if (groupAction == false)
                {
                    groupInfo.handle(user, usageInfo);
                }
            }
        },
        ALL()
        {

            @Override
            void handleUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo, boolean groupAction)
            {
                groupInfo.handle(user, usageInfo);
            }
        };
        void handleUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo, boolean groupAction)
        {
        }
    }

    static final String USER_REPORTING_KEY = "user-reporting-type";

    static final String COUNT_ALL_ENTITIES_KEY = "count-all-entities";

    static final String TITLE_NAME_PREFIX = "title-name-prefix";

    static final String REPORT_NAME_PREFIX = "report-name-prefix";

    static final String PATH_TO_SAVE_REPORT = "path-to-save-report";

    static final String DELIM = "\t";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String TIME_STAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private PeriodType periodType;

    private List<EMailAddress> eMailAddresses;

    private UserReportingType userReportingType;

    private boolean countAllEntities;

    private Set<String> spacesToBeIgnored;

    private String titleNamePrefix;

    private String reportNamePrefix;

    private String pathToSaveReport;

    public UsageReportingTask()
    {
        super(false);
    }

    private List<String> logFolderNames = List.of("targets", "logs");
    private List<String> statisticsLogNames = List.of("openbis_statistics.log");
    List<String> statisticsLogApiColumns = List.of(
            "space access", "project access", "experiment access", "sample access", "data sets access");
    Map<String, String> statisticsLogApiFunctionsToColumns = Map.of(
            "get-spaces", "space access",
            "get-projects", "project access",
            "get-experiments", "experiment access",
            "get-samples", "sample access",
            "get-data-sets", "data sets access",
            "search-spaces", "space access",
            "search-projects", "project access",
            "search-experiments", "experiment access",
            "search-samples", "sample access",
            "search-data-sets", "data sets access"
    );

    @Override
    protected void setUpSpecific(Properties properties)
    {
        long interval;
        INextTimestampProvider nextTimestampProvider = parameters.getNextTimestampProvider();
        if (nextTimestampProvider != null)
        {
            Date next = nextTimestampProvider.getNextTimestamp(getActualTimeStamp());
            interval = nextTimestampProvider.getNextTimestamp(next).getTime() - next.getTime();
        } else
        {
            interval = parameters.getIntervalSeconds() * 1000;
        }
        periodType = PeriodType.getBestType(interval);
        eMailAddresses = PluginUtils.getEMailAddresses(properties, ",");
        spacesToBeIgnored = new HashSet<>(PropertyUtils.getList(properties, "spaces-to-be-ignored"));
        userReportingType = UserReportingType.valueOf(properties.getProperty(USER_REPORTING_KEY, UserReportingType.ALL.name()));
        countAllEntities = PropertyUtils.getBoolean(properties, COUNT_ALL_ENTITIES_KEY, false);
        titleNamePrefix = PropertyUtils.getProperty(properties, TITLE_NAME_PREFIX);
        reportNamePrefix = PropertyUtils.getProperty(properties, REPORT_NAME_PREFIX);
        pathToSaveReport = PropertyUtils.getProperty(properties, PATH_TO_SAVE_REPORT);
    }

    @Override
    public void execute()
    {
        List<String> groups = getGroups();
        Date actualTimeStamp = getActualTimeStamp();
        Period period = periodType.getPeriod(actualTimeStamp);
        Map<String, Map<String, Long>> apiUsage = getApiUsage(period);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String fromDateString = dateFormat.format(period.getFrom());
        String untilDateString = dateFormat.format(period.getUntil());
        operationLog.info("Gather usage information for the period from " + fromDateString + " until " + untilDateString);
        UsageAndGroupsInfo usageAndGroupsInfo = gatherUsageAndGroups(groups, period);
        UsageAndGroupsInfo usageAndGroupsInfoForAllEntities = null;
        if (countAllEntities)
        {
            usageAndGroupsInfoForAllEntities = gatherUsageAndGroups(groups, new Period(new Date(0), period.getUntil()));
        }
        String report = createReport(usageAndGroupsInfo, usageAndGroupsInfoForAllEntities, period, groups, apiUsage);
        sendReport(fromDateString, untilDateString, report);
        operationLog.info("Usage report created and sent.");
    }

    public static List<String> getDateLogPostFixes(Period period) {
        LocalDate from = period.getFrom()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate until = period.getUntil()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        List<String> result = new ArrayList<>();
        result.add("");

        while (!from.isAfter(until)) {
            result.add("." + from.toString());
            from = from.plusDays(1);
        }

        return result;
    }

    private Map<String, Map<String, Long>> getApiUsage(Period period)
    {

        Map<String, Map<String, Long>> apiUsage = new HashMap<>();
        for (String logFolderName: logFolderNames) {
            for (String statisticsLog: statisticsLogNames) {
                List<String> postFixes = getDateLogPostFixes(period);
                for (String postFix: postFixes)
                {
                    try (Stream<String> lines = Files.lines(
                            Paths.get(logFolderName, statisticsLog + postFix)))
                    {
                        lines.forEach((String line) -> {
                            String[] lineParts = line.split(" ");
                            String isoYear = lineParts[0];
                            String isoTime = lineParts[1];
                            String functionTime = lineParts[2];
                            String user = lineParts[3];
                            String function = lineParts[4];
                            Map<String, Long> stringLongMap = apiUsage.get(user);
                            if (stringLongMap == null)
                            {
                                stringLongMap = new HashMap<>();
                                apiUsage.put(user, stringLongMap);
                            }
                            String column = statisticsLogApiFunctionsToColumns.get(function);
                            Long l = stringLongMap.get(column);
                            if (l == null)
                            {
                                l = 0L;
                            }
                            l++;
                            stringLongMap.put(column, l);
                        });
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return apiUsage;
    }

    protected Date getActualTimeStamp()
    {
        return new Date();
    }

    protected UsageAndGroupsInfo gatherUsageAndGroups(List<String> groups, Period period)
    {
        UsageGatherer gatherer = new UsageGatherer(CommonServiceProvider.getApplicationServerApi(), spacesToBeIgnored);
        return gatherer.gatherUsageAndGroups(period, groups);
    }

    protected IMailClient getMailClient()
    {
        return CommonServiceProvider.createEMailClient();
    }

    private List<String> getGroups()
    {
        UserManagerConfig config = readUserManagerConfig(null);
        if (config == null)
        {
            return null;
        }
        return config.getGroups().stream().map(UserGroup::getKey).collect(Collectors.toList());
    }

    private void sendReport(String fromDateString, String untilDateString, String report)
    {
        String subject = "";
        String fileName = "";

        if (titleNamePrefix != null && !titleNamePrefix.isEmpty())
        {
            subject = titleNamePrefix + " ";
        }
        if (reportNamePrefix != null && !reportNamePrefix.isEmpty())
        {
            fileName = reportNamePrefix + "_";
            fileName = fileName.toLowerCase().replace(" ", "_");
        }

        subject = subject + "Usage report for the period from " + fromDateString + " until " + untilDateString;
        fileName = fileName + "usage_report_" + fromDateString + "_" + untilDateString + ".tsv";

        IMailClient mailClient = getMailClient();
        try
        {
            for (EMailAddress eMailAddress : eMailAddresses)
            {
                mailClient.sendEmailMessageWithAttachment(subject, "The usage report can be found in the attached TSV file.",
                        fileName, new DataHandler(new ByteArrayDataSource(report, "text/plain")), null, null, eMailAddress);
            }
            backUpReport(fileName, report);
        } catch (IOException e)
        {
            notificationLog.error("Couldn't sent usage report:" + e);
            throw CheckedExceptionTunnel.wrapIfNecessary(e);
        }
    }

    private String createReport(UsageAndGroupsInfo usageAndGroupsInfo,
            UsageAndGroupsInfo usageAndGroupsInfoForAllEntitiesOrNull, Period period, List<String> groups,
            Map<String, Map<String, Long>> apiUsage)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("period start" + DELIM + "period end" + DELIM + "group name" + DELIM + "number of users" + DELIM
                + "idle users" + DELIM + "number of new collections" + DELIM + "number of new objects" + DELIM
                + "number of new data sets");
        if (usageAndGroupsInfoForAllEntitiesOrNull != null)
        {
            builder.append(DELIM).append("total number of entities");
        }

        for (String distinctApiColumns:statisticsLogApiColumns) {
            builder.append(DELIM).append(distinctApiColumns);
        }
        builder.append("\n");
        Map<String, GroupInfo> groupInfos = initializeGroupInfos(usageAndGroupsInfo, apiUsage);
        Map<String, GroupInfo> individualInfos = initializeUserInfos(usageAndGroupsInfo, apiUsage);

        handleUsageAndGroupInfos(usageAndGroupsInfo, groupInfos, individualInfos, new IUsageInfoHandler()
            {
                @Override
                public void handleUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo, boolean groupAction)
                {
                    userReportingType.handleUsageInfo(individualInfos.get(user), user, usageInfo, groupAction);
                }

                @Override
                public void handleGroupUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo)
                {
                    groupInfo.handle(user, usageInfo);
                }
            });
        if (usageAndGroupsInfoForAllEntitiesOrNull != null)
        {
            handleUsageAndGroupInfos(usageAndGroupsInfoForAllEntitiesOrNull, groupInfos, individualInfos, new IUsageInfoHandler()
                {
                    @Override
                    public void handleUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo, boolean groupAction)
                    {
                        groupInfo.countEntities(usageInfo);
                    }

                    @Override
                    public void handleGroupUsageInfo(GroupInfo groupInfo, String user, UsageInfo usageInfo)
                    {
                        groupInfo.countEntities(usageInfo);
                    }
                });
        }

        addInfos(builder, period, groupInfos, true);

        addInfos(builder, period, individualInfos, false);
        return builder.toString();
    }

    private void backUpReport(String fileName, String content)
    {
        if (pathToSaveReport == null || pathToSaveReport.isEmpty())
        {
            return;
        }
        File reportFile = new File(pathToSaveReport + File.separator + fileName);
        reportFile.getParentFile().mkdirs();
        FileUtilities.writeToFile(reportFile, content);
        operationLog.info("Backup usage report path: " + reportFile.getAbsolutePath());
    }

    private void handleUsageAndGroupInfos(UsageAndGroupsInfo usageAndGroupsInfo, Map<String, GroupInfo> groupInfos,
            Map<String, GroupInfo> individualInfos, IUsageInfoHandler handler)
    {
        Map<String, Set<String>> usersByGroups = usageAndGroupsInfo.getUsersByGroups();
        for (Entry<String, Map<String, UsageInfo>> entry : usageAndGroupsInfo.getUsageByUsersAndSpaces().entrySet())
        {
            String user = entry.getKey();
            for (Entry<String, UsageInfo> entry2 : entry.getValue().entrySet())
            {
                String space = entry2.getKey();
                UsageInfo usageInfo = entry2.getValue();
                handler.handleGroupUsageInfo(groupInfos.get(""), user, usageInfo);
                String[] spaceParts = space.split("_");
                boolean groupAction = false;
                if (spaceParts.length > 1)
                {
                    String group = spaceParts[0];
                    GroupInfo groupInfo = groupInfos.get(group);
                    Set<String> groupUsers = usersByGroups.get(group);
                    if (groupInfo != null && groupUsers != null && groupUsers.contains(user))
                    {
                        handler.handleGroupUsageInfo(groupInfo, user, usageInfo);
                        groupAction = true;
                    }
                }
                handler.handleUsageInfo(individualInfos.get(user), user, usageInfo, groupAction);
            }
        }
    }
    private Map<String, GroupInfo> initializeUserInfos(UsageAndGroupsInfo usageAndGroupsInfo, Map<String, Map<String, Long>> apiUsage)
    {
        Map<String, GroupInfo> individualInfos = new HashMap<>();
        for (String user : usageAndGroupsInfo.getUsageByUsersAndSpaces().keySet())
        {
            GroupInfo groupInfo = new GroupInfo(Arrays.asList(user));
            Map<String, Long> userApiUsage = apiUsage.get(user);
            if (userApiUsage == null) {
                userApiUsage = new HashMap<>();
                apiUsage.put(user, userApiUsage);
            }
            groupInfo.setApiUsage(userApiUsage);
            individualInfos.put(user, groupInfo);
        }

        return individualInfos;
    }

    private Map<String, GroupInfo> initializeGroupInfos(UsageAndGroupsInfo usageAndGroupsInfo, Map<String, Map<String, Long>> apiUsage)
    {
        Map<String, GroupInfo> groupInfos = new TreeMap<>();
        for (Entry<String, Set<String>> entry : usageAndGroupsInfo.getUsersByGroups().entrySet())
        {
            groupInfos.put(entry.getKey(), new GroupInfo(entry.getValue()));
        }
        for (String groupKey:groupInfos.keySet()) {
            GroupInfo groupUsageStatistics = groupInfos.get(groupKey);
            // Group Api Usage Initialization
            Map<String, Long> groupApiUsage = groupUsageStatistics.getApiUsage();
            if (groupApiUsage == null) {
                groupApiUsage = new HashMap<>();
                groupUsageStatistics.setApiUsage(groupApiUsage);
            }
            for (String groupUser:usageAndGroupsInfo.getUsersByGroups().get(groupKey)) {
                // Group Api Usage Update
                Map<String, Long> userApiUsage = apiUsage.get(groupUser);
                if (userApiUsage != null) {
                    for (String column:userApiUsage.keySet()) {
                        Long groupUsageCount = groupApiUsage.get(column);
                        if (groupUsageCount == null)
                        {
                            groupUsageCount = 0L;
                        }
                        Long userUsageCount = userApiUsage.get(column);
                        if (userUsageCount == null)
                        {
                            userUsageCount = 0L;
                        }
                        groupUsageCount += userUsageCount;
                        groupApiUsage.put(column, groupUsageCount);
                    }
                }
            }
        }
        groupInfos.put("", new GroupInfo(usageAndGroupsInfo.getUsageByUsersAndSpaces().keySet()));
        return groupInfos;
    }

    private void addInfos(StringBuilder builder, Period period, Map<String, GroupInfo> infos, boolean showIdle)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_STAMP_FORMAT);
        String fromDate = dateFormat.format(period.getFrom());
        String untilDate = dateFormat.format(period.getUntil());
        Set<Entry<String, GroupInfo>> entrySet = infos.entrySet();
        for (Entry<String, GroupInfo> entry : entrySet)
        {
            GroupInfo info = entry.getValue();
            Set<String> idleUsers = info.getIdleUsers();
            int numberOfUsers = info.getNumberOfUsers();
            if (showIdle || idleUsers.size() < numberOfUsers)
            {
                builder.append(fromDate).append(DELIM).append(untilDate).append(DELIM).append(entry.getKey()).append(DELIM);
                builder.append(numberOfUsers).append(DELIM);
                builder.append(StringUtils.join(idleUsers, ' ')).append(DELIM);
                builder.append(info.getNumberOfNewExperiments()).append(DELIM);
                builder.append(info.getNumberOfNewSamples()).append(DELIM);
                builder.append(info.getNumberOfNewDataSets());
                if (countAllEntities)
                {
                    builder.append(DELIM).append(info.getNumberOfEntities());
                }
                for (String column:statisticsLogApiColumns)
                {
                    if (info.getApiUsage() != null)
                    {
                        builder.append(DELIM).append(info.getApiUsage().get(column));
                    } else {
                        builder.append(DELIM);
                    }
                }
                builder.append("\n");
            }
        }
    }

    private static final class GroupInfo
    {
        private Set<String> allUsers = new HashSet<>();

        private Set<String> activeUsers = new HashSet<>();

        private int numberOfNewExperiments;

        private int numberOfNewSamples;

        private int numberOfNewDataSets;

        private int numberOfEntities;

        private Map<String, Long> apiUsage;

        public GroupInfo(Collection<String> users)
        {
            allUsers.addAll(users);
        }

        void handle(String user, UsageInfo usageInfo)
        {
            if (usageInfo.isIdle() == false)
            {
                activeUsers.add(user);
            }
            numberOfNewExperiments += usageInfo.getNumberOfNewExperiments();
            numberOfNewSamples += usageInfo.getNumberOfNewSamples();
            numberOfNewDataSets += usageInfo.getNumberOfNewDataSets();
        }

        void countEntities(UsageInfo usageInfo)
        {
            numberOfEntities += usageInfo.getNumberOfNewDataSets();
            numberOfEntities += usageInfo.getNumberOfNewExperiments();
            numberOfEntities += usageInfo.getNumberOfNewSamples();
        }

        int getNumberOfUsers()
        {
            return allUsers.size();
        }

        Set<String> getIdleUsers()
        {
            Set<String> idleUsers = new TreeSet<>(allUsers);
            idleUsers.removeAll(activeUsers);
            return idleUsers;
        }

        int getNumberOfNewExperiments()
        {
            return numberOfNewExperiments;
        }

        int getNumberOfNewSamples()
        {
            return numberOfNewSamples;
        }

        int getNumberOfNewDataSets()
        {
            return numberOfNewDataSets;
        }

        int getNumberOfEntities()
        {
            return numberOfEntities;
        }

        public Map<String, Long> getApiUsage()
        {
            return apiUsage;
        }

        public void setApiUsage(Map<String, Long> apiUsage)
        {
            this.apiUsage = apiUsage;
        }
    }
}
