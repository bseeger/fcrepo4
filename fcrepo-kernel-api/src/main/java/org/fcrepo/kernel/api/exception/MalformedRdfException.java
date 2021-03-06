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
package org.fcrepo.kernel.api.exception;

/**
 * Indicates that RDF was presented for persistence to the repository, but could not be persisted for some reportable
 * reason.
 *
 * @author ajs6f
 * @author whikloj
 * @since Oct 24, 2013
 */
public class MalformedRdfException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public MalformedRdfException(final String msg) {
        super(msg);
    }


    /**
     * Ordinary constructor.
     *
     * @param rootCause the root cause
     */
    public MalformedRdfException(final Throwable rootCause) {
        super(rootCause);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public MalformedRdfException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

}
