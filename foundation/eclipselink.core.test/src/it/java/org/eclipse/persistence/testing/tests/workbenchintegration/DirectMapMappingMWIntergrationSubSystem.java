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
package org.eclipse.persistence.testing.tests.workbenchintegration;

/**
 *  This test system uses the DirectMapMapping test system to test the
 *  integration between the Mapping Workbench and the Foundation Library. To do
 *  this, it writes our test project to a project class file and then compile
 *  and instantiate the project class and runs the DirectMapMapping tests on it.
 */
public class DirectMapMappingMWIntergrationSubSystem extends DirectMapMappingMWIntergrationSystem {
    @Override
    protected void buildProject() {
        project = WorkbenchIntegrationSystemHelper.buildProjectClass(project, PROJECT_FILE);
    }
}
