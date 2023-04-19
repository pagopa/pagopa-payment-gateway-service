package it.pagopa.pm.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static it.pagopa.pm.gateway.constant.ApiPaths.METHOD_NOTIFICATIONS;
import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;

@Slf4j
@Controller
@RequestMapping(REQUEST_PAYMENTS_VPOS)
public class WebViewVposController {

    @Autowired
    private Environment environment;

    @PostMapping(METHOD_NOTIFICATIONS)
    public String methodNotifications(Model model) {
        String origin = environment.getProperty("vpos.method.origin");
        model.addAttribute("origin", origin);
        return "methodNotification";
    }

}
