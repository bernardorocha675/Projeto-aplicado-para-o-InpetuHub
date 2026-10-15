package br.com.inpetu.hub.observability;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ServerLogEvents {
  @EventListener
  public void ready(ApplicationReadyEvent event) {
    OperationalEvents.lifecycle(true);
  }

  @EventListener
  public void stopped(ContextClosedEvent event) {
    OperationalEvents.lifecycle(false);
  }
}
