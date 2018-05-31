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
package org.ejbca.ui.web.rest.api.io.response;

/**
 * A container for response status information. Are where more results? Next offset, how many results left etc..
 *
 * @version $Id: PaginationRestResponseComponent.java 29010 2018-05-23 13:09:53Z andrey_s_helmes $
 */
public class PaginationRestResponseComponent {

    private boolean moreResults;
    private int nextOffset;
    private int numberOfResults;

    private PaginationRestResponseComponent(boolean moreResults, int nextOffset, int numberOfResults) {
        this.moreResults = moreResults;
        if(moreResults) {
            this.nextOffset = nextOffset;
            this.numberOfResults = numberOfResults;
        }
    }

    public boolean isMoreResults() {
        return moreResults;
    }

    public void setMoreResults(boolean moreResults) {
        this.moreResults = moreResults;
    }

    public int getNextOffset() {
        return nextOffset;
    }

    public void setNextOffset(int nextOffset) {
        this.nextOffset = nextOffset;
    }

    public int getNumberOfResults() {
        return numberOfResults;
    }

    public void setNumberOfResults(int numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    public static PaginationRestResponseComponentBuilder builder() {
        return new PaginationRestResponseComponentBuilder();
    }

    public static class PaginationRestResponseComponentBuilder {
        private boolean moreResults;
        private int nextOffset;
        private int numberOfResults;

        public PaginationRestResponseComponentBuilder setMoreResults(boolean moreResults) {
            this.moreResults = moreResults;
            return this;
        }

        public PaginationRestResponseComponentBuilder setNextOffset(int nextOffset) {
            this.nextOffset = nextOffset;
            return this;
        }

        public PaginationRestResponseComponentBuilder setNumberOfResults(int numberOfResults) {
            this.numberOfResults = numberOfResults;
            return this;
        }

        public PaginationRestResponseComponent build() {
            return new PaginationRestResponseComponent(moreResults, nextOffset, numberOfResults);
        }
    }
}
