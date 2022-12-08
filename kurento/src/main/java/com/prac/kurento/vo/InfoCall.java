package com.prac.kurento.vo;

import lombok.Data;

@Data
public class InfoCall {
    String id;
    String sdpOffer;
    String candidate = "";

    public InfoCall(String id, String sdpOffer) {
        this.id = id;
        this.sdpOffer = sdpOffer;
    }
}
