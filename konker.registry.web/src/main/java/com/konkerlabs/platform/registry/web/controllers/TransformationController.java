package com.konkerlabs.platform.registry.web.controllers;

import com.konkerlabs.platform.registry.business.model.Application;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.Transformation;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.TransformationService;
import com.konkerlabs.platform.registry.web.forms.TransformationForm;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("transformation")
@Scope("request")
public class TransformationController implements ApplicationContextAware {

    public enum Messages {
        TRANSFORMATION_REGISTERED_SUCCESSFULLY("controller.transformation.registered.success"),
        TRANSFORMATION_REMOVED_SUCCESSFULLY("controller.transformation.removed.success");

        public String getCode() {
            return code;
        }

        private String code;

        Messages(String code) {
            this.code = code;
        }
    }

    private ApplicationContext applicationContext;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private Tenant tenant;

    @Autowired
    private Application application;

    @RequestMapping
    @PreAuthorize("hasAuthority('LIST_TRANSFORMATION')")
    public ModelAndView index() {
        return new ModelAndView("transformations/index")
                .addObject("transformations", transformationService.getAll(
                        tenant,
                        Application.builder()
                                .name(tenant.getDomainName()).build()
                ).getResult());
    }

    @RequestMapping("new")
    @PreAuthorize("hasAuthority('CREATE_TRANSFORMATION')")
    public ModelAndView newTransformation() {
        return new ModelAndView("transformations/form")
                .addObject("transformation", new TransformationForm())
                .addObject("action", "/transformation/save");
    }

    @RequestMapping(value = "save", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('CREATE_TRANSFORMATION')")
    public ModelAndView save(@ModelAttribute("transformation") TransformationForm transformationForm,
                             BindingResult bindingResult, RedirectAttributes redirectAttributes, Locale locale) {

        ServiceResponse<Transformation> serviceResponse =
                transformationService.register(
                        tenant,
                        transformationForm.toModel().getApplication(),
                        transformationForm.toModel());

        switch (serviceResponse.getStatus()) {
            case ERROR:

                List<String> messages = new ArrayList<>();
                for (Map.Entry<String, Object[]> message : serviceResponse.getResponseMessages().entrySet()) {
                    messages.add(applicationContext.getMessage(message.getKey(), message.getValue(), locale));
                }

                return new ModelAndView("transformations/form")
                        .addObject("errors", messages)
                        .addObject("method", "")
                        .addObject("transformation", transformationForm);
            default:
                redirectAttributes.addFlashAttribute("message",
                        applicationContext.getMessage(Messages.TRANSFORMATION_REGISTERED_SUCCESSFULLY.getCode(), null, locale)
                );
                return new ModelAndView(MessageFormat.format("redirect:/transformation/{0}", serviceResponse.getResult().getGuid()));
        }
    }

    @RequestMapping(value = "/{transformationGuid}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('SHOW_TRANSFORMATION')")
    public ModelAndView show(@PathVariable("transformationGuid") String transformationGuid) {
        return new ModelAndView("transformations/show", "transformation",
                new TransformationForm()
                        .fillFrom(
                                transformationService.get(
                                        tenant,
                                        application,
                                        transformationGuid).getResult()));
    }

    @RequestMapping("/{transformationGuid}/edit")
    @PreAuthorize("hasAuthority('EDIT_TRANSFORMATION')")
    public ModelAndView edit(@PathVariable("transformationGuid") String transformationGuid) {
        return new ModelAndView("transformations/form")
                .addObject("transformation",
                        new TransformationForm()
                                .fillFrom(
                                        transformationService.get(
                                                tenant,
                                                application,
                                                transformationGuid).getResult()))
                .addObject("action", MessageFormat.format("/transformation/{0}", transformationGuid))
                .addObject("method", "put");
    }

    @RequestMapping(path = "/{transformationGuid}", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('EDIT_TRANSFORMATION')")
    public ModelAndView saveEdit(@PathVariable String transformationGuid,
                                 @ModelAttribute("transformation") TransformationForm transformationForm, Locale locale,
                                 RedirectAttributes redirectAttributes) {

        ServiceResponse<Transformation> response =
                transformationService.update(
                        tenant,
                        application,
                        transformationGuid, transformationForm.toModel());

        switch (response.getStatus()) {
            case ERROR: {

                List<String> messages = new ArrayList<>();
                for (Map.Entry<String, Object[]> message : response.getResponseMessages().entrySet()) {
                    messages.add(applicationContext.getMessage(message.getKey(), message.getValue(), locale));
                }

                return new ModelAndView("transformations/form")
                        .addObject("errors", messages)
                        .addObject("method", "put")
                        .addObject("transformation", transformationForm);
            }
            default: {
                redirectAttributes.addFlashAttribute("message",
                        applicationContext.getMessage(Messages.TRANSFORMATION_REGISTERED_SUCCESSFULLY.getCode(), null, locale)
                );
                return new ModelAndView(MessageFormat.format("redirect:/transformation/{0}",
                        response.getResult().getGuid()));
            }
        }
    }

    @RequestMapping(path = "/{transformationGuid}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('REMOVE_TRANSFORMATION')")
    public ModelAndView remove(@PathVariable("transformationGuid") String transformationGuid, @ModelAttribute("transformation") TransformationForm transformationForm,
                               RedirectAttributes redirectAttributes, Locale locale) {
        ModelAndView modelAndView;
        ServiceResponse<Transformation> serviceResponse =
                transformationService.remove(
                        tenant,
                        application,
                        transformationGuid);

        switch (serviceResponse.getStatus()) {
            case ERROR:
                transformationForm.setId(transformationGuid);
                modelAndView = new ModelAndView("transformations/form")
                        .addObject("errors", serviceResponse.getResponseMessages().entrySet().stream().map(message -> applicationContext.getMessage(message.getKey(), message.getValue(), locale)).collect(Collectors.toList()))
                        .addObject("method", "put")
                        .addObject("transformation", transformationForm);
                break;

            default:
                modelAndView = new ModelAndView("redirect:/transformation");
                redirectAttributes.addFlashAttribute("message",
                        applicationContext.getMessage(Messages.TRANSFORMATION_REMOVED_SUCCESSFULLY.getCode(), null, locale));
                break;
        }

        return modelAndView;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
