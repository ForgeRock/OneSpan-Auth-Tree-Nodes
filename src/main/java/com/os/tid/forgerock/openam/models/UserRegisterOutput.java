package com.os.tid.forgerock.openam.models;

public class UserRegisterOutput {
      public String activationPassword;
      public String digipassSerial;
      public String irmResponse;
      public String message;
      public String retcode;

    public UserRegisterOutput() {
    }

    public UserRegisterOutput(String activationPassword, String digipassSerial, String irmResponse, String message, String retcode) {
        this.activationPassword = activationPassword;
        this.digipassSerial = digipassSerial;
        this.irmResponse = irmResponse;
        this.message = message;
        this.retcode = retcode;
    }

    public void setActivationPassword(String activationPassword) {
        this.activationPassword = activationPassword;
    }

    public void setDigipassSerial(String digipassSerial) {
        this.digipassSerial = digipassSerial;
    }

    public void setIrmResponse(String irmResponse) {
        this.irmResponse = irmResponse;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRetcode(String retcode) {
        this.retcode = retcode;
    }

    public String getActivationPassword() {
        return activationPassword;
    }

    public String getDigipassSerial() {
        return digipassSerial;
    }

    public String getIrmResponse() {
        return irmResponse;
    }

    public String getMessage() {
        return message;
    }

    public String getRetcode() {
        return retcode;
    }
}
