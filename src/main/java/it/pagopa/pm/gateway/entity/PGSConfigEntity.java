package it.pagopa.pm.gateway.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "PP_PGS_CONFIG")
public class PGSConfigEntity {

    @Id
    @Column(name = "ID_CONFIG", nullable = false)
    @SequenceGenerator(name = "SEQ_PGS_CONFIG", sequenceName = "SEQ_PGS_CONFIG", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PGS_CONFIG")
    private Long id;

    @Column(name = "CONFIG_KEY", nullable = false)
    private String key;

    @Column(name = "CONFIG_VALUE", nullable = false)
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PGS_CONFIG { id=" + id + ", key=" + key + ", value=" + value + " }";
    }

}
