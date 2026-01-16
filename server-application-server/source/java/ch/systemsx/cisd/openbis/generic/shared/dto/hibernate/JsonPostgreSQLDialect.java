///*
// * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package ch.systemsx.cisd.openbis.generic.shared.dto.hibernate;
//
//import org.hibernate.dialect.PostgreSQLDialect;
//
//import java.sql.Types;
//
//
///**
// * @author Franz-Josef Elmer
// */
//public class JsonPostgreSQLDialect extends PostgreSQLDialect
//{
//    public JsonPostgreSQLDialect()
//    {
//        this.registerColumnTypes(Types.JAVA_OBJECT, "JSONB");
//    }
//}
//In Hibernate 6, column/type handling was revamped. While Dialect still has internal mappings, JSON for PostgreSQL is already handled, and custom per-type registerColumnType(...) hooks are not the intended extension point anymore for JSON.
