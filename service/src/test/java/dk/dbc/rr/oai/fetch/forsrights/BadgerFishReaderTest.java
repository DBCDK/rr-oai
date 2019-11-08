/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-web-api
 *
 * opensearch-web-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-web-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.fetch.forsrights;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BadgerFishReaderTest {

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        String json = "{'content': {'$': 'hello', '@': 'ns1'}, 'optBool': {'$': 'true', '@': 'ns1'}, 'bool' : {'$': 'true', '@': 'ns1'}, 'optNum': {'$': '4', '@': 'ns1'}, 'num': {'$': '2', '@': 'ns1'}}".replaceAll("'", "\"");
        System.out.println("json = " + json);
        try (InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            TestData testData = BadgerFishReader.O.readValue(is, TestData.class);
            System.out.println("testData = " + testData);

            assertThat(testData.content, is("hello"));
            assertThat(testData.optBool, is(true));
            assertThat(testData.bool, is(true));
            assertThat(testData.optNum, is(4));
            assertThat(testData.num, is(2));
        }
    }

    public static class TestData {

        public String content;
        public Boolean optBool;
        public boolean bool;
        public Integer optNum;
        public int num;

        @Override
        public String toString() {
            return "TestData{" + "content=" + content + ", optBool=" + optBool + ", bool=" + bool + ", optNum=" + optNum + ", num=" + num + '}';
        }
    }

}
