package com.prac.kurento.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prac.kurento.dto.ErrorCallDto;
import com.prac.kurento.vo.InfoCall;
import com.prac.kurento.vo.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@RequiredArgsConstructor
@Controller
@Slf4j
public class CallController {

    @Autowired
    private KurentoClient kurento;
    private MediaPipeline pipeline;
    private UserInfo presenterUser;

    private final SimpMessageSendingOperations so;

    private static final Gson gson = new GsonBuilder().create();

    @MessageMapping("videoStream")
    @SendTo("/call/video")
    public void videoStream(String message) throws IOException {
        log.info(message);
        JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);

        switch (jsonMessage.get("id").getAsString()) {
            case "presenter":
                try {
                    presenter(jsonMessage);
                } catch (Throwable t) {
                    ErrorResponse(t, "presenterResponse");
                }
                break;
            case "viewer":
                try {
                    viewer(jsonMessage);
                } catch (Throwable t) {
                    ErrorResponse(t, "viewerResponse");
                }
                break;
            case "onIceCandidate": {
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

                UserInfo user = null;
                if (user != null) {
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                            candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(cand);
                }
                break;
            }
            case "stop":
                stop();
                break;
            default:
                break;
        }
    }

    private synchronized void presenter(JsonObject jsonMessage) throws IOException {
        if (presenterUser == null) {
            //presenterUser = new UserInfo(jsonMessage);

            pipeline = kurento.createMediaPipeline();
            presenterUser.setWebRtcEndpoint(new WebRtcEndpoint.Builder(pipeline).build());

            WebRtcEndpoint presenterWebRtc = presenterUser.getWebRtcEndpoint();

            presenterWebRtc.addIceCandidateFoundListener(event -> {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                try {
                    synchronized (so) {
                        so.convertAndSend(response);
                    }
                } catch (Exception e) {
                    log.debug(e.getMessage());
                }
            });

            String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
            String sdpAnswer = presenterWebRtc.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "presenterResponse");
            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (so) {
                so.convertAndSend(response);
            }
            presenterWebRtc.gatherCandidates();

        } else {
            JsonObject response = new JsonObject();
            response.addProperty("id", "presenterResponse");
            response.addProperty("response", "rejected");
            response.addProperty("message", "Another user is currently acting as sender. Try again later ...");
            so.convertAndSend(response);
        }
    }

    private synchronized void viewer(JsonObject jsonMessage) throws IOException {
        if (presenterUser == null || presenterUser.getWebRtcEndpoint() == null) {
            JsonObject response = new JsonObject();
            response.addProperty("id", "viewerResponse");
            response.addProperty("response", "rejected");
            response.addProperty("message", "No active sender now. Become sender or . Try again later ...");
            so.convertAndSend(response);
        } else {
            UserInfo viewer = new UserInfo("test");
            String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
            WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(pipeline).build();
            nextWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (so) {
                            so.convertAndSend(response);
                        }
                    } catch (Exception e) {
                        log.debug(e.getMessage());
                    }
                }
            });

            viewer.setWebRtcEndpoint(nextWebRtc);
            presenterUser.getWebRtcEndpoint().connect(nextWebRtc);
            String sdpAnswer = nextWebRtc.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "viewerResponse");
            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (so) {
                so.convertAndSend(response);
            }
            nextWebRtc.gatherCandidates();
        }
    }


    private void ErrorResponse(Throwable t, String responseId) throws IOException {
            log.info(t.getMessage(), t);
            ErrorCallDto err = new ErrorCallDto(responseId, "rejected", t.getMessage());
            so.convertAndSend(err);
    }

    private synchronized void stop() throws IOException {

    }
}
