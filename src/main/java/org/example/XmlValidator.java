package org.example;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlValidator {
    private final Schema schema;
    private final List<String> validationErrors;
    private Node errorNode;

    public XmlValidator(String xsdPath) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.schema = schemaFactory.newSchema(new File(xsdPath));
        this.validationErrors = new ArrayList<>();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public Node getErrorNode() {
        return errorNode;
    }

    public boolean validateXml(String xmlPath) throws JAXBException, IOException, ParserConfigurationException, SAXException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        LineNumberHandler handler = new LineNumberHandler(doc);

        FileInputStream is = new FileInputStream(xmlPath);
        parser.parse(is, handler);

//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setNamespaceAware(true);
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        Document document = builder.parse(new File(xmlPath));

        Validator validator = schema.newValidator();
        validator.setErrorHandler(new XmlValidationErrorHandler(doc));
        try {
            validator.validate(new StreamSource(new File(xmlPath)));
            return true; // No validation errors
        } catch (SAXException e) {
            return false; // Validation errors occurred
        }
    }

    public Object unmarshalXml(String xmlPath, Class<?> clazz) throws JAXBException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new File(xmlPath));
    }

    private class XmlValidationErrorHandler extends DefaultHandler {
        private final Document document;

        public XmlValidationErrorHandler(Document document) {
            this.document = document;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            handleValidationError(e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            handleValidationError(e);
        }

        private void handleValidationError(SAXParseException e) {
            if(!e.getMessage().startsWith("cvc-maxLength-valid") || !e.getMessage().startsWith("cvc-minLength-valid")) {
                validationErrors.add("Error: " + e.getMessage());
                errorNode = findErrorNode(document.getDocumentElement(), e.getLineNumber(), e.getColumnNumber());
                if(errorNode != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(errorNode.getNodeName());
                    constructNodeTree(errorNode, stringBuilder);
                    String nodeTree = stringBuilder.toString();
                    System.out.println(nodeTree);
                }
            }
        }

        private Node findErrorNode(Node node, int line, int column) {
            //if (node.getNodeType() == Node.ELEMENT_NODE) {
                String lineNumber = (String)node.getUserData("lineNumber");
                String columnNumber = (String)node.getUserData("columnNumber");
                String tagEndLineNumber = (String)node.getUserData("tagEndLineNumber");
                String tagEndColumnNumber = (String)node.getUserData("tagEndColumnNumber");
                if ((node.getUserData("lineNumber") != null && node.getUserData("columnNumber") != null)
                    || (node.getUserData("tagEndLineNumber") != null && node.getUserData("tagEndColumnNumber") != null)) {
                    String nodeLine = (String) node.getUserData("lineNumber");
                    String nodeColumn = (String) node.getUserData("columnNumber");
                    String tagEndNodeLine = (String) node.getUserData("tagEndLineNumber");
                    String tagEndNodeColumn = (String) node.getUserData("tagEndColumnNumber");
                    if (nodeLine.equals(Integer.toString(line)) && nodeColumn.equals(Integer.toString(column))
                        || tagEndNodeLine.equals(Integer.toString(line)) && tagEndNodeColumn.equals(Integer.toString(column))) {
                        return node;
                    }
                }
//                String childName = node.getFirstChild().getNodeName();
//                String nodeName = node.getParentNode().getNodeName();
                Node childNode = node.getFirstChild();
                while (childNode != null) {
                    Node foundNode = findErrorNode(childNode, line, column);
                    if (foundNode != null) {
                        return foundNode;
                    }
                    childNode = childNode.getNextSibling();
                }
            //}
            return null;
        }
    }

    private void constructNodeTree(Node errorNode, StringBuilder stringBuilder) {
        if (errorNode.getNodeType() == Node.ELEMENT_NODE) {
            Node parentNode = errorNode.getParentNode();
            if(parentNode != null) {
                stringBuilder.insert(0, parentNode.getNodeName() + "/");
                constructNodeTree(parentNode, stringBuilder);
            }
        }
    }
}
