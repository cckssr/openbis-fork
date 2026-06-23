/*
 * Copyright ETH 2025 Zürich, Scientific IT Services
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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * Hibernate UserType for JSON string properties. Passes the raw JSON string directly to JDBC
 * via Types.OTHER (bypassing Jackson serialization), so PostgreSQL stores it as-is and
 * whitespace/formatting in user-entered JSON is preserved.
 */
public class JsonStringType implements UserType<String>
{
    @Override
    public int getSqlType()
    {
        return SqlTypes.JSON;
    }

    @Override
    public Class<String> returnedClass()
    {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) throws HibernateException
    {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(String x) throws HibernateException
    {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public String nullSafeGet(ResultSet rs, int position,
            SharedSessionContractImplementor session, Object owner)
            throws SQLException
    {
        return rs.getString(position);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String value, int index,
            SharedSessionContractImplementor session)
            throws SQLException
    {
        if (value == null)
        {
            st.setNull(index, Types.OTHER);
        } else
        {
            st.setObject(index, value, Types.OTHER);
        }
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public String deepCopy(String value) throws HibernateException
    {
        return value;
    }

    @Override
    public Serializable disassemble(String value) throws HibernateException
    {
        return value;
    }

    @Override
    public String assemble(Serializable cached, Object owner) throws HibernateException
    {
        return (String) cached;
    }

    @Override
    public String replace(String detached, String managed, Object owner)
    {
        return detached;
    }
}
