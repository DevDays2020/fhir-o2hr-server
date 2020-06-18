package edu.gatech.chai.fhir.scheduled;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@Component
public class O2HRSubscribeTask {
	private String HRstate = "NotConnected";
	private String O2state = "NotConnected";
	private IGenericClient client;
	private IIdType HRsubscriptionId;
	private IIdType O2subscriptionId;
	public O2HRSubscribeTask() {
		FhirContext myFhirCtx = FhirContext.forR4();
		client = myFhirCtx.newRestfulGenericClient("https://hapi4-dev.lantanagroup.com/fhir");
		HRsubscriptionId = new IdType();
		O2subscriptionId = new IdType();
		checkSubscriptions();
	}
	
	//Check every 15 minutes
	@Scheduled(fixedDelay = 900000)
	public void checkSubscriptions() {
		checkHRSubscription();
		checkO2Subscription();
	}
	
	public void checkHRSubscription() {
		//Search for Id
		Bundle subs = client.search().forResource(Subscription.class)
		.where(Subscription.CRITERIA.matches().values("Observation?code=http://loinc.org|8867-4"))
		.returnBundle(Bundle.class).execute();
		for(BundleEntryComponent bec:subs.getEntry()) {
			Subscription possibleSub = (Subscription)bec.getResource();
			if(possibleSub.getChannel().getEndpoint().equalsIgnoreCase("http://52.188.54.157:8080/fhir/Observation")) {
				HRsubscriptionId = possibleSub.getIdElement();
				HRstate = "created";
				return;
			}
		}
		Subscription hrSubscription = new Subscription();
		hrSubscription.setStatus(SubscriptionStatus.REQUESTED);
		hrSubscription.setReason("Heart Rate");
		hrSubscription.setCriteria("Observation?code=http://loinc.org|8867-4");
		SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
		channel.setType(SubscriptionChannelType.RESTHOOK);
		channel.setEndpoint("http://52.188.54.157:8080/fhir/Observation");
		channel.setPayload("application/fhir+xml");
		channel.addHeader("Authorization: Basic Y2xpZW50OnNlY3JldA==");
		hrSubscription.setChannel(channel);
		MethodOutcome outcome = client.create().resource(hrSubscription).encodedJson().execute();
		if(outcome.getCreated()) {
			HRstate = "created";
			HRsubscriptionId = outcome.getId();
		}
		else {
			HRstate = "error";
		}
	}
	
	public void checkO2Subscription() {
		//Search for Id
		Bundle subs = client.search().forResource(Subscription.class)
		.where(Subscription.CRITERIA.matches().values("Observation?code=http://loinc.org|2708-6"))
		.returnBundle(Bundle.class).execute();
		for(BundleEntryComponent bec:subs.getEntry()) {
			Subscription possibleSub = (Subscription)bec.getResource();
			if(possibleSub.getChannel().getEndpoint().equalsIgnoreCase("http://52.188.54.157:8080/fhir/Observation")) {
				HRsubscriptionId = possibleSub.getIdElement();
				O2state = "created";
				return;
			}
		}
		Subscription o2Subscription = new Subscription();
		o2Subscription.setStatus(SubscriptionStatus.REQUESTED);
		o2Subscription.setReason("Oxygen saturation");
		o2Subscription.setCriteria("Observation?code=http://loinc.org|2708-6");
		SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
		channel.setType(SubscriptionChannelType.RESTHOOK);
		channel.setEndpoint("http://52.188.54.157:8080/fhir/Observation");
		channel.setPayload("application/fhir+xml");
		channel.addHeader("Authorization: Basic Y2xpZW50OnNlY3JldA==");
		o2Subscription.setChannel(channel);
		MethodOutcome outcome = client.create().resource(o2Subscription).encodedJson().execute();
		if(outcome.getCreated()) {
			O2state = "created";
			O2subscriptionId = outcome.getId();
		}
		else {
			O2state = "error";
		}
	}
}
