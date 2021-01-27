/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

/* global Assert, OaiSetMatcher, UnitTest */

/** @file Module that contains unit tests for functions in OaiSetMatcher module */

use( "OaiSetMatcher" );
use( "UnitTest" );

UnitTest.addFixture( "test OaiSetMatcher.getOaiSets", function( ) {

    var recordString = (
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n    2200000   4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23645564</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
                '<marcx:subfield code="c">20160525133413</marcx:subfield>' +
                '<marcx:subfield code="d">20010824</marcx:subfield>' +
                '<marcx:subfield code="f">a</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">n</marcx:subfield>' +
                '<marcx:subfield code="a">e</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="009">' +
                '<marcx:subfield code="a">a</marcx:subfield>' +
                '<marcx:subfield code="g">xx</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="032">' +
                '<marcx:subfield code="a">DBF200338</marcx:subfield>' +
                '<marcx:subfield code="x">SFD200338</marcx:subfield>' +
                '<marcx:subfield code="x">ACC200134</marcx:subfield>' +
                '<marcx:subfield code="x">ACC200332</marcx:subfield>' +
                '<marcx:subfield code="x">DAT201623</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="100">' +
                '<marcx:subfield code="a">Murakami</marcx:subfield>' +
                '<marcx:subfield code="h">Haruki</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
            '<marcx:subfield code="a">Traekopfuglens kroenike</marcx:subfield>' +
            '<marcx:subfield code="e">Haruki Murakami</marcx:subfield>' +
            '<marcx:subfield code="f">oversat af Mette Holm</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>'
    );

    var expected = [ "NAT", "BKM" ];

    var actual = OaiSetMatcher.getOaiSets( 870970, recordString );

    Assert.equalValue( "Record is contained in NAT and BKM", actual, expected );

    recordString = (
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n    2200000   4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">36007761</marcx:subfield>' +
        '<marcx:subfield code="b">870971</marcx:subfield>' +
        '<marcx:subfield code="c">20160503113220</marcx:subfield>' +
        '<marcx:subfield code="d">20140409</marcx:subfield>' +
        '<marcx:subfield code="f">a</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">i</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="009">' +
        '<marcx:subfield code="a">a</marcx:subfield>' +
        '<marcx:subfield code="g">xe</marcx:subfield>' +
        '<marcx:subfield code="g">xx</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="016">' +
        '<marcx:subfield code="a">49375395</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="032">' +
        '<marcx:subfield code="a">ABU201619</marcx:subfield>' +
        '<marcx:subfield code="a">DAR201416</marcx:subfield>' +
        '<marcx:subfield code="x">ARK201619</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>'
    );

    expected = [ "NAT", "ART" ];

    actual = OaiSetMatcher.getOaiSets( 870971, recordString );

    Assert.equalValue( "Record is contained in ART and NAT", actual, expected );


    recordString = (
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n    2200000   4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">23645564</marcx:subfield>' +
        '<marcx:subfield code="b">710100</marcx:subfield>' +
        '<marcx:subfield code="c">20160525133413</marcx:subfield>' +
        '<marcx:subfield code="d">20010824</marcx:subfield>' +
        '<marcx:subfield code="f">a</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">e</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="009">' +
        '<marcx:subfield code="a">a</marcx:subfield>' +
        '<marcx:subfield code="g">xx</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="032">' +
        '<marcx:subfield code="a">DBF200338</marcx:subfield>' +
        '<marcx:subfield code="x">SFD200338</marcx:subfield>' +
        '<marcx:subfield code="x">ACC200134</marcx:subfield>' +
        '<marcx:subfield code="x">ACC200332</marcx:subfield>' +
        '<marcx:subfield code="x">DAT201623</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="100">' +
        '<marcx:subfield code="a">Murakami</marcx:subfield>' +
        '<marcx:subfield code="h">Haruki</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="245">' +
        '<marcx:subfield code="a">Traekopfuglens kroenike</marcx:subfield>' +
        '<marcx:subfield code="e">Haruki Murakami</marcx:subfield>' +
        '<marcx:subfield code="f">oversat af Mette Holm</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>'
    );

    expected = [ ];

    actual = OaiSetMatcher.getOaiSets( 710100, recordString );

    Assert.equalValue( "Record not included in any sets because agencyId is not right", actual, expected );


    recordString = (
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n    2200000   4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">26848806</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '<marcx:subfield code="c">20161216102123</marcx:subfield>' +
        '<marcx:subfield code="d">20070725</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">e</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="009">' +
        '<marcx:subfield code="a">a</marcx:subfield>' +
        '<marcx:subfield code="g">xe</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="032">' +
        '<marcx:subfield code="a">IDO200731</marcx:subfield>' +
        '<marcx:subfield code="x">NET200731</marcx:subfield>' +
        '<marcx:subfield code="x">ACC200730</marcx:subfield>' +
        '<marcx:subfield code="x">DIT990737</marcx:subfield>' +
        '<marcx:subfield code="x">DAT201701</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="100">' +
        '<marcx:subfield code="a">Madsen</marcx:subfield>' +
        '<marcx:subfield code="h">Svend Åge</marcx:subfield>' +
        '<marcx:subfield code="c">f. 1939</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="245">' +
        '<marcx:subfield code="a">De ¤gode mennesker i Århus</marcx:subfield>' +
        '<marcx:subfield>Læselysten</marcx:subfield>' +
        '<marcx:subfield code="c">dobbeltroman</marcx:subfield>' +
        '<marcx:subfield code="e">Svend Åge Madsen</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>'
    );

    expected = [ "NAT", "BKM", "ONL" ];

    actual = OaiSetMatcher.getOaiSets( 870970, recordString );

    Assert.equalValue( "Record is part of NAT, BKM and ONL sets", actual, expected );
    
    recordString = (
        '<record xmlns="info:lc/xmlns/marcxchange-v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd">' +
        '    <leader>00000n    2200000   4500</leader>' +
        '    <datafield ind1="0" ind2="0" tag="001">' +
        '        <subfield code="a">47666813</subfield>' +
        '        <subfield code="b">870970</subfield>' +
        '        <subfield code="c">20200129083614</subfield>' +
        '        <subfield code="d">20200121</subfield>' +
        '        <subfield code="f">a</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="004">' +
        '        <subfield code="r">n</subfield>' +
        '        <subfield code="a">e</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="008">' +
        '        <subfield code="t">m</subfield>' +
        '        <subfield code="u">r</subfield>' +
        '        <subfield code="a">2019</subfield>' +
        '        <subfield code="z">2019</subfield>' +
        '        <subfield code="b">fr</subfield>' +
        '        <subfield code="d">y</subfield>' +
        '        <subfield code="l">per</subfield>' +
        '        <subfield code="v">0</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="009">' +
        '        <subfield code="a">a</subfield>' +
        '        <subfield code="g">xx</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="021">' +
        '        <subfield code="e">9782366124194</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="100">' +
        '        <subfield code="a">Najafī</subfield>' +
        '        <subfield code="h">Shāhīn</subfield>' +
        '        <subfield code="4">aut</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="245">' +
        '        <subfield code="a">Bītriks</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="260">' +
        '        <subfield code="a">Paris</subfield>' +
        '        <subfield code="b">Nashr-i Nākujā</subfield>' +
        '        <subfield code="c">2019</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="300">' +
        '        <subfield code="a">152 sider</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="504">' +
        '        <subfield code="a">Eksil sanger, musiker og komposer, Shāhīn Najafī skriver om kunst med fokus på begrebet &quot;overlevelse&quot;</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="532">' +
        '        <subfield code="a">Med litteraturhenvisninger</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="652">' +
        '        <subfield code="m">70.1</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="666">' +
        '        <subfield code="f">kunst</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="666">' +
        '        <subfield code="f">livet</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="666">' +
        '        <subfield code="f">kultur</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="666">' +
        '        <subfield code="f">musik</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="666">' +
        '        <subfield code="f">værdier</subfield>' +
        '    </datafield>' +
        '    <datafield ind1="0" ind2="0" tag="996">' +
        '        <subfield code="a">700300</subfield>' +
        '    </datafield>' +
        '</record>' );

    expected = [ "FDEPOT" ];

    actual = OaiSetMatcher.getOaiSets( 870970, recordString );

    Assert.equalValue( "Record is part of BCI sets", actual, expected );
    

} );

