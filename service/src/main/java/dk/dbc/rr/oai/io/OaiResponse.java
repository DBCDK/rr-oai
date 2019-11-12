/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-service
 *
 * rr-oai-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.io;

import dk.dbc.oai.pmh.GetRecordType;
import dk.dbc.oai.pmh.IdentifyType;
import dk.dbc.oai.pmh.ListIdentifiersType;
import dk.dbc.oai.pmh.ListMetadataFormatsType;
import dk.dbc.oai.pmh.ListRecordsType;
import dk.dbc.oai.pmh.ListSetsType;
import dk.dbc.oai.pmh.OAIPMH;
import dk.dbc.oai.pmh.OAIPMHerrorType;
import dk.dbc.oai.pmh.OAIPMHerrorcodeType;
import dk.dbc.oai.pmh.ObjectFactory;
import dk.dbc.oai.pmh.RequestType;
import dk.dbc.oai.pmh.VerbType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * Response structure for an OAI request
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public final class OaiResponse {

    private static final Logger log = LoggerFactory.getLogger(OaiResponse.class);

    public static final ObjectFactory O = new ObjectFactory();
    private static final JAXBContext C = makeJAXBContext();
    private static final DatatypeFactory D = makeDatatypeFactory();
    private static final XMLOutputFactory OF = makeXMLOutputFactory();
    private static final XMLEventFactory E = makeXMLEventFactory();

    private final String baseUrl;
    private final OaiRequest request;
    private final MultivaluedMap<String, String> requestParameters;
    private final OAIPMH oaipmh;

    /**
     * Create an UTC timestamp for xml output
     *
     * @param instant time information
     * @return xml-timestamp as declared in OAIPMH
     */
    public static XMLGregorianCalendar xmlDate(Instant instant) {
        ZonedDateTime zoned = instant.atZone(ZoneId.of("Z"));
        GregorianCalendar calendar = GregorianCalendar.from(zoned);
        return D.newXMLGregorianCalendar(calendar);
    }

    /**
     * Create an UTC timestamp for xml output
     *
     * @param timestamp time information
     * @return xml-timestamp as declared in OAIPMH
     */
    public static XMLGregorianCalendar xmlDate(Timestamp timestamp) {
        ZonedDateTime zoned = timestamp.toLocalDateTime().atZone(ZoneId.of("Z"));
        GregorianCalendar calendar = GregorianCalendar.from(zoned);
        return D.newXMLGregorianCalendar(calendar);
    }

    // For UNITTESTING
    static OaiResponse withoutRequestObject(String baseUrl, MultivaluedMap<String, String> requestParameters) {
        OAIPMH oaipmh = O.createOAIPMH();
        return new OaiResponse(baseUrl, null, requestParameters, oaipmh);
    }

    OaiResponse(String baseUrl, OaiRequest request, MultivaluedMap<String, String> requestParameters, OAIPMH oaipmh) {
        this.baseUrl = baseUrl;
        this.request = request;
        this.requestParameters = requestParameters;
        this.oaipmh = oaipmh;
    }

    /**
     * Add an error to the output
     *
     * @param code    error code
     * @param message textual representation of error
     */
    public void error(OAIPMHerrorcodeType code, String message) {
        OAIPMHerrorType err = O.createOAIPMHerrorType();
        err.setCode(code);
        err.setValue(message);
        oaipmh.getErrors().add(err);
    }

    /**
     * Get the request object
     *
     * @return null if the request is invalid
     */
    public OaiRequest getRequest() {
        return request;
    }

    /**
     * Create a GetRecord object
     *
     * @return response object
     */
    public GetRecordType getRecord() {
        GetRecordType obj = O.createGetRecordType();
        oaipmh.setGetRecord(obj);
        return obj;
    }

    /**
     * Create a Identify object
     *
     * @return response object
     */
    public IdentifyType identify() {
        IdentifyType obj = O.createIdentifyType();
        oaipmh.setIdentify(obj);
        return obj;
    }

    /**
     * Create a List Identifiers object
     *
     * @return response object
     */
    public ListIdentifiersType listIdentifiers() {
        ListIdentifiersType obj = O.createListIdentifiersType();
        oaipmh.setListIdentifiers(obj);
        return obj;
    }

    /**
     * Create a List Metadata Formats object
     *
     * @return response object
     */
    public ListMetadataFormatsType listMetadataFormats() {
        ListMetadataFormatsType obj = O.createListMetadataFormatsType();
        oaipmh.setListMetadataFormats(obj);
        return obj;
    }

    /**
     * Create a List Records object
     *
     * @return response object
     */
    public ListRecordsType listRecords() {
        ListRecordsType obj = O.createListRecordsType();
        oaipmh.setListRecords(obj);
        return obj;
    }

    /**
     * Create a List Sets object
     *
     * @return response object
     */
    public ListSetsType listSets() {
        ListSetsType obj = O.createListSetsType();
        oaipmh.setListSets(obj);
        return obj;
    }

    /**
     * Format data to the client
     * <p>
     * Add request parameters as required by error status
     * <p>
     * Ensure declared (from metadata formats) prefixes are used
     *
     * @return bytes to send to the user
     * @throws JAXBException      in case of object to xml fails
     * @throws XMLStreamException is case that reformatting with namespaces
     *                            fails
     * @throws IOException        on internal error of
     *                            {@link ByteArrayOutputStream}
     */
    public byte[] content() throws JAXBException, XMLStreamException, IOException {
        oaipmh.setResponseDate(xmlDate(Instant.now()));
        RequestType reqType = O.createRequestType();
        if (requiresRequestParameters())
            fillInRequestParameters(reqType);
        reqType.setValue(baseUrl);
        oaipmh.setRequest(reqType);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Marshaller marshaller = C.createMarshaller();
            XMLEventWriter writer = OF.createXMLEventWriter(bos);
            XMLEventWriterWithNamespaces nsWriter = new XMLEventWriterWithNamespaces(writer);
            marshaller.marshal(oaipmh, nsWriter);
            nsWriter.close(); // outputs to writer
            writer.close();
            return bos.toByteArray();
        }
    }

    /**
     * Check char no error is of type badVerb or badArgument
     *
     * @return if request parameters should be added to response
     */
    private boolean requiresRequestParameters() {
        return oaipmh.getErrors()
                .stream()
                .map(OAIPMHerrorType::getCode)
                .noneMatch(code -> code == OAIPMHerrorcodeType.BAD_ARGUMENT ||
                                     code == OAIPMHerrorcodeType.BAD_VERB);
    }

    /**
     * Copy known parameters from the request to the response
     *
     * @param reqType The response object that contains the request parameters
     */
    private void fillInRequestParameters(RequestType reqType) {
        setIfSet("verb", s -> reqType.setVerb(VerbType.fromValue(s)));
        setIfSet("from", reqType::setFrom);
        setIfSet("identifier", reqType::setIdentifier);
        setIfSet("metadataPrefix", reqType::setMetadataPrefix);
        setIfSet("resumptionToken", reqType::setResumptionToken);
        setIfSet("set", reqType::setSet);
        setIfSet("until", reqType::setUntil);
    }

    /**
     * Copy a parameter from the request to the response
     *
     * @param reqParameter name of the parameter in the request
     * @param setter       setter that puts the value in the response
     */
    private void setIfSet(String reqParameter, Consumer<String> setter) {
        String value = requestParameters.getFirst(reqParameter);
        if (value != null)
            setter.accept(value);
    }

    /**
     * XMLEventWriter that caches all events, and records which
     * namespaces/prefixes are used.
     * <p>
     * Upon close all events are sent to the writer declare in the constructor,
     * and all namespaces are added to the root element
     */
    private static class XMLEventWriterWithNamespaces implements XMLEventWriter {

        private final XMLEventWriter writer;
        private final ArrayList<XMLEvent> events;
        private final NamespaceContextWithDefaults namespaces;

        public XMLEventWriterWithNamespaces(XMLEventWriter writer) {
            this.writer = writer;
            this.events = new ArrayList<>();
            this.namespaces = new NamespaceContextWithDefaults();
        }

        @Override
        public void add(XMLEvent event) throws XMLStreamException {
            events.add(event);
        }

        @Override
        public void close() throws XMLStreamException {
            boolean setNamespaces = false;
            List<XMLEvent> nsEvents = namespaces.xmlEvents();
            for (XMLEvent event : events) {
                if (!nsEvents.isEmpty()) {
                    // We have namespaces to add
                    if (setNamespaces && !event.isAttribute() && !event.isNamespace()) {
                        // after all attributes in root element
                        for (XMLEvent xmlEvent : nsEvents) {
                            writer.add(xmlEvent);
                        }
                        writer.add(
                                E.createAttribute(
                                        "xsi:schemaLocation",
                                        "http://www.openarchives.org/OAI/2.0/" + " " +
                                        "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd"));
                        nsEvents = EMPTY_LIST;
                    }
                    if (event.isStartElement()) {
                        // root element - the one that should have all namespaces
                        setNamespaces = true;
                    }
                }
                writer.add(event);
            }
        }

        @Override
        public void flush() throws XMLStreamException {
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            return namespaces;
        }

        // Rest of methods not called
        @Override
        public void add(XMLEventReader reader) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPrefix(String uri) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPrefix(String prefix, String uri) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    /**
     * Namespace context, that records used namespaces, and can supply namespace
     * declarations as XMLEvents
     */
    private static class NamespaceContextWithDefaults implements NamespaceContext {

        private static final Map<String, String> DECLARED_NAMESPACES = unmodifiableMap(new HashMap<String, String>() {
            {
                put("info:lc/xmlns/marcxchange-v1", "marcx");
            }
        });
        private final Map<String, String> namespaces;
        private int no;

        public NamespaceContextWithDefaults() {
            this.namespaces = new HashMap<>();
            // from http://www.openarchives.org/OAI/openarchivesprotocol.html#XMLResponse
            this.namespaces.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
            this.no = 1;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPrefix(String namespaceURI) {
            // from http://www.openarchives.org/OAI/openarchivesprotocol.html#XMLResponse
            if ("http://www.openarchives.org/OAI/2.0/".equals(namespaceURI))
                return null;
            return namespaces.computeIfAbsent(namespaceURI, this::findPrefixForNamespace);
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /**
         * Get a prefix from the declared list
         * <p>
         * Log en error if a namespace occurs, that is unknown to the
         * implementation
         *
         * @param namespaceURI namespace
         * @return prefix
         */
        private String findPrefixForNamespace(String namespaceURI) {
            String prefix = DECLARED_NAMESPACES.get(namespaceURI);
            if (prefix == null) {
                log.error("Undeclared namespace {} add to DECLARED_NAMESPACES", namespaceURI);
                prefix = "ns" + no++;
            }
            return prefix;
        }

        /**
         * Construct a list of Namespace Events that represent those use in the
         * document
         *
         * @return list of namespace events
         */
        private List<XMLEvent> xmlEvents() {
            return namespaces
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(this::eventFromNamespace)
                    .collect(toList());
        }

        private XMLEvent eventFromNamespace(Map.Entry<String, String> entry) {
            return E.createNamespace(entry.getValue(), entry.getKey());
        }
    }

    private static JAXBContext makeJAXBContext() {
        try {
            return JAXBContext.newInstance(OAIPMH.class);
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static DatatypeFactory makeDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static XMLOutputFactory makeXMLOutputFactory() {
        synchronized (XMLOutputFactory.class) {
            return XMLOutputFactory.newInstance();
        }
    }

    private static XMLEventFactory makeXMLEventFactory() {
        synchronized (XMLEventFactory.class) {
            return XMLEventFactory.newInstance();
        }
    }
}
