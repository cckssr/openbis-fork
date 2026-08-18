package ch.openbis.drive.util;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.PersonalAccessToken;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.create.PersonalAccessTokenCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.fetchoptions.PersonalAccessTokenFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.id.PersonalAccessTokenPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.search.PersonalAccessTokenSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.openbis.drive.DriveTestCase;
import ch.openbis.drive.model.SyncJob;
import ch.systemsx.cisd.common.exceptions.InvalidSessionException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.remoting.RemoteAccessException;

import java.time.ZoneId;
import java.util.*;

public class OpenBISQueryUtilTest extends DriveTestCase {

    public void testCheckPATSuccess() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        PersonalAccessToken personalAccessToken = new PersonalAccessToken();
        personalAccessToken.setValidToDate(new Date(System.currentTimeMillis() + 100000));
        Person owner = new Person();
        owner.setUserId("user1");
        personalAccessToken.setOwner(owner);
        PersonalAccessTokenFetchOptions personalAccessTokenFetchOptions = new PersonalAccessTokenFetchOptions();
        personalAccessTokenFetchOptions.withOwner();
        personalAccessToken.setFetchOptions(personalAccessTokenFetchOptions);
        Mockito.doReturn(Map.of(new PersonalAccessTokenPermId("PAT-pat"), personalAccessToken))
            .when(openbisClientMock).getPersonalAccessTokens(
                    Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                    Mockito.any()
            );