UnitTest.addFixture( "test OaiSetMatcher.setRecordVariables", function( ) {

    var record = new Record( );
    record.fromString(
        '001 00 *a23645564 *b870970 *c20160525133413 *d20010824 *fa\n' +
        '004 00 *rn *ae\n' +
        '009 00 *aa *gxx\n' +
        '032 00 *aDBF200338 *xSFD200338 *xACC200134 *xACC200332 *xDAT201623\n' +
        '100 00 *aMurakami *hHaruki\n' +
        '245 00 *aTrækopfuglens krønike *eHaruki Murakami *foversat af Mette Holm'
    );

    var actual = OaiSetMatcher.setRecordVariables( 870970, record );

    var expected = {
        agencyId: 870970,
        valuesOf001b : [ '870970' ],
        valuesOf009g : [ 'xx' ],
        valuesOf014x : [ ],
        codesIn032a : [ 'DBF' ],
        codesIn032x : [ 'SFD', 'ACC', 'ACC', 'DAT' ],
        exist856u : false,
        valuesOf996a : [ ]        
    };

    Assert.equalValue( "set record variables for record without field 014", actual, expected );

    record = new Record( );
    record.fromString(
        '001 00 *a80204612 *b870971 *c20140321154529 *fa *oc\n' +
        '004 00 *rn *ai\n' +
        '009 00 *aa *gxx\n' +
        '014 00 *a80204590 *xANM\n' +
        '016 00 *a03243095\n' +
        '032 00 *aANU201413 *aDAN199105\n' +
        '245 00 *a[Anmeldelse]'
    );

    actual = OaiSetMatcher.setRecordVariables( 870971, record );

    expected = {
        agencyId: 870971,
        valuesOf001b : [ '870971' ],
        valuesOf009g : [ 'xx' ],
        valuesOf014x : [ 'ANM' ],
        codesIn032a : [ 'ANU', 'DAN' ],
        codesIn032x : [ ],
        exist856u : false,
        valuesOf996a : [ ]        
    };

    Assert.equalValue( "set record variables for record with field 014", actual, expected );


    record = new Record();
    record.fromString(
        '001 00*a5 281 864 8*b870970*c20161216100730*d20161209*fa\n' +
        '004 00*rn*ab\n' +
        '008 00*tp*a2014*na*x04*v0\n' +
        '014 00*a2 514 722 7\n' +
        '021 00*e9789289327756*bEPUB\n' +
        '032 00*aDBF201701\n' +
        '041 00*aeng*dnor\n' +
        '245 00*g2014:529*aRoad to regulation of endocrine disruptors and combination effects\n' +
        '260 00*c2014\n' +
        '512 00*aKan også købes i EPUB-format\n' +
        '700 00*0*aPetersen*hKarina*cf. 1983*4aut\n' +
        '700 00*0*aLindeman*hBirgitte*4aut\n' +
        '856 00*zAdgangsmåde: Internet*uhttps://www.diva-portal.org/smash/get/diva2:715846/FULLTEXT01.pdf*yPDF-format\n' +
        's10 00*aDBC\n'
    );

    actual = OaiSetMatcher.setRecordVariables( 870970, record );

    expected = {
        agencyId: 870970,
        valuesOf001b : [ '870970' ],
        valuesOf009g : [ ],
        valuesOf014x : [ ],
        codesIn032a : [ 'DBF' ],
        codesIn032x : [ ],
        exist856u : true,
        valuesOf996a : [ ]        
    };

    Assert.equalValue( "set record variables for volume record with field 856", actual, expected );

    record = new Record();
    record.fromString(
        '001 00 *a 47666813 *b 870970 *c 20200129083614 *d 20200121 *f a\n' +
        '004 00 *r n *a e\n' +
        '008 00 *t m *u r *a 2019 *z 2019 *b fr *d y *l per *v 0\n' +
        '009 00 *a a *g xx\n' +
        '021 00 *e 9782366124194\n' +
        '100 00 *a Najafī *h Shāhīn *4 aut\n' +
        '245 00 *a Bītriks\n' +
        '260 00 *a Paris *b Nashr-i Nākujā *c 2019\n' +
        '300 00 *a 152 sider\n' +
        '504 00 *a Eksil sanger, musiker og komposer, Shāhīn Najafī skriver om kunst med fokus på begrebet "overlevelse"\n' +
        '532 00 *a Med litteraturhenvisninger\n' +
        '652 00 *m 70.1\n' +
        '666 00 *f kunst\n' +
        '666 00 *f livet\n' +
        '666 00 *f kultur\n' +
        '666 00 *f musik\n' +
        '666 00 *f værdier\n' +
        '996 00 *a 700300\n' 
    );

    actual = OaiSetMatcher.setRecordVariables( 870970, record );

    expected = {
        agencyId: 870970,
        valuesOf001b : [ '870970' ],
        valuesOf009g : [ 'xx'],
        valuesOf014x : [ ],
        codesIn032a : [ ],
        codesIn032x : [ ],
        exist856u : false,
        valuesOf996a : [ '700300' ]        
    };

    Assert.equalValue( "set record variables for volume record with field 996", actual, expected );

} );


