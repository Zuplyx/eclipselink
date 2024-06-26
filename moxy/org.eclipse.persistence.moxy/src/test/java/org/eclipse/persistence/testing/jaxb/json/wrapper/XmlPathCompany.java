/*
 * Copyright (c) 2013, 2024 Oracle and/or its affiliates. All rights reserved.
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
//     Blaise Doughan - 2.4.2 - initial implementation
package org.eclipse.persistence.testing.jaxb.json.wrapper;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlType;

import org.eclipse.persistence.oxm.annotations.XmlPath;

@XmlType(propOrder={"strings", "employees", "phoneNumbers"})
public class XmlPathCompany {

    @XmlPath("strings/string/text()")
    public List<String> strings = new ArrayList<String>();

    @XmlPath("employees/employee")
    public List<XmlPathEmployee> employees = new ArrayList<XmlPathEmployee>();

    @XmlPath("phone-numbers/phone-number")
    public List<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();

    @Override
    public boolean equals(Object obj) {
        if(null == obj || obj.getClass() != this.getClass()) {
            return false;
        }

        XmlPathCompany test = (XmlPathCompany) obj;
        if(!equals(strings, test.strings)) {
            return false;
        }
        if(!equals(employees, test.employees)) {
            return false;
        }
        if(!equals(phoneNumbers, test.phoneNumbers)) {
            return false;
        }
        return true;
    }

    private boolean equals(List<?> control, List<?> test) {
        if(control == test) {
            return true;
        }
        if(null == control || null == test) {
            return false;
        }
        if(control.size() != test.size()) {
            return false;
        }
        for(int x=0; x<control.size(); x++) {
            if(!control.get(x).equals(test.get(x))) {
                return false;
            }
        }
        return true;
    }

}
