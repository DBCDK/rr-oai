/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

/** @file Module that contains unit tests for functions in MarcXchangeToOaiMarcX module */

use( "MarcXchangeToOaiMarcX" );
use( "UnitTest" );


UnitTest.addFixture( "MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType", function( ) {

    var marcRecord = new Record( );
    marcRecord.fromString(
        '001 00 *a 20049278 *b 870970\n' +
        '004 00 *r n *a e\n' +
        '008 00 *a 1992 *z 2016\n' +
        '009 00 *a a *g xx\n' +
        '032 00 *a DBF201709 *x BKM201709\n' +
        '100 00 *a Madsen *h Peter\n' +
        '245 00 *a Frejas smykke\n' +
        '300 00 *a 48 sider\n' +
        '440 00 *0 *a Valhalla *v 8\n' +
        '504 00 *& 1 *a Kaerlighedsgudinden Freja er i besiddelse af et kostbart halssmykke\n' +
        '520 00 *& 1 *a Originaludgave: 1992\n' +
        '520 00 *a Tidligere: 1. udgave. 1992\n' +
        '521 00 *b 4. oplag *c 2016\n' +
        '652 00 *o sk\n' +
        '700 00 *a Kure *h Henning\n' +
        '945 00 *a Peter Madsens Valhalla *z 440(a)\n' +
        '996 00 *a DBC'

    );

    var expected = XmlUtil.fromString(
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000name 22000004  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">20049278</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">e</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="008">' +
        '<marcx:subfield code="a">1992</marcx:subfield>' +
        '<marcx:subfield code="z">2016</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="009">' +
        '<marcx:subfield code="a">a</marcx:subfield>' +
        '<marcx:subfield code="g">xx</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="032">' +
        '<marcx:subfield code="a">DBF201709</marcx:subfield>' +
        '<marcx:subfield code="x">BKM201709</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="100">' +
        '<marcx:subfield code="a">Madsen</marcx:subfield>' +
        '<marcx:subfield code="h">Peter</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="245">' +
        '<marcx:subfield code="a">Frejas smykke</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="300">' +
        '<marcx:subfield code="a">48 sider</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="440">' +
        '<marcx:subfield code="0"/>' +
        '<marcx:subfield code="a">Valhalla</marcx:subfield>' +
        '<marcx:subfield code="v">8</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="504">' +
        '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
        '<marcx:subfield code="a">Kaerlighedsgudinden Freja er i besiddelse af et kostbart halssmykke</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="520">' +
        '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
        '<marcx:subfield code="a">Originaludgave: 1992</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="520">' +
        '<marcx:subfield code="a">Tidligere: 1. udgave. 1992</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="521">' +
        '<marcx:subfield code="b">4. oplag</marcx:subfield><marcx:subfield code="c">2016</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="652">' +
        '<marcx:subfield code="o">sk</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="700">' +
        '<marcx:subfield code="a">Kure</marcx:subfield>' +
        '<marcx:subfield code="h">Henning</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="945">' +
        '<marcx:subfield code="a">Peter Madsens Valhalla</marcx:subfield>' +
        '<marcx:subfield code="z">440(a)</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="996">' +
        '<marcx:subfield code="a">DBC</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>'
    );

    var actual = MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType( marcRecord );

    Assert.equalXml( "createMarcXmlWithRightRecordType - bibliographic", actual, expected );


    marcRecord = new Record( );
    marcRecord.fromString(
        '001 00 *a 12345678 *b 870970\n' +
        '004 00 *r n *a h'
    );

    expected = XmlUtil.fromString(
        '<marcx:record format="danMARC2" type="BibliographicMain" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n mh 22000004  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">12345678</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">h</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>'
    );

    actual = MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType( marcRecord );

    Assert.equalXml( "createMarcXmlWithRightRecordType - bibliographicMain", actual, expected );


    marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a12345678 *b870970\n' +
        '004 00 *rn *as'
    );

    expected = XmlUtil.fromString(
        '<marcx:record format="danMARC2" type="BibliographicSection" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n ms 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">12345678</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">n</marcx:subfield>' +
                '<marcx:subfield code="a">s</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>'
    );

    actual = MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType( marcRecord );

    Assert.equalXml( "createMarcXmlWithRightRecordType - BibliographicSection", actual, expected );


    marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a20775084 *b870970 *c20060901135613 *d19941116 *fa *oc\n' +
        '004 00 *rn *ab'
    );

    expected = XmlUtil.fromString(
        '<marcx:record format="danMARC2" type="BibliographicVolume" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n mb 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">20775084</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
                '<marcx:subfield code="c">20060901135613</marcx:subfield>' +
                '<marcx:subfield code="d">19941116</marcx:subfield>' +
                '<marcx:subfield code="f">a</marcx:subfield>' +
                '<marcx:subfield code="o">c</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">n</marcx:subfield>' +
                '<marcx:subfield code="a">b</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>'
    );

    actual = MarcXchangeToOaiMarcX.createMarcXmlWithRightRecordType( marcRecord );

    Assert.equalXml( "createMarcXmlWithRightRecordType - BibliographicVolume", actual, expected );

} );


