/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.fcrepo.kernel.api.FedoraTypes.CONTENT_DIGEST;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FILENAME;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.FedoraTypes.PROXY_FOR;
import static org.fcrepo.kernel.api.FedoraTypes.REDIRECTS_TO;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.getContentNodeMock;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.checksumString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.Collections.singleton;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.io.InputStream;
import java.net.URI;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * @author bbpennel
 */
@RunWith(MockitoJUnitRunner.class)
public class UrlBinaryTest {

    private static final String DS_ID = "testDs";

    private static final String EXPECTED_CONTENT = "test content";

    private FedoraBinary testObj;

    private String mimeType;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    private String fileUrl;

    @Mock
    private Session mockSession;

    @Mock
    private Property mimeTypeProperty;

    @Mock
    private Property proxyURLProperty;

    @Mock
    private Property redirectURLProperty;

    @Mock
    private Property mockProperty;

    @Mock
    private Value mockValue;

    @Mock
    private Value mockURIValue;

    @Mock
    private Node mockDescNode, mockContent, mockParentNode;

    @Mock
    private NodeType mockDescNodeType;

    @Mock
    private InputStream mockStream;

    @Before
    public void setUp() throws Exception {
        final NodeType[] nodeTypes = new NodeType[] { mockDescNodeType };

        stubFor(get(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(EXPECTED_CONTENT)));
        fileUrl = "http://localhost:" + wireMockRule.port() + "/file.txt";

        mimeType = "text/plain";

        when(mockDescNodeType.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        when(mockDescNode.getMixinNodeTypes()).thenReturn(nodeTypes);
        when(mockDescNode.getParent()).thenReturn(mockContent);
        when(mockContent.getSession()).thenReturn(mockSession);
        when(mockContent.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.getParent()).thenReturn(mockParentNode);
        when(mockContent.getNode(FEDORA_DESCRIPTION)).thenReturn(mockDescNode);
        when(mockDescNode.setProperty(anyString(), any(Binary.class))).thenReturn(mockProperty);

        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:versionedFile");
        when(mockContent.getPrimaryNodeType()).thenReturn(mockNodeType);

        testObj = new UrlBinary(mockContent);
    }

    @Test
    public void testSetContent() throws Exception {
        mockProxyProperty();
        testObj.setContent(mockStream, mimeType, null, null, null);

        verify(mockDescNode).setProperty(HAS_MIME_TYPE, mimeType);
    }

    @Test
    public void testSetContentWithFilename() throws Exception {
        mockProxyProperty();
        final String fileName = "content.txt";
        testObj.setContent(mockStream, mimeType, null, fileName, null);

        verify(mockDescNode).setProperty(HAS_MIME_TYPE, mimeType);
        verify(mockDescNode).setProperty(FILENAME, fileName);
    }

    @Test
    public void testSetContentWithChecksum() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        mockProxyProperty();

        testObj.setContent(mockStream, mimeType, singleton(
                new URI(checksum)), null, null);
    }

    @Test(expected = InvalidChecksumException.class)
    public void testSetContentWithChecksumMismatch() throws Exception {
        mockProxyProperty();
        testObj.setContent(mockStream, mimeType, singleton(new URI("urn:sha1:xyz")), null, null);
    }

    @Test
    public void getContentSize() throws Exception {
        mockProxyProperty();
        testObj.setContent(mockStream, mimeType, null, null, null);

        final long contentSize = testObj.getContentSize();
        assertEquals(EXPECTED_CONTENT.length(), contentSize);
    }

    @Test
    public void testGetProxyURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockProxyProperty();

        final String url = testObj.getProxyURL();
        assertEquals(fileUrl, url);
    }

    @Test
    public void testSetProxyURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockProxyProperty();

        testObj.setProxyURL(fileUrl);
        verify(mockDescNode).setProperty(PROXY_FOR, fileUrl);

        assertEquals(fileUrl, testObj.getProxyURL());
    }

    @Test
    public void testGetRedirectURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockRedirectProperty();

        final String url = testObj.getRedirectURL();
        assertEquals(fileUrl, url);
    }

    @Test
    public void testSetRedirectURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockRedirectProperty();

        testObj.setRedirectURL(fileUrl);
        verify(mockDescNode).setProperty(REDIRECTS_TO, fileUrl);

        assertEquals(fileUrl, testObj.getRedirectURL());
    }

    @Test
    public void testGetContentDigest() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        mockProxyProperty();
        mockChecksumProperty(checksum);

        testObj.setContent(mockStream, mimeType, singleton(
                new URI(checksum)), null, null);

        final URI digestUri = testObj.getContentDigest();
        assertEquals(checksum, digestUri.toString());
    }

    @Test
    public void testGetMimeType() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);

        final String mimeType = testObj.getMimeType();
        assertEquals(mimeType, mimeType);
    }

    private void mockProxyProperty() {
        try {
            when(proxyURLProperty.getString()).thenReturn(fileUrl);
            when(proxyURLProperty.getValue()).thenReturn(mockURIValue);
            when(proxyURLProperty.getName()).thenReturn(PROXY_FOR.toString());
            when(mockURIValue.toString()).thenReturn(fileUrl);

            when(mockDescNode.hasProperty(PROXY_FOR)).thenReturn(true);
            when(mockDescNode.getProperty(PROXY_FOR)).thenReturn(proxyURLProperty);
        } catch (final RepositoryException e) {
            // This catch left intentionally blank.
        }
    }

    private void mockRedirectProperty() {
        try {
            when(redirectURLProperty.getString()).thenReturn(fileUrl);
            when(redirectURLProperty.getValue()).thenReturn(mockURIValue);
            when(redirectURLProperty.getName()).thenReturn(PROXY_FOR.toString());
            when(mockDescNode.hasProperty(REDIRECTS_TO)).thenReturn(true);
            when(mockDescNode.getProperty(REDIRECTS_TO)).thenReturn(redirectURLProperty);
        } catch (final RepositoryException e) {
            // This catch left intentionally blank.
        }

    }

    private void mockChecksumProperty(final String checksum) throws Exception {
        when(mockDescNode.hasProperty(CONTENT_DIGEST)).thenReturn(true);
        final Property checksumProperty = mock(Property.class);
        final Value checksumValue = mock(Value.class);
        when(checksumValue.getString()).thenReturn(checksum);
        when(checksumProperty.getString()).thenReturn(checksum);
        when(checksumProperty.getValue()).thenReturn(checksumValue);
        when(mockDescNode.getProperty(CONTENT_DIGEST)).thenReturn(checksumProperty);
    }
}
