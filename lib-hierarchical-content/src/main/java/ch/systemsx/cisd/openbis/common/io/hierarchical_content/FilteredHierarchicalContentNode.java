package ch.systemsx.cisd.openbis.common.io.hierarchical_content;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.base.io.IRandomAccessFile;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContentNode;

public class FilteredHierarchicalContentNode implements IHierarchicalContentNode
{
    private final IHierarchicalContentNode node;

    private final IHierarchicalContentNodeFilter filter;

    public FilteredHierarchicalContentNode(IHierarchicalContentNode node, IHierarchicalContentNodeFilter filter)
    {
        this.node = node;
        this.filter = filter != null ? filter : IHierarchicalContentNodeFilter.MATCH_ALL;
    }

    @Override
    public String getName()
    {
        return node.getName();
    }

    @Override
    public String getRelativePath()
    {
        return node.getRelativePath();
    }

    @Override
    public String getParentRelativePath()
    {
        return node.getParentRelativePath();
    }

    @Override
    public boolean exists()
    {
        return node.exists();
    }

    @Override
    public boolean isDirectory()
    {
        return node.isDirectory();
    }

    @Override
    public long getLastModified()
    {
        return node.getLastModified();
    }

    @Override
    public List<IHierarchicalContentNode> getChildNodes() throws UnsupportedOperationException
    {
        return wrap(node.getChildNodes());
    }

    @Override
    public File getFile() throws UnsupportedOperationException
    {
        return node.getFile();
    }

    @Override
    public File tryGetFile()
    {
        return node.tryGetFile();
    }

    @Override
    public long getFileLength() throws UnsupportedOperationException
    {
        return node.getFileLength();
    }

    @Override
    public String getChecksum() throws UnsupportedOperationException
    {
        return node.getChecksum();
    }

    @Override
    public int getChecksumCRC32() throws UnsupportedOperationException
    {
        return node.getChecksumCRC32();
    }

    @Override
    public boolean isChecksumCRC32Precalculated()
    {
        return node.isChecksumCRC32Precalculated();
    }

    @Override
    public IRandomAccessFile getFileContent() throws UnsupportedOperationException, IOExceptionUnchecked
    {
        return node.getFileContent();
    }

    @Override
    public InputStream getInputStream() throws UnsupportedOperationException, IOExceptionUnchecked
    {
        return node.getInputStream();
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
