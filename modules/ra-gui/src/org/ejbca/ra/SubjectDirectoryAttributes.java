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
package org.ejbca.ra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cesecore.certificates.util.DNFieldExtractor;
import org.cesecore.certificates.util.DnComponents;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile.Field;

/**
 * Contains Subject Directory attributes
 * @version $Id$
 *
 */
public class SubjectDirectoryAttributes {

    public final static List<String> COMPONENTS = Arrays.asList(
            DnComponents.DATEOFBIRTH,
            DnComponents.PLACEOFBIRTH,
            DnComponents.GENDER,
            DnComponents.COUNTRYOFCITIZENSHIP,
            DnComponents.COUNTRYOFRESIDENCE
            );

    private List<EndEntityProfile.FieldInstance> fieldInstances = new ArrayList<>();
    private String value;

    public SubjectDirectoryAttributes(final EndEntityProfile endEntityProfile) {
        this(endEntityProfile, null);
    }

    public SubjectDirectoryAttributes(final EndEntityProfile endEntityProfile, final String subjectDirectoryAttributes) {
        DNFieldExtractor dnFieldExtractor = null;
        if (subjectDirectoryAttributes!=null) {
            dnFieldExtractor = new DNFieldExtractor(subjectDirectoryAttributes, DNFieldExtractor.TYPE_SUBJECTDIRATTR);
        }
        for (final String key : COMPONENTS) {
            final Field field = endEntityProfile.new Field(key);
            for (final EndEntityProfile.FieldInstance fieldInstance : field.getInstances()) {
                if (dnFieldExtractor!=null) {
                    fieldInstance.setValue(dnFieldExtractor.getField(DnComponents.profileIdToDnId(fieldInstance.getProfileId()), fieldInstance.getNumber()));
                }
                fieldInstances.add(fieldInstance);
            }
        }
    }

    public List<EndEntityProfile.FieldInstance> getFieldInstances() {
        return fieldInstances;
    }

    public void setFieldInstances(List<EndEntityProfile.FieldInstance> fieldInstances) {
        this.fieldInstances = fieldInstances;
    }

    public void updateValue(){
        StringBuilder subjectDn = new StringBuilder();
        for(EndEntityProfile.FieldInstance fieldInstance : fieldInstances){
            if(!fieldInstance.getValue().isEmpty()){
                int dnId = DnComponents.profileIdToDnId(fieldInstance.getProfileId());
                String nameValueDnPart = DNFieldExtractor.getFieldComponent(dnId, DNFieldExtractor.TYPE_SUBJECTDIRATTR) + fieldInstance.getValue().trim();
                nameValueDnPart = org.ietf.ldap.LDAPDN.escapeRDN(nameValueDnPart);
                if(subjectDn.length() != 0){
                    subjectDn.append(", ");
                }
                subjectDn.append(nameValueDnPart);
            }
        }
        //TODO DNEMAILADDRESS copying from UserAccountData
        value = subjectDn.toString();
    }

    @Override
    public String toString() {
        return getValue();
    }

    /**
     * @return the value
     */
    public String getValue() {
        if(value == null){
            updateValue();
        }
        return value;
    }
    
    public String getUpdatedValue() {
        updateValue();
        return value;
    }

    @SuppressWarnings("unused")
    private void setValue(String value) {
        this.value = value;
    }
}
