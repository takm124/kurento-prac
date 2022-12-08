package com.prac.kurento.dto;

import lombok.Data;

@Data
public class ErrorCallDto {
    String id;
    String response;
    String message;

    public ErrorCallDto(String id, String response, String message) {
        this.id = id;
        this.response = response;
        this.message = message;
    }
}
