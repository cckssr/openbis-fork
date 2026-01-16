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
package ch.systemsx.cisd.openbis.generic.shared.hibernate.type;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.postgresql.util.PGobject;

/**
 * Base helper for Postgres domain-based arrays (e.g. long_value[]) that may arrive as {@link PGobject} or {@link Array}.
 */
abstract class AbstractPgArrayJavaType<T> extends ArrayJavaType<T>
{
    protected AbstractPgArrayJavaType(JavaType<T> elementJdbcType)
    {
        super(elementJdbcType);
    }

    @Override
    public T[] wrap(Object raw, WrapperOptions options)
    {
        if (raw == null)
        {
            return null;
        }
        try
        {
            if (raw instanceof Array)
            {
                Object array = ((Array) raw).getArray();
                if (array instanceof Object[])
                {
                    return convert((Object[]) array);
                }
            }
            if (raw instanceof PGobject)
            {
                return parse(((PGobject) raw).getValue());
            }
            if (raw instanceof Object[])
            {
                return convert((Object[]) raw);
            }
            if (raw instanceof String)
            {
                return parse((String) raw);
            }
        } catch (SQLException e)
        {
            throw new IllegalArgumentException("Failed to convert SQL array", e);
        }
        return super.wrap(raw, options);
    }

    private T[] convert(Object[] array)
    {
        List<T> result = new ArrayList<>();
        for (Object value : array)
        {
            result.add(value == null ? null : convertElement(value.toString()));
        }
        @SuppressWarnings("unchecked")
        T[] arr = (T[]) result.toArray(createArray(result.size()));
        return arr;
    }

    private T[] parse(String text)
    {
        if (text == null)
        {
            return null;
        }
        String txt = text.trim();
        if (txt.startsWith("{") && txt.endsWith("}"))
        {
            txt = txt.substring(1, txt.length() - 1);
        }
        if (txt.isEmpty())
        {
            return createArray(0);
        }
        String[] parts = txt.split(",");
        List<T> result = new ArrayList<>();
        for (String part : parts)
        {
            String val = part.trim();
            result.add(val.isEmpty() ? null : convertElement(val));
        }
        @SuppressWarnings("unchecked")
        T[] arr = (T[]) result.toArray(createArray(result.size()));
        return arr;
    }

    protected abstract T convertElement(String value);

    protected abstract T[] createArray(int length);
}
