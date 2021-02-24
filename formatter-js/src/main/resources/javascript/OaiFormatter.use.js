/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

/* global XmlUtil, MarcXchangeToOaiMarcX, MarcXchangeToOaiDc, Log, XmlNamespaces */

/** @file Module that formats MarcX. */

use( "DanMarc2Util" );
use( "Log" );
use( "XmlUtil" );
use( "MarcXchangeToOaiDc" );
use( "MarcXchangeToOaiMarcX" );

EXPORTED_SYMBOLS = [ 'OaiFormatter' ];

/**
 * Module with functions that creates simple Dublin Core elements for OAI harvest of records from RawRepo.
 *
 * This module contains functions to create dc record
 *
 * @type {namespace}
 * @namespace
 */

var OaiFormatter = function() {

    Log.info( "Entering OaiFormatter module" );


    /**
     * Formats MarcX records, either producing a Dublin Core record
     * or MarcX record(s)
     * 
     * @function
     * @type {function}
     * @param {MarcXChangeWrapper[]} records Array consisting of record, and its 
     * ancestors (ordered from closest ancestor). Array items are instances of the 
     * MarcXChangeWrapper Java class.
     * 
     * Example use of records:
     * var head = records[2] // Assuming records contain volume, section and head
     * var content = head.content  // The actual xml
     * var children = head.children;   
     * for( var i = 0; i < children.length; i++ ) {
     *      children[i].recId;
     *      children[i].agencyId;
     * }
     * 
     * @param {String} format The format to return
     * @param {String[]} allowedSets Names of allowed OAI sets
     * @returns {String} DC or MarcX record(s)
     * @name OaiFormatter.formatRecords
     */
    function formatRecords( records, format, allowedSets ) {

        //lowercasing to make matching easier and avoid errors if input changes
        for ( var i = 0; i < allowedSets.length; i++ ) {
            allowedSets[ i ] = allowedSets[ i ].toLowerCase();
        }

        var marcRecords;
        var includeField015 = false;

        switch( format ) {

            case 'oai_dc':
                marcRecords = OaiFormatter.convertXmlRecordStringsToMarcObjects( records, includeField015 );
                var higherLevelIdentifiers = [ ];
                for ( var k = 0; k < marcRecords.length - 1; k++ ) {
                    var higherLevelId = MarcXchangeToOaiDc.getHigherLevelIdentifier( marcRecords[ k ] );
                    higherLevelIdentifiers.push( higherLevelId );
                }
                // Format the first record (if more records, the first one is volume)
                return XmlUtil.toXmlString( MarcXchangeToOaiDc.createDcXml( marcRecords[0], higherLevelIdentifiers ) );

            case 'marcx':
                includeField015 = true;
                marcRecords = OaiFormatter.convertXmlRecordStringsToMarcObjects( records, includeField015 );
                var marcXCollection = XmlUtil.createDocument( "collection", XmlNamespaces.marcx );
                var bkmRecordAllowed = ( allowedSets.indexOf( 'bkm' ) > -1 );
                
                // Traverse from head to volume
                for ( var j = marcRecords.length - 1; j >= 0; j-- ) {
                    var marcRecord = MarcXchangeToOaiMarcX.removeLocalFieldsIfAny( marcRecords[ j ] );
                    marcRecord = MarcXchangeToOaiMarcX.removeField665( marcRecord ); //Search US#2373. Remove this call when 665 is official field.
                    if ( !bkmRecordAllowed ) {
                        marcRecord = MarcXchangeToOaiMarcX.removeBkmFields( marcRecord );
                        marcRecord = MarcXchangeToOaiMarcX.removeSubfield241u( marcRecord ); // US#OAI-9
                    }
                    marcRecord = MarcXchangeToOaiMarcX.removeLocalSubfieldsIfAny( marcRecord );
                    var marcXDoc = MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType( marcRecord );
                    XmlUtil.appendChild( marcXCollection, marcXDoc );
                }                
                
                return XmlUtil.toXmlString( marcXCollection );
            
            default:
                throw Error( "Format: " + format + " not allowed" );
        }    
    }

    /**
     * Converts xml marc records to marc record objects, adding fields 015 with identifiers
     * of children for the record if includeField015 is set to true.
     *
     * @function
     * @syntax OaiFormatter.convertXmlRecordStringsToMarcObjects( records, includeField015 )
     * @type {function}
     * @param {MarcXChangeWrapper[]} records Array consisting of record, and its
     * ancestors (ordered from closest ancestor). Array items are instances of the
     * MarcXChangeWrapper Java class.
     *
     * Example use of records:
     * var head = records[2] // Assuming records contain volume, section and head
     * var content = head.content  // The actual xml
     * var children = head.children;
     * for( var i = 0; i < children.length; i++ ) {
     *      children[i].recId;
     *      children[i].agencyId;
     * }
     * @param {Boolean} includeField015 true if fields 015 should be added to the record
     * @returns {Record[]} new array of the same records in the same order but as Record objects
     * @name OaiFormatter.convertXmlRecordStringsToMarcObjects
     */
    function convertXmlRecordStringsToMarcObjects( records, includeField015 ) {

        Log.trace( "Entering OaiFormatter.convertXmlRecordStringsToMarcObjects" );

        var recordObjects = [ ];

        var __identifiersForField015 = function( record ) {
            var childrenObjects = record.children;
            var childrenIdentifiers = [];
            for ( var j = 0; j < childrenObjects.length; j++ ) {
                childrenIdentifiers.push( childrenObjects[ j ].getBibliographicRecordId() );
            }
            return childrenIdentifiers;
        };

        var __addFields015ToRecordObject = function( recordObject, childrenIdentifiers ) {
            for ( var k = 0; k < childrenIdentifiers.length; k++ ) {
                var field015 = new Field( "015", "00" );
                field015.append( "a", childrenIdentifiers[ k ] );
                recordObject.append( field015 );
            }
        };

        for ( var i = 0; i < records.length; i++ ) {
            var record = records[ i ];
            var recordObject = MarcXchange.marcXchangeToMarcRecord( record.content );
            if ( includeField015 ) {
                __addFields015ToRecordObject( recordObject, __identifiersForField015( record ) );
                recordObject = DanMarc2Util.sortFields( recordObject );
            }
            recordObjects.push( recordObject );
        }

        Log.trace( "Leaving OaiFormatter.convertXmlRecordStringsToMarcObjects" );

        return recordObjects;

    }


    /**
     * Used for validating format
     * 
     * @returns {Array} list of allowed formats
     */
    function getAllowedFormats( ) {
        return [ 'oai_dc', 'marcx' ];
    }


    Log.info( "Leaving OaiFormatter module" );

    return {
        formatRecords: formatRecords,
        convertXmlRecordStringsToMarcObjects: convertXmlRecordStringsToMarcObjects,
        getAllowedFormats: getAllowedFormats
    };
}();
