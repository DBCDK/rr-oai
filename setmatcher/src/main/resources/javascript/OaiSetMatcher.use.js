/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */


/* global Log, MarcXchange */

/** @file Module with functions used to check which OAI sets a given MarcX record belongs to. */

use( "Log" );
use( "MarcXchange" );

EXPORTED_SYMBOLS = [ 'OaiSetMatcher' ];

/**
 * Module with functions used to check which OAI sets a given MarcX record belongs to.
 *
 * This module contains functions to validate
 *
 * @type {namespace}
 * @namespace
 */

var OaiSetMatcher = function() {

    Log.info( "Entering OaiSetMatcher module" );

    /**
     * Returns the names of OAI sets, in which the given MarcX record 
     * should be contained in.
     *
     * @syntax OaiSetMatcher.getOaiSets( agencyId, marcXrecord )
     * @param {Number} agencyId
     * @param {String} marcXrecord the marcxchange record
     * @return {Array} string array of OAI set names
     * @type {function}
     * @function
     * @name OaiSetMatcher.getOaiSets
     */
    function getOaiSets( agencyId, marcXrecord ) {

        Log.trace( "Entering SetMatcher.getOaiSets" );

        var oaiSets = [];

        //return at this point if agency is not 870970 or 870971 as
        //no records from other agencies should possibly belong to
        //any OAI set.
        if ( 870970 !== agencyId && 870971 !== agencyId ) {
            Log.trace( "Leaving OaiSetMatcher.getOaiSets - agencyId outside of scope: " + agencyId );
            return oaiSets;
        }

        var marcRecord = MarcXchange.marcXchangeToMarcRecord( marcXrecord );
        var recordVariables = OaiSetMatcher.setRecordVariables( agencyId, marcRecord );

        if ( OaiSetMatcher.isPartOfNAT( recordVariables ) ) {
            oaiSets.push( "NAT" );
        }

        if ( OaiSetMatcher.isPartOfBKM( recordVariables ) ) {
            oaiSets.push( "BKM" );
        }

        if ( OaiSetMatcher.isPartOfART( recordVariables ) ) {
            oaiSets.push( "ART" );
        }

        if ( OaiSetMatcher.isPartOfONL( recordVariables ) ) {
            oaiSets.push( "ONL" );
        }

        Log.trace( "Leaving SetMatcher.getOaiSets" );

        return oaiSets;

    }


    /**
     * Function that runs through a marc record and extracts data
     * to use in determining which sets the record should belong to.
     *
     * Returns an object with agencyId (array), valuesOf009g (array),
     * valuesOf014x (array), codesIn032a (array), codesIn032x (array)
     * and exist856u (boolean).
     *
     *
     * @syntax OaiSetMatcher.setRecordVariables( agencyId, record )
     * @param {Number} agencyId The agency that the record belongs to
     * @param {Record} record The marc record to extract data from
     * @return {Object}
     * @type {function}
     * @function
     * @name OaiSetMatcher.setRecordVariables
     */
    function setRecordVariables( agencyId, record ) {

        Log.trace( "Entering OaiSetMatcher.setRecordVariables" );

        var recordVariables = {
            agencyId: agencyId,
            valuesOf009g : [ ],
            valuesOf014x : [ ],
            codesIn032a : [ ],
            codesIn032x : [ ],
            exist856u : false
        };

        var map = new MatchMap( );

        map.put( "009", function( field ) {
            recordVariables.valuesOf009g = field.getValueAsArray( "g" );
        } );

        map.put( "014", function( field ) {
            recordVariables.valuesOf014x = field.getValueAsArray( "x" );
        } );

        map.put( "032", function( field ) {
            field.eachSubField( "a", function( field, subfield ) {
                var code = subfield.value.trim().replace( /[0-9]/g, "" );
                recordVariables.codesIn032a.push( code );
            } );
            field.eachSubField( "x", function( field, subfield ) {
                var code = subfield.value.trim().replace( /[0-9]/g, "" );
                recordVariables.codesIn032x.push( code );
            } );
        } );

        map.put( "856", function( field ) {
            if ( field.getValue( "u" ) ) {
                recordVariables.exist856u = true;
            }
        } );

        record.eachFieldMap( map );

        Log.trace( "Leaving OaiSetMatcher.setRecordVariables" );

        return recordVariables;

    }


    /**
     * Function that checks if recordVariables match criteria for including the
     * record in the 'ART' set.
     *
     * Members of 'ART' are records from 870971 which do not contain
     * 'ANM' in field 014 subfield x
     *
     * @syntax OaiSetMatcher.isPartOfART( recordVariables )
     * @param {Object} recordVariables An object with necessary variables extracted from the marc record
     * @return {Boolean} true if the record should be part of ART set, otherwise false
     * @type {function}
     * @function
     * @name OaiSetMatcher.isPartOfART
     */
    function isPartOfART( recordVariables ) {

        Log.trace( "Entering OaiSetMatcher.isPartOfART" );

        var result = false;

        if ( 870971 !== recordVariables.agencyId ) {
            Log.trace( "Leaving OaiSetMatcher.isPartOfART" );
            return result;
        }

        if ( -1 === recordVariables.valuesOf014x.indexOf( "ANM" ) ) {
            result = true;
        }

        Log.trace( "Leaving OaiSetMatcher.isPartOfART" );

        return result;
    }

    /**
     * Function that checks if recordVariables match criteria for including the
     * record in the 'BKM' set.
     *
     * Members of 'BKM' are records from 870970 which contains at least one of a
     * defined set of codes in field 032x.
     *
     * @syntax OaiSetMatcher.isPartOfBKM( recordVariables )
     * @param {Object} recordVariables An object with necessary variables extracted from the marc record
     * @return {Boolean} true if the record should be part of BKM set, otherwise false
     * @type {function}
     * @function
     * @name OaiSetMatcher.isPartOfBKM
     */
    function isPartOfBKM( recordVariables ) {
        
        Log.trace( "Entering OaiSetMatcher.isPartOfBKM" );

        var result = false;
        
        if ( 870970 !== recordVariables.agencyId ) {
            Log.trace( "Leaving SetMatcher.isPartOfBKM" );
            return result;
        }

        var numberOfCodesIn032x = recordVariables.codesIn032x.length;

        for ( var i = 0; i < numberOfCodesIn032x; i++ ) {
            var code = recordVariables.codesIn032x[ i ];
            if ( code.match( /^((BK[MRX])|(SF.)|(AC.)|(INV)|(UTI)|(NET))/i ) ) {
                result = true;
                break;
            }
        }
        
        Log.trace( "Leaving OaiSetMatcher.isPartOfBKM" );

        return result;
    }

    /**
     * Function that checks if recordVariables match criteria for including the
     * record in the 'NAT' set.
     *
     * Members of 'NAT' are records from 870970 and 870971 which have a subfield 'a'
     * in field 032 (not matter what value it contains).
     *
     * @syntax OaiSetMatcher.isPartOfNAT( recordVariables )
     * @param {Object} recordVariables An object with necessary variables extracted from the marc record
     * @return {Boolean} true if the record should be part of NAT set, otherwise false
     * @type {function}
     * @function
     * @name OaiSetMatcher.isPartOfNAT
     */
    function isPartOfNAT( recordVariables ) {

        Log.trace( "Entering OaiSetMatcher.isPartOfNAT" );

        var result = false;

        if ( 870970 !== recordVariables.agencyId && 870971 !== recordVariables.agencyId ) {
            Log.trace( "Leaving OaiSetMatcher.isPartOfNAT" );
            return result;
        }

        if ( 0 < recordVariables.codesIn032a.length ) {
            result = true;
        }

        Log.trace( "Leaving OaiSetMatcher.isPartOfNAT" );

        return result;

    }

    /**
     * Function that checks if recordVariables match criteria for including the
     * record in the 'ONL' set.
     *
     * Members of 'ONL' are records from 870970 which matches one of the following:
     * * 1. have at least one of the codes DBF|DPF|BKM|DAT|NEP|SNE|IDU in field 032 AND
     * * at the same time have either 'xe' in 009g OR a field 856 with subfield u.
     * * 2. have code 'DAT' AND at least one of the codes IDO|IDP|NEP|NET|SNE in field 032.
     *
     * @syntax OaiSetMatcher.isPartOfONL( recordVariables )
     * @param {Object} recordVariables An object with necessary variables extracted from the marc record
     * @return {Boolean} true if the record should be part of ONL set, otherwise false
     * @type {function}
     * @function
     * @name OaiSetMatcher.isPartOfONL
     */
    function isPartOfONL( recordVariables ) {

        Log.trace( "Entering OaiSetMatcher.isPartOfONL" );

        var result = false;

        if ( 870970 !== recordVariables.agencyId ) {
            Log.trace( "Leaving SetMatcher.isPartOfONL" );
            return result;
        }

        function catCodeMatch( allCatalogueCodes, codeCheckRegEx ) {
            var catCodeMatch = false;
            for ( var i = 0; i < allCatalogueCodes.length; i++ ) {
                if ( allCatalogueCodes[ i ].match( codeCheckRegEx ) ) {
                    catCodeMatch = true;
                    break;
                }
            }
            return catCodeMatch;
        }

        var isOnlineMaterialType = ( -1 < recordVariables.valuesOf009g.indexOf( 'xe' ) );
        var hasOnlineLocalization = recordVariables.exist856u;  //volume records will not match 'isOnlineMaterialType'

        var allCatalogueCodes = recordVariables.codesIn032a.concat( recordVariables.codesIn032x );

        var codeCheckRegEx = /DBF|DPF|BKM|DAT|NEP|SNE|IDU/i;

        if ( ( isOnlineMaterialType || hasOnlineLocalization ) && catCodeMatch( allCatalogueCodes, codeCheckRegEx ) ) {
            result = true;
            Log.trace( "Leaving OaiSetMatcher.isPartOfONL" );
            return result;
        }

        var hasDATcode = ( -1 < allCatalogueCodes.indexOf( 'DAT' ) );
        codeCheckRegEx = /IDO|IDP|NEP|NET|SNE/i;

        if ( hasDATcode && catCodeMatch( allCatalogueCodes, codeCheckRegEx ) ) {
            result = true;
        }

        Log.trace( "Leaving OaiSetMatcher.isPartOfONL" );

        return result;
    }


    Log.info( "Leaving OaiSetMatcher module" );

    return {
        getOaiSets: getOaiSets,
        setRecordVariables: setRecordVariables,
        isPartOfART: isPartOfART,
        isPartOfBKM: isPartOfBKM,
        isPartOfNAT: isPartOfNAT,
        isPartOfONL: isPartOfONL
    };

}();
