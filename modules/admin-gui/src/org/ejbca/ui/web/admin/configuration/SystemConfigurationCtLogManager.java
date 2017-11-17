package org.ejbca.ui.web.admin.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.cesecore.certificates.certificatetransparency.CTLogInfo;
import org.cesecore.certificates.certificatetransparency.CtLogManager;
import org.cesecore.keys.util.KeyTools;

/**
 * This class is used to manage CT logs in EJBCA's system configuration. It adds some additional
 * functionality to the CtLogManager, such as loading and saving state from the database, editing of
 * new CT logs, checking whether a CT log is in use before removing it and language awareness.
 * @version $Id$
 */
public class SystemConfigurationCtLogManager extends CtLogManager {
    private static final String EDIT_CT_LOG = "editCTLog";
    private static final String CT_LOG_SAVED = "saved";
    private static final Logger log = Logger.getLogger(SystemConfigurationCtLogManager.class);
    private final SystemConfigurationHelper systemConfigurationHelper;
    private Map<String, CtLogEditor> ctLogEditors;
    private CtLogEditor defaultCtLogEditor;
    private CtLogEditor ctLogEditorInEditMode;

    public class CtLogEditor {
        private String url;
        private UploadedFile publicKeyFile;
        private String label;
        private int timeout;
        private CTLogInfo ctLogBeingEdited;

        public String getCtLogUrl() {
            return url;
        }

        public UploadedFile getPublicKeyFile() {
            return publicKeyFile;
        }

        public String getCtLogLabel() {
            return label;
        }

        public int getCtLogTimeout() {
            return timeout;
        }

        public void setCtLogUrl(final String url) {
            this.url = url;
        }

        public void setPublicKeyFile(final UploadedFile publicKeyFile) {
            this.publicKeyFile = publicKeyFile;
        }

        public void setCtLogLabel(final String label) {
            this.label = label;
        }

        public void setCtLogTimeout(final int timeout) {
            this.timeout = timeout;
        }

        /**
         * Load an existing CT log into the editor.
         */
        public void loadIntoEditor(final CTLogInfo ctLog) {
            this.label = ctLog.getLabel();
            // Only replace the key if a new one was uploaded
            this.publicKeyFile = null;
            this.timeout = ctLog.getTimeout();
            this.url = ctLog.getUrl();
            this.ctLogBeingEdited = ctLog;
        }

        /**
         * Reset all input to this CT log editor.
         */
        public void clear() {
            url = null;
            publicKeyFile = null;
            label = null;
            timeout = 0;
        }

        /**
         * Returns the CT log currently being edited by this CT log editor.
         * @return the CT log being edited, or null
         */
        public CTLogInfo getCtLogBeingEdited() {
            return ctLogBeingEdited;
        }

        public void stopEditing() {
            ctLogBeingEdited = null;
            clear();
        }
    }

    public interface SystemConfigurationHelper {
        /**
         * Displays an error message to the user.
         * @param languageKey the language key of the message to show
         */
        public void addErrorMessage(String languageKey);

        /**
         * Displays an error message to the user with a formatted message.
         * @param languageKey the language key of the message to show
         * @param params additional parameters to include in the error message
         */
        public void addErrorMessage(String languageKey, Object... params);

        /**
         * Displays an information message to the user.
         * @param languageKey the language key of the message to show
         */
        public void addInfoMessage(String languageKey);

        /**
         * Saves a list of CT logs to persistent storage.
         * @param ctLogs the CT logs to save
         */
        public void saveCtLogs(List<CTLogInfo> ctLogs);

        /**
         * Get a list with names of certificate profiles which references a particular CT log.
         * @param ctLog a CT log which should be checked
         * @return a list of profile names, referencing the CT log given as input or empty if the CT log is not in use
         */
        public List<String> getCertificateProfileNamesByCtLog(CTLogInfo ctLog);
    }

    public SystemConfigurationCtLogManager(final List<CTLogInfo> ctLogs, final SystemConfigurationHelper systemConfigurationHelper) {
        super(ctLogs);
        this.systemConfigurationHelper = systemConfigurationHelper;
        this.ctLogEditors = getCtLogEditors();
        this.defaultCtLogEditor = new CtLogEditor();
    }

    /**
     * Creates one CT log editor per label and returns them in a map.
     * @return mapping between labels and CT log editors
     */
    private Map<String, CtLogEditor> getCtLogEditors() {
        final HashMap<String, CtLogEditor> availableCtLogEditors = new HashMap<String, CtLogEditor>();
        for (final String label : super.getLabels()) {
            availableCtLogEditors.put(label, new CtLogEditor());
        }
        return availableCtLogEditors;
    }

