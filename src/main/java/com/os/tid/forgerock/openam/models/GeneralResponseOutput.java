package com.os.tid.forgerock.openam.models;

public class GeneralResponseOutput {
      public String challenge;
      public String requestID;
      public int riskResponseCode = -1;
      public int eventId;
      public String sessionStatus;
      public String requestMessage;
      public String fidoAuthenticationRequest;
      public int uafStatusCode;

    public GeneralResponseOutput() {
    }

    public GeneralResponseOutput(String challenge, String requestID, int riskResponseCode, String sessionStatus, String requestMessage, int eventId) {
        this.challenge = challenge;
        this.requestID = requestID;
        this.riskResponseCode = riskResponseCode;
        this.sessionStatus = sessionStatus;
        this.requestMessage = requestMessage;
        this.eventId = eventId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public int getRiskResponseCode() {
        return riskResponseCode;
    }

    public int getEventId() {
        return eventId;
    }

    public void setRiskResponseCode(int riskResponseCode) {
        this.riskResponseCode = riskResponseCode;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getRequestMessage() {
        return requestMessage;
    }



    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }

    public String getFidoAuthenticationRequest() {
        return fidoAuthenticationRequest;
    }

    public void setFidoAuthenticationRequest(String fidoAuthenticationRequest) {
        this.fidoAuthenticationRequest = fidoAuthenticationRequest;
    }

    public int getUafStatusCode() {
        return uafStatusCode;
    }

    public void setUafStatusCode(int uafStatusCode) {
        this.uafStatusCode = uafStatusCode;
    }
}
