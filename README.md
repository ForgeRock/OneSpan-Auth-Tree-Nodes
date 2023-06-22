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
# OneSpan Auth Tree Nodes

OneSpan [Intelligent Adaptive Authentication (IAA)](https://www.onespan.com/products/intelligent-adaptive-authentication) helps drive down fraud, improve customer experience, and meet compliance requirements by combining our powerful [OneSpan Cloud Authentication](https://www.onespan.com/products/onespan-cloud-authentication) (OCA) and [Risk Analytics](https://www.onespan.com/products/risk-analytics) (RA) service. 
By analyzing vast and disparate data acquired from user actions, device integrity, and transaction data in real time, the end-user can be dynamically presented with the appropriate authentication level for the current session or transaction.

Watch this video to learn [how OneSpan intelligent adaptive authentication works](https://www.onespan.com/partners/forgerock).

OneSpan Auth Tree Nodes work in tandem with ForgeRock [Access Management (AM) 7.3](https://backstage.forgerock.com/docs/am/7.3) and ForgeRock [Identity Cloud](https://backstage.forgerock.com/docs/idcloud/latest/overview.html).

## About OneSpan IAA

OneSpan Adaptive Authentication provides hosted solutions to test and build web and mobile applications for login and transaction signing flows.

Integration with OneSpan Adaptive Authentication is incredibly simple and extensible, as it will support future authentication technologies without the need to change anything in your integration code.

OneSpan intelligent Adaptive Authentication uses a 'trusted device' (e.g. a mobile phone using the OneSpan [Mobile Security Suite SDKs](http://community.onespan.com/products/mobile-security-suite/sdks) to provide strong multi-factor authentication whenever the risk associated with an action is high.

OneSpan Adaptive Authentication evaluates the risk related to an end-user request through vast data collected from the devices which is then scored with a sophisticated machine-learning engine. Depending on the risk, OneSpan Adaptive Authentication can dynamically adjust the end-user security requirements by requesting step-up authentication for higher risk transactions using various configurations of device-based, PIN-based, fingerprint-based, or face recognition-based authentication as needed to fully secure transactions.

![ScreenShot](./doc/images/IAA%20Overview.png)

## About OneSpan OCA

As important components of Adaptive Authentication, OneSpan Cloud Authentication (OCA) and Risk Analytics (RA) can also be leveraged as stand-alone services. 

With OCA, OneSpan offers a comprehensive solution for strong authentication in the Cloud, integrating Push Notification and Secure Channel operations.

OneSpan Auth Tree Nodes facilitates developers to integrate with below OCA use cases:

**User Authentication**
* With static password
* With offline one-time password (OTP)
* Secure Channel-based
* Push Notification-based
* FIDO-based

**Transaction Data Signing**
* Offline signing
* Secure Channel-based signing
* Push Notification-based signing
* FIDO-based signing (UAF only)

![ScreenShot](./doc/images/OCA%20Overview.png)

## About OneSpan RA

OneSpan Risk Analytics (RA) leverages the latest machine learning and sophisticated data modeling. It analyzes data in real time to produce a transaction risk score. The risk score can then drive intelligent workflows that trigger immediate action based on pre-defined and/or customer-defined security policies and rules.
 
 ![ScreenShot](./doc/images/OSRA%20Overview.png)
 
The combination of intelligent automation and risk scoring streamlines processes and helps prevent account takeover, new account fraud, and mobile fraud.

In below sections, we will showcase you how to configure the [Risk Analytics Presentation Service](https://sdb.tid.onespan.cloud/riskui/Login) where you can monitor and design rules, factors & actions to prevent various types of fraud attack and to manage risks for corporate banking applications across multi-channels.

## Installation

Download the current release [here](https://github.com/ForgeRock/OneSpan-Auth-Tree-Nodes/releases).

Copy the jar file to the "../web-container/webapps/openam/WEB-INF/lib" folder where AM is deployed, then restart the AM. The nodes will be available in the tree designer.

## Before You Begin

Below sections only give you a brief introduction to get started. For more detailed descriptions, refer to the [completed guide](./doc/OneSpan%20Auth%20Tree%20Nodes%20Guide%20-%20v1.2.12.pdf).

**For OneSpan IAA & RA Users:**

1. Create an OneSpan [Developer Community account](https://community.onespan.com/user/registration).

2. Once logged in the community portal, you'll be able to create an OneSpan [IAA Sandbox account](https://community.onespan.com/tid-sandbox-registration).
 
3. Set up a mobile application integrated with the [Mobile Security Suite](http://community.onespan.com/documentation/mobile-security-suite). As an easy start up, you can install the OneSpan IAA [Demo App](https://gs.onespan.cloud/downloads) on your phone. 

4. Configure the [Risk Analytics Presentation Service](https://sdb.tid.onespan.cloud/riskui/Login) service. 

**For OneSpan OCA Users:**

1. Create an OneSpan [Developer Community account](https://community.onespan.com/user/registration).

2. Once logged in the community portal, you'll be able to create an OneSpan [IAA Sandbox account](https://community.onespan.com/tid-sandbox-registration).

3. Install a mobile application which acts as the Digipass authenticator. You can follow either of below two options:
* To customize and build your own [Mobile Authenticator Studio](https://community.onespan.com/documentation/onespan-cloud-authentication/guides/integration-guide/developer/configuring-mobile-authenticator-studio) (MAS) app. As a quick start, you can download a sample MAS app through our [demo site](https://gs.onespan.cloud/te-oca-sample/). 
* Download “OneSpan Mobile Authenticator” from [Google Play](https://play.google.com/store/apps/details?id=com.vasco.digipass.es&hl=en_US&gl=US) or [Apple Store](https://apps.apple.com/us/app/onespan-mobile-authenticator/id1172835583#?platform=iphone).


## Nodes Overview

The OneSpan Auth Tree Nodes contains 1 Auxiliary Service, 16 nodes, and 4 demo nodes which will only be used for testing purpose. 

![ScreenShot](./doc/images/Node%20Overview.png)

## Auxiliary Service

The node provides a realm-specific service named "OneSpan Configuration", where allows you to specify the OneSpan IAA common configurations.

![ScreenShot](./doc/images/Global%20Config.png)


## Quick Start

Below sample trees help you to address the most common use cases. Before start, make sure you've followed below steps:

(1) Add the "OneSpan Configuration" service.

(2) Reproduce below sample trees using either of below two methods: 

-Manually create a new tree following the design and remain all the settings default. 

-Import the JSON files under the "/sample" folder.
(3) If you are testing with IAA Sandbox environment, your IAA domain is the same as your tenant name, update it in corresponding journeys and nodes.


**1. OneSpan IAA User Register**

![ScreenShot](./doc/images/IAA%20User%20Register.png)

**2. OneSpan IAA User Login**

![ScreenShot](./doc/images/IAA%20User%20Login.png)

**3. OneSpan IAA Validate Transaction Event**

![ScreenShot](./doc/images/IAA%20Validate%20Transaction.png)

**4. OneSpan IAA Validate Non-Monetary Event**

![ScreenShot](./doc/images/IAA%20Validate%20Event.png)

**5. OneSpan IAA VOTP User Register**

![ScreenShot](./doc/images/IAA%20VDP%20User%20Register.png)

**6. OneSpan IAA VOTP User Login**

![ScreenShot](./doc/images/IAA%20VDP%20User%20Login.png)

**7. OneSpan OCA Offline User Registration and Digipass Activation**

![ScreenShot](./doc/images/OCA%20User%20Register.png)

**8. OneSpan OCA User Login with one-time password (OTP)**

![ScreenShot](./doc/images/OCA%20User%20Login%20with%20OTP.png)

**9. OneSpan OCA User Login with Challenge/Response (CR)**

![ScreenShot](./doc/images/OCA%20User%20Login%20with%20CR.png)

**10. OneSpan OCA Offline Transaction Data Signing**

![ScreenShot](./doc/images/OCA%20Validate%20Transaction.png)

**11. OneSpan RA Insert Transaction**

![ScreenShot](./doc/images/OSRA%20Insert%20Transaction.png)



## Using Authentication

In this section, we will use the OneSpan IAA user register authentication for example and showcase you how the authentication nodes works in action.

To start off the authentication process, hit below link in your browser:

*https://{your_instance_url}/am/XUI/?realm=alpha&authIndexType=service&authIndexValue=OneSpan-Adapative-Authentication-User-Register-Sample-Tree*

You will be prompt to input the username and password. (Password should include at least one lowercase, one uppercase, one number, 8 digits in length, and doesn't include part of the username for any 3 characters)
 
![ScreenShot](./doc/images/IAAUserRegister1.png) 
 
Once the Risk Analytics has accepted the user registration, the IAA service creates a Digipass user account and awaits for a trusted device to activate the license with an activation token, which is rendered as a visual code.
  
![ScreenShot](./doc/images/IAAUserRegister2.png)  
  
Launch the AAS Demo App, click the "SCAN" button and use the camera to scan the above visual code. Once the code was detected, the app will prompt you to enter a 6 digits security pin twice.
After completion the registration process, the demo app will jump to the user page and the browser will be redirected to the success URL.
 
![ScreenShot](./doc/images/IAAUserRegister3.png) 
 
Log onto your IRM system and navigate to SUPERVISE & INVESTIGATE > Latest Events, you will find the user register process has been logged by the system with necessary information.
 
![ScreenShot](./doc/images/IAAUserRegister4.png)




