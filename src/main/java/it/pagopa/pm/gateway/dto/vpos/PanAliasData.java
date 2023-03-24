package it.pagopa.pm.gateway.dto.vpos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.pm.gateway.config.UnmaskSerializer;
import it.pagopa.pm.gateway.utils.GenericUtils;
import lombok.Data;

@Data
public class PanAliasData {
    @JsonSerialize(using = UnmaskSerializer.class)
    private String panAlias;
    private String panAliasRev;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String panAliasExpDate;
    @JsonSerialize(using = UnmaskSerializer.class)
    private String panAliasTail;
    private String mac;

    public String unmaskedPanAlias() {
        return panAlias;
    }

    public String unmaskedPanAliasExpDate() {
        return panAlias;
    }

    public String unmaskedPanAliasTail() {
        return panAliasTail;
    }

    private String getPanAlias() {
        return GenericUtils.maskValue(panAlias);
    }

    private String getPanAliasExpDate() {
        return GenericUtils.maskValue(panAliasExpDate);
    }

    private String getPanAliasTail() {
        return GenericUtils.maskValue(panAliasTail);
    }
}
