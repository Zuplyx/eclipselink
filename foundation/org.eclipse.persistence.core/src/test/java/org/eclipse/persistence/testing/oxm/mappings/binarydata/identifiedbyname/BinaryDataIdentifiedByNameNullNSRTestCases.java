/*
 * Copyright (c) 1998, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
package org.eclipse.persistence.testing.oxm.mappings.binarydata.identifiedbyname;

import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.testing.oxm.mappings.XMLWithJSONMappingTestCases;
import org.eclipse.persistence.testing.oxm.mappings.binarydata.Employee;
import org.eclipse.persistence.testing.oxm.mappings.binarydatacollection.MyAttachmentMarshaller;
import org.eclipse.persistence.testing.oxm.mappings.binarydatacollection.MyAttachmentUnmarshaller;

/**
 * Not supported
 * base64Binary as attribute - no singlenode\xmlattribute
 */
public class BinaryDataIdentifiedByNameNullNSRTestCases extends XMLWithJSONMappingTestCases {
    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/oxm/mappings/binarydata/identifiedbyname/BinaryDataIdentifiedByNameNullNSR.xml";
    private final static String JSON_RESOURCE = "org/eclipse/persistence/testing/oxm/mappings/binarydata/identifiedbyname/BinaryDataIdentifiedByNameNullNSR.json";

    public BinaryDataIdentifiedByNameNullNSRTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);
        setControlJSON(JSON_RESOURCE);
        NamespaceResolver namespaceResolver = null;//new NamespaceResolver();

        setProject(new BinaryDataIdentifiedByNameProject(namespaceResolver));
    }

    @Override
    protected Object getControlObject() {
        return Employee.example1();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MyAttachmentUnmarshaller attachmentUnmarshaller = new MyAttachmentUnmarshaller();
        byte[] bytes = MyAttachmentUnmarshaller.PHOTO_BASE64.getBytes();

        attachmentUnmarshaller.attachments.put(MyAttachmentUnmarshaller.ATTACHMENT_TEST_ID, bytes);
        xmlUnmarshaller.setAttachmentUnmarshaller(attachmentUnmarshaller);
    }

    @Override
    protected XMLMarshaller createMarshaller() {
        XMLMarshaller marshaller = super.createMarshaller();
        marshaller.setAttachmentMarshaller(new MyAttachmentMarshaller());
        return marshaller;
    }

}