UnitTest.addFixture( "test OaiSetMatcher.isPartOfART (record in ART)", function( ) {

    var record = new Record( );
    record.fromString(
        '001 00 *a36007761 *b870971 *c20160503113220 *d20140409\n' +
        '004 00 *rn *ai\n' +
        '009 00 *aa *gxe *gxx\n' +
        '016 00 *a49375395'
    );

    var recordVariables = OaiSetMatcher.setRecordVariables( 870971, record );

    var expected = true;
    var actual = OaiSetMatcher.isPartOfART( recordVariables );

    Assert.equalValue( "Record is contained in ART set (field 014 with no subfield x)", actual, expected );

    record = new Record( );
    record.fromString(
        '001 00*a3 747 460 6*b870971*c20170804132940*d20170804*fa\n' +
        '004 00*rn*ai\n' +
        '008 00*ta*uf*a2017*bdk*ldan*nb*ran*v0\n' +
        '009 00*aa*gxx*gxe\n' +
        '014 00*a3 746 821 5*xDEB\n' +
        '016 00*a0 336 102 0\n' +
        '032 00*aABU201732*aDAR201732\n' +
        '245 00*a[Debat]*aPalmemordet\n'
    );

    recordVariables = OaiSetMatcher.setRecordVariables( 870971, record );

    expected = true;
    actual = OaiSetMatcher.isPartOfART( recordVariables );

    Assert.equalValue( "Record is contained in ART set (field 014 with subfield x=DEB)", actual, expected );

} );

