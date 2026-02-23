#
# Copyright 2016-2026 ETH Zuerich, Scientific IT Services
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import json
import traceback

from ch.ethz.sis import JobScheduler
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service import CustomASServiceExecutionOptions
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id import CustomASServiceCode
from java.nio.file import Paths


from org.eclipse.jetty.client import HttpClient
from org.eclipse.jetty.client import HttpProxy
from org.eclipse.jetty.client.transport import HttpClientTransportOverHTTP
from org.eclipse.jetty.util import Jetty
from org.eclipse.jetty.util.ssl import SslContextFactory
from org.eclipse.jetty.client import BasicAuthentication
from org.eclipse.jetty.http import HttpMethod
from org.eclipse.jetty.client import StringRequestContent

# from org.eclipse.jetty.client import HttpClient
# from org.eclipse.jetty.client import HttpProxy
# from org.eclipse.jetty.client.http import HttpClientTransportOverHTTP
# from org.eclipse.jetty.client.util import MultiPartContentProvider
# from org.eclipse.jetty.client.util import PathContentProvider
# from org.eclipse.jetty.client.util import StringContentProvider
# from org.eclipse.jetty.http import HttpMethod
# from org.eclipse.jetty.util import Jetty
# from org.eclipse.jetty.util.ssl import SslContextFactory
from org.json import JSONObject
import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider

from exportsApi import checkResponseStatus, getDownloadUrlFromASService

OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)


def process(context, params):
    method = params.get('method')

    # Set user using the service
    # tr.setUserId(userId)
    if method == 'exportZenodo':
        resultUrl = exportZenodo(context, params)
        return {
            "url": resultUrl
        }
        # displayResult(resultUrl is not None, tableBuilder, '{"url": "' + resultUrl + '"}' if resultUrl is not None else None)

def exportZenodo(context, params):
    sessionToken = params.get('sessionToken')

    exportModel = params.get('entities')
    v3 = context.getApplicationService()
    downloadResultMap = getDownloadUrlFromASService(sessionToken, exportModel, v3)

    resultUrl = sendToZenodo(context=context, params=params, tempZipFilePath=downloadResultMap.get('canonicalPath'), entities=exportModel.get('nodeExportList'))
    return resultUrl


def sendToZenodo(context, params, tempZipFilePath, entities):
    depositRootUrl = CommonServiceProvider.tryToGetProperty('zenodo-exports-api.zenodoUrl') + '/api/deposit/depositions'
    httpProxyURL = CommonServiceProvider.tryToGetProperty('zenodo-exports-api.httpProxyURL')
    httpProxyPort = CommonServiceProvider.tryToGetProperty('zenodo-exports-api.httpProxyPort')

    # depositRootUrl = str(getConfigurationProperty(tr, 'zenodoUrl')) + '/api/deposit/depositions'
    # httpProxyURL = str(getConfigurationProperty(tr, 'httpProxyURL'))
    # httpProxyPort = str(getConfigurationProperty(tr, 'httpProxyPort'))
    accessToken = params.get('accessToken')

    httpClient = None
    try:
        httpClient = createHttpClient(httpProxyURL, httpProxyPort)

        httpClient.setFollowRedirects(False)
        httpClient.start()
        OPERATION_LOG.info('Creating request to: ' + str(depositRootUrl))
        depositionData = createDepositionResource(httpClient.newRequest(depositRootUrl), accessToken)
        OPERATION_LOG.error('||> TESTAaa 0: ')
        depositionLinks = depositionData.get('links')
        depositUrl = depositionLinks.get('files')
        selfUrl = depositionLinks.get('self')
        OPERATION_LOG.error('||> TESTAbbb 0: ')
        submitFile(httpClient.newRequest(depositUrl), accessToken, tempZipFilePath)
        addMetadata(params, httpClient.newRequest(selfUrl), accessToken)
        OPERATION_LOG.error('||> TESTA 0: ')
        entityPermIds = map(lambda entity: entity['permId'], entities)
        zenodoCallable = ZenodoCallable(params, accessToken, selfUrl, httpProxyURL, httpProxyPort,
                                        reduce(lambda str, permId: str + ',' + permId, entityPermIds),
                                        context)
        OPERATION_LOG.error('||> TESTA 1: ')
        zenodoCallable.scheduleMetadataCheck()

        result = depositionLinks.get('html')
        return result
    except Exception as e:
        OPERATION_LOG.error('Exception at: ' + traceback.format_exc())
        OPERATION_LOG.error('Exception: ' + str(e))
        raise e
    finally:
        if httpClient is not None:
            httpClient.stop()


def submitFile(request, accessToken, tempZipFilePath):
    multiPart = MultiPartContentProvider()
    multiPart.addFilePart('file', 'content.zip', PathContentProvider(Paths.get(tempZipFilePath)), None)
    multiPart.close()
    addAuthenticationHeader(accessToken, request)
    response = request.method(HttpMethod.POST).content(multiPart).send()
    checkResponseStatus(response)
    contentStr = response.getContentAsString()

    return JSONObject(contentStr)


def addMetadata(params, request, accessToken):
    data = {
        'metadata': {
            'title': params.get('submissionTitle'),
            'license': 'cc-zero',
            'upload_type': 'dataset',
            'description': 'Add some description.',
            'creators': [{'name': userId}]
        }
    }

    addAuthenticationHeader(accessToken, request)
    jsonString = json.dumps(data)
    content = StringRequestContent('application/json', jsonString)
    response = request.method(HttpMethod.PUT).body(content).send()
    # response = request.method(HttpMethod.PUT).body(StringContentProvider(jsonString), 'application/json').send()

    checkResponseStatus(response)


