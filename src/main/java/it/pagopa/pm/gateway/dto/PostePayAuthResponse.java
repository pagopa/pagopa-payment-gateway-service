package it.pagopa.pm.gateway.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class PostePayAuthResponse {

   String channel;
   String urlRedirect;
   String error;


}
