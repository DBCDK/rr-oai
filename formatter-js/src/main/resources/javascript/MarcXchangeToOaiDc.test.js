/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

/** @file Module that contains unit tests for functions in MarcXchangeToOaiDc module */

use( "MarcXchangeToOaiDc" );
use( "UnitTest" );

UnitTest.addFixture( "test createDcXml", function( ) {

    var marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a23645564 *b870970 *c20160525133413 *d20010824 *fa\n' +
        '004 00 *rn *ae\n' +
        '008 00 *tm *ur *a2001 *z2003 *bdk *dx *jf *ldan *v0\n' +
        '009 00 *aa *gxx\n' +
        '021 00 *a87-7724-857-0 *cib. *dkr. 169,95\n' +
        '041 00 *adan *cjpn\n' +
        '100 00 *aMurakami *hHaruki\n' +
        '241 00 *aNejimaki-dori kuronikure\n' +
        '245 00 *aTraekopfuglens kroenike *eHaruki Murakami *foversat af Mette Holm\n' +
        '260 00 *aAarhus *bKlim *c2003 *ktr.i udl.\n' +
        '700 00 *aHolm *hMette'
    );

    var higherLevelIdentifiers = [ ];

    var expected = XmlUtil.fromString(
       '<oai_dc:dc ' +
       'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
       'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
       'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
       'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
           '<dc:contributor>Mette Holm</dc:contributor>' +
           '<dc:creator>Haruki Murakami</dc:creator>' +
           '<dc:date>2001</dc:date>' +
           '<dc:identifier>ISBN:87-7724-857-0</dc:identifier>' +
           '<dc:identifier>870970,23645564</dc:identifier>' +
           '<dc:language>dan</dc:language>' +
           '<dc:publisher>Klim</dc:publisher>' +
           '<dc:source>Nejimaki-dori kuronikure</dc:source>' +
           '<dc:title>Traekopfuglens kroenike</dc:title>' +
       '</oai_dc:dc>'
   );

    var actual = MarcXchangeToOaiDc.createDcXml( marcRecord, higherLevelIdentifiers );

    Assert.equalXml( "createDcXml - no higher level identifiers", actual, expected );


    marcRecord = new Record();
    marcRecord.fromString(
        '001 00 *a23642468 *b870970 *c20130118221228 *d20010823 *fa\n' +
        '004 00 *rn *ab\n' +
        '008 00 *tm *uf *a2001 *ldan *v0\n' +
        '014 00 *a23642433\n' +
        '032 00 *aIDO200137 *xNET200137 *xDAT991304\n' +
        '245 00 *g2.1 *aIntern sikkerhedsdokumentation *eudarbejdet af: Holstberg Management *eforfatter: Anne Gram\n' +
        '526 00 *iHertil findes *tBilag 1-8 *uhttp://www.arbejdsulykker.dk/pdf/2_1_bilag.pdf\n' +
        '700 00 *0 *aGram *hAnne\n' +
        '710 00 *aHolstberg Management\n' +
        '856 00 *zAdgangsmaade: Internet *uhttp://www.arbejdsulykker.dk/pdf/met_2_1.pdf *zKraever laeseprogrammet Acrobat Reader'
    );

    higherLevelIdentifiers = [ "870970,23642433", "870970,23641348" ];

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
            '<dc:contributor>Holstberg Management</dc:contributor>' +
            '<dc:contributor>Anne Gram</dc:contributor>' +
            '<dc:date>2001</dc:date>' +
            '<dc:identifier>870970,23642468</dc:identifier>' +
            '<dc:language>dan</dc:language>' +
            '<dc:relation>870970,23641348</dc:relation>' +
            '<dc:relation>870970,23642433</dc:relation>' +
            '<dc:title>Intern sikkerhedsdokumentation. 2.1</dc:title>' +
        '</oai_dc:dc>'
    );

    actual = MarcXchangeToOaiDc.createDcXml( marcRecord, higherLevelIdentifiers );

    Assert.equalXml( "createDcXml with higher level identifiers as dc:relation", actual, expected );

} );

