/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */
package com.os.tid.forgerock.openam.nodes;

import com.sun.identity.sm.RequiredValueValidator;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.Config;

/**
 * Common Configurations for the OneSpan IAA Auth Nodes.
 */
@Config(scope = Config.Scope.REALM)
public interface OSTIDConfigurationsService {

    @Attribute(order = 100, validators = RequiredValueValidator.class)
    String tenantName();

    @Attribute(order = 200, validators = RequiredValueValidator.class)
    EnvOptions environment();

    @Attribute(order = 300, validators = RequiredValueValidator.class)
    String applicationRef();

    default String tenantNameToLowerCase() {return tenantName().toLowerCase();}

    enum EnvOptions {
        sdb, prod
    }

}

