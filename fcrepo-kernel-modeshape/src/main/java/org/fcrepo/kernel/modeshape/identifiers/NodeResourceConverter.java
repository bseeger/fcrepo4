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
package org.fcrepo.kernel.modeshape.identifiers;

import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;

import com.google.common.base.Converter;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.FedoraTimeMapImpl;
import org.fcrepo.kernel.modeshape.FedoraWebacAclImpl;
import org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.ContainerImpl;
import org.fcrepo.kernel.modeshape.TombstoneImpl;
import org.fcrepo.kernel.modeshape.services.FedoraBinaryFactory;

import javax.jcr.Node;

/**
 * @author cabeer
 * @since 10/15/14
 */
public class NodeResourceConverter extends Converter<Node, FedoraResource> {
    public static final NodeResourceConverter nodeConverter = new NodeResourceConverter();

    /**
     * Get a converter that can transform a Node to a Resource
     * @param c the given node
     * @return the converter that can transform a node to resource
     */
    public static Converter<Node, Resource> nodeToResource(final Converter<Resource, FedoraResource> c) {
        return nodeConverter.andThen(c.reverse());
    }

    @Override
    protected FedoraResource doForward(final Node node) {

        final FedoraResource fedoraResource;

        if (NonRdfSourceDescriptionImpl.hasMixin(node)) {
            fedoraResource = new NonRdfSourceDescriptionImpl(node);
        } else if (FedoraBinaryImpl.hasMixin(node)) {
            return FedoraBinaryFactory.getBinary(node);
        } else if (TombstoneImpl.hasMixin(node)) {
            fedoraResource = new TombstoneImpl(node);
        } else if (FedoraTimeMapImpl.hasMixin(node)) {
            fedoraResource = new FedoraTimeMapImpl(node);
        } else if (FedoraWebacAclImpl.hasMixin(node)) {
            fedoraResource = new FedoraWebacAclImpl(node);
        } else {
            fedoraResource = new ContainerImpl(node);
        }

        return fedoraResource;
    }

    @Override
    protected Node doBackward(final FedoraResource resource) {
        return getJcrNode(resource);
    }
}
