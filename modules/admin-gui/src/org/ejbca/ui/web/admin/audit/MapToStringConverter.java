/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ui.web.admin.audit;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * One way converter from Map<Object,Object> to String.
 * @version $Id$
 */
public class MapToStringConverter implements Converter {

	@Override
	public Object getAsObject(final FacesContext facesContext, final UIComponent uiComponent, final String value) {
		return value;
	}

	@Override
	public String getAsString(final FacesContext facesContext, final UIComponent uiComponent, final Object value) {
		if (value instanceof String) {
			return (String)value;
		}
		final StringBuilder sb = new StringBuilder();
		final Map<Object,Object> map = (Map<Object, Object>) value;
		if (map.size() == 1 && map.containsKey("msg")) {
			return (String) map.get("msg");
		}
		for (final Object key : map.keySet()) {
			if (sb.length()!=0) {
				sb.append("; ");
			}
			sb.append(key).append('=').append(map.get(key));
		}
		return sb.toString();
	}
}
