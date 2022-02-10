//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.09 alle 04:22:41 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the it.pagopa.pm.gateway.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _InquiryTransactionStatus_QNAME = new QName("http://p2b.gft.it/srv/pp", "inquiryTransactionStatus");
    private final static QName _InquiryTransactionStatusResponse_QNAME = new QName("http://p2b.gft.it/srv/pp", "inquiryTransactionStatusResponse");
    private final static QName _InserimentoRichiestaPagamentoPagoPa_QNAME = new QName("http://p2b.gft.it/srv/pp", "inserimentoRichiestaPagamentoPagoPa");
    private final static QName _InserimentoRichiestaPagamentoPagoPaResponse_QNAME = new QName("http://p2b.gft.it/srv/pp", "inserimentoRichiestaPagamentoPagoPaResponse");
    private final static QName _StornoPagamento_QNAME = new QName("http://p2b.gft.it/srv/pp", "stornoPagamento");
    private final static QName _StornoPagamentoResponse_QNAME = new QName("http://p2b.gft.it/srv/pp", "stornoPagamentoResponse");
    private final static QName _Exception_QNAME = new QName("http://p2b.gft.it/srv/pp", "Exception");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: it.pagopa.pm.gateway.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link InquiryTransactionStatus }
     * 
     */
    public InquiryTransactionStatus createInquiryTransactionStatus() {
        return new InquiryTransactionStatus();
    }

    /**
     * Create an instance of {@link InquiryTransactionStatusResponse }
     * 
     */
    public InquiryTransactionStatusResponse createInquiryTransactionStatusResponse() {
        return new InquiryTransactionStatusResponse();
    }

    /**
     * Create an instance of {@link InserimentoRichiestaPagamentoPagoPa }
     * 
     */
    public InserimentoRichiestaPagamentoPagoPa createInserimentoRichiestaPagamentoPagoPa() {
        return new InserimentoRichiestaPagamentoPagoPa();
    }

    /**
     * Create an instance of {@link InserimentoRichiestaPagamentoPagoPaResponse }
     * 
     */
    public InserimentoRichiestaPagamentoPagoPaResponse createInserimentoRichiestaPagamentoPagoPaResponse() {
        return new InserimentoRichiestaPagamentoPagoPaResponse();
    }

    /**
     * Create an instance of {@link StornoPagamento }
     * 
     */
    public StornoPagamento createStornoPagamento() {
        return new StornoPagamento();
    }

    /**
     * Create an instance of {@link StornoPagamentoResponse }
     * 
     */
    public StornoPagamentoResponse createStornoPagamentoResponse() {
        return new StornoPagamentoResponse();
    }

    /**
     * Create an instance of {@link Exception }
     * 
     */
    public Exception createException() {
        return new Exception();
    }

    /**
     * Create an instance of {@link RequestStornoPagamentoVO }
     * 
     */
    public RequestStornoPagamentoVO createRequestStornoPagamentoVO() {
        return new RequestStornoPagamentoVO();
    }

    /**
     * Create an instance of {@link BaseRequestVO }
     * 
     */
    public BaseRequestVO createBaseRequestVO() {
        return new BaseRequestVO();
    }

    /**
     * Create an instance of {@link BaseRequestLightVO }
     * 
     */
    public BaseRequestLightVO createBaseRequestLightVO() {
        return new BaseRequestLightVO();
    }

    /**
     * Create an instance of {@link BaseVO }
     * 
     */
    public BaseVO createBaseVO() {
        return new BaseVO();
    }

    /**
     * Create an instance of {@link OtpContestoVO }
     * 
     */
    public OtpContestoVO createOtpContestoVO() {
        return new OtpContestoVO();
    }

    /**
     * Create an instance of {@link DeviceIdDataVO }
     * 
     */
    public DeviceIdDataVO createDeviceIdDataVO() {
        return new DeviceIdDataVO();
    }

    /**
     * Create an instance of {@link ContestoVO }
     * 
     */
    public ContestoVO createContestoVO() {
        return new ContestoVO();
    }

    /**
     * Create an instance of {@link UtenteAttivoVO }
     * 
     */
    public UtenteAttivoVO createUtenteAttivoVO() {
        return new UtenteAttivoVO();
    }

    /**
     * Create an instance of {@link UtenteVO }
     * 
     */
    public UtenteVO createUtenteVO() {
        return new UtenteVO();
    }

    /**
     * Create an instance of {@link ResponseStornoPagamentoVO }
     * 
     */
    public ResponseStornoPagamentoVO createResponseStornoPagamentoVO() {
        return new ResponseStornoPagamentoVO();
    }

    /**
     * Create an instance of {@link BaseResponseVO }
     * 
     */
    public BaseResponseVO createBaseResponseVO() {
        return new BaseResponseVO();
    }

    /**
     * Create an instance of {@link EsitoVO }
     * 
     */
    public EsitoVO createEsitoVO() {
        return new EsitoVO();
    }

    /**
     * Create an instance of {@link RequestInserimentoRichiestaPagamentoPagoPaVO }
     * 
     */
    public RequestInserimentoRichiestaPagamentoPagoPaVO createRequestInserimentoRichiestaPagamentoPagoPaVO() {
        return new RequestInserimentoRichiestaPagamentoPagoPaVO();
    }

    /**
     * Create an instance of {@link RichiestaPagamentoPagoPaVO }
     * 
     */
    public RichiestaPagamentoPagoPaVO createRichiestaPagamentoPagoPaVO() {
        return new RichiestaPagamentoPagoPaVO();
    }

    /**
     * Create an instance of {@link RichiestaPagamentoOnlineVO }
     * 
     */
    public RichiestaPagamentoOnlineVO createRichiestaPagamentoOnlineVO() {
        return new RichiestaPagamentoOnlineVO();
    }

    /**
     * Create an instance of {@link ResponseInserimentoRichiestaPagamentoPagoPaVO }
     * 
     */
    public ResponseInserimentoRichiestaPagamentoPagoPaVO createResponseInserimentoRichiestaPagamentoPagoPaVO() {
        return new ResponseInserimentoRichiestaPagamentoPagoPaVO();
    }

    /**
     * Create an instance of {@link RequestInquiryTransactionStatusVO }
     * 
     */
    public RequestInquiryTransactionStatusVO createRequestInquiryTransactionStatusVO() {
        return new RequestInquiryTransactionStatusVO();
    }

    /**
     * Create an instance of {@link ResponseInquiryTransactionStatusVO }
     * 
     */
    public ResponseInquiryTransactionStatusVO createResponseInquiryTransactionStatusVO() {
        return new ResponseInquiryTransactionStatusVO();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InquiryTransactionStatus }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link InquiryTransactionStatus }{@code >}
     */
    @XmlElementDecl(namespace = "http://p2b.gft.it/srv/pp", name = "inquiryTransactionStatus")
    public JAXBElement<InquiryTransactionStatus> createInquiryTransactionStatus(InquiryTransactionStatus value) {
        return new JAXBElement<InquiryTransactionStatus>(_InquiryTransactionStatus_QNAME, InquiryTransactionStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InquiryTransactionStatusResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link InquiryTransactionStatusResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://p2b.gft.it/srv/pp", name = "inquiryTransactionStatusResponse")
    public JAXBElement<InquiryTransactionStatusResponse> createInquiryTransactionStatusResponse(InquiryTransactionStatusResponse value) {
        return new JAXBElement<InquiryTransactionStatusResponse>(_InquiryTransactionStatusResponse_QNAME, InquiryTransactionStatusResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InserimentoRichiestaPagamentoPagoPa }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link InserimentoRichiestaPagamentoPagoPa }{@code >}
     */
    @XmlElementDecl(namespace = "http://p2b.gft.it/srv/pp", name = "inserimentoRichiestaPagamentoPagoPa")
    public JAXBElement<InserimentoRichiestaPagamentoPagoPa> createInserimentoRichiestaPagamentoPagoPa(InserimentoRichiestaPagamentoPagoPa value) {
        return new JAXBElement<InserimentoRichiestaPagamentoPagoPa>(_InserimentoRichiestaPagamentoPagoPa_QNAME, InserimentoRichiestaPagamentoPagoPa.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InserimentoRichiestaPagamentoPagoPaResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link InserimentoRichiestaPagamentoPagoPaResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://p2b.gft.it/srv/pp", name = "inserimentoRichiestaPagamentoPagoPaResponse")
    public JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> createInserimentoRichiestaPagamentoPagoPaResponse(InserimentoRichiestaPagamentoPagoPaResponse value) {
        return new JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>(_InserimentoRichiestaPagamentoPagoPaResponse_QNAME, InserimentoRichiestaPagamentoPagoPaResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StornoPagamento }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link StornoPagamento }{@code >}
     */
    @XmlElementDecl(namespace = "http://p2b.gft.it/srv/pp", name = "stornoPagamento")
    public JAXBElement<StornoPagamento> createStornoPagamento(StornoPagamento value) {
        return new JAXBElement<StornoPagamento>(_StornoPagamento_QNAME, StornoPagamento.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StornoPagamentoResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link StornoPagamentoResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://p2b.gft.it/srv/pp", name = "stornoPagamentoResponse")
    public JAXBElement<StornoPagamentoResponse> createStornoPagamentoResponse(StornoPagamentoResponse value) {
        return new JAXBElement<StornoPagamentoResponse>(_StornoPagamentoResponse_QNAME, StornoPagamentoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Exception }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Exception }{@code >}
     */
    @XmlElementDecl(namespace = "http://p2b.gft.it/srv/pp", name = "Exception")
    public JAXBElement<Exception> createException(Exception value) {
        return new JAXBElement<Exception>(_Exception_QNAME, Exception.class, null, value);
    }

}
