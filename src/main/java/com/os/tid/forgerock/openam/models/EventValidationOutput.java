package com.os.tid.forgerock.openam.models;

public class EventValidationOutput {
      public String irmResponse;
      public String message;
      public String requestID;
      public String retcode;
      public String sessionStatus;
      public String command;
      public String requestMessage;

    public EventValidationOutput() {
    }

    public EventValidationOutput(String irmResponse, String message, String requestID, String retcode, String sessionStatus, String command, String requestMessage) {
        this.irmResponse = irmResponse;
        this.message = message;
        this.requestID = requestID;
        this.retcode = retcode;
        this.sessionStatus = sessionStatus;
        this.command = command;
        this.requestMessage = requestMessage;
    }

    public String getIrmResponse() {
        return irmResponse;
    }

    public void setIrmResponse(String irmResponse) {
        this.irmResponse = irmResponse;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getRetcode() {
        return retcode;
    }

    public void setRetcode(String retcode) {
        this.retcode = retcode;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }
}