def retrieve(request, accessToken):
    addAuthenticationHeader(accessToken, request)
    response = request.method(HttpMethod.GET).send()
    contentStr = response.getContentAsString()

    # If the resource has been deleted instead of published return None.
    if response.getStatus() == 410:
        return None

    checkResponseStatus(response)

    return JSONObject(contentStr)


def createDepositionResource(request, accessToken):
    addAuthenticationHeader(accessToken, request)
    content = StringRequestContent('application/json', '{}')
    # response = request.method(HttpMethod.POST).body(StringContentProvider('{}'), 'application/json').send()
    response = request.method(HttpMethod.POST).body(content).send()
    checkResponseStatus(response)

    contentStr = response.getContentAsString()
    return JSONObject(contentStr)
    # return JSONObject(json.dumps({
    #     "links": []
    # }))


def addAuthenticationHeader(accessToken, request):
    request.getHeaders().add('Authorization', 'Bearer ' + accessToken)


def isNonEmptyString(s):
    return isinstance(s, str) and bool(s.strip())

def createHttpClient(httpProxyURL, httpProxyPort):
    jettyVersion = Jetty.VERSION

    sslContextFactory = SslContextFactory.Client()
    sslContextFactory.setTrustAll(True)

    httpClient = None
    if jettyVersion.startswith('9.'):
        httpClient =  HttpClient(sslContextFactory)
    elif jettyVersion.startswith('10.') or jettyVersion.startswith('12.'):
        from org.eclipse.jetty.io import ClientConnector
        clientConnector = ClientConnector()
        clientConnector.setSslContextFactory(sslContextFactory)
        httpClient =  HttpClient(HttpClientTransportOverHTTP(clientConnector))
    else:
        raise ValueError('Unsupported Jetty version: %s. Only [9.x, 10.x, 12.x] are handled for HttpClient creation.' % jettyVersion)

    if isNonEmptyString(httpProxyURL) and isNonEmptyString(httpProxyPort):
        proxyConfig = httpClient.getProxyConfiguration()
        proxyConfig.getProxies().add(HttpProxy(httpProxyURL, int(httpProxyPort)))
    return httpClient


class ZenodoCallable(object):
    params = None
    accessToken = None
    selfUrl = None
    httpProxyURL = None
    httpProxyPort = None
    permIdsStr = None
    context = None

    def __init__(self, params, accessToken, selfUrl, httpProxyURL, httpProxyPort, permIdsStr, context):
        self.params = params
        self.accessToken = accessToken
        self.selfUrl = selfUrl
        self.httpProxyURL = httpProxyURL
        self.httpProxyPort = httpProxyPort
        self.permIdsStr = permIdsStr
        self.context = context

    def scheduleMetadataCheck(self):
        JobScheduler.scheduleRepeatedRequest(120000, 60, self.call)

    def call(self):
        httpClient = None

        # Whether this method returned a completion result and it should not be called repeatedly.
        actionCompleted = False

        try:
            httpClient = createHttpClient(self.httpProxyURL, self.httpProxyPort)

            httpClient.setFollowRedirects(False)
            httpClient.start()

            try:
                publicationJson = retrieve(httpClient.newRequest(self.selfUrl), self.accessToken)
                if publicationJson is None:
                    OPERATION_LOG.info('Publication at the URL has been deleted.' % self.selfUrl)
                    actionCompleted = True
                elif publicationJson.get('submitted'):
                    OPERATION_LOG.info('Publication #%d submitted. Registering metadata.' % publicationJson.get('id'))
                    self.registerPublicationInOpenbis(publicationJson.get('metadata'), publicationJson.get('links'))
                    actionCompleted = True
                else:
                    OPERATION_LOG.info('Publication #%d not submitted yet.' % publicationJson.get('id'))
            except Exception as e:
                OPERATION_LOG.error('Exception at: ' + traceback.format_exc())
                OPERATION_LOG.error('Exception: ' + str(e))
                actionCompleted = False
        except Exception as e:
            OPERATION_LOG.error('Exception at: ' + traceback.format_exc())
            OPERATION_LOG.error('Exception: ' + str(e))
            raise e
        finally:
            if httpClient is not None:
                httpClient.stop()

        return actionCompleted


    def registerPublicationInOpenbis(self, publicationMetadataJson, publicationLinksJson):
        sessionToken = self.params.get('sessionToken')
        v3 = self.context.getApplicationService()

        id = CustomASServiceCode('publication-api')
        options = CustomASServiceExecutionOptions() \
            .withParameter('method', 'insertPublication') \
            .withParameter('publicationURL', publicationLinksJson.get('doi')) \
            .withParameter('openBISRelatedIdentifiers', self.permIdsStr) \
            .withParameter('publicationOrganization', 'Zenodo') \
            .withParameter('name', publicationMetadataJson.get('title')) \
            .withParameter('publicationDescription', publicationMetadataJson.get('description')) \
            .withParameter('publicationType', publicationMetadataJson.get('upload_type')) \
            .withParameter('publicationIdentifier', publicationMetadataJson.get('doi'))
        result = v3.executeCustomASService(sessionToken, id, options)
        return result
