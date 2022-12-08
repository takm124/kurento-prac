package com.prac.kurento.vo;

import lombok.Data;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class UserInfo {
    private String id;

    public UserInfo(String id) {
        this.id = id;
    }

    private WebRtcEndpoint webRtcEndpoint;

    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }

    public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
        this.webRtcEndpoint = webRtcEndpoint;
    }

    public void addCandidate(IceCandidate candidate) {
        webRtcEndpoint.addIceCandidate(candidate);
    }
}