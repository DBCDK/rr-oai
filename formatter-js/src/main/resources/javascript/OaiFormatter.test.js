/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

/** @file Module that contains unit tests for functions in OaiFormatter module */

use( "OaiFormatter" );
use( "UnitTest" );

UnitTest.addFixture( "OaiFormatter.formatRecords (format DC)", function() {

    var format = 'oai_dc'; //applies to all tests in this Fixture
    var allowedSets = [ "BKM", "NAT" ]; //applies to all tests in this Fixture

    var recordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c    2200000   4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23645564</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
                '<marcx:subfield code="a">e</marcx:subfield>' +
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
        '</marcx:record>';

    var records = [
        {
            content: recordString,
            children: []
        }
    ];

    var expected =
        '<oai_dc:dc ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
            '<dc:creator>Haruki Murakami</dc:creator>' +
            '<dc:identifier>870970,23645564</dc:identifier>' +
            '<dc:title>Traekopfuglens kroenike</dc:title>' +
        '</oai_dc:dc>';

    var actual = OaiFormatter.formatRecords( records, format, allowedSets );

    var testName = "Format one of one record to DC";

    Assert.equalXml( testName, actual, expected );


    var volumeRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c    2200000   4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">44816687</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
                '<marcx:subfield code="a">b</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                '<marcx:subfield code="a">44783851</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="g">4. bok</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    var headRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c    2200000   4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">44783851</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="a">h</marcx:subfield>' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="100">' +
                '<marcx:subfield code="a">Knausgård</marcx:subfield>' +
                '<marcx:subfield code="h">Karl Ove</marcx:subfield>' +
                '<marcx:subfield code="4">aut</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="a">Min kamp</marcx:subfield>' +
                '<marcx:subfield code="c">roman</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    records = [
        {
            content: volumeRecordString,
            children: []
        },
        {
            content: headRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return "44816687"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        }
    ];

    expected =
        '<oai_dc:dc ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
        '<dc:identifier>870970,44816687</dc:identifier>' +
        '<dc:relation>870970,44783851</dc:relation>' +
        '<dc:title>4. bok</dc:title>' +
        '</oai_dc:dc>';

    actual = OaiFormatter.formatRecords( records, format, allowedSets );

    testName = "Format one (the first) of two records to DC";

    Assert.equalXml( testName, actual, expected );


    volumeRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n m  22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
            '<marcx:subfield code="a">23642468</marcx:subfield>' +
            '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
            '<marcx:subfield code="r">n</marcx:subfield>' +
            '<marcx:subfield code="a">b</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="014">' +
            '<marcx:subfield code="a">23642433</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
            '<marcx:subfield code="g">2.1</marcx:subfield>' +
            '<marcx:subfield code="a">Intern sikkerhedsdokumentation</marcx:subfield>' +
            '<marcx:subfield code="e">udarbejdet af: Holstberg Management</marcx:subfield>' +
            '<marcx:subfield code="e">forfatter: Anne Gram</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    var sectionRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n m  22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23642433</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">n</marcx:subfield>' +
                '<marcx:subfield code="a">s</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                '<marcx:subfield code="a">23641348</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="n">2</marcx:subfield>' +
                '<marcx:subfield code="o">Intern sikkerhedsdokumentation og -gennemgang</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    headRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n m  22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
            '<marcx:subfield code="a">23641348</marcx:subfield>' +
            '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
            '<marcx:subfield code="r">n</marcx:subfield>' +
            '<marcx:subfield code="a">h</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
            '<marcx:subfield code="a">Forebyggelse af arbejdsulykker</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';
    
    records = [
        {
            content: volumeRecordString,
            children: []
        },
        {
            content: sectionRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return "23642468"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        },
        {
            content: headRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return  "23642433"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        }
    ];
 
    expected =
        '<oai_dc:dc ' +
        'xmlns:dc="http://purl.org/dc/elements/1.1/" ' +
        'xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" ' +
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
        'xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">' +
            '<dc:identifier>870970,23642468</dc:identifier>' +
            '<dc:relation>870970,23641348</dc:relation>' +
            '<dc:relation>870970,23642433</dc:relation>' +
            '<dc:title>Intern sikkerhedsdokumentation. 2.1</dc:title>' +
        '</oai_dc:dc>';

    actual = OaiFormatter.formatRecords( records, format, allowedSets );

    testName = "Format one (the first) of three records to DC";

    Assert.equalXml( testName, actual, expected );

} );

