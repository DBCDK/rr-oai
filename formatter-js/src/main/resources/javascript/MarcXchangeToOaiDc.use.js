/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

/** @file Module that creates simple Dublin Core elements based on marcxchange record. */

use( "DanMarc2Util" );
use( "Log" );
use( "Marc" );
use( "MarcXchange" );
use( "XmlNamespaces" );
use( "XmlUtil" );

EXPORTED_SYMBOLS = [ 'MarcXchangeToOaiDc' ];

/**
 * Module with functions that creates simple Dublin Core elements for OAI harvest of records from RawRepo.
 *
 * This module contains functions to create dc record
 *
 * @type {namespace}
 * @namespace
 */

var MarcXchangeToOaiDc = function() {

    Log.info( "Entering MarcXchangeToOaiDc module" );

    /**
     * Function that is entry to create a complete Dublin Core Record.
     *
     * @syntax MarcXchangeToOaiDc.createDcXml( marcRecord, higherLevelIdentifiers )
     * @param {Record} marcRecord the marc record to create DC from
     * @param {String[]} higherLevelIdentifiers Array of possible identifiers of records on higher level (section/head)
     * @return {Document} the created DC record
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.createDcXml
     */
    function createDcXml( marcRecord, higherLevelIdentifiers ) {

        Log.trace( "Entering MarcXchangeToOaiDc.createDcXml" );

        var oaiDcXml = XmlUtil.createDocument( "dc", XmlNamespaces.oai_dc );
        XmlUtil.addNamespace( oaiDcXml, XmlNamespaces.dc );
        XmlUtil.addNamespace( oaiDcXml, XmlNamespaces.xsi );
        XmlUtil.setAttribute( oaiDcXml, 
                              "schemaLocation", 
                              "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd", 
                              XmlNamespaces.xsi );

        var map = new MatchMap( );

        MarcXchangeToOaiDc.addDcTitleElement( oaiDcXml, map );
        MarcXchangeToOaiDc.addDcCreatorOrContributorElements( oaiDcXml, map, "creator" );
        MarcXchangeToOaiDc.addDcPublisherElement( oaiDcXml, map );
        MarcXchangeToOaiDc.addDcCreatorOrContributorElements( oaiDcXml, map, "contributor" );
        MarcXchangeToOaiDc.addDcDateElement( oaiDcXml, map );
        MarcXchangeToOaiDc.addDcIdentifierElement( oaiDcXml, map );
        MarcXchangeToOaiDc.addDcSourceElement( oaiDcXml, map );
        MarcXchangeToOaiDc.addDcLanguageElement( oaiDcXml, map );
        MarcXchangeToOaiDc.addDcRelationElements( oaiDcXml, higherLevelIdentifiers );

        marcRecord.eachFieldMap( map );

        oaiDcXml = XmlUtil.sortElements( oaiDcXml );

        Log.trace( "Leaving MarcXchangeToOaiDc.createDcXml" );

        return oaiDcXml;

    }

    /**
     * Function that puts eachField function on map to create dc:title from field 245 subfield a and/or g.
     *
     * @syntax MarcXchangeToOaiDc.addDcTitleElement( oaiDcXml, map )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {MatchMap} map The map to register handler methods in
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcTitleElement
     */
    function addDcTitleElement( oaiDcXml, map ) {

        Log.trace( "Entering MarcXchangeToOaiDc.createDcTitleElement" );

        map.put( "245", function( field ) {
            var titles = [ ];
            field.eachSubField( "a", function( field, subfield ) {
                titles.push( subfield.value );
            } );
            field.eachSubField( "g", function( field, subfield ) {
                titles.push( subfield.value );
            } );
            var dcTitleValue = titles.join( ". " );
            if ( dcTitleValue ) {
                dcTitleValue = MarcXchangeToOaiDc.__removeUnwantedCharacters( dcTitleValue );
                XmlUtil.appendChildElement( oaiDcXml, "title", XmlNamespaces.dc, dcTitleValue );
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiDc.createDcTitleElement" );

    }

    /**
     * Function that puts eachField function on map to create dc:creator from field 100 or 110
     * or dc:contributor from field 700 and 710, depending on the paramter elementName.
     *
     * @syntax MarcXchangeToOaiDc.addDcCreatorOrContributorElements( oaiDcXml, map, elementName )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {MatchMap} map The map to register handler methods in
     * @param {String} elementName The Name of the element to create, i.e. either 'creator' or 'contributor'
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcCreatorOrContributorElements
     */
    function addDcCreatorOrContributorElements( oaiDcXml, map, elementName ) {

        Log.trace( "Entering MarcXchangeToOaiDc.addDcCreatorOrContributorElements" );

        var personFieldName;
        var corporationFieldName;
        var fieldsDefined = false;

        switch( elementName ) {
            case 'creator':
                personFieldName = "100";
                corporationFieldName = "110";
                fieldsDefined = true;
                break;
            case 'contributor':
                personFieldName = "700";
                corporationFieldName = "710";
                fieldsDefined = true;
                break;
            default:
                break;
        }

        if ( !fieldsDefined ) {
            Log.debug( "MarcXchangeToOaiDc.addDcCreatorOrContributorElements: no element name defined" );
            Log.trace( "Leaving MarcXchangeToOaiDc.addDcCreatorOrContributorElements" );
            return;
        }

        //handles field 100 and 700
        map.put( personFieldName, function( field ) {
            var personName = DanMarc2Util.createPersonNameFromMarcField( field );
            var nameAddition = field.getValue( /e|f/, " " );
            var elementValue = nameAddition ? ( personName + " " + nameAddition ) : personName;
            if ( elementValue ) {
                elementValue = MarcXchangeToOaiDc.__removeUnwantedCharacters( elementValue );
                XmlUtil.appendChildElement( oaiDcXml, elementName, XmlNamespaces.dc, elementValue );
            }
        } );

        //handles field 110 and 710
        map.put( corporationFieldName, function( field ) {
            var elementValue = field.getValue( /a|s/ ) + " " + field.getValue( /e|c|i|k|j/, " " );
            if ( elementValue ) {
                elementValue = MarcXchangeToOaiDc.__removeUnwantedCharacters( elementValue );
                XmlUtil.appendChildElement( oaiDcXml, elementName, XmlNamespaces.dc, elementValue );
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiDc.addDcCreatorOrContributorElements" );

    }


    /**
     * Function that puts eachField function on map to create dc:publisher from field 260 subfield b.
     *
     * @syntax MarcXchangeToOaiDc.addDcPublisherElement( oaiDcXml, map )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {MatchMap} map The map to register handler methods in
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcPublisherElement
     */
    function addDcPublisherElement( oaiDcXml, map ) {

        Log.trace( "Entering MarcXchangeToOaiDc.addDcPublisherElement" );

        map.put( "260", function( field ) {
            var dcPublisherValues = field.getValueAsArray( "b" );
            for ( var i = 0; i < dcPublisherValues.length; i++ ) {
                var dcPublisherValue = dcPublisherValues[ i ];
                if ( "" !== dcPublisherValue && !dcPublisherValue.match( /i samarbejde med|i kommission hos/i ) ) {
                    dcPublisherValue = MarcXchangeToOaiDc.__removeUnwantedCharacters( dcPublisherValue );
                    XmlUtil.appendChildElement( oaiDcXml, "publisher", XmlNamespaces.dc, dcPublisherValue );
                }
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiDc.addDcPublisherElement" );

    }

    /**
     * Function that puts eachField function on map to create dc:date from field 008 subfield a.
     *
     * @syntax MarcXchangeToOaiDc.addDcDateElement( oaiDcXml, map )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {MatchMap} map The map to register handler methods in
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcDateElement
     */
    function addDcDateElement( oaiDcXml, map ) {

        Log.trace( "Entering MarcXchangeToOaiDc.addDcDateElement" );

        map.put( "008", function( field ) {
            var dcDateValue = field.getValue( "a" );
            if ( dcDateValue ) {
                XmlUtil.appendChildElement( oaiDcXml, "date", XmlNamespaces.dc, dcDateValue );
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiDc.addDcDateElement" );

    }

    /**
     * Function that puts eachField function on map to create dc:identifier from fields 001, 021 and 022.
     *
     * @syntax MarcXchangeToOaiDc.addDcIdentifierElement( oaiDcXml, map )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {MatchMap} map The map to register handler methods in
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcIdentifierElement
     */
    function addDcIdentifierElement( oaiDcXml, map ) {

        Log.trace( "Entering MarcXchangeToOaiDc.addDcIdentifierElement" );

        map.put( "001", function( field ) {
            var valueOf001a = field.getValue( "a" );
            var valueOf001b = field.getValue( "b" );
            if ( valueOf001a && valueOf001b ) {
                var identifierValue = valueOf001b + "," + valueOf001a;
                XmlUtil.appendChildElement( oaiDcXml, "identifier", XmlNamespaces.dc, identifierValue );
            }
        } );

        map.put( "021", function( field ) {
            var prefix = "ISBN:";
            field.eachSubField( /a|e/, function( field, subfield ) {
                var identifierValue = prefix + subfield.value;
                XmlUtil.appendChildElement( oaiDcXml, "identifier", XmlNamespaces.dc, identifierValue );
            } );
        } );

        map.put( "022", function( field ) {
            var prefix = "ISSN:";
            field.eachSubField( "a", function( field, subfield ) {
                var identifierValue = prefix + subfield.value;
                XmlUtil.appendChildElement( oaiDcXml, "identifier", XmlNamespaces.dc, identifierValue );
            } );
        } );

        Log.trace( "Leaving MarcXchangeToOaiDc.addDcIdentifierElement" );

    }


    /**
     * Function that puts eachField function on map to create dc:source from field 241 subfield a.
     *
     * @syntax MarcXchangeToOaiDc.addDcSourceElement( oaiDcXml, map )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {MatchMap} map The map to register handler methods in
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcSourceElement
     */
    function addDcSourceElement( oaiDcXml, map ) {

        Log.trace( "Entering MarcXchangeToOaiDc.addDcSourceElement" );

        map.put( "241", function( field ) {
            var dcSourceValue = field.getValue( "a" );
            if ( dcSourceValue ) {
                dcSourceValue = MarcXchangeToOaiDc.__removeUnwantedCharacters( dcSourceValue );
                XmlUtil.appendChildElement( oaiDcXml, "source", XmlNamespaces.dc, dcSourceValue );
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiDc.addDcSourceElement" );

    }

    /**
     * Function that puts eachField function on map to create dc:language from field 008 subfield l.
     *
     * @syntax MarcXchangeToOaiDc.addDcLanguageElement( oaiDcXml, map )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {MatchMap} map The map to register handler methods in
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcLanguageElement
     */
    function addDcLanguageElement( oaiDcXml, map ) {

        Log.trace( "Entering MarcXchangeToOaiDc.addDcLanguageElement" );

        map.put( "008", function( field ) {
            var languageCode = field.getValue( "l" );
            if ( languageCode ) {
                XmlUtil.appendChildElement( oaiDcXml, "language", XmlNamespaces.dc, languageCode );
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiDc.addDcLanguageElement" );

    }

    /**
     * Function that puts eachField function on map to create dc:language from field 008 subfield l.
     *
     * @syntax MarcXchangeToOaiDc.addDcLanguageElement( oaiDcXml, map )
     * @param {Document} oaiDcXml The document to add the dc element to
     * @param {String[]} higherLevelIdentifiers Identifiers of possible section/head records
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.addDcLanguageElement
     */
    function addDcRelationElements( oaiDcXml, higherLevelIdentifiers ) {

        Log.trace( "Entering MarcXchangeToOaiDc.addDcRelationElements" );

        for ( var i = 0; i < higherLevelIdentifiers.length; i++ ) {
            XmlUtil.appendChildElement( oaiDcXml, "relation", XmlNamespaces.dc, higherLevelIdentifiers[ i ] );
        }

        Log.trace( "Leaving MarcXchangeToOaiDc.addDcRelationElements" );

    }

    /**
     * Function that gets an identifier for a higher level record; value from field 001 subfield b
     * concatenated with comma and value from field 014 subfield a.
     *
     * @syntax MarcXchangeToOaiDc.getHigherLevelIdentifier( marcRecord )
     * @param {Record} marcRecord the marc record to get higher level id from
     * @return {String} the higher level identifier, empty string, if no value from field 014a
     * @type {function}
     * @function
     * @name MarcXchangeToOaiDc.getHigherLevelIdentifier
     */
    function getHigherLevelIdentifier( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiDc.getHigherLevelIdentifier" );

        var map = new MatchMap();

        var valueOf001b = "";
        var valueOf014a = "";

        map.put( "001", function( field ) {
            valueOf001b = field.getValue( "b" );
        } );

        map.put( "014", function( field ) {
            if ( !field.exists( "x" ) ) {
                valueOf014a = field.getValue( "a" );
            }
        } );

        marcRecord.eachFieldMap( map );

        var higherLevelId = valueOf014a ? ( valueOf001b + "," + valueOf014a ) : "";

        Log.trace( "Leaving MarcXchangeToOaiDc.getHigherLevelIdentifier" );

        return higherLevelId;

    }

    /**
     * Function that takes the input string and creates a new string similar to the input
     * but without currency sign (skildpadde), sharp parentheses and a space character at the
     * beginning or end of the string.
     *
     *
     * @type {function}
     * @syntax MarcXchangeToOaiDc.__removeUnwantedCharacters( string )
     * @param {String} string The string that should have unwanted characters removed
     * @return {String} A new string without unwanted characters
     * @name MarcXchangeToOaiDc.__removeUnwantedCharacters
     * @function
     */
    function __removeUnwantedCharacters( string ) {

        Log.trace( "Entering MarcXchangeToOaiDc.__removeUnwantedCharacters" );

        var newString = string.replace( /\u00A4|\[|\]|/g, "" );
        newString = newString.trim( ); //remove whitespaces at beginning and end of line

        Log.debug( "Changed string: '", string, "' to: '", newString, "'" );

        Log.trace( "Leaving MarcXchangeToOaiDc.__removeUnwantedCharacters" );

        return newString;

    }



    /**
     * Create a MatchMap, add mapping functions from a specific element method and build the dcxml from a record.
     * To be used when unit testing individual functions that adds elements to xml.
     *
     *
     * @type {function}
     * @syntax MarcXchangeToOaiDc.__callElementMethod( func, xml, record, [elementName] )
     * @param {Function} func The add-element function to call
     * @param {Document} xml The xml to add element to
     * @param {Record} record The record from which to create the element
     * @param {String} [elementName] Name of dc element to create
     * @return {Document} xml with added element
     * @name MarcXchangeToOaiDc.__callElementMethod
     * @function
     */
    function __callElementMethod( func, xml, record, elementName ) {

        var map = new MatchMap();
        func( xml, map, elementName );
        record.eachFieldMap( map );

        return xml;
    }


    Log.info( "Leaving MarcXchangeToOaiDc module" );

    return {
        createDcXml: createDcXml,
        addDcTitleElement: addDcTitleElement,
        addDcCreatorOrContributorElements: addDcCreatorOrContributorElements,
        addDcPublisherElement: addDcPublisherElement,
        addDcDateElement: addDcDateElement,
        addDcIdentifierElement: addDcIdentifierElement,
        addDcSourceElement: addDcSourceElement,
        addDcLanguageElement: addDcLanguageElement,
        addDcRelationElements: addDcRelationElements,
        getHigherLevelIdentifier: getHigherLevelIdentifier,
        __removeUnwantedCharacters: __removeUnwantedCharacters,
        __callElementMethod: __callElementMethod
    }

}();