UnitTest.addFixture( "test OaiSetMatcher.isPartOfART (record NOT in ART)", function( ) {

    var record = new Record( );
    record.fromString(
        '001 00*a3 747 650 1*b870971*c20170808104059*d20170808*fa\n' +
        '004 00*rn*ai\n' +
        '008 00*ta*uf*a2017*bdk*ds*ldan*nb*ran*v0\n' +
        '009 00*aa*gxe*gxx\n' +
        '014 00*a5 335 984 1*xANM\n' +
        '016 00*a0 324 309 5\n' +
        '032 00*aANU201733*aDAN201733\n' +
        '245 00*a[Anmeldelse]\n'

    );
    var recordVariables = OaiSetMatcher.setRecordVariables( 870971, record );

    var expected = false;
    var actual = OaiSetMatcher.isPartOfART( recordVariables );

    Assert.equalValue( "Record is NOT contained in ART set (field 014 with subfield x=ANM)", actual, expected );


    record = new Record( );
    record.fromString(
        '001 00*a2 011 700 1*b870970*c20170808182139*d19921022*fa\n' +
        '004 00*rn*ab\n' +
        '008 00*ts*uf*a1992*dz*d<E5>*ldan*v0\n' +
        '014 00*a5 048 431 9\n' +
        '021 00*a87-89190-23-8*chf.*dkr. 90,00\n' +
        '032 00*aDBF199247*xSFD199247*xB<D8>K199301*xDAT201733\n' +
        '245 00*g[Bind] 5*fredaktion: Jacob Wisby\n'

    );
    recordVariables = OaiSetMatcher.setRecordVariables( 870970, record );

    expected = false;
    actual = OaiSetMatcher.isPartOfART( recordVariables );

    Assert.equalValue( "Record is NOT contained in ART set (record from 870970)", actual, expected );


    record = new Record( );
    record.fromString(
        '001 00 *a87654321 *b870971\n' +
        '014 00 *a12345678\n' +
        '014 00 *a24686420 *xANM'
    );
    recordVariables = OaiSetMatcher.setRecordVariables( 870971, record );

    expected = false;
    actual = OaiSetMatcher.isPartOfART( recordVariables );

    Assert.equalValue( "Record is NOT contained in ART set (record with ANM in at least one field 014)", actual, expected );

} );