UnitTest.addFixture( "OaiFormatter.formatRecords (format marcx, bkm as allowed set)", function() {

    var format = 'marcx'; //applies to all tests in this Fixture
    var allowedSets = [ "BKM", "NAT" ]; //applies to all tests in this Fixture

    var recordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c me 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23645564</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
                '<marcx:subfield code="a">e</marcx:subfield>' +
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
            '<marcx:datafield ind1="0" ind2="0" tag="260">' +
                '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
                '<marcx:subfield code="a">Århus</marcx:subfield>' +
                '<marcx:subfield code="b">Klim</marcx:subfield>' +
                '<marcx:subfield code="c">2003</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="504">' +
                '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
                '<marcx:subfield code="a">Det japanske samfund og sindets afkroge</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="z99">' +
                '<marcx:subfield code="a">masseret</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    var records = [
        {
            content: recordString,
            children: []
        }
    ];

    var expected =
        '<marcx:collection xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
            '<marcx:record format="danMARC2" type="Bibliographic">' +
            '<marcx:leader>00000c me 22000004  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">23645564</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="r">c</marcx:subfield>' +
                    '<marcx:subfield code="a">e</marcx:subfield>' +
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
                '<marcx:datafield ind1="0" ind2="0" tag="260">' +
                    '<marcx:subfield code="a">Århus</marcx:subfield>' +
                    '<marcx:subfield code="b">Klim</marcx:subfield>' +
                    '<marcx:subfield code="c">2003</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="504">' +
                    '<marcx:subfield code="a">Det japanske samfund og sindets afkroge</marcx:subfield>' +
                '</marcx:datafield>' +
            '</marcx:record>' +
        '</marcx:collection>';

    var actual = OaiFormatter.formatRecords( records, format, allowedSets );

    var testName = "Format single record to marcxchange + bkm allowed set + remove local fields and subfields";

    Assert.equalXml( testName, actual, expected );


    //This next test can be removed when field 665 is allowed and code hence changed:
    recordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c me 22000004  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">23645564</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">c</marcx:subfield>' +
        '<marcx:subfield code="a">e</marcx:subfield>' +
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
        '<marcx:datafield ind1="0" ind2="0" tag="260">' +
        '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
        '<marcx:subfield code="a">Århus</marcx:subfield>' +
        '<marcx:subfield code="b">Klim</marcx:subfield>' +
        '<marcx:subfield code="c">2003</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="504">' +
        '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
        '<marcx:subfield code="a">Det japanske samfund og sindets afkroge</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="665">' +
        '<marcx:subfield code="u">magisk realistisk</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="z99">' +
        '<marcx:subfield code="a">masseret</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>';

    records = [
        {
            content: recordString,
            children: []
        }
    ];

    expected =
        '<marcx:collection xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:record format="danMARC2" type="Bibliographic">' +
        '<marcx:leader>00000c me 22000004  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">23645564</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">c</marcx:subfield>' +
        '<marcx:subfield code="a">e</marcx:subfield>' +
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
        '<marcx:datafield ind1="0" ind2="0" tag="260">' +
        '<marcx:subfield code="a">Århus</marcx:subfield>' +
        '<marcx:subfield code="b">Klim</marcx:subfield>' +
        '<marcx:subfield code="c">2003</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="504">' +
        '<marcx:subfield code="a">Det japanske samfund og sindets afkroge</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>' +
        '</marcx:collection>';

    actual = OaiFormatter.formatRecords( records, format, allowedSets );

    testName = "Format single record to marcxchange + bkm allowed set + remove local fields and subfields + remove field 665";

    Assert.equalXml( testName, actual, expected );


    var volumeRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c mb 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">44816687</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
                '<marcx:subfield code="a">b</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                '<marcx:subfield code="a">44783851</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="g">4. bok</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    var headRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c mh 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">44783851</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="a">h</marcx:subfield>' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="100">' +
                '<marcx:subfield code="a">Knausgård</marcx:subfield>' +
                '<marcx:subfield code="h">Karl Ove</marcx:subfield>' +
                '<marcx:subfield code="4">aut</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="a">Min kamp</marcx:subfield>' +
                '<marcx:subfield code="c">roman</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';
                     
    records = [
        {
            content: volumeRecordString,
            children: []
        },
        {
            content: headRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return "44816687"; },
                    getAgencyId: function() { return 870970; }
                }, {
                    getBibliographicRecordId: function() { return "44816679"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        }
    ];

    expected =
        '<marcx:collection xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
            '<marcx:record format="danMARC2" type="BibliographicMain">' +
            '<marcx:leader>00000c mh 22000004  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">44783851</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="a">h</marcx:subfield>' +
                    '<marcx:subfield code="r">c</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="015">' +
                    '<marcx:subfield code="a">44816687</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="015">' +
                    '<marcx:subfield code="a">44816679</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="100">' +
                    '<marcx:subfield code="a">Knausgård</marcx:subfield>' +
                    '<marcx:subfield code="h">Karl Ove</marcx:subfield>' +
                    '<marcx:subfield code="4">aut</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                    '<marcx:subfield code="a">Min kamp</marcx:subfield>' +
                    '<marcx:subfield code="c">roman</marcx:subfield>' +
                '</marcx:datafield>' +
                '</marcx:record>' +
            '<marcx:record format="danMARC2" type="BibliographicVolume">' +
            '<marcx:leader>00000c mb 22000004  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">44816687</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="r">c</marcx:subfield>' +
                    '<marcx:subfield code="a">b</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                    '<marcx:subfield code="a">44783851</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                    '<marcx:subfield code="g">4. bok</marcx:subfield>' +
                '</marcx:datafield>' +
            '</marcx:record>' +
        '</marcx:collection>';

    actual = OaiFormatter.formatRecords( records, format, allowedSets );

    testName = "Format head and volume records to marcxchange - bkm allowed set";

    Assert.equalXml( testName, actual, expected );


    volumeRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n mb 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23642468</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">n</marcx:subfield>' +
                '<marcx:subfield code="a">b</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                '<marcx:subfield code="a">23642433</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="g">2.1</marcx:subfield>' +
                '<marcx:subfield code="a">Intern sikkerhedsdokumentation</marcx:subfield>' +
                '<marcx:subfield code="e">udarbejdet af: Holstberg Management</marcx:subfield>' +
                '<marcx:subfield code="e">forfatter: Anne Gram</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    var sectionRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n ms 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23642433</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">n</marcx:subfield>' +
                '<marcx:subfield code="a">s</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                '<marcx:subfield code="a">23641348</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="n">2</marcx:subfield>' +
                '<marcx:subfield code="o">Intern sikkerhedsdokumentation og -gennemgang</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    headRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n mh 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23641348</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">n</marcx:subfield>' +
                '<marcx:subfield code="a">h</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                '<marcx:subfield code="a">Forebyggelse af arbejdsulykker</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';
                
    records = [
        {
            content: volumeRecordString,
            children: []
        }, {
            content: sectionRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return "23642468"; },
                    getAgencyId: function() { return 870970; }
                }, {
                    getBibliographicRecordId: function() { return "23642980"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        }, {
            content: headRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return "23642433"; },
                    getAgencyId: function() { return 870970; }
                }, {
                    getBibliographicRecordId: function() { return "23644584"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        }
    ];                              

    expected =
        '<marcx:collection xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
            '<marcx:record format="danMARC2" type="BibliographicMain">' +
            '<marcx:leader>00000n mh 22000004  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">23641348</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="r">n</marcx:subfield>' +
                    '<marcx:subfield code="a">h</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="015">' +
                    '<marcx:subfield code="a">23642433</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="015">' +
                    '<marcx:subfield code="a">23644584</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                    '<marcx:subfield code="a">Forebyggelse af arbejdsulykker</marcx:subfield>' +
                '</marcx:datafield>' +
            '</marcx:record>' +
            '<marcx:record format="danMARC2" type="BibliographicSection">' +
            '<marcx:leader>00000n ms 22000004  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">23642433</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="r">n</marcx:subfield>' +
                    '<marcx:subfield code="a">s</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                    '<marcx:subfield code="a">23641348</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="015">' +
                    '<marcx:subfield code="a">23642468</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="015">' +
                    '<marcx:subfield code="a">23642980</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                    '<marcx:subfield code="n">2</marcx:subfield>' +
                    '<marcx:subfield code="o">Intern sikkerhedsdokumentation og -gennemgang</marcx:subfield>' +
                '</marcx:datafield>' +
            '</marcx:record>' +
            '<marcx:record format="danMARC2" type="BibliographicVolume">' +
            '<marcx:leader>00000n mb 22000004  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">23642468</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="r">n</marcx:subfield>' +
                    '<marcx:subfield code="a">b</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="014">' +
                    '<marcx:subfield code="a">23642433</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="245">' +
                    '<marcx:subfield code="g">2.1</marcx:subfield>' +
                    '<marcx:subfield code="a">Intern sikkerhedsdokumentation</marcx:subfield>' +
                    '<marcx:subfield code="e">udarbejdet af: Holstberg Management</marcx:subfield>' +
                    '<marcx:subfield code="e">forfatter: Anne Gram</marcx:subfield>' +
                '</marcx:datafield>' +
            '</marcx:record>' +
        '</marcx:collection>';

    actual = OaiFormatter.formatRecords( records, format, allowedSets );

    testName = "Format head, section and volume records to marcxchange - bkm allowed set";

    Assert.equalXml( testName, actual, expected );

} );

