package com.os.tid.forgerock.openam.models;

/**
 * activationPassword*	string
 * minLength: 6
 * maxLength: 4202
 * example: 9h2679Q8
 * Activation password (max 40 chars) in case of onlineMDL activation. First activation message in case of offlineMDL activation.
 *
 * riskResponseCode	integer($int32)
 * example: 0
 * Risk Analytics response code. Standard supported values are: 0 = Accept, 1 = Decline, 2 = Challenge, 3 = ChallengeSMS, 5 = ChallengeDevice2FA, 8 = ChallengeEmail, 11 = ChallengeCronto, 21 = ChallengeNoPIN, 22 = ChallengePIN, 23 = ChallengeFingerprint, 24 = ChallengeFace. Additional values can be configured through the Risk Analytics Presentation Service.
 *
 * serialNumber*	string
 * pattern: ^[A-Z0-9]{3}[0-9]{7}$
 * minLength: 10
 * maxLength: 10
 * example: VDS1234567
 * Serial number of the authenticator used.
 *
 * registrationID	string
 * maxLength: 40
 * example: vjO1THY2s7qtt99YjyHldWWF
 * Registration identifier to continue offline activation.
 */
public class UserRegisterOutputEx {
      public String activationPassword;
      public String riskResponseCode;
      public String serialNumber;
      public String registrationID;

    public UserRegisterOutputEx() {
    }

    public UserRegisterOutputEx(String activationPassword, String riskResponseCode, String serialNumber, String registrationID) {
        this.activationPassword = activationPassword;
        this.riskResponseCode = riskResponseCode;
        this.serialNumber = serialNumber;
        this.registrationID = registrationID;
    }

    public String getActivationPassword() {
        return activationPassword;
    }

    public void setActivationPassword(String activationPassword) {
        this.activationPassword = activationPassword;
    }

    public String getRiskResponseCode() {
        return riskResponseCode;
    }

    public void setRiskResponseCode(String riskResponseCode) {
        this.riskResponseCode = riskResponseCode;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getRegistrationID() {
        return registrationID;
    }

    public void setRegistrationID(String registrationID) {
        this.registrationID = registrationID;
    }
}