UnitTest.addFixture( "MarcXchangeToOaiMarcX.removeBkmFields", function( ) {

    var marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a20049278 *b870970\n' +
        '004 00 *rn *ae\n' +
        '008 00 *a1992 *z2016\n' +
        '009 00 *aa *gxx\n' +
        '032 00 *aDBF201709 *xBKM201709\n' +
        '100 00 *aMadsen *hPeter\n' +
        '245 00 *aFrejas smykke\n' +
        '300 00 *a48 sider\n' +
        '440 00 *0 *aValhalla *v8\n' +
        '504 00 *&1 *aKaerlighedsgudinden Freja er i besiddelse af et kostbart halssmykke\n' +
        '520 00 *&1 *aOriginaludgave: 1992\n' +
        '520 00 *aTidligere: 1. udgave. 1992\n' +
        '521 00 *b4. oplag *c2016\n' +
        '652 00 *osk\n' +
        '700 00 *aKure *hHenning\n' +
        '945 00 *aPeter Madsens Valhalla *z440(a)\n' +
        '996 00 *aDBC'
    );

    var expected = new Record( );
    expected.fromString(
        '001 00 *a20049278 *b870970\n' +
        '004 00 *rn *ae\n' +
        '008 00 *a1992 *z2016\n' +
        '009 00 *aa *gxx\n' +
        '032 00 *aDBF201709 *xBKM201709\n' +
        '100 00 *aMadsen *hPeter\n' +
        '245 00 *aFrejas smykke\n' +
        '300 00 *a48 sider\n' +
        '440 00 *0 *aValhalla *v8\n' +
        '520 00 *aTidligere: 1. udgave. 1992\n' +
        '521 00 *b4. oplag *c2016\n' +
        '652 00 *osk\n' +
        '700 00 *aKure *hHenning\n' +
        '945 00 *aPeter Madsens Valhalla *z440(a)\n' +
        '996 00 *aDBC'
    );

    var actual = MarcXchangeToOaiMarcX.removeBkmFields( marcRecord );

    var testName = "removeBkmFields (no field 504 or 520 (with subfield &))";

    Assert.equalValue( testName, actual.toString( ), expected.toString( ) );


    marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '006 00 *d11 *2b\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '504 00 *&1 *aI Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '508 00 *aPersisk tale\n' +
        '517 00 *&1 *aMaerkning: Tilladt for boern over 11 aar\n' +
        '600 00 *1 *aAlizadeh *hSonita'
    );

    expected =  new Record( );
    expected.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '006 00 *d11 *2b\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '508 00 *aPersisk tale\n'
    );

    actual = MarcXchangeToOaiMarcX.removeBkmFields( marcRecord );

    testName = "removeBkmFields (no field 504, 517 (with subfield &) and 600)";

    Assert.equalValue( testName, actual.toString( ), expected.toString( ) );


    marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a 52714524 *b 870970\n' +
        '004 00 *r n *a e\n' +
        '008 00 *t m *a 2016 *b dk *l dan\n' +
        '009 00 *a a *g xe\n' +
        '021 00 *e 9788740037258\n' +
        '032 00 *x ACC201644 *a DBF201650 *x BKM201650\n' +
        '041 00 *a dan *c nor\n' +
        '245 00 *a Vores historie\n' +
        '504 00 *& 1 *a Marcus og Martinus Gunnarsen (f. 2002) er to helt normale norske drenge\n' +
        '512 00 *a Downloades i EPUB-format\n' +
        '600 00 *a Gunnarsen *h Marcus *1\n' +
        '600 00 *a Gunnarsen *h Martinus *1\n' +
        '610 00 *a Marcus & Martinus *1\n' +
        '666 00 *0 *f sangere\n' +
        '666 00 *0 *e Norge\n' +
        '990 00 *o 201650 *b b\n' +
        '991 00 *o Trykt version med lektoerudtalelse (5 272 376 0)'
    );

    expected = new Record( );
    expected.fromString(
        '001 00 *a 52714524 *b 870970\n' +
        '004 00 *r n *a e\n' +
        '008 00 *t m *a 2016 *b dk *l dan\n' +
        '009 00 *a a *g xe\n' +
        '021 00 *e 9788740037258\n' +
        '032 00 *x ACC201644 *a DBF201650 *x BKM201650\n' +
        '041 00 *a dan *c nor\n' +
        '245 00 *a Vores historie\n' +
        '512 00 *a Downloades i EPUB-format\n'
    );

    actual = MarcXchangeToOaiMarcX.removeBkmFields( marcRecord );

    testName = "removeBkmFields (no field 504, 600, 610, 666, 990, 991)";

    Assert.equalValue( testName, actual.toString( ), expected.toString( ) );


    marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '006 00 *d11 *2b\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '504 00 *&1 *aI Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '508 00 *aPersisk tale\n' +
        '517 00 *&1 *aMaerkning: Tilladt for boern over 11 aar\n' +
        '600 00 *1 *aAlizadeh *hSonita'
    );

    expected = new Record( );
    expected.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '006 00 *d11 *2b\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '508 00 *aPersisk tale'
    );

    actual = MarcXchangeToOaiMarcX.removeBkmFields( marcRecord );

    testName = "removeBkmFields (no field 504, 517 (with subfield &) and 600)";

    Assert.equalValue( testName, actual.toString( ), expected.toString( ) );

} );