        ArgumentCaptor<PersonalAccessTokenFetchOptions> personalAccessTokenFetchOptionsArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenFetchOptions.class);
        OpenBISQueryUtil.PATCheckResult patCheckResult = openBISQueryUtil.checkPAT("https://myopenbis.example.com", "PAT-pat");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).setSessionToken("PAT-pat");
        Mockito.verify(openbisClientMock).getPersonalAccessTokens(
                Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                personalAccessTokenFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(OpenBISQueryUtil.PATCheckResultEnum.OK, patCheckResult.result());
        assertEquals("user1", patCheckResult.user());
        assertEquals(personalAccessToken.getValidToDate(), patCheckResult.validUntil());
        assertTrue(personalAccessTokenFetchOptionsArgumentCaptor.getValue().hasOwner());
    }

    public void testCheckPATNotFound() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doReturn(Collections.emptyMap())
                .when(openbisClientMock).getPersonalAccessTokens(
                        Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                        Mockito.any()
                );

        ArgumentCaptor<PersonalAccessTokenFetchOptions> personalAccessTokenFetchOptionsArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenFetchOptions.class);
        OpenBISQueryUtil.PATCheckResult patCheckResult = openBISQueryUtil.checkPAT("https://myopenbis.example.com", "PAT-pat");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).setSessionToken("PAT-pat");
        Mockito.verify(openbisClientMock).getPersonalAccessTokens(
                Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                personalAccessTokenFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(OpenBISQueryUtil.PATCheckResultEnum.INVALID_SESSION, patCheckResult.result());
        assertTrue(personalAccessTokenFetchOptionsArgumentCaptor.getValue().hasOwner());
    }

    public void testCheckPATInvalidSession() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doThrow(new InvalidSessionException("error"))
                .when(openbisClientMock).getPersonalAccessTokens(
                        Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                        Mockito.any()
                );

        ArgumentCaptor<PersonalAccessTokenFetchOptions> personalAccessTokenFetchOptionsArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenFetchOptions.class);
        OpenBISQueryUtil.PATCheckResult patCheckResult = openBISQueryUtil.checkPAT("https://myopenbis.example.com", "PAT-pat");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).setSessionToken("PAT-pat");
        Mockito.verify(openbisClientMock).getPersonalAccessTokens(
                Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                personalAccessTokenFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(OpenBISQueryUtil.PATCheckResultEnum.INVALID_SESSION, patCheckResult.result());
        assertTrue(personalAccessTokenFetchOptionsArgumentCaptor.getValue().hasOwner());
    }

    public void testCheckPATServerUnreachable() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doThrow(new RemoteAccessException("error"))
                .when(openbisClientMock).getPersonalAccessTokens(
                        Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                        Mockito.any()
                );

        ArgumentCaptor<PersonalAccessTokenFetchOptions> personalAccessTokenFetchOptionsArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenFetchOptions.class);
        OpenBISQueryUtil.PATCheckResult patCheckResult = openBISQueryUtil.checkPAT("https://myopenbis.example.com", "PAT-pat");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).setSessionToken("PAT-pat");
        Mockito.verify(openbisClientMock).getPersonalAccessTokens(
                Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                personalAccessTokenFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(OpenBISQueryUtil.PATCheckResultEnum.ERROR_REACHING_SERVER, patCheckResult.result());
        assertTrue(personalAccessTokenFetchOptionsArgumentCaptor.getValue().hasOwner());
    }

    public void testCheckPATUnknownError() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doThrow(new RuntimeException())
                .when(openbisClientMock).getPersonalAccessTokens(
                        Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                        Mockito.any()
                );

        ArgumentCaptor<PersonalAccessTokenFetchOptions> personalAccessTokenFetchOptionsArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenFetchOptions.class);
        OpenBISQueryUtil.PATCheckResult patCheckResult = openBISQueryUtil.checkPAT("https://myopenbis.example.com", "PAT-pat");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).setSessionToken("PAT-pat");
        Mockito.verify(openbisClientMock).getPersonalAccessTokens(
                Mockito.eq(Collections.singletonList(new PersonalAccessTokenPermId("PAT-pat"))),
                personalAccessTokenFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(OpenBISQueryUtil.PATCheckResultEnum.UNKNOWN_ERROR, patCheckResult.result());
        assertTrue(personalAccessTokenFetchOptionsArgumentCaptor.getValue().hasOwner());
    }

    public void testGetAvailableSessions() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());

        SyncJob syncJob1 = new SyncJob();
        syncJob1.setOpenBisUrl("https://myopenbis1.example.com");
        syncJob1.setOpenBisPersonalAccessToken("pat-1");
        SyncJob syncJob2 = new SyncJob();
        syncJob2.setOpenBisUrl("https://myopenbis2.example.com");
        syncJob2.setOpenBisPersonalAccessToken("pat-2");
        SyncJob syncJob3 = new SyncJob();
        syncJob3.setOpenBisUrl("https://myopenbis3.example.com");
        syncJob3.setOpenBisPersonalAccessToken("pat-3");
        SyncJob syncJob4 = new SyncJob();
        syncJob4.setOpenBisUrl("https://myopenbis4.example.com");
        syncJob4.setOpenBisPersonalAccessToken("pat-4");
        SyncJob syncJob5 = new SyncJob();
        syncJob5.setOpenBisUrl("https://myopenbis5.example.com");
        syncJob5.setOpenBisPersonalAccessToken("pat-5");
        SyncJob syncJob6 = new SyncJob();
        syncJob6.setOpenBisUrl("https://myopenbis5.example.com");
        syncJob6.setOpenBisPersonalAccessToken("pat-6");

        Date validUntil1 = new Date(System.currentTimeMillis() + 100000);
        Mockito.doReturn(new OpenBISQueryUtil.PATCheckResult(
                OpenBISQueryUtil.PATCheckResultEnum.OK,
                "user1",
                validUntil1
        )).when(openBISQueryUtil).checkPAT("https://myopenbis1.example.com", "pat-1");

        Mockito.doReturn(new OpenBISQueryUtil.PATCheckResult(
                OpenBISQueryUtil.PATCheckResultEnum.INVALID_SESSION,
                null,
                null
        )).when(openBISQueryUtil).checkPAT("https://myopenbis2.example.com", "pat-2");

        Mockito.doReturn(new OpenBISQueryUtil.PATCheckResult(
                OpenBISQueryUtil.PATCheckResultEnum.ERROR_REACHING_SERVER,
                null,
                null
        )).when(openBISQueryUtil).checkPAT("https://myopenbis3.example.com", "pat-3");

        Mockito.doReturn(new OpenBISQueryUtil.PATCheckResult(
                OpenBISQueryUtil.PATCheckResultEnum.UNKNOWN_ERROR,
                null,
                null
        )).when(openBISQueryUtil).checkPAT("https://myopenbis4.example.com", "pat-4");

        Date validUntil2 = new Date(System.currentTimeMillis() + 200000);
        Mockito.doReturn(new OpenBISQueryUtil.PATCheckResult(
                OpenBISQueryUtil.PATCheckResultEnum.OK,
                "user2",
                validUntil2
        )).when(openBISQueryUtil).checkPAT("https://myopenbis5.example.com", "pat-5");

        Date validUntil3 = new Date(System.currentTimeMillis() + 150000);
        Mockito.doReturn(new OpenBISQueryUtil.PATCheckResult(
                OpenBISQueryUtil.PATCheckResultEnum.OK,
                "user2",
                validUntil3
        )).when(openBISQueryUtil).checkPAT("https://myopenbis5.example.com", "pat-6");

        Set<OpenBISQueryUtil.AvailableSession> availableSessionSet = openBISQueryUtil.getAvailableSessions(List.of(
                syncJob1,
                syncJob2,
                syncJob3,
                syncJob4,
                syncJob5,
                syncJob6
        ));

        assertEquals(Set.of(
                new OpenBISQueryUtil.AvailableSession(
                        "user1",
                        "https://myopenbis1.example.com",
                        "pat-1",
                        validUntil1.toInstant().atZone(ZoneId.systemDefault())),
                new OpenBISQueryUtil.AvailableSession(
                        "user2",
                        "https://myopenbis5.example.com",
                        "pat-5",
                        validUntil2.toInstant().atZone(ZoneId.systemDefault()))
        ), availableSessionSet);
    }

    public void testGetNewSessionSuccessRetrievingExistingSession() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doReturn("session-tkn").when(openbisClientMock).login("user1", "pWd");

        Date validUntil = new Date(System.currentTimeMillis()
                + OpenBISQueryUtil.PAT_MINIMUM_LEFT_VALIDITY_MILLIS
                + 1000000
        );
        PersonalAccessToken personalAccessToken = new PersonalAccessToken();
        personalAccessToken.setPermId(new PersonalAccessTokenPermId("pat-PAT"));
        personalAccessToken.setValidFromDate(new Date(System.currentTimeMillis() - 1000));
        personalAccessToken.setValidToDate(validUntil);
        SearchResult<PersonalAccessToken> searchResult = new SearchResult<>(Collections.singletonList(personalAccessToken), 1);

        Mockito.doReturn(searchResult).when(openbisClientMock).searchPersonalAccessTokens(Mockito.any(), Mockito.any());

        ArgumentCaptor<PersonalAccessTokenSearchCriteria> searchCriteriaArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenSearchCriteria.class);

        OpenBISQueryUtil.NewSessionResult newSessionResult = openBISQueryUtil.getNewSession(
                "https://myopenbis.example.com", "user1", "pWd");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).login("user1", "pWd");
        Mockito.verify(openbisClientMock, Mockito.times(1)).searchPersonalAccessTokens(
                searchCriteriaArgumentCaptor.capture(),
                Mockito.any()
        );
        assertTrue(searchCriteriaArgumentCaptor.getValue().toString().contains("with attribute 'sessionName' equal to 'OPENBIS_DRIVE_GENERATED_SESSION'"));
        assertTrue(searchCriteriaArgumentCaptor.getValue().toString().contains("with attribute 'userId' equal to 'user1'"));

        assertEquals(OpenBISQueryUtil.NewSessionResultEnum.OK, newSessionResult.result());
        assertEquals("pat-PAT", newSessionResult.availableSession().personalAccessToken());
        assertEquals("user1", newSessionResult.availableSession().username());
        assertEquals("https://myopenbis.example.com", newSessionResult.availableSession().openBISUrl());
        assertEquals(validUntil.toInstant().atZone(ZoneId.systemDefault()), newSessionResult.availableSession().validUntil());
    }

    public void testGetNewSessionSuccessCreatingNewSession() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doReturn("session-tkn").when(openbisClientMock).login("user1", "pWd");

        Date validUntil = new Date(System.currentTimeMillis()
                + OpenBISQueryUtil.PAT_MINIMUM_LEFT_VALIDITY_MILLIS
                - 1000
        );
        PersonalAccessToken personalAccessToken = new PersonalAccessToken();
        personalAccessToken.setPermId(new PersonalAccessTokenPermId("pat-PAT"));
        personalAccessToken.setValidFromDate(new Date(System.currentTimeMillis() - 1000));
        personalAccessToken.setValidToDate(validUntil);
        SearchResult<PersonalAccessToken> searchResult = new SearchResult<>(Collections.singletonList(personalAccessToken), 1);

        Mockito.doReturn(searchResult).when(
                openbisClientMock
        ).searchPersonalAccessTokens(Mockito.any(), Mockito.any());

        Mockito.doReturn(Collections.singletonList(new PersonalAccessTokenPermId("new-pat"))).when(
                openbisClientMock
        ).createPersonalAccessTokens(Mockito.anyList());

        ArgumentCaptor<PersonalAccessTokenSearchCriteria> searchCriteriaArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenSearchCriteria.class);

        ArgumentCaptor<List<PersonalAccessTokenCreation>> patCreationsArgumentCaptor =
                ArgumentCaptor.forClass(List.class);

        OpenBISQueryUtil.NewSessionResult newSessionResult = openBISQueryUtil.getNewSession(
                "https://myopenbis.example.com", "user1", "pWd");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).login("user1", "pWd");
        Mockito.verify(openbisClientMock, Mockito.times(1)).searchPersonalAccessTokens(
                searchCriteriaArgumentCaptor.capture(),
                Mockito.any()
        );
        Mockito.verify(openbisClientMock, Mockito.times(1)).createPersonalAccessTokens(
                patCreationsArgumentCaptor.capture()
        );
        assertTrue(searchCriteriaArgumentCaptor.getValue().toString().contains("with attribute 'sessionName' equal to 'OPENBIS_DRIVE_GENERATED_SESSION'"));
        assertTrue(searchCriteriaArgumentCaptor.getValue().toString().contains("with attribute 'userId' equal to 'user1'"));

        assertEquals(1, patCreationsArgumentCaptor.getValue().size());
        assertEquals(
                OpenBISQueryUtil.DRIVE_PAT_SESSION_NAME,
                patCreationsArgumentCaptor.getValue().getFirst().getSessionName()
        );
        assertEquals(
                new PersonPermId("user1"),
                patCreationsArgumentCaptor.getValue().getFirst().getOwnerId()
        );
        assertTrue(
                patCreationsArgumentCaptor.getValue().getFirst().getValidFromDate().getTime() <= System.currentTimeMillis()
        );
        assertTrue(
                patCreationsArgumentCaptor.getValue().getFirst().getValidToDate().getTime() >
                        System.currentTimeMillis() + OpenBISQueryUtil.GENERATED_PAT_DURATION_MILLIS - 100000
        );
        assertTrue(
                patCreationsArgumentCaptor.getValue().getFirst().getValidToDate().getTime() <
                        System.currentTimeMillis() + OpenBISQueryUtil.GENERATED_PAT_DURATION_MILLIS + 100000
        );

        assertEquals(OpenBISQueryUtil.NewSessionResultEnum.OK, newSessionResult.result());
        assertEquals("new-pat", newSessionResult.availableSession().personalAccessToken());
        assertEquals("user1", newSessionResult.availableSession().username());
        assertEquals("https://myopenbis.example.com", newSessionResult.availableSession().openBISUrl());
    }

    public void testGetNewSessionFailureBadCredentials() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doReturn(null).when(openbisClientMock).login("user1", "pWd");

        OpenBISQueryUtil.NewSessionResult newSessionResult = openBISQueryUtil.getNewSession(
                "https://myopenbis.example.com", "user1", "pWd");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).login("user1", "pWd");
        Mockito.verify(openbisClientMock, Mockito.times(0)).searchPersonalAccessTokens(
                Mockito.any(),
                Mockito.any()
        );

        assertEquals(OpenBISQueryUtil.NewSessionResultEnum.BAD_CREDENTIALS, newSessionResult.result());
        assertNull(newSessionResult.availableSession());
    }

    public void testGetNewSessionFailureReachingServer() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doThrow(new RemoteAccessException("error")).when(openbisClientMock).login("user1", "pWd");

        OpenBISQueryUtil.NewSessionResult newSessionResult = openBISQueryUtil.getNewSession(
                "https://myopenbis.example.com", "user1", "pWd");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).login("user1", "pWd");
        Mockito.verify(openbisClientMock, Mockito.times(0)).searchPersonalAccessTokens(
                Mockito.any(),
                Mockito.any()
        );

        assertEquals(OpenBISQueryUtil.NewSessionResultEnum.ERROR_REACHING_SERVER, newSessionResult.result());
        assertNull(newSessionResult.availableSession());
    }

    public void testGetNewSessionFailureUnknownError() {
        OpenBISQueryUtil openBISQueryUtil = Mockito.spy(new OpenBISQueryUtil());
        OpenBIS openbisClientMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openbisClientMock).when(openBISQueryUtil)
                .getOpenbisClient(Mockito.anyString());

        Mockito.doReturn("session-tkn").when(openbisClientMock).login("user1", "pWd");

        Mockito.doThrow(new RuntimeException()).when(openbisClientMock).searchPersonalAccessTokens(Mockito.any(), Mockito.any());

        ArgumentCaptor<PersonalAccessTokenSearchCriteria> searchCriteriaArgumentCaptor =
                ArgumentCaptor.forClass(PersonalAccessTokenSearchCriteria.class);

        OpenBISQueryUtil.NewSessionResult newSessionResult = openBISQueryUtil.getNewSession(
                "https://myopenbis.example.com", "user1", "pWd");
        Mockito.verify(openBISQueryUtil, Mockito.times(1)).getOpenbisClient("https://myopenbis.example.com");
        Mockito.verify(openbisClientMock, Mockito.times(1)).login("user1", "pWd");
        Mockito.verify(openbisClientMock, Mockito.times(1)).searchPersonalAccessTokens(
                searchCriteriaArgumentCaptor.capture(),
                Mockito.any()
        );
        assertTrue(searchCriteriaArgumentCaptor.getValue().toString().contains("with attribute 'sessionName' equal to 'OPENBIS_DRIVE_GENERATED_SESSION'"));
        assertTrue(searchCriteriaArgumentCaptor.getValue().toString().contains("with attribute 'userId' equal to 'user1'"));

        assertEquals(OpenBISQueryUtil.NewSessionResultEnum.UNKNOWN_ERROR, newSessionResult.result());
        assertNull(newSessionResult.availableSession());
    }
}