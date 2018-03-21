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
package org.ejbca.ui.psm.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectItems;
import javax.faces.component.behavior.AjaxBehavior;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.convert.BigIntegerConverter;
import javax.faces.convert.FloatConverter;
import javax.faces.convert.IntegerConverter;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.HtmlInputFileUpload;
import org.cesecore.util.ui.DynamicUiModel;
import org.cesecore.util.ui.DynamicUiModelException;
import org.cesecore.util.ui.DynamicUiProperty;
import org.ejbca.ui.web.admin.configuration.EjbcaJSFHelper;

/**
 * Factory for PSM JSF 2 HTML components.
 *
 * @version $Id$
 */
public class JsfDynamicUiPsmFactory {
    
    /** Class logger. */
    private static final Logger log = Logger.getLogger(JsfDynamicUiPsmFactory.class);

    private static final String STYLE_CLASS_SUB_ITEM = "subItem";
    
    /**
     * Initializes the dynamic UI properties on a grid panel with two columns, label on the left, UI component on the right.
     * @param panelGrid the panel grid instance to (re-)build.
     * @param properties the dynamic UI properties.
     * @param i18nPrefix the name prefix for the I18N message resources.
     * @throws DynamicUiModelException if the PSM could not be created by the dynamic UI model.
     */
    public static final void initGridInstance(final HtmlPanelGrid panelGrid, final DynamicUiModel model,
            final String i18nPrefix) throws DynamicUiModelException {
        final Map<String, DynamicUiProperty<? extends Serializable>> properties = model.getProperties();
        if (log.isDebugEnabled()) {
            log.debug("Build dynamic UI model PSM " + model + ", i18nPrefix is '" + i18nPrefix + "'.");
        }
        panelGrid.getFacets().clear();
        panelGrid.getChildren().clear();
        panelGrid.setColumns(2);
        // Build PSM fields by PIM.
        HtmlOutputLabel label;
        UIComponentBase component;
        int index = 0;
        for (DynamicUiProperty<? extends Serializable> property : properties.values()) {
            label = new HtmlOutputLabel();
            label.setValue(getText(i18nPrefix, property.getName()));
            label.setStyleClass(STYLE_CLASS_SUB_ITEM);
            if (index == 0 && label instanceof HtmlOutputLabel) { // Re-factor: Set header bold.
                ((HtmlOutputLabel) label).setStyle("font-weight: bold;");
            }
            panelGrid.getChildren().add(label);
            if (!property.isLabelOnly()) {
                component = createComponentInstance(i18nPrefix, property);
                panelGrid.getChildren().add(component);
                if (log.isDebugEnabled()) {
                    log.debug("Registered UIComponent " + component + " for dynamic UI property " + property.getName());
                }
            } else {
                label.setStyle("font-weight: bold;");
                label.setStyleClass(StringUtils.EMPTY);
                panelGrid.getChildren().add(new HtmlPanelGroup());
            }
            index++;
        }
    }

    /**
     * Create a concrete UIComponentBase instance based on the dynamic UI property.
     * 
     * @param i18nPrefix the message resources prefix.
     * @param property the dynamic UI property ({@link DynamicUiProperty}).
     * @return the JSF component.
     * @throws DynamicUiModelException if a component instance could not be created (i.e. does not exist).
     */
    public static final UIComponentBase createComponentInstance(final String i18nPrefix, final DynamicUiProperty<?> property)
            throws DynamicUiModelException {
        final String hint = property.getRenderingHint();
        UIComponentBase component = null;
        if (DynamicUiProperty.RENDER_NONE.equals(hint)) { // Insert dummy component.
            component = new HtmlPanelGroup();
        }
        else if (property.isBooleanType()) {
            if (DynamicUiProperty.RENDER_CHECKBOX.equals(hint)) {
                component = createCheckBoxInstance(property);
            }
        } else if (property.isStringType()) {
            if (DynamicUiProperty.RENDER_TEXTFIELD.equals(hint)) {
                component = createTextFieldInstance(property);
            } else if (DynamicUiProperty.RENDER_TEXTAREA.equals(hint)) {
                component = createTextAreaInstance(property);
            } else if (DynamicUiProperty.RENDER_SELECT_ONE.equals(hint)) {
                component = createDropDownBoxInstance(property);
            } else if (DynamicUiProperty.RENDER_SELECT_MANY.equals(hint)) {
                component = createListBoxInstance(property);
            } else if (DynamicUiProperty.RENDER_BUTTON.equals(hint)) {
                component = createButtonInstance(i18nPrefix, property);
            } else if (DynamicUiProperty.RENDER_LABEL.equals(hint)) {
                component = createLabelInstance(property);
            }
        } else if (property.isIntegerType()) {
            if (DynamicUiProperty.RENDER_TEXTFIELD.equals(hint)) {
                component = createIntegerTextFieldInstance(property);
            } else if (DynamicUiProperty.RENDER_SELECT_ONE.equals(hint)) {
                component = createIntegerDropDownBoxInstance(property);
            }
        } else if (property.isBigIntegerType()) {
            if (DynamicUiProperty.RENDER_TEXTFIELD.equals(hint)) {
                component = createBigIntegerTextFieldInstance(property);
            }
        } else if (property.isFloatType()) {
            if (DynamicUiProperty.RENDER_TEXTFIELD.equals(hint)) {
                component = createFloatTextFieldInstance(property);
            }
        } else if (property.isFileType()) {
            if (DynamicUiProperty.RENDER_TEXTFIELD.equals(hint)) {
                component = createTextFieldInstance(property);
            } else if (DynamicUiProperty.RENDER_FILE_CHOOSER.equals(hint)) {
                component = createFileChooserInstance(property);
            }
        }
        if (component == null) {
            throw new DynamicUiModelException("DynmicUiRendering component could not be found ...: " + property);
        }
        return component;
    }

