/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.workflow.simple.alfresco.conversion;

import java.text.MessageFormat;
import java.util.UUID;

import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.workflow.simple.alfresco.model.M2Model;
import org.activiti.workflow.simple.alfresco.model.M2Namespace;
import org.activiti.workflow.simple.alfresco.model.config.Configuration;
import org.activiti.workflow.simple.alfresco.model.config.Form;
import org.activiti.workflow.simple.alfresco.model.config.FormField;
import org.activiti.workflow.simple.alfresco.model.config.FormFieldControl;
import org.activiti.workflow.simple.alfresco.model.config.FormFieldControlParameter;
import org.activiti.workflow.simple.alfresco.model.config.Module;
import org.activiti.workflow.simple.converter.WorkflowDefinitionConversion;
import org.activiti.workflow.simple.converter.listener.WorkflowDefinitionConversionListener;

/**
 * A {@link WorkflowDefinitionConversionListener} that creates a {@link M2Model} and a {@link Configuration}
 * before conversion, that can be used to add any models and configuration needed throughout the conversion.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class InitializeAlfrescoModelsConversionListener implements WorkflowDefinitionConversionListener, AlfrescoConversionConstants {

  private static final long serialVersionUID = 1L;
  
	@Override
	public void beforeStepsConversion(WorkflowDefinitionConversion conversion) {
		String processId = null;
		if(conversion.getWorkflowDefinition().getId() != null) {
			processId = AlfrescoConversionUtil.getValidIdString(conversion.getWorkflowDefinition().getId());
		} else {
			processId = generateUniqueProcessId(conversion);
		}
		addContentModel(conversion, processId);
		addModule(conversion, processId);
	}

	@Override
	public void afterStepsConversion(WorkflowDefinitionConversion conversion) {
		for(FlowElement flowElement : conversion.getProcess().getFlowElements()) {
			if(flowElement instanceof StartEvent) {
				StartEvent startEvent = (StartEvent) flowElement;
				if(startEvent.getFormKey() == null) {
					startEvent.setFormKey(DEFAULT_START_FORM_TYPE);
					
					// Also add form-config to the share-module for workflow detail screen, based on the default form
					Module module = AlfrescoConversionUtil.getModule(conversion);
					Configuration detailsForm = module.addConfiguration(EVALUATOR_STRING_COMPARE, 
							MessageFormat.format(EVALUATOR_CONDITION_ACTIVITI, conversion.getProcess().getId()));
					
					populateDefaultDetailFormConfig(detailsForm);
				}
			}
		}
	}
	
	protected String generateUniqueProcessId(WorkflowDefinitionConversion conversion) {
		String processId = AlfrescoConversionUtil.getValidIdString(
				PROCESS_ID_PREFIX + UUID.randomUUID().toString());
		conversion.getProcess().setId(processId);
		return processId;
  }
	
	protected void addContentModel(WorkflowDefinitionConversion conversion, String processId) {
		// The process ID is used as namespace prefix, to guarantee uniqueness
		
		// Set general model properties
		M2Model model = new M2Model();
		model.setName(AlfrescoConversionUtil.getQualifiedName(processId, 
				CONTENT_MODEL_UNQUALIFIED_NAME));
		
		M2Namespace namespace = AlfrescoConversionUtil.createNamespace(processId);
		model.getNamespaces().add(namespace);
		
		
		// Import required alfresco models
		model.getImports().add(DICTIONARY_NAMESPACE);
		model.getImports().add(CONTENT_NAMESPACE);
		model.getImports().add(BPM_NAMESPACE);
		
		// Store model in the conversion artifacts to be accessed later
		AlfrescoConversionUtil.storeContentModel(model, conversion);
		AlfrescoConversionUtil.storeModelNamespacePrefix(namespace.getPrefix(), conversion);
  }
	
	protected void addModule(WorkflowDefinitionConversion conversion, String processId) {
		// Create form-configuration
		Module module = new Module();
		module.setId(MessageFormat.format(MODULE_ID, processId));
		AlfrescoConversionUtil.storeModule(module, conversion);
  }
	
	protected void populateDefaultDetailFormConfig(Configuration configuration) {
	  Form form = configuration.createForm();
	  
	  // Add visibility of fields
	  form.getFormFieldVisibility().addShowFieldElement(PROPERTY_WORKFLOW_DESCRIPTION);
	  form.getFormFieldVisibility().addShowFieldElement(PROPERTY_WORKFLOW_DUE_DATE);
	  form.getFormFieldVisibility().addShowFieldElement(PROPERTY_WORKFLOW_PRIORITY);
	  form.getFormFieldVisibility().addShowFieldElement(PROPERTY_PACKAGEITEMS);
	  form.getFormFieldVisibility().addShowFieldElement(PROPERTY_SEND_EMAIL_NOTIFICATIONS);
	  
	  // Add all sets to the appearance
	  form.getFormAppearance().addFormSet(FORM_SET_GENERAL, FORM_SET_APPEARANCE_TITLE, FORM_SET_GENERAL_LABEL, null);
	  form.getFormAppearance().addFormSet(FORM_SET_INFO, null, null, FORM_SET_TEMPLATE_2_COLUMN);
	  form.getFormAppearance().addFormSet(FORM_SET_ASSIGNEE, FORM_SET_APPEARANCE_TITLE, FORM_SET_ASSIGNEE_LABEL, null);
	  form.getFormAppearance().addFormSet(FORM_SET_ITEMS, FORM_SET_APPEARANCE_TITLE, FORM_SET_ITEMS_LABEL, null);
	  form.getFormAppearance().addFormSet(FORM_SET_OTHER, FORM_SET_APPEARANCE_TITLE, FORM_SET_OTHER_LABEL, null);
	  
	  // Finally, add the individual fields
	  FormField descriptionField = new FormField();
	  descriptionField.setId(PROPERTY_WORKFLOW_DESCRIPTION);
	  descriptionField.setControl(new FormFieldControl(FORM_MULTILINE_TEXT_TEMPLATE));
	  descriptionField.setLabelId(FORM_WORKFLOW_DESCRIPTION_LABEL);
	  form.getFormAppearance().addFormAppearanceElement(descriptionField);
	  
	  FormField dueDateField = new FormField();
	  dueDateField.setId(PROPERTY_WORKFLOW_DUE_DATE);
	  dueDateField.setSet(FORM_SET_INFO);
	  dueDateField.setLabelId(FORM_WORKFLOW_DUE_DATE_LABEL);
	  dueDateField.setControl(new FormFieldControl(FORM_DATE_TEMPLATE));
	  dueDateField.getControl().getControlParameters().add(new FormFieldControlParameter(FORM_DATE_PARAM_SHOW_TIME, Boolean.FALSE.toString()));
	  dueDateField.getControl().getControlParameters().add(new FormFieldControlParameter(FORM_DATE_PARAM_SUBMIT_TIME, Boolean.FALSE.toString()));
	  form.getFormAppearance().addFormAppearanceElement(dueDateField);
	  
	  FormField priorityField = new FormField();
	  priorityField.setSet(FORM_SET_INFO);
	  priorityField.setLabelId(FORM_WORKFLOW_PRIORITY_LABEL);
	  priorityField.setId(PROPERTY_WORKFLOW_PRIORITY);
	  priorityField.setControl(new FormFieldControl(FORM_PRIORITY_TEMPLATE));
	  form.getFormAppearance().addFormAppearanceElement(priorityField);
	  
	  form.getFormAppearance().addFormField(PROPERTY_PACKAGEITEMS, null, FORM_SET_ITEMS);
	  
	  FormField emailNotificationsField = new FormField();
	  emailNotificationsField.setSet(FORM_SET_OTHER);
	  emailNotificationsField.setId(PROPERTY_SEND_EMAIL_NOTIFICATIONS);
	  emailNotificationsField.setControl(new FormFieldControl(FORM_EMAIL_NOTIFICATION_TEMPLATE));
	  form.getFormAppearance().addFormAppearanceElement(emailNotificationsField);
	}
}