UnitTest.addFixture( "test OaiSetMatcher.isPartOfBKM", function( ) {

    var expected = true; //applies for all tests in this Fixture

    var record = new Record( );
    record.fromString(
        '001 00*a2 013 619 7*b870970*c20170227145556*d19921111*fa*oc\n' +
        '004 00*rn*ae\n' +
        '008 00*ts*uf*a1992*bdk*dx*jp*kb*ldan*v0\n' +
        '009 00*aa*bc*gxx\n' +
        '021 00*a87-598-0622-2*cib.*dkr. 198,00\n' +
        '032 00*aDBF199250*xFRD199250*xSFD199250*xBØK199301*xSAN198507*xDAT201710\n' +
        '245 00*aDans med mig*cSebastians 50 største sange og historien bag dem*efortalt af Torben Bille\n'
    );
    var recordVariables = OaiSetMatcher.setRecordVariables( 870970, record );

    var actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    var testName = "Record is contained in BKM set (code SFD in 032x and correct agency)";
    Assert.equalValue( testName, actual, expected );


    recordVariables = {
        agencyId : 870970,
        codesIn032x : [ 'FRD', 'BKX', 'ACC' ]
    };

    actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    testName = "Record is contained in BKM set (code BKX in 032x but not first in array - and correct agency)";
    Assert.equalValue( testName, actual, expected );


    recordVariables = {
        agencyId : 870970,
        codesIn032x : [ 'BKM' ]
    };

    actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    testName = "Record is contained in BKM set (code BKX in 032x - and correct agency)";
    Assert.equalValue( testName, actual, expected );


    recordVariables = {
        agencyId : 870970,
        codesIn032x : [ 'BKR' ]
    };

    actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    testName = "Record is contained in BKM set (code BKR in 032x - and correct agency)";
    Assert.equalValue( testName, actual, expected );


    recordVariables = {
        agencyId : 870970,
        codesIn032x : [ 'ACC' ]
    };

    actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    testName = "Record is contained in BKM set (code ACC in 032x - and correct agency)";
    Assert.equalValue( testName, actual, expected );


    recordVariables = {
        agencyId : 870970,
        codesIn032x : [ 'INV' ]
    };

    actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    testName = "Record is contained in BKM set (code INV in 032x - and correct agency)";
    Assert.equalValue( testName, actual, expected );


    recordVariables = {
        agencyId : 870970,
        codesIn032x : [ 'UTI' ]
    };

    actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    testName = "Record is contained in BKM set (code UTI in 032x - and correct agency)";
    Assert.equalValue( testName, actual, expected );


    recordVariables = {
        agencyId : 870970,
        codesIn032x : [ 'NET' ]
    };

    actual = OaiSetMatcher.isPartOfBKM( recordVariables );
    testName = "Record is contained in BKM set (code NET in 032x - and correct agency)";
    Assert.equalValue( testName, actual, expected );

} );

