/*
 * Copyright ETH 2019 - 2025 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.generic.shared.dto.hibernate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JsonMapUserType implements UserType<Map<String, String>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public JsonMapUserType() { }

    @Override
    public int getSqlType() {
        // Hibernate logical JSON type
        return SqlTypes.JSON;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Map<String, String>> returnedClass() {
        return (Class<Map<String, String>>) (Class<?>) Map.class;
    }

    @Override
    public boolean equals(Map<String, String> x, Map<String, String> y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Map<String, String> x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Map<String, String> nullSafeGet(
            ResultSet rs,
            int position,
            SharedSessionContractImplementor session,
            Object owner) throws SQLException {
        final String json = rs.getString(position);
        if (rs.wasNull() || json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            throw new HibernateException("Failed to deserialize JSON to Map<String,String>", e);
        }
    }

    @Override
    public void nullSafeSet(
            PreparedStatement st,
            Map<String, String> value,
            int index,
            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            // Postgres JSON/JSONB commonly uses OTHER; adjust if your DB needs a different code
            st.setNull(index, Types.OTHER);
            return;
        }
        try {
            final String json = MAPPER.writeValueAsString(value);
            st.setObject(index, json, Types.OTHER); // for MySQL you can also use st.setString(index, json)
        } catch (IOException e) {
            throw new HibernateException("Failed to serialize Map<String,String> to JSON", e);
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Map<String, String> deepCopy(Map<String, String> value) {
        return value == null ? null : new HashMap<>(value);
    }

    @Override
    public Serializable disassemble(Map<String, String> value) throws HibernateException {
        return value == null ? null : new HashMap<>(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) return null;
        if (cached instanceof Map) {
            return new HashMap<>((Map<String, String>) cached);
        }
        throw new HibernateException("Cached value is not a Map: " + cached.getClass());
    }

    @Override
    public Map<String, String> replace(
            Map<String, String> detached,
            Map<String, String> managed,
            Object owner) {
        if (detached == null) {
            return null;
        }
        return new HashMap<>(detached);
    }

}
