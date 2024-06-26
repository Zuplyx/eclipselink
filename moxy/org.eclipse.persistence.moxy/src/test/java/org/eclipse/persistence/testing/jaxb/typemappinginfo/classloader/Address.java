/*
 * Copyright (c) 1998, 2024 Oracle and/or its affiliates. All rights reserved.
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
//     bdoughan - February 5/2010 - 2.0.1 - Initial implementation
package org.eclipse.persistence.testing.jaxb.typemappinginfo.classloader;

public class Address {

    public String street;
    public String city;

    public boolean equals(Object theObject){
        if(theObject instanceof Address addr){
            if(!street.equals(addr.street)){
               return false;
           }
           if(!city.equals(addr.city)){
               return false;
           }
           return true;
        }else{
           return false;
        }
    }

}