UnitTest.addFixture( "test OaiSetMatcher.isPartOfBKM (not in set)", function( ) {

    var recordVariables = {
        agencyId : 870971,
        codesIn032x : [ 'ACC' ]
    };

    var expected = false;
    var actual = OaiSetMatcher.isPartOfBKM( recordVariables );

    var testName = "Record is not contained in BKM set (wrong agencyId)";

    Assert.equalValue( testName, actual, expected );


    var record = new Record();
    record.fromString(
        '001 00*a5 298 104 2*b870970*c20170227143342*d20170227*fa\n' +
        '004 00*rn*ab\n' +
        '008 00*tp*då*v0\n' +
        '014 00*a2 573 805 5\n' +
        '021 00*e9788776058449*chf.\n' +
        '032 00*aDBF201711\n' +
        '041 00*aeng\n' +
        '245 00*G2016 07*g2016:07*aThe meso-level interplay of climate and disaster risk management in Vietnam\n'
    );
    recordVariables = OaiSetMatcher.setRecordVariables( 870970, record );

    expected = false;
    actual = OaiSetMatcher.isPartOfBKM( recordVariables );

    testName = "Record is not contained in BKM set (no complying code in 032x)";

    Assert.equalValue( testName, actual, expected );

} );


UnitTest.addFixture( "test OaiSetMatcher.isPartOfNAT (record in NAT)", function( ) {

    var recordVariables = {
        agencyId : 870970,
        codesIn032a : [ 'DBF' ],
        codesIn032x : [ 'SFD', 'ACC', 'ACC', 'DAT' ]
    };

    var expected = true;
    var actual = OaiSetMatcher.isPartOfNAT( recordVariables );

    var testName = "Record is contained in NAT set (record from 870970 with value in 032a)";

    Assert.equalValue( testName, actual, expected );

    recordVariables = {
        agencyId : 870971,
        codesIn032a : [ 'ABU' ],
        codesIn032x : [ 'ACC' ]
    };

    expected = true;
    actual = OaiSetMatcher.isPartOfNAT( recordVariables );

    testName = "Record is contained in NAT set (record from 870971 with value in 032a)";

    Assert.equalValue( testName, actual, expected );

} );

UnitTest.addFixture( "test OaiSetMatcher.isPartOfNAT (record not in NAT)", function( ) {

    var recordVariables = {
        agencyId : 870970,
        codesIn032a : [ ],
        codesIn032x : [ 'SFD', 'ACC', 'ACC', 'DAT' ]
    };

    var expected = false;
    var actual = OaiSetMatcher.isPartOfNAT( recordVariables );

    var testName = "Record is not contained in NAT set (record from 870970 without value in 032a)";

    Assert.equalValue( testName, actual, expected );

} );

