package it.pagopa.pm.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostePayPatchRequestData {
    private PatchRequest data;
}