UnitTest.addFixture( "OaiFormatter.formatRecords (format marcx, bkm NOT allowed set)", function() {

    var format = 'marcx';
    var allowedSets = [ "NAT" ];

    var recordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c me 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23645564</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
                '<marcx:subfield code="a">e</marcx:subfield>' +
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
            '<marcx:datafield ind1="0" ind2="0" tag="260">' +
                '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
                '<marcx:subfield code="a">Århus</marcx:subfield>' +
                '<marcx:subfield code="b">Klim</marcx:subfield>' +
                '<marcx:subfield code="c">2003</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="504">' +
                '<marcx:subfield code="&#38;">1</marcx:subfield>' +  //subfield &
                '<marcx:subfield code="a">Det japanske samfund og sindets afkroge</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="z99">' +
                '<marcx:subfield code="a">masseret</marcx:subfield>' +
            '</marcx:datafield>' +
        '</marcx:record>';

    var records = [
        {
            content: recordString,
            children: []
        }
    ];   

    var expected =
        '<marcx:collection xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
            '<marcx:record format="danMARC2" type="Bibliographic">' +
            '<marcx:leader>00000c me 22000004  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">23645564</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="r">c</marcx:subfield>' +
                    '<marcx:subfield code="a">e</marcx:subfield>' +
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
                '<marcx:datafield ind1="0" ind2="0" tag="260">' +
                    '<marcx:subfield code="a">Århus</marcx:subfield>' +
                    '<marcx:subfield code="b">Klim</marcx:subfield>' +
                    '<marcx:subfield code="c">2003</marcx:subfield>' +
                '</marcx:datafield>' +
            '</marcx:record>' +
        '</marcx:collection>';

    var actual = OaiFormatter.formatRecords( records, format, allowedSets );

    var testName = "Format single record to marcxchange - BKM not allowed set + remove local fields and subfields";

    Assert.equalXml( testName, actual, expected );

} );

