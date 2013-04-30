/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.ocsp;

import java.util.Collection;

import javax.ejb.Local;

import org.cesecore.certificates.ocsp.cache.CryptoTokenAndChain;

/**
 * Local interface for OcspResponseGeneratorSession
 * 
 * @version $Id: IntegratedOcspResponseGeneratorSessionLocal.java 14382 2012-03-20 11:50:54Z mikekushner $
 * 
 */
@Local
public interface OcspResponseGeneratorSessionLocal extends OcspResponseGeneratorSession{

    /**
     * 
     * @return the contents of the token and chain cache.
     */
    Collection<CryptoTokenAndChain> getCacheValues();
    
    void reloadTokenAndChainCache();
    
}
