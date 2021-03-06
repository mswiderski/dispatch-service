package com.acme.ride.dispatch;

import java.util.Collection;

import org.jbpm.kie.services.impl.CustomIdKModuleDeploymentUnit;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessDefinition;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.query.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.annotation.EnableJms;

@SpringBootConfiguration
@ComponentScan
@EnableAutoConfiguration
@EnableJms
public class DispatchServiceApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatchServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DispatchServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner deployAndValidate() {
        return new CommandLineRunner() {
            
            @Value("${dispatch.deployment.id}")
            private String deploymentId;
            
            @Autowired            
            private DeploymentService deploymentService;
            
            @Autowired            
            private RuntimeDataService runtimeDataService;
            
            @Override
            public void run(String... strings) throws Exception {                
                CustomIdKModuleDeploymentUnit unit = new CustomIdKModuleDeploymentUnit(deploymentId, "com.acme.ride.dispatch", "dispatch-service", "1.0", 
                        "dispatch-process-kbase", "dispatch-process-ksession");
                
                KieContainer kieContainer = KieServices.Factory.get().newKieClasspathContainer();
                unit.setKieContainer(kieContainer);
                LOGGER.info("Service up and running");
     
                deploymentService.deploy(unit);
                
                Collection<ProcessDefinition> processes = runtimeDataService.getProcesses(new QueryContext());
                processes.forEach(p -> LOGGER.info(p.toString()));
            }
        };
    }
}
