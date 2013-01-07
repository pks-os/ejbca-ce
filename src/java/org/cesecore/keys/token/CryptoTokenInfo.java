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
package org.cesecore.keys.token;

import java.io.Serializable;

/**
 * Non-sensitive information about a CryptoToken.
 * 
 * @version $Id$
 */
public class CryptoTokenInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Integer cryptoTokenId;
    private final String name;
    private final boolean active;
    private final boolean autoActivation;
    private final String type;
    private final boolean allowExportPrivateKey;
    private final String p11Library;
    private final String p11Slot;
    private final String p11AttributeFile;
    
    public CryptoTokenInfo(Integer cryptoTokenId, String name, boolean active, boolean autoActivation, String type,
            boolean allowExportPrivateKey, String p11Library, String p11Slot, String p11AttributeFile) {
        this.cryptoTokenId = cryptoTokenId;
        this.name = name;
        this.active = active;
        this.autoActivation = autoActivation;
        this.type = type;
        this.allowExportPrivateKey = allowExportPrivateKey;
        this.p11Library = p11Library;
        this.p11Slot = p11Slot;
        this.p11AttributeFile = p11AttributeFile;
    }

    public Integer getCryptoTokenId() { return cryptoTokenId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public boolean isAutoActivation() { return autoActivation; }
    public String getType() { return type; }
    public boolean isAllowExportPrivateKey() { return allowExportPrivateKey; }
    public String getP11Library() { return p11Library; }
    public String getP11Slot() { return p11Slot; }
    public String getP11AttributeFile() { return p11AttributeFile; }
}
