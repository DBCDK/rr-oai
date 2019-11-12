/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-service
 *
 * rr-oai-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.rr.oai;

import javax.ejb.EJBException;
import org.junit.Test;

import static dk.dbc.rr.oai.BeanFactory.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ForsRightsConfigValidatorIT extends DB {

    @Test(timeout = 2_000L)
    public void testOk() throws Exception {
        System.out.println("testOk");
        Config config = newConfig("FORS_RIGHTS_RULES=*=nat,art;foo/100=bkm,onl");
        newForsRightsConfigValidator(config, ds);
    }

    @Test(timeout = 2_000L)
    public void testOkWithMultipleRulesForSameSet() throws Exception {
        System.out.println("testOkWithMultipleRulesForSameSet");
        Config config = newConfig("FORS_RIGHTS_RULES=*=nat,art;foo/100=bkm,onl;bar/200=onl");
        newForsRightsConfigValidator(config, ds);
    }

    @Test(timeout = 2_000L, expected = EJBException.class)
    public void testInDatabaseButNotInRules() throws Exception {
        System.out.println("testInDatabaseButNotInRules");
        Config config = newConfig("FORS_RIGHTS_RULES=*=nat;foo/100=bkm,onl");
        newForsRightsConfigValidator(config, ds);
    }

    @Test(timeout = 2_000L, expected = EJBException.class)
    public void testInRulesButNotInDatabase() throws Exception {
        System.out.println("testInRulesButNotInDatabase");
        Config config = newConfig("FORS_RIGHTS_RULES=*=nat,unknown;foo/100=bkm,onl");
        newForsRightsConfigValidator(config, ds);
    }
}
