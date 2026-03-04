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
from java.nio.file import Path


from org.eclipse.jetty.client import HttpClient
from org.eclipse.jetty.client import HttpProxy

from org.eclipse.jetty.util import Jetty
from org.eclipse.jetty.util.ssl import SslContextFactory
from org.eclipse.jetty.http import HttpMethod

if Jetty.VERSION.startswith('12.'):
    from org.eclipse.jetty.client.transport import HttpClientTransportOverHTTP
    from org.eclipse.jetty.http import HttpFields
    from org.eclipse.jetty.http import MultiPart
    from org.eclipse.jetty.client import StringRequestContent
    from org.eclipse.jetty.client import MultiPartRequestContent
    from org.eclipse.jetty.io import ByteBufferPool
else:
    from org.eclipse.jetty.client.http import HttpClientTransportOverHTTP
    from org.eclipse.jetty.client.util import MultiPartContentProvider
    from org.eclipse.jetty.client.util import PathContentProvider
    from org.eclipse.jetty.client.util import StringContentProvider

from org.json import JSONObject

import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider

from util import checkResponseStatus, getDownloadUrlFromASService

OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

ZENODO_URL_PROPERTY_KEY = 'exports-api.zenodo.url'
ZENODO_PROXY_URL_PROPERTY_KEY = 'exports-api.zenodo.http.proxy.url'
ZENODO_PROXY_PORT_PROPERTY_KEY = 'exports-api.zenodo.http.proxy.port'

def exportZenodo(context, params):
    sessionToken = params.get('sessionToken')

    exportModel = params.get('entities')
    v3 = context.getApplicationService()
    downloadResultMap = getDownloadUrlFromASService(sessionToken, exportModel, v3)

    resultUrl = sendToZenodo(context=context, params=params, tempZipFilePath=downloadResultMap.get('canonicalPath'), entities=exportModel.get('nodeExportList'))

    result = {
        "url": resultUrl,
    }
    return result


def sendToZenodo(context, params, tempZipFilePath, entities):
    depositRootUrl = CommonServiceProvider.tryToGetProperty(ZENODO_URL_PROPERTY_KEY) + '/api/deposit/depositions'
    httpProxyURL = CommonServiceProvider.tryToGetProperty(ZENODO_PROXY_URL_PROPERTY_KEY)
    httpProxyPort = CommonServiceProvider.tryToGetProperty(ZENODO_PROXY_PORT_PROPERTY_KEY)

    accessToken = params.get('accessToken')

    httpClient = None
    try:
        httpClient = createHttpClient(httpProxyURL, httpProxyPort)

        httpClient.setFollowRedirects(False)
        httpClient.start()
        OPERATION_LOG.info('Creating request to: ' + str(depositRootUrl))
        depositionData = createDepositionResource(httpClient.newRequest(depositRootUrl), accessToken)
        depositionLinks = depositionData.get('links')
        depositUrl = depositionLinks.get('files')
        selfUrl = depositionLinks.get('self')
        OPERATION_LOG.info('Submitting file to: ' + str(depositUrl))
        if Jetty.VERSION.startswith('12.'):
        # if False:
            submitFileJettyLess(depositUrl, accessToken, tempZipFilePath, params.get('fileName'), httpProxyURL, httpProxyPort)
        else:
            submitFile(httpClient, httpClient.newRequest(depositUrl), accessToken, tempZipFilePath, params.get('fileName'),)
        OPERATION_LOG.info('Submitting metadata to: ' + str(selfUrl))
        addMetadata(params, httpClient.newRequest(selfUrl), accessToken)

        entityPermIds = map(lambda entity: entity['permId'], entities)
        zenodoCallable = ZenodoCallable(params, accessToken, selfUrl, httpProxyURL, httpProxyPort,
                                        reduce(lambda str, permId: str + ',' + permId, entityPermIds),
                                        context)

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



