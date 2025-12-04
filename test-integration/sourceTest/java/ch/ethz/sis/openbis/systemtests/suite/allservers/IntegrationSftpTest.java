package ch.ethz.sis.openbis.systemtests.suite.allservers;

import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.INSTANCE_ADMIN;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.PASSWORD;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.environment;
import static org.testng.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;
import ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

public class IntegrationSftpTest
{

    private static final Logger log = LogFactory.getLogger(IntegrationSftpTest.class);

    private static final String SFTP_HOST = "localhost";

    private static final int SFTP_PORT = 2222;

    private static final long SFTP_TIMEOUT = 5000;

    private static final String ENTITY_CODE_PREFIX = "SFTP_TEST_";

    private static final String DSS_DATA_SET_FILE_NAME = "test-file-1.txt";

    private static final String AFS_DATA_SET_FILE_NAME = "test-file-2.txt";

    private static final String DSS_DATA_SET_FILE_CONTENT = "test-content-1";

    private static final String AFS_DATA_SET_FILE_CONTENT = "test-content-2";

    private IntegrationTestFacade facade;

    private DataSet dssDataSet;

    private DataSet afsDataSet;

    @BeforeSuite
    public void beforeSuite()
    {
        AllServersIntegrationTestEnvironment.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        AllServersIntegrationTestEnvironment.stop();
    }

