/*
 * Copyright (c) 2006, 2021 Oracle and/or its affiliates. All rights reserved.
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
//     Oracle - initial API and implementation
//
package org.eclipse.persistence.jpa.jpql.parser;

/**
 * The modulo operation finds the remainder of division of one number by another.
 * <br>
 * It takes two integer arguments and returns an integer.
 * <br>
 * JPA 1.0, 2.0:
 * <div><b>BNF:</b> <code>expression ::= MOD(simple_arithmetic_expression, simple_arithmetic_expression)</code></div>
 * <br>
 * JPA 2.1:
 * <div><b>BNF:</b> <code>expression ::= MOD(arithmetic_expression, arithmetic_expression)</code></div>
 *
 * @version 2.5
 * @since 2.3
 * @author Pascal Filion
 */
public final class ModExpression extends AbstractDoubleEncapsulatedExpression {

    /**
     * Creates a new <code>ModExpression</code>.
     *
     * @param parent The parent of this expression
     */
    public ModExpression(AbstractExpression parent) {
        super(parent, MOD);
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public JPQLQueryBNF getQueryBNF() {
        return getQueryBNF(FunctionsReturningNumericsBNF.ID);
    }

    @Override
    public String parameterExpressionBNF(int index) {
        return InternalModExpressionBNF.ID;
    }
}
