package it.pagopa.pm.gateway.client.vpos;

import java.io.Serializable;

/**
 * The Class HttpClientResponse.
 *
 */
public class HttpClientResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private int status;
    private byte[] entity;

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the entity
     */
    public byte[] getEntity() {
        return entity;
    }

    /**
     * @param entity
     *            the entity to set
     */
    public void setEntity(byte[] entity) {
        this.entity = entity;
    }

}