UnitTest.addFixture( "MarcXchangeToOaiMarcX.removeLocalFieldsIfAny", function( ) {

    var marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '504 00 *&1 *aI Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '508 00 *aPersisk tale\n' +
        '517 00 *&1 *aMaerkning: Tilladt for boern over 11 aar\n' +
        '600 00 *1 *aAlizadeh *hSonita\n' +
        's12 00 *tTeamBMV201707\n' +
        'z99 00 *anoe'
    );

    var expected = new Record();
    expected.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '504 00 *&1 *aI Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '508 00 *aPersisk tale\n' +
        '517 00 *&1 *aMaerkning: Tilladt for boern over 11 aar\n' +
        '600 00 *1 *aAlizadeh *hSonita'
    );

    var actual = MarcXchangeToOaiMarcX.removeLocalFieldsIfAny( marcRecord );

    Assert.equalValue( "removeLocalFieldsIfAny - remove s12 and z99", actual.toString(), expected.toString() );


    marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a 52331048 *b 870970\n' +
        '004 00 *r n *a e\n' +
        '008 00 *t m *u f *a 2017 *b dk *k b *l per\n' +
        '009 00 *a m *g xe\n' +
        '032 00 *x ACC201707 *a DBI201709 *x BKM201709 *x FSB201709 *x FSC201709\n' +
        '245 00 *a Sonita\n' +
        '260 00 *b [Det Danske Filminstitut] *c [2017]\n' +
        '504 00 *& 1 *a I Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '600 00 *1 *a Alizadeh *h Sonita\n'
    );

    expected = new Record();
    expected.fromString(
        '001 00 *a 52331048 *b 870970\n' +
        '004 00 *r n *a e\n' +
        '008 00 *t m *u f *a 2017 *b dk *k b *l per\n' +
        '009 00 *a m *g xe\n' +
        '032 00 *x ACC201707 *a DBI201709 *x BKM201709 *x FSB201709 *x FSC201709\n' +
        '245 00 *a Sonita\n' +
        '260 00 *b [Det Danske Filminstitut] *c [2017]\n' +
        '504 00 *& 1 *a I Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '600 00 *1 *a Alizadeh *h Sonita\n'
    );

    actual = MarcXchangeToOaiMarcX.removeLocalFieldsIfAny( marcRecord );

    Assert.equalValue( "removeLocalFieldsIfAny - no local fields", actual.toString(), expected.toString() );

} );


UnitTest.addFixture( "MarcXchangeToOaiMarcX.removeLocalSubfieldsIfAny", function() {

    var inputRecord = new Record();
    inputRecord.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '006 00 *d11 *2b\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '504 00 *&1 *aI Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '508 00 *aPersisk tale\n' +
        '517 00 *&1 *aMaerkning: Tilladt for boern over 11 aar\n' +
        '600 00 *1 *aAlizadeh *hSonita'
    );

    var expected = new Record();
    expected.fromString(
        '001 00 *a52331048 *b870970\n' +
        '004 00 *rn *ae\n' +
        '006 00 *d11 *2b\n' +
        '008 00 *tm *uf *a2017 *bdk *kb *lper *na *x06 *v0\n' +
        '009 00 *am *gxe\n' +
        '032 00 *xACC201707 *aDBI201709 *xBKM201709 *xFSB201709 *xFSC201709\n' +
        '041 00 *aper *cdan\n' +
        '245 00 *aSonita\n' +
        '260 00 *b[Det Danske Filminstitut] *c[2017]\n' +
        '504 00 *aI Iran lever 15-aarige Sonita som illegal flygtning\n' +
        '508 00 *aPersisk tale\n' +
        '517 00 *aMaerkning: Tilladt for boern over 11 aar\n' +
        '600 00 *1 *aAlizadeh *hSonita'
    );

    var actual = MarcXchangeToOaiMarcX.removeLocalSubfieldsIfAny( inputRecord );
    var testName = "removeLocalSubfieldsIfAny - remove subfield & from several fields (present in field 504 and 517)";

    Assert.equalValue( testName, actual.toString(), expected.toString() );

} );

