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
package org.ejbca.core.ejb.ca.publisher;

import java.util.Date;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.ejbca.core.model.ca.publisher.BasePublisher;
import org.ejbca.core.model.ca.publisher.PublisherConst;
import org.ejbca.core.model.util.EjbLocalHelper;


public class PublisherQueueDataSynchronization implements Synchronization {

    private static final AuthenticationToken authenticationToken = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("ServiceSession"));

    private EjbLocalHelper ejbLocalHelper;

    private PublisherSessionLocal publisherSession;
    private PublisherQueueData entity;
    
    public PublisherQueueDataSynchronization(PublisherQueueData entity) {
        this.entity = entity;
        ejbLocalHelper = new EjbLocalHelper();
        publisherSession = ejbLocalHelper.getPublisherSession();
    }
    
    @Override
    public void afterCompletion(int transactionStatus) {
        // PublisherQueueDataEntry has been committed to database. Should be safe to publish.
        if (transactionStatus == Status.STATUS_COMMITTED) {
            final BasePublisher publisher = publisherSession.getPublisher(entity.getPublisherId());
            // We only care about direct certificate publishing
            // TODO safeDirect &&  (or transient field on the entity)
            if (!publisher.getOnlyUseQueue() && publisher.getType() == PublisherConst.PUBLISH_TYPE_CERT) {
                org.ejbca.core.model.ca.publisher.PublisherQueueData queuedData = 
                        new org.ejbca.core.model.ca.publisher.PublisherQueueData(entity.getPk(), new Date(entity.getTimeCreated()), new Date(entity.getLastUpdate()),
                        entity.getPublishStatus(), entity.getTryCounter(), entity.getPublishType(), entity.getFingerprint(), entity.getPublisherId(),
                        entity.getPublisherQueueVolatileData());
                
                publisherSession.publishQueuedEntry(authenticationToken, publisher, queuedData);
            }
            
        }
    }

    @Override
    public void beforeCompletion() {
        // NOOP
    }
    
}
