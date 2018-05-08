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

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;
import org.slf4j.Logger;

/**
 * External binary from a local file
 *
 * @author bbpennel
 * @since 11/29/2017
 */
public class LocalFileBinary extends UrlBinary {
    private static final Logger LOGGER = getLogger(LocalFileBinary.class);

    /**
     * Constructs a LocalFileBinaryImpl
     *
     * @param node node
     */
    public LocalFileBinary(final Node node) {
        super(node);
    }

    /*
     * (non-Javadoc)
      mlol* @see org.fcrepo.kernel.modeshape.FedoraBinaryImpl#setContent(java.io.InputStream, java.lang.String,
     * java.util.Collection, java.lang.String, org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint)
     */
    @Override
    public void setContent(final InputStream content, final String contentType,
                           final Collection<URI> checksums, final String originalFileName,
                           final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {

        try {
            /* that this is a proxy or redirect has already been set before we get here
             */
            final Node descNode = getDescriptionNode();
            final Node contentNode = getNode();

            LOGGER.debug("HERE HERE HERE HERE HERE");
            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }

            if (originalFileName != null) {
                descNode.setProperty(FILENAME, originalFileName);
            }

            if (contentType == null) {
                descNode.setProperty(HAS_MIME_TYPE, DEFAULT_MIME_TYPE);
            } else {
                descNode.setProperty(HAS_MIME_TYPE, contentType);
            }

            // Store the required jcr:data property
            contentNode.setProperty(JCR_DATA, "");

            LOGGER.debug("Created content node at path: {}", contentNode.getPath());

            // Ensure provided checksums are valid
            final Collection<URI> nonNullChecksums = (null == checksums) ? new HashSet<>() : checksums;
            verifyChecksums(nonNullChecksums);

            decorateContentNode(contentNode, descNode, nonNullChecksums, getContentSize());
            FedoraTypesUtils.touch(contentNode);
            FedoraTypesUtils.touch(descNode);

            LOGGER.debug("Set local file content at path: {}", getResourceLocation());

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private static void decorateContentNode(final Node dsNode, final Node descNode, final Collection<URI> checksums,
        final long size)
        throws RepositoryException {
        if (dsNode == null) {
            LOGGER.warn("{} node appears to be null!", JCR_CONTENT);
            return;
        }

        if (dsNode.hasProperty(JCR_DATA)) {
            // Store checksums on node
            final String[] checksumArray = new String[checksums.size()];
            checksums.stream().map(Object::toString).collect(Collectors.toSet()).toArray(checksumArray);
            descNode.setProperty(CONTENT_DIGEST, checksumArray);

            // Store the size of the file
            contentSizeHistogram.update(size);
            descNode.setProperty(CONTENT_SIZE, size);

            LOGGER.debug("Decorated local file data property at path: {}", dsNode.getPath());
        }
    }

    @Override
    public String getMimeType() {
        return getMimeTypeValue();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.modeshape.FedoraBinaryImpl#getContentSize()
     */
    @Override
    public long getContentSize() {
        final long sizeValue = super.getContentSize();
        if (sizeValue > -1L) {
            return sizeValue;
        }
        final File file = new File(getResourceUri().getPath());
        setContentSize(file.length());
        return file.length();
    }
}
