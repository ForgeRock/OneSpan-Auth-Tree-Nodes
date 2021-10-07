package com.os.tid.forgerock.openam.models;

public class GenerateChallengeOutput {
      public String challenge;
      public String requestID;

    public GenerateChallengeOutput() {
    }

    public GenerateChallengeOutput(String challenge, String requestID) {
        this.challenge = challenge;
        this.requestID = requestID;
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
}
