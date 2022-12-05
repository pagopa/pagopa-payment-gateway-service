package it.pagopa.pm.gateway.client.vpos;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class HttpClientResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private int status;
    private byte[] entity;
}