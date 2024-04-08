package org.example;

import jakarta.xml.bind.JAXBException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        XmlValidator validator = null;
        try {
            validator = new XmlValidator("src/main/resources/sample.xsd");
            validator.validateXml("src/main/resources/sample.xml");
            validator.getValidationErrors().forEach(System.out::println);
        } catch (SAXException | JAXBException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }

    }
}