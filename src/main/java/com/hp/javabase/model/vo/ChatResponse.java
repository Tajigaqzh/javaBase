package com.hp.javabase.model.vo;

public class ChatResponse {

    private String model;

    private String answer;

    public ChatResponse() {
    }

    public ChatResponse(String model, String answer) {
        this.model = model;
        this.answer = answer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
