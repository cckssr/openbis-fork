/*
 * Copyright ETH 2014 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.common.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;

/**
 * @author Franz-Josef Elmer
 *
 */
public class HeaderFilter implements Filter
{
    private List<String> headerNames;
    private List<String> headerValues;

    @Override
    public void init(FilterConfig config) throws ServletException
    {
        Enumeration<String> initParameterNames = config.getInitParameterNames();
        headerNames = Arrays.asList(StringUtils.toStringArray(initParameterNames));
        headerValues = new ArrayList<>(headerNames.size());
        for (String name : headerNames)
        {
            headerValues.add(config.getInitParameter(name));
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        for (int i = 0; i < headerNames.size(); i++)
        {
            httpResponse.addHeader(headerNames.get(i), headerValues.get(i));
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy()
    {
    }

}
