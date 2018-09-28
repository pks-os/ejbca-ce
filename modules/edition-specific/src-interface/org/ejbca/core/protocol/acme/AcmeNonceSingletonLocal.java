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

package org.ejbca.core.protocol.acme;

import javax.ejb.Local;

/**
 * Replay Protection nonce logic
 *
 * https://tools.ietf.org/html/draft-ietf-acme-acme-12#section-6.4
 *
 *   Example value from RFC draft: D8s4D2mLs8Vn-goWuPQeKA
 *
 * When presented with a nonce, we need to verify that:
 * - It has been generated
 * - It has not been used already
 *
 * We don't want to store every single nonce that we ever have generated since
 * - it would slow down each request to wait for the transaction to commit
 * - hammering the service would fill up the database with things that will be never used
 *
 * Instead we should
 * - Use an HMAC embedded in the nonce to check if the nonce has been generated by one of our servers
 * - Since we with an HMAC can ensure data integrity, we should embed a timestamp and prevent too old nonces from being
 *   used. This will allow us to purge used nonces from the database, when they can no longer be used anyway.
 *
 * If we reject the nonce, the client "SHOULD" retry the request, so we expect that most clients wont roll over and die
 * permanently if we expire nonces after a while.
 *
 * The format of the nonce is as follows:
 * ---------------------32 byte message------------------------------|----------32 byte HMAC--------------
 * --------8 byte timestamp-|-8 byte counter-|-16 byte nodeId--------|------------------------------------
 *
 * @version $Id$
 */

@Local
public interface AcmeNonceSingletonLocal {

    /** @return true if the provided nonce is one that has been generated and has not yet been used. (Non RFC quirk: we will reject expired nonces.) */
    boolean isNonceValid(String nonce);

    /** @return a new nonce as a base64 url-safe encoded String */
    String getReplayNonce() throws IllegalStateException;

}