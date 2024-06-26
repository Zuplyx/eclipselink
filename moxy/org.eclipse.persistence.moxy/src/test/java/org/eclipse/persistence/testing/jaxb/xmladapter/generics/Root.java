/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
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
// Matt MacIvor - July 4th 2011
package org.eclipse.persistence.testing.jaxb.xmladapter.generics;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement
public class Root {

    @XmlJavaTypeAdapter(value = AAdapter.class)
    public A<B> a;

    public boolean equals(Object obj) {
        if(!(obj instanceof Root)) {
            return false;
        }
        return a.equals(((Root)obj).a);
    }

}