    /**
     * Creates label component by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the label instance.
     */
    public static final HtmlOutputText createLabelInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlOutputLabel result = new JsfDynamicUiHtmlOutputLabel();
        result.setDynamicUiProperty(property);
        setUIOutputAttributes(result, property);
        return result;
    }

    /**
     * Creates check box component by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the check box instance.
     */
    public static final HtmlSelectBooleanCheckbox createCheckBoxInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlSelectBooleanCheckbox result = new JsfDynamicUiHtmlSelectBooleanCheckbox();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        addAjaxListener(property, result);
        return result;
    }

    /**
     * Creates text area component by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the text area instance.
     */
    public static final HtmlInputTextarea createTextAreaInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlInputTextarea result = new JsfDynamicUiHtmlInputTextarea();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        result.setCols(45);
        result.setRows(3);
        return result;
    }

    /**
     * Creates text field component by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the text field instance.
     */
    public static final HtmlInputText createTextFieldInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlInputText result = new JsfDynamicUiHtmlInputText();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        result.setSize(44);
        return result;
    }

    /**
     * Creates text field component for {@link java.lang.Integer} by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the text field instance.
     */
    public static final HtmlInputText createIntegerTextFieldInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlInputText result = new JsfDynamicUiHtmlInputText();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        result.setConverter(new IntegerConverter());
        result.setSize(12);
        return result;
    }

    /**
     * Creates text field component for {@link java.math.BigInteger} by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the text field instance.
     */
    public static final HtmlInputText createBigIntegerTextFieldInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlInputText result = new JsfDynamicUiHtmlInputText();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        result.setConverter(new BigIntegerConverter());
        result.setSize(44);
        return result;
    }

    /**
     * Creates text field component for {@link java.lang.Float} by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the text field instance.
     */
    public static final HtmlInputText createFloatTextFieldInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlInputText result = new JsfDynamicUiHtmlInputText();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        result.setConverter(new FloatConverter());
        result.setSize(12);
        return result;
    }

    /**
     * Creates drop down box component by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the drop down box instance.
     */
    public static final HtmlSelectOneMenu createDropDownBoxInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlSelectOneMenu result = new JsfDynamicUiHtmlSelectOneMenu();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        final List<SelectItem> items = new ArrayList<SelectItem>();
        final Map<?, String> labels = property.getLabels();
        for (Entry<?, String> entry : labels.entrySet()) {
            items.add(new SelectItem(entry.getKey(),
                    property.isI18NLabeled() ? EjbcaJSFHelper.getBean().getEjbcaWebBean().getText(entry.getValue()) : entry.getValue()));
        }
        final UISelectItems selectItems = new UISelectItems();
        selectItems.setValue(items);
        addAjaxListener(property, result);
        result.getChildren().add(selectItems);
        return result;
    }

    /**
     * Creates drop down box component with {@link java.lang.Integer} keys by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the drop down box instance.
     */
    public static final HtmlSelectOneMenu createIntegerDropDownBoxInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlSelectOneMenu result = new JsfDynamicUiHtmlSelectOneMenu();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        result.setConverter(new IntegerConverter());
        final List<SelectItem> items = new ArrayList<SelectItem>();
        final Map<?, String> labels = property.getLabels();
        for (Entry<?, String> entry : labels.entrySet()) {
            items.add(new SelectItem(entry.getKey(),
                    property.isI18NLabeled() ? EjbcaJSFHelper.getBean().getEjbcaWebBean().getText(entry.getValue()) : entry.getValue()));
        }
        final UISelectItems selectItems = new UISelectItems();
        selectItems.setValue(items);
        addAjaxListener(property, result);
        result.getChildren().add(selectItems);
        return result;
    }

    /**
     * Creates a file list box by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the list box instance.
     */
    public static final HtmlSelectManyListbox createListBoxInstance(final DynamicUiProperty<?> property) {
        final JsfDynamicUiHtmlSelectManyListbox result = new JsfDynamicUiHtmlSelectManyListbox();
        result.setDynamicUiProperty(property);
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        final List<SelectItem> items = new ArrayList<SelectItem>();
        final Map<?, String> labels = property.getLabels();
        for (Entry<?, String> entry : labels.entrySet()) {
            items.add(new SelectItem(entry.getKey(),
                    property.isI18NLabeled() ? EjbcaJSFHelper.getBean().getEjbcaWebBean().getText(entry.getValue()) : entry.getValue()));
        }
        final UISelectItems selectItems = new UISelectItems();
        selectItems.setValue(items);
        result.getChildren().add(selectItems);
        return result;
    }

    /**
     * Creates a command button by the given dynamic UI property.
     * @param i18nPrefix the message resources prefix.
     * @param property the dynamic UI property.
     * @return the command button instance.
     */
    public static final HtmlCommandButton createButtonInstance(final String i18nPrefix, final DynamicUiProperty<?> property) {
        final HtmlCommandButton result = new HtmlCommandButton();
        result.setId(property.getName());
        result.setRendered(true);
        result.setDisabled(property.isDisabled());
        result.setValue(getText(i18nPrefix, (String) property.getValue()));
        result.addActionListener(new JsfDynamicUiActionListener(property));
        return result;
    }

    /**
     * Creates a file chooser component by the given dynamic UI property.
     * @param property the dynamic UI property.
     * @return the file chooser instance.
     */
    public static final HtmlInputFileUpload createFileChooserInstance(final DynamicUiProperty<?> property) {
        final HtmlInputFileUpload result = new HtmlInputFileUpload();
        setUIInputAttributes(result, property);
        result.setDisabled(property.isDisabled());
        result.setSize(44);
        return result;
    }

    /**
     * Sets the common properties for a component.
     * @param component the component.
     * @param property the dynamic property.
     */
    private static final void setBaseProperties(final UIComponentBase component, final DynamicUiProperty<? extends Serializable> property) {
        final String name = property.getName();
        component.setId(name);
        component.setRendered(true);
    }    
    
    /**
     * Sets the common properties for a component.
     * @param component the component.
     * @param property the dynamic property.
     */
    private static final void setUIOutputAttributes(final UIOutput component, final DynamicUiProperty<? extends Serializable> property) {
        setBaseProperties(component, property);
        if (!property.getHasMultipleValues()) {
            component.setValue(property.getValue());
        } else {
            component.setValue(property.getValues());
        }
    }
    
    /**
     * Sets the common properties for a component.
     * @param component the component.
     * @param property the dynamic property.
     */
    private static final void setUIInputAttributes(final UIInput component, final DynamicUiProperty<? extends Serializable> property) {
        setUIOutputAttributes(component, property);
        component.setRequired(property.isRequired());
        component.addValueChangeListener(new JsfDynamicUiValueChangeListener(property));
    }

    /**
     * Gets an I18N message.
     * @param i18nPrefix the name prefix.
     * @param name the name.
     * @return the I18N message if present, i18nPrefix.concat(name).toUpperCase() otherwise.
     */
    private static final String getText(final String i18nPrefix, final String name) {
        return EjbcaJSFHelper.getBean().getEjbcaWebBean().getText(i18nPrefix.concat(name).toUpperCase());
    }

    /**
     * Adds an ajax behavior listener to the component if the dynamic UI properties action callback is not null.
     * @param property the dynamic UI property.
     * @param component the JSF UIInput component.
     */
    private static final void addAjaxListener(final DynamicUiProperty<? extends Serializable> property, final UIInput component) {
        if (property.getActionCallback() != null) {
            if (log.isDebugEnabled()) {
                log.debug("Registered dynamic UI model action callback for component " + property.getName() + ".");
            }
            final AjaxBehavior behavior = (AjaxBehavior) FacesContext.getCurrentInstance().getApplication().createBehavior(AjaxBehavior.BEHAVIOR_ID);
            behavior.addAjaxBehaviorListener(new JsfDynamicUiAjaxBehaviorListener(property, component));
            behavior.setRender(Arrays.asList(new String[] {"@all"}));
            behavior.setTransient(false);
            behavior.setImmediate(true);
            component.addClientBehavior("change", behavior);
        }
    }

    /** 
     * Avoid instantiation.
     */
    private JsfDynamicUiPsmFactory() {
    }
}
