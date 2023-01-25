package it.pagopa.pm.gateway.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WebViewVposController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class WebViewVposControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void testMethodNotifications() throws Exception {
        String HTML = "<html>" +
                "<head>" +
                "<script>" +
                "            var getUrl = window.location;" +
                "window.parent.postMessage(\"3DS.Notification.Received\", getUrl.protocol + \"//\" + getUrl.host);" +
                "</script>" +
                "    </head>" +
                "<body></body>" +
                "</html>";
        String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";
        mvc.perform(post(REQUEST_PAYMENTS_VPOS + "/" + UUID_SAMPLE + "/method/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().string(HTML));
    }
}
