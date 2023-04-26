package it.pagopa.pm.gateway.dto.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum XpayErrorCodeEnum {

    ERROR_1(1L, "Uno dei valori dei parametri del json in input non è corretto"),
    ERROR_2(2L, "Non è possibile trovare l’informazione richiesta"),
    ERROR_3(3L, "MAC errato"),
    ERROR_4(4L, "MAC non presente nella richiesta json"),
    ERROR_5(5L, "Sono trascorsi più di 5 minuti da quando il timestamp è stato generato"),
    ERROR_6(6L, "apiKey non contiene un alias valido"),
    ERROR_7(7L, "apiKey non contiene un alias valido"),
    ERROR_8(8L, "Contratto non valido"),
    ERROR_9(9L, "Transazione già presente"),
    ERROR_12(12L, "Gruppo non valido"),
    ERROR_13(13L, "La transazione non è stata trovata"),
    ERROR_14(14L, "La carta è scaduta"),
    ERROR_15(15L, "Brand carta non permesso"),
    ERROR_16(16L, "Valore non valido nello stato corrente"),
    ERROR_17(17L, "Importo operazione troppo alto"),
    ERROR_18(18L, "Numero tentativi di retry esauriti"),
    ERROR_19(19L, "Pagamento rifiutato. Verificare le note presenti in fondo alla pagina per verificare i possibili valori del campo \"messaggio\"."),
    ERROR_20(20L, "Autenticazione 3DS annullata"),
    ERROR_21(21L, "Autenticazione 3DS fallita"),
    ERROR_22(22L, "Carta non valida per l’addebito (scaduta o bloccata)"),
    ERROR_50(50L, "Impossibile calcolare il mac, nei caso in cui l’alias non sia valido o il json in ingresso non sia conforme a quello richiesto"),
    ERROR_96(96L, "In caso di esito KO con codice di errore 96, è possibile ritentare il pagamento riutilizzando lo stesso codice transazione e passando come parametro \"softDecline\" valorizzato a \"S\" nella creaNonce. Si riceverà in risposta il codice html che forzerà la SCA, in modo da ottenere un nuovo nonce da utilizzare nell'api pagaNonce"),
    ERROR_97(97L, "Errore generico"),
    ERROR_98(98L, "Metodo non ancora implementato"),
    ERROR_99(99L, "Operazione non permessa, il merchant non ha i requisiti per effettuare l’operazione richiesta"),
    ERROR_100(100L, "Errore interno");

    private final Long errorCode;
    private final String description;

    public static XpayErrorCodeEnum getEnumFromCode(long code) {
        return Arrays.stream(XpayErrorCodeEnum.values())
                .filter(enumValue -> enumValue.errorCode.equals(code))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Error code " + code + " not supported"));
    }
}
