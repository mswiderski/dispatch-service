package com.acme.ride.dispatch.message.listeners;

import org.jbpm.services.api.ProcessService;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.message.model.Message;
import com.acme.ride.dispatch.message.model.PassengerCanceledEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@Component
public class PassengerCanceledEventMessageListener {

    private final static Logger log = LoggerFactory.getLogger(PassengerCanceledEventMessageListener.class);

    @Autowired
    private ProcessService processService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RideDao rideDao;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @JmsListener(destination = "${listener.destination.passenger-canceled-event}",
            subscription = "${listener.subscription.passenger-canceled-event}")
    public void processMessage(String messageAsJson) {

        if (!accept(messageAsJson)) {
            return;
        }
        Message<PassengerCanceledEvent> message;
        try {

            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<PassengerCanceledEvent>>() {});

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'PassengerCanceled' message for ride " +  rideId);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                
                ProcessInstance instance = processService.getProcessInstance(correlationKey);
                processService.signalProcessInstance(instance.getId(), "PassengerCanceled", null);
                return null;
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private boolean accept(String messageAsJson) {
        try {
            String messageType = JsonPath.read(messageAsJson, "$.messageType");
            if (!"PassengerCanceledEvent".equalsIgnoreCase(messageType) ) {
                log.debug("Message with type '" + messageType + "' is ignored");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Unexpected message without 'messageType' field.");
            return false;
        }
    }

}
