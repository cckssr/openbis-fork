package ch.systemsx.cisd.openbis.common.io.hierarchical_content;

import java.io.File;
import java.util.List;

import ch.systemsx.cisd.common.action.IDelegatedAction;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContent;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContentNode;

public class FilteredHierarchicalContentFactory implements IHierarchicalContentFactory
{

    private final IHierarchicalContentFactory contentFactory;

    private final IHierarchicalContentNodeFilter filter;

    public FilteredHierarchicalContentFactory(IHierarchicalContentFactory contentFactory, IHierarchicalContentNodeFilter filter)
    {
        this.contentFactory = contentFactory;
        this.filter = filter;
    }

    @Override public IHierarchicalContent asVirtualHierarchicalContent(final List<IHierarchicalContent> components)
    {
        final IHierarchicalContent content = contentFactory.asVirtualHierarchicalContent(components);
        return content != null ? new FilteredHierarchicalContent(content, filter) : null;
    }

    @Override public IHierarchicalContent asHierarchicalContent(final File file, final IDelegatedAction onCloseAction)
    {
        final IHierarchicalContent content = contentFactory.asHierarchicalContent(file, onCloseAction);
        return content != null ? new FilteredHierarchicalContent(content, filter) : null;
    }

    @Override public IHierarchicalContentNode asHierarchicalContentNode(final IHierarchicalContent rootContent, final File file)
    {
        final IHierarchicalContentNode contentNode = contentFactory.asHierarchicalContentNode(rootContent, file);
        return contentNode != null ? new FilteredHierarchicalContentNode(contentNode, filter) : null;
    }
}
