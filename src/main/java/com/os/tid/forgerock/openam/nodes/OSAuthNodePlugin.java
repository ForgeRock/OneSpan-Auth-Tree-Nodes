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

import java.security.AccessController;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.PluginTools;
import org.forgerock.openam.plugins.StartupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Definition of an <a href="https://backstage.forgerock.com/docs/am/6/apidocs/org/forgerock/openam/auth/node/api/AbstractNodeAmPlugin.html">AbstractNodeAmPlugin</a>. 
 * Implementations can use {@code @Inject} setters to get access to APIs 
 * available via Guice dependency injection. For example, if you want to add an SMS service on install, you 
 * can add the following setter:
 * <pre><code>
 * {@code @Inject}
 * public void setPluginTools(PluginTools tools) {
 *     this.tools = tools;
 * }
 * </code></pre>
 * So that you can use the addSmsService api to load your schema XML for example.
 * PluginTools javadoc may be found 
 * <a href="https://backstage.forgerock.com/docs/am/6/apidocs/org/forgerock/openam/plugins/PluginTools.html#addSmsService-java.io.InputStream-">here</a> 
 * <p>
 *     It can be assumed that when running, implementations of this class will be singleton instances.
 * </p>
 * <p>
 *     It should <i>not</i> be expected that the runtime singleton instances will be the instances on which
 *     {@link #onAmUpgrade(String, String)} will be called. Guice-injected properties will also <i>not</i> be populated
 *     during that method call.
 * </p>
 * <p>
 *     Plugins should <i>not</i> use the {@code ShutdownManager}/{@code ShutdownListener} API for handling shutdown, as
 *     the order of calling those listeners is not deterministic. The {@link #onShutdown()} method for all plugins will
 *     be called in the reverse order from the order that {@link #onStartup()} was called, with dependent plugins being
 *     notified after their dependencies for startup, and before them for shutdown.
 * </p>
 * @supported.all.api
 * @since AM 5.5.0
 */
public class OSAuthNodePlugin extends AbstractNodeAmPlugin {
	static private String currentVersion = "1.2.2";
    private final Logger logger = LoggerFactory.getLogger(OSAuthNodePlugin.class);

	private final List<Class<? extends Node>> nodeList = ImmutableList.of(
			//OCA
			OS_Auth_AddDeviceNode.class,
			OS_Auth_ActivateDeviceNode.class,
			OS_Auth_GenerateChallengeNode.class,

			//Adaptive
			OS_Auth_CheckActivationNode.class,
			OS_Auth_CheckSessionStatusNode.class,
			OS_Auth_UserRegisterNode.class,
			OS_Auth_UserLoginNode.class,
			OS_Auth_ValidateTransactionNode.class,
			OS_Auth_ValidateEventNode.class,

			//Risk
			OS_Risk_CDDCNode.class,
			OS_Risk_InsertTransactionNode.class,

			//Util
			OS_Auth_VisualCodeNode.class,
			OS_Auth_VisualCodeStopNode.class,

			//Sample
			OS_Sample_ErrorDisplayNode.class,
			OS_Sample_TransactionCollector.class,
			OS_Sample_StoreCommandNode.class,
			OS_Sample_AttributesCollector.class	
	);

//	private final Class serviceClass = OSConfigurationsService.class;

    /** 
     * Specify the Map of list of node classes that the plugin is providing. These will then be installed and
     *  registered at the appropriate times in plugin lifecycle.
     *
     * @return The list of node classes.
     */
	@Override
	protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
		return Collections.singletonMap(OSAuthNodePlugin.currentVersion, nodeList);
	}

    /** 
     * Handle plugin installation. This method will only be called once, on first AM startup once the plugin
     * is included in the classpath. The {@link #onStartup()} method will be called after this one.
     * 
     * No need to implement this unless your AuthNode has specific requirements on install.
     */
	@Override
	public void onInstall() throws PluginException {
        logger.info("Installing OSConfigurationsService");
		pluginTools.installService(OSConfigurationsService.class);
		super.onInstall();
	}

	/**
	 * Handle plugin startup. This method will be called every time AM starts, after
	 * {@link #onInstall()}, {@link #onAmUpgrade(String, String)} and
	 * {@link #upgrade(String)} have been called (if relevant).
	 *
	 * No need to implement this unless your AuthNode has specific requirements on
	 * startup.
	 *
	 * @param startupType The type of startup that is taking place.
	 */
	@Override
	public void onStartup(StartupType startupType) throws PluginException {
        logger.info("Starting OSConfigurationsService");
		pluginTools.startService(OSConfigurationsService.class);
		super.onStartup(startupType);
	}



	/**
     * This method will be called when the version returned by {@link #getPluginVersion()} is higher than the
     * version already installed. This method will be called before the {@link #onStartup()} method.
     * 
     * No need to implement this untils there are multiple versions of your auth node.
     *
     * @param fromVersion The old version of the plugin that has been installed.
     */
	@Override
	public void upgrade(String fromVersion) throws PluginException {
		try {
		    SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
		    if (fromVersion.equals(PluginTools.DEVELOPMENT_VERSION)) {
		        ServiceManager sm = new ServiceManager(adminToken);
		        if (sm.getServiceNames().contains("OSConfigurationsService")) {
		            sm.removeService("OSConfigurationsService", fromVersion);
		        }
		        pluginTools.installService(OSConfigurationsService.class);
		    }
		} catch(SSOException | SMSException e) {
	    	e.printStackTrace();
	    }
		super.upgrade(fromVersion);

	}

    /** 
     * The plugin version. This must be in semver (semantic version) format.
     *
     * @return The version of the plugin.
     * @see <a href="https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf">Semantic Versioning</a>
     */
	@Override
	public String getPluginVersion() {
		return OSAuthNodePlugin.currentVersion;
	}
}
