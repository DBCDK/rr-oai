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

import dk.dbc.oai.pmh.*;
import org.eclipse.microprofile.metrics.annotation.Timed;

import static dk.dbc.rr.oai.io.OaiResponse.O;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
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

    /**
     * Extract a single record from the database / formatter service
     * <p>
     * As described in:
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#GetRecord
     * <p>
     * Validates metadataPrefix, identifier. Returns no metadata if record is
     * deleted
     *
     * @param response    Where to write data
     * @param request     The request parameters (validated)
     * @param allowedSets Which sets user has access to
     * @throws SQLException In case of database communication problems
     */
    @Timed
    public void getRecord(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        log.info("getRecord");
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
        rec.setHeader(makeHeaderFromIdentifierWithLimitedSetsFunction(allowedSets).apply(identifier));
        if (!identifier.isDeleted()) {
            MetadataType metadata = O.createMetadataType();
            URI uri = parallelFetch.buildUri(identifier.getIdentifier(), metadataPrefix, identifier.setspecsLimitedTo(allowedSets));
            metadata.setAny(parallelFetch.fetchASingleDocument(uri).getDocumentElement());
            rec.setMetadata(metadata);
        }
        record.setRecord(rec);
    }

    /**
     * Identify repository
     * <p>
     * As described in:
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#Identify
     * <p>
     * Takes data from database and from configuration
     *
     * @param response Where to write data
     */
    @Timed
    public void identify(OaiResponse response) {
        log.info("identify");
        IdentifyType identify = response.identify();
        identify.setRepositoryName(config.getRepoName());
        identify.setBaseURL(config.getExposedUrl());
        identify.setProtocolVersion("2.0");
        identify.getAdminEmails().add(config.getAdminEmail());
        identify.setEarliestDatestamp("1970-01-01T00:00:00Z"); // Epoch
        identify.setDeletedRecord(DeletedRecordType.TRANSIENT); // Cannot guarantee against database wipes
        identify.setGranularity(GranularityType.YYYY_MM_DD_THH_MM_SS_Z);

    }

    /**
     * List identifiers from a given set within an optional range
     * <p>
     * As described in:
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#ListIdentifiers
     * <p>
     * Takes parameters from either resumptionToken or from/until/set...
     * <p>
     * It is possible to get a header with an empty setSpecs list, if the record
     * used to be in the set, but has been moved out of it, but not deleted
     *
     * @param response    Where to write data
     * @param request     The request parameters (validated)
     * @param allowedSets Which sets user has access to
     * @throws SQLException In case of database communication problems
     */
    @Timed
    public void listIdentifiers(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        log.info("listIdentifiers");
        ListIdentifiersType list = response.listIdentifiers();
        List<OaiIdentifier> identifiers = getIdentifiers(response, request, list::setResumptionToken, allowedSets);

        if (identifiers.isEmpty())
            return;

        List<HeaderType> headers = list.getHeaders();
        Function<OaiIdentifier, HeaderType> headerBuilder =
                makeHeaderFromIdentifierWithLimitedSetsFunction(allowedSets);
        identifiers.stream()
                .map(headerBuilder)
                .forEach(headers::add);
    }

    /**
     * List all metadata formats from the database
     * <p>
     * As described in:
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#ListMetadataFormats
     * <p>
     * Validates that the user has access to the optional identifier
     *
     * @param response    Where to write data
     * @param request     The request parameters (validated)
     * @param allowedSets Which sets user has access to
     * @throws SQLException In case of database communication problems
     */
    @Timed
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

    /**
     * List identifiers and metadata from a given set within an optional range
     * <p>
     * As described in:
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#ListRecords
     * <p>
     * a lot like
     * {@link #listIdentifiers(dk.dbc.rr.oai.io.OaiResponse, dk.dbc.rr.oai.io.OaiRequest, java.util.Set)}
     * except the metadata is fetched too.
     *
     * @param response    Where to write data
     * @param request     The request parameters (validated)
     * @param allowedSets Which sets user has access to
     * @throws SQLException In case of database communication problems
     */
    @Timed
    public void listRecords(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        log.info("listRecords");
        ListRecordsType list = response.listRecords();
        List<OaiIdentifier> identifiers = getIdentifiers(response, request, list::setResumptionToken, allowedSets);

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

        Function<OaiIdentifier, HeaderType> headerBuilder =
                makeHeaderFromIdentifierWithLimitedSetsFunction(allowedSets);
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

    /**
     * List set specifications
     * <p>
     * As described in:
     * http://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets
     *
     * @param response Where to write data
     */
    @Timed
    public void listSets(OaiResponse response) {
        log.info("listSets");
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

    private static final byte[] DC_DESCRIPTION_BYTES =
            ( "<oai_dc:dc" +
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
     * @param resumptionTokenSetter How to set the resumption token
     * @return list of identifiers - might be empty
     * @throws SQLException If the database acts up
     */
    private List<OaiIdentifier> getIdentifiers(OaiResponse response, OaiRequest request, Consumer<ResumptionTokenType> resumptionTokenSetter, Set<String> allowedSets) throws SQLException {
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
            identifiers = databaseWorker.listIdentifiers(resumptionToken, allowedSets);
        } else {
            from = request.getFrom();
            until = request.getUntil();
            set = request.getSet();
            if (set == null) {
                identifiers = databaseWorker.listIdentifiers(from, until, allowedSets);
            } else {
                identifiers = databaseWorker.listIdentifiers(from, until, singleton(set));
            }
        }
        if (from.getTimestamp().after(until.getTimestamp())) {
            response.error(OAIPMHerrorcodeType.BAD_ARGUMENT, "Error in timestamp arguments: 'from' cannot be after 'until'");
            return Collections.EMPTY_LIST;
        }
        log.debug("identifiers.size() = {}", identifiers.size());
        if (identifiers.isEmpty()) {
            response.error(OAIPMHerrorcodeType.NO_RECORDS_MATCH, "There are no records in the interval");
            return Collections.EMPTY_LIST;
        }
        if (identifiers.size() > config.getMaxRowsPrRequest()) {
            OaiIdentifier resumeFrom = identifiers.removeLast();
            ResumptionTokenType token = ioBean.resumptionTokenFor(from, resumeFrom, until, set);
            resumptionTokenSetter.accept(token);
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
    private Function<OaiIdentifier, HeaderType> makeHeaderFromIdentifierWithLimitedSetsFunction(Set<String> allowedSets) {
        return identifier -> {
            HeaderType header = O.createHeaderType();
            header.setDatestamp(identifier.getChanged().toInstant().toString());
            header.setIdentifier(identifier.getIdentifier());
            if (identifier.isDeleted())
                header.setStatus(StatusType.DELETED);
            List<String> setSpecs = header.getSetSpecs();
            identifier.setspecsLimitedTo(allowedSets)
                    .stream()
                    .sorted()
                    .forEach(setSpecs::add);
            return header;
        };
    }

}
