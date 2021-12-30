/*
 * Copyright (C) 2017 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-rawrepo-oai-formatter
 *
 * dbc-rawrepo-oai-formatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-rawrepo-oai-formatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* global Log, OaiSetMatcher */

use( "Log" );
use( "OaiSetMatcher" );

/**
 * Determines ia an agencyIs is elibible for setmatching.
 *
 * @param {String} agencyId
 * @returns {Boolean} If the record could be setmatched
 */
var oaiIsEligible = function( agencyId ) {
    return OaiSetMatcher.isEligible( agencyId );
};

/**
 * Determines which sets a MarcX record belongs to.
 * 
 * @param {String} agencyId
 * @param {String} content The MarcX document
 * @returns {Array} sets represented as array of strings 
 */
var oaiSetMatcher = function( agencyId, content ) {    
    return OaiSetMatcher.getOaiSets( agencyId, content );
};
