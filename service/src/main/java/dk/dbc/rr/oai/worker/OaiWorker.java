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
package dk.dbc.rr.oai.worker;

import dk.dbc.oai.pmh.DeletedRecordType;
import dk.dbc.oai.pmh.DescriptionType;
import dk.dbc.oai.pmh.GetRecordType;
import dk.dbc.oai.pmh.GranularityType;
import dk.dbc.oai.pmh.HeaderType;
import dk.dbc.oai.pmh.IdentifyType;
import dk.dbc.oai.pmh.ListIdentifiersType;
import dk.dbc.oai.pmh.ListRecordsType;
import dk.dbc.oai.pmh.MetadataFormatType;
import dk.dbc.oai.pmh.MetadataType;
import dk.dbc.oai.pmh.OAIPMHerrorcodeType;
import dk.dbc.oai.pmh.RecordType;
import dk.dbc.oai.pmh.ResumptionTokenType;
import dk.dbc.oai.pmh.SetType;
import dk.dbc.oai.pmh.StatusType;
import dk.dbc.rr.oai.Config;
import dk.dbc.rr.oai.fetch.DocumentBuilderPool;
import dk.dbc.rr.oai.fetch.ParallelFetch;
import dk.dbc.rr.oai.io.OaiIOBean;
import dk.dbc.rr.oai.io.OaiIdentifier;
import dk.dbc.rr.oai.io.OaiRequest;
import dk.dbc.rr.oai.io.OaiResponse;
import dk.dbc.rr.oai.io.OaiResumptionToken;
import dk.dbc.rr.oai.io.OaiTimestamp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static dk.dbc.rr.oai.io.OaiResponse.O;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class OaiWorker {

    private static final Logger log = LoggerFactory.getLogger(OaiWorker.class);

    @Inject
    public Config config;

    @Inject
    public OaiDatabaseMetadata databaseMetadata;

    @Inject
    public OaiDatabaseWorker databaseWorker;

    @Inject
    public DocumentBuilderPool documentBuilders;

    @Inject
    public OaiIOBean ioBean;

    @Inject
    public ParallelFetch parallelFetch;

    public void getRecord(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        GetRecordType record = response.getRecord();
        String metadataPrefix = request.getMetadataPrefix();
        if (!databaseMetadata.knownPrefix(metadataPrefix)) {
            response.error(OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, "Unknown metadata prefix");
            return;
        }
        OaiIdentifier identifier = databaseWorker.getIdentifier(request.getIdentifier());
        if (identifier == null || identifier.setspecsLimitedTo(allowedSets).isEmpty()) {
            response.error(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "No such record");
            return;
        }
        RecordType rec = O.createRecordType();
        rec.setHeader(headerFromIdentifierWithLimitedSets(allowedSets).apply(identifier));
        MetadataType metadata = O.createMetadataType();
        URI uri = parallelFetch.buildUri(identifier.getIdentifier(), metadataPrefix, identifier.setspecsLimitedTo(allowedSets));
        metadata.setAny(parallelFetch.fetchASingleDocument(uri).getDocumentElement());
        rec.setMetadata(metadata);
        record.setRecord(rec);
    }

    public void identify(OaiResponse response) {

        IdentifyType identify = response.identify();
        identify.setRepositoryName(config.getRepoName());
        identify.setBaseURL(config.getExposedUrl());
        identify.setProtocolVersion("2.0");
        identify.getAdminEmails().add(config.getAdminEmail());
        identify.setEarliestDatestamp("1970-01-01T00:00:00Z"); // Epoch
        identify.setDeletedRecord(DeletedRecordType.TRANSIENT); // Cannot guarantee against database wipes
        identify.setGranularity(GranularityType.YYYY_MM_DD_THH_MM_SS_Z);

    }

    public void listIdentifiers(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        ListIdentifiersType list = response.listIdentifiers();
        List<OaiIdentifier> identifiers = getIdentifiers(response, request, list::setResumptionToken);

        if (identifiers.isEmpty())
            return;

        List<HeaderType> headers = list.getHeaders();
        identifiers.stream()
                .map(headerFromIdentifierWithLimitedSets(allowedSets))
                .forEach(headers::add);
    }

    public void listMetadataFormats(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        log.info("listMetadataFormats");
        if (request.getIdentifier() != null) {
            OaiIdentifier identifier = databaseWorker.getIdentifier(request.getIdentifier());
            if (identifier == null || identifier.setspecsLimitedTo(allowedSets).isEmpty()) {
                response.error(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "No such record");
                return;
            }
        }
        List<MetadataFormatType> metadataFormats = response.listMetadataFormats()
                .getMetadataFormats();
        databaseMetadata.getFormats()
                .forEach(format -> {
                    MetadataFormatType xml = O.createMetadataFormatType();
                    xml.setMetadataPrefix(format.getPrefix());
                    xml.setSchema(format.getSchema());
                    xml.setMetadataNamespace(format.getNamespace());
                    metadataFormats.add(xml);
                });
    }

    public void listRecords(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        ListRecordsType list = response.listRecords();
        List<OaiIdentifier> identifiers = getIdentifiers(response, request, list::setResumptionToken);

        if (identifiers.isEmpty())
            return;

        String metadataPrefix = request.getMetadataPrefix();

        List<URI> uris = identifiers.stream()
                .filter(i -> !i.isDeleted() && !i.setspecsLimitedTo(allowedSets).isEmpty())
                .map(i -> parallelFetch.buildUri(i.getIdentifier(), metadataPrefix, i.setspecsLimitedTo(allowedSets)))
                .collect(Collectors.toList());
        List<Element> elements = parallelFetch.parallelFetch(uris);

        if (uris.size() != elements.size()) {
            log.error("Error formatting records - got different list sizes: uris = {} and elements = {}", uris.size(), elements.size());
            throw new ServerErrorException("Error formatting records", INTERNAL_SERVER_ERROR);
        }

        Function<OaiIdentifier, HeaderType> headerBuilder = headerFromIdentifierWithLimitedSets(allowedSets);
        List<RecordType> records = list.getRecords();
        Iterator<OaiIdentifier> ids = identifiers.iterator();
        Iterator<Element> elems = elements.iterator();
        while (ids.hasNext()) {
            OaiIdentifier id = ids.next();
            RecordType record = O.createRecordType();
            HeaderType header = headerBuilder.apply(id);
            record.setHeader(header);
            if (!id.isDeleted() && !id.setspecsLimitedTo(allowedSets).isEmpty()) {
                Element elem = elems.next();
                // is not deleted and client has access to a set it is in
                MetadataType metadata = O.createMetadataType();
                metadata.setAny(elem);
                record.setMetadata(metadata);
            }
            records.add(record);
        }
    }

    public void listSets(OaiResponse response) {
        List<SetType> sets = response.listSets().getSets();
        databaseMetadata.getSets()
                .forEach(s -> {
                    SetType set = O.createSetType();
                    set.setSetSpec(s.getSetspec());
                    set.setSetName(s.getSetname());
                    DescriptionType desc = O.createDescriptionType();
                    desc.setAny(makeSetDescription(s.getDescription()));
                    set.getSetDescriptions().add(desc);
                    sets.add(set);
                });
    }

    private static final byte[] DC_DESCRIPTION_BYTES = ( "<oai_dc:dc" +
                                                         " xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"" +
                                                         " xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
                                                         " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                                                         " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/" +
                                                         " http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">" +
                                                         "<dc:description/>" +
                                                         "</oai_dc:dc>" ).getBytes(UTF_8);

    private Element makeSetDescription(String desc) {
        try (DocumentBuilderPool.Lease lease = documentBuilders.lease() ;
             InputStream is = new ByteArrayInputStream(DC_DESCRIPTION_BYTES)) {
            Document doc = lease.get().parse(is);
            NodeList nodes = doc.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "description");
            if (nodes.getLength() != 1)
                throw new AssertionError();
            nodes.item(0).appendChild(doc.createTextNode(desc));
            return doc.getDocumentElement();
        } catch (SAXException | IOException ex) {
            log.error("Error parsing dc description (static xml): {}", ex.getMessage());
            log.debug("Error parsing dc description (static xml): ", ex);
            throw new ServerErrorException("Cannot build description", INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Returns a list of identifiers for the given interval.
     * <p>
     * - If metadataPrefix is unknown an error is set and an empty list is
     * returned
     * - If no records are returned an error is set and an empty list is
     * returned
     * - If there are more records, the resumption token is set using the setter
     *
     * @param response               Where errors are posted
     * @param request                The user request data
     * @param resumpotionTokenSetter How to set the resumption token
     * @return list of identifiers - might be empty
     * @throws SQLException If the database acts up
     */
    private List<OaiIdentifier> getIdentifiers(OaiResponse response, OaiRequest request, Consumer<ResumptionTokenType> resumpotionTokenSetter) throws SQLException {
        if (!databaseMetadata.knownPrefix(request.getMetadataPrefix())) {
            response.error(OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, "Unknown metadata prefix");
            return Collections.EMPTY_LIST;
        }
        LinkedList<OaiIdentifier> identifiers;
        OaiTimestamp from;
        OaiTimestamp until;
        String set;
        if (request.getResumptionToken() != null) {
            OaiResumptionToken resumptionToken = request.getResumptionToken();
            from = resumptionToken.getFrom();
            until = resumptionToken.getUntil();
            set = resumptionToken.getSet();
            identifiers = databaseWorker.listIdentifiers(resumptionToken);
        } else {
            from = request.getFrom();
            until = request.getUntil();
            set = request.getSet();
            identifiers = databaseWorker.listIdentifiers(from, until, set);
        }
        log.debug("identifiers.size() = {}", identifiers.size());
        if (identifiers.isEmpty()) {
            response.error(OAIPMHerrorcodeType.NO_RECORDS_MATCH, "There are no records in the interval");
            return Collections.EMPTY_LIST;
        }
        if (identifiers.size() > config.getMaxRowsPrRequest()) {
            OaiIdentifier resumeFrom = identifiers.removeLast();
            ResumptionTokenType token = ioBean.resumptionTokenFor(from, resumeFrom, until, set);
            resumpotionTokenSetter.accept(token);
        }
        return identifiers;
    }

    /**
     * Build a function that converts a OaiIdentifier to a XML HeaderType,
     * but limits setSpec tags to the sets allowed
     *
     * @param allowedSets filter for setspec
     * @return method
     */
    private Function<OaiIdentifier, HeaderType> headerFromIdentifierWithLimitedSets(Set<String> allowedSets) {
        return identifier -> {
            HeaderType header = O.createHeaderType();
            header.setDatestamp(identifier.getChanged().toInstant().toString());
            header.setIdentifier(identifier.getIdentifier());
            if (identifier.isDeleted())
                header.setStatus(StatusType.DELETED);
            List<String> setSpecs = header.getSetSpecs();
            identifier.setspecsLimitedTo(allowedSets).forEach(setSpecs::add);
            return header;
        };
    }

}
