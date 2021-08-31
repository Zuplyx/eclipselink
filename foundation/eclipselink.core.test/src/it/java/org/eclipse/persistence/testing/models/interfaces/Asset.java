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
package org.eclipse.persistence.testing.models.interfaces;

public class Asset {

    public CompanyAsset asset;

    public Asset() {
        super();
    }

    @Override
    public Object clone() {
        Asset object = new Asset();
        object.asset = (CompanyAsset)this.asset.clone();
        return object;
    }

    public CompanyAsset getAsset() {
        return this.asset;
    }

    public java.math.BigDecimal getSerNum() {
        return null;
    }

    public void setAsset(CompanyAsset ca) {
        this.asset = ca;
    }
}