UnitTest.addFixture( "MarcXchangeToOaiMarcX.removeField665", function() {

    var inputRecord = new Record();
    inputRecord.fromString(
        '001 00 *a20049278 *b870970\n' +
        '004 00 *rn *ae\n' +
        '008 00 *a1992 *z2016\n' +
        '009 00 *aa *gxx\n' +
        '032 00 *aDBF201709 *xBKM201709\n' +
        '100 00 *aMadsen *hPeter\n' +
        '245 00 *aFrejas smykke\n' +
        '300 00 *a48 sider\n' +
        '440 00 *0 *aValhalla *v8\n' +
        '504 00 *&1 *aKaerlighedsgudinden Freja er i besiddelse af et kostbart halssmykke\n' +
        '520 00 *&1 *aOriginaludgave: 1992\n' +
        '520 00 *aTidligere: 1. udgave. 1992\n' +
        '521 00 *b4. oplag *c2016\n' +
        '652 00 *osk\n' +
        '665 00 *qSkandinavien\n' +
        '665 00 *umytologi\n' +
        '700 00 *aKure *hHenning\n' +
        '945 00 *aPeter Madsens Valhalla *z440(a)\n' +
        '996 00 *aDBC'
    );

    var expected = new Record();
    expected.fromString(
        '001 00 *a20049278 *b870970\n' +
        '004 00 *rn *ae\n' +
        '008 00 *a1992 *z2016\n' +
        '009 00 *aa *gxx\n' +
        '032 00 *aDBF201709 *xBKM201709\n' +
        '100 00 *aMadsen *hPeter\n' +
        '245 00 *aFrejas smykke\n' +
        '300 00 *a48 sider\n' +
        '440 00 *0 *aValhalla *v8\n' +
        '504 00 *&1 *aKaerlighedsgudinden Freja er i besiddelse af et kostbart halssmykke\n' +
        '520 00 *&1 *aOriginaludgave: 1992\n' +
        '520 00 *aTidligere: 1. udgave. 1992\n' +
        '521 00 *b4. oplag *c2016\n' +
        '652 00 *osk\n' +
        '700 00 *aKure *hHenning\n' +
        '945 00 *aPeter Madsens Valhalla *z440(a)\n' +
        '996 00 *aDBC'
    );

    var actual = MarcXchangeToOaiMarcX.removeField665( inputRecord );
    var testName = "remove all field 665";

    Assert.equalValue( testName, actual.toString(), expected.toString() );


} );


UnitTest.addFixture( "MarcXchangeToOaiMarcX.getRecordType", function() {

    var inputRecord = new Record();
    inputRecord.fromString( '004 00 *ah *rn' );

    var actual = MarcXchangeToOaiMarcX.getRecordType( inputRecord );

    Assert.equalValue( "getRecordType - BibliographicMain (004a=h)", actual, "BibliographicMain" );


    inputRecord = new Record();
    inputRecord.fromString( '004 00 *as *rn' );

    actual = MarcXchangeToOaiMarcX.getRecordType( inputRecord );

    Assert.equalValue( "getRecordType - BibliographicSection (004a=s)", actual, "BibliographicSection" );

    inputRecord = new Record();
    inputRecord.fromString( '004 00 *ab *rn' );

    actual = MarcXchangeToOaiMarcX.getRecordType( inputRecord );

    Assert.equalValue( "getRecordType - BibliographicVolume (004a=b)", actual, "BibliographicVolume" );


    inputRecord = new Record();
    inputRecord.fromString( '004 00 *ae *rn' );

    actual = MarcXchangeToOaiMarcX.getRecordType( inputRecord );

    Assert.equalValue( "getRecordType - Bibliographic (004a=e)", actual, "Bibliographic" );


    inputRecord = new Record();
    inputRecord.fromString( '004 00 *ai *rn' );

    actual = MarcXchangeToOaiMarcX.getRecordType( inputRecord );

    Assert.equalValue( "getRecordType - Bibliographic (004a=i)", actual, "Bibliographic" );

} );

