package it.pagopa.pm.gateway.config;

import com.google.auto.service.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.WARNING;

@SupportedAnnotationTypes(value = {"it.pagopa.pm.gateway.config.Sensitive"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SensitiveAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Sensitive.class);
        for (Element element : elements) {
            //processingEnv.getMessager().printMessage(WARNING, "Sensitive field, do not log", element);
        }
        return true;
    }

}
