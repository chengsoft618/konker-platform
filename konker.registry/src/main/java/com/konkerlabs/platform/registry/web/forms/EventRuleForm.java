package com.konkerlabs.platform.registry.web.forms;

import com.konkerlabs.platform.registry.business.model.EventRule;
import com.konkerlabs.platform.registry.business.model.behaviors.DeviceURIDealer;
import com.konkerlabs.platform.registry.business.model.behaviors.SmsURIDealer;
import com.konkerlabs.platform.registry.business.services.rules.EventRuleExecutorImpl;
import com.konkerlabs.platform.registry.web.forms.api.ModelBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

@Data
@EqualsAndHashCode(exclude={"tenantDomainSupplier"})
public class EventRuleForm implements ModelBuilder<EventRule,EventRuleForm,String>,
        DeviceURIDealer,
        SmsURIDealer {

    private String id;
    private String name;
    private String description;
    private String incomingAuthority;
    private String incomingChannel;
    private String outgoingScheme = DeviceURIDealer.DEVICE_URI_SCHEME;
    private String outgoingDeviceAuthority;
    private String outgoingDeviceChannel;
    private String outgoingSmsPhoneNumber;
    private String filterClause;
    private boolean active;
    private Supplier<String> tenantDomainSupplier;

    @Override
    public EventRule toModel() {
        Optional.ofNullable(tenantDomainSupplier)
            .orElseThrow(() -> new IllegalStateException("Tenant domain name supplier cannot be null"));

        Optional.ofNullable(tenantDomainSupplier.get())
            .filter(s -> !s.isEmpty())
            .orElseThrow(() -> new IllegalStateException("Tenant domain name supplier cannot return null or empty"));

        EventRule rule;

        EventRule.RuleTransformation contentFilterTransformation = new EventRule.RuleTransformation(EventRuleExecutorImpl.RuleTransformationType.EXPRESSION_LANGUAGE.name());
        contentFilterTransformation.getData().put("value",getFilterClause());

        rule = EventRule.builder()
                .id(id)
                .name(getName())
                .description(getDescription())
                .incoming(new EventRule.RuleActor(
                    toDeviceRuleURI(tenantDomainSupplier.get(),getIncomingAuthority())
                ))
                .outgoing(new EventRule.RuleActor(buildOutgoingURI()))
                .transformations(Arrays.asList(new EventRule.RuleTransformation[]{contentFilterTransformation}))
                .active(isActive())
                .build();
        applyIncomingMetadata(rule);
        applyOutgoingMetadata(rule);

        return rule;
    }

    private URI buildOutgoingURI() {
        switch (getOutgoingScheme()) {
            case DeviceURIDealer.DEVICE_URI_SCHEME : return
                toDeviceRuleURI(tenantDomainSupplier.get(),getOutgoingDeviceAuthority());
            case SmsURIDealer.SMS_URI_SCHEME : return
                toSmsURI(getOutgoingSmsPhoneNumber());
            default: return null;
        }
    }

    private void applyIncomingMetadata(EventRule rule) {
        rule.getIncoming().getData().put("channel",getIncomingChannel());
    }

    private void applyOutgoingMetadata(EventRule rule) {
        switch (getOutgoingScheme()) {
            case DeviceURIDealer.DEVICE_URI_SCHEME : {
                rule.getOutgoing().getData().put("channel", getOutgoingDeviceChannel());
                break;
            }
            default: break;
        }
    }

    @Override
    public EventRuleForm fillFrom(EventRule model) {
        this.setId(model.getId());
        this.setName(model.getName());
        this.setDescription(model.getDescription());
        this.setIncomingAuthority(model.getIncoming().getUri().getPath().replaceAll("/",""));
        this.setIncomingChannel(model.getIncoming().getData().get("channel"));
        this.setOutgoingScheme(model.getOutgoing().getUri().getScheme());
        switch (getOutgoingScheme()) {
            case DeviceURIDealer.DEVICE_URI_SCHEME : {
                this.setOutgoingDeviceAuthority(model.getOutgoing().getUri().getPath().replaceAll("/",""));
                this.setOutgoingDeviceChannel(model.getOutgoing().getData().get("channel"));
                break;
            }
            case SmsURIDealer.SMS_URI_SCHEME : {
                this.setOutgoingSmsPhoneNumber(model.getOutgoing().getUri().getAuthority());
                break;
            }
            default: break;
        }
        this.setFilterClause(model.getTransformations().get(0).getData().get("value"));
        this.setActive(model.isActive());
        return this;
    }

    @Override
    public void setAdditionalSupplier(Supplier<String> supplier) {
        tenantDomainSupplier = supplier;
    }
}