def upload_file_with_proxy(url, file_path, accessToken, fileName, proxy_host=None, proxy_port=None):
    '''
        Pure Java implementation of multi-part upload of data to zenodo.org
    '''
    import java.net.URI as URI
    import java.net.InetSocketAddress as InetSocketAddress
    import java.net.ProxySelector as ProxySelector
    import java.net.http.HttpClient as HttpClient
    import java.net.http.HttpRequest as HttpRequest
    import java.net.http.HttpResponse as HttpResponse
    import java.nio.file.Path as Path
    import java.lang.System as System
    boundary = "OpenBISBoundary" + str(System.currentTimeMillis())
    path = Path.of(file_path)

    if proxy_host is None or proxy_host == "":
        client = HttpClient.newHttpClient()
    else:
        client = HttpClient.newBuilder() \
            .proxy(ProxySelector.of(InetSocketAddress(proxy_host, int(proxy_port)))) \
            .build()

    if fileName == "":
        fileName = "content"

    before = ("--" + boundary + "\r\n" +
              "Content-Disposition: form-data; name=\"file\"; filename=\"" + str(fileName) + ".zip\"\r\n" +
              "Content-Type: application/octet-stream\r\n\r\n").encode('utf-8')
    after = ("\r\n--" + boundary + "--\r\n").encode('utf-8')

    BodyPublishers = HttpRequest.BodyPublishers

    request = HttpRequest.newBuilder() \
        .uri(URI.create(url)) \
        .header("Content-Type", "multipart/form-data; boundary=" + boundary) \
        .header("Authorization", 'Bearer ' + accessToken) \
        .POST(BodyPublishers.concat(
        BodyPublishers.ofByteArray(before),
        BodyPublishers.ofFile(path),
        BodyPublishers.ofByteArray(after)
    )) \
        .build()

    response = client.send(request, HttpResponse.BodyHandlers.ofString())

    status = response.statusCode()
    if status >= 300:
        reason = json.loads(response.body())['message']
        raise ValueError('Unsuccessful response from the server: %s %s' % (status, reason))

    return response.body()

def submitFileJettyLess(url, accessToken, tempZipFilePath, fileName, httpProxyURL=None, proxyPort=None):
    response = upload_file_with_proxy(url, tempZipFilePath, accessToken, fileName, httpProxyURL, proxyPort)
    return JSONObject(str(response))

def submitFile(httpClient, request, accessToken, tempZipFilePath, fileName):
    if fileName == "":
        fileName = "content"
    if Jetty.VERSION.startswith('12.'):
        '''
        Although this looks proper, upload to zenodo.org fails with 400 Bad Request error for jetty 12+
        check out submitFileJettyLess() method.
        '''
        sized = ByteBufferPool.Sized(httpClient.getByteBufferPool())
        path = Path.of(tempZipFilePath)
        multiPart = MultiPartRequestContent()
        multiPart.addPart(MultiPart.PathPart(sized, "file", str(fileName) + '.zip', HttpFields.EMPTY, path))
        multiPart.close()

        addAuthenticationHeader(accessToken, request)
        response = request.method(HttpMethod.POST).body(multiPart).send()
    else:
        multiPart = MultiPartContentProvider()
        multiPart.addFilePart('file', str(fileName) + '.zip', PathContentProvider(Paths.get(tempZipFilePath)), None)
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
            'creators': [{'name': params['userId']}]
        }
    }

    addAuthenticationHeader(accessToken, request)
    jsonString = json.dumps(data)
    if Jetty.VERSION.startswith('12.'):
        content = StringRequestContent('application/json', jsonString)
        response = request.method(HttpMethod.PUT).body(content).send()
    else:
        content = StringContentProvider(jsonString)
        response = request.method(HttpMethod.PUT).content(content, 'application/json').send()

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

    if Jetty.VERSION.startswith('12.'):
        content = StringRequestContent('application/json', '{}')
        response = request.method(HttpMethod.POST).body(content).send()
    else:
        content = StringContentProvider('{}')
        response = request.method(HttpMethod.POST).content(content, 'application/json').send()
    checkResponseStatus(response)

    contentStr = response.getContentAsString()
    return JSONObject(contentStr)


def addAuthenticationHeader(accessToken, request):
    if Jetty.VERSION.startswith('12.'):
        request.getHeaders().add('Authorization', 'Bearer ' + accessToken)
    else:
        request.header('Authorization', 'Bearer ' + accessToken)


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
        if jettyVersion.startswith('12.'):
            proxyConfig = httpClient.getProxyConfiguration()
            proxyConfig.addProxy(HttpProxy(httpProxyURL, int(httpProxyPort)))
        else:
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