    @BeforeClass public void beforeClass() throws Exception
    {
        IntegrationTestFacade facade = new IntegrationTestFacade(environment);

        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Space space = facade.createSpace(openBIS, "SFTP");
        Project project = facade.createProject(openBIS, space.getPermId(), "SFTP");
        Experiment experiment = facade.createExperiment(openBIS, project.getPermId(), "SFTP");

        dssDataSet = facade.createDataSet(openBIS, experiment.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID(), DSS_DATA_SET_FILE_NAME,
                DSS_DATA_SET_FILE_CONTENT.getBytes());

        openBIS.getAfsServerFacade().write(experiment.getPermId().getPermId(), AFS_DATA_SET_FILE_NAME, 0L, AFS_DATA_SET_FILE_CONTENT.getBytes());
        // calculate a hash for the file, it will be stored in OperationExecutor.HIDDEN_AFS_DIRECTORY folder
        openBIS.getAfsServerFacade().hash(experiment.getPermId().getPermId(), AFS_DATA_SET_FILE_NAME);

        DataSetPermId afsDataSetId = new DataSetPermId(experiment.getPermId().getPermId());
        afsDataSet = openBIS.getDataSets(List.of(afsDataSetId), new DataSetFetchOptions()).get(afsDataSetId);

        log.info("Created DSS data set " + dssDataSet.getPermId());
        log.info("Created AFS data set " + afsDataSet.getPermId());

        openBIS.logout();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception
    {
        facade = new IntegrationTestFacade(environment);
    }

    @Test
    public void testRootFolderContainsDefaultAndElnViews() throws Exception
    {
        final String folder = "/";

        test(INSTANCE_ADMIN, sftp ->
        {
            List<SftpClient.DirEntry> dirEntries = listDir(sftp, folder);
            assertDirEntries(dirEntries, ".", "DEFAULT", "ELN-LIMS");
        });
    }

    @Test
    public void testDefaultView() throws Exception
    {
        test(INSTANCE_ADMIN, sftp ->
        {
            OpenBIS openBIS = facade.createOpenBIS();
            openBIS.login(INSTANCE_ADMIN, PASSWORD);

            List<Space> spaces = openBIS.searchSpaces(new SpaceSearchCriteria(), new SpaceFetchOptions()).getObjects();

            List<SftpClient.DirEntry> viewEntries = listDir(sftp, "/DEFAULT");
            List<String> expectedViewEntries = new ArrayList<>();
            expectedViewEntries.add(".");
            expectedViewEntries.add("..");
            for (Space space : spaces)
            {
                expectedViewEntries.add(space.getCode());
            }
            assertDirEntries(viewEntries, expectedViewEntries.toArray(new String[0]));

            List<SftpClient.DirEntry> spaceEntries = listDir(sftp, "/DEFAULT/SFTP");
            assertDirEntries(spaceEntries, ".", "..", "SFTP");

            List<SftpClient.DirEntry> projectEntries = listDir(sftp, "/DEFAULT/SFTP/SFTP");
            assertDirEntries(projectEntries, ".", "..", "SFTP");

            List<SftpClient.DirEntry> experimentEntries = listDir(sftp, "/DEFAULT/SFTP/SFTP/SFTP");
            assertDirEntries(experimentEntries, ".", "..", dssDataSet.getPermId().getPermId());

            byte[] dssFileContent = readFile(sftp, "/DEFAULT/SFTP/SFTP/SFTP/" + dssDataSet.getPermId().getPermId() + "/" + DSS_DATA_SET_FILE_NAME);
            assertEquals(dssFileContent, DSS_DATA_SET_FILE_CONTENT.getBytes());
        });
    }

    @Test
    public void testElnView() throws Exception
    {
        test(INSTANCE_ADMIN, sftp ->
        {
            List<SftpClient.DirEntry> dirEntries = listDir(sftp, "/ELN-LIMS");
            assertDirEntries(dirEntries, ".", "..", "Lab Notebook", "Inventory", "Stock");
        });
    }

    @Test
    public void testElnViewLabNotebook() throws Exception
    {
        test(INSTANCE_ADMIN, sftp ->
        {
            List<SftpClient.DirEntry> spaceEntries = listDir(sftp, "/ELN-LIMS/Lab Notebook/SFTP");
            assertDirEntries(spaceEntries, ".", "..", "SFTP");

            List<SftpClient.DirEntry> projectEntries = listDir(sftp, "/ELN-LIMS/Lab Notebook/SFTP/SFTP");
            assertDirEntries(projectEntries, ".", "..", "SFTP");

            List<SftpClient.DirEntry> experimentEntries = listDir(sftp, "/ELN-LIMS/Lab Notebook/SFTP/SFTP/SFTP");
            assertDirEntries(experimentEntries, ".", "..", afsDataSet.getPermId().getPermId(), dssDataSet.getPermId().getPermId());

            List<SftpClient.DirEntry> afsDataSetEntries =
                    listDir(sftp, "/ELN-LIMS/Lab Notebook/SFTP/SFTP/SFTP/" + afsDataSet.getPermId().getPermId());
            assertDirEntries(afsDataSetEntries, ".", "..", AFS_DATA_SET_FILE_NAME);

            List<SftpClient.DirEntry> dssDataSetEntries =
                    listDir(sftp, "/ELN-LIMS/Lab Notebook/SFTP/SFTP/SFTP/" + dssDataSet.getPermId().getPermId());
            assertDirEntries(dssDataSetEntries, ".", "..", DSS_DATA_SET_FILE_NAME);

            byte[] afsFileContent =
                    readFile(sftp, "/ELN-LIMS/Lab Notebook/SFTP/SFTP/SFTP/" + afsDataSet.getPermId().getPermId() + "/" + AFS_DATA_SET_FILE_NAME);
            assertEquals(afsFileContent, AFS_DATA_SET_FILE_CONTENT.getBytes());

            byte[] dssFileContent =
                    readFile(sftp, "/ELN-LIMS/Lab Notebook/SFTP/SFTP/SFTP/" + dssDataSet.getPermId().getPermId() + "/" + DSS_DATA_SET_FILE_NAME);
            assertEquals(dssFileContent, DSS_DATA_SET_FILE_CONTENT.getBytes());
        });
    }

    @Test
    public void testElnViewInventory() throws Exception
    {
        test(INSTANCE_ADMIN, sftp ->
        {
            List<SftpClient.DirEntry> inventoryEntries = listDir(sftp, "/ELN-LIMS/Inventory");
            assertDirEntries(inventoryEntries, ".", "..", "MATERIALS", "PUBLICATIONS");

            List<SftpClient.DirEntry> materialsEntries = listDir(sftp, "/ELN-LIMS/Inventory/MATERIALS");
            assertDirEntries(materialsEntries, ".", "..");

            List<SftpClient.DirEntry> publicationsEntries = listDir(sftp, "/ELN-LIMS/Inventory/PUBLICATIONS");
            assertDirEntries(publicationsEntries, ".", "..", "PUBLIC_REPOSITORIES");

            List<SftpClient.DirEntry> publicRepositoriesEntries = listDir(sftp, "/ELN-LIMS/Inventory/PUBLICATIONS/PUBLIC_REPOSITORIES");
            assertDirEntries(publicRepositoriesEntries, ".", "..", "Publications Collection");
        });
    }

    @Test
    public void testElnViewStock() throws Exception
    {
        test(INSTANCE_ADMIN, sftp ->
        {
            List<SftpClient.DirEntry> stockEntries = listDir(sftp, "/ELN-LIMS/Stock");
            assertDirEntries(stockEntries, ".", "..", "STOCK_CATALOG", "STOCK_ORDERS");

            List<SftpClient.DirEntry> stockCatalogEntries = listDir(sftp, "/ELN-LIMS/Stock/STOCK_CATALOG");
            assertDirEntries(stockCatalogEntries, ".", "..", "PRODUCTS", "SUPPLIERS", "REQUESTS");

            List<SftpClient.DirEntry> productsEntries = listDir(sftp, "/ELN-LIMS/Stock/STOCK_CATALOG/PRODUCTS");
            assertDirEntries(productsEntries, ".", "..", "Product Collection");

            List<SftpClient.DirEntry> requestsEntries = listDir(sftp, "/ELN-LIMS/Stock/STOCK_CATALOG/REQUESTS");
            assertDirEntries(requestsEntries, ".", "..", "Request Collection");

            List<SftpClient.DirEntry> suppliersEntries = listDir(sftp, "/ELN-LIMS/Stock/STOCK_CATALOG/SUPPLIERS");
            assertDirEntries(suppliersEntries, ".", "..", "Supplier Collection");

            List<SftpClient.DirEntry> stockOrdersEntries = listDir(sftp, "/ELN-LIMS/Stock/STOCK_ORDERS");
            assertDirEntries(stockOrdersEntries, ".", "..", "ORDERS");

            List<SftpClient.DirEntry> ordersEntries = listDir(sftp, "/ELN-LIMS/Stock/STOCK_ORDERS/ORDERS");
            assertDirEntries(ordersEntries, ".", "..", "Order Collection");
        });
    }

    private List<SftpClient.DirEntry> listDir(SftpClient sftp, String dirPath) throws Exception
    {
        try (SftpClient.CloseableHandle handle = sftp.openDir(dirPath))
        {
            return sftp.readDir(handle);
        }
    }

    private byte[] readFile(SftpClient sftp, String filePath) throws Exception
    {
        try (InputStream inputStream = sftp.read(filePath))
        {
            return IOUtils.toByteArray(inputStream);
        }
    }

    private void assertDirEntries(List<SftpClient.DirEntry> dirEntries, String... expectedFileNames)
    {
        assertEquals(dirEntries.stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toList()), Arrays.asList(expectedFileNames));
    }

    private void test(String user, SftpTestAction action) throws Exception
    {
        try (SshClient client = SshClient.setUpDefaultClient())
        {
            client.start();

            try (ClientSession session = client.connect(user, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession())
            {
                session.addPasswordIdentity(PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session))
                {
                    action.execute(sftp);
                }
            } finally
            {
                client.stop();
            }
        }
    }

    private interface SftpTestAction
    {

        void execute(SftpClient sftp) throws Exception;

    }
}
