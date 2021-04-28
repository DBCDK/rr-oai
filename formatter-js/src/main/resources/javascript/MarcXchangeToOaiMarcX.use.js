/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

/** @file Module that creates marcXchange record for OAI harvested records. */

use( "Log" );
use( "Marc" );
use( "MarcXchange" );

EXPORTED_SYMBOLS = [ 'MarcXchangeToOaiMarcX' ];

/**
 * Module with functions that removes certain fields in marc records
 * for OAI harvest of records from RawRepo.
 *
 * This module contains functions to create modified marcxchange record
 *
 * @type {namespace}
 * @namespace
 */

var MarcXchangeToOaiMarcX = function() {

    Log.info( "Entering MarcXchangeToOaiMarcX module" );


    /**
     * Function that is entry to create a marcxchange record with the right
     * record type set (BibliographicVolume/BibliographicSection/BibliographicMain or
     * Bibliographic) according to the actual type of the record.
     *
     * @syntax MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType( marcRecord )
     * @param {Record} marcRecord the marc record to create modified record from
     * @return {Document} the created marcXchange record with the right record type
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType
     */
    function createMarcXmlWithRightRecordType( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType" );

        var recordType = MarcXchangeToOaiMarcX.getRecordType( marcRecord );
        var marcXml = MarcXchange.marcRecordToMarcXchange( marcRecord, "danMARC2", recordType );

        Log.trace( "Leaving MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType" );

        return marcXml;

    }


    /**
     * Function that clones the input record and removes fields created for BKM
     * from the cloned record.
     *
     * @syntax MarcXchangeToOaiMarcX.removeBkmFields( marcRecord )
     * @param {Record} marcRecord the marc record to remove BKM fields from
     * @return {Record} new record without fields that belong to BKM
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.removeBkmFields
     */
    function removeBkmFields( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.removeBkmFields" );

        var modifiedRecord = marcRecord.clone( );

        modifiedRecord.removeAll( "504" );
        modifiedRecord.removeAll( "600" );
        modifiedRecord.removeAll( "610" );
        modifiedRecord.removeAll( "666" );
        modifiedRecord.removeAll( "990" );
        modifiedRecord.removeAll( "991" );

        var fieldMatcher = {
            matchField: function( modifiedRecord, field ) {
                //fields 500-599 should be removed if they have subfield & with value 1
                return ( field.name.match( /^5/ ) && "1" === field.getValue( "&" ) );
            }
        };
        modifiedRecord.removeWithMatcher( fieldMatcher );

        Log.trace( "Leaving MarcXchangeToOaiMarcX.removeBkmFields" );

        return modifiedRecord;

    }


    /**
     * Function that removes local fields in the marc record
     * (local fields starts with a letter instead of a number) if there
     * is any.
     *
     * @syntax MarcXchangeToOaiMarcX.removeLocalFieldsIfAny( marcRecord )
     * @param {Record} marcRecord the marc record to check for local fields
     * @return {Record} a new record without local fields
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.removeLocalFieldsIfAny
     */
    function removeLocalFieldsIfAny( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.removeLocalFieldsIfAny" );

        var modifiedRecord = marcRecord.clone();

        var fieldMatcher = {
            matchField: function( modifiedRecord, field ) {
                //fields starting with a letter should be removed
                return ( field.name.match( /^[a-z]/ ) );
            }
        };
        modifiedRecord.removeWithMatcher( fieldMatcher );

        Log.trace( "Leaving MarcXchangeToOaiMarcX.removeLocalFieldsIfAny" );

        return modifiedRecord;

    }


    /**
     * Function that removes local subfields (&) in the marc record if there
     * is any.
     *
     * @syntax MarcXchangeToOaiMarcX.removeLocalSubfieldsIfAny( marcRecord )
     * @param {Record} marcRecord the marc record to check for local fields
     * @return {Record} a new record without local fields
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.removeLocalSubfieldsIfAny
     */
    function removeLocalSubfieldsIfAny( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.removeLocalSubfieldsIfAny" );

        var modifiedRecord = marcRecord.clone();

        modifiedRecord.eachField( /./, function( field ) {
            var subfieldMatcher = {
                matchSubField: function( field, subfield ) {
                    return ( subfield.name === '&' );  //bug21248
                }
            };
            field.removeWithMatcher( subfieldMatcher );
        } );

        Log.trace( "Leaving MarcXchangeToOaiMarcX.removeLocalSubfieldsIfAny" );

        return modifiedRecord;

    }

    /**
     * Function that removes subfield 241u in the marc record if it exists.
     * Search US#OAI-9
     * @syntax MarcXchangeToOaiMarcX.removeSubfield241u( marcRecord )
     * @param {Record} marcRecord the marc record to check for subfield 241u
     * @return {Record} a new record without subfield 241u
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.removeSubfield241u
     */
    function removeSubfield241u( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.removeSubfield241u" );

        var modifiedRecord = marcRecord.clone();

        modifiedRecord.eachField( /./, function( field ) {
            if( field.name === "241") {
                var subfieldMatcher = {
                    matchSubField: function (field, subfield) {
                        return (subfield.name === 'u');
                    }
                };
                field.removeWithMatcher(subfieldMatcher);
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiMarcX.removeSubfield241u" );

        return modifiedRecord;
    }

    /**
     * Function that removes subfield 0 in 5xx-fields in the marc record if it exists.
     * Search US#OAI-10
     * @syntax MarcXchangeToOaiMarcX.removeSubfield0of5XXFields( marcRecord )
     * @param {Record} marcRecord the marc record to check for subfield 5XX *0
     * @return {Record} a new record without subfields 0 in 5XX-fields
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.removeSubfield0of5XXFields
     */
    function removeSubfield0of5XXFields( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.removeSubfield0of5XXFields" );

        var modifiedRecord = marcRecord.clone();

        modifiedRecord.eachField( /./, function( field ) {
            if( field.name.length === 3 && field.name.startsWith("5", 0)) {
                var subfieldMatcher = {
                    matchSubField: function (field, subfield) {
                        return (subfield.name === '0');
                    }
                };
                field.removeWithMatcher(subfieldMatcher);
            }
        } );

        Log.trace( "Leaving MarcXchangeToOaiMarcX.removeSubfield0of5XXFields" );

        return modifiedRecord;
    }

    /**
     * Function that removes field 665 from the marc record if it exists.
     * This is a function for temporary use - until field 665 is an official
     * danMARC2 field. Search US #2373.
     *
     * @syntax MarcXchangeToOaiMarcX.removeField665( marcRecord )
     * @param {Record} marcRecord the marc record to check for field 665
     * @return {Record} a new record without field 665
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.removeField665
     */
    function removeField665( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.removeField665" );

        var modifiedRecord = marcRecord.clone();

        var fieldMatcher = {
            matchField: function( modifiedRecord, field ) {
                return ( "665" === field.name );
            }
        };
        modifiedRecord.removeWithMatcher( fieldMatcher );

        Log.trace( "Leaving MarcXchangeToOaiMarcX.removeField665" );

        return modifiedRecord;

    }


    /**
     * Function finds the record type to send to function MarcXchange.marcRecordToMarcXchange,
     * either "Bibliographic", "BibliographicMain", "BibliographicSection" or "BibliographicVolume".
     *
     * @syntax MarcXchangeToOaiMarcX.getRecordType( marcRecord )
     * @param {Record} marcRecord the record to get type for
     * @return {String} the type to send to MarcXchange.marcRecordToMarcXchange function
     * @type {function}
     * @function
     * @name MarcXchangeToOaiMarcX.getRecordType
     */
    function getRecordType( marcRecord ) {

        Log.trace( "Entering MarcXchangeToOaiMarcX.getRecordType" );

        var recordTypeCode = marcRecord.getValue( "004", "a" );
        var recordType;
        switch ( recordTypeCode ) {
            case "h":
                recordType = "BibliographicMain";
                break;
            case "s":
                recordType = "BibliographicSection";
                break;
            case "b":
                recordType = "BibliographicVolume";
                break;
            default:
                recordType = "Bibliographic";
                break;
        }

        Log.trace( "Entering MarcXchangeToOaiMarcX.getRecordType" );

        return recordType;
    }

    Log.info( "Leaving MarcXchangeToOaiMarcX module" );

    return {
        createMarcXmlWithRightRecordType: createMarcXmlWithRightRecordType,
        removeBkmFields: removeBkmFields,
        removeLocalFieldsIfAny: removeLocalFieldsIfAny,
        removeLocalSubfieldsIfAny: removeLocalSubfieldsIfAny,
        removeSubfield241u: removeSubfield241u,
        removeField665: removeField665,
        removeSubfield0of5XXFields: removeSubfield0of5XXFields,
        getRecordType: getRecordType
    }

}();