UnitTest.addFixture( "test addDcTitleElement", function( ) {

    var record = new Record();
    record.fromString(
        '001 00 *a2 364 556 4 *b870970 *c20160525133413 *d20010824 *fa\n' +
        '245 00 *aTraekopfuglens kroenike *eHaruki Murakami *foversat af Mette Holm\n'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcTitleElement, xml, record );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:title>Traekopfuglens kroenike</dc:title>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "addDcTitleElement from field 245 subfield a", actual, expected );


    record = new Record( );
    record.fromString(
        '245 00*aGlem det*aDaarligt nyt*aEn \u00A4smule haab*\u00F8MagnaPrintserien'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcTitleElement, xml, record );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:title>Glem det. Daarligt nyt. En smule haab</dc:title>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "addDcTitleElement for volume record with more than one subfield a in field 245", actual, expected );


    record = new Record( );
    record.fromString(
        "001 00*a90337025*fa*b726500\n" +
        "004 00*ab*rn\n" +
        "245 00*G4*gDisc four\n" +
        "530 00*iIndhold*tFool's luck*tA \u00A4god in Colchester*tOld king Log"
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcTitleElement, xml, record );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:title>Disc four</dc:title>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "addDcTitleElement for volume record with no subfield a in field 245", actual, expected );


    record = new Record();
    record.fromString(
        "245 00 *G5 *gBand 5/6 *aDie \u00A4Schule von Neapel, Antonius Stradivarius *aDie \u00A4Schule von Rom, Livorno, Verona, Ferrara, Brescia und Mantua *fBearbeitung Adolf Heinrich Koenig"
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcTitleElement, xml, record );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:title>Die Schule von Neapel, Antonius Stradivarius. Die Schule von Rom, Livorno, Verona, Ferrara, Brescia und Mantua. Band 5/6</dc:title>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "addDcTitleElement for volume record with both subfields a and g in field 245", actual, expected );

} );

