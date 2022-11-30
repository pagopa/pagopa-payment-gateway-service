package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.dto.enums.VposRequestEnum;
import org.jdom2.Document;
import org.jdom2.Element;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class VPosDocumentBuilder {
    private final Element root;
    private final Map<String, Element> children = new LinkedHashMap<>();
    private final Document request;

    public VPosDocumentBuilder(Locale locale) {
        if (Locale.ITALIAN.equals(locale)) {
            this.root = new Element(VposRequestEnum.getRootElementIta().getTagName());
        } else if (Locale.ENGLISH.equals(locale)) {
            this.root = new Element(VposRequestEnum.getRootElementEng().getTagName());
        } else {
            this.root = null;
        }
        this.request = new Document(root);
    }

    public void addElement(VposRequestEnum tag, Object value) {
        addElement(null, tag, value);
    }

    public void addElement(VposRequestEnum elementRoot, Element element) {
        Element rootElement = findRoot(elementRoot);
        rootElement.addContent(element);
    }

    public void addElement(VposRequestEnum elementRoot, VposRequestEnum tag, Object value) {
        Element rootElement = findRoot(elementRoot);
        if (tag != null) {
            if (tag.getType() != null) {
                if (tag.isMandatory() && value == null) {
                    throw new IllegalArgumentException("The request tag: " + tag.getTagName() + " is mandatory");
                }
                if (value.getClass().equals(tag.getType())) {
                    Element element = new Element(tag.getTagName());
                    String textValue = value.toString();
                    checkTextValue(value, tag, element, textValue);
                    rootElement.addContent(element);
                } else {
                    throw new IllegalArgumentException("The request tag: " + tag.getTagName()
                            + " need a value of type: " + tag.getType().getSimpleName());
                }
            } else {
                Element element = new Element(tag.getTagName());
                rootElement.addContent(element);
                children.put(tag.getTagName(), element);
            }
        } else {
            throw new IllegalArgumentException("Argument tag cannot be null");
        }
    }

    private void checkTextValue(Object value, VposRequestEnum tag, Element element, String textValue) {
        if (textValue.trim().length() > 0) {
            if (value instanceof Date) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(tag.getFormat());
                textValue = simpleDateFormat.format((Date) value);
            }
            element.setText(textValue);
        }
    }

    public void addBodyElement(VposRequestEnum tag) {
        addElement(null, tag, null);
    }

    public void addBodyElement(VposRequestEnum elementRoot, VposRequestEnum tag) {
        Element rootElement = children.get(elementRoot.getTagName());
        if (rootElement == null) {
            throw new IllegalArgumentException("element root invalid");
        }
        Element element = new Element(tag.getTagName());
        rootElement.addContent(element);
        children.put(tag.getTagName(), element);
    }

    public Document build() {
        children.clear();
        return request;
    }

    private Element findRoot(VposRequestEnum element) {
        Element rootElement = this.root;
        if (element != null) {
            rootElement = children.get(element.getTagName());
            if (rootElement == null) {
                throw new IllegalArgumentException("element root invalid");
            }
        }
        return rootElement;
    }
}