    private byte[] getCtLogPublicKey(final UploadedFile upload) {
        if (log.isDebugEnabled()) {
            log.debug("Received uploaded public key file: " + upload.getName());
        }
        try {
            byte[] uploadedFileBytes = upload.getBytes();
            return KeyTools.getBytesFromPublicKeyFile(uploadedFileBytes);
        } catch (final IOException e) {
            log.info("Could not parse the public key file.", e);
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_BADKEYFILE", upload.getName(), e.getLocalizedMessage());
            return null;
        } catch (final Exception e) {
            log.info("Failed to add CT Log.", e);
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_GENERICADDERROR", e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Add a new CT log with the label given as argument. The contents of the CT log, such as URL and
     * public key is fetched from the corresponding CT log editor.
     * @param label the label of the new CT log
     */
    public void addCtLog(final String label) {
        addCtLog(getCtLogEditor(label), label);
    }

    /**
     * Adds a CT log with the information stored in the default CT log editor.
     */
    public void addCtLog() {
        addCtLog(defaultCtLogEditor, defaultCtLogEditor.getCtLogLabel());
    }

    private void addCtLog(final CtLogEditor ctLogEditor, final String label) {
        if (ctLogEditor.getCtLogUrl() == null || !ctLogEditor.getCtLogUrl().contains("://")) {
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_MISSINGPROTOCOL");
            return;
        }
        if (ctLogEditor.getPublicKeyFile() == null) {
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_UPLOADFAILED");
            return;
        }
        if (ctLogEditor.getCtLogTimeout() <= 0) {
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_TIMEOUTNEGATIVE");
            return;
        }

        final byte[] newCtLogPublicKey = getCtLogPublicKey(ctLogEditor.getPublicKeyFile());
        if (newCtLogPublicKey == null) {
            // Error already reported
            return;
        }

        final CTLogInfo newCtLog = new CTLogInfo(CTLogInfo.fixUrl(ctLogEditor.getCtLogUrl()), newCtLogPublicKey, label,
                ctLogEditor.getCtLogTimeout());

        if (!super.canAdd(newCtLog)) {
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_ALREADYEXISTS", newCtLog.toString());
            return;
        }

        super.addCtLog(newCtLog);
        systemConfigurationHelper.saveCtLogs(super.getAllCtLogs());

        ctLogEditors = getCtLogEditors();
        ctLogEditor.clear();
    }

    @Override
    public void removeCtLog(final CTLogInfo ctLog) {
        final List<String> usedByProfiles = systemConfigurationHelper.getCertificateProfileNamesByCtLog(ctLog);
        if (!usedByProfiles.isEmpty()) {
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_INUSEBYPROFILES", StringUtils.join(usedByProfiles, ", "));
            return;
        }
        super.removeCtLog(ctLog);
        ctLogEditors = getCtLogEditors();
        systemConfigurationHelper.saveCtLogs(super.getAllCtLogs());
    }

    @Override
    public void moveUp(final CTLogInfo ctLog) {
        super.moveUp(ctLog);
        systemConfigurationHelper.saveCtLogs(super.getAllCtLogs());
        systemConfigurationHelper.addInfoMessage("CTLOGTAB_MOVEDCTLOGS");
    }

    @Override
    public void moveDown(final CTLogInfo ctLog) {
        super.moveDown(ctLog);
        systemConfigurationHelper.saveCtLogs(super.getAllCtLogs());
        systemConfigurationHelper.addInfoMessage("CTLOGTAB_MOVEDCTLOGS");
    }

    /**
     * Returns a list of all available labels for a given CT log, including the label of the CT log itself.
     * A label is considered "available" if it is possible to switch to this label without creating duplicates.
     * @return a list of available labels for the given CT log
     */
    public List<String> getAvailableLabels(final CTLogInfo ctLog) {
        final List<String> allLabels = super.getLabels();
        final List<String> availableLabels = new ArrayList<String>();
        for (final String label : allLabels) {
            if (StringUtils.equals(ctLog.getLabel(), label) || !groupHasCtLogWithUrl(getCtLogsByLabel(label), ctLog.getUrl())) {
                availableLabels.add(label);
            }
        }
        return availableLabels;
    }

    private boolean groupHasCtLogWithUrl(final List<CTLogInfo> ctLogGroup, final String url) {
        for (final CTLogInfo ctLog : ctLogGroup) {
            if (StringUtils.equals(ctLog.getUrl(), url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prepares for a CT log to be edited. This method will load the specified CT log into
     * the correct CT log editor and set the editor in edit mode.
     * @param ctLog the CT log to be edited
     * @return the constant string EDIT_CT_LOG
     */
    public String editCtLog(final CTLogInfo ctLog) {
        final CtLogEditor ctLogEditor = getCtLogEditor(ctLog.getLabel());
        ctLogEditor.loadIntoEditor(ctLog);
        ctLogEditorInEditMode = ctLogEditor;
        return EDIT_CT_LOG;
    }

    /**
     * Returns an editor of CT logs for a particular label which allows you to
     * change the fields of a CT log to add.
     * @return an editor of CT logs for the label given as argument
     */
    public CtLogEditor getCtLogEditor(final String label) {
        return ctLogEditors.get(label);
    }

    /**
     * Retrieves the default CT log editor for this CT log manager.
     * @return the default CT log editor
     */
    public CtLogEditor getDefaultCtLogEditor() {
        return defaultCtLogEditor;
    }

    /**
     * Returns the CT log editor belonging to the CT log given as argument
     * to a previous invocation of @link{#editCtLog}.
     * @return the CT editor currently in edit mode
     */
    public CtLogEditor getCtLogEditorInEditMode() {
        return ctLogEditorInEditMode;
    }

    /**
     * Save the CT log currently being edited.
     * @return an empty string on failure or the constant string CT_LOG_SAVED on success
     * @throws IllegalStateException if there is no CT log to save
     */
    public String saveCtLogBeingEdited() {
        if (ctLogEditorInEditMode == null) {
            throw new IllegalStateException("No CT log editor in edit mode");
        }
        if (ctLogEditorInEditMode.getCtLogBeingEdited() == null) {
            throw new IllegalStateException("The CT log being edited has already been saved or was never loaded.");
        }

        /* Validate data entry by the user */
        if (!ctLogEditorInEditMode.getCtLogUrl().contains("://")) {
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_MISSINGPROTOCOL");
            return StringUtils.EMPTY;
        }
        if (ctLogEditorInEditMode.getCtLogTimeout() <= 0) {
            systemConfigurationHelper.addErrorMessage("CTLOGTAB_TIMEOUTNEGATIVE");
            return StringUtils.EMPTY;
        }
        if (ctLogEditorInEditMode.getPublicKeyFile() != null) {
            final byte[] keyBytes = getCtLogPublicKey(ctLogEditorInEditMode.getPublicKeyFile());
            if (keyBytes == null) {
                // Error already reported
                return StringUtils.EMPTY;
            }
        }

        /* Ensure the new log configuration is not conflicting with another log */
        final CTLogInfo ctLogToUpdate = ctLogEditorInEditMode.getCtLogBeingEdited();
        for (final CTLogInfo existing : super.getAllCtLogs()) {
            // TODO Perhaps change this logic to something else
            final boolean isSameLog = existing.getLogId() == ctLogToUpdate.getLogId();
            final boolean hasSameUrl = StringUtils.equals(existing.getUrl(), ctLogToUpdate.getUrl());
            final boolean inSameGroup = StringUtils.equals(existing.getLabel(), ctLogToUpdate.getLabel());
            if (!isSameLog && hasSameUrl && inSameGroup) {
                systemConfigurationHelper.addErrorMessage("CTLOGTAB_ALREADYEXISTS", existing.getUrl());
                return StringUtils.EMPTY;
            }
        }

        /* Update the configuration */
        final String url = ctLogEditorInEditMode.getCtLogUrl();
        final byte[] keyBytes = ctLogEditorInEditMode.getPublicKeyFile() != null ? getCtLogPublicKey(ctLogEditorInEditMode.getPublicKeyFile())
                : ctLogEditorInEditMode.getCtLogBeingEdited().getPublicKeyBytes();
        final int timeout = ctLogEditorInEditMode.getCtLogTimeout();
        final String label = ctLogEditorInEditMode.getCtLogLabel();
        ctLogToUpdate.setLogPublicKey(keyBytes);
        ctLogToUpdate.setTimeout(timeout);
        ctLogToUpdate.setUrl(url);
        ctLogToUpdate.setLabel(label);
        systemConfigurationHelper.saveCtLogs(super.getAllCtLogs());
        ctLogEditorInEditMode.stopEditing();
        ctLogEditors = getCtLogEditors();
        return CT_LOG_SAVED;
    }

    @Override
    public void renameLabel(final String oldLabel, final String newLabel) {
        super.renameLabel(oldLabel, newLabel);
        systemConfigurationHelper.saveCtLogs(super.getAllCtLogs());
        ctLogEditors = getCtLogEditors();
    }
}
