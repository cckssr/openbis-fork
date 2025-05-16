package ch.systemsx.cisd.common.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

public class CaptureParameterValues<T>
{

    private final List<T> capturedValues = new ArrayList<>();

    public List<T> getCapturedValuesList()
    {
        return capturedValues;
    }

    public Set<T> getCapturedValuesSet()
    {
        return new HashSet<>(capturedValues);
    }

    public CustomAction captureParameter(int parameterIndex)
    {
        return new CustomAction("capture parameter " + parameterIndex + " values")
        {
            @Override public Object invoke(final Invocation invocation) throws Throwable
            {
                capturedValues.add((T) invocation.getParameter(parameterIndex));
                return null;
            }
        };
    }
}
