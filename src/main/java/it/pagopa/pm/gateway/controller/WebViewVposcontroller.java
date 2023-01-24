package it.pagopa.pm.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static it.pagopa.pm.gateway.constant.ApiPaths.METHOD_NOTIFICATIONS;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;
import static it.pagopa.pm.gateway.constant.VposConstant.HTML_METHOD;

@Slf4j
@Controller
@RequestMapping(REQUEST_PAYMENTS_VPOS)
public class WebViewVposcontroller {

    @ResponseBody
    @PostMapping(METHOD_NOTIFICATIONS)
    public String methodNotifications() {
        return HTML_METHOD;
    }

}