UnitTest.addFixture( "OaiFormatter.formatRecords (unknown format name)", function() {

    var format = 'illegal';
    var allowedSets = [ "BKM", "NAT" ];

    var recordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000c me 22000004  4500</marcx:leader>' +
            '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                '<marcx:subfield code="a">23645564</marcx:subfield>' +
                '<marcx:subfield code="b">870970</marcx:subfield>' +
            '</marcx:datafield>' +
            '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                '<marcx:subfield code="r">c</marcx:subfield>' +
                '<marcx:subfield code="a">e</marcx:subfield>' +
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
        '</marcx:record>';

    var records = [
        {
            content: recordString,
            children: []
        }
    ];   

    var error = new Error( "Format: illegal not allowed" );

    Assert.exception( "Throw error when format is not legal", function( ) {
        OaiFormatter.formatRecords( records, format, allowedSets )
    }, error );

} );

UnitTest.addFixture( "OaiFormatter.convertXmlRecordStringsToMarcObjects", function( ) {

    var volumeRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n mb 22000004  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">23642468</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">b</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="014">' +
        '<marcx:subfield code="a">23642433</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="245">' +
        '<marcx:subfield code="g">2.1</marcx:subfield>' +
        '<marcx:subfield code="a">Intern sikkerhedsdokumentation</marcx:subfield>' +
        '<marcx:subfield code="e">udarbejdet af: Holstberg Management</marcx:subfield>' +
        '<marcx:subfield code="e">forfatter: Anne Gram</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>';

    var sectionRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n ms 22000004  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">23642433</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">s</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="014">' +
        '<marcx:subfield code="a">23641348</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="245">' +
        '<marcx:subfield code="n">2</marcx:subfield>' +
        '<marcx:subfield code="o">Intern sikkerhedsdokumentation og -gennemgang</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>';

    var headRecordString =
        '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:leader>00000n mh 22000004  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">23641348</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">h</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="245">' +
        '<marcx:subfield code="a">Forebyggelse af arbejdsulykker</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>';
    
    var records = [
        {
            content: volumeRecordString,
            children: []
        }, {
            content: sectionRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return "23642468"; },
                    getAgencyId: function() { return 870970; }
                }, {
                    getBibliographicRecordId: function() { return "23642980"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        }, {
            content: headRecordString,
            children: [
                {
                    getBibliographicRecordId: function() { return "23642433"; },
                    getAgencyId: function() { return 870970; }
                }, {
                    getBibliographicRecordId: function() { return "23644584"; },
                    getAgencyId: function() { return 870970; }
                }
            ]
        }
    ];

    //first test (includeFields015 = true)
    var includeFields015 = true;
    var actual = OaiFormatter.convertXmlRecordStringsToMarcObjects( records, includeFields015 );

    var volumeRecordObject = new Record();
    volumeRecordObject.fromString(
        '001 00 *a23642468 *b870970\n' +
        '004 00 *rn *ab\n' +
        '014 00 *a23642433\n' +
        '245 00 *g2.1 *aIntern sikkerhedsdokumentation *eudarbejdet af: Holstberg Management *eforfatter: Anne Gram'
    );

    var sectionRecordObject = new Record();
    sectionRecordObject.fromString(
        '001 00 *a23642433 *b870970\n' +
        '004 00 *rn *as\n' +
        '014 00 *a23641348\n' +
        '015 00 *a23642468\n' +
        '015 00 *a23642980\n' +
        '245 00 *n2 *oIntern sikkerhedsdokumentation og -gennemgang'
    );

    var headRecordObject = new Record();
    headRecordObject.fromString(
        '001 00 *a23641348 *b870970\n' +
        '004 00 *rn *ah\n' +
        '015 00 *a23642433\n' +
        '015 00 *a23644584\n' +
        '245 00 *aForebyggelse af arbejdsulykker'
    );

    var expected = [ volumeRecordObject, sectionRecordObject, headRecordObject ];

    var testName = "convertXmlRecordStringsToMarcObjects - first record";
    Assert.equalValue( testName, String( actual[ 0 ] ), String( expected[ 0 ] ) );

    testName = "convertXmlRecordStringsToMarcObjects - second record";
    Assert.equalValue( testName, String( actual[ 1 ] ), String( expected[ 1 ] ) );

    testName = "convertXmlRecordStringsToMarcObjects - third record";
    Assert.equalValue( testName, String( actual[ 2 ] ), String( expected[ 2 ] ) );


    //second test includeFields015 = false
    includeFields015 = false;
    actual = OaiFormatter.convertXmlRecordStringsToMarcObjects( records, includeFields015 );

    volumeRecordObject = new Record();
    volumeRecordObject.fromString(
        '001 00 *a23642468 *b870970\n' +
        '004 00 *rn *ab\n' +
        '014 00 *a23642433\n' +
        '245 00 *g2.1 *aIntern sikkerhedsdokumentation *eudarbejdet af: Holstberg Management *eforfatter: Anne Gram'
    );

    sectionRecordObject = new Record();
    sectionRecordObject.fromString(
        '001 00 *a23642433 *b870970\n' +
        '004 00 *rn *as\n' +
        '014 00 *a23641348\n' +
        '245 00 *n2 *oIntern sikkerhedsdokumentation og -gennemgang'
    );

    headRecordObject = new Record();
    headRecordObject.fromString(
        '001 00 *a23641348 *b870970\n' +
        '004 00 *rn *ah\n' +
        '245 00 *aForebyggelse af arbejdsulykker'
    );

    expected = [ volumeRecordObject, sectionRecordObject, headRecordObject ];

    testName = "convertXmlRecordStringsToMarcObjects - first record";
    Assert.equalValue( testName, String( actual[ 0 ] ), String( expected[ 0 ] ) );

    testName = "convertXmlRecordStringsToMarcObjects - second record";
    Assert.equalValue( testName, String( actual[ 1 ] ), String( expected[ 1 ] ) );

    testName = "convertXmlRecordStringsToMarcObjects - third record";
    Assert.equalValue( testName, String( actual[ 2 ] ), String( expected[ 2 ] ) );


} );

