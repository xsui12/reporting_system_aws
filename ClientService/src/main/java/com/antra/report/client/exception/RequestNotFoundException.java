package com.antra.report.client.exception;

public class RequestNotFoundException extends RuntimeException{
    public RequestNotFoundException(String errorMessage){
        super(errorMessage);
    }
    public RequestNotFoundException(){
        super();
    }
}
