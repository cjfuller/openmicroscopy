/*
 * org.openmicroscopy.shoola.agents.treeviewer.browser.BrowserComponent
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.treeviewer.browser;

//Java imports
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.events.treeviewer.ShowProperties;
import org.openmicroscopy.shoola.agents.treeviewer.HierarchyLoader;
import org.openmicroscopy.shoola.agents.treeviewer.IconManager;
import org.openmicroscopy.shoola.agents.treeviewer.TreeViewerAgent;
import org.openmicroscopy.shoola.agents.treeviewer.TreeViewerTranslator;
import org.openmicroscopy.shoola.agents.treeviewer.cmd.EditVisitor;
import org.openmicroscopy.shoola.agents.treeviewer.editors.Editor;
import org.openmicroscopy.shoola.agents.treeviewer.util.FilterWindow;
import org.openmicroscopy.shoola.agents.treeviewer.view.TreeViewer;
import org.openmicroscopy.shoola.env.event.EventBus;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import org.openmicroscopy.shoola.util.ui.component.AbstractComponent;
import pojos.DataObject;

/** 
 * Implements the {@link Browser} interface to provide the functionality
 * required of the tree viewer component.
 * This class is the component hub and embeds the component's MVC triad.
 * It manages the component's state machine and fires state change 
 * notifications as appropriate, but delegates actual functionality to the
 * MVC sub-components.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
class BrowserComponent
    extends AbstractComponent
    implements Browser
{

    /** The Model sub-component. */
    private BrowserModel    model;
    
    /** The View sub-component. */
    private BrowserUI       view;
    
    /** The Controller sub-component. */
    private BrowserControl  controller;
    
    /**
     * Returns the frame hosting the {@link BrowserUI view}.
     * 
     * @param c The parent container.
     * @return See above.
     */
    private JFrame getViewParent(Container c)
    {
        if (c instanceof JFrame) return (JFrame) c;
        return getViewParent(c.getParent());
    }
    
    /**
     * Creates a new instance.
     * The {@link #initialize() initialize} method should be called straight 
     * after to complete the MVC set up.
     * 
     * @param model The Model sub-component.
     */
    BrowserComponent(BrowserModel model)
    {
        if (model == null) throw new NullPointerException("No model.");
        this.model = model;
        controller = new BrowserControl(this);
        view = new BrowserUI();
    }
    
    /** Links up the MVC triad. */
    void initialize()
    {
        model.initialize(this);
        controller.initialize(view);
        view.initialize(controller, model);
    }
    
    /**
     * Returns the Model sub-component.
     * 
     * @return See above.
     */
    BrowserModel getModel() { return model; }
    
    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getState()
     */
    public int getState() { return model.getState(); }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#activate()
     */
    public void activate()
    {
        int state = model.getState();
        switch (state) {
            case NEW:
                
                break;
            case DISCARDED:
                throw new IllegalStateException(
                        "This method can't be invoked in the DISCARDED state.");
            default:
                break;
        }
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#discard()
     */
    public void discard()
    {
        if (model.getState() != DISCARDED) {
            model.discard();
            fireStateChange();
        }
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getUI()
     */
    public JComponent getUI()
    { 
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        return view;
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setNodes(Set)
     */
    public void setNodes(Set nodes)
    {
        if (model.getState() != LOADING_DATA)
            throw new IllegalStateException(
                    "This method can only be invoked in the LOADING_DATA "+
                    "state.");
        if (nodes == null) throw new NullPointerException("No nodes.");
        Set visNodes = TreeViewerTranslator.transformHierarchy(nodes);
        view.setViews(visNodes);
        model.setState(READY);
        fireStateChange();
    }

    
    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getBrowserType()
     */
    public int getBrowserType() { return model.getBrowserType(); }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#cancel()
     */
    public void cancel()
    { 
        int state = model.getState();
        if ((state == LOADING_DATA) || (state == LOADING_LEAVES) ||
             (state == COUNTING_ITEMS)) {
            model.cancel();
            view.cancel(model.getSelectedDisplay()); 
            fireStateChange();
        }
    }
    
    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#loadData()
     */
    public void loadData()
    {
        int state = model.getState();
        if ((state == DISCARDED) || (state == LOADING_LEAVES))
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED or" +
                    "LOADING_LEAVES state.");
        model.fireDataLoading();
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#loadData()
     */
    public void loadLeaves()
    {
        int state = model.getState();
        if ((state == DISCARDED) || (state == LOADING_DATA))
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED or" +
                    "LOADING_LEAVES state.");
        model.fireLeavesLoading();
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setLeaves(Set)
     */
    public void setLeaves(Set leaves)
    {
        if (model.getState() != LOADING_LEAVES)
            throw new IllegalStateException(
                    "This method can only be invoked in the LOADING_LEAVES "+
                    "state.");
        if (leaves == null) throw new NullPointerException("No leaves.");
        Set visLeaves = TreeViewerTranslator.transformHierarchy(leaves);
        view.setLeavesViews(visLeaves);
        model.setState(READY);
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setSelectedDisplay(TreeImageDisplay)
     */
    public void setSelectedDisplay(TreeImageDisplay display)
    {
        switch (model.getState()) {
            case LOADING_DATA:
            case LOADING_LEAVES:
            case DISCARDED:
                throw new IllegalStateException(
                        "This method cannot be invoked in the LOADING_DATA, "+
                        " LOADING_LEAVES or DISCARDED state.");
        }
        TreeImageDisplay oldDisplay = model.getSelectedDisplay();
        if (oldDisplay != null && oldDisplay.equals(display)) return;
        model.setSelectedDisplay(display);
        firePropertyChange(SELECTED_DISPLAY_PROPERTY, oldDisplay, display);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#showPopupMenu()
     */
    public void showPopupMenu()
    {
        int state = model.getState();
        switch (state) {
            case LOADING_DATA:
            case LOADING_LEAVES:
            case DISCARDED:
                throw new IllegalStateException(
                        "This method can only be invoked in the LOADING_DATA, "+
                        " LOADING_LEAVES or DISCARDED state.");
        }
        firePropertyChange(POPUP_MENU_PROPERTY, null, view.getTreeDisplay());
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getClickPoint()
     */
    public Point getClickPoint() { return model.getClickPoint(); }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getSelectedDisplay()
     */
    public TreeImageDisplay getSelectedDisplay()
    {
        return model.getSelectedDisplay();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#close()
     */
    public void close()
    {
        switch (model.getState()) {
            case LOADING_DATA:
            case LOADING_LEAVES:
            case DISCARDED:
                throw new IllegalStateException(
                        "This method can only be invoked in the LOADING_DATA, "+
                        " LOADING_LEAVES or DISCARDED state.");
        }
        firePropertyChange(CLOSE_PROPERTY, null, this);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#collapse(TreeImageDisplay)
     */
    public void collapse(TreeImageDisplay node)
    {
        if (node == null) return;
        view.collapsePath(node);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#accept(TreeImageDisplayVisitor)
     */
    public void accept(TreeImageDisplayVisitor visitor)
    {
        DefaultTreeModel model = (DefaultTreeModel) 
                                    view.getTreeDisplay().getModel();
        TreeImageDisplay root = (TreeImageDisplay) model.getRoot();
        root.accept(visitor);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#accept(TreeImageDisplayVisitor, int)
     */
    public void accept(TreeImageDisplayVisitor visitor, int algoType)
    {
        DefaultTreeModel model = (DefaultTreeModel) 
        view.getTreeDisplay().getModel();
        TreeImageDisplay root = (TreeImageDisplay) model.getRoot();
        root.accept(visitor, algoType);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getTitle()
     */
    public String getTitle() { return view.getBrowserTitle(); }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getIcon()
     */
    public Icon getIcon()
    {
        IconManager im = IconManager.getInstance();
        switch (model.getBrowserType()) {
            case HIERARCHY_EXPLORER:
                return im.getIcon(IconManager.HIERARCHY_EXPLORER);
            case CATEGORY_EXPLORER:
                return im.getIcon(IconManager.CATEGORY_EXPLORER);
            case IMAGES_EXPLORER:
                return im.getIcon(IconManager.IMAGES_EXPLORER);
        }
        return null;
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setSortedNodes(List)
     */
    public void setSortedNodes(List nodes)
    {
        switch (model.getState()) {
        	case COUNTING_ITEMS:
            case LOADING_DATA:
            case LOADING_LEAVES:
            case DISCARDED:
                throw new IllegalStateException(
                        "This method cannot be invoked in the LOADING_DATA, "+
                        " LOADING_LEAVES or DISCARDED state.");
        }
        view.setSortedNodes(nodes);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setFilterNodes(Set, int)
     */
    public void setFilterNodes(Set nodes, int type)
    {
        if (model.getState() != LOADING_DATA)
            throw new IllegalStateException(
                    "This method can only be invoked in the LOADING_DATA"+
                    "state.");
        if (nodes == null) throw new NullPointerException("No nodes.");
        int index = -1;
        if (type == HierarchyLoader.DATASET) index = FilterWindow.DATASET;
        else if (type == HierarchyLoader.CATEGORY) 
            index = FilterWindow.CATEGORY;
        if (index == -1) throw new IllegalStateException("Index not valid.");
        model.setFilterType(type);
        model.setState(READY);
        fireStateChange();
        JFrame frame = getViewParent(view.getParent());
        FilterWindow window = new FilterWindow(frame, index,nodes);
        window.addPropertyChangeListener(FILTER_NODES_PROPERTY, controller);
        UIUtilities.centerAndShow(window);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#loadFilterData(int)
     */
    public void loadFilterData(int type)
    {
        /*
        switch (model.getState()) {
        	case COUNTING_ITEMS:
            case LOADING_DATA:
            case LOADING_LEAVES:
            case DISCARDED:
                throw new IllegalStateException(
                        "This method can only be invoked in the LOADING_DATA, "+
                        " LOADING_LEAVES or DISCARDED state.");
        }
        */
        if (model.getBrowserType() != Browser.IMAGES_EXPLORER) 
            throw new IllegalStateException(
                    "This method can only be invoked in the Image Explorer.");
        model.fireFilterDataLoading(type);
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#showFilterMenu(Component, Point)
     */
    public void showFilterMenu(Component c, Point p)
    {
        view.showFilterMenu(c, p);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#loadData(Set)
     */
    public void loadData(Set nodeIDs)
    {
        int state = model.getState();
        if ((state == DISCARDED) || (state == LOADING_LEAVES))
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED or" +
                    "LOADING_LEAVES state.");
        if (nodeIDs == null || nodeIDs.size() == 0) return;
        model.fireDataLoading(nodeIDs);
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#refresh()
     */
    public void refresh()
    {
        switch (model.getState()) {
            case LOADING_DATA:
            case LOADING_LEAVES:
            case DISCARDED:
                throw new IllegalStateException(
                        "This method cannot be invoked in the LOADING_DATA, "+
                        " LOADING_LEAVES or DISCARDED state.");
        }
        TreeImageDisplay display = model.getSelectedDisplay();
        if (display == null) return;
        if (!display.hasChildrenDisplay()) return;
        DefaultTreeModel dtm = (DefaultTreeModel) 
                                view.getTreeDisplay().getModel();
        TreeImageDisplay root = (TreeImageDisplay) dtm.getRoot();
        display.removeAllChildrenDisplay();
        if (root.equals(display)) loadData();
        else model.refreshSelectedDisplay();
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#refreshTree()
     */
    public void refreshTree()
    {
        switch (model.getState()) {
	        case LOADING_DATA:
	        case LOADING_LEAVES:
	        case DISCARDED:
	            throw new IllegalStateException(
	                    "This method cannot be invoked in the LOADING_DATA, "+
	                    " LOADING_LEAVES or DISCARDED state.");
        }
        DefaultTreeModel dtm = (DefaultTreeModel) 
        view.getTreeDisplay().getModel();
        TreeImageDisplay root = (TreeImageDisplay) dtm.getRoot();
        if (!root.hasChildrenDisplay()) return;
	    if (!model.isSelected()) {
	        view.clearTree();
	        return;
	    }
	    root.removeAllChildrenDisplay();
	    model.setSelectedDisplay(root);
	    loadData();
    }
    
    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setContainerNodes(Set, TreeImageDisplay)
     */
    public void setContainerNodes(Set nodes, TreeImageDisplay parent)
    {
        int state = model.getState();
        if (state != LOADING_DATA)
            throw new IllegalStateException(
                    "This method can only be invoked in the LOADING_DATA "+
                    "state.");
        if (nodes == null) throw new NullPointerException("No nodes.");
        if (parent == null) {
            view.setViews(TreeViewerTranslator.transformHierarchy(nodes));
        } else view.setViews(TreeViewerTranslator.transformContainers(nodes), 
                			model.getSelectedDisplay());
        model.fireContainerCountLoading();
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setHierarchyRoot(int, int)
     */
	public void setHierarchyRoot(int rootLevel, int rootID)
	{
	    int state = model.getState();
		if (state != READY && state != NEW)
		    throw new IllegalStateException(
                    "This method can only be invoked in the READY state.");
		int oldLevel = model.getRootLevel();
		model.setHierarchyRoot(rootLevel, rootID);
		firePropertyChange(HIERARCHY_ROOT_PROPERTY, new Integer(oldLevel), 
		        				new Integer(rootLevel));
	}

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getRootLevel()
     */
    public int getRootLevel()
    {
        if (model.getState() == DISCARDED)
		    throw new IllegalStateException(
                    "This method can't only be invoked in the DISCARDED " +
                    "state.");
        return model.getRootLevel();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getRootID()
     */
    public int getRootID()
    {
        if (model.getState() == DISCARDED)
		    throw new IllegalStateException(
                    "This method can't only be invoked in the DISCARDED " +
                    "state.");
        return model.getRootID();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setContainerCountValue(int, int)
     */
    public void setContainerCountValue(int containerID, int value)
    {
        int state = model.getState();
        switch (state) {
	        case COUNTING_ITEMS:
	            model.setContainerCountValue(view.getTreeDisplay(), 
	                    					containerID, value);
	            if (model.getState() == READY) fireStateChange();
	            break;
	        case READY:
	            model.setContainerCountValue(view.getTreeDisplay(), 
    										containerID, value);
	            break;
	        default:
	            throw new IllegalStateException(
	                    "This method can only be invoked in the " +
	                    "COUNTING_ITEMS or READY state.");
        }
        
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getContainersWithImagesNodes()
     */
    public Set getContainersWithImagesNodes()
    {
        //Note: avoid caching b/c we don't know yet what we are going
        //to do with updates
        ContainerFinder finder = new ContainerFinder();
        accept(finder, TreeImageDisplayVisitor.TREEIMAGE_SET_ONLY);
        return finder.getContainerNodes();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#getContainersWithImages()
     */
    public Set getContainersWithImages()
    {
        //Note: avoid caching b/c we don't know yet what we are going
        //to do with updates
        ContainerFinder finder = new ContainerFinder();
        accept(finder, TreeImageDisplayVisitor.TREEIMAGE_SET_ONLY);
        return finder.getContainers();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setFoundInBrowser(Set)
     */
    public void setFoundInBrowser(Set nodes)
    {
        if (nodes == null || nodes.size() == 0) {
            model.setFoundNodes(null); // reset default value.
            model.setFoundNodeIndex(-1); // reset default value.
            view.getTreeDisplay().repaint();
            return;
        }
        ArrayList list = new ArrayList(nodes.size());
        Iterator i = nodes.iterator();
        
        final JTree tree = view.getTreeDisplay();
        while (i.hasNext()) list.add(i.next());
        Comparator c = new Comparator() {
            public int compare(Object o1, Object o2)
            {
                TreeImageDisplay node1 = (TreeImageDisplay) o1;
                TreeImageDisplay node2 = (TreeImageDisplay) o2;
                int i1 = tree.getRowForPath(new TreePath(node1.getPath()));
                int i2 = tree.getRowForPath(new TreePath(node2.getPath()));
                return (i1-i2);
            }
        };
        Collections.sort(list, c);
        model.setFoundNodes(list);
        model.setFoundNodeIndex(0);
        TreeImageDisplay node = (TreeImageDisplay) list.get(0);
        view.selectFoundNode(node);
        Object ho = node.getUserObject();
        if (ho instanceof DataObject) {
            EventBus bus = TreeViewerAgent.getRegistry().getEventBus();
            bus.post(new ShowProperties((DataObject) ho, ShowProperties.EDIT));
        }
        tree.repaint();
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#findNext()
     */
    public void findNext()
    {
        List l = model.getFoundNodes();
        if (l == null || l.size() == 0) return;
        int index = model.getFoundNodeIndex();
        int n = l.size()-1;
        if (index < n) index++; //not last element
        else if (index == n) index = 0;
        model.setFoundNodeIndex(index);
        TreeImageDisplay node = (TreeImageDisplay) l.get(index);
        view.selectFoundNode(node);
        Object ho = node.getUserObject();
        if (ho instanceof DataObject) {
            EventBus bus = TreeViewerAgent.getRegistry().getEventBus();
            bus.post(new ShowProperties((DataObject) ho, ShowProperties.EDIT));
        }
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#findPrevious()
     */
    public void findPrevious()
    {
        List l = model.getFoundNodes();
        if (l == null || l.size() == 0) return;
        int index = model.getFoundNodeIndex();
        if (index > 0)  index--; //not last element
        else if (index == 0)  index = l.size()-1;
        TreeImageDisplay node = (TreeImageDisplay) l.get(index);
        view.selectFoundNode(node);
        Object ho = node.getUserObject();
        if (ho instanceof DataObject) {
            EventBus bus = TreeViewerAgent.getRegistry().getEventBus();
            bus.post(new ShowProperties((DataObject) ho, ShowProperties.EDIT));
        }
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#setSelected(boolean)
     */
    public void setSelected(boolean b)
    {
        switch (model.getState()) {
	        case LOADING_DATA:
	        case LOADING_LEAVES:
	        case COUNTING_ITEMS:
	        case DISCARDED:
	            throw new IllegalStateException(
	                    "This method can only be invoked in the " +
	                    "NEW or READY state.");
        }
        boolean old = model.isSelected();
        if (old == b) return;
        setSelectedDisplay(null);
        model.setSelected(b);
    }

    /**
     * Implemented as specified by the {@link Browser} interface.
     * @see Browser#refreshEdit(DataObject, int)
     */
    public void refreshEdit(DataObject object, int op)
    {
        Object o = object;
        if (op == Editor.CREATE_OBJECT)
            o = getSelectedDisplay().getUserObject();
        EditVisitor visitor = new EditVisitor(this, o);
        accept(visitor, TreeImageDisplayVisitor.TREEIMAGE_SET_ONLY);
        Set nodes = visitor.getFoundNodes();
        if (op == Editor.UPDATE_OBJECT) view.updateNodes(nodes, object);
        else if (op == TreeViewer.REMOVE_OBJECT) {
            TreeImageDisplay parentDisplay = 
                getSelectedDisplay().getParentDisplay();
            setSelectedDisplay(parentDisplay);
            view.removeNodes(nodes, parentDisplay);
        } else if (op == Editor.CREATE_OBJECT) {
            TreeImageDisplay display = 
                    TreeViewerTranslator.transformDataObject(object);
            TreeImageDisplay parentDisplay = getSelectedDisplay();
            setSelectedDisplay(display);
            view.createNodes(nodes, display, parentDisplay);
        }  
    }
    
}
