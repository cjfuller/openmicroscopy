/*
 * org.openmicroscopy.shoola.agents.treeviewer.actions.DeleteAction
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.treeviewer.actions;


//Java imports
import java.awt.event.ActionEvent;
import javax.swing.Action;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.treeviewer.IconManager;
import org.openmicroscopy.shoola.agents.treeviewer.TreeViewerAgent;
import org.openmicroscopy.shoola.agents.treeviewer.browser.Browser;
import org.openmicroscopy.shoola.agents.treeviewer.cmd.DeleteCmd;
import org.openmicroscopy.shoola.agents.treeviewer.view.TreeViewer;
import org.openmicroscopy.shoola.agents.util.browser.TreeImageDisplay;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.FileAnnotationData;
import pojos.GroupData;
import pojos.ImageData;
import pojos.PlateData;
import pojos.ProjectData;
import pojos.ScreenData;
import pojos.TagAnnotationData;

/** 
 * Action to delete the selected element and {@link DeleteCmd} is executed.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class DeleteAction
    extends TreeViewerAction
{

    /** Name of the action. */
    private static final String NAME = "Delete";

    /** 
     * Description of the action if the selected item is <code>null</code>. 
     */
    private static final String DESCRIPTION = "Delete the selected elements.";
    
    /** 
     * Sets the action enabled depending on the state of the {@link Browser}.
     * @see TreeViewerAction#onBrowserStateChange(Browser)
     */
    protected void onBrowserStateChange(Browser browser)
    {
        if (browser == null) return;
        switch (browser.getState()) {
            case Browser.LOADING_DATA:
            case Browser.LOADING_LEAVES:
            case Browser.COUNTING_ITEMS:  
                setEnabled(false);
                break;
            default:
                onDisplayChange(browser.getLastSelectedDisplay());
                break;
        }
    }
    
    /**
     * Sets the action enabled depending on the selected type.
     * @see TreeViewerAction#onDisplayChange(TreeImageDisplay)
     */
    protected void onDisplayChange(TreeImageDisplay selectedDisplay)
    {
        if (selectedDisplay == null) {
            name = NAME;
            description = DESCRIPTION;
            putValue(Action.SHORT_DESCRIPTION, 
                    UIUtilities.formatToolTipText(DESCRIPTION));
            setEnabled(false);
            return;
        }
        Browser browser = model.getSelectedBrowser();
        if (browser == null) {
            name = NAME;
            putValue(Action.SHORT_DESCRIPTION, 
                    UIUtilities.formatToolTipText(DESCRIPTION));
            setEnabled(false);
            description = DESCRIPTION;
            return;
        } 
        Object ho = selectedDisplay.getUserObject(); 
        if ((ho instanceof DatasetData) || (ho instanceof ProjectData) ||
        	(ho instanceof FileAnnotationData) ||
        	(ho instanceof TagAnnotationData) || 
        	(ho instanceof ScreenData) || (ho instanceof PlateData))  {
        	TreeImageDisplay[] selected = browser.getSelectedDisplays();
        	if (selected.length > 1) setEnabled(false);
        	else {
        		setEnabled(model.isUserOwner(ho));
        	}
        } else if (ho instanceof ImageData) {
        	TreeImageDisplay[] selected = browser.getSelectedDisplays();
        	int count = 0;
    		for (int i = 0; i < selected.length; i++) {
				if (model.isUserOwner(selected[i].getUserObject())) count++;
			}
    		setEnabled(count == selected.length);
        } else if ((ho instanceof GroupData)) {
        	if (browser.getBrowserType() == Browser.ADMIN_EXPLORER) {
        		setEnabled(true); //TODO
         	} else setEnabled(false);
        } else if (ho instanceof ExperimenterData) {
        	if (browser.getBrowserType() == Browser.ADMIN_EXPLORER) {
        		setEnabled(true);
        		TreeImageDisplay[] selected = browser.getSelectedDisplays();
        		if (selected != null) {
        			TreeImageDisplay d;
        			ExperimenterData exp;
        			boolean b = true;
        			for (int i = 0; i < selected.length; i++) {
        				d = selected[i];
        				exp = (ExperimenterData) d.getUserObject();
        				if (exp.getId() == 
        					TreeViewerAgent.getUserDetails().getId()) {
        					b = false;
        					break;
        				}
        			}
        			setEnabled(b);
        		}
        	} else setEnabled(false);
        } else setEnabled(false);
        description = (String) getValue(Action.SHORT_DESCRIPTION);
    }
    
    /**
     * Creates a new instance.
     * 
     * @param model Reference to the Model. Mustn't be <code>null</code>.
     */
    public DeleteAction(TreeViewer model)
    {
        super(model);
        name = NAME;
        putValue(Action.NAME, name);
        putValue(Action.SHORT_DESCRIPTION, 
                UIUtilities.formatToolTipText(DESCRIPTION));
        IconManager im = IconManager.getInstance();
        putValue(Action.SMALL_ICON, im.getIcon(IconManager.DELETE));
    }

    /**
     * Creates a {@link DeleteCmd} command to execute the action. 
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        DeleteCmd cmd = new DeleteCmd(model.getSelectedBrowser());
        cmd.execute();
    }
    
}
