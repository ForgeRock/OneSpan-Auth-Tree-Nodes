package com.os.tid.forgerock.openam.models;

public class AddDeviceOutput {
      public String activationMessage2;
      public String activationType;
      public String deviceStatus;
      public String deviceType;
      public String domain;
      public String registrationID;
      public String serialNumber;
      public String userID;

    public AddDeviceOutput() {
    }

    public AddDeviceOutput(String activationMessage2, String activationType, String deviceStatus, String deviceType, String domain, String registrationID, String serialNumber, String userID) {
        this.activationMessage2 = activationMessage2;
        this.activationType = activationType;
        this.deviceStatus = deviceStatus;
        this.deviceType = deviceType;
        this.domain = domain;
        this.registrationID = registrationID;
        this.serialNumber = serialNumber;
        this.userID = userID;
    }

    public String getActivationMessage2() {
        return activationMessage2;
    }

    public void setActivationMessage2(String activationMessage2) {
        this.activationMessage2 = activationMessage2;
    }

    public String getActivationType() {
        return activationType;
    }

    public void setActivationType(String activationType) {
        this.activationType = activationType;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRegistrationID() {
        return registrationID;
    }

    public void setRegistrationID(String registrationID) {
        this.registrationID = registrationID;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }
}