UnitTest.addFixture( "Test addDcCreatorOrContributorElements", function() {

    var elementName = "creator"; //this applies to all tests in this fixture

    var record = new Record( );
    record.fromString(
        '100 00 *aNoerholm *hMorten'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>Morten Noerholm</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:creator element from field 100 subfields a and h", actual, expected );


    record = new Record( );
    record.fromString(
        '100 00 *aAndersen *hH. C. *cf. 1805'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>H. C. Andersen</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "addDcCreatorElement from field 100 subfields a and h (do not include subfield c)", actual, expected );


    record = new Record( );
    record.fromString(
        '100 00 *aTherese *faf Lisieux'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>Therese af Lisieux</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "addDcCreatorElement from field 100 subfields a and f", actual, expected );


    record = new Record();
    record.fromString(
        '100 00 *aRomanoff *hRoman *fprins'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>Roman Romanoff prins</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "addDcCreatorElement from field 100 subfields a, h and f", actual, expected );


    record = new Record( );
    record.fromString(
        "100 00 *aLightnin Moe"
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>Lightnin Moe</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:creator element from field 100 subfield a", actual, expected );


    record = new Record();
    record.fromString( '110 00 *aDansk Sygehus Institut' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>Dansk Sygehus Institut</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:creator element from field 110 subfield a", actual, expected );


    record = new Record();
    record.fromString( '110 00 *aDet \u00A4Nordiske Symposium for Ikonografiske Studier *i5 *k1976 *jFuglsang' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>Det Nordiske Symposium for Ikonografiske Studier 5 1976 Fuglsang</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:creator element from field 110 subfield a, i, k and j", actual, expected );


    record = new Record();
    record.fromString( '110 00 *sRoskilde *eamt *cRegionplanafdelingen' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:creator>Roskilde amt Regionplanafdelingen</dc:creator>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:creator element from field 110 subfield s, e and c", actual, expected );

} );

UnitTest.addFixture( "Test addDcCreatorOrContributorElements", function() {

    var elementName = "contributor"; //this applies to all tests in this fixture

    var record = new Record( );
    record.fromString(
        '700 00 *0 *aHjorth *hMichael *cf. 1963-05-13 *4cre\n' +
        '700 00 *0 *aRosenfeldt *hHans *4cre'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:contributor>Michael Hjorth</dc:contributor>' +
        '<dc:contributor>Hans Rosenfeldt</dc:contributor>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:contributor elements from field 700 subfield a and h", actual, expected );


    record = new Record();
    record.fromString(
        '700 00 *aMargrethe *E2 *eII *fdronning af Danmark'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:contributor>Margrethe II dronning af Danmark</dc:contributor>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:contributor elements from field 700 subfields a, e and f", actual, expected );


    record = new Record();
    record.fromString(
        '700 00 *aMorrissey'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:contributor>Morrissey</dc:contributor>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:contributor elements from field 700 subfield a (no subfield h) 2", actual, expected );


    record = new Record();
    record.fromString( '710 00*0 *sRoskilde Stift *cStiftsraadet' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:contributor>Roskilde Stift Stiftsraadet</dc:contributor>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:contributor element from field 710 subfield s and c", actual, expected );


    record = new Record();
    record.fromString( '710 00 *aDen \u00A4Europaeiske Union*cKommissionen*cGeneraldirektorat XI' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:contributor>Den Europaeiske Union Kommissionen Generaldirektorat XI</dc:contributor>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:contributor element from field 710 subfield a and c", actual, expected );


    record = new Record();
    record.fromString( '710 00*aRigsarkivet' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:contributor>Rigsarkivet</dc:contributor>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:contributor element from field 710 subfield a alone", actual, expected );

    record = new Record();
    record.fromString( '710 00 *cIndenrigsministeriet' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:contributor>Indenrigsministeriet</dc:contributor>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:contributor element from field 710 subfield c alone", actual, expected );

} );


UnitTest.addFixture( "Test addDcCreatorOrContributorElements", function( ) {

    var elementName = "missing";
    var testName = "addDcCreatorOrContributorElements: do not add elements if elementName is not either 'creator' or 'contributor'";

    var record = new Record( );
    record.fromString(
        '700 00 *0 *aHjorth *hMichael *cf. 1963-05-13 *4cre\n' +
        '700 00 *0 *aRosenfeldt *hHans *4cre'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcCreatorOrContributorElements, xml, record, elementName );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    Assert.equalXml( testName, actual, expected );

} );


UnitTest.addFixture( "Test addDcPublisherElement", function( ) {

    var record = new Record( );
    record.fromString(
        '260 00 *aZelhem *bArboris *f[Haslev] *bi kommission hos Nordisk Bog Center *c1996'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcPublisherElement, xml, record );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:publisher>Arboris</dc:publisher>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:publisher element from field 260 b (only from first subfield)", actual, expected );


    record = new Record( );
    record.fromString(
        '260 00 *a[Hedehusene] *bNyt Dansk Litteraturselskab *a[Kolding] *bBibliodan *c2004 *kGrafia, Soeborg'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcPublisherElement, xml, record );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:publisher>Nyt Dansk Litteraturselskab</dc:publisher>' +
        '<dc:publisher>Bibliodan</dc:publisher>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:publisher element from field 260 b (from two subfields)", actual, expected );

} );

UnitTest.addFixture( "Test addDcDateElement", function( ) {

    var record = new Record( );
    record.fromString(
        '008 00 *uf *a2016 *bdk *lmul *v0'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcDateElement, xml, record );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:date>2016</dc:date>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:date element from 008a", actual, expected );


    record = new Record( );
    record.fromString(
        '004 00 *rc *ah\n' +
        '008 00 *bdk *v0' );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcDateElement, xml, record );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    Assert.equalXml( "add no dc:date element when 008a does not exist", actual, expected );


    record = new Record( );
    record.fromString(
        '008 00 *bdk *ldan *tm *v0*ur *a???? *z1917'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcDateElement, xml, record );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:date>????</dc:date>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:date element from 008a even when value is ????", actual, expected );

} );

UnitTest.addFixture( "Test addDcIdentifierElement", function() {

    var record = new Record( );
    record.fromString(
        '001 00 *a20049278 *b870970 *c20170213143753 *d19920818 *fa\n' +
        '021 00*e9788756260503*chf.\n' +
        '021 00*a87-562-6050-4'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcIdentifierElement, xml, record );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:identifier>870970,20049278</dc:identifier>' +
        '<dc:identifier>ISBN:9788756260503</dc:identifier>' +
        '<dc:identifier>ISBN:87-562-6050-4</dc:identifier>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:identifier elements from 001 and 021", actual, expected );

    record = new Record();
    record.fromString(
        '001 00 *a10456444 *b870970 *c20170212152323 *d19910208 *fa\n' +
        '022 00 *a0906-1142 *chf.'
    );

    xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcIdentifierElement, xml, record );

    expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:identifier>870970,10456444</dc:identifier>' +
        '<dc:identifier>ISSN:0906-1142</dc:identifier>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:identifier elements from 001 and 022", actual, expected );

} );

UnitTest.addFixture( "Test addDcSourceElement", function( ) {

    var record = new Record( );
    record.fromString(
        '241 00*aThe \u00A4mirror cracked from side to side\n' +
        '245 00*aDet \u00A4forstenede ansigt'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcSourceElement, xml, record );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:source>The mirror cracked from side to side</dc:source>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:identifier elements from 001 and 021", actual, expected );

} );

UnitTest.addFixture( "Test addDcLanguageElement", function( ) {

    var record = new Record();
    record.fromString(
        '008 00 *tp *ud *a1990 *z1998 *bdk *ca *e2 *hz *ib *ldan *v0'
    );

    var xml = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"/>'
    );

    var actual = MarcXchangeToOaiDc.__callElementMethod( MarcXchangeToOaiDc.addDcLanguageElement, xml, record );

    var expected = XmlUtil.fromString(
        '<oai_dc:dc ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:language>dan</dc:language>' +
        '</oai_dc:dc>'
    );

    Assert.equalXml( "add dc:language element from field 008 subfield l", actual, expected );

} );

UnitTest.addFixture( "Test getHigherLevelIdentifier", function( ) {

    var record = new Record();
    record.fromString(
        '001 00 *a23642433 *b870970\n' +
        '004 00 *rn *as\n' +
        '014 00 *a23641348\n' +
        '245 00 *n2 *oIntern sikkerhedsdokumentation og -gennemgang'
    );

    var expected = "870970,23641348";
    var actual = MarcXchangeToOaiDc.getHigherLevelIdentifier( record );
    var testName = "get higher level identifier";

    Assert.equalValue( testName, actual, expected );


    record = new Record();
    record.fromString(
        '001 00 *a23641348 *b870970\n' +
        '004 00 *rn *ah\n' +
        '245 00 *aForebyggelse af arbejdsulykker'
    );

    expected = "";
    actual = MarcXchangeToOaiDc.getHigherLevelIdentifier( record );
    testName = "get higher level identifier - no field 014, return empty string";

    Assert.equalValue( testName, actual, expected );


    record = new Record();
    record.fromString(
        '001 00 *a37304239 *b870970\n' +
        '004 00 *rn *ai\n' +
        '014 00 *a52971314 *xANM\n' +
        '016 00 *a03243796\n' +
        '245 00 *a[Anmeldelse]'
    );

    expected = "";
    actual = MarcXchangeToOaiDc.getHigherLevelIdentifier( record );
    testName = "get higher level identifier - field 014 has subfield x; return empty string";

    Assert.equalValue( testName, actual, expected );

} );



UnitTest.addFixture( "Test __removeUnwantedCharacters", function( ) {

    var input = "Die \u00A4Schule von Neapel, Antonius Stradivarius. Die \u00A4Schule von Rom, Livorno, Verona, Ferrara, Brescia und Mantua";
    var expected = "Die Schule von Neapel, Antonius Stradivarius. Die Schule von Rom, Livorno, Verona, Ferrara, Brescia und Mantua";

    var actual = MarcXchangeToOaiDc.__removeUnwantedCharacters( input );

    Assert.equalValue( "remove two currency signs", actual, expected );


    input = " en streng med [mellemrum] i begyndelsen og slutningen og [skarpe] parenteser ";
    expected = "en streng med mellemrum i begyndelsen og slutningen og skarpe parenteser";

    actual = MarcXchangeToOaiDc.__removeUnwantedCharacters( input );

    Assert.equalValue( "remove sharp parentheses and white spaces at beginning and end of string", actual, expected );

} );
