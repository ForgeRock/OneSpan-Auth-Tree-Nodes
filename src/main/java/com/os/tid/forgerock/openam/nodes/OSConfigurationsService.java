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

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.Config;

/**
 * Common Configurations for the OneSpan Auth Tree Nodes.
 */
@Config(scope = Config.Scope.REALM)
public interface OSConfigurationsService {

    @Attribute(order = 1)
    default String tenantName(){ return ""; };

    @Attribute(order = 2)
    EnvOptions environment();

    @Attribute(order = 3)
    default String customUrl() { return ""; };

    @Attribute(order = 4)
    default String applicationRef(){ return ""; };

    @Attribute(order = 5)
    default String publicKey(){ return ""; };

    @Attribute(order = 6)
    default String privateKey(){ return ""; };

    
    public enum EnvOptions {
        sdb,
        prod,
        Sandbox,
        Production_NA1,
        Production_EU1,
        Production_EU2,
        Staging_NA1,
        Staging_EU1,
        Staging_EU2,
        UAT_EU1,
    	CUSTOMIZED;
    }
    
}

