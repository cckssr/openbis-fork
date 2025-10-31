/*
 * Copyright ETH 2013 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.common.io.hierarchical_content;

import java.util.ArrayList;
import java.util.List;

import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContent;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContentNode;

/**
 * @author Franz-Josef Elmer
 */
public class FilteredHierarchicalContent implements IHierarchicalContent
{
    private final IHierarchicalContent content;

    private final IHierarchicalContentNodeFilter filter;

    public FilteredHierarchicalContent(IHierarchicalContent content, IHierarchicalContentNodeFilter filter)
    {
        this.content = content;
        this.filter = filter != null ? filter : IHierarchicalContentNodeFilter.MATCH_ALL;
    }

    @Override
    public IHierarchicalContentNode getRootNode()
    {
        return wrap(content.getRootNode());
    }

    @Override
    public IHierarchicalContentNode getNode(String relativePath) throws IllegalArgumentException
    {
        return wrap(content.getNode(relativePath));
    }

    @Override
    public IHierarchicalContentNode tryGetNode(String relativePath)
    {
        return wrap(content.tryGetNode(relativePath));
    }

    @Override
    public List<IHierarchicalContentNode> listMatchingNodes(String relativePathPattern)
    {
        return wrap(content.listMatchingNodes(relativePathPattern));
    }

    @Override
    public List<IHierarchicalContentNode> listMatchingNodes(String startingPath, String fileNamePattern)
    {
        return wrap(content.listMatchingNodes(startingPath, fileNamePattern));
    }

    @Override
    public void close()
    {
        content.close();
    }

    private List<IHierarchicalContentNode> wrap(List<IHierarchicalContentNode> nodes)
    {
        List<IHierarchicalContentNode> wrappedNodes = new ArrayList<IHierarchicalContentNode>(nodes.size());
        for (IHierarchicalContentNode node : nodes)
        {
            if (filter.accept(node))
            {
                wrappedNodes.add(wrap(node));
            }
        }
        return wrappedNodes;
    }

    private IHierarchicalContentNode wrap(IHierarchicalContentNode node)
    {
        return node == null ? null : new FilteredHierarchicalContentNode(node, filter);
    }

}
