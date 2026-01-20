package ch.ethz.sis.openbis.generic.dss.systemtest.api.v3;

import ch.ethz.sis.filetransfer.*;
import ch.ethz.sis.openbis.generic.server.dssapi.v3.FileTransferServerServlet;
import ch.systemsx.cisd.common.action.IDelegatedAction;
import ch.systemsx.cisd.common.exceptions.Status;
import ch.systemsx.cisd.openbis.dss.generic.server.DatasetSessionAuthorizer;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataStoreServiceInternal;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderTestWrapper;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.internal.authorization.DssSessionAuthorizationHolder;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.internal.authorization.IDssSessionAuthorizer;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.SpaceIdentifier;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fastdownload.FastDownloadMethod.*;
import static ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fastdownload.FastDownloadParameter.*;
import static org.junit.Assert.assertEquals;

public final class FileTransferServerServletTest {


    private FileTransferServerServlet servlet;
    private Mockery context;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private IDownloadServer downloadServer;
    private ObjectMapper mapper;
    private IDataStoreServiceInternal dataStoreService;


    @BeforeMethod
    public void setUp()
    {
        DssSessionAuthorizationHolder.setAuthorizer(MOCK_AUTHORIZER);
        context = new Mockery();
        request = context.mock(HttpServletRequest.class);
        response = context.mock(HttpServletResponse.class);
        mapper = new ObjectMapper();
        downloadServer = context.mock(IDownloadServer.class);
        dataStoreService = context.mock(IDataStoreServiceInternal.class);
        servlet = new FileTransferServerServletTestWrapper(downloadServer, dataStoreService);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown()
    {
        DssSessionAuthorizationHolder.setAuthorizer(new DatasetSessionAuthorizer());
    }

    private BufferedReader prepareReaderForRequest(Map<String, List<String>> params) throws Exception {
        String paramsStr = mapper.writeValueAsString(params);
        Reader inputString = new StringReader(paramsStr);
        BufferedReader reader = new BufferedReader(inputString);
        return reader;
    }

    @Test
    public void testStartDownloadSession_v1_simpleCase() throws Exception {
        final String downloadItem1 = "some_data_set/some_data1.txt";
        final String downloadItem2 = "some_data_set/some_data2.txt";

        Map<String, List<String>> params = new HashMap<String, List<String>>() {{
            put(METHOD_PARAMETER.getParameterName(), Arrays.asList(START_DOWNLOAD_SESSION_METHOD.getMethodName()));
            put(VERSION_PARAMETER.getParameterName(), Arrays.asList("1"));
            put(USER_SESSION_ID_PARAMETER.getParameterName(), Arrays.asList("test"));
            //V1 download items are just comma-separated names
            put(DOWNLOAD_ITEM_IDS_PARAMETER.getParameterName(), Arrays.asList(downloadItem1+","+downloadItem2));
            put(WISHED_NUMBER_OF_STREAMS_PARAMETER.getParameterName(), Arrays.asList("1"));
        }};

        DownloadSessionId sessionId = new DownloadSessionId();
        Map<IDownloadItemId, DownloadRange> ranges = new HashMap<>();
        ranges.put(new DownloadItemId(downloadItem1), new DownloadRange(0, 10));
        ranges.put(new DownloadItemId(downloadItem2), new DownloadRange(0, 15));
        List<DownloadStreamId> streamIds = Arrays.asList(new DownloadStreamId());
        DownloadSession downloadSession = new DownloadSession(sessionId, ranges, streamIds);

        StringWriter out    = new StringWriter();
        PrintWriter  writer = new PrintWriter(out);

        context.checking(new Expectations()
        {
            {
                one(request).getReader();
                will(returnValue(prepareReaderForRequest(params)));

                one(downloadServer).startDownloadSession(with(any(IUserSessionId.class)), with(any(List.class)), with(any(DownloadPreferences.class)));
                will(returnValue(downloadSession));

                one(dataStoreService).addCleanupAction(with(any(String.class)), with(any(IDelegatedAction.class)));

                one(response).setContentType(with(any(String.class)));

                one(response).getWriter();
                will(returnValue(writer));
            }
        });

        servlet.service(request, response);
        String result = out.toString();

        HashMap<String, Object> responseParams = mapper.readValue(result, HashMap.class);

        assertEquals(sessionId.getId(), responseParams.get("downloadSessionId"));
        List<String> streamsResult = ((List<String>)responseParams.get("streamIds"));
        assertEquals(1, streamsResult.size());
        assertEquals(streamIds.get(0).getId(), streamsResult.get(0));
        Map<String, String> rangesResult = ((Map<String, String>)responseParams.get("ranges"));
        assertEquals(2, rangesResult.keySet().size());
        assertEquals("0:10", rangesResult.get(downloadItem1));
        assertEquals("0:15", rangesResult.get(downloadItem2));
    }

    @Test
    public void testStartDownloadSession_missingVersion_sameAsV1() throws Exception {
        final String downloadItem1 = "some_data_set/some_data1.txt";
        final String downloadItem2 = "some_data_set/some_data2.txt";

        Map<String, List<String>> params = new HashMap<String, List<String>>() {{
            put(METHOD_PARAMETER.getParameterName(), Arrays.asList(START_DOWNLOAD_SESSION_METHOD.getMethodName()));
            put(USER_SESSION_ID_PARAMETER.getParameterName(), Arrays.asList("test"));
            //V1 download items are just comma-separated names
            put(DOWNLOAD_ITEM_IDS_PARAMETER.getParameterName(), Arrays.asList(downloadItem1+","+downloadItem2));
            put(WISHED_NUMBER_OF_STREAMS_PARAMETER.getParameterName(), Arrays.asList("1"));
        }};

        DownloadSessionId sessionId = new DownloadSessionId();
        Map<IDownloadItemId, DownloadRange> ranges = new HashMap<>();
        ranges.put(new DownloadItemId(downloadItem1), new DownloadRange(0, 10));
        ranges.put(new DownloadItemId(downloadItem2), new DownloadRange(0, 15));
        List<DownloadStreamId> streamIds = Arrays.asList(new DownloadStreamId());
        DownloadSession downloadSession = new DownloadSession(sessionId, ranges, streamIds);

        StringWriter out    = new StringWriter();
        PrintWriter  writer = new PrintWriter(out);

        context.checking(new Expectations()
        {
            {
                one(request).getReader();
                will(returnValue(prepareReaderForRequest(params)));

                one(downloadServer).startDownloadSession(with(any(IUserSessionId.class)), with(any(List.class)), with(any(DownloadPreferences.class)));
                will(returnValue(downloadSession));

                one(dataStoreService).addCleanupAction(with(any(String.class)), with(any(IDelegatedAction.class)));

                one(response).setContentType(with(any(String.class)));

                one(response).getWriter();
                will(returnValue(writer));
            }
        });

        servlet.service(request, response);
        String result = out.toString();

        HashMap<String, Object> responseParams = mapper.readValue(result, HashMap.class);

        assertEquals(sessionId.getId(), responseParams.get("downloadSessionId"));
        List<String> streamsResult = ((List<String>)responseParams.get("streamIds"));
        assertEquals(1, streamsResult.size());
        assertEquals(streamIds.get(0).getId(), streamsResult.get(0));
        Map<String, String> rangesResult = ((Map<String, String>)responseParams.get("ranges"));
        assertEquals(2, rangesResult.keySet().size());
        assertEquals("0:10", rangesResult.get(downloadItem1));
        assertEquals("0:15", rangesResult.get(downloadItem2));
    }

    @Test
    public void testStartDownloadSession_v2_simpleCase() throws Exception {
        final String downloadItem1 = "some_data_set/some_data1.txt";
        final String downloadItem2 = "some_data_set/some_data2.txt";

        Map<String, List<String>> params = new HashMap<String, List<String>>() {{
          put(METHOD_PARAMETER.getParameterName(), Arrays.asList(START_DOWNLOAD_SESSION_METHOD.getMethodName()));
          put(VERSION_PARAMETER.getParameterName(), Arrays.asList("2"));
          put(USER_SESSION_ID_PARAMETER.getParameterName(), Arrays.asList("test"));
          //V2 download items are stored in JSON array
          put(DOWNLOAD_ITEM_IDS_PARAMETER.getParameterName(), Arrays.asList("[\"" + downloadItem1 + "\", \"" + downloadItem2 + "\"]"));
          put(WISHED_NUMBER_OF_STREAMS_PARAMETER.getParameterName(), Arrays.asList("1"));
        }};

        DownloadSessionId sessionId = new DownloadSessionId();
        Map<IDownloadItemId, DownloadRange> ranges = new HashMap<>();
        ranges.put(new DownloadItemId(downloadItem1), new DownloadRange(0, 10));
        ranges.put(new DownloadItemId(downloadItem2), new DownloadRange(0, 15));
        List<DownloadStreamId> streamIds = Arrays.asList(new DownloadStreamId());
        DownloadSession downloadSession = new DownloadSession(sessionId, ranges, streamIds);

        StringWriter out    = new StringWriter();
        PrintWriter  writer = new PrintWriter(out);

        context.checking(new Expectations()
        {
            {
                one(request).getReader();
                will(returnValue(prepareReaderForRequest(params)));

                one(downloadServer).startDownloadSession(with(any(IUserSessionId.class)), with(any(List.class)), with(any(DownloadPreferences.class)));
                will(returnValue(downloadSession));

                one(dataStoreService).addCleanupAction(with(any(String.class)), with(any(IDelegatedAction.class)));

                one(response).setContentType(with(any(String.class)));

                one(response).getWriter();
                will(returnValue(writer));
            }
        });

        servlet.service(request, response);
        String result = out.toString();

        HashMap<String, Object> responseParams = mapper.readValue(result, HashMap.class);

        assertEquals(sessionId.getId(), responseParams.get("downloadSessionId"));
        List<String> streamsResult = ((List<String>)responseParams.get("streamIds"));
        assertEquals(1, streamsResult.size());
        assertEquals(streamIds.get(0).getId(), streamsResult.get(0));
        Map<String, String> rangesResult = ((Map<String, String>)responseParams.get("ranges"));
        assertEquals(2, rangesResult.keySet().size());
        assertEquals("0:10", rangesResult.get(downloadItem1));
        assertEquals("0:15", rangesResult.get(downloadItem2));
    }

    @Test
    public void testStartDownloadSession_v2_fileWithComma() throws Exception {
        final String downloadItem1 = "some_data_set/some_data1,txt";
        final String downloadItem2 = "some_data_set/some_data2.txt";

        Map<String, List<String>> params = new HashMap<String, List<String>>() {{
            put(METHOD_PARAMETER.getParameterName(), Arrays.asList(START_DOWNLOAD_SESSION_METHOD.getMethodName()));
            put(VERSION_PARAMETER.getParameterName(), Arrays.asList("2"));
            put(USER_SESSION_ID_PARAMETER.getParameterName(), Arrays.asList("test"));
            //V2 download items are stored in JSON array
            put(DOWNLOAD_ITEM_IDS_PARAMETER.getParameterName(), Arrays.asList("[\"" + downloadItem1 + "\", \"" + downloadItem2 + "\"]"));
            put(WISHED_NUMBER_OF_STREAMS_PARAMETER.getParameterName(), Arrays.asList("1"));
        }};

        DownloadSessionId sessionId = new DownloadSessionId();
        Map<IDownloadItemId, DownloadRange> ranges = new HashMap<>();
        ranges.put(new DownloadItemId(downloadItem1), new DownloadRange(0, 10));
        ranges.put(new DownloadItemId(downloadItem2), new DownloadRange(0, 15));
        List<DownloadStreamId> streamIds = Arrays.asList(new DownloadStreamId());
        DownloadSession downloadSession = new DownloadSession(sessionId, ranges, streamIds);

        StringWriter out    = new StringWriter();
        PrintWriter  writer = new PrintWriter(out);

        context.checking(new Expectations()
        {
            {
                one(request).getReader();
                will(returnValue(prepareReaderForRequest(params)));

                one(downloadServer).startDownloadSession(with(any(IUserSessionId.class)), with(any(List.class)), with(any(DownloadPreferences.class)));
                will(returnValue(downloadSession));

                one(dataStoreService).addCleanupAction(with(any(String.class)), with(any(IDelegatedAction.class)));

                one(response).setContentType(with(any(String.class)));

                one(response).getWriter();
                will(returnValue(writer));
            }
        });

        servlet.service(request, response);
        String result = out.toString();

        HashMap<String, Object> responseParams = mapper.readValue(result, HashMap.class);

        assertEquals(sessionId.getId(), responseParams.get("downloadSessionId"));
        List<String> streamsResult = ((List<String>)responseParams.get("streamIds"));
        assertEquals(1, streamsResult.size());
        assertEquals(streamIds.get(0).getId(), streamsResult.get(0));
        Map<String, String> rangesResult = ((Map<String, String>)responseParams.get("ranges"));
        assertEquals(2, rangesResult.keySet().size());
        assertEquals("0:10", rangesResult.get(downloadItem1));
        assertEquals("0:15", rangesResult.get(downloadItem2));
    }



    // Helper classes & methods
    private static class FileTransferServerServletTestWrapper extends FileTransferServerServlet {

        FileTransferServerServletTestWrapper(IDownloadServer server, IDataStoreServiceInternal dataStoreServiceInternal)
        {
            super();
            downloadServer = server;
            dataStoreService = dataStoreServiceInternal;
            jsonFactory = new JsonFactory();
        }

    }

    private static final IDssSessionAuthorizer MOCK_AUTHORIZER = new IDssSessionAuthorizer()
    {
        @Override
        public Status checkDatasetAccess(String sessionToken,
                                         List<String> datasetCodes)
        {
            return Status.OK;
        }

        @Override
        public Status checkDatasetAccess(String sessionToken, String datasetCode)
        {
            return Status.OK;
        }

        @Override
        public Status checkSpaceWriteable(String sessionToken, SpaceIdentifier spaceId)
        {
            return Status.createError("Data set authorizer not set.");
        }

        @Override
        public Status checkExperimentWriteable(String sessionToken, String experimentIdentifier)
        {
            return Status.createError("Data set authorizer not set.");
        }

        @Override
        public Status checkSampleWriteable(String sessionToken, String sampleIdentifier)
        {
            return Status.createError("Data set authorizer not set.");
        }

        @Override
        public Status checkInstanceAdminAuthorization(String sessionToken)
        {
            return Status.createError("Data set authorizer not set.");
        }

        @Override
        public Status checkProjectPowerUserAuthorization(String sessionToken)
        {
            return Status.createError("Data set authorizer not set.");
        }
    };

}