UnitTest.addFixture( "OaiFormatter.getAllowedFormats", function() {

    Assert.equalValue( "get allowed formats", OaiFormatter.getAllowedFormats(), [ 'oai_dc', 'marcx' ] );

} );

// SAHU Unit test to ensure show/hide subfield 241u
UnitTest.addFixture( "OaiFormatter.formatRecords (format marcx, nat and/or bkm, subfield 241u)", function() {

    var format = 'marcx'; // applies to all tests in this fixture

    var recordString =
       '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
           '<marcx:leader>00000name 22000000  4500</marcx:leader>' +
           '<marcx:datafield ind1="0" ind2="0" tag="001">' +
               '<marcx:subfield code="a">38240803</marcx:subfield>' + 
               '<marcx:subfield code="b">870970</marcx:subfield>' + 
               '<marcx:subfield code="c">20210216122929</marcx:subfield>' + 
               '<marcx:subfield code="d">20201022</marcx:subfield>' + 
               '<marcx:subfield code="f">a</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="004">' +
               '<marcx:subfield code="r">n</marcx:subfield>' + 
               '<marcx:subfield code="a">e</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="008">' +
               '<marcx:subfield code="t">s</marcx:subfield>' + 
               '<marcx:subfield code="u">f</marcx:subfield>' + 
               '<marcx:subfield code="a">2020</marcx:subfield>' + 
               '<marcx:subfield code="b">dk</marcx:subfield>' + 
               '<marcx:subfield code="d">x</marcx:subfield>' + 
               '<marcx:subfield code="j">p</marcx:subfield>' + 
               '<marcx:subfield code="l">dan</marcx:subfield>' + 
               '<marcx:subfield code="v">0</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="009">' +
               '<marcx:subfield code="a">a</marcx:subfield>' + 
               '<marcx:subfield code="g">xx</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="021">' +
               '<marcx:subfield code="e">9788793871472</marcx:subfield>' + 
               '<marcx:subfield code="c">hf.</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="032">' +
               '<marcx:subfield code="x">ACC202043</marcx:subfield>' + 
               '<marcx:subfield code="a">DBF202048</marcx:subfield>' + 
               '<marcx:subfield code="x">BKM202048</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="041">' +
               '<marcx:subfield code="a">dan</marcx:subfield>' + 
               '<marcx:subfield code="c">spa</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="100">' +
               '<marcx:subfield code="5">870979</marcx:subfield>' + 
               '<marcx:subfield code="6">68339480</marcx:subfield>' + 
               '<marcx:subfield code="a">García Lorca</marcx:subfield>' + 
               '<marcx:subfield code="h">Federico</marcx:subfield>' + 
               '<marcx:subfield code="4">aut</marcx:subfield>' + 
            '</marcx:datafield>' +
           '<marcx:datafield ind1="0" ind2="0" tag="241">' +
               '<marcx:subfield code="a">Romancero gitano</marcx:subfield>' + 
               '<marcx:subfield code="u">1928</marcx:subfield>' +
        '</marcx:datafield>' +
        '</marcx:record>';


    var records = [
        {
            content: recordString,
            children: []
        }
    ];

    // Part 1: Include 241u when allowed sets contains BKM
    var allowedSets = [ "BKM", "NAT" ];

    var expected =
        '<marcx:collection xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
            '<marcx:record format="danMARC2" type="Bibliographic">' +
                '<marcx:leader>00000name 22000000  4500</marcx:leader>' +
                '<marcx:datafield ind1="0" ind2="0" tag="001">' +
                    '<marcx:subfield code="a">38240803</marcx:subfield>' +
                    '<marcx:subfield code="b">870970</marcx:subfield>' +
                    '<marcx:subfield code="c">20210216122929</marcx:subfield>' +
                    '<marcx:subfield code="d">20201022</marcx:subfield>' +
                    '<marcx:subfield code="f">a</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="004">' +
                    '<marcx:subfield code="r">n</marcx:subfield>' +
                    '<marcx:subfield code="a">e</marcx:subfield>' +
                '</marcx:datafield>' +
                    '<marcx:datafield ind1="0" ind2="0" tag="008">' +
                    '<marcx:subfield code="t">s</marcx:subfield>' +
                    '<marcx:subfield code="u">f</marcx:subfield>' +
                    '<marcx:subfield code="a">2020</marcx:subfield>' +
                    '<marcx:subfield code="b">dk</marcx:subfield>' +
                    '<marcx:subfield code="d">x</marcx:subfield>' +
                    '<marcx:subfield code="j">p</marcx:subfield>' +
                    '<marcx:subfield code="l">dan</marcx:subfield>' +
                    '<marcx:subfield code="v">0</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="009">' +
                    '<marcx:subfield code="a">a</marcx:subfield>' +
                    '<marcx:subfield code="g">xx</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="021">' +
                    '<marcx:subfield code="e">9788793871472</marcx:subfield>' +
                    '<marcx:subfield code="c">hf.</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="032">' +
                    '<marcx:subfield code="x">ACC202043</marcx:subfield>' +
                    '<marcx:subfield code="a">DBF202048</marcx:subfield>' +
                    '<marcx:subfield code="x">BKM202048</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="041">' +
                    '<marcx:subfield code="a">dan</marcx:subfield>' +
                    '<marcx:subfield code="c">spa</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="100">' +
                    '<marcx:subfield code="5">870979</marcx:subfield>' +
                    '<marcx:subfield code="6">68339480</marcx:subfield>' +
                    '<marcx:subfield code="a">García Lorca</marcx:subfield>' +
                    '<marcx:subfield code="h">Federico</marcx:subfield>' +
                    '<marcx:subfield code="4">aut</marcx:subfield>' +
                '</marcx:datafield>' +
                '<marcx:datafield ind1="0" ind2="0" tag="241">' +
                    '<marcx:subfield code="a">Romancero gitano</marcx:subfield>' +
                    '<marcx:subfield code="u">1928</marcx:subfield>' + // subfield 241u
                '</marcx:datafield>' +
            '</marcx:record>' +
        '</marcx:collection>';

    var actual = OaiFormatter.formatRecords( records, format, allowedSets );
    var testName = "Format single record to marcxchange + bkm allowed set + show subfield 241u";
    Assert.equalXml( testName, actual, expected );

    // Part 2: Excluding 241u when allowed set is only NAT
    allowedSets = ["NAT"];

    expected =
        '<marcx:collection xmlns:marcx="info:lc/xmlns/marcxchange-v1">' +
        '<marcx:record format="danMARC2" type="Bibliographic">' +
        '<marcx:leader>00000name 22000000  4500</marcx:leader>' +
        '<marcx:datafield ind1="0" ind2="0" tag="001">' +
        '<marcx:subfield code="a">38240803</marcx:subfield>' +
        '<marcx:subfield code="b">870970</marcx:subfield>' +
        '<marcx:subfield code="c">20210216122929</marcx:subfield>' +
        '<marcx:subfield code="d">20201022</marcx:subfield>' +
        '<marcx:subfield code="f">a</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="004">' +
        '<marcx:subfield code="r">n</marcx:subfield>' +
        '<marcx:subfield code="a">e</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="008">' +
        '<marcx:subfield code="t">s</marcx:subfield>' +
        '<marcx:subfield code="u">f</marcx:subfield>' +
        '<marcx:subfield code="a">2020</marcx:subfield>' +
        '<marcx:subfield code="b">dk</marcx:subfield>' +
        '<marcx:subfield code="d">x</marcx:subfield>' +
        '<marcx:subfield code="j">p</marcx:subfield>' +
        '<marcx:subfield code="l">dan</marcx:subfield>' +
        '<marcx:subfield code="v">0</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="009">' +
        '<marcx:subfield code="a">a</marcx:subfield>' +
        '<marcx:subfield code="g">xx</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="021">' +
        '<marcx:subfield code="e">9788793871472</marcx:subfield>' +
        '<marcx:subfield code="c">hf.</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="032">' +
        '<marcx:subfield code="x">ACC202043</marcx:subfield>' +
        '<marcx:subfield code="a">DBF202048</marcx:subfield>' +
        '<marcx:subfield code="x">BKM202048</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="041">' +
        '<marcx:subfield code="a">dan</marcx:subfield>' +
        '<marcx:subfield code="c">spa</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="100">' +
        '<marcx:subfield code="5">870979</marcx:subfield>' +
        '<marcx:subfield code="6">68339480</marcx:subfield>' +
        '<marcx:subfield code="a">García Lorca</marcx:subfield>' +
        '<marcx:subfield code="h">Federico</marcx:subfield>' +
        '<marcx:subfield code="4">aut</marcx:subfield>' +
        '</marcx:datafield>' +
        '<marcx:datafield ind1="0" ind2="0" tag="241">' +
        '<marcx:subfield code="a">Romancero gitano</marcx:subfield>' +
        // no subfield 241u
        '</marcx:datafield>' +
        '</marcx:record>' +
        '</marcx:collection>';

    actual = OaiFormatter.formatRecords( records, format, allowedSets );
    testName = "Format single record to marcxchange + bkm not allowed set + hide subfield 241u";
    Assert.equalXml( testName, actual, expected );

});
