package it.pagopa.pm.gateway.dto;


import lombok.Data;

@Data
public class PostePayAuthResponse {

   String channel;
   String urlRedirect;
   String error;


}
