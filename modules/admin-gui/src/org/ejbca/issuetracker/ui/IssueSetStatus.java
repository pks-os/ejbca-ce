/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.issuetracker.ui;

import org.ejbca.issuetracker.IssueSet;

/**
 * Represents a mutable pair (issueSet, status) rendered in the GUI.
 *
 * @version $Id$
 */
public class IssueSetStatus {
    private IssueSet issueSet;
    private boolean isEnabled;

    public IssueSetStatus(final IssueSet issueSet, final boolean isEnabled) {
        this.issueSet = issueSet;
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(final boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public IssueSet getIssueSet() {
        return issueSet;
    }
}
