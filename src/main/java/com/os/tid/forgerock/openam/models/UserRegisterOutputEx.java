package com.os.tid.forgerock.openam.models;

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
