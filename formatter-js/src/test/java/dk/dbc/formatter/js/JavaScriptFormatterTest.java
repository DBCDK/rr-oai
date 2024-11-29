/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-formatter-js
 *
 * rr-oai-formatter-js is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-formatter-js is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.formatter.js;

import dk.dbc.rawrepo.dto.RecordIdDTO;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class JavaScriptFormatterTest {

    private static final String MARCX_28413882 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><record xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\" xmlns=\"info:lc/xmlns/marcxchange-v1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><leader>00000n    2200000   4500</leader><datafield tag=\"001\" ind1=\"0\" ind2=\"0\"><subfield code=\"a\">28413882</subfield><subfield code=\"b\">870970</subfield><subfield code=\"c\">20111214085311</subfield><subfield code=\"d\">20100903</subfield><subfield code=\"f\">a</subfield></datafield><datafield tag=\"004\" ind1=\"0\" ind2=\"0\"><subfield code=\"r\">n</subfield><subfield code=\"a\">h</subfield></datafield><datafield tag=\"008\" ind1=\"0\" ind2=\"0\"><subfield code=\"b\">dk</subfield><subfield code=\"d\">x</subfield><subfield code=\"j\">f</subfield><subfield code=\"l\">dan</subfield><subfield code=\"v\">0</subfield></datafield><datafield tag=\"009\" ind1=\"0\" ind2=\"0\"><subfield code=\"a\">a</subfield><subfield code=\"g\">xx</subfield></datafield><datafield tag=\"041\" ind1=\"0\" ind2=\"0\"><subfield code=\"a\">dan</subfield><subfield code=\"c\">nor</subfield></datafield><datafield tag=\"100\" ind1=\"0\" ind2=\"0\"><subfield code=\"5\">870979</subfield><subfield code=\"6\">68562554</subfield><subfield code=\"4\">aut</subfield></datafield><datafield tag=\"241\" ind1=\"0\" ind2=\"0\"><subfield code=\"a\">Min kamp</subfield><subfield code=\"r\">norsk</subfield></datafield><datafield tag=\"245\" ind1=\"0\" ind2=\"0\"><subfield code=\"a\">Min kamp</subfield><subfield code=\"c\">roman</subfield></datafield><datafield tag=\"260\" ind1=\"0\" ind2=\"0\"><subfield code=\"&amp;\">1</subfield><subfield code=\"a\">[Kbh.]</subfield><subfield code=\"b\">Gyldendals Bogklubber</subfield><subfield code=\"c\">2010-</subfield></datafield><datafield tag=\"300\" ind1=\"0\" ind2=\"0\"><subfield code=\"a\">bind</subfield></datafield><datafield tag=\"652\" ind1=\"0\" ind2=\"0\"><subfield code=\"n\">85</subfield><subfield code=\"z\">26</subfield></datafield><datafield tag=\"652\" ind1=\"0\" ind2=\"0\"><subfield code=\"o\">sk</subfield></datafield><datafield tag=\"720\" ind1=\"0\" ind2=\"0\"><subfield code=\"o\">Sara Koch</subfield><subfield code=\"4\">trl</subfield></datafield><datafield tag=\"996\" ind1=\"0\" ind2=\"0\"><subfield code=\"a\">DBC</subfield></datafield></record>";
    private static final String MARCX_28407866 = "<?xml version=\"1.0\" encoding=\"utf8\"?><marcx:record format=\"danMARC2\" type=\"Bibliographic\" xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\"><marcx:leader>00000n    2200000   4500</marcx:leader><marcx:datafield tag=\"001\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">28407866</marcx:subfield><marcx:subfield code=\"b\">870970</marcx:subfield><marcx:subfield code=\"c\">20111213101808</marcx:subfield><marcx:subfield code=\"d\">20100831</marcx:subfield><marcx:subfield code=\"f\">a</marcx:subfield></marcx:datafield><marcx:datafield tag=\"004\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"r\">n</marcx:subfield><marcx:subfield code=\"a\">b</marcx:subfield></marcx:datafield><marcx:datafield tag=\"008\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"t\">m</marcx:subfield><marcx:subfield code=\"u\">u</marcx:subfield><marcx:subfield code=\"a\">2010</marcx:subfield><marcx:subfield code=\"v\">0</marcx:subfield></marcx:datafield><marcx:datafield tag=\"014\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">28413882</marcx:subfield></marcx:datafield><marcx:datafield tag=\"021\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"e\">9788703042633</marcx:subfield><marcx:subfield code=\"c\">ib.</marcx:subfield><marcx:subfield code=\"d\">kr. 249,00</marcx:subfield><marcx:subfield code=\"b\">kun for medlemmer</marcx:subfield></marcx:datafield><marcx:datafield tag=\"032\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"x\">ACC201035</marcx:subfield><marcx:subfield code=\"a\">DBF201038</marcx:subfield><marcx:subfield code=\"x\">DAT201201</marcx:subfield></marcx:datafield><marcx:datafield tag=\"245\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"g\">1. [bog]</marcx:subfield></marcx:datafield><marcx:datafield tag=\"250\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">1. bogklubudgave</marcx:subfield></marcx:datafield><marcx:datafield tag=\"260\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"c\">2010</marcx:subfield></marcx:datafield><marcx:datafield tag=\"300\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">487 sider</marcx:subfield></marcx:datafield><marcx:datafield tag=\"521\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"&amp;\">REX</marcx:subfield><marcx:subfield code=\"b\">1. oplag</marcx:subfield><marcx:subfield code=\"c\">2010</marcx:subfield></marcx:datafield><marcx:datafield tag=\"996\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">DBC</marcx:subfield></marcx:datafield></marcx:record>";

    private JavaScriptFormatter formatter;

    @Before
    public void setUp() throws Exception {
        formatter = new JavaScriptFormatter();

    }

    @Test(timeout = 30_000L)
    public void testFormattingMultilevel() throws Exception {
        System.out.println("testFormattingMultilevel");

        MarcXChangeWrapper[] records = new MarcXChangeWrapper[] {
            new MarcXChangeWrapper(MARCX_28407866, new RecordIdDTO[] {}),
            new MarcXChangeWrapper(MARCX_28413882, new RecordIdDTO[] {
                new RecordIdDTO("this-is-magic", 870970),
                new RecordIdDTO("28407866", 870970)
            })
        };
        String formatted = formatter.format(records, "marcx", "bkm");
        System.out.println("formatted = " + formatted);

        assertThat(formatted, containsString("<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\"><marcx:subfield code=\"a\">28413882</marcx:subfield>"));
        assertThat(formatted, containsString("<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\"><marcx:subfield code=\"a\">28407866</marcx:subfield>"));
        assertThat(formatted, containsString("<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"015\"><marcx:subfield code=\"a\">this-is-magic</marcx:subfield></marcx:datafield>"));
    }

    @Test(timeout = 2_000L)
    public void testCheckFormat() throws Exception {
        System.out.println("testCheckFormat");
        assertThat(formatter.checkFormat("foo"), is(false));
        assertThat(formatter.checkFormat("marcx"), is(true));
        assertThat(formatter.checkFormat("oai_dc"), is(true));
    }
}
