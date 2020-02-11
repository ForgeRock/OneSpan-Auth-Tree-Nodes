<!--
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
 * Copyright 2019 ForgeRock AS.
-->
# OneSpan IAA Auth Tree Nodes

OneSpan Intelligent Adaptive Authentication (IAA) secures your web and mobile applications by analyzing vast and disparate data acquired through user actions and events. Based on this analysis, OneSpan Adaptive Authentication dynamically assesses which authentication and/or transaction security measures are appropriate for each unique end user.


**About OneSpan IAA**

OneSpan Adaptive Authentication provides hosted solutions to test and build web and mobile applications for login and transaction signing flows.
Integration with OneSpan Adaptive Authentication is incredibly simple and extensible, as it will support future authentication technologies without the need to change anything in your integration code.
OneSpan intelligent Adaptive Authentication uses a 'trusted device' (e.g. a mobile phone using the OneSpan [Mobile Security Suite SDKs](http://community.onespan.com/products/mobile-security-suite/sdks) to provide strong multi-factor authentication whenever the risk associated with an action is high.
OneSpan Adaptive Authentication evaluates the risk related to an end-user request through vast data collected from the devices which is then scored with a sophisticated machine-learning engine. Depending on the risk, OneSpan Adaptive Authentication can dynamically adjust the end-user security requirements by requesting step-up authentication for higher risk transactions using various configurations of device-based, PIN-based, fingerprint-based, or face recognition-based authentication as needed to fully secure transactions.

![ScreenShot](./doc/images/Adaptive%20Authentication%20Overview.png)

**Installation**

Download the current release [here](https://github.com/ForgeRock/OneSpan-Auth-Tree-Nodes/releases).

Copy the jar file to the "../web-container/webapps/openam/WEB-INF/lib" folder where AM is deployed, then restart the AM. The nodes will be available in the tree designer.

You'll find the complete guide [here](./doc/OneSpan%20IAA%20Auth%20Tree%20Nodes%20Guide.pdf).  

**Before You Begin**

1. Create an OneSpan [Developer Community account](https://community.onespan.com/user/registration).

2. Once logged in the Developer Community portal, you'll be able to create an OneSpan [IAA Sandbox account](https://community.onespan.com/tid-sandbox-registration).
 
3. Set up a mobile application integrated with the [Mobile Security Suite](http://community.onespan.com/documentation/mobile-security-suite). As an easy start up, you can install the OneSpan IAA [Demo App](https://sdb.tid.onespan.cloud/devportal/InstallingVAASDemoApp) on your phone. 

4. Configure the [Intelligent Risk Management](https://sdb.tid.onespan.cloud/irm) (IRM) service. 