UnitTest.addFixture( "test OaiSetMatcher.isPartOfONL (record in ONL)", function( ) {

    var record = new Record( );
    record.fromString(
        '001 00 *a2 786 916 5*b870970*c20170802104841*d20090813*fa\n' +
        '004 00 *rn*ae\n' +
        '009 00 *aa*gxe\n' +
        '032 00 *aIDO200935*xNET200935*xDIT201038*xDAT201733\n' +
        '044 00 *aF*b07f*b09x*b95a\n' +
        '100 00 *0*aVellev*hHelle*4aut\n' +
        '245 00 *aOskar og Laura'
    );
    var recordVariables = OaiSetMatcher.setRecordVariables( 870970, record );

    var actual = OaiSetMatcher.isPartOfONL( recordVariables );
    var testName = "Record contained in ONL set (009g=xe and 032x=DAT)";
    Assert.equalValue( testName, actual, true );


    record = new Record( );
    record.fromString(
        '001 00 *a5 209 445 3*b870970*c20170803085025*d20151127*fa\n' +
        '004 00 *rn*ab\n' +
        '014 00 *a2 514 722 7\n' +
        '032 00 *aDBF201602*xARK201602*xDAT201733\n' +
        '245 00 *g2015:576*aEnvironmental policy analysis*cdealing with economic distortions\n' +
        '856 00*zAdgangsmåde: Internet*uhttp://norden.diva-portal.org/smash/get/diva2:862679/FULLTEXT03.pdf*yPDF-format'
    );
    recordVariables = OaiSetMatcher.setRecordVariables( 870970, record );

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Volume record contained in ONL set (has 856u)";
    Assert.equalValue( testName, actual, true );


    record = new Record();
    record.fromString(
        '001 00*a2 894 250 8*b870970*c20170102055805*d20110909*fa\n' +
        '004 00 *rn*ae\n' +
        '005 00 *zp\n' +
        '008 00 *tm*uf*a2011*bdk*dx*jf*ldan*v0*nb\n' +
        '009 00*ar*bs*gxe\n' +
        '032 00*xACC201136*aDLF201141*xNET201141*xNLY201141*xERA201702*xDAT201525\n' +
        '100 00 *0*aHall*hMartin*cf. 1963*4aut*4cmp*4mus\n' +
        '245 00 *aKinoplex\n'
    );
    recordVariables = OaiSetMatcher.setRecordVariables( 870970, record );

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (032)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ 'xe' ],
        codesIn032a : [ 'DBF' ],
        codesIn032x : [],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (009g=xe and 032a DBF)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ 'xe' ],
        codesIn032a : [ 'DPF' ],
        codesIn032x : [],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (009g=xe and 032a DPF)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ 'xe' ],
        codesIn032a : [ ],
        codesIn032x : [ 'BKM' ],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (009g=xe and 032x BKM)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [ ],
        codesIn032x : [ 'NEP' ],
        exist856u : true
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (856u exists and 032x NEP)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [ ],
        codesIn032x : [ 'SNE' ],
        exist856u : true
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (856u exists and 032x SNE)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [ ],
        codesIn032x : [ 'IDU' ],
        exist856u : true
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (856u exists and 032x IDU)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [ 'IDO' ],
        codesIn032x : [ 'DAT' ],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (032x DAT and 032a IDO)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [ 'IDP' ],
        codesIn032x : [ 'DAT' ],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (032x DAT and 032a IDP)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [  ],
        codesIn032x : [ 'DAT','NEP' ],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (032x DAT and NEP)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [  ],
        codesIn032x : [ 'DAT','NET' ],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (032x DAT and NET)";
    Assert.equalValue( testName, actual, true );


    recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [  ],
        codesIn032x : [ 'DAT','SNE' ],
        exist856u : false
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record contained in ONL set (032x DAT and SNE)";
    Assert.equalValue( testName, actual, true );

} );

UnitTest.addFixture( "test OaiSetMatcher.isPartOfONL (record not in ONL)", function( ) {

    var recordVariables = {
        agencyId : 870970,
        valuesOf009g : [ ],
        codesIn032a : [  ],
        codesIn032x : [ 'SNE' ],
        exist856u : false
    };

    var actual = OaiSetMatcher.isPartOfONL( recordVariables );
    var testName = "Record NOT contained in ONL set (no 009g=xe, no 856u and no DAT code in 032x)";
    Assert.equalValue( testName, actual, false );


    recordVariables = {
        agencyId : 870971,
        valuesOf009g : [ 'xe' ],
        codesIn032a : [  ],
        codesIn032x : [ 'DAT', 'SNE' ],
        exist856u : true
    };

    actual = OaiSetMatcher.isPartOfONL( recordVariables );
    testName = "Record NOT contained in ONL set (wrong agency)";
    Assert.equalValue( testName, actual, false );

} );

UnitTest.addFixture( "test OaiSetMatcher.isPartOfBCI", function( ) {

    var recordVariables = {
        agencyId : 870970,
        valuesOf001b : [ "870970" ],
        valuesOf996a : [ "700300" ]
    };
    var actual = OaiSetMatcher.isPartOfBCI( recordVariables );
    Assert.equalValue( "Record is BCI", actual, true );
    
    var recordVariables = {
        agencyId : 870970,
        valuesOf001b : [ "870970" ],
        valuesOf996a : [ "700301" ]
    };
    var actual = OaiSetMatcher.isPartOfBCI( recordVariables );
    Assert.equalValue( "Record is not BCI, 996a is not 700300", actual, false );

    var recordVariables = {
        agencyId : 870970,
        valuesOf001b : [ "870971" ],
        valuesOf996a : [ "700300" ]
    };
    var actual = OaiSetMatcher.isPartOfBCI( recordVariables );
    Assert.equalValue( "Record is not BCI, 1b is not 870970", actual, false );


} );

